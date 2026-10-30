import { initializeApp, getApps, getApp, type FirebaseApp } from 'firebase/app';
import {
  getFirestore,
  collection,
  doc,
  setDoc,
  getDocs,
  onSnapshot,
  query,
  orderBy,
  type Firestore,
  type Unsubscribe,
} from 'firebase/firestore';
import {
  sanitizeEmail,
  sanitizeIdentifier,
  sanitizePhone,
  sanitizeText,
  issueRoleSessionToken,
  verifyRoleSessionToken,
} from './securityGuard';

export interface FirebaseConfig {
  apiKey: string;
  authDomain?: string;
  projectId: string;
  storageBucket?: string;
  messagingSenderId?: string;
  appId: string;
}

export interface DatabaseSyncStatus {
  provider: 'FIREBASE' | 'SUPABASE' | 'LOCAL_OFFLINE';
  isConnected: boolean;
  lastSyncedAt: string | null;
  syncedOrderCount: number;
  syncNotice: string;
}

const STORAGE_KEYS = {
  ORDERS: 'quick_delivery_orders_v6',
  CONFIG: 'quick_delivery_firebase_config_v6',
  RIDER_WALLET: 'quick_delivery_rider_wallet_v6',
  USER_PROFILES: 'quick_delivery_user_profiles_v6',
  SYNC_STATUS: 'quick_delivery_sync_status_v6',
};

const LEGACY_DEMO_ORDER_IDS = new Set([
  'ord_qd_5001',
  'ord_qd_5000',
  'ord_qd_4999',
  'ord_qd_4998',
  'ord_qd_4997',
  'ord_qd_4996',
]);

// Strip undefined properties recursively so Firebase Firestore setDoc() never throws
// "FirebaseError: Function setDoc() called with invalid data. Unsupported field value: undefined"
function sanitizeForFirestore<T>(input: T): T {
  if (input === null || input === undefined || typeof input !== 'object') {
    return input;
  }
  if (Array.isArray(input)) {
    return input
      .filter((item) => item !== undefined)
      .map((item) => sanitizeForFirestore(item)) as unknown as T;
  }
  const cleaned: Record<string, any> = {};
  for (const [key, value] of Object.entries(input as Record<string, any>)) {
    if (value !== undefined) {
      cleaned[key] = sanitizeForFirestore(value);
    }
  }
  return cleaned as T;
}

// Ensure every order object has safe fallback fields so UI panels never crash on partial/legacy records
export function normalizeOrderRecord(raw: any): any {
  if (!raw || typeof raw !== 'object') return null;
  const validVehicles = new Set(['BIKE', 'EV', 'MINI_TRUCK']);
  const vehicleType = validVehicles.has(raw.vehicleType) ? raw.vehicleType : 'BIKE';
  const totalAmountPaise =
    typeof raw.totalAmountPaise === 'number' && !Number.isNaN(raw.totalAmountPaise)
      ? raw.totalAmountPaise
      : 5500;
  const companyCommissionPaise =
    typeof raw.companyCommissionPaise === 'number' && !Number.isNaN(raw.companyCommissionPaise)
      ? raw.companyCommissionPaise
      : Math.round(totalAmountPaise * 0.2);
  const riderEarningPaise =
    typeof raw.riderEarningPaise === 'number' && !Number.isNaN(raw.riderEarningPaise)
      ? raw.riderEarningPaise
      : totalAmountPaise - companyCommissionPaise;

  const code = sanitizeIdentifier(raw.orderCode || raw.id || 'QD-5002', 40) || 'QD-5002';
  const safeId = sanitizeIdentifier(raw.id || `ord_${code.toLowerCase()}`, 64) || `ord_${code.toLowerCase()}`;
  return sanitizeForFirestore({
    ...raw,
    id: safeId,
    orderCode: code,
    customerName: sanitizeText(raw.customerName || 'Customer', 80) || 'Customer',
    customerPhone: sanitizePhone(raw.customerPhone || '9876543210') || '9876543210',
    receiverName: sanitizeText(raw.receiverName || 'Receiver', 80) || 'Receiver',
    receiverPhone: sanitizePhone(raw.receiverPhone || '9820112233') || '9820112233',
    pickupAddress: sanitizeText(raw.pickupAddress || 'Pickup Location', 300) || 'Pickup Location',
    dropAddress: sanitizeText(raw.dropAddress || 'Drop Location', 300) || 'Drop Location',
    pickupTag: raw.pickupTag || 'Home',
    dropTag: raw.dropTag || 'Office',
    packageCategory: raw.packageCategory || 'Documents',
    weightBracket: raw.weightBracket || 'Up to 5kg',
    weightKg: typeof raw.weightKg === 'number' && !Number.isNaN(raw.weightKg) ? raw.weightKg : 2,
    distanceKm:
      typeof raw.distanceKm === 'number' && !Number.isNaN(raw.distanceKm) ? raw.distanceKm : 4.5,
    vehicleType,
    paymentMethod: String(raw.paymentMethod || 'CASH_ON_DELIVERY'),
    paymentStatus: String(raw.paymentStatus || 'CASH_COLLECT_ON_DELIVERY'),
    status: String(raw.status || 'PENDING_ASSIGNMENT'),
    totalAmountPaise,
    companyCommissionPaise,
    riderEarningPaise,
    pickupOtp: String(raw.pickupOtp || '4821'),
    deliveryOtp: String(raw.deliveryOtp || '9134'),
    createdAt: String(
      raw.createdAt || new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    ),
  });
}

class DatabaseService {
  private app: FirebaseApp | null = null;
  private db: Firestore | null = null;
  private currentConfig: FirebaseConfig | null = null;
  private syncListeners: Unsubscribe[] = [];

  constructor() {
    this.loadSavedConfig();
  }

  // Load saved configuration from localStorage or Vite environment variables
  private loadSavedConfig() {
    try {
      const saved = localStorage.getItem(STORAGE_KEYS.CONFIG);
      if (saved) {
        this.currentConfig = JSON.parse(saved);
        this.initFirebase(this.currentConfig!);
        return;
      }

      // Check Vite env
      const envApiKey = (import.meta as unknown as { env: Record<string, string> }).env?.VITE_FIREBASE_API_KEY;
      const envProjectId = (import.meta as unknown as { env: Record<string, string> }).env?.VITE_FIREBASE_PROJECT_ID;
      const envAppId = (import.meta as unknown as { env: Record<string, string> }).env?.VITE_FIREBASE_APP_ID;

      if (envApiKey && envProjectId) {
        this.currentConfig = {
          apiKey: envApiKey,
          projectId: envProjectId,
          appId: envAppId || '1:quick-delivery:web',
          authDomain: `${envProjectId}.firebaseapp.com`,
        };
        this.initFirebase(this.currentConfig);
      }
    } catch (e) {
      console.warn('Database config init error:', e);
    }
  }

  // Initialize Firebase App & Firestore
  public initFirebase(config: FirebaseConfig): boolean {
    try {
      if (!config.apiKey || !config.projectId) {
        return false;
      }

      if (getApps().length > 0) {
        this.app = getApp();
      } else {
        this.app = initializeApp({
          apiKey: config.apiKey,
          authDomain: config.authDomain || `${config.projectId}.firebaseapp.com`,
          projectId: config.projectId,
          storageBucket: config.storageBucket || `${config.projectId}.appspot.com`,
          messagingSenderId: config.messagingSenderId,
          appId: config.appId,
        });
      }

      this.db = getFirestore(this.app);
      this.currentConfig = config;
      localStorage.setItem(STORAGE_KEYS.CONFIG, JSON.stringify(config));
      return true;
    } catch (e) {
      console.error('Failed to initialize Firebase:', e);
      this.db = null;
      return false;
    }
  }

  public getSavedConfig(): FirebaseConfig | null {
    return this.currentConfig;
  }

  public isConnected(): boolean {
    return Boolean(this.db && this.currentConfig?.projectId);
  }

  // Save single order to Firestore (and LocalStorage)
  public async saveOrder(order: any): Promise<boolean> {
    try {
      const normalized = normalizeOrderRecord(order);
      if (!normalized) return false;

      // 1. Always update local storage for instant zero-latency feedback
      this.saveLocalOrder(normalized);

      // 2. If Firebase Firestore is connected, push sanitized payload to cloud
      if (this.db) {
        const orderRef = doc(this.db, 'delivery_orders', normalized.id);
        await setDoc(
          orderRef,
          sanitizeForFirestore({
            ...normalized,
            updatedAt: new Date().toISOString(),
          }),
          { merge: true }
        );
        return true;
      }
      return true;
    } catch (e) {
      console.error('Error saving order to database:', e);
      return false;
    }
  }

  // Automatically save and sync user profile data in the background using the user's logged-in account
  public async saveUserProfile(user: any): Promise<boolean> {
    if (!user || !user.id) return false;
    try {
      const profilesRaw = localStorage.getItem(STORAGE_KEYS.USER_PROFILES);
      const profilesMap: Record<string, any> = profilesRaw ? JSON.parse(profilesRaw) : {};
      const safeId = sanitizeIdentifier(user.id, 64);
      const safeRole =
        user.role === 'ADMIN' || user.role === 'RIDER' || user.role === 'CUSTOMER'
          ? user.role
          : 'CUSTOMER';
      const safeName = sanitizeText(user.name, 80) || 'User';
      const safeEmail = sanitizeEmail(user.email) || 'user@quickdelivery.in';
      const safePhone = sanitizePhone(user.phone) || '9876543210';
      const sessionToken = issueRoleSessionToken({
        id: safeId,
        role: safeRole,
        email: safeEmail,
        phone: safePhone,
      });

      const updatedProfile = sanitizeForFirestore({
        ...profilesMap[safeId],
        ...user,
        id: safeId,
        name: safeName,
        email: safeEmail,
        phone: safePhone,
        role: safeRole,
        sessionToken,
        updatedAt: new Date().toISOString(),
      });
      profilesMap[safeId] = updatedProfile;
      localStorage.setItem(STORAGE_KEYS.USER_PROFILES, JSON.stringify(profilesMap));
      localStorage.setItem('quick_delivery_auth_user', JSON.stringify(updatedProfile));

      if (this.db) {
        const userRef = doc(this.db, 'user_profiles', safeId);
        await setDoc(userRef, updatedProfile, { merge: true });
      }
      return true;
    } catch (e) {
      console.warn('Profile background sync note:', e);
      return false;
    }
  }

  public getSavedUserProfile(userId: string): any | null {
    try {
      const profilesRaw = localStorage.getItem(STORAGE_KEYS.USER_PROFILES);
      if (!profilesRaw) return null;
      const profilesMap: Record<string, any> = JSON.parse(profilesRaw);
      const profile = profilesMap[sanitizeIdentifier(userId, 64)] || null;
      if (profile && !verifyRoleSessionToken(profile)) {
        return null;
      }
      return profile;
    } catch {
      return null;
    }
  }

  // Automatically synchronize all order, profile, and wallet data in the background for the logged-in user
  public async syncAccountSessionInBackground(
    user: any | null,
    orders: any[],
    riderWallet?: any
  ): Promise<void> {
    try {
      if (user && user.id) {
        await this.saveUserProfile(user);
      }
      if (riderWallet) {
        const cleanWallet = sanitizeForFirestore(riderWallet);
        localStorage.setItem(STORAGE_KEYS.RIDER_WALLET, JSON.stringify(cleanWallet));
        if (this.db && user?.id) {
          const walletRef = doc(this.db, 'rider_wallets', user.id);
          await setDoc(
            walletRef,
            sanitizeForFirestore({
              ...cleanWallet,
              userId: user.id,
              updatedAt: new Date().toISOString(),
            }),
            { merge: true }
          );
        }
      }
      if (Array.isArray(orders)) {
        const cleanOrders = orders
          .filter((o) => o && !LEGACY_DEMO_ORDER_IDS.has(o.id))
          .map((o) => normalizeOrderRecord(o))
          .filter(Boolean);
        localStorage.setItem(STORAGE_KEYS.ORDERS, JSON.stringify(cleanOrders));
        if (this.db && cleanOrders.length > 0) {
          for (const order of cleanOrders) {
            const orderRef = doc(this.db, 'delivery_orders', order.id);
            await setDoc(
              orderRef,
              sanitizeForFirestore({
                ...order,
                syncedByUserId: user?.id || order.userId || 'auto_account_sync',
                updatedAt: new Date().toISOString(),
              }),
              { merge: true }
            );
          }
        }
      }
      localStorage.setItem(
        STORAGE_KEYS.SYNC_STATUS,
        JSON.stringify({
          provider: this.db ? 'FIREBASE' : 'LOCAL_OFFLINE',
          isConnected: Boolean(this.db),
          lastSyncedAt: new Date().toISOString(),
          syncedOrderCount: Array.isArray(orders) ? orders.length : 0,
          accountEmail: user?.email || null,
        })
      );
    } catch (e) {
      console.warn('Background account sync note:', e);
    }
  }

  // Bulk sync all orders from local to cloud
  public async syncAllToCloud(orders: any[]): Promise<{ success: boolean; count: number; message: string }> {
    if (!this.db) {
      return {
        success: false,
        count: 0,
        message: 'Firebase is not initialized. Using automatic account background sync.',
      };
    }

    try {
      let synced = 0;
      for (const rawOrder of orders) {
        const order = normalizeOrderRecord(rawOrder);
        if (!order) continue;
        const orderRef = doc(this.db, 'delivery_orders', order.id);
        await setDoc(
          orderRef,
          sanitizeForFirestore({
            ...order,
            updatedAt: new Date().toISOString(),
          }),
          { merge: true }
        );
        synced++;
      }

      return {
        success: true,
        count: synced,
        message: `Successfully synchronized ${synced} orders to Firebase Firestore collection 'delivery_orders'!`,
      };
    } catch (e: any) {
      return {
        success: false,
        count: 0,
        message: `Cloud sync error: ${e?.message || 'Check Firestore security rules.'}`,
      };
    }
  }

  // Subscribe to real-time changes in Firestore
  public subscribeToOrders(onUpdate: (orders: any[]) => void): () => void {
    if (!this.db) {
      // Fallback: Listen to cross-tab storage events
      const handleStorage = (e: StorageEvent) => {
        if (e.key === STORAGE_KEYS.ORDERS && e.newValue) {
          try {
            const parsed = JSON.parse(e.newValue);
            const clean = Array.isArray(parsed)
              ? parsed
                  .filter((o: any) => o && !LEGACY_DEMO_ORDER_IDS.has(o.id))
                  .map((o: any) => normalizeOrderRecord(o))
                  .filter(Boolean)
              : [];
            onUpdate(clean);
          } catch {}
        }
      };
      window.addEventListener('storage', handleStorage);
      return () => window.removeEventListener('storage', handleStorage);
    }

    try {
      const q = query(collection(this.db, 'delivery_orders'), orderBy('createdAt', 'desc'));
      const unsubscribe = onSnapshot(
        q,
        (snapshot) => {
          const cloudOrders: any[] = [];
          snapshot.forEach((d) => {
            const data = d.data();
            if (data && !LEGACY_DEMO_ORDER_IDS.has(data.id)) {
              const normalized = normalizeOrderRecord(data);
              if (normalized) cloudOrders.push(normalized);
            }
          });
          onUpdate(cloudOrders);
          localStorage.setItem(STORAGE_KEYS.ORDERS, JSON.stringify(cloudOrders));
        },
        (error) => {
          console.warn('Firestore snapshot listener note (rules or offline):', error);
        }
      );

      this.syncListeners.push(unsubscribe);
      return unsubscribe;
    } catch (e) {
      console.warn('Failed to attach Firestore snapshot listener:', e);
      return () => {};
    }
  }

  // Save order to Local Storage with cross-tab notification
  private saveLocalOrder(order: any) {
    try {
      const normalized = normalizeOrderRecord(order);
      if (!normalized) return;
      const existing = this.getLocalOrders();
      const idx = existing.findIndex((o) => o.id === normalized.id);
      let updated: any[];
      if (idx >= 0) {
        updated = existing.map((o) =>
          o.id === normalized.id ? normalizeOrderRecord({ ...o, ...normalized }) : o
        );
      } else {
        updated = [normalized, ...existing];
      }
      localStorage.setItem(STORAGE_KEYS.ORDERS, JSON.stringify(updated));
    } catch (e) {
      console.warn('LocalStorage save error:', e);
    }
  }

  // Get orders stored locally
  public getLocalOrders(): any[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.ORDERS);
      if (!data) return [];
      const parsed = JSON.parse(data);
      if (!Array.isArray(parsed)) return [];
      return parsed
        .filter((o: any) => o && !LEGACY_DEMO_ORDER_IDS.has(o.id))
        .map((o: any) => normalizeOrderRecord(o))
        .filter(Boolean);
    } catch {
      return [];
    }
  }

  public getSavedRiderWallet(): any | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEYS.RIDER_WALLET);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }

  // Clean up listeners
  public disconnect() {
    this.syncListeners.forEach((unsub) => unsub());
    this.syncListeners = [];
    this.db = null;
    this.app = null;
  }
}

export const dbService = new DatabaseService();

