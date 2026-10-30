import React, { useState, useMemo, useEffect, useRef } from 'react';
import {
  ShoppingBag,
  Bike,
  ShieldCheck,
  MapPin,
  Package,
  IndianRupee,
  CheckCircle2,
  Clock,
  Camera,
  Navigation,
  MessageSquare,
  Send,
  Wallet,
  AlertTriangle,
  Download,
  RefreshCw,
  Phone,
  User,
  LayoutGrid,
  Columns,
  BatteryCharging,
  Lock,
  Unlock,
  ArrowRight,
  Truck,
  Zap,
  Star,
  FileText,
  Tag,
  LifeBuoy,
  Menu,
  X,
  Bell,
  Power,
  Home,
  Briefcase,
  Bookmark,
  BadgeCheck,
  Settings,
  Sliders,
  Smartphone,
  Sparkles,
  Megaphone,
  Share2,
  Copy,
  ExternalLink,
  Check,
  QrCode,
  Printer,
  ScanLine,
  Database,
  Cloud,
  Banknote,
  LogOut,
  Radio
} from 'lucide-react';
import { DeliveryMapComponent } from './components/DeliveryMapComponent';
import { ShopFloorUpiQrModal } from './components/ShopFloorUpiQrModal';
import { ParcelQrScannerModal } from './components/ParcelQrScannerModal';
import { dbService } from './services/databaseService';
import { DatabaseConfigModal } from './components/DatabaseConfigModal';
import { LiveOrderTrackingModal } from './components/LiveOrderTrackingModal';
import { AuthScreen, type AuthUser, type UserRole, DEMO_USERS } from './components/AuthScreen';
import { AdminFleetGpsRadar } from './components/AdminFleetGpsRadar';
import { gpsService, type RiderGpsLocation } from './services/gpsTrackingService';
import { notificationService, type NotificationItem, type ToastItem } from './services/notificationService';
import { NotificationToastContainer } from './components/NotificationToastContainer';
import { NotificationCenterModal } from './components/NotificationCenterModal';
import { AdminOrderStatusManager } from './components/AdminOrderStatusManager';
import { CustomerOrderHistoryView } from './components/CustomerOrderHistoryView';
import { RiderTripHistoryView } from './components/RiderTripHistoryView';
import { ImageUploadModal } from './components/ImageUploadModal';
import { ErrorBoundary } from './components/ErrorBoundary';
import {
  checkOrderPlacementRateLimit,
  sanitizeText,
  sanitizePhone,
  issueRoleSessionToken,
  verifyRoleSessionToken,
} from './services/securityGuard';

type VehicleType = 'BIKE' | 'EV' | 'MINI_TRUCK';
type PaymentMethod =
  | 'UPI_INSTANT_PAY'
  | 'CASH_AT_PICKUP'
  | 'CASH_AT_DROP'
  | 'QUICK_WALLET'
  | 'CASH_ON_DELIVERY'
  | 'GATEWAY_UPI'
  | 'DIRECT_RIDER_UPI'
  | 'CREDIT_CARD';
type AddressTag = 'Home' | 'Office' | 'Other';
type PackageCategory = 'Documents' | 'Food/Tiffin' | 'Electronics' | 'Clothes' | 'Fragile';
type WeightBracket = 'Up to 5kg' | '5-15kg' | '15-20kg';

type OrderStatus =
  | 'PENDING_ASSIGNMENT'
  | 'ASSIGNED'
  | 'ARRIVED_AT_PICKUP'
  | 'PICKED_UP'
  | 'IN_TRANSIT'
  | 'DELIVERED'
  | 'CANCELLED';

interface ChatMessage {
  id: string;
  orderId: string;
  senderRole: 'CUSTOMER' | 'RIDER';
  senderName: string;
  messageText: string;
  timestamp: string;
}

interface DeliveryOrder {
  id: string;
  orderCode: string;
  customerName: string;
  customerPhone: string;
  receiverName?: string;
  receiverPhone?: string;
  pickupAddress: string;
  dropAddress: string;
  pickupTag?: AddressTag;
  dropTag?: AddressTag;
  packageCategory?: PackageCategory;
  weightBracket?: WeightBracket;
  weightKg: number;
  distanceKm: number;
  vehicleType: VehicleType;
  paymentMethod: PaymentMethod;
  paymentStatus: string;
  status: OrderStatus;
  totalAmountPaise: number;
  companyCommissionPaise: number;
  riderEarningPaise: number;
  baseFareInr?: number;
  distanceChargeInr?: number;
  platformFeeInr?: number;
  promoDiscountInr?: number;
  promoCode?: string;
  pickupOtp: string;
  deliveryOtp: string;
  assignedRiderId?: string;
  assignedRiderName?: string;
  assignedRiderRating?: string;
  assignedRiderPlate?: string;
  pickupPhotoCaptured?: boolean;
  codCashCollected?: boolean;
  pickupProofNote?: string;
  deliveryProofNote?: string;
  riderLat?: number;
  riderLng?: number;
  createdAt: string;
  cancellationReason?: string;
}

interface RiderWallet {
  riderId: string;
  riderName: string;
  phone: string;
  balancePaise: number;
  todayNetEarningsPaise: number;
  commissionDebtPaise: number;
  maxCommissionDebtPaise: number;
  shiftStatus: 'ONLINE_ACTIVE' | 'OFFLINE_RESTING';
  batteryPercent: number;
  completedTrips: number;
}

interface LedgerEntry {
  id: string;
  entryType: string;
  referenceId: string;
  amountPaise: number;
  isCredit: boolean;
  settlementStatus: string;
  description: string;
  createdAt: string;
}

const VEHICLE_CATALOG: Record<
  VehicleType,
  {
    label: string;
    subtitle: string;
    capacityBadge: string;
    capacityKg: number;
    baseInr: number;
    perKmInr: number;
    etaMins: string;
  }
> = {
  BIKE: {
    label: '2-Wheeler Scooter/Bike (Instant)',
    subtitle: 'Fastest hyperlocal zip dispatch',
    capacityBadge: 'Up to 20 kg • 40x40cm',
    capacityKg: 20,
    baseInr: 45,
    perKmInr: 12,
    etaMins: '3 mins away',
  },
  EV: {
    label: 'Eco Delivery',
    subtitle: 'Zero-emission electric scooter',
    capacityBadge: 'Up to 15 kg • Eco Saver',
    capacityKg: 20,
    baseInr: 40,
    perKmInr: 10,
    etaMins: '5 mins away',
  },
  MINI_TRUCK: {
    label: 'Mini EV',
    subtitle: 'Enclosed 3W / Mini EV Cargo',
    capacityBadge: 'Up to 20 kg • Fragile Safe',
    capacityKg: 20,
    baseInr: 85,
    perKmInr: 16,
    etaMins: '7 mins away',
  },
};

const SAVED_PICKUP_PRESETS: Record<AddressTag, string> = {
  Home: '',
  Office: '',
  Other: '',
};

const SAVED_DROP_PRESETS: Record<AddressTag, string> = {
  Home: '',
  Office: '',
  Other: '',
};

const PACKAGE_CATEGORIES: PackageCategory[] = [
  'Documents',
  'Food/Tiffin',
  'Electronics',
  'Clothes',
  'Fragile',
];

const WEIGHT_BRACKETS: { label: WeightBracket; defaultKg: string; rangeText: string }[] = [
  { label: 'Up to 5kg', defaultKg: '3.5', rangeText: 'Small / Light' },
  { label: '5-15kg', defaultKg: '10.0', rangeText: 'Medium Parcel' },
  { label: '15-20kg', defaultKg: '18.0', rangeText: 'Heavy (Max 20kg)' },
];

const MAX_WEIGHT_KG = 20;
const MAX_COMMISSION_DEBT_PAISE = 15000; // ₹150.00 strict cap

export default function App() {
  // Dedicated App Mode via build-time VITE_APP_ROLE or URL parameter (?app=customer, ?app=rider, ?app=admin)
  const [dedicatedAppMode, setDedicatedAppMode] = useState<'customer' | 'rider' | 'admin' | null>(() => {
    const envRole = ((import.meta as any).env?.VITE_APP_ROLE || '').toLowerCase().trim();
    if (envRole === 'customer') return 'customer';
    if (envRole === 'rider' || envRole === 'partner') return 'rider';
    if (envRole === 'admin' || envRole === 'enterprise') return 'admin';

    if (typeof window !== 'undefined') {
      const p = new URLSearchParams(window.location.search).get('app')?.toLowerCase();
      if (p === 'customer') return 'customer';
      if (p === 'rider' || p === 'partner') return 'rider';
      if (p === 'admin' || p === 'enterprise') return 'admin';
    }
    return null;
  });

  const [viewMode, setViewMode] = useState<'TABS' | 'SPLIT_ALL'>(() => {
    const envRole = ((import.meta as any).env?.VITE_APP_ROLE || '').toLowerCase().trim();
    if (envRole === 'customer' || envRole === 'rider' || envRole === 'partner' || envRole === 'admin' || envRole === 'enterprise') {
      return 'TABS';
    }
    if (typeof window !== 'undefined') {
      const p = new URLSearchParams(window.location.search).get('app')?.toLowerCase();
      if (p === 'customer' || p === 'rider' || p === 'partner' || p === 'admin' || p === 'enterprise') {
        return 'TABS';
      }
    }
    return 'SPLIT_ALL';
  });

  const [activeTab, setActiveTab] = useState<'CUSTOMER' | 'RIDER' | 'ADMIN'>(() => {
    const envRole = ((import.meta as any).env?.VITE_APP_ROLE || '').toLowerCase().trim();
    if (envRole === 'customer') return 'CUSTOMER';
    if (envRole === 'rider' || envRole === 'partner') return 'RIDER';
    if (envRole === 'admin' || envRole === 'enterprise') return 'ADMIN';

    if (typeof window !== 'undefined') {
      const p = new URLSearchParams(window.location.search).get('app')?.toLowerCase();
      if (p === 'customer') return 'CUSTOMER';
      if (p === 'rider' || p === 'partner') return 'RIDER';
      if (p === 'admin' || p === 'enterprise') return 'ADMIN';
    }
    return 'CUSTOMER';
  });

  const [currentUser, setCurrentUser] = useState<AuthUser | null>(() => {
    try {
      const saved = localStorage.getItem('quick_delivery_auth_user');
      if (!saved) return null;
      const parsed: AuthUser = JSON.parse(saved);
      if (!verifyRoleSessionToken(parsed)) {
        localStorage.removeItem('quick_delivery_auth_user');
        return null;
      }
      const envRole = ((import.meta as any).env?.VITE_APP_ROLE || '').toLowerCase().trim();
      const urlRole = typeof window !== 'undefined' ? new URLSearchParams(window.location.search).get('app')?.toLowerCase() : null;
      const targetMode = envRole || urlRole;
      if (targetMode === 'customer' && parsed.role !== 'CUSTOMER') return null;
      if ((targetMode === 'rider' || targetMode === 'partner') && parsed.role !== 'RIDER') return null;
      if ((targetMode === 'admin' || targetMode === 'enterprise') && parsed.role !== 'ADMIN') return null;
      return parsed;
    } catch {
      return null;
    }
  });

  const handleLoginSuccess = (user: AuthUser) => {
    const savedProfile = dbService.getSavedUserProfile(user.id);
    const baseMerged: AuthUser = savedProfile
      ? { ...user, ...savedProfile, role: user.role }
      : user;
    const signedToken =
      baseMerged.sessionToken ||
      issueRoleSessionToken({
        id: baseMerged.id,
        role: baseMerged.role,
        phone: baseMerged.phone,
        email: baseMerged.email,
      });
    const mergedUser: AuthUser = {
      ...baseMerged,
      name: sanitizeText(baseMerged.name, 80) || baseMerged.name,
      phone: sanitizePhone(baseMerged.phone) || baseMerged.phone,
      sessionToken: signedToken,
    };
    setCurrentUser(mergedUser);
    localStorage.setItem('quick_delivery_auth_user', JSON.stringify(mergedUser));
    dbService.saveUserProfile(mergedUser);

    if (mergedUser.role === 'CUSTOMER') {
      setActiveTab('CUSTOMER');
      setCustomerName(mergedUser.name);
      setCustomerPhone(mergedUser.phone);
    } else if (mergedUser.role === 'RIDER') {
      setActiveTab('RIDER');
      setRiderWallet((prev) => ({
        ...prev,
        riderName: mergedUser.name,
        phone: mergedUser.phone,
      }));
    } else if (mergedUser.role === 'ADMIN') {
      setActiveTab('ADMIN');
    }
  };

  const handleLogout = () => {
    setCurrentUser(null);
    localStorage.removeItem('quick_delivery_auth_user');
  };

  const handleSwitchRole = (role: UserRole) => {
    handleLoginSuccess(DEMO_USERS[role]);
  };

  const [showSeparateAppsModal, setShowSeparateAppsModal] = useState<boolean>(false);
  const [showUpiQrModal, setShowUpiQrModal] = useState<boolean>(false);
  const [showParcelScannerModal, setShowParcelScannerModal] = useState<boolean>(false);
  const [showDatabaseModal, setShowDatabaseModal] = useState<boolean>(false);
  const [showNotificationModal, setShowNotificationModal] = useState<boolean>(false);
  const [unreadNotifCount, setUnreadNotifCount] = useState<number>(() => notificationService.getUnreadCount());
  const [selectedTrackingOrder, setSelectedTrackingOrder] = useState<DeliveryOrder | null>(null);
  const [copiedLinkKey, setCopiedLinkKey] = useState<string | null>(null);
  const [copiedAllLinks, setCopiedAllLinks] = useState<boolean>(false);

  useEffect(() => {
    const unsub = notificationService.subscribe(() => {
      setUnreadNotifCount(notificationService.getUnreadCount());
    });
    return () => unsub();
  }, []);

  // Customer booking form state (fresh & clean, hydrated from logged-in account if Customer)
  const [customerName, setCustomerName] = useState(() =>
    currentUser?.role === 'CUSTOMER' ? currentUser.name : ''
  );
  const [customerPhone, setCustomerPhone] = useState(() =>
    currentUser?.role === 'CUSTOMER' ? currentUser.phone : ''
  );
  const [receiverName, setReceiverName] = useState('');
  const [receiverPhone, setReceiverPhone] = useState('');
  const [pickupTag, setPickupTag] = useState<AddressTag>('Home');
  const [dropTag, setDropTag] = useState<AddressTag>('Office');
  const [pickupAddress, setPickupAddress] = useState('');
  const [dropAddress, setDropAddress] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<PackageCategory>('Documents');
  const [selectedWeightBracket, setSelectedWeightBracket] = useState<WeightBracket>('Up to 5kg');
  const [weightInput, setWeightInput] = useState('1.0');
  const [distanceKm, setDistanceKm] = useState(5.0);
  const [selectedVehicle, setSelectedVehicle] = useState<VehicleType>('BIKE');
  const [selectedPayment, setSelectedPayment] = useState<PaymentMethod>('UPI_INSTANT_PAY');
  const [promoCodeInput, setPromoCodeInput] = useState('');
  const [promoApplied, setPromoApplied] = useState(false);
  const [bookingBanner, setBookingBanner] = useState<string | null>(null);
  const [customerWalletInr, setCustomerWalletInr] = useState<number>(0);
  const [customerSubTab, setCustomerSubTab] = useState<
    'BOOK_ORDER' | 'LIVE_TRACKING' | 'ORDERS_INVOICES' | 'WALLET_SUPPORT'
  >('BOOK_ORDER');
  const [customerBookStep, setCustomerBookStep] = useState<1 | 2>(1);
  const [customerChatModalOpen, setCustomerChatModalOpen] = useState<boolean>(false);

  // Rider state (fresh & clean, hydrated from saved wallet or logged-in account)
  const [riderSubTab, setRiderSubTab] = useState<
    'DUTY_RADAR' | 'ACTIVE_TRIP' | 'TRIP_HISTORY' | 'EARNINGS_WALLET' | 'PROFILE_SUPPORT'
  >('DUTY_RADAR');
  const [riderChatModalOrderId, setRiderChatModalOrderId] = useState<string | null>(null);
  const [riderWallet, setRiderWallet] = useState<RiderWallet>(() => {
    const saved = dbService.getSavedRiderWallet();
    if (saved && typeof saved === 'object') {
      return saved;
    }
    return {
      riderId: 'RIDER-101',
      riderName: currentUser?.role === 'RIDER' ? currentUser.name : 'Rider Partner',
      phone: currentUser?.role === 'RIDER' ? currentUser.phone : '',
      balancePaise: 0,
      todayNetEarningsPaise: 0,
      commissionDebtPaise: 0,
      maxCommissionDebtPaise: MAX_COMMISSION_DEBT_PAISE,
      shiftStatus: 'ONLINE_ACTIVE',
      batteryPercent: 100,
      completedTrips: 0,
    };
  });

  const [otpInputByOrder, setOtpInputByOrder] = useState<Record<string, string>>({});
  const [photoCapturedByOrder, setPhotoCapturedByOrder] = useState<Record<string, boolean>>({});
  const [codCollectedByOrder, setCodCollectedByOrder] = useState<Record<string, boolean>>({});
  const [declinedOrderIds, setDeclinedOrderIds] = useState<string[]>([]);
  const [tripTimerSeconds, setTripTimerSeconds] = useState<number>(252); // 04:12 live trip timer
  const [riderNotice, setRiderNotice] = useState<string | null>(null);

  // Image Upload Modal & Delivery Photo Proof State
  const [imageUploadModalState, setImageUploadModalState] = useState<{
    isOpen: boolean;
    category: 'DELIVERY_PROOF' | 'PROFILE_PHOTO';
    title: string;
    subtitle: string;
    orderId?: string;
    currentImageUrl?: string;
  } | null>(null);

  const [deliveryPhotoByOrder, setDeliveryPhotoByOrder] = useState<
    Record<string, { url: string; note?: string }>
  >({});

  // Real-time GPS Location Broadcasting state
  const [isRiderGpsBroadcasting, setIsRiderGpsBroadcasting] = useState<boolean>(true);
  const [riderCurrentGps, setRiderCurrentGps] = useState<RiderGpsLocation>(() => gpsService.getCurrentLocation());

  useEffect(() => {
    if (isRiderGpsBroadcasting) {
      gpsService.startBroadcasting();
    } else {
      gpsService.stopBroadcasting();
    }

    const unsub = gpsService.subscribe((loc) => {
      setRiderCurrentGps(loc);
    });

    return () => unsub();
  }, [isRiderGpsBroadcasting]);

  // Orders state (starts completely clean; hydrated only from real user-created orders)
  const [orders, setOrders] = useState<DeliveryOrder[]>(() => dbService.getLocalOrders());

  // Live timer tick when any order is IN_TRANSIT or PICKED_UP
  useEffect(() => {
    const hasActiveTrip = orders.some(
      (o) => o.status === 'PICKED_UP' || o.status === 'IN_TRANSIT' || o.status === 'ARRIVED_AT_PICKUP'
    );
    if (!hasActiveTrip) return;
    const interval = setInterval(() => {
      setTripTimerSeconds((prev) => prev + 1);
    }, 1000);
    return () => clearInterval(interval);
  }, [orders]);

  const formatTimer = (secs: number) => {
    const mins = Math.floor(secs / 60);
    const rem = secs % 60;
    return `${String(mins).padStart(2, '0')}:${String(rem).padStart(2, '0')}`;
  };

  // Real-time Chat messages state (starts clean)
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
  const [customerChatDraft, setCustomerChatDraft] = useState('');
  const [riderChatDraft, setRiderChatDraft] = useState('');

  // Company Ledger state (starts clean; populated from real completed deliveries & settlements)
  const [ledger, setLedger] = useState<LedgerEntry[]>([]);

  // --- DYNAMIC APP CONTROL & PLAY STORE UPDATE MANAGER STATE (SYNCED ACROSS ADMIN, CUSTOMER & RIDER) ---
  const [vehiclePricing, setVehiclePricing] = useState<
    Record<VehicleType, { baseInr: number; perKmInr: number }>
  >({
    BIKE: { baseInr: 45, perKmInr: 12 },
    EV: { baseInr: 40, perKmInr: 10 },
    MINI_TRUCK: { baseInr: 85, perKmInr: 16 },
  });
  const [surgeMultiplier, setSurgeMultiplier] = useState<number>(1.0);
  const [platformFeeConfigInr, setPlatformFeeConfigInr] = useState<number>(10);
  const [promoCodesConfig, setPromoCodesConfig] = useState<
    { code: string; discountInr: number; desc: string; active: boolean }[]
  >([
    {
      code: 'QUICK15',
      discountInr: 15,
      desc: 'Flat ₹15 OFF on Instant 2-Wheeler & Mini EV Parcels',
      active: true,
    },
    {
      code: 'FIRST50',
      discountInr: 50,
      desc: 'Flat ₹50 OFF on First Hyperlocal Parcel Delivery',
      active: true,
    },
  ]);
  const [newPromoCodeInput, setNewPromoCodeInput] = useState<string>('');
  const [newPromoDiscountInput, setNewPromoDiscountInput] = useState<string>('25');
  const [serviceAvailable, setServiceAvailable] = useState<boolean>(true);
  const [customerAnnouncement, setCustomerAnnouncement] = useState<string>(
    '⚡ Live Config Sync: Instant 3-min Bike & Mini EV doorstep pickup active across Bengaluru!'
  );
  const [riderAnnouncement, setRiderAnnouncement] = useState<string>(
    '🎯 Rider Partner Bonus: 80% Direct Share + Zero Instant UPI Payout Fee Active Today!'
  );
  const [adminConfigSavedNotice, setAdminConfigSavedNotice] = useState<string | null>(null);

  // Play Store Release & Version Control State
  const [latestCustomerVersion, setLatestCustomerVersion] = useState<string>('1.1.0');
  const [latestRiderVersion, setLatestRiderVersion] = useState<string>('1.1.0');
  const [installedCustomerVersion, setInstalledCustomerVersion] = useState<string>('1.0.0');
  const [installedRiderVersion, setInstalledRiderVersion] = useState<string>('1.0.0');
  const [releaseNotes, setReleaseNotes] = useState<string>(
    '• Faster 2-Step Parcel Booking & Live GPS Radar\n• Real-Time Dynamic Surge & Promo Coupon Sync\n• Instant 80% Rider UPI Payouts & GST Invoice Download'
  );
  const [updateMode, setUpdateMode] = useState<'NONE' | 'FLEXIBLE' | 'FORCE'>('FLEXIBLE');
  const [customerDismissedFlexible, setCustomerDismissedFlexible] = useState<boolean>(false);
  const [riderDismissedFlexible, setRiderDismissedFlexible] = useState<boolean>(false);
  const [playStorePublishedStamp, setPlayStorePublishedStamp] = useState<string>(
    'Live on Google Play Track • v1.1.0'
  );

  // --- DATABASE & REAL-TIME FIRESTORE/LOCAL SYNC LISTENER ---
  useEffect(() => {
    // 1. Hydrate existing real user orders from local storage (never seed demo orders)
    const local = dbService.getLocalOrders();
    setOrders(local);

    // 2. Subscribe to real-time updates (cross-tab storage and cloud Firestore)
    const unsubscribe = dbService.subscribeToOrders((updatedOrders) => {
      if (Array.isArray(updatedOrders)) {
        setOrders(updatedOrders);
      }
    });

    return () => unsubscribe();
  }, []);

  // Automatic background sync of all order, profile, and wallet data using logged-in account credentials
  useEffect(() => {
    if (!currentUser) return;
    const syncedUser: AuthUser =
      currentUser.role === 'CUSTOMER'
        ? { ...currentUser, name: customerName || currentUser.name, phone: customerPhone || currentUser.phone }
        : currentUser.role === 'RIDER'
        ? { ...currentUser, name: riderWallet.riderName || currentUser.name, phone: riderWallet.phone || currentUser.phone }
        : currentUser;
    dbService.syncAccountSessionInBackground(syncedUser, orders, riderWallet);
  }, [currentUser, orders, riderWallet, customerName, customerPhone]);

  // --- PWA INSTALLATION HOOKS & CONTROLS ---
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [deferredInstallPrompt, setDeferredInstallPrompt] = useState<any>(null);
  const [isAppInstalled, setIsAppInstalled] = useState<boolean>(false);
  const [showInstallGuideModal, setShowInstallGuideModal] = useState<boolean>(false);

  useEffect(() => {
    // Check if app is already running standalone
    const isStandalone =
      window.matchMedia('(display-mode: standalone)').matches ||
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      (window.navigator as any).standalone === true;
    if (isStandalone) {
      setIsAppInstalled(true);
    }

    const handleBeforeInstallPrompt = (e: Event) => {
      e.preventDefault();
      setDeferredInstallPrompt(e);
      console.log('[PWA] beforeinstallprompt event captured');
    };

    const handleAppInstalled = () => {
      setIsAppInstalled(true);
      setDeferredInstallPrompt(null);
      setShowInstallGuideModal(false);
      console.log('[PWA] App successfully installed!');
    };

    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    window.addEventListener('appinstalled', handleAppInstalled);

    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
      window.removeEventListener('appinstalled', handleAppInstalled);
    };
  }, []);

  const handleTriggerInstall = async () => {
    if (deferredInstallPrompt) {
      try {
        await deferredInstallPrompt.prompt();
        const choice = await deferredInstallPrompt.userChoice;
        if (choice && choice.outcome === 'accepted') {
          console.log('[PWA] User accepted installation prompt');
          setIsAppInstalled(true);
        }
        setDeferredInstallPrompt(null);
      } catch (err) {
        console.error('[PWA] Error triggering install prompt:', err);
        setShowInstallGuideModal(true);
      }
    } else {
      setShowInstallGuideModal(true);
    }
  };

  // --- SPA NAVIGATION HISTORY STACK (SMOOTH BACK-BUTTON POP WITHOUT FULL RELOAD) ---
  interface QdNavSnapshot {
    dedicatedAppMode: 'customer' | 'rider' | 'admin' | null;
    viewMode: 'TABS' | 'SPLIT_ALL';
    activeTab: 'CUSTOMER' | 'RIDER' | 'ADMIN';
    customerSubTab: 'BOOK_ORDER' | 'LIVE_TRACKING' | 'ORDERS_INVOICES' | 'WALLET_SUPPORT';
    customerBookStep: 1 | 2;
    riderSubTab: 'DUTY_RADAR' | 'ACTIVE_TRIP' | 'TRIP_HISTORY' | 'EARNINGS_WALLET' | 'PROFILE_SUPPORT';
  }

  const navHistoryStackRef = useRef<QdNavSnapshot[]>([]);
  const isPoppingHistoryRef = useRef<boolean>(false);
  const hasInitializedHistoryRef = useRef<boolean>(false);

  const activeModalStateRef = useRef({
    showInstallGuideModal: false,
    imageUploadOpen: false,
    riderChatOpen: false,
    customerChatOpen: false,
    trackingModalOpen: false,
    notificationModalOpen: false,
    databaseModalOpen: false,
    parcelScannerModalOpen: false,
    upiQrModalOpen: false,
    separateAppsModalOpen: false,
  });

  useEffect(() => {
    activeModalStateRef.current = {
      showInstallGuideModal,
      imageUploadOpen: Boolean(imageUploadModalState?.isOpen),
      riderChatOpen: Boolean(riderChatModalOrderId),
      customerChatOpen: customerChatModalOpen,
      trackingModalOpen: Boolean(selectedTrackingOrder),
      notificationModalOpen: showNotificationModal,
      databaseModalOpen: showDatabaseModal,
      parcelScannerModalOpen: showParcelScannerModal,
      upiQrModalOpen: showUpiQrModal,
      separateAppsModalOpen: showSeparateAppsModal,
    };
  }, [
    showInstallGuideModal,
    imageUploadModalState,
    riderChatModalOrderId,
    customerChatModalOpen,
    selectedTrackingOrder,
    showNotificationModal,
    showDatabaseModal,
    showParcelScannerModal,
    showUpiQrModal,
    showSeparateAppsModal,
  ]);

  // Push history entry whenever a modal opens so hardware/browser Back closes the modal first
  const prevAnyModalOpenRef = useRef<boolean>(false);
  useEffect(() => {
    const anyModalOpen =
      showInstallGuideModal ||
      Boolean(imageUploadModalState?.isOpen) ||
      Boolean(riderChatModalOrderId) ||
      customerChatModalOpen ||
      Boolean(selectedTrackingOrder) ||
      showNotificationModal ||
      showDatabaseModal ||
      showParcelScannerModal ||
      showUpiQrModal ||
      showSeparateAppsModal;

    if (anyModalOpen && !prevAnyModalOpenRef.current && !isPoppingHistoryRef.current) {
      window.history.pushState({ qdModalGuard: true }, '');
    }
    prevAnyModalOpenRef.current = anyModalOpen;
  }, [
    showInstallGuideModal,
    imageUploadModalState,
    riderChatModalOrderId,
    customerChatModalOpen,
    selectedTrackingOrder,
    showNotificationModal,
    showDatabaseModal,
    showParcelScannerModal,
    showUpiQrModal,
    showSeparateAppsModal,
  ]);

  // Record screen/tab/step transitions in history stack so Back pops smoothly without reloading
  useEffect(() => {
    const currentSnapshot: QdNavSnapshot = {
      dedicatedAppMode,
      viewMode,
      activeTab,
      customerSubTab,
      customerBookStep,
      riderSubTab,
    };

    if (!hasInitializedHistoryRef.current) {
      hasInitializedHistoryRef.current = true;
      navHistoryStackRef.current = [currentSnapshot];
      window.history.replaceState({ qdRootGuard: true, snapshot: currentSnapshot }, '');
      window.history.pushState({ qdNavIndex: 0, snapshot: currentSnapshot }, '');
      return;
    }

    if (isPoppingHistoryRef.current) {
      isPoppingHistoryRef.current = false;
      return;
    }

    const last = navHistoryStackRef.current[navHistoryStackRef.current.length - 1];
    const isSame =
      last &&
      last.dedicatedAppMode === currentSnapshot.dedicatedAppMode &&
      last.viewMode === currentSnapshot.viewMode &&
      last.activeTab === currentSnapshot.activeTab &&
      last.customerSubTab === currentSnapshot.customerSubTab &&
      last.customerBookStep === currentSnapshot.customerBookStep &&
      last.riderSubTab === currentSnapshot.riderSubTab;

    if (!isSame) {
      navHistoryStackRef.current.push(currentSnapshot);
      window.history.pushState(
        { qdNavIndex: navHistoryStackRef.current.length - 1, snapshot: currentSnapshot },
        ''
      );
    }
  }, [dedicatedAppMode, viewMode, activeTab, customerSubTab, customerBookStep, riderSubTab]);

  useEffect(() => {
    const applyTitleForMode = (mode: 'customer' | 'rider' | 'admin' | null) => {
      if (mode === 'customer') {
        document.title = 'Quick Delivery';
      } else if (mode === 'rider') {
        document.title = 'Quick Delivery Partner';
      } else if (mode === 'admin') {
        document.title = 'QD Admin Enterprise';
      } else {
        document.title = 'Quick Delivery — Customer, Rider & Admin Unified Hub';
      }
    };

    const p = new URLSearchParams(window.location.search).get('app')?.toLowerCase();
    if (p === 'customer') {
      applyTitleForMode('customer');
    } else if (p === 'rider' || p === 'partner') {
      applyTitleForMode('rider');
    } else if (p === 'admin' || p === 'enterprise') {
      applyTitleForMode('admin');
    } else {
      applyTitleForMode(null);
    }

    const handlePopState = () => {
      const modals = activeModalStateRef.current;
      // 1. If any modal/overlay is open, close it in LIFO order without leaving the screen
      if (modals.showInstallGuideModal) {
        isPoppingHistoryRef.current = true;
        setShowInstallGuideModal(false);
        return;
      }
      if (modals.imageUploadOpen) {
        isPoppingHistoryRef.current = true;
        setImageUploadModalState(null);
        return;
      }
      if (modals.riderChatOpen) {
        isPoppingHistoryRef.current = true;
        setRiderChatModalOrderId(null);
        return;
      }
      if (modals.customerChatOpen) {
        isPoppingHistoryRef.current = true;
        setCustomerChatModalOpen(false);
        return;
      }
      if (modals.trackingModalOpen) {
        isPoppingHistoryRef.current = true;
        setSelectedTrackingOrder(null);
        return;
      }
      if (modals.notificationModalOpen) {
        isPoppingHistoryRef.current = true;
        setShowNotificationModal(false);
        return;
      }
      if (modals.databaseModalOpen) {
        isPoppingHistoryRef.current = true;
        setShowDatabaseModal(false);
        return;
      }
      if (modals.parcelScannerModalOpen) {
        isPoppingHistoryRef.current = true;
        setShowParcelScannerModal(false);
        return;
      }
      if (modals.upiQrModalOpen) {
        isPoppingHistoryRef.current = true;
        setShowUpiQrModal(false);
        return;
      }
      if (modals.separateAppsModalOpen) {
        isPoppingHistoryRef.current = true;
        setShowSeparateAppsModal(false);
        return;
      }

      // 2. Pop the internal navigation history stack smoothly
      if (navHistoryStackRef.current.length > 1) {
        navHistoryStackRef.current.pop();
        const prev = navHistoryStackRef.current[navHistoryStackRef.current.length - 1];
        if (prev) {
          isPoppingHistoryRef.current = true;
          setDedicatedAppMode(prev.dedicatedAppMode);
          setViewMode(prev.viewMode);
          setActiveTab(prev.activeTab);
          setCustomerSubTab(prev.customerSubTab);
          setCustomerBookStep(prev.customerBookStep);
          setRiderSubTab(prev.riderSubTab);
          applyTitleForMode(prev.dedicatedAppMode);
          return;
        }
      }

      // 3. Root fallback guard: keep user inside the app and re-arm history state so it never reloads
      const rootSnapshot = navHistoryStackRef.current[0];
      if (rootSnapshot) {
        isPoppingHistoryRef.current = true;
        if (rootSnapshot.customerBookStep !== 1) {
          setCustomerBookStep(1);
        } else if (rootSnapshot.customerSubTab !== 'BOOK_ORDER') {
          setCustomerSubTab('BOOK_ORDER');
        } else if (rootSnapshot.riderSubTab !== 'DUTY_RADAR') {
          setRiderSubTab('DUTY_RADAR');
        }
        window.history.pushState({ qdNavIndex: 0, snapshot: rootSnapshot }, '');
      }
    };

    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  const getInstallButtonLabel = () => {
    if (isAppInstalled) {
      if (dedicatedAppMode === 'customer') return 'Customer App Installed ✓';
      if (dedicatedAppMode === 'rider') return 'Rider App Installed ✓';
      if (dedicatedAppMode === 'admin') return 'Admin App Installed ✓';
      return 'App Installed ✓';
    }
    if (dedicatedAppMode === 'customer') return 'Install Customer App';
    if (dedicatedAppMode === 'rider') return 'Install Rider App';
    if (dedicatedAppMode === 'admin') return 'Install Admin App';
    return 'Install Quick Delivery App';
  };

  const handleCopyLink = (key: string, url: string) => {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(url).then(() => {
        setCopiedLinkKey(key);
        setTimeout(() => setCopiedLinkKey(null), 2500);
      });
    } else {
      window.prompt('Copy this dedicated app URL:', url);
    }
  };

  const handleCopyAllLinks = () => {
    const origin = typeof window !== 'undefined' ? window.location.origin : '';
    const text = `🚀 Quick Delivery — 3 Dedicated Standalone Mobile Apps:

1. 🛍️ Customer App (Book Parcels, Live OTP & 5-Step Tracking):
${origin}/?app=customer

2. 🛵 Rider Partner App (On/Off Duty, Dispatch Radar & 80% Net Share):
${origin}/?app=rider

3. 💼 Admin Enterprise App (Fleet Radar, 20% Commission & Live Pricing):
${origin}/?app=admin

📱 How to install on 3 separate phones:
Open each link in mobile Chrome on that specific phone and tap 'Install App' or Chrome menu (⋮) -> 'Add to Home screen' to install as 3 separate apps!`;

    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).then(() => {
        setCopiedAllLinks(true);
        setTimeout(() => setCopiedAllLinks(false), 3000);
      });
    } else {
      window.prompt('Copy all 3 app URLs:', text);
    }
  };

  // Fare calculation with Dynamic Base Fare + Per Km + Surge Multiplier + Platform Fee - Promo Code Discount
  const parsedWeight = parseFloat(weightInput) || 0;
  const isOverWeight = parsedWeight <= 0 || parsedWeight > MAX_WEIGHT_KG;

  const getVehicleLabel = (v?: string) =>
    (VEHICLE_CATALOG[(v as VehicleType) || 'BIKE'] || VEHICLE_CATALOG.BIKE).label;

  const fareQuote = useMemo(() => {
    const safeDistanceKm = Number.isFinite(Number(distanceKm)) && Number(distanceKm) > 0 ? Number(distanceKm) : 6.4;
    const dynamicSpec =
      vehiclePricing[selectedVehicle] ||
      VEHICLE_CATALOG[selectedVehicle] ||
      VEHICLE_CATALOG.BIKE;
    const baseFareInr = dynamicSpec.baseInr;
    const rawDistanceInr = Math.round(dynamicSpec.perKmInr * safeDistanceKm);
    const surgeExtraInr = Math.round((baseFareInr + rawDistanceInr) * Math.max(0, surgeMultiplier - 1));
    const distanceChargeInr = rawDistanceInr + surgeExtraInr;
    const platformFeeInr = platformFeeConfigInr;

    const normalizedPromo = promoCodeInput.trim().toUpperCase();
    const matchedPromo = promoCodesConfig.find(
      (p) => p.active && p.code.toUpperCase() === normalizedPromo
    );
    const promoDiscountInr =
      promoApplied && normalizedPromo.length >= 3
        ? matchedPromo
          ? matchedPromo.discountInr
          : 15
        : 0;

    const totalInr = Math.max(
      35,
      baseFareInr + distanceChargeInr + platformFeeInr - promoDiscountInr
    );
    const totalPaise = totalInr * 100;
    const commissionPaise = Math.round(totalPaise * 0.2);
    const riderPaise = totalPaise - commissionPaise;
    return {
      distanceKm: safeDistanceKm,
      baseFareInr,
      rawDistanceInr,
      surgeExtraInr,
      distanceChargeInr,
      platformFeeInr,
      promoDiscountInr,
      totalInr,
      totalPaise,
      commissionPaise,
      riderPaise,
    };
  }, [
    selectedVehicle,
    distanceKm,
    promoApplied,
    promoCodeInput,
    vehiclePricing,
    surgeMultiplier,
    platformFeeConfigInr,
    promoCodesConfig,
  ]);

  const handleSelectPickupTag = (tag: AddressTag) => {
    setPickupTag(tag);
    if (SAVED_PICKUP_PRESETS[tag]) {
      setPickupAddress(SAVED_PICKUP_PRESETS[tag]);
    }
  };

  const handleSelectDropTag = (tag: AddressTag) => {
    setDropTag(tag);
    if (SAVED_DROP_PRESETS[tag]) {
      setDropAddress(SAVED_DROP_PRESETS[tag]);
    }
  };

  const handleSelectWeightBracket = (bracket: WeightBracket, defaultKg: string) => {
    setSelectedWeightBracket(bracket);
    setWeightInput(defaultKg);
  };

  const handleCreateOrder = (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (isOverWeight) {
        setBookingBanner('Maximum allowed parcel weight is 20 kg. Parcels above 20 kg cannot be booked.');
        return;
      }
      const cleanPickup = sanitizeText(pickupAddress, 300) || 'Pickup Location';
      const cleanDrop = sanitizeText(dropAddress, 300) || 'Drop Location';
      const cleanCustName = sanitizeText(customerName, 80) || 'Customer';
      const cleanCustPhone = sanitizePhone(customerPhone) || '9876543210';
      const cleanRecvName = sanitizeText(receiverName, 80) || 'Receiver';
      const cleanRecvPhone = sanitizePhone(receiverPhone) || '9820112233';

      const activePendingCount = orders.filter(
        (o) => o.status === 'PENDING_ASSIGNMENT'
      ).length;
      const rateLimitCheck = checkOrderPlacementRateLimit({
        userId: currentUser?.id || cleanCustPhone,
        pickupAddress: cleanPickup,
        dropAddress: cleanDrop,
        vehicleType: selectedVehicle || 'BIKE',
        activePendingCount,
      });
      if (!rateLimitCheck.allowed) {
        setBookingBanner(`🛡️ Security Guard: ${rateLimitCheck.reason}`);
        return;
      }

      const randomNum = Math.floor(5002 + Math.random() * 4000);
      const code = `QD-${randomNum}`;
      const newId = `ord_${code.toLowerCase()}`;
      const pickupOtp = String(Math.floor(1000 + Math.random() * 9000));
      const deliveryOtp = String(Math.floor(1000 + Math.random() * 9000));

      const isCashMethod =
        selectedPayment === 'CASH_AT_PICKUP' ||
        selectedPayment === 'CASH_AT_DROP' ||
        selectedPayment === 'CASH_ON_DELIVERY';

      const paymentStatus = isCashMethod
        ? 'CASH_COLLECT_ON_IVERY'
            .replace('CASH_COLLECT_ON_IVERY', 'CASH_COLLECT_ON_DELIVERY')
        : selectedPayment === 'QUICK_WALLET'
        ? 'VERIFIED_PAID_TO_COMPANY'
        : 'PENDING_GATEWAY_VERIFICATION';

      if (selectedPayment === 'QUICK_WALLET') {
        setCustomerWalletInr((prev) => Math.max(0, prev - fareQuote.totalInr));
      }

      const newOrder: DeliveryOrder = {
        id: newId,
        orderCode: code,
        customerName: cleanCustName,
        customerPhone: cleanCustPhone,
        receiverName: cleanRecvName,
        receiverPhone: cleanRecvPhone,
        pickupAddress: cleanPickup,
        dropAddress: cleanDrop,
        pickupTag: pickupTag || 'Home',
        dropTag: dropTag || 'Office',
        packageCategory: selectedCategory || 'Documents',
        weightBracket: selectedWeightBracket || 'Up to 5kg',
        weightKg: parsedWeight > 0 ? parsedWeight : 1,
        distanceKm: fareQuote.distanceKm,
        vehicleType: selectedVehicle || 'BIKE',
        paymentMethod: selectedPayment || 'CASH_ON_DELIVERY',
        paymentStatus,
        status: 'PENDING_ASSIGNMENT',
        totalAmountPaise: fareQuote.totalPaise,
        companyCommissionPaise: fareQuote.commissionPaise,
        riderEarningPaise: fareQuote.riderPaise,
        baseFareInr: fareQuote.baseFareInr,
        distanceChargeInr: fareQuote.distanceChargeInr,
        platformFeeInr: fareQuote.platformFeeInr,
        promoDiscountInr: fareQuote.promoDiscountInr,
        ...(promoApplied && promoCodeInput.trim() ? { promoCode: sanitizeText(promoCodeInput, 32) } : {}),
        pickupOtp,
        deliveryOtp,
        assignedRiderRating: '4.9★',
        assignedRiderPlate: 'KA-01-EQ-4821',
        createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };

      setOrders((prev) => [newOrder, ...(Array.isArray(prev) ? prev : [])]);
      setSelectedTrackingOrder(null);
      setCustomerBookStep(1);
      setCustomerSubTab('LIVE_TRACKING');
      setBookingBanner(
        `Order ${code} booked (${selectedCategory}, ${selectedWeightBracket})! Automatically switched to Live Tracking.`
      );

      Promise.resolve(dbService.saveOrder(newOrder)).catch((err) => {
        console.warn('Order saved locally; background cloud sync deferred:', err);
      });

      // Real-time Push Notification to Customer
      try {
        notificationService.sendNotification({
          title: '📦 Order Placed & Confirmed!',
          message: `Order #${code} booked (${pickupTag} → ${dropTag}). Searching for nearby rider partner...`,
          role: 'CUSTOMER',
          type: 'ORDER_PLACED',
          orderCode: code,
          soundType: 'STATUS_UPDATE',
          actionLabel: 'Track GPS',
        });

        // Real-time Dispatch Alert to Riders
        notificationService.sendNotification({
          title: '🔔 New Dispatch Available!',
          message: `Pickup at ${cleanPickup} (${fareQuote.distanceKm} km • ₹${(fareQuote.riderPaise / 100).toFixed(0)} payout). Tap to accept!`,
          role: 'RIDER',
          type: 'DISPATCH_NEW',
          orderCode: code,
          soundType: 'ORDER_DISPATCH',
          actionLabel: 'Accept Trip',
        });
      } catch (notifErr) {
        console.warn('Notification dispatch non-fatal error:', notifErr);
      }
    } catch (err) {
      console.error('Error while creating order:', err);
      setBookingBanner('Order confirmed with fallback mode. Switching to Live Tracking.');
      setCustomerBookStep(1);
      setCustomerSubTab('LIVE_TRACKING');
    }
  };

  const handleDownloadGstInvoice = (order: DeliveryOrder) => {
    const totalInr = (order.totalAmountPaise / 100).toFixed(2);
    const taxableInr = ((order.totalAmountPaise / 100) / 1.18).toFixed(2);
    const cgstInr = (((order.totalAmountPaise / 100) - parseFloat(taxableInr)) / 2).toFixed(2);
    const sgstInr = cgstInr;
    const invoiceLines = [
      '====================================================',
      '        QUICK DELIVERY — TAX / GST INVOICE          ',
      '   GSTIN: 29AABCQ9421K1Z5 • SAC Code: 996812        ',
      '====================================================',
      `Invoice No       : INV-${order.orderCode}`,
      `Order ID         : ${order.orderCode}`,
      `Order Status     : ${order.status}`,
      `Time             : ${order.createdAt}`,
      '----------------------------------------------------',
      `Sender           : ${order.customerName} (+91 ${order.customerPhone})`,
      `Pickup (${order.pickupTag || 'Home'})    : ${order.pickupAddress}`,
      `Receiver         : ${order.receiverName || 'Receiver'} (+91 ${order.receiverPhone || '9820112233'})`,
      `Drop (${order.dropTag || 'Office'})      : ${order.dropAddress}`,
      '----------------------------------------------------',
      `Package Details  : ${order.packageCategory || 'Parcel'} • ${order.weightKg} kg (${order.weightBracket || 'Up to 5kg'})`,
      `Vehicle Fleet    : ${getVehicleLabel(order.vehicleType)} (${order.distanceKm} km)`,
      `Rider Partner    : ${order.assignedRiderName || 'Vikram Singh'} (${order.assignedRiderPlate || 'KA-01-EQ-4821'})`,
      `Payment Method   : ${(order.paymentMethod || 'CASH_ON_DELIVERY').replace(/_/g, ' ')}`,
      '----------------------------------------------------',
      `Taxable Freight  : INR ${taxableInr}`,
      `CGST (9%)        : INR ${cgstInr}`,
      `SGST (9%)        : INR ${sgstInr}`,
      `Total GST Fare   : INR ${totalInr}`,
      '----------------------------------------------------',
      `80% Rider Share  : INR ${(order.riderEarningPaise / 100).toFixed(2)}`,
      `20% Platform Fee : INR ${(order.companyCommissionPaise / 100).toFixed(2)}`,
      '====================================================',
    ].join('\n');

    const blob = new Blob([invoiceLines], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `QuickDelivery_GST_Invoice_${order.orderCode}.txt`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    setBookingBanner(`Downloaded official GST Invoice for Order #${order.orderCode}!`);
  };

  // Comprehensive Order Status Management Handlers (Admin, Customer, Rider)
  const handleAdminUpdateOrderStatus = (
    orderId: string,
    newStatus: OrderStatus,
    details?: {
      assignedRiderName?: string;
      assignedRiderId?: string;
      note?: string;
      cancellationReason?: string;
    }
  ) => {
    const target = orders.find((o) => o.id === orderId);
    if (!target) return;

    const prevStatus = target.status;
    const updatedOrder: DeliveryOrder = {
      ...target,
      status: newStatus,
      assignedRiderName:
        details?.assignedRiderName !== undefined ? details.assignedRiderName : target.assignedRiderName,
      assignedRiderId:
        details?.assignedRiderId !== undefined ? details.assignedRiderId : target.assignedRiderId,
      cancellationReason: details?.cancellationReason || target.cancellationReason,
      deliveryProofNote: details?.note || target.deliveryProofNote,
    };

    // If marked as DELIVERED, credit 80% to rider wallet
    if (newStatus === 'DELIVERED' && prevStatus !== 'DELIVERED') {
      const riderEarn = target.riderEarningPaise;
      setRiderWallet((prev) => ({
        ...prev,
        balancePaise: prev.balancePaise + riderEarn,
        todayNetEarningsPaise: prev.todayNetEarningsPaise + riderEarn,
        completedTrips: prev.completedTrips + 1,
      }));

      if (target.paymentMethod.toLowerCase().includes('cash')) {
        updatedOrder.codCashCollected = true;
      }
    }

    setOrders((prev) => prev.map((o) => (o.id === orderId ? updatedOrder : o)));
    dbService.saveOrder(updatedOrder);

    // Push notification alerts
    let notifTitle = `Order #${target.orderCode} Status Updated`;
    let notifMsg = `Order #${target.orderCode} is now ${newStatus.replace(/_/g, ' ')}`;
    let notifType: any = 'ORDER_ASSIGNED';

    if (newStatus === 'DELIVERED') {
      notifTitle = `🎉 Order #${target.orderCode} Delivered!`;
      notifMsg = `Parcel delivered safely to ${target.dropAddress}. Verified via Delivery OTP.`;
      notifType = 'ORDER_DELIVERED';
    } else if (newStatus === 'CANCELLED') {
      notifTitle = `⚠️ Order #${target.orderCode} Cancelled`;
      notifMsg = `Order cancelled. Reason: ${details?.cancellationReason || 'Admin or customer cancelled'}`;
      notifType = 'SYSTEM_ALERT';
    } else if (newStatus === 'IN_TRANSIT') {
      notifTitle = `🛵 Order #${target.orderCode} Out for Delivery`;
      notifMsg = `Rider ${updatedOrder.assignedRiderName || 'Partner'} is en route to drop location.`;
      notifType = 'ORDER_PICKED_UP';
    } else if (newStatus === 'ASSIGNED') {
      notifTitle = `📋 Rider Assigned to #${target.orderCode}`;
      notifMsg = `Assigned to ${updatedOrder.assignedRiderName || 'Rider'}. En route to pickup.`;
      notifType = 'ORDER_ASSIGNED';
    } else if (newStatus === 'PENDING_ASSIGNMENT') {
      notifTitle = `⏳ Order #${target.orderCode} Pending Assignment`;
      notifMsg = `Order in queue searching for nearby fleet riders.`;
      notifType = 'SYSTEM_ALERT';
    }

    notificationService.sendNotification({
      title: notifTitle,
      message: notifMsg,
      role: 'ALL',
      type: notifType,
      orderCode: target.orderCode,
      orderId: target.id,
    });
  };

  const handleAdminBulkUpdateStatus = (orderIds: string[], newStatus: OrderStatus) => {
    orderIds.forEach((id) => handleAdminUpdateOrderStatus(id, newStatus));
  };

  const handleCustomerCancelOrder = (orderId: string, reason: string) => {
    handleAdminUpdateOrderStatus(orderId, 'CANCELLED', { cancellationReason: reason });
    setBookingBanner(`⚠️ Order #${orders.find((o) => o.id === orderId)?.orderCode || ''} was cancelled.`);
  };

  const handleReOrder = (pastOrder: DeliveryOrder) => {
    setPickupAddress(pastOrder.pickupAddress);
    setDropAddress(pastOrder.dropAddress);
    if (pastOrder.weightBracket) {
      setSelectedWeightBracket(pastOrder.weightBracket);
    }
    setSelectedVehicle(pastOrder.vehicleType);
    setCustomerSubTab('BOOK_ORDER');
    setCustomerBookStep(1);
    setBookingBanner(`✨ Loaded details from Order #${pastOrder.orderCode}. Ready to book!`);
  };

  const handleUpdateProfilePhoto = (newAvatarUrl: string) => {
    if (!currentUser) return;
    const updatedUser: AuthUser = {
      ...currentUser,
      avatarUrl: newAvatarUrl,
    };
    setCurrentUser(updatedUser);
    localStorage.setItem('quick_delivery_auth_user', JSON.stringify(updatedUser));
    dbService.saveUserProfile(updatedUser);
    setBookingBanner(`✅ Profile photo updated successfully!`);
    notificationService.sendNotification({
      title: '📸 Profile Photo Updated',
      message: 'Your profile avatar has been updated and synchronized.',
      role: currentUser.role,
      type: 'SYSTEM_ALERT',
    });
  };

  const handleSelectDeliveryPhoto = (orderId: string, photoUrl: string, note?: string) => {
    setDeliveryPhotoByOrder((prev) => ({
      ...prev,
      [orderId]: { url: photoUrl, note },
    }));
    setRiderNotice(`📸 Delivery proof photo attached! Enter 4-digit Drop OTP to finalize delivery.`);
  };

  const handleToggleRiderOnline = () => {
    setRiderWallet((prev) => {
      const nextStatus = prev.shiftStatus === 'ONLINE_ACTIVE' ? 'OFFLINE_RESTING' : 'ONLINE_ACTIVE';
      setRiderNotice(
        nextStatus === 'ONLINE_ACTIVE'
          ? 'You are now ON DUTY • Live GPS Active & eligible for instant order dispatch.'
          : 'You are now OFF DUTY. Switch to ON DUTY to receive new delivery pings.'
      );
      return { ...prev, shiftStatus: nextStatus };
    });
  };

  const handleInstantUpiPayout = () => {
    if (riderWallet.balancePaise <= 0) {
      setRiderNotice('Wallet balance is ₹0. Complete deliveries to withdraw earnings.');
      return;
    }
    const payoutInr = (riderWallet.balancePaise / 100).toFixed(2);
    const ts = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    setLedger((prev) => [
      {
        id: `led_payout_${Date.now()}`,
        entryType: 'RIDER_INSTANT_UPI_PAYOUT',
        referenceId: riderWallet.riderId,
        amountPaise: riderWallet.balancePaise,
        isCredit: false,
        settlementStatus: 'SETTLED_TO_RIDER_UPI',
        description: `Instant UPI Payout of ₹${payoutInr} transferred to ${riderWallet.riderName} (vikram@okaxis)`,
        createdAt: ts,
      },
      ...prev,
    ]);
    setRiderWallet((prev) => ({ ...prev, balancePaise: 0 }));
    setRiderNotice(`Instant UPI Payout of ₹${payoutInr} credited to your linked bank account!`);
  };

  const handleClaimOrder = (order: DeliveryOrder) => {
    if (riderWallet.shiftStatus !== 'ONLINE_ACTIVE') {
      setRiderNotice('Switch the top header toggle to ON DUTY before accepting orders.');
      return;
    }
    if (riderWallet.commissionDebtPaise > MAX_COMMISSION_DEBT_PAISE) {
      setRiderNotice(
        'Order assignment blocked: Cash-in-Hand COD limit exceeds ₹150.00. Please settle COD dues first.'
      );
      return;
    }
    setOrders((prev) =>
      prev.map((o) =>
        o.id === order.id
          ? {
              ...o,
              status: 'ARRIVED_AT_PICKUP',
              assignedRiderId: riderWallet.riderId,
              assignedRiderName: riderWallet.riderName,
              assignedRiderRating: '4.9★',
              assignedRiderPlate: 'KA-01-EQ-4821',
              riderLat: 12.9784,
              riderLng: 77.6408,
            }
          : o
      )
    );
    setRiderSubTab('ACTIVE_TRIP');
    setRiderNotice(
      `Order ${order.orderCode} accepted! Switched to Active Trip • Step 1 (At Pickup): Enter Customer's 4-digit Pickup OTP (${order.pickupOtp}) & Capture Parcel Photo.`
    );

    // Customer Alert: Rider Assigned
    notificationService.sendNotification({
      title: '🛵 Rider Assigned!',
      message: `${riderWallet.riderName} (KA-01-EQ-4821) accepted #${order.orderCode}. Share 4-digit Pickup OTP ${order.pickupOtp} at doorstep.`,
      role: 'CUSTOMER',
      type: 'ORDER_ASSIGNED',
      orderCode: order.orderCode,
      soundType: 'STATUS_UPDATE',
      actionLabel: 'Track GPS',
    });

    // Rider Alert: Trip Assigned
    notificationService.sendNotification({
      title: '🏍️ Trip Accepted!',
      message: `Navigating to pickup for #${order.orderCode} (${order.pickupAddress}). Request OTP ${order.pickupOtp}.`,
      role: 'RIDER',
      type: 'ORDER_ASSIGNED',
      orderCode: order.orderCode,
      soundType: 'ORDER_DISPATCH',
      actionLabel: 'Active Trip',
    });
  };

  const handleDeclineOrder = (order: DeliveryOrder) => {
    setDeclinedOrderIds((prev) => [...prev, order.id]);
    setRiderNotice(`Order ${order.orderCode} declined and rotated to next nearby Rider Partner.`);
  };

  const handleCaptureParcelPhoto = (order: DeliveryOrder) => {
    const ts = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    setPhotoCapturedByOrder((prev) => ({ ...prev, [order.id]: true }));
    setOrders((prev) =>
      prev.map((o) =>
        o.id === order.id
          ? {
              ...o,
              pickupPhotoCaptured: true,
              pickupProofNote: `Parcel Photo Proof Captured (HD Seal Verified @ ${ts})`,
            }
          : o
      )
    );
    setRiderNotice(`Parcel photo proof captured for ${order.orderCode}! Now verify Pickup OTP (${order.pickupOtp}) to start trip.`);
  };

  const handleReleaseOrderBeforePickup = (order: DeliveryOrder) => {
    setOrders((prev) =>
      prev.map((o) =>
        o.id === order.id
          ? {
              ...o,
              status: 'PENDING_ASSIGNMENT',
              assignedRiderId: undefined,
              assignedRiderName: undefined,
            }
          : o
      )
    );
    setRiderSubTab('DUTY_RADAR');
    setRiderNotice(`Order ${order.orderCode} released back to the open pool for fair rotation.`);
  };

  const handleAdvanceRiderOrder = (order: DeliveryOrder) => {
    const enteredOtp = (otpInputByOrder[order.id] || '').trim();
    const isPhotoDone = Boolean(photoCapturedByOrder[order.id] || order.pickupPhotoCaptured);
    const isCashOrder =
      order.paymentMethod === 'CASH_ON_DELIVERY' ||
      order.paymentMethod === 'CASH_AT_PICKUP' ||
      order.paymentMethod === 'CASH_AT_DROP' ||
      order.paymentMethod === 'DIRECT_RIDER_UPI';

    if (order.status === 'ASSIGNED') {
      setOrders((prev) =>
        prev.map((o) =>
          o.id === order.id ? { ...o, status: 'ARRIVED_AT_PICKUP', riderLat: 12.9784, riderLng: 77.6408 } : o
        )
      );
      setRiderNotice(
        `Arrived at pickup for ${order.orderCode}. Enter Pickup OTP (${order.pickupOtp}) & Capture Parcel Photo.`
      );
      notificationService.sendNotification({
        title: '📍 Rider Arrived at Pickup!',
        message: `${order.assignedRiderName || 'Rider'} is at pickup location. Share 4-digit OTP: ${order.pickupOtp}`,
        role: 'CUSTOMER',
        type: 'RIDER_ARRIVED',
        orderCode: order.orderCode,
        soundType: 'STATUS_UPDATE',
        actionLabel: 'View OTP',
      });
      return;
    }

    // Step 1 (At Pickup): Require Customer's 4-digit Pickup OTP + Capture Parcel Photo before starting trip (moves to Step 2: PICKED_UP)
    if (order.status === 'ARRIVED_AT_PICKUP') {
      if (enteredOtp !== order.pickupOtp) {
        setRiderNotice(`Invalid Pickup OTP for ${order.orderCode}. Customer's 4-digit Pickup OTP is ${order.pickupOtp}.`);
        return;
      }
      if (!isPhotoDone) {
        setRiderNotice(`Please click 'Capture Parcel Photo' proof button before starting the trip for ${order.orderCode}.`);
        return;
      }
      const ts = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      setOrders((prev) =>
        prev.map((o) =>
          o.id === order.id
            ? {
                ...o,
                status: 'PICKED_UP',
                pickupPhotoCaptured: true,
                pickupProofNote: o.pickupProofNote || `Pickup Parcel Photo Proof Verified (640x480px @ ${ts})`,
                riderLat: 12.9562,
                riderLng: 77.6315,
              }
            : o
        )
      );
      setOtpInputByOrder((prev) => ({ ...prev, [order.id]: '' }));
      setRiderNotice(
        `Step 1 Complete! Now in Step 2 (In Transit): Use Map Navigation & Call Receiver, then click 'Arrived at Drop Location' for Step 3.`
      );

      // Customer Notification: Picked Up
      notificationService.sendNotification({
        title: '📦 Parcel Picked Up & In Transit!',
        message: `Handover verified with OTP. Your package is on the way to ${order.dropAddress}. Track live on GPS map!`,
        role: 'CUSTOMER',
        type: 'ORDER_PICKED_UP',
        orderCode: order.orderCode,
        soundType: 'STATUS_UPDATE',
        actionLabel: 'Track GPS',
      });

      // Rider Notification: Start delivery
      notificationService.sendNotification({
        title: '✅ Pickup Complete!',
        message: `Parcel collected for #${order.orderCode}. Drive safely to ${order.dropAddress}.`,
        role: 'RIDER',
        type: 'ORDER_PICKED_UP',
        orderCode: order.orderCode,
        soundType: 'STATUS_UPDATE',
        actionLabel: 'Drop Route',
      });
      return;
    }

    // Step 2 (In Transit): Move to Step 3 (At Drop - IN_TRANSIT)
    if (order.status === 'PICKED_UP') {
      setOrders((prev) =>
        prev.map((o) =>
          o.id === order.id ? { ...o, status: 'IN_TRANSIT', riderLat: 12.9352, riderLng: 77.6245 } : o
        )
      );
      setRiderNotice(`Step 3 (At Drop): Enter 4-digit Drop OTP (${order.deliveryOtp}) at drop-off to complete delivery.`);
      return;
    }

    // Step 3 (At Drop): Require 4-digit Drop OTP (+ Collect Cash confirmation if COD) to mark Delivered
    if (order.status === 'IN_TRANSIT') {
      if (enteredOtp !== order.deliveryOtp) {
        setRiderNotice(`Invalid Drop OTP for ${order.orderCode}. Customer's 4-digit Drop OTP is ${order.deliveryOtp}.`);
        return;
      }
      if (isCashOrder && !codCollectedByOrder[order.id] && !order.codCashCollected) {
        setRiderNotice(
          `Please confirm 'Collect Cash (₹${(order.totalAmountPaise / 100).toFixed(0)})' before completing COD delivery.`
        );
        return;
      }
      const ts = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      const deliveryPhoto = deliveryPhotoByOrder[order.id]?.url || order.deliveryPhotoUrl;
      const deliveryNote = deliveryPhotoByOrder[order.id]?.note || `Drop-off OTP (${order.deliveryOtp}) & Photo POD Verified @ ${ts}`;
      const completedOrder: DeliveryOrder = {
        ...order,
        status: 'DELIVERED',
        codCashCollected: true,
        deliveryPhotoUrl: deliveryPhoto,
        deliveryProofNote: deliveryNote,
        paymentStatus: isCashOrder ? 'VERIFIED_PAID_TO_COMPANY' : order.paymentStatus,
      };

      setOrders((prev) =>
        prev.map((o) => (o.id === order.id ? completedOrder : o))
      );
      dbService.saveOrder(completedOrder);

      // Customer Notification: Delivered
      notificationService.sendNotification({
        title: '🎉 Parcel Delivered Successfully!',
        message: `Order #${order.orderCode} handover verified with Drop OTP. Thank you for choosing Quick Delivery!`,
        role: 'CUSTOMER',
        type: 'ORDER_DELIVERED',
        orderCode: order.orderCode,
        soundType: 'DELIVERED',
        actionLabel: 'Invoice',
      });

      // Rider Notification: Trip completed and earnings credited
      notificationService.sendNotification({
        title: `💰 Trip Completed • ₹${(order.riderEarningPaise / 100).toFixed(0)} Credited!`,
        message: `80% payout credited to your Rider Wallet. Total earnings today: ₹${((riderWallet.todayNetEarningsPaise + order.riderEarningPaise) / 100).toFixed(0)}`,
        role: 'RIDER',
        type: 'ORDER_DELIVERED',
        orderCode: order.orderCode,
        soundType: 'DELIVERED',
        actionLabel: 'View Wallet',
      });

      // Admin Notification: Ledger Commission
      notificationService.sendNotification({
        title: `🏢 Delivery Completed #${order.orderCode}`,
        message: `20% company commission (₹${(order.companyCommissionPaise / 100).toFixed(0)}) audited and settled in ledger.`,
        role: 'ADMIN',
        type: 'ORDER_DELIVERED',
        orderCode: order.orderCode,
        soundType: 'STATUS_UPDATE',
        actionLabel: 'Audit',
      });

      if (isCashOrder) {
        notificationService.sendNotification({
          title: '💵 Cash Handover Confirmed',
          message: `₹${(order.totalAmountPaise / 100).toFixed(0)} cash on delivery collected for #${order.orderCode}.`,
          role: 'CUSTOMER',
          type: 'COD_COLLECTED',
          orderCode: order.orderCode,
          soundType: 'STATUS_UPDATE',
        });
      }

      // Immediately credit 80% to Rider Wallet and 20% to Admin Web dashboard ledger
      setRiderWallet((prev) => ({
        ...prev,
        balancePaise: prev.balancePaise + order.riderEarningPaise,
        todayNetEarningsPaise: prev.todayNetEarningsPaise + order.riderEarningPaise,
        commissionDebtPaise: isCashOrder
          ? prev.commissionDebtPaise + order.companyCommissionPaise
          : prev.commissionDebtPaise,
        completedTrips: prev.completedTrips + 1,
      }));

      setLedger((prev) => [
        {
          id: `led_${Date.now()}`,
          entryType: isCashOrder ? 'RIDER_COMMISSION_RECEIVABLE' : 'COMPANY_COMMISSION_EARNED',
          referenceId: order.orderCode,
          amountPaise: order.companyCommissionPaise,
          isCredit: true,
          settlementStatus: isCashOrder ? 'RECEIVABLE_FROM_RIDER' : 'VERIFIED_LEDGER_FINAL',
          description: `20% Admin Commission (₹${(order.companyCommissionPaise / 100).toFixed(0)}) • 80% Rider Credited (₹${(order.riderEarningPaise / 100).toFixed(0)}) on ${order.orderCode}`,
          createdAt: ts,
        },
        ...prev,
      ]);

      setOtpInputByOrder((prev) => ({ ...prev, [order.id]: '' }));
      setRiderSubTab('EARNINGS_WALLET');
      setRiderNotice(
        `Order ${order.orderCode} DELIVERED! +₹${(order.riderEarningPaise / 100).toFixed(0)} (80%) credited to Rider Wallet & ₹${(order.companyCommissionPaise / 100).toFixed(0)} (20%) synced to Admin Web.`
      );
    }
  };

  const handleRepayRiderDebt = () => {
    if (riderWallet.commissionDebtPaise <= 0) return;
    const repaid = riderWallet.commissionDebtPaise;
    const ts = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    setRiderWallet((prev) => ({
      ...prev,
      balancePaise: Math.max(0, prev.balancePaise - repaid),
      commissionDebtPaise: 0,
    }));
    setLedger((prev) => [
      {
        id: `led_${Date.now()}`,
        entryType: 'RIDER_COMMISSION_REPAYMENT',
        referenceId: riderWallet.riderId,
        amountPaise: repaid,
        isCredit: true,
        settlementStatus: 'SETTLED_TO_BANK',
        description: `Rider ${riderWallet.riderName} settled COD commission debt of ₹${(repaid / 100).toFixed(2)}`,
        createdAt: ts,
      },
      ...prev,
    ]);
    setRiderNotice(`Cash-in-Hand COD dues of ₹${(repaid / 100).toFixed(2)} settled via UPI!`);
  };

  const handleSendChat = (orderId: string, role: 'CUSTOMER' | 'RIDER') => {
    const text = (role === 'CUSTOMER' ? customerChatDraft : riderChatDraft).trim();
    if (!text) return;
    const newMsg: ChatMessage = {
      id: `msg_${Date.now()}`,
      orderId,
      senderRole: role,
      senderName: role === 'CUSTOMER' ? customerName : riderWallet.riderName,
      messageText: text,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };
    setChatMessages((prev) => [...prev, newMsg]);
    if (role === 'CUSTOMER') setCustomerChatDraft('');
    else setRiderChatDraft('');
  };

  const handleExportCsv = () => {
    const headers = ['Entry ID', 'Type', 'Reference', 'Amount (INR)', 'Status', 'Description', 'Time'];
    const rows = ledger.map((l) => [
      l.id,
      l.entryType,
      l.referenceId,
      (l.amountPaise / 100).toFixed(2),
      l.settlementStatus,
      `"${l.description}"`,
      l.createdAt,
    ]);
    const csvContent =
      'data:text/csv;charset=utf-8,' +
      [headers.join(','), ...rows.map((e) => e.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `quick_delivery_ledger_${Date.now()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const activeOrder = orders[0];

  // Helper for 5-step customer tracking timeline
  const getTimelineStepIndex = (status: OrderStatus): number => {
    switch (status) {
      case 'PENDING_ASSIGNMENT':
        return 0;
      case 'ASSIGNED':
      case 'ARRIVED_AT_PICKUP':
        return 1;
      case 'PICKED_UP':
        return 2;
      case 'IN_TRANSIT':
        return 3;
      case 'DELIVERED':
        return 4;
      default:
        return 0;
    }
  };

  // --- 1. PROFESSIONAL CUSTOMER APP PANEL (4 CLEAN SUB-TABS + 2-STEP BOOKING + PLAY STORE & LIVE CONFIG SYNC) ---
  const renderCustomerPanel = () => {
    const currentStepIdx = activeOrder ? getTimelineStepIndex(activeOrder.status) : 0;
    const TIMELINE_STEPS = ['Searching', 'Rider Assigned', 'Picked Up', 'In Transit', 'Delivered'];
    const activeOrdersCount = orders.filter((o) => o.status !== 'DELIVERED').length;
    const hasCustomerUpdate =
      updateMode !== 'NONE' && installedCustomerVersion.trim() !== latestCustomerVersion.trim();

    return (
      <div className="bg-white rounded-2xl shadow-md border border-slate-200 overflow-hidden flex flex-col relative">
        {/* GOOGLE PLAY STORE FORCE UPDATE FULL-SCREEN BLOCKING MODAL (WHEN updateMode === 'FORCE') */}
        {hasCustomerUpdate && updateMode === 'FORCE' && (
          <div className="absolute inset-0 z-40 bg-slate-950/85 backdrop-blur-xs p-5 flex items-center justify-center">
            <div className="w-full max-w-md rounded-2xl bg-white border-2 border-[#059669] shadow-2xl overflow-hidden">
              <div className="bg-gradient-to-r from-[#042F2E] via-[#065F46] to-[#059669] px-4 py-3.5 text-white flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="px-2 py-0.5 rounded-md bg-white text-[#042F2E] text-[10px] font-extrabold tracking-wider uppercase flex items-center gap-1">
                    ▶ Google Play
                  </span>
                  <span className="text-xs font-extrabold text-[#A7F3D0]">
                    Mandatory Force Update
                  </span>
                </div>
                <span className="px-2 py-0.5 rounded-full bg-red-500/30 border border-red-300/40 text-[10px] font-extrabold text-white">
                  OLD APP BLOCKED
                </span>
              </div>

              <div className="p-4 space-y-3.5">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <h3 className="text-sm sm:text-base font-extrabold text-slate-900">
                      Update Quick Delivery Customer App to v{latestCustomerVersion}
                    </h3>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Installed: <strong className="text-red-600">v{installedCustomerVersion}</strong> • Required:{' '}
                      <strong className="text-[#059669]">v{latestCustomerVersion}</strong>
                    </p>
                  </div>
                  <Smartphone className="w-7 h-7 text-[#059669] shrink-0" />
                </div>

                <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
                  <div className="text-[10px] font-extrabold uppercase tracking-wider text-[#065F46]">
                    What&apos;s New in v{latestCustomerVersion} (Release Notes)
                  </div>
                  <div className="text-xs text-slate-700 whitespace-pre-line leading-relaxed">
                    {releaseNotes}
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() => {
                    setInstalledCustomerVersion(latestCustomerVersion);
                    setBookingBanner(
                      `✅ Updated Customer App to v${latestCustomerVersion} from Google Play Store!`
                    );
                  }}
                  className="w-full py-3 px-4 rounded-xl bg-[#059669] hover:bg-emerald-600 text-white font-extrabold text-xs sm:text-sm shadow-md transition flex items-center justify-center gap-2 cursor-pointer"
                >
                  <Download className="w-4 h-4" />
                  <span>Update Now from Play Store (v{latestCustomerVersion})</span>
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Customer Header with Top-Left Menu Icon linked to Tab 4 (Wallet, Coupons & Support) */}
        <div className="bg-gradient-to-r from-[#042F2E] via-[#065F46] to-[#059669] text-white px-4 py-3.5 flex items-center justify-between gap-2">
          <div className="flex items-center gap-2.5">
            <button
              type="button"
              onClick={() => setCustomerSubTab('WALLET_SUPPORT')}
              title="Open Wallet, Coupons & Support"
              className="p-2 bg-white/15 hover:bg-white/25 rounded-xl border border-emerald-300/30 transition cursor-pointer"
            >
              <Menu className="w-4 h-4 text-[#A7F3D0]" />
            </button>
            <div>
              <div className="flex flex-wrap items-center gap-1.5">
                <h2 className="font-extrabold text-sm sm:text-base leading-tight">Quick Delivery — Customer</h2>
                <span className="text-[10px] font-extrabold uppercase tracking-wider bg-emerald-400/20 text-[#A7F3D0] border border-emerald-300/30 px-2 py-0.5 rounded-full">
                  v{installedCustomerVersion}
                </span>
                {surgeMultiplier > 1 && (
                  <span className="text-[10px] font-extrabold uppercase tracking-wider bg-amber-400/25 text-amber-200 border border-amber-300/40 px-2 py-0.5 rounded-full">
                    ⚡ {surgeMultiplier.toFixed(1)}x Surge
                  </span>
                )}
              </div>
              <p className="text-[11px] text-emerald-100/90">
                2-Step Parcel Booking • Live OTP Security • GST Invoice
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={() => setCustomerSubTab('WALLET_SUPPORT')}
            className="text-right bg-white/10 hover:bg-white/20 px-3 py-1.5 rounded-xl border border-white/15 transition cursor-pointer"
          >
            <div className="text-[10px] uppercase tracking-wider text-emerald-200 font-bold">Quick Wallet</div>
            <div className="text-sm font-extrabold text-white">₹{customerWalletInr}.00</div>
          </button>
        </div>

        {/* GOOGLE PLAY STORE FLEXIBLE UPDATE BANNER (WHEN updateMode === 'FLEXIBLE') */}
        {hasCustomerUpdate && updateMode === 'FLEXIBLE' && !customerDismissedFlexible && (
          <div className="bg-gradient-to-r from-emerald-950 via-[#042F2E] to-teal-900 text-white px-4 py-3 border-b border-emerald-700 flex flex-wrap items-center justify-between gap-2.5">
            <div className="space-y-0.5 max-w-xl">
              <div className="flex flex-wrap items-center gap-1.5">
                <span className="px-2 py-0.5 rounded bg-white text-[#042F2E] text-[10px] font-extrabold uppercase tracking-wider">
                  ▶ Google Play Store
                </span>
                <span className="text-xs font-extrabold text-[#A7F3D0]">
                  Update Available: v{latestCustomerVersion} (Installed: v{installedCustomerVersion})
                </span>
              </div>
              <div className="text-[11px] text-emerald-100/90 line-clamp-2">
                <strong className="text-white">What&apos;s New:</strong>{' '}
                {releaseNotes.replace(/\n/g, ' • ')}
              </div>
            </div>
            <div className="flex items-center gap-2 shrink-0">
              <button
                type="button"
                onClick={() => setCustomerDismissedFlexible(true)}
                className="px-2.5 py-1.5 rounded-lg bg-white/10 hover:bg-white/20 text-emerald-100 text-[11px] font-bold cursor-pointer"
              >
                Later
              </button>
              <button
                type="button"
                onClick={() => {
                  setInstalledCustomerVersion(latestCustomerVersion);
                  setBookingBanner(
                    `✅ Customer App updated to v${latestCustomerVersion} via Google Play Store!`
                  );
                }}
                className="px-3 py-1.5 rounded-lg bg-[#10B981] hover:bg-emerald-400 text-[#042F2E] text-xs font-extrabold flex items-center gap-1.5 shadow-xs cursor-pointer"
              >
                <Download className="w-3.5 h-3.5" />
                <span>Update Now from Play Store</span>
              </button>
            </div>
          </div>
        )}

        {/* LIVE ADMIN ANNOUNCEMENT BANNER FOR CUSTOMERS */}
        {customerAnnouncement.trim() && (
          <div className="bg-emerald-50/90 border-b border-emerald-200 px-4 py-2 flex items-center justify-between gap-2 text-xs text-[#042F2E]">
            <div className="flex items-center gap-2 font-bold">
              <Megaphone className="w-3.5 h-3.5 text-[#059669] shrink-0" />
              <span>{customerAnnouncement}</span>
            </div>
            <span className="px-2 py-0.5 rounded-full bg-emerald-200/70 text-[#065F46] text-[10px] font-extrabold shrink-0">
              LIVE CONFIG
            </span>
          </div>
        )}

        {/* 4 Clean Sub-Tabs Navigation Bar */}
        <div className="bg-slate-50 border-b border-slate-200 px-3 py-2 grid grid-cols-2 sm:grid-cols-4 gap-1.5">
          <button
            type="button"
            onClick={() => setCustomerSubTab('BOOK_ORDER')}
            className={`px-2.5 py-2 rounded-xl text-xs font-extrabold flex items-center justify-center gap-1.5 transition cursor-pointer ${
              customerSubTab === 'BOOK_ORDER'
                ? 'bg-[#042F2E] text-white shadow-xs'
                : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-100'
            }`}
          >
            <ShoppingBag className="w-3.5 h-3.5 text-[#10B981]" />
            <span>1. Book Order</span>
          </button>

          <button
            type="button"
            onClick={() => setCustomerSubTab('LIVE_TRACKING')}
            className={`px-2.5 py-2 rounded-xl text-xs font-extrabold flex items-center justify-center gap-1.5 transition cursor-pointer ${
              customerSubTab === 'LIVE_TRACKING'
                ? 'bg-[#042F2E] text-white shadow-xs'
                : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-100'
            }`}
          >
            <Navigation className="w-3.5 h-3.5 text-[#10B981]" />
            <span>2. Live Tracking</span>
            {activeOrdersCount > 0 && (
              <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-emerald-500 text-white">
                {activeOrdersCount}
              </span>
            )}
          </button>

          <button
            type="button"
            onClick={() => setCustomerSubTab('ORDERS_INVOICES')}
            className={`px-2.5 py-2 rounded-xl text-xs font-extrabold flex items-center justify-center gap-1.5 transition cursor-pointer ${
              customerSubTab === 'ORDERS_INVOICES'
                ? 'bg-[#042F2E] text-white shadow-xs'
                : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-100'
            }`}
          >
            <FileText className="w-3.5 h-3.5 text-[#10B981]" />
            <span>3. Orders & GST</span>
            <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-slate-200 text-slate-800">
              {orders.length}
            </span>
          </button>

          <button
            type="button"
            onClick={() => setCustomerSubTab('WALLET_SUPPORT')}
            className={`px-2.5 py-2 rounded-xl text-xs font-extrabold flex items-center justify-center gap-1.5 transition cursor-pointer ${
              customerSubTab === 'WALLET_SUPPORT'
                ? 'bg-[#042F2E] text-white shadow-xs'
                : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-100'
            }`}
          >
            <Wallet className="w-3.5 h-3.5 text-[#10B981]" />
            <span>4. Wallet & Support</span>
          </button>
        </div>

        <div className="p-4 sm:p-5 space-y-4">
          {/* SERVICE MAINTENANCE MODE BANNER (WHEN ADMIN DISABLES BOOKINGS) */}
          {!serviceAvailable && (
            <div className="p-3.5 rounded-xl bg-red-50 border border-red-300 text-red-900 text-xs font-bold flex items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 text-red-600 shrink-0" />
                <span>
                  🔧 Service Maintenance Mode Active: New parcel bookings are temporarily paused by Admin Command Center. Live tracking for ongoing orders remains active.
                </span>
              </div>
              <span className="px-2 py-0.5 rounded bg-red-200 text-red-950 text-[10px] font-extrabold shrink-0">
                PAUSED
              </span>
            </div>
          )}

          {bookingBanner && (
            <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-900 text-xs font-semibold flex items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-[#059669] shrink-0" />
                <span>{bookingBanner}</span>
              </div>
              <button
                type="button"
                onClick={() => setBookingBanner(null)}
                className="text-emerald-700 hover:text-emerald-950 p-1"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          )}

          {/* =========================================================
              SUB-TAB 1: BOOK ORDER (CLEAN 2-STEP FLOW)
             ========================================================= */}
          {customerSubTab === 'BOOK_ORDER' && (
            <ErrorBoundary
              inline
              fallbackTitle="Parcel Booking Assistant"
              fallbackMessage="An error occurred while displaying the parcel booking form. Click below to return to Step 1."
              onReset={() => setCustomerBookStep(1)}
            >
              <form onSubmit={handleCreateOrder} className="space-y-4">
              {/* 2-Step Progress Pill Header */}
              <div className="flex items-center justify-between bg-slate-100 p-2 rounded-xl border border-slate-200">
                <button
                  type="button"
                  onClick={() => setCustomerBookStep(1)}
                  className={`flex-1 py-2 px-3 rounded-lg text-xs font-extrabold flex items-center justify-center gap-1.5 transition cursor-pointer ${
                    customerBookStep === 1
                      ? 'bg-[#059669] text-white shadow-xs'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  <MapPin className="w-3.5 h-3.5" />
                  <span>Step 1: Pickup/Drop & Vehicle</span>
                </button>
                <button
                  type="button"
                  onClick={() => setCustomerBookStep(2)}
                  className={`flex-1 py-2 px-3 rounded-lg text-xs font-extrabold flex items-center justify-center gap-1.5 transition cursor-pointer ${
                    customerBookStep === 2
                      ? 'bg-[#059669] text-white shadow-xs'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  <Package className="w-3.5 h-3.5" />
                  <span>Step 2: Parcel, Fare & Payment</span>
                </button>
              </div>

              {customerBookStep === 1 ? (
                /* --- STEP 1: ONLY PICKUP & DROP ADDRESSES (WITH SAVED TAGS) + VEHICLE FLEET SELECTOR --- */
                <div className="space-y-4">
                  <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200/90 space-y-3.5">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-extrabold uppercase tracking-wider text-slate-700 flex items-center gap-1.5">
                        <MapPin className="w-4 h-4 text-[#059669]" />
                        Pickup & Drop Route (Saved Address Tags)
                      </span>
                      <span className="text-[11px] font-bold text-[#059669] bg-emerald-50 px-2.5 py-0.5 rounded-full border border-emerald-200">
                        {distanceKm.toFixed(1)} km Route
                      </span>
                    </div>

                    {/* Pickup Address + Saved Tags (Home, Office, Other) */}
                    <div className="p-3 rounded-xl bg-white border border-slate-200 space-y-2">
                      <div className="flex items-center justify-between flex-wrap gap-1.5">
                        <span className="text-[11px] font-extrabold text-emerald-800 flex items-center gap-1">
                          <span className="w-2 h-2 rounded-full bg-[#10B981]" />
                          PICKUP LOCATION
                        </span>
                        <div className="flex items-center gap-1">
                          {(['Home', 'Office', 'Other'] as AddressTag[]).map((tag) => (
                            <button
                              key={`pickup_${tag}`}
                              type="button"
                              onClick={() => handleSelectPickupTag(tag)}
                              className={`px-2.5 py-0.5 rounded-full text-[10px] font-extrabold transition flex items-center gap-1 cursor-pointer ${
                                pickupTag === tag
                                  ? 'bg-[#059669] text-white shadow-2xs'
                                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                              }`}
                            >
                              {tag === 'Home' && <Home className="w-2.5 h-2.5" />}
                              {tag === 'Office' && <Briefcase className="w-2.5 h-2.5" />}
                              {tag === 'Other' && <Bookmark className="w-2.5 h-2.5" />}
                              {tag}
                            </button>
                          ))}
                        </div>
                      </div>
                      <input
                        type="text"
                        value={pickupAddress}
                        onChange={(e) => setPickupAddress(e.target.value)}
                        className="w-full px-3 py-2 text-xs font-medium bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:border-[#059669]"
                        placeholder="Enter Pickup Address"
                      />
                    </div>

                    {/* Drop Address + Saved Tags (Home, Office, Other) */}
                    <div className="p-3 rounded-xl bg-white border border-slate-200 space-y-2">
                      <div className="flex items-center justify-between flex-wrap gap-1.5">
                        <span className="text-[11px] font-extrabold text-amber-800 flex items-center gap-1">
                          <span className="w-2 h-2 rounded-full bg-amber-500" />
                          DROP-OFF LOCATION
                        </span>
                        <div className="flex items-center gap-1">
                          {(['Home', 'Office', 'Other'] as AddressTag[]).map((tag) => (
                            <button
                              key={`drop_${tag}`}
                              type="button"
                              onClick={() => handleSelectDropTag(tag)}
                              className={`px-2.5 py-0.5 rounded-full text-[10px] font-extrabold transition flex items-center gap-1 cursor-pointer ${
                                dropTag === tag
                                  ? 'bg-amber-600 text-white shadow-2xs'
                                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                              }`}
                            >
                              {tag === 'Home' && <Home className="w-2.5 h-2.5" />}
                              {tag === 'Office' && <Briefcase className="w-2.5 h-2.5" />}
                              {tag === 'Other' && <Bookmark className="w-2.5 h-2.5" />}
                              {tag}
                            </button>
                          ))}
                        </div>
                      </div>
                      <input
                        type="text"
                        value={dropAddress}
                        onChange={(e) => setDropAddress(e.target.value)}
                        className="w-full px-3 py-2 text-xs font-medium bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:border-[#059669]"
                        placeholder="Enter Drop-off Address"
                      />
                    </div>

                    {/* Route Distance Slider */}
                    <div className="pt-1">
                      <div className="flex items-center justify-between text-xs font-semibold text-slate-700 mb-1">
                        <span>Estimated Route Distance</span>
                        <span className="font-extrabold text-[#059669]">{distanceKm.toFixed(1)} km</span>
                      </div>
                      <input
                        type="range"
                        min="1"
                        max="25"
                        step="0.5"
                        value={distanceKm}
                        onChange={(e) => setDistanceKm(parseFloat(e.target.value))}
                        className="w-full accent-[#059669]"
                      />
                    </div>
                  </div>

                  {/* Vehicle Fleet Selector Cards (Dynamically Synced with Admin Pricing & Surge) */}
                  <div className="space-y-2.5">
                    <div className="flex items-center justify-between">
                      <label className="block text-xs font-extrabold uppercase tracking-wider text-slate-700 flex items-center gap-1.5">
                        <Bike className="w-4 h-4 text-[#059669]" />
                        Select Vehicle Fleet (Live Admin Pricing & ETA)
                      </label>
                      <span className="text-[11px] font-bold text-emerald-700">
                        {surgeMultiplier > 1 ? `⚡ ${surgeMultiplier.toFixed(1)}x Surge Active` : 'Standard Live Rates'}
                      </span>
                    </div>

                    <div className="space-y-2">
                      {(['BIKE', 'EV', 'MINI_TRUCK'] as VehicleType[]).map((v) => {
                        const spec = VEHICLE_CATALOG[v];
                        const liveRate = vehiclePricing[v] || { baseInr: spec.baseInr, perKmInr: spec.perKmInr };
                        const active = selectedVehicle === v;
                        const rawDist = Math.round(liveRate.perKmInr * distanceKm);
                        const surgeAdd = Math.round((liveRate.baseInr + rawDist) * Math.max(0, surgeMultiplier - 1));
                        const vehicleEstFare = Math.max(
                          35,
                          Math.round(
                            liveRate.baseInr +
                              rawDist +
                              surgeAdd +
                              platformFeeConfigInr -
                              fareQuote.promoDiscountInr
                          )
                        );
                        return (
                          <button
                            type="button"
                            key={v}
                            onClick={() => setSelectedVehicle(v)}
                            className={`w-full p-3 rounded-xl border text-left transition flex items-center justify-between cursor-pointer ${
                              active
                                ? 'border-[#059669] bg-emerald-50/70 ring-1 ring-[#059669] shadow-xs'
                                : 'border-slate-200 bg-white hover:bg-slate-50'
                            }`}
                          >
                            <div className="flex items-center gap-3">
                              <div
                                className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${
                                  active ? 'bg-[#059669] text-white' : 'bg-slate-100 text-slate-700'
                                }`}
                              >
                                {v === 'BIKE' && <Bike className="w-5 h-5" />}
                                {v === 'EV' && <Zap className="w-5 h-5" />}
                                {v === 'MINI_TRUCK' && <Truck className="w-5 h-5" />}
                              </div>
                              <div>
                                <div className="flex flex-wrap items-center gap-1.5">
                                  <span className="text-xs sm:text-sm font-extrabold text-slate-900">
                                    {spec.label}
                                  </span>
                                  <span className="px-2 py-0.5 rounded-full bg-emerald-100 text-[#042F2E] text-[10px] font-extrabold">
                                    {spec.etaMins}
                                  </span>
                                </div>
                                <div className="text-[11px] text-slate-500 mt-0.5 flex flex-wrap items-center gap-1.5">
                                  <span>{spec.subtitle}</span>
                                  <span>•</span>
                                  <span className="px-1.5 py-0.2 rounded bg-slate-200/70 text-slate-700 font-bold text-[10px]">
                                    {spec.capacityBadge}
                                  </span>
                                </div>
                              </div>
                            </div>
                            <div className="text-right shrink-0 ml-2">
                              <div className="text-sm sm:text-base font-extrabold text-[#042F2E]">
                                ₹{vehicleEstFare}
                              </div>
                              <div className="text-[10px] text-slate-500 font-semibold">
                                Base ₹{liveRate.baseInr} + ₹{liveRate.perKmInr}/km
                              </div>
                            </div>
                          </button>
                        );
                      })}
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => {
                      if (!pickupAddress.trim()) {
                        setPickupAddress('Pickup Location');
                      }
                      if (!dropAddress.trim()) {
                        setDropAddress('Drop Location');
                      }
                      setCustomerBookStep(2);
                    }}
                    className="w-full py-3.5 px-4 rounded-xl bg-[#059669] hover:bg-emerald-600 text-white font-extrabold text-sm shadow-sm transition flex items-center justify-center gap-2 cursor-pointer"
                  >
                    <span>
                      Proceed with {getVehicleLabel(selectedVehicle)} (₹{fareQuote.totalInr}) →
                    </span>
                  </button>
                </div>
              ) : (
                /* --- STEP 2: SENDER/RECEIVER, PACKAGE CATEGORY, WEIGHT BRACKETS, PROMO, FARE BREAKDOWN & PAYMENT --- */
                <div className="space-y-4">
                  {/* Compact Route & Vehicle Summary Bar with Back to Step 1 Button */}
                  <div className="p-3 rounded-xl bg-emerald-50/90 border border-emerald-200 flex flex-wrap items-center justify-between gap-2">
                    <div className="text-xs text-emerald-950">
                      <div className="font-extrabold flex items-center gap-1.5">
                        <Bike className="w-3.5 h-3.5 text-[#059669]" />
                        <span>
                          {getVehicleLabel(selectedVehicle)} • {fareQuote.distanceKm.toFixed(1)} km
                        </span>
                      </div>
                      <div className="text-[11px] text-emerald-800 truncate max-w-md">
                        {pickupTag}: {pickupAddress || 'Pickup Location'} → {dropTag}: {dropAddress || 'Drop Location'}
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() => setCustomerBookStep(1)}
                      className="px-2.5 py-1.5 rounded-lg bg-white border border-emerald-300 text-emerald-900 text-xs font-extrabold hover:bg-emerald-100 transition cursor-pointer"
                    >
                      ← Edit Route / Vehicle
                    </button>
                  </div>

                  {/* Sender & Receiver Name & Phone Inputs */}
                  <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200 space-y-3">
                    <div className="text-xs font-extrabold uppercase tracking-wider text-slate-700">
                      Sender & Receiver Contact Details
                    </div>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                      <div>
                        <label className="block text-[10px] font-bold uppercase text-slate-500 mb-1">
                          Sender Name
                        </label>
                        <input
                          type="text"
                          value={customerName}
                          onChange={(e) => setCustomerName(e.target.value)}
                          className="w-full px-2.5 py-1.5 text-xs bg-white border border-slate-200 rounded-lg"
                          placeholder="Sender Name"
                        />
                      </div>
                      <div>
                        <label className="block text-[10px] font-bold uppercase text-slate-500 mb-1">
                          Sender Phone
                        </label>
                        <input
                          type="text"
                          value={customerPhone}
                          onChange={(e) => setCustomerPhone(e.target.value)}
                          className="w-full px-2.5 py-1.5 text-xs bg-white border border-slate-200 rounded-lg"
                          placeholder="Sender Phone"
                        />
                      </div>
                      <div>
                        <label className="block text-[10px] font-bold uppercase text-slate-500 mb-1">
                          Receiver Name
                        </label>
                        <input
                          type="text"
                          value={receiverName}
                          onChange={(e) => setReceiverName(e.target.value)}
                          className="w-full px-2.5 py-1.5 text-xs bg-white border border-slate-200 rounded-lg"
                          placeholder="Receiver Name"
                        />
                      </div>
                      <div>
                        <label className="block text-[10px] font-bold uppercase text-slate-500 mb-1">
                          Receiver Phone
                        </label>
                        <input
                          type="text"
                          value={receiverPhone}
                          onChange={(e) => setReceiverPhone(e.target.value)}
                          className="w-full px-2.5 py-1.5 text-xs bg-white border border-slate-200 rounded-lg"
                          placeholder="Receiver Mobile Number"
                        />
                      </div>
                    </div>
                  </div>

                  {/* Package Category Selector Pills & Weight Brackets */}
                  <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200 space-y-3">
                    <div>
                      <label className="block text-[11px] font-extrabold uppercase tracking-wider text-slate-600 mb-1.5">
                        Package Category
                      </label>
                      <div className="flex flex-wrap gap-1.5">
                        {PACKAGE_CATEGORIES.map((cat) => (
                          <button
                            key={cat}
                            type="button"
                            onClick={() => setSelectedCategory(cat)}
                            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer ${
                              selectedCategory === cat
                                ? 'bg-[#042F2E] text-[#A7F3D0] shadow-xs'
                                : 'bg-white text-slate-700 border border-slate-200 hover:border-slate-300'
                            }`}
                          >
                            {cat}
                          </button>
                        ))}
                      </div>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-12 gap-2.5 items-end">
                      <div className="sm:col-span-8">
                        <label className="block text-[11px] font-extrabold uppercase tracking-wider text-slate-600 mb-1.5">
                          Weight Bracket (Up to 20kg Limit)
                        </label>
                        <div className="grid grid-cols-3 gap-1.5">
                          {WEIGHT_BRACKETS.map((wb) => (
                            <button
                              key={wb.label}
                              type="button"
                              onClick={() => handleSelectWeightBracket(wb.label, wb.defaultKg)}
                              className={`py-1.5 px-2 rounded-xl text-xs font-extrabold border transition cursor-pointer ${
                                selectedWeightBracket === wb.label
                                  ? 'border-[#059669] bg-emerald-50 text-[#065F46]'
                                  : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-100'
                              }`}
                            >
                              {wb.label}
                            </button>
                          ))}
                        </div>
                      </div>
                      <div className="sm:col-span-4">
                        <label className="block text-[10px] font-bold uppercase text-slate-500 mb-1">
                          Exact Weight (kg)
                        </label>
                        <input
                          type="number"
                          step="0.5"
                          min="0.5"
                          max="25"
                          value={weightInput}
                          onChange={(e) => setWeightInput(e.target.value)}
                          className={`w-full px-2.5 py-1.5 text-xs font-bold border rounded-xl bg-white ${
                            isOverWeight ? 'border-red-500 bg-red-50 text-red-900' : 'border-slate-200'
                          }`}
                        />
                      </div>
                    </div>
                    {isOverWeight && (
                      <p className="text-[11px] text-red-600 font-semibold flex items-center gap-1">
                        <AlertTriangle className="w-3.5 h-3.5" />
                        Weight exceeds 20 kg maximum hyperlocal bike/EV parcel limit.
                      </p>
                    )}
                  </div>

                  {/* Transparent Fare Breakdown + Promo Code */}
                  <div className="p-3.5 rounded-2xl bg-[#042F2E] text-white space-y-2.5">
                    <div className="flex flex-wrap items-center justify-between gap-2 pb-2 border-b border-white/10">
                      <div className="flex items-center gap-1.5 text-xs font-extrabold text-[#A7F3D0]">
                        <Tag className="w-3.5 h-3.5" />
                        <span>
                          Transparent Fare Breakdown (GST Included
                          {surgeMultiplier > 1 ? ` • ${surgeMultiplier.toFixed(1)}x Surge` : ''})
                        </span>
                      </div>
                      <div className="flex items-center gap-1.5">
                        <input
                          type="text"
                          value={promoCodeInput}
                          onChange={(e) => setPromoCodeInput(e.target.value.toUpperCase())}
                          placeholder="PROMO CODE"
                          className="w-24 px-2 py-1 text-[11px] font-extrabold uppercase rounded-lg bg-white/10 border border-white/20 text-white placeholder:text-slate-400 focus:outline-none"
                        />
                        <button
                          type="button"
                          onClick={() => setPromoApplied((prev) => !prev)}
                          className={`px-2.5 py-1 rounded-lg text-[10px] font-extrabold transition cursor-pointer ${
                            promoApplied
                              ? 'bg-[#10B981] text-[#042F2E]'
                              : 'bg-white/15 text-white hover:bg-white/25'
                          }`}
                        >
                          {promoApplied ? `APPLIED (-₹${fareQuote.promoDiscountInr})` : 'APPLY'}
                        </button>
                      </div>
                    </div>

                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-[11px]">
                      <div className="bg-white/5 p-2 rounded-lg border border-white/10">
                        <div className="text-slate-300">Base Fare</div>
                        <div className="font-extrabold text-white">₹{fareQuote.baseFareInr}</div>
                      </div>
                      <div className="bg-white/5 p-2 rounded-lg border border-white/10">
                        <div className="text-slate-300">
                          Distance ({distanceKm}km{surgeMultiplier > 1 ? ` • ${surgeMultiplier.toFixed(1)}x` : ''})
                        </div>
                        <div className="font-extrabold text-white">+₹{fareQuote.distanceChargeInr}</div>
                      </div>
                      <div className="bg-white/5 p-2 rounded-lg border border-white/10">
                        <div className="text-slate-300">Platform Fee</div>
                        <div className="font-extrabold text-white">+₹{fareQuote.platformFeeInr}</div>
                      </div>
                      <div className="bg-emerald-500/20 p-2 rounded-lg border border-emerald-400/30">
                        <div className="text-emerald-200">Promo Discount</div>
                        <div className="font-extrabold text-[#A7F3D0]">-₹{fareQuote.promoDiscountInr}</div>
                      </div>
                    </div>

                    <div className="flex flex-wrap items-center justify-between gap-2 pt-1">
                      <div>
                        <span className="text-[11px] text-emerald-200 font-bold">Total Payable Fare: </span>
                        <span className="text-lg font-extrabold text-white">₹{fareQuote.totalInr}.00</span>
                      </div>
                      <div className="flex items-center gap-2 text-[11px]">
                        <span className="px-2 py-0.5 rounded bg-emerald-500/20 text-[#A7F3D0] font-bold">
                          80% Rider: ₹{(fareQuote.riderPaise / 100).toFixed(0)}
                        </span>
                        <span className="px-2 py-0.5 rounded bg-white/10 text-slate-200 font-bold">
                          20% Platform: ₹{(fareQuote.commissionPaise / 100).toFixed(0)}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Payment Method Selector */}
                  <div>
                    <label className="block text-xs font-extrabold uppercase tracking-wider text-slate-700 mb-2">
                      Payment Method Selector
                    </label>
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                      {[
                        { id: 'CASH_ON_DELIVERY', label: 'Cash on Delivery (COD)', sub: 'Pay Cash to Rider at Door', badge: 'Zero Advance' },
                        { id: 'UPI_INSTANT_PAY', label: 'UPI Instant Pay', sub: 'GPay / PhonePe / Paytm', badge: 'Fast' },
                        { id: 'CASH_AT_PICKUP', label: 'Cash at Pickup', sub: 'Sender Pays Rider', badge: 'Cash' },
                        { id: 'QUICK_WALLET', label: 'Quick Wallet', sub: `Bal: ₹${customerWalletInr}`, badge: 'Prepaid' },
                      ].map((pm) => (
                        <button
                          key={pm.id}
                          type="button"
                          onClick={() => setSelectedPayment(pm.id as PaymentMethod)}
                          className={`p-2.5 rounded-xl border text-left transition cursor-pointer relative ${
                            selectedPayment === pm.id
                              ? 'border-[#059669] bg-emerald-50 text-[#065F46] ring-1 ring-[#059669]'
                              : 'border-slate-200 text-slate-700 hover:bg-slate-50'
                          }`}
                        >
                          <div className="flex items-center justify-between gap-1">
                            <div className="text-xs font-extrabold leading-tight">{pm.label}</div>
                            {pm.badge && (
                              <span className="text-[8px] font-black uppercase px-1.5 py-0.5 rounded bg-emerald-100 text-emerald-800 shrink-0">
                                {pm.badge}
                              </span>
                            )}
                          </div>
                          <div className="text-[10px] text-slate-500 mt-0.5">{pm.sub}</div>
                        </button>
                      ))}
                    </div>

                    {/* Informative COD banner */}
                    {selectedPayment === 'CASH_ON_DELIVERY' && (
                      <div className="mt-2.5 p-2.5 rounded-xl bg-amber-50 border border-amber-300 text-amber-950 text-xs flex items-center gap-2.5">
                        <Banknote className="w-4 h-4 text-amber-700 shrink-0" />
                        <span>
                          <strong>Cash on Delivery (COD):</strong> No advance online payment required. Pay <strong>₹{fareQuote.totalInr}</strong> in cash or scan rider&apos;s dynamic UPI QR upon doorstep delivery.
                        </span>
                      </div>
                    )}
                  </div>

                  {/* Route & GPS Map Preview before Booking */}
                  <div className="space-y-1.5">
                    <div className="flex items-center justify-between text-xs font-extrabold text-[#042F2E]">
                      <span className="flex items-center gap-1.5">
                        <Navigation className="w-3.5 h-3.5 text-[#059669]" />
                        Estimated GPS Route ({fareQuote.distanceKm} km • ~{Math.round(fareQuote.distanceKm * 3.2)} mins)
                      </span>
                      <span className="text-[10px] font-bold text-slate-500">
                        PWA High-Precision Map
                      </span>
                    </div>
                    <ErrorBoundary
                      inline
                      fallbackTitle="Route Map Preview (Offline Mode)"
                      fallbackMessage="GPS route preview is temporarily unavailable, but your addresses and fare calculation are verified and ready to book."
                    >
                      <DeliveryMapComponent
                        orderId="preview-booking-route"
                        orderCode="PREVIEW"
                        status="PENDING_ASSIGNMENT"
                        pickupAddress={pickupAddress || 'Pickup Location'}
                        dropAddress={dropAddress || 'Drop Location'}
                        pickupTag={pickupTag || 'Pickup'}
                        dropTag={dropTag || 'Drop'}
                        vehicleType={selectedVehicle}
                        compact={true}
                      />
                    </ErrorBoundary>
                  </div>

                  <div className="flex gap-2">
                    <button
                      type="button"
                      onClick={() => setCustomerBookStep(1)}
                      className="px-4 py-3.5 rounded-xl border border-slate-300 bg-white text-slate-700 font-extrabold text-xs hover:bg-slate-50 transition cursor-pointer"
                    >
                      ← Step 1
                    </button>
                    <button
                      type="submit"
                      disabled={isOverWeight || !serviceAvailable}
                      className="flex-1 py-3.5 px-4 rounded-xl bg-[#059669] hover:bg-emerald-600 disabled:bg-slate-300 text-white font-extrabold text-sm shadow-sm transition flex items-center justify-center gap-2 cursor-pointer"
                    >
                      <Send className="w-4 h-4" />
                      <span>
                        {!serviceAvailable
                          ? 'Service Paused (Maintenance Mode)'
                          : `Confirm & Book Parcel (${getVehicleLabel(selectedVehicle)}) • ₹${fareQuote.totalInr}`}
                      </span>
                    </button>
                  </div>
                </div>
              )}
            </form>
          </ErrorBoundary>
          )}

          {/* =========================================================
              SUB-TAB 2: LIVE TRACKING (5-STEP TIMELINE, OTPs, VERIFIED RIDER & COLLAPSIBLE CHAT MODAL)
             ========================================================= */}
          {customerSubTab === 'LIVE_TRACKING' && (
            <ErrorBoundary
              inline
              fallbackTitle="Live Order Tracking Temporarily Re-syncing"
              fallbackMessage="Your order has been booked and saved! Reloading live GPS telemetry..."
              onReset={() => setCustomerSubTab('LIVE_TRACKING')}
            >
            <div className="space-y-4">
              {activeOrder ? (
                <div className="p-4 rounded-2xl bg-[#0D1B2A] text-white space-y-4 border border-slate-800 shadow-md">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-[11px] uppercase tracking-wider text-[#A7F3D0] font-extrabold">
                          LIVE TRACKING • #{activeOrder.orderCode}
                        </span>
                        <span className="px-2 py-0.5 rounded-full bg-emerald-500/20 text-[#A7F3D0] border border-emerald-400/30 text-[10px] font-bold">
                          {activeOrder.packageCategory || 'Documents'} • {activeOrder.weightKg} kg
                        </span>
                      </div>
                      <h3 className="text-sm font-extrabold mt-0.5 text-white">
                        {(activeOrder.status || 'PENDING_ASSIGNMENT').replace(/_/g, ' ')} • {getVehicleLabel(activeOrder.vehicleType)}
                      </h3>
                      <p className="text-[11px] text-slate-300 mt-0.5">
                        {activeOrder.pickupTag || 'Pickup'}: {activeOrder.pickupAddress} →{' '}
                        {activeOrder.dropTag || 'Drop'}: {activeOrder.dropAddress}
                      </p>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="px-3 py-1 rounded-lg bg-white/10 text-xs font-extrabold text-white">
                        ₹{(activeOrder.totalAmountPaise / 100).toFixed(0)}
                      </span>
                      <button
                        type="button"
                        onClick={() => handleDownloadGstInvoice(activeOrder)}
                        className="px-3 py-1.5 rounded-lg bg-[#059669] hover:bg-emerald-600 text-white text-xs font-extrabold flex items-center gap-1.5 border border-[#A7F3D0]/40 transition shadow-xs cursor-pointer"
                      >
                        <FileText className="w-3.5 h-3.5" />
                        Download GST Invoice
                      </button>
                    </div>
                  </div>

                  {/* Sleek 5-Stage Status Timeline: Searching -> Rider Assigned -> Picked Up -> In Transit -> Delivered */}
                  <div className="p-3 rounded-xl bg-white/5 border border-white/10 space-y-2">
                    <div className="grid grid-cols-5 gap-1">
                      {TIMELINE_STEPS.map((stepLabel, idx) => {
                        const done = idx <= currentStepIdx;
                        const isCurrent = idx === currentStepIdx;
                        return (
                          <div key={stepLabel} className="flex flex-col items-center text-center">
                            <div
                              className={`w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-extrabold border transition ${
                                done
                                  ? 'bg-[#10B981] text-[#022C22] border-[#A7F3D0]'
                                  : 'bg-slate-800 text-slate-400 border-slate-700'
                              } ${isCurrent ? 'ring-2 ring-[#A7F3D0]' : ''}`}
                            >
                              {done ? '✓' : idx + 1}
                            </div>
                            <span
                              className={`text-[10px] mt-1 font-bold leading-tight ${
                                done ? 'text-[#A7F3D0]' : 'text-slate-400'
                              }`}
                            >
                              {stepLabel}
                            </span>
                          </div>
                        );
                      })}
                    </div>
                  </div>

                  {/* Interactive GPS Route & Live Telemetry Map (Crash-Proof Standalone PWA Engine) */}
                  <ErrorBoundary
                    inline
                    fallbackTitle="Live GPS Map Recovering"
                    fallbackMessage="Telemetry map is refreshing. Your order status and OTPs remain active below."
                  >
                    <DeliveryMapComponent
                      orderId={activeOrder.id}
                      orderCode={activeOrder.orderCode}
                      status={activeOrder.status || 'PENDING_ASSIGNMENT'}
                      pickupAddress={activeOrder.pickupAddress || 'Pickup Location'}
                      dropAddress={activeOrder.dropAddress || 'Drop Location'}
                      pickupTag={activeOrder.pickupTag || 'Pickup'}
                      dropTag={activeOrder.dropTag || 'Drop'}
                      vehicleType={activeOrder.vehicleType || 'BIKE'}
                      riderName={activeOrder.assignedRiderName || 'Vikram Singh'}
                      riderPlate={activeOrder.assignedRiderPlate || 'KA-01-EQ-4821'}
                      riderPhone={activeOrder.assignedRiderPhone || '9811223344'}
                      riderRating={activeOrder.assignedRiderRating || '4.9★'}
                      isRiderView={false}
                      compact={false}
                    />
                  </ErrorBoundary>

                  {/* 4-Digit Pickup OTP & 4-Digit Drop OTP Security Badges */}
                  <div className="grid grid-cols-2 gap-2.5 text-xs">
                    <div className="p-3 rounded-xl bg-amber-500/15 border border-amber-400/30 flex items-center justify-between">
                      <div>
                        <div className="text-amber-200 text-[10px] font-bold uppercase">
                          4-Digit Pickup OTP
                        </div>
                        <div className="text-lg font-extrabold text-amber-300 tracking-widest mt-0.5">
                          {activeOrder.pickupOtp}
                        </div>
                        <div className="text-[10px] text-amber-100/80">Share with Rider at Pickup</div>
                      </div>
                      <Lock className="w-5 h-5 text-amber-300/80" />
                    </div>
                    <div className="p-3 rounded-xl bg-emerald-500/15 border border-emerald-400/30 flex items-center justify-between">
                      <div>
                        <div className="text-emerald-200 text-[10px] font-bold uppercase">
                          4-Digit Drop OTP
                        </div>
                        <div className="text-lg font-extrabold text-[#A7F3D0] tracking-widest mt-0.5">
                          {activeOrder.deliveryOtp}
                        </div>
                        <div className="text-[10px] text-emerald-100/80">Share at Doorstep Drop</div>
                      </div>
                      <ShieldCheck className="w-5 h-5 text-[#A7F3D0]/80" />
                    </div>
                  </div>

                  {/* Verified Rider Profile Card + Collapsible Chat Drawer/Modal Button */}
                  <div className="p-3 rounded-xl bg-white/10 border border-white/15 flex flex-wrap items-center justify-between gap-2">
                    <div className="flex items-center gap-2.5">
                      <div className="w-10 h-10 rounded-xl bg-[#059669] border border-[#A7F3D0] flex items-center justify-center font-extrabold text-sm text-white">
                        VS
                      </div>
                      <div>
                        <div className="flex items-center gap-1.5">
                          <span className="text-xs font-extrabold text-white">
                            {activeOrder.assignedRiderName || 'Vikram Singh'}
                          </span>
                          <span className="px-1.5 py-0.5 rounded bg-amber-400/20 text-amber-300 text-[10px] font-extrabold flex items-center gap-0.5">
                            <Star className="w-3 h-3 fill-amber-300 text-amber-300" />
                            {activeOrder.assignedRiderRating || '4.9★'}
                          </span>
                          <span className="px-1.5 py-0.5 rounded bg-emerald-500/20 text-[#A7F3D0] text-[10px] font-bold">
                            KYC Verified
                          </span>
                        </div>
                        <div className="text-[11px] text-slate-300 mt-0.5">
                          Plate: <strong className="text-white">{activeOrder.assignedRiderPlate || 'KA-01-EQ-4821'}</strong> • Receiver: {activeOrder.receiverName}
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-1.5">
                      <button
                        type="button"
                        onClick={() =>
                          setBookingBanner(
                            `Connecting masked bridge call to Rider ${activeOrder.assignedRiderName || 'Vikram Singh'} (+91 98112-23344)...`
                          )
                        }
                        className="px-2.5 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold flex items-center gap-1 transition cursor-pointer"
                      >
                        <Phone className="w-3.5 h-3.5" />
                        Call
                      </button>

                      <button
                        type="button"
                        onClick={() => setCustomerChatModalOpen((prev) => !prev)}
                        className={`px-3 py-1.5 rounded-lg text-xs font-extrabold flex items-center gap-1.5 transition cursor-pointer ${
                          customerChatModalOpen
                            ? 'bg-amber-400 text-slate-950'
                            : 'bg-white/15 hover:bg-white/25 text-white border border-white/20'
                        }`}
                      >
                        <MessageSquare className="w-3.5 h-3.5" />
                        <span>
                          {customerChatModalOpen ? 'Close Chat' : '💬 Chat with Rider'} (
                          {chatMessages.filter((m) => m.orderId === activeOrder.id).length})
                        </span>
                      </button>
                    </div>
                  </div>

                  {/* Collapsible Live Rider Chat Drawer/Modal */}
                  {customerChatModalOpen && (
                    <div className="p-3.5 rounded-xl bg-slate-900/95 border border-emerald-400/40 space-y-2.5">
                      <div className="text-xs font-semibold text-[#A7F3D0] flex items-center justify-between">
                        <span className="flex items-center gap-1.5">
                          <MessageSquare className="w-3.5 h-3.5" />
                          Live Rider Chat ({activeOrder.assignedRiderName || 'Vikram Singh'})
                        </span>
                        <button
                          type="button"
                          onClick={() => setCustomerChatModalOpen(false)}
                          className="text-slate-400 hover:text-white"
                        >
                          <X className="w-3.5 h-3.5" />
                        </button>
                      </div>
                      <div className="max-h-32 overflow-y-auto space-y-1.5 pr-1">
                        {chatMessages
                          .filter((m) => m.orderId === activeOrder.id)
                          .map((msg) => (
                            <div
                              key={msg.id}
                              className={`text-xs px-2.5 py-1.5 rounded-xl ${
                                msg.senderRole === 'CUSTOMER'
                                  ? 'bg-emerald-600/40 text-white ml-6'
                                  : 'bg-white/10 text-slate-100 mr-6'
                              }`}
                            >
                              <span className="font-bold text-[10px] opacity-80 block">
                                {msg.senderName} ({msg.senderRole}) • {msg.timestamp}
                              </span>
                              {msg.messageText}
                            </div>
                          ))}
                      </div>
                      <div className="flex gap-1.5">
                        <input
                          type="text"
                          placeholder="Message Rider Partner..."
                          value={customerChatDraft}
                          onChange={(e) => setCustomerChatDraft(e.target.value)}
                          className="flex-1 px-2.5 py-1.5 rounded-xl bg-white/10 text-white placeholder-slate-400 text-xs focus:outline-none"
                        />
                        <button
                          type="button"
                          onClick={() => handleSendChat(activeOrder.id, 'CUSTOMER')}
                          className="px-3 py-1.5 rounded-xl bg-[#059669] text-white text-xs font-bold flex items-center gap-1 cursor-pointer"
                        >
                          <Send className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <div className="p-8 text-center rounded-2xl bg-slate-50 border border-slate-200 space-y-3">
                  <Navigation className="w-8 h-8 text-slate-400 mx-auto" />
                  <div className="text-sm font-extrabold text-slate-800">No Active Order Being Tracked</div>
                  <p className="text-xs text-slate-500">
                    Book a parcel in Tab 1 to track live GPS progress, Pickup/Drop OTPs, and Rider details.
                  </p>
                  <button
                    type="button"
                    onClick={() => setCustomerSubTab('BOOK_ORDER')}
                    className="px-4 py-2 rounded-xl bg-[#059669] text-white text-xs font-extrabold cursor-pointer"
                  >
                    Go to Book Order
                  </button>
                </div>
              )}
            </div>
            </ErrorBoundary>
          )}

          {/* =========================================================
              SUB-TAB 3: ORDERS & GST INVOICES (COMPREHENSIVE ORDER HISTORY)
             ========================================================= */}
          {customerSubTab === 'ORDERS_INVOICES' && (
            <ErrorBoundary
              inline
              fallbackTitle="Order History View"
              fallbackMessage="Refreshing order history..."
            >
              <CustomerOrderHistoryView
                orders={orders as any}
                onTrackOrder={(ord) => setSelectedTrackingOrder(ord as any)}
                onDownloadGstInvoice={handleDownloadGstInvoice}
                onReOrder={handleReOrder}
                onCancelOrder={handleCustomerCancelOrder}
                onJumpToBooking={() => setCustomerSubTab('BOOK_ORDER')}
              />
            </ErrorBoundary>
          )}

          {/* =========================================================
              SUB-TAB 4: WALLET, COUPONS & SUPPORT
             ========================================================= */}
          {customerSubTab === 'WALLET_SUPPORT' && (
            <div className="space-y-4">
              {/* Customer Profile & Photo Card */}
              <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div className="flex items-center gap-3">
                  <div className="relative group shrink-0">
                    {currentUser.avatarUrl ? (
                      <img
                        src={currentUser.avatarUrl}
                        alt={currentUser.name}
                        className="w-14 h-14 rounded-2xl object-cover border-2 border-emerald-500 shadow-sm"
                      />
                    ) : (
                      <div className="w-14 h-14 rounded-2xl bg-[#042F2E] text-[#A7F3D0] flex items-center justify-center font-black text-lg border-2 border-emerald-500">
                        {currentUser.avatarText || currentUser.name.slice(0, 2).toUpperCase()}
                      </div>
                    )}
                    <button
                      type="button"
                      onClick={() =>
                        setImageUploadModalState({
                          isOpen: true,
                          category: 'PROFILE_PHOTO',
                          title: 'Update Customer Profile Photo',
                          subtitle: 'Upload or capture a photo for your customer profile',
                          currentImageUrl: currentUser.avatarUrl,
                        })
                      }
                      title="Update Profile Photo"
                      className="absolute -bottom-1 -right-1 w-6 h-6 rounded-full bg-[#059669] hover:bg-emerald-600 text-white flex items-center justify-center shadow-md cursor-pointer transition"
                    >
                      <Camera className="w-3.5 h-3.5" />
                    </button>
                  </div>

                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-extrabold text-sm sm:text-base text-slate-900">
                        {currentUser.name}
                      </span>
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-emerald-100 text-[#065F46]">
                        Verified Customer
                      </span>
                    </div>
                    <div className="text-xs text-slate-500 mt-0.5">
                      +91 {currentUser.phone} • {currentUser.email}
                    </div>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() =>
                    setImageUploadModalState({
                      isOpen: true,
                      category: 'PROFILE_PHOTO',
                      title: 'Update Customer Profile Photo',
                      subtitle: 'Upload or capture a photo for your customer profile',
                      currentImageUrl: currentUser.avatarUrl,
                    })
                  }
                  className="px-3.5 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-bold transition flex items-center gap-1.5 self-start sm:self-auto cursor-pointer"
                >
                  <Camera className="w-3.5 h-3.5 text-[#059669]" />
                  <span>Change Profile Photo</span>
                </button>
              </div>

              {/* Quick Wallet Balance Card */}
              <div className="p-4 rounded-2xl bg-gradient-to-r from-[#042F2E] to-[#065F46] text-white flex flex-wrap items-center justify-between gap-3">
                <div>
                  <div className="text-[10px] font-extrabold uppercase tracking-wider text-[#A7F3D0]">
                    Quick Delivery Customer Wallet
                  </div>
                  <div className="text-2xl font-extrabold mt-0.5">₹{customerWalletInr}.00</div>
                  <div className="text-[11px] text-emerald-100 mt-0.5">
                    1-Click Instant Checkout • Zero Payment Gateway Failure
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    setCustomerWalletInr((prev) => prev + 500);
                    setBookingBanner('Added ₹500.00 to Quick Wallet via UPI AutoPay!');
                  }}
                  className="px-3.5 py-2 rounded-xl bg-[#10B981] hover:bg-emerald-400 text-[#042F2E] font-extrabold text-xs transition cursor-pointer"
                >
                  + Add ₹500 via UPI
                </button>
              </div>

              {/* Available Promo Codes (Live Synced with Admin Promo Config) */}
              <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200 space-y-2.5">
                <div className="text-xs font-extrabold uppercase tracking-wider text-slate-700 flex items-center justify-between gap-1.5">
                  <span className="flex items-center gap-1.5">
                    <Tag className="w-3.5 h-3.5 text-[#059669]" />
                    Available Hyperlocal Promo Coupons (Live Admin Sync)
                  </span>
                  <span className="text-[10px] font-bold text-[#059669]">
                    {promoCodesConfig.filter((p) => p.active).length} Active
                  </span>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                  {promoCodesConfig
                    .filter((cp) => cp.active)
                    .map((cp) => (
                      <div
                        key={cp.code}
                        className="p-3 rounded-xl bg-white border border-emerald-200 flex items-center justify-between gap-2"
                      >
                        <div>
                          <div className="font-mono font-extrabold text-xs text-[#065F46]">
                            {cp.code} • Flat ₹{cp.discountInr} OFF
                          </div>
                          <div className="text-[11px] text-slate-500">{cp.desc}</div>
                        </div>
                        <button
                          type="button"
                          onClick={() => {
                            setPromoCodeInput(cp.code);
                            setPromoApplied(true);
                            setCustomerSubTab('BOOK_ORDER');
                            setCustomerBookStep(2);
                            setBookingBanner(
                              `Promo code ${cp.code} applied! ₹${cp.discountInr} discount unlocked.`
                            );
                          }}
                          className="px-2.5 py-1.5 rounded-lg bg-emerald-50 hover:bg-emerald-100 text-[#065F46] font-extrabold text-xs border border-emerald-200 shrink-0 cursor-pointer"
                        >
                          Apply
                        </button>
                      </div>
                    ))}
                </div>
              </div>

              {/* Saved Addresses Directory */}
              <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200 space-y-2">
                <div className="text-xs font-extrabold uppercase tracking-wider text-slate-700 flex items-center gap-1.5">
                  <Bookmark className="w-3.5 h-3.5 text-[#059669]" />
                  Saved Pickup & Drop Addresses
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                  {(['Home', 'Office', 'Other'] as AddressTag[]).map((tag) => (
                    <div key={tag} className="p-2.5 rounded-xl bg-white border border-slate-200 space-y-1">
                      <div className="text-xs font-extrabold text-slate-900">{tag}</div>
                      <div className="text-[11px] text-slate-500">
                        {SAVED_PICKUP_PRESETS[tag] || 'Enter custom address in Book Order'}
                      </div>
                      <button
                        type="button"
                        onClick={() => {
                          handleSelectPickupTag(tag);
                          setCustomerSubTab('BOOK_ORDER');
                          setCustomerBookStep(1);
                        }}
                        className="text-[10px] font-extrabold text-[#059669] hover:underline cursor-pointer"
                      >
                        Select Tag →
                      </button>
                    </div>
                  ))}
                </div>
              </div>

              {/* 24x7 Customer Support & Parcel Insurance */}
              <div className="p-3.5 rounded-2xl bg-slate-900 text-white flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center gap-2.5">
                  <LifeBuoy className="w-4 h-4 text-[#A7F3D0]" />
                  <div>
                    <div className="text-xs font-extrabold">24×7 Customer Support & Parcel Protection</div>
                    <div className="text-[10px] text-slate-400">
                      Instant resolution for live deliveries, OTP issues & GST billing
                    </div>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() =>
                    setBookingBanner(
                      '🎧 Connected to Quick Delivery 24×7 Priority Customer Support Desk!'
                    )
                  }
                  className="px-3 py-1.5 rounded-xl bg-[#10B981] text-[#042F2E] text-xs font-extrabold cursor-pointer"
                >
                  Request Callback
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    );
  };

  // --- 2. PROFESSIONAL RIDER PARTNER APP PANEL (4 CLEAN SUB-TABS + PLAY STORE & LIVE CONFIG SYNC) ---
  const renderRiderPanel = () => {
    const isOnline = riderWallet.shiftStatus === 'ONLINE_ACTIVE';
    const debtInr = riderWallet.commissionDebtPaise / 100;
    const debtPercent = Math.min(
      100,
      Math.round((riderWallet.commissionDebtPaise / MAX_COMMISSION_DEBT_PAISE) * 100)
    );
    const incomingOrders = orders.filter(
      (o) => o.status === 'PENDING_ASSIGNMENT' && !declinedOrderIds.includes(o.id)
    );
    const activeAssignedOrders = orders.filter(
      (o) => o.status !== 'PENDING_ASSIGNMENT' && o.status !== 'DELIVERED'
    );
    const completedOrders = orders.filter((o) => o.status === 'DELIVERED');
    const hasRiderUpdate =
      updateMode !== 'NONE' && installedRiderVersion.trim() !== latestRiderVersion.trim();

    return (
      <div className="bg-white rounded-2xl shadow-md border border-slate-200 overflow-hidden flex flex-col relative">
        {/* GOOGLE PLAY STORE FORCE UPDATE FULL-SCREEN BLOCKING MODAL FOR RIDER APP */}
        {hasRiderUpdate && updateMode === 'FORCE' && (
          <div className="absolute inset-0 z-40 bg-slate-950/85 backdrop-blur-xs p-5 flex items-center justify-center">
            <div className="w-full max-w-md rounded-2xl bg-white border-2 border-[#059669] shadow-2xl overflow-hidden">
              <div className="bg-[#0D1B2A] px-4 py-3.5 text-white flex items-center justify-between border-b border-emerald-800">
                <div className="flex items-center gap-2">
                  <span className="px-2 py-0.5 rounded-md bg-white text-[#042F2E] text-[10px] font-extrabold tracking-wider uppercase">
                    ▶ Google Play
                  </span>
                  <span className="text-xs font-extrabold text-[#A7F3D0]">
                    Rider Partner Force Update
                  </span>
                </div>
                <span className="px-2 py-0.5 rounded-full bg-red-500/30 border border-red-300/40 text-[10px] font-extrabold text-white">
                  DISPATCH LOCKED
                </span>
              </div>

              <div className="p-4 space-y-3.5">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <h3 className="text-sm sm:text-base font-extrabold text-slate-900">
                      Update Quick Delivery Rider Partner to v{latestRiderVersion}
                    </h3>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Installed: <strong className="text-red-600">v{installedRiderVersion}</strong> • Required:{' '}
                      <strong className="text-[#059669]">v{latestRiderVersion}</strong>
                    </p>
                  </div>
                  <Smartphone className="w-7 h-7 text-[#059669] shrink-0" />
                </div>

                <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
                  <div className="text-[10px] font-extrabold uppercase tracking-wider text-[#065F46]">
                    What&apos;s New in v{latestRiderVersion} (Release Notes)
                  </div>
                  <div className="text-xs text-slate-700 whitespace-pre-line leading-relaxed">
                    {releaseNotes}
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() => {
                    setInstalledRiderVersion(latestRiderVersion);
                    setRiderNotice(
                      `✅ Updated Rider Partner App to v${latestRiderVersion} via Google Play Store!`
                    );
                  }}
                  className="w-full py-3 px-4 rounded-xl bg-[#059669] hover:bg-emerald-600 text-white font-extrabold text-xs sm:text-sm shadow-md transition flex items-center justify-center gap-2 cursor-pointer"
                >
                  <Download className="w-4 h-4" />
                  <span>Update Now from Play Store (v{latestRiderVersion})</span>
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Top Rider Header: Compact ON/OFF DUTY Toggle, Live GPS Badge & Always-Accessible Emergency SOS */}
        <div className="bg-[#0D1B2A] text-white px-4 py-3.5 border-b border-slate-800 flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-emerald-500/20 rounded-xl text-[#A7F3D0] border border-emerald-400/30">
              <Bike className="w-5 h-5" />
            </div>
            <div>
              <div className="flex flex-wrap items-center gap-1.5">
                <h2 className="font-extrabold text-sm sm:text-base leading-tight">
                  Rider Partner ({riderWallet.riderName})
                </h2>
                <span className="px-2 py-0.5 rounded-full bg-emerald-500/20 text-[#A7F3D0] border border-emerald-400/30 text-[10px] font-extrabold">
                  v{installedRiderVersion}
                </span>
                <span className="px-2 py-0.5 rounded-full bg-emerald-500/20 text-[#A7F3D0] border border-emerald-400/30 text-[10px] font-extrabold">
                  GPS LIVE • 12.97°N
                </span>
                {surgeMultiplier > 1 && (
                  <span className="px-2 py-0.5 rounded-full bg-amber-400/25 text-amber-200 border border-amber-300/40 text-[10px] font-extrabold">
                    ⚡ {surgeMultiplier.toFixed(1)}x Surge
                  </span>
                )}
              </div>
              <p className="text-[11px] text-slate-300 mt-0.5">
                {riderWallet.riderId} • KA-01-EQ-4821 • 4.9★ •{' '}
                <BatteryCharging className="w-3.5 h-3.5 inline text-emerald-400" />{' '}
                {riderWallet.batteryPercent}%
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {/* Compact Always-Accessible Emergency SOS Button */}
            <button
              type="button"
              onClick={() =>
                setRiderNotice(
                  '🚨 EMERGENCY SOS ALERT TRIGGERED! Live GPS coordinates (12.9716°N, 77.5946°E) dispatched to Admin Command Center.'
                )
              }
              className="px-2.5 py-1.5 rounded-xl bg-red-600 hover:bg-red-500 text-white text-[11px] font-extrabold flex items-center gap-1 transition shadow-xs cursor-pointer"
              title="Trigger Emergency SOS"
            >
              <AlertTriangle className="w-3.5 h-3.5" />
              <span>SOS</span>
            </button>

            {/* Interactive ON DUTY / OFF DUTY Toggle Switch */}
            <button
              type="button"
              onClick={handleToggleRiderOnline}
              className={`px-3.5 py-1.5 rounded-full text-xs font-extrabold flex items-center gap-2 transition shadow-sm border cursor-pointer ${
                isOnline
                  ? 'bg-[#059669] text-white border-[#A7F3D0] hover:bg-emerald-600'
                  : 'bg-slate-800 text-slate-300 border-slate-600 hover:bg-slate-700'
              }`}
            >
              <span
                className={`w-2 h-2 rounded-full ${
                  isOnline ? 'bg-[#A7F3D0] animate-ping' : 'bg-slate-400'
                }`}
              />
              {isOnline ? 'ON DUTY' : 'OFF DUTY'}
            </button>
          </div>
        </div>

        {/* GOOGLE PLAY STORE FLEXIBLE UPDATE BANNER FOR RIDER APP */}
        {hasRiderUpdate && updateMode === 'FLEXIBLE' && !riderDismissedFlexible && (
          <div className="bg-gradient-to-r from-slate-900 via-[#042F2E] to-emerald-950 text-white px-4 py-3 border-b border-emerald-700 flex flex-wrap items-center justify-between gap-2.5">
            <div className="space-y-0.5 max-w-xl">
              <div className="flex flex-wrap items-center gap-1.5">
                <span className="px-2 py-0.5 rounded bg-white text-[#042F2E] text-[10px] font-extrabold uppercase tracking-wider">
                  ▶ Google Play Store
                </span>
                <span className="text-xs font-extrabold text-[#A7F3D0]">
                  Rider Update Available: v{latestRiderVersion} (Installed: v{installedRiderVersion})
                </span>
              </div>
              <div className="text-[11px] text-emerald-100/90 line-clamp-2">
                <strong className="text-white">What&apos;s New:</strong>{' '}
                {releaseNotes.replace(/\n/g, ' • ')}
              </div>
            </div>
            <div className="flex items-center gap-2 shrink-0">
              <button
                type="button"
                onClick={() => setRiderDismissedFlexible(true)}
                className="px-2.5 py-1.5 rounded-lg bg-white/10 hover:bg-white/20 text-emerald-100 text-[11px] font-bold cursor-pointer"
              >
                Later
              </button>
              <button
                type="button"
                onClick={() => {
                  setInstalledRiderVersion(latestRiderVersion);
                  setRiderNotice(
                    `✅ Rider Partner App updated to v${latestRiderVersion} via Google Play Store!`
                  );
                }}
                className="px-3 py-1.5 rounded-lg bg-[#10B981] hover:bg-emerald-400 text-[#042F2E] text-xs font-extrabold flex items-center gap-1.5 shadow-xs cursor-pointer"
              >
                <Download className="w-3.5 h-3.5" />
                <span>Update Now from Play Store</span>
              </button>
            </div>
          </div>
        )}

        {/* LIVE ADMIN ANNOUNCEMENT BANNER FOR RIDER PARTNERS */}
        {riderAnnouncement.trim() && (
          <div className="bg-emerald-50/90 border-b border-emerald-200 px-4 py-2 flex items-center justify-between gap-2 text-xs text-[#042F2E]">
            <div className="flex items-center gap-2 font-bold">
              <Megaphone className="w-3.5 h-3.5 text-[#059669] shrink-0" />
              <span>{riderAnnouncement}</span>
            </div>
            <span className="px-2 py-0.5 rounded-full bg-emerald-200/70 text-[#065F46] text-[10px] font-extrabold shrink-0">
              LIVE CONFIG
            </span>
          </div>
        )}

        {/* 5 Clean Sub-Tabs Navigation Bar */}
        <div className="bg-slate-50 border-b border-slate-200 px-3 py-2 grid grid-cols-2 sm:grid-cols-5 gap-1.5">
          <button
            type="button"
            onClick={() => setRiderSubTab('DUTY_RADAR')}
            className={`px-2.5 py-2 rounded-xl text-xs font-extrabold flex items-center justify-center gap-1.5 transition cursor-pointer ${
              riderSubTab === 'DUTY_RADAR'
                ? 'bg-[#042F2E] text-white shadow-xs'
                : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-100'
            }`}
          >
            <Bell className="w-3.5 h-3.5 text-[#10B981]" />
            <span>1. Duty & Radar</span>
            {incomingOrders.length > 0 && (
              <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-emerald-500 text-white">
                {incomingOrders.length}
              </span>
            )}
          </button>

          <button
            type="button"
            onClick={() => setRiderSubTab('ACTIVE_TRIP')}
            className={`px-2.5 py-2 rounded-xl text-xs font-extrabold flex items-center justify-center gap-1.5 transition cursor-pointer ${
              riderSubTab === 'ACTIVE_TRIP'
                ? 'bg-[#042F2E] text-white shadow-xs'
                : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-100'
            }`}
          >
            <Navigation className="w-3.5 h-3.5 text-[#10B981]" />
            <span>2. Active Trip</span>
            {activeAssignedOrders.length > 0 && (
              <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-amber-500 text-white">
                {activeAssignedOrders.length}
              </span>
            )}
          </button>

          <button
            type="button"
            onClick={() => setRiderSubTab('TRIP_HISTORY')}
            className={`px-2.5 py-2 rounded-xl text-xs font-extrabold flex items-center justify-center gap-1.5 transition cursor-pointer ${
              riderSubTab === 'TRIP_HISTORY'
                ? 'bg-[#042F2E] text-white shadow-xs'
                : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-100'
            }`}
          >
            <Clock className="w-3.5 h-3.5 text-[#10B981]" />
            <span>3. Trip History</span>
            <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-emerald-500 text-white">
              {orders.filter((o) => o.status === 'DELIVERED').length}
            </span>
          </button>

          <button
            type="button"
            onClick={() => setRiderSubTab('EARNINGS_WALLET')}
            className={`px-2.5 py-2 rounded-xl text-xs font-extrabold flex items-center justify-center gap-1.5 transition cursor-pointer ${
              riderSubTab === 'EARNINGS_WALLET'
                ? 'bg-[#042F2E] text-white shadow-xs'
                : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-100'
            }`}
          >
            <Wallet className="w-3.5 h-3.5 text-[#10B981]" />
            <span>4. Earnings & Wallet</span>
          </button>

          <button
            type="button"
            onClick={() => setRiderSubTab('PROFILE_SUPPORT')}
            className={`px-2.5 py-2 rounded-xl text-xs font-extrabold flex items-center justify-center gap-1.5 transition cursor-pointer ${
              riderSubTab === 'PROFILE_SUPPORT'
                ? 'bg-[#042F2E] text-white shadow-xs'
                : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-100'
            }`}
          >
            <ShieldCheck className="w-3.5 h-3.5 text-[#10B981]" />
            <span>5. Profile & KYC</span>
          </button>
        </div>

        <div className="p-4 sm:p-5 space-y-4 flex-1 overflow-y-auto">
          {riderNotice && (
            <div className="p-3 rounded-xl bg-amber-50 border border-amber-200 text-xs text-amber-950 font-bold flex items-center justify-between gap-2">
              <span>{riderNotice}</span>
              <button
                type="button"
                onClick={() => setRiderNotice(null)}
                className="text-amber-700 hover:text-amber-950 p-1"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          )}

          {/* =========================================================
              RIDER SUB-TAB 1: DUTY & RADAR (COMPACT DUTY BAR + INCOMING DISPATCH CARDS ONLY)
             ========================================================= */}
          {riderSubTab === 'DUTY_RADAR' && (
            <div className="space-y-4">
              {/* Compact Duty Status & Live Radar Telemetry Strip */}
              <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200 flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center gap-2.5">
                  <span
                    className={`w-3 h-3 rounded-full ${
                      isOnline ? 'bg-[#10B981] animate-ping' : 'bg-slate-400'
                    }`}
                  />
                  <div>
                    <div className="text-xs font-extrabold text-slate-900">
                      {isOnline
                        ? 'Live Hyperlocal Dispatch Radar Active (Bengaluru)'
                        : 'Rider Radar Paused — Switch to ON DUTY to Receive Pings'}
                    </div>
                    <div className="text-[11px] text-slate-500">
                      80% Rider Share Guaranteed • Today&apos;s Net: ₹
                      {(riderWallet.todayNetEarningsPaise / 100).toFixed(0)}
                    </div>
                  </div>
                </div>
                {activeAssignedOrders.length > 0 && (
                  <button
                    type="button"
                    onClick={() => setRiderSubTab('ACTIVE_TRIP')}
                    className="px-3 py-1.5 rounded-xl bg-amber-500 hover:bg-amber-600 text-white text-xs font-extrabold cursor-pointer"
                  >
                    Open Active Trip ({activeAssignedOrders.length}) →
                  </button>
                )}
              </div>

              {/* Incoming Order Dispatch Cards ONLY */}
              {incomingOrders.length > 0 ? (
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <h3 className="text-xs font-extrabold uppercase tracking-wider text-slate-600">
                      Incoming Order Dispatch Cards ({incomingOrders.length})
                    </h3>
                    <span className="text-[11px] font-bold text-[#059669]">
                      80% Rider Share Guaranteed
                    </span>
                  </div>

                  {incomingOrders.map((order) => (
                    <div
                      key={order.id}
                      className="p-4 rounded-2xl border space-y-3.5 shadow-xs bg-[#042F2E] text-white border-emerald-700"
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div>
                          <div className="flex flex-wrap items-center gap-1.5">
                            <span className="text-xs font-extrabold px-2 py-0.5 rounded-md bg-emerald-500/20 text-[#A7F3D0] border border-emerald-400/30">
                              #{order.orderCode}
                            </span>
                            <span className="text-xs font-bold px-2 py-0.5 rounded-md bg-white/10 text-white">
                              📦 {order.packageCategory || 'Documents'} • {order.weightKg} kg
                            </span>
                            <span className="text-[11px] font-bold px-2 py-0.5 rounded-md bg-amber-400/20 text-amber-300">
                              {order.distanceKm} km
                            </span>
                          </div>
                          <div className="text-[11px] mt-1 font-medium text-emerald-200">
                            {getVehicleLabel(order.vehicleType)} •{' '}
                            {(order.paymentMethod || 'CASH_ON_DELIVERY').replace(/_/g, ' ')}
                          </div>
                        </div>

                        <div className="text-right shrink-0">
                          <div className="text-[10px] font-extrabold uppercase text-[#A7F3D0]">
                            80% Rider Earning
                          </div>
                          <div className="text-lg font-extrabold text-[#A7F3D0]">
                            ₹{(order.riderEarningPaise / 100).toFixed(0)}
                          </div>
                          <div className="text-[10px] text-emerald-200/80">
                            Total Fare ₹{(order.totalAmountPaise / 100).toFixed(0)}
                          </div>
                        </div>
                      </div>

                      <div className="p-3 rounded-xl text-xs space-y-1.5 bg-white/10 border border-white/15 text-white">
                        <div className="flex items-start justify-between gap-2">
                          <div>
                            <strong className="text-[#A7F3D0]">
                              📍 PICKUP ({order.pickupTag || 'Home'}):
                            </strong>{' '}
                            {order.pickupAddress}
                          </div>
                          <span className="text-[10px] font-bold shrink-0 opacity-80">1.2 km away</span>
                        </div>
                        <div className="flex items-start justify-between gap-2 pt-1 border-t border-current/10">
                          <div>
                            <strong className="text-amber-300">
                              🏁 DROP ({order.dropTag || 'Office'}):
                            </strong>{' '}
                            {order.dropAddress}
                          </div>
                          <span className="text-[10px] font-bold shrink-0 opacity-80">
                            {order.distanceKm} km trip
                          </span>
                        </div>
                      </div>

                      <div className="grid grid-cols-3 gap-2 pt-1">
                        <button
                          type="button"
                          onClick={() => handleClaimOrder(order)}
                          className="col-span-2 py-2.5 px-4 rounded-xl bg-[#10B981] hover:bg-emerald-400 text-[#022C22] font-extrabold text-xs sm:text-sm transition shadow-sm flex items-center justify-center gap-1.5 cursor-pointer"
                        >
                          <CheckCircle2 className="w-4 h-4" />
                          Accept Order (Earn ₹{(order.riderEarningPaise / 100).toFixed(0)})
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDeclineOrder(order)}
                          className="py-2.5 px-3 rounded-xl bg-white/10 hover:bg-white/20 text-white font-bold text-xs transition border border-white/20 cursor-pointer"
                        >
                          Decline
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="p-8 text-center rounded-2xl bg-slate-50 border border-slate-200 space-y-3">
                  <div className="w-12 h-12 rounded-full bg-emerald-100 text-[#059669] flex items-center justify-center mx-auto">
                    <Bike className="w-6 h-6" />
                  </div>
                  <div className="text-sm font-extrabold text-slate-900">
                    Searching nearby orders...
                  </div>
                  <p className="text-xs text-slate-500 max-w-sm mx-auto">
                    Your live GPS radar is scanning for hyperlocal parcel bookings. New orders will appear here with 80% Rider Share.
                  </p>
                  {declinedOrderIds.length > 0 && (
                    <button
                      type="button"
                      onClick={() => setDeclinedOrderIds([])}
                      className="px-3.5 py-2 rounded-xl bg-white border border-slate-300 text-slate-700 text-xs font-extrabold hover:bg-slate-100 cursor-pointer"
                    >
                      Restore Declined Order Pings ({declinedOrderIds.length})
                    </button>
                  )}
                </div>
              )}
            </div>
          )}

          {/* =========================================================
              RIDER SUB-TAB 2: ACTIVE TRIP (ISOLATED STEP 1 -> STEP 2 -> STEP 3 + TOGGLEABLE CHAT MODAL)
             ========================================================= */}
          {riderSubTab === 'ACTIVE_TRIP' && (
            <div className="space-y-4">
              {activeAssignedOrders.length > 0 ? (
                activeAssignedOrders.map((order) => {
                  const isStep1Pickup =
                    order.status === 'ASSIGNED' || order.status === 'ARRIVED_AT_PICKUP';
                  const isStep2Transit = order.status === 'PICKED_UP';
                  const isStep3Drop = order.status === 'IN_TRANSIT';
                  const isPhotoCaptured = Boolean(
                    photoCapturedByOrder[order.id] || order.pickupPhotoCaptured
                  );
                  const isCashOrder =
                    order.paymentMethod === 'CASH_ON_DELIVERY' ||
                    order.paymentMethod === 'CASH_AT_PICKUP' ||
                    order.paymentMethod === 'CASH_AT_DROP' ||
                    order.paymentMethod === 'DIRECT_RIDER_UPI';
                  const isChatOpen = riderChatModalOrderId === order.id;

                  return (
                    <div
                      key={order.id}
                      className="p-4 rounded-2xl border border-slate-200 bg-white space-y-3.5 shadow-xs"
                    >
                      {/* Active Trip Top Summary + Toggleable Chat with Customer Button */}
                      <div className="flex flex-wrap items-start justify-between gap-2">
                        <div>
                          <div className="flex flex-wrap items-center gap-1.5">
                            <span className="text-xs font-extrabold px-2 py-0.5 rounded-md bg-emerald-50 text-[#059669] border border-emerald-200">
                              #{order.orderCode}
                            </span>
                            <span className="text-xs font-bold px-2 py-0.5 rounded-md bg-slate-100 text-slate-800">
                              📦 {order.packageCategory || 'Documents'} • {order.weightKg} kg
                            </span>
                            <span className="text-[11px] font-bold px-2 py-0.5 rounded-md bg-amber-50 text-amber-800">
                              {order.distanceKm} km
                            </span>
                          </div>
                          <div className="text-xs text-slate-600 mt-1.5 space-y-0.5">
                            <div>
                              <strong className="text-emerald-700">Pickup:</strong> {order.pickupAddress}
                            </div>
                            <div>
                              <strong className="text-orange-600">Drop:</strong> {order.dropAddress} (
                              {order.receiverName || 'Receiver'})
                            </div>
                          </div>
                        </div>

                        <div className="flex items-center gap-2">
                          <div className="text-right">
                            <div className="text-[10px] font-extrabold uppercase text-emerald-700">
                              80% Earning
                            </div>
                            <div className="text-base font-extrabold text-[#059669]">
                              ₹{(order.riderEarningPaise / 100).toFixed(0)}
                            </div>
                          </div>

                          <button
                            type="button"
                            onClick={() =>
                              setRiderChatModalOrderId(isChatOpen ? null : order.id)
                            }
                            className={`px-3 py-2 rounded-xl text-xs font-extrabold flex items-center gap-1.5 transition cursor-pointer ${
                              isChatOpen
                                ? 'bg-[#042F2E] text-white'
                                : 'bg-slate-100 text-slate-800 hover:bg-slate-200 border border-slate-300'
                            }`}
                          >
                            <MessageSquare className="w-3.5 h-3.5 text-[#059669]" />
                            <span>{isChatOpen ? 'Hide Chat' : '💬 Chat with Customer'}</span>
                          </button>
                        </div>
                      </div>

                      {/* Step Progress Pill Header */}
                      <div className="grid grid-cols-3 gap-1.5 text-[10px] font-extrabold">
                        <div
                          className={`p-2 rounded-lg text-center border ${
                            isStep1Pickup
                              ? 'bg-amber-500 text-white border-amber-600'
                              : 'bg-emerald-50 border-emerald-200 text-emerald-800'
                          }`}
                        >
                          Step 1: Pickup OTP & Photo {isStep1Pickup ? '●' : '✓'}
                        </div>
                        <div
                          className={`p-2 rounded-lg text-center border ${
                            isStep2Transit
                              ? 'bg-blue-600 text-white border-blue-700'
                              : isStep3Drop
                              ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
                              : 'bg-slate-50 border-slate-200 text-slate-400'
                          }`}
                        >
                          Step 2: Map & Timer {isStep2Transit ? '●' : isStep3Drop ? '✓' : ''}
                        </div>
                        <div
                          className={`p-2 rounded-lg text-center border ${
                            isStep3Drop
                              ? 'bg-[#059669] text-white border-emerald-700'
                              : 'bg-slate-50 border-slate-200 text-slate-400'
                          }`}
                        >
                          Step 3: Drop OTP & COD {isStep3Drop ? '●' : ''}
                        </div>
                      </div>

                      {/* --- SHOW ONLY CURRENT ACTIVE STEP CLEARLY --- */}

                      {/* STEP 1 (At Pickup): Pickup OTP & Capture Parcel Photo */}
                      {isStep1Pickup && (
                        <div className="p-3.5 rounded-xl bg-amber-50/80 border border-amber-200 space-y-3">
                          <div className="flex items-center justify-between">
                            <span className="text-xs font-extrabold text-amber-950">
                              Step 1 (At Pickup): Verify Handover & Photo Proof
                            </span>
                            <span className="px-2 py-0.5 rounded bg-amber-200/70 text-amber-950 text-[10px] font-extrabold">
                              Pickup OTP: {order.pickupOtp}
                            </span>
                          </div>

                          {/* Pickup Route Navigation Map for Rider */}
                          <DeliveryMapComponent
                            orderId={order.id}
                            orderCode={order.orderCode}
                            status="ASSIGNED"
                            pickupAddress={order.pickupAddress}
                            dropAddress={order.dropAddress}
                            pickupTag={order.pickupTag || 'Pickup'}
                            dropTag={order.dropTag || 'Drop'}
                            vehicleType={order.vehicleType}
                            riderName="Vikram Singh"
                            riderPlate="KA-01-EQ-4821"
                            isRiderView={true}
                            compact={true}
                          />

                          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                            <input
                              type="text"
                              maxLength={4}
                              placeholder={`Enter 4-Digit Pickup OTP (${order.pickupOtp})`}
                              value={otpInputByOrder[order.id] || ''}
                              onChange={(e) =>
                                setOtpInputByOrder((prev) => ({
                                  ...prev,
                                  [order.id]: e.target.value,
                                }))
                              }
                              className="px-3 py-2 text-xs bg-white border border-amber-300 rounded-xl font-bold focus:outline-none focus:border-[#059669]"
                            />

                            <button
                              type="button"
                              onClick={() => handleCaptureParcelPhoto(order)}
                              className={`px-3 py-2 rounded-xl text-xs font-extrabold flex items-center justify-center gap-1.5 border transition cursor-pointer ${
                                isPhotoCaptured
                                  ? 'bg-emerald-600 text-white border-emerald-600'
                                  : 'bg-white text-slate-800 border-slate-300 hover:bg-slate-100'
                              }`}
                            >
                              <Camera className="w-3.5 h-3.5" />
                              {isPhotoCaptured ? '✓ Parcel Photo Captured' : 'Capture Parcel Photo'}
                            </button>
                          </div>

                          <div className="flex gap-2">
                            <button
                              type="button"
                              onClick={() => handleAdvanceRiderOrder(order)}
                              className="flex-1 py-2.5 rounded-xl bg-[#059669] hover:bg-emerald-600 text-white font-extrabold text-xs transition shadow-xs cursor-pointer"
                            >
                              Verify Pickup OTP & Start Trip (Step 2) →
                            </button>
                            <button
                              type="button"
                              onClick={() => handleReleaseOrderBeforePickup(order)}
                              className="px-3 py-2.5 rounded-xl border border-slate-300 bg-white text-slate-600 text-xs font-bold hover:bg-slate-50 cursor-pointer"
                            >
                              Release
                            </button>
                          </div>
                        </div>
                      )}

                      {/* STEP 2 (In Transit): Map Navigation, Call Receiver & Live Trip Timer */}
                      {isStep2Transit && (
                        <div className="p-3.5 rounded-xl bg-blue-50/80 border border-blue-200 space-y-3">
                          <div className="flex flex-wrap items-center justify-between gap-2">
                            <div className="flex items-center gap-2">
                              <Clock className="w-4 h-4 text-blue-700" />
                              <div>
                                <div className="text-[10px] font-bold uppercase text-blue-700">
                                  Step 2 (In Transit) • Live Trip Timer
                                </div>
                                <div className="text-sm font-extrabold text-blue-950">
                                  {formatTimer(tripTimerSeconds)} elapsed • En Route to Drop
                                </div>
                              </div>
                            </div>

                            <div className="flex items-center gap-1.5">
                              <button
                                type="button"
                                onClick={() => {
                                  const origin = encodeURIComponent(order.pickupAddress);
                                  const dest = encodeURIComponent(order.dropAddress);
                                  const mapsUrl = `https://www.google.com/maps/dir/?api=1&origin=${origin}&destination=${dest}&travelmode=two_wheeler`;
                                  try {
                                    window.open(mapsUrl, '_blank', 'noopener,noreferrer');
                                  } catch {
                                    window.location.href = mapsUrl;
                                  }
                                }}
                                className="px-3 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-700 text-white text-xs font-extrabold flex items-center gap-1 transition shadow-xs cursor-pointer active:scale-95"
                              >
                                <Navigation className="w-3.5 h-3.5 text-blue-100" />
                                Launch Google Maps GPS
                              </button>
                              <button
                                type="button"
                                onClick={() =>
                                  setRiderNotice(
                                    `Calling Receiver ${order.receiverName || 'Rohan Verma'} (+91 ${order.receiverPhone || '9820112233'})...`
                                  )
                                }
                                className="px-3 py-1.5 rounded-lg bg-white border border-blue-300 text-blue-900 text-xs font-extrabold flex items-center gap-1 hover:bg-blue-100 transition cursor-pointer"
                              >
                                <Phone className="w-3.5 h-3.5" />
                                Call Receiver
                              </button>
                            </div>
                          </div>

                          {/* Rider Live GPS Location Broadcasting & Sharing Control */}
                          <div className="p-3 rounded-xl bg-white border border-blue-200 shadow-xs flex flex-wrap items-center justify-between gap-2.5">
                            <div className="flex items-center gap-2.5">
                              <div
                                className={`w-8 h-8 rounded-lg flex items-center justify-center text-white ${
                                  isRiderGpsBroadcasting ? 'bg-emerald-600 animate-pulse' : 'bg-slate-400'
                                }`}
                              >
                                <Radio className="w-4 h-4" />
                              </div>
                              <div>
                                <div className="text-xs font-black text-slate-900 flex items-center gap-1.5">
                                  <span>
                                    {isRiderGpsBroadcasting
                                      ? 'Broadcasting Live GPS Location'
                                      : 'Live GPS Broadcast Paused'}
                                  </span>
                                  <span
                                    className={`text-[9px] font-black uppercase px-1.5 py-0.5 rounded ${
                                      isRiderGpsBroadcasting
                                        ? 'bg-emerald-100 text-emerald-800'
                                        : 'bg-slate-100 text-slate-600'
                                    }`}
                                  >
                                    {isRiderGpsBroadcasting ? 'Live 5G' : 'Paused'}
                                  </span>
                                </div>
                                <div className="text-[10px] text-slate-500 font-mono mt-0.5">
                                  {riderCurrentGps.lat.toFixed(4)}° N, {riderCurrentGps.lng.toFixed(4)}° E • {riderCurrentGps.speedKmh} km/h • Accuracy ±{riderCurrentGps.accuracyMeters}m
                                </div>
                              </div>
                            </div>

                            <button
                              type="button"
                              onClick={() => {
                                setIsRiderGpsBroadcasting((prev) => !prev);
                                setRiderNotice(
                                  !isRiderGpsBroadcasting
                                    ? '🟢 Live GPS location sharing enabled! Customer & Admin can view your real-time movement on map.'
                                    : '⏸️ Live GPS location sharing paused.'
                                );
                              }}
                              className={`px-3 py-1.5 rounded-lg text-xs font-black transition cursor-pointer border ${
                                isRiderGpsBroadcasting
                                  ? 'bg-emerald-50 text-emerald-800 border-emerald-300 hover:bg-emerald-100'
                                  : 'bg-slate-100 text-slate-700 border-slate-300 hover:bg-slate-200'
                              }`}
                            >
                              {isRiderGpsBroadcasting ? '✓ Sharing GPS (ON)' : 'Share Live GPS'}
                            </button>
                          </div>

                          {/* Full Interactive Turn-by-Turn GPS Radar Map for Rider */}
                          <DeliveryMapComponent
                            orderId={order.id}
                            orderCode={order.orderCode}
                            status="IN_TRANSIT"
                            pickupAddress={order.pickupAddress}
                            dropAddress={order.dropAddress}
                            pickupTag={order.pickupTag || 'Pickup'}
                            dropTag={order.dropTag || 'Drop'}
                            vehicleType={order.vehicleType}
                            riderName="Vikram Singh"
                            riderPlate="KA-01-EQ-4821"
                            isRiderView={true}
                            compact={false}
                          />

                          <button
                            type="button"
                            onClick={() => handleAdvanceRiderOrder(order)}
                            className="w-full py-2.5 rounded-xl bg-[#059669] hover:bg-emerald-600 text-white font-extrabold text-xs transition shadow-xs cursor-pointer"
                          >
                            Arrived at Drop Location (Proceed to Step 3: Drop OTP) →
                          </button>
                        </div>
                      )}

                      {/* STEP 3 (At Drop): 4-digit Drop OTP + Collect Cash confirmation if COD */}
                      {isStep3Drop && (
                        <div className="p-3.5 rounded-xl bg-emerald-50/80 border border-emerald-200 space-y-2.5">
                          <div className="flex items-center justify-between">
                            <span className="text-xs font-extrabold text-emerald-950">
                              Step 3 (At Drop): Enter 4-Digit Drop OTP
                            </span>
                            <span className="px-2 py-0.5 rounded bg-emerald-200/70 text-emerald-950 text-[10px] font-extrabold">
                              Drop OTP: {order.deliveryOtp}
                            </span>
                          </div>

                          {isCashOrder && (
                            <label className="flex items-center gap-2 p-2 rounded-lg bg-white border border-amber-300 text-xs font-bold text-slate-800 cursor-pointer">
                              <input
                                type="checkbox"
                                checked={Boolean(codCollectedByOrder[order.id])}
                                onChange={(e) =>
                                  setCodCollectedByOrder((prev) => ({
                                    ...prev,
                                    [order.id]: e.target.checked,
                                  }))
                                }
                                className="rounded accent-[#059669]"
                              />
                              <span>
                                Collect Cash: <strong>₹{(order.totalAmountPaise / 100).toFixed(0)}</strong> (
                                {order.paymentMethod.replace(/_/g, ' ')})
                              </span>
                            </label>
                          )}

                          {/* Delivery Proof Photo Upload */}
                          {deliveryPhotoByOrder[order.id]?.url || order.deliveryPhotoUrl ? (
                            <div className="p-2.5 rounded-xl bg-white border border-emerald-300 flex items-center justify-between gap-3">
                              <div className="flex items-center gap-2.5">
                                <img
                                  src={deliveryPhotoByOrder[order.id]?.url || order.deliveryPhotoUrl}
                                  alt="Delivery Proof"
                                  className="w-12 h-12 rounded-lg object-cover border border-emerald-300"
                                />
                                <div>
                                  <div className="text-xs font-extrabold text-emerald-950 flex items-center gap-1">
                                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                                    <span>Delivery Proof Photo Attached</span>
                                  </div>
                                  <div className="text-[10px] text-slate-500">
                                    {deliveryPhotoByOrder[order.id]?.note || 'Geotagged POD verified'}
                                  </div>
                                </div>
                              </div>
                              <button
                                type="button"
                                onClick={() =>
                                  setImageUploadModalState({
                                    isOpen: true,
                                    category: 'DELIVERY_PROOF',
                                    title: `Delivery Proof Photo — #${order.orderCode}`,
                                    subtitle: 'Upload parcel at doorstep or recipient handover photo',
                                    orderId: order.id,
                                    currentImageUrl: deliveryPhotoByOrder[order.id]?.url || order.deliveryPhotoUrl,
                                  })
                                }
                                className="px-2.5 py-1 text-xs font-bold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-lg transition"
                              >
                                Retake / Change
                              </button>
                            </div>
                          ) : (
                            <button
                              type="button"
                              onClick={() =>
                                setImageUploadModalState({
                                  isOpen: true,
                                  category: 'DELIVERY_PROOF',
                                  title: `Delivery Proof Photo — #${order.orderCode}`,
                                  subtitle: 'Upload parcel at doorstep or recipient handover photo',
                                  orderId: order.id,
                                })
                              }
                              className="w-full py-2.5 px-3 rounded-xl border border-dashed border-emerald-400 bg-white hover:bg-emerald-50 text-emerald-900 text-xs font-extrabold flex items-center justify-center gap-2 transition cursor-pointer"
                            >
                              <Camera className="w-4 h-4 text-emerald-700" />
                              <span>📸 Capture / Upload Delivery Proof Photo (Required for POD)</span>
                            </button>
                          )}

                          <div className="flex gap-2">
                            <input
                              type="text"
                              maxLength={4}
                              placeholder={`Enter 4-Digit Drop OTP (${order.deliveryOtp})`}
                              value={otpInputByOrder[order.id] || ''}
                              onChange={(e) =>
                                setOtpInputByOrder((prev) => ({
                                  ...prev,
                                  [order.id]: e.target.value,
                                }))
                              }
                              className="flex-1 px-3 py-2 text-xs bg-white border border-emerald-300 rounded-xl font-bold focus:outline-none focus:border-[#059669]"
                            />
                            <button
                              type="button"
                              onClick={() => handleAdvanceRiderOrder(order)}
                              className="px-4 py-2 rounded-xl bg-[#059669] hover:bg-emerald-600 text-white font-extrabold text-xs transition shadow-xs cursor-pointer"
                            >
                              Complete Delivery (Credit 80%)
                            </button>
                          </div>
                        </div>
                      )}

                      {/* Toggleable Customer Chat Modal/Drawer */}
                      {isChatOpen && (
                        <div className="p-3 rounded-xl bg-slate-50 border-2 border-[#059669] space-y-2">
                          <div className="flex items-center justify-between">
                            <span className="text-xs font-extrabold text-slate-800">
                              Customer Chat ({order.customerName})
                            </span>
                            <button
                              type="button"
                              onClick={() => setRiderChatModalOrderId(null)}
                              className="text-slate-400 hover:text-slate-700"
                            >
                              <X className="w-3.5 h-3.5" />
                            </button>
                          </div>
                          <div className="max-h-28 overflow-y-auto space-y-1.5">
                            {chatMessages
                              .filter((m) => m.orderId === order.id)
                              .map((m) => (
                                <div
                                  key={m.id}
                                  className={`text-xs px-2.5 py-1.5 rounded-lg ${
                                    m.senderRole === 'RIDER'
                                      ? 'bg-emerald-100 text-emerald-950 ml-4'
                                      : 'bg-white text-slate-800 mr-4 border border-slate-200'
                                  }`}
                                >
                                  <span className="font-bold">{m.senderName}: </span>
                                  {m.messageText}
                                </div>
                              ))}
                          </div>
                          <div className="flex gap-1.5 pt-1">
                            <input
                              type="text"
                              placeholder="Message customer / receiver..."
                              value={riderChatDraft}
                              onChange={(e) => setRiderChatDraft(e.target.value)}
                              className="flex-1 px-2.5 py-1.5 text-xs border border-slate-200 rounded-xl bg-white"
                            />
                            <button
                              type="button"
                              onClick={() => handleSendChat(order.id, 'RIDER')}
                              className="px-3 py-1.5 rounded-xl bg-[#0D1B2A] text-white text-xs font-semibold cursor-pointer"
                            >
                              Send
                            </button>
                          </div>
                        </div>
                      )}

                      {order.pickupProofNote && (
                        <div className="text-[11px] text-emerald-800 bg-emerald-50 border border-emerald-200 px-2.5 py-1.5 rounded-lg flex items-center gap-1.5 font-semibold">
                          <Camera className="w-3.5 h-3.5 shrink-0 text-[#059669]" />
                          {order.pickupProofNote}
                        </div>
                      )}
                    </div>
                  );
                })
              ) : (
                <div className="p-8 text-center rounded-2xl bg-slate-50 border border-slate-200 space-y-3">
                  <Navigation className="w-8 h-8 text-slate-400 mx-auto" />
                  <div className="text-sm font-extrabold text-slate-900">No Active Trip Assigned</div>
                  <p className="text-xs text-slate-500">
                    Accept an incoming order dispatch card in Tab 1 (Duty & Radar) to start step-by-step delivery execution.
                  </p>
                  <button
                    type="button"
                    onClick={() => setRiderSubTab('DUTY_RADAR')}
                    className="px-4 py-2 rounded-xl bg-[#059669] text-white text-xs font-extrabold cursor-pointer"
                  >
                    Go to Duty & Radar
                  </button>
                </div>
              )}
            </div>
          )}

          {/* =========================================================
              RIDER SUB-TAB 3: TRIP & DELIVERY HISTORY
             ========================================================= */}
          {riderSubTab === 'TRIP_HISTORY' && (
            <RiderTripHistoryView
              orders={orders as any}
              currentRiderId="RIDER-101"
              onTrackOrder={(trip) => setSelectedTrackingOrder(trip as any)}
              onDownloadGstInvoice={(trip) => handleDownloadGstInvoice(trip as any)}
            />
          )}

          {/* =========================================================
              RIDER SUB-TAB 4: EARNINGS & WALLET (80% SHARE, INSTANT UPI PAYOUT, COD LIMIT BAR & TRIP LEDGER)
             ========================================================= */}
          {riderSubTab === 'EARNINGS_WALLET' && (
            <div className="space-y-4">
              {/* Today's Net Earnings (80% Share), Completed Trips, Wallet & Instant UPI Payout */}
              <div className="grid grid-cols-3 gap-2">
                <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200">
                  <div className="text-[10px] text-emerald-800 font-bold uppercase">
                    Today&apos;s Net (80% Share)
                  </div>
                  <div className="text-base sm:text-lg font-extrabold text-[#042F2E] mt-0.5">
                    ₹{(riderWallet.todayNetEarningsPaise / 100).toFixed(0)}
                  </div>
                  <div className="text-[10px] text-emerald-700 font-semibold">80% Instant Credit</div>
                </div>

                <div className="p-3 rounded-xl bg-slate-50 border border-slate-200">
                  <div className="text-[10px] text-slate-500 font-bold uppercase">Completed Trips</div>
                  <div className="text-base sm:text-lg font-extrabold text-slate-900 mt-0.5">
                    {riderWallet.completedTrips} Trips
                  </div>
                  <div className="text-[10px] text-amber-700 font-semibold">Rating 4.9★ • Top Partner</div>
                </div>

                <div className="p-3 rounded-xl bg-[#0D1B2A] text-white flex flex-col justify-between">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] text-[#A7F3D0] font-bold uppercase">Wallet</span>
                    <span className="text-xs font-extrabold text-white">
                      ₹{(riderWallet.balancePaise / 100).toFixed(0)}
                    </span>
                  </div>
                  <button
                    type="button"
                    onClick={handleInstantUpiPayout}
                    className="mt-1.5 w-full py-1.5 px-2 rounded-lg bg-[#059669] hover:bg-emerald-500 text-white text-[10px] font-extrabold transition shadow-xs cursor-pointer"
                  >
                    ⚡ Instant UPI Payout
                  </button>
                </div>
              </div>

              {/* Cash-in-Hand (COD) ₹150 Limit Bar & Settlement */}
              <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200 space-y-2.5">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-xs font-extrabold text-slate-800">
                      Cash-in-Hand (COD) ₹150 Limit Bar
                    </div>
                    <div className="text-[11px] text-slate-500">
                      Auto-blocks new COD dispatches if unpaid 20% fee exceeds ₹150
                    </div>
                  </div>
                  <div className="text-right">
                    <div
                      className={`text-xs font-extrabold ${
                        riderWallet.commissionDebtPaise > MAX_COMMISSION_DEBT_PAISE
                          ? 'text-red-600'
                          : 'text-amber-700'
                      }`}
                    >
                      ₹{debtInr.toFixed(2)} / ₹150.00 Limit
                    </div>
                  </div>
                </div>

                <div className="w-full h-2.5 bg-slate-200 rounded-full overflow-hidden">
                  <div
                    className={`h-full transition-all ${
                      debtPercent >= 100 ? 'bg-red-600' : 'bg-[#059669]'
                    }`}
                    style={{ width: `${debtPercent}%` }}
                  />
                </div>

                <div className="flex items-center justify-between text-xs pt-0.5">
                  <span className="text-slate-600 font-medium">
                    COD Status:{' '}
                    <strong className="text-emerald-700">
                      {debtPercent < 100 ? 'Eligible for All Orders' : 'Limit Reached'}
                    </strong>
                  </span>
                  {riderWallet.commissionDebtPaise > 0 && (
                    <button
                      type="button"
                      onClick={handleRepayRiderDebt}
                      className="px-3 py-1 rounded-lg bg-[#0D1B2A] text-white font-bold hover:bg-slate-800 transition cursor-pointer"
                    >
                      Settle ₹{debtInr.toFixed(0)} COD via UPI
                    </button>
                  )}
                </div>
              </div>

              {/* Completed Trip Ledger */}
              <div className="space-y-2">
                <div className="text-xs font-extrabold uppercase tracking-wider text-slate-700">
                  Completed Trip Ledger ({completedOrders.length})
                </div>
                {completedOrders.length > 0 ? (
                  completedOrders.map((ord) => (
                    <div
                      key={ord.id}
                      className="py-2.5 px-3.5 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-900 text-xs font-extrabold flex items-center justify-between"
                    >
                      <span>
                        ✓ #{ord.orderCode} • {ord.pickupTag} → {ord.dropTag} ({ord.distanceKm} km)
                      </span>
                      <span>
                        +₹{(ord.riderEarningPaise / 100).toFixed(0)} (80% Credited) • 20% Admin Synced
                      </span>
                    </div>
                  ))
                ) : (
                  <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-500 text-center">
                    Complete your active delivery to see instant 80% trip settlements here.
                  </div>
                )}
              </div>
            </div>
          )}

          {/* =========================================================
              RIDER SUB-TAB 4: PROFILE, KYC & SUPPORT
             ========================================================= */}
          {riderSubTab === 'PROFILE_SUPPORT' && (
            <div className="space-y-4">
              {/* Rider Profile Card */}
              <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-3">
                  <div className="relative">
                    {currentUser.avatarUrl ? (
                      <img
                        src={currentUser.avatarUrl}
                        alt={riderWallet.riderName}
                        className="w-14 h-14 rounded-2xl object-cover border-2 border-emerald-500 shadow-sm"
                      />
                    ) : (
                      <div className="w-14 h-14 rounded-2xl bg-[#042F2E] text-[#A7F3D0] flex items-center justify-center font-extrabold text-lg border-2 border-emerald-500/30">
                        {currentUser.avatarText || 'VS'}
                      </div>
                    )}
                    <button
                      type="button"
                      onClick={() =>
                        setImageUploadModalState({
                          isOpen: true,
                          category: 'PROFILE_PHOTO',
                          title: 'Update Rider Partner Photo',
                          subtitle: 'Upload a clear headshot or take a live selfie for rider ID card & customer verification.',
                          currentImageUrl: currentUser.avatarUrl,
                        })
                      }
                      className="absolute -bottom-1 -right-1 w-6 h-6 rounded-full bg-[#059669] text-white flex items-center justify-center shadow hover:bg-emerald-600 transition"
                      title="Upload new Rider photo"
                    >
                      <Camera className="w-3.5 h-3.5" />
                    </button>
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-extrabold text-slate-900">
                        {riderWallet.riderName}
                      </span>
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-emerald-100 text-emerald-900">
                        {riderWallet.riderId}
                      </span>
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-amber-100 text-amber-900">
                        4.9★ Rating
                      </span>
                    </div>
                    <div className="text-xs text-slate-600 mt-0.5">
                      Vehicle Plate: <strong>KA-01-EQ-4821</strong> • Phone: +91 {riderWallet.phone}
                    </div>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() =>
                    setImageUploadModalState({
                      isOpen: true,
                      category: 'PROFILE_PHOTO',
                      title: 'Update Rider Partner Photo',
                      subtitle: 'Upload a clear headshot or take a live selfie for rider ID card & customer verification.',
                      currentImageUrl: currentUser.avatarUrl,
                    })
                  }
                  className="px-3 py-1.5 rounded-xl bg-white border border-slate-300 hover:border-emerald-500 hover:bg-emerald-50 text-slate-700 hover:text-emerald-900 text-xs font-bold flex items-center gap-1.5 transition shadow-2xs"
                >
                  <Camera className="w-3.5 h-3.5 text-[#059669]" />
                  <span>Update Photo</span>
                </button>
              </div>

              {/* KYC Status Checklist */}
              <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200 space-y-2.5">
                <div className="text-xs font-extrabold uppercase tracking-wider text-slate-700 flex items-center gap-1.5">
                  <BadgeCheck className="w-4 h-4 text-[#059669]" />
                  KYC Verification Status
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
                  {[
                    'Aadhaar & PAN e-KYC Verified',
                    'Commercial Driving License Active',
                    '2-Wheeler RC & Insurance Verified',
                    'UPI Settlement Bank Account Active',
                  ].map((item) => (
                    <div
                      key={item}
                      className="p-2.5 rounded-xl bg-white border border-emerald-200 flex items-center gap-2 font-bold text-emerald-950"
                    >
                      <CheckCircle2 className="w-4 h-4 text-[#059669] shrink-0" />
                      <span>{item}</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* 24x7 Admin Dispatch Support */}
              <div className="p-3.5 rounded-2xl bg-slate-900 text-white flex flex-wrap items-center justify-between gap-2 border border-slate-800">
                <div className="flex items-center gap-2">
                  <ShieldCheck className="w-4 h-4 text-[#A7F3D0]" />
                  <div>
                    <div className="text-xs font-extrabold">24×7 Dispatch Support &amp; Partner Helpdesk</div>
                    <div className="text-[10px] text-slate-400">
                      Priority fleet assistance for active orders, KYC &amp; settlements
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() =>
                      setRiderNotice(
                        '🎧 Admin Helpdesk ticket opened! Fleet Dispatcher connected on priority channel.'
                      )
                    }
                    className="px-3 py-1.5 rounded-xl bg-white/10 hover:bg-white/20 text-white text-xs font-extrabold flex items-center gap-1 transition border border-white/15 cursor-pointer"
                  >
                    <LifeBuoy className="w-3.5 h-3.5 text-[#A7F3D0]" />
                    Admin Helpdesk
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    );
  };

  // --- ADMIN WEB ENTERPRISE COMMAND CENTER (QUICK DELIVERY EMERALD, MINT & SLATE THEME) ---
  const [adminMobileSidebarOpen, setAdminMobileSidebarOpen] = useState(false);
  const [adminActiveSection, setAdminActiveSection] = useState<
    | 'OVERVIEW'
    | 'APP_CONTROL_UPDATES'
    | 'LIVE_ORDERS'
    | 'LIVE_TRACKING'
    | 'RIDERS'
    | 'CUSTOMERS'
    | 'KYC_VERIFICATION'
    | 'PAYMENTS_SETTLEMENTS'
  >('OVERVIEW');
  type AdminOrderStatusFilter =
    | 'ALL'
    | 'PENDING_ASSIGNMENT'
    | 'ASSIGNED'
    | 'IN_TRANSIT'
    | 'DELIVERED';
  const [adminOrderStatusFilter, setAdminOrderStatusFilter] =
    useState<AdminOrderStatusFilter>('ALL');
  const [adminCityFilter, setAdminCityFilter] = useState('All Cities • Bengaluru');
  const [adminTimeFilter, setAdminTimeFilter] = useState('All Time');
  const [adminSearchQuery, setAdminSearchQuery] = useState('');
  const [adminTrackedOrderId, setAdminTrackedOrderId] = useState<string | null>(null);

  const ADMIN_NAV_ITEMS: {
    id:
      | 'OVERVIEW'
      | 'APP_CONTROL_UPDATES'
      | 'LIVE_ORDERS'
      | 'LIVE_TRACKING'
      | 'RIDERS'
      | 'CUSTOMERS'
      | 'KYC_VERIFICATION'
      | 'PAYMENTS_SETTLEMENTS';
    label: string;
    badge?: string;
  }[] = [
    { id: 'OVERVIEW', label: 'Overview' },
    { id: 'APP_CONTROL_UPDATES', label: 'App Settings & Play Updates', badge: 'LIVE SYNC' },
    { id: 'LIVE_ORDERS', label: 'Live Orders', badge: String(orders.length) },
    {
      id: 'LIVE_TRACKING',
      label: 'Live Tracking',
      badge: String(orders.filter((o) => o.status !== 'DELIVERED' && o.status !== 'CANCELLED').length),
    },
    { id: 'RIDERS', label: 'Riders', badge: '1 Online' },
    { id: 'CUSTOMERS', label: 'Customers', badge: String(orders.length) },
    { id: 'KYC_VERIFICATION', label: 'KYC Verification', badge: 'Verified' },
    { id: 'PAYMENTS_SETTLEMENTS', label: 'Payments & Settlements', badge: String(ledger.length) },
  ];

  const renderAdminPanel = () => {
    const filteredAdminOrders = orders.filter((o) => {
      // 1. Filter by Status Pill
      if (adminOrderStatusFilter !== 'ALL') {
        if (adminOrderStatusFilter === 'PENDING_ASSIGNMENT') {
          if (o.status !== 'PENDING_ASSIGNMENT') return false;
        } else if (adminOrderStatusFilter === 'ASSIGNED') {
          if (o.status !== 'ASSIGNED' && o.status !== 'ARRIVED_AT_PICKUP') return false;
        } else if (adminOrderStatusFilter === 'IN_TRANSIT') {
          if (o.status !== 'IN_TRANSIT' && o.status !== 'PICKED_UP') return false;
        } else if (adminOrderStatusFilter === 'DELIVERED') {
          if (o.status !== 'DELIVERED') return false;
        }
      }

      // 2. Filter by Search Query
      const q = adminSearchQuery.trim().toLowerCase();
      if (!q) return true;
      return (
        o.orderCode.toLowerCase().includes(q) ||
        o.customerName.toLowerCase().includes(q) ||
        (o.assignedRiderName || '').toLowerCase().includes(q) ||
        o.pickupAddress.toLowerCase().includes(q) ||
        o.dropAddress.toLowerCase().includes(q)
      );
    });

    const totalGrossInr = Math.round(orders.reduce((acc, o) => acc + o.totalAmountPaise, 0) / 100);
    const totalCommissionInr = Math.round(
      orders.reduce((acc, o) => acc + o.companyCommissionPaise, 0) / 100
    );
    const totalRiderShareInr = Math.round(
      orders.reduce((acc, o) => acc + o.riderEarningPaise, 0) / 100
    );

    const searchingCount = orders.filter((o) => o.status === 'PENDING_ASSIGNMENT').length;
    const assignedCount = orders.filter(
      (o) => o.status === 'ASSIGNED' || o.status === 'ARRIVED_AT_PICKUP'
    ).length;
    const inTransitCount = orders.filter(
      (o) => o.status === 'PICKED_UP' || o.status === 'IN_TRANSIT'
    ).length;
    const deliveredCount = orders.filter((o) => o.status === 'DELIVERED').length;
    const activeOrdersCount = searchingCount + assignedCount + inTransitCount;
    const totalOrdersCount = Math.max(1, orders.length);

    return (
      <div className="bg-[#F1F5F9] rounded-2xl shadow-md border border-slate-300 overflow-hidden flex flex-col relative">
        {/* 1. ENTERPRISE COMMAND HEADER & QUICK NAVIGATION */}
        <div className="bg-[#022C22] text-white border-b border-emerald-900">
          {/* Top Command Row */}
          <div className="px-4 py-3 flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              {/* Top-left ☰ Hamburger Icon (opens slide-over drawer on mobile < 768px) */}
              <button
                type="button"
                onClick={() => setAdminMobileSidebarOpen(true)}
                aria-label="Open Navigation Menu"
                className="md:hidden w-9 h-9 rounded-lg bg-white/10 hover:bg-white/20 border border-emerald-300/30 flex items-center justify-center text-white font-bold text-lg transition"
              >
                ☰
              </button>

              {/* QD - Quick Delivery | ADMIN WEB ENTERPRISE */}
              <div className="w-9 h-9 rounded-lg bg-[#059669] border border-[#A7F3D0] flex items-center justify-center font-extrabold text-sm tracking-tight shadow-xs">
                QD
              </div>
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <span className="font-extrabold text-sm sm:text-base tracking-tight text-white">
                    QD - Quick Delivery | ADMIN WEB ENTERPRISE
                  </span>
                  {/* Live Pulsing SYSTEM ONLINE Indicator */}
                  <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-extrabold bg-emerald-500/20 text-[#A7F3D0] border border-emerald-400/50">
                    <span className="w-2 h-2 rounded-full bg-[#10B981] animate-ping" />
                    <span className="w-2 h-2 rounded-full bg-[#A7F3D0] -ml-3.5" />
                    SYSTEM ONLINE
                  </span>
                </div>
                <p className="text-[11px] text-[#A7F3D0]/85 font-medium">
                  Enterprise Logistics & Hyperlocal Delivery Command Center • 80% Rider / 20% Admin Split
                </p>
              </div>
            </div>

            {/* City / Time Selector Pill + Actions */}
            <div className="flex flex-wrap items-center gap-2">
              <button
                type="button"
                onClick={() =>
                  setAdminTimeFilter((prev) => (prev === 'All Time' ? 'Today Live' : 'All Time'))
                }
                className="px-3 py-1.5 rounded-full bg-white/10 hover:bg-white/15 border border-[#A7F3D0]/40 text-xs font-bold text-white flex items-center gap-1.5 transition"
              >
                <Clock className="w-3.5 h-3.5 text-[#A7F3D0]" />
                <span>
                  {adminCityFilter} • {adminTimeFilter}
                </span>
              </button>

              <button
                type="button"
                onClick={handleExportCsv}
                className="px-3 py-1.5 rounded-lg bg-white/10 hover:bg-white/20 border border-emerald-300/30 text-white text-xs font-bold flex items-center gap-1.5 transition"
              >
                <Download className="w-3.5 h-3.5 text-[#A7F3D0]" />
                Export CSV
              </button>
            </div>
          </div>

          {/* Search Bar Row */}
          <div className="px-4 pb-2.5 flex items-center gap-2">
            <input
              type="text"
              value={adminSearchQuery}
              onChange={(e) => setAdminSearchQuery(e.target.value)}
              placeholder="Global Search Order ID, Customer, Rider, Pickup/Drop Hub..."
              className="flex-1 px-3 py-1.5 rounded-lg bg-white/10 border border-emerald-400/30 text-xs text-white placeholder-emerald-200/60 focus:outline-none focus:border-[#A7F3D0]"
            />
          </div>

          {/* Smooth Horizontally Scrollable Quick-Tab Bar */}
          <div className="bg-[#042F2E] px-4 py-2 flex items-center gap-2 overflow-x-auto no-scrollbar border-t border-emerald-900/80">
            {ADMIN_NAV_ITEMS.map((item) => {
              const active = adminActiveSection === item.id;
              return (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => {
                    setAdminActiveSection(item.id);
                    if (item.id === 'LIVE_TRACKING') {
                      setTimeout(() => {
                        document.getElementById('admin-fleet-radar')?.scrollIntoView({ behavior: 'smooth' });
                      }, 50);
                    }
                  }}
                  className={`shrink-0 px-3 py-1.5 rounded-lg text-xs font-bold transition flex items-center gap-1.5 border ${
                    active
                      ? 'bg-[#059669] text-white border-[#A7F3D0] shadow-xs'
                      : 'bg-white/5 text-emerald-100 border-white/10 hover:bg-white/10'
                  }`}
                >
                  <span>{item.label}</span>
                  {item.badge && (
                    <span
                      className={`px-1.5 py-0.2 rounded text-[10px] font-extrabold ${
                        active ? 'bg-white/20 text-white' : 'bg-[#022C22] text-[#A7F3D0]'
                      }`}
                    >
                      {item.badge}
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        </div>

        {/* MOBILE SLIDE-OVER OVERLAY DRAWER (< 768px) WITH DARK BACKDROP & CLOSE BUTTON */}
        {adminMobileSidebarOpen && (
          <div
            className="fixed inset-0 z-50 md:hidden bg-black/60 backdrop-blur-xs flex"
            onClick={() => setAdminMobileSidebarOpen(false)}
          >
            <div
              className="w-72 max-w-[82vw] bg-[#042F2E] text-white h-full p-4 flex flex-col justify-between shadow-2xl border-r border-emerald-800"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="space-y-4">
                <div className="flex items-center justify-between pb-3 border-b border-emerald-800">
                  <div>
                    <div className="text-xs font-extrabold tracking-wider text-[#A7F3D0]">
                      NAVIGATION MENU
                    </div>
                    <div className="text-[11px] text-emerald-200/70">
                      Quick Delivery Enterprise
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => setAdminMobileSidebarOpen(false)}
                    className="px-2.5 py-1 rounded-lg bg-white/10 hover:bg-white/20 border border-emerald-300/30 text-xs font-bold text-white"
                  >
                    ✕ Close
                  </button>
                </div>

                <nav className="space-y-1.5">
                  {ADMIN_NAV_ITEMS.map((item) => {
                    const active = adminActiveSection === item.id;
                    return (
                      <button
                        key={item.id}
                        type="button"
                        onClick={() => {
                          setAdminActiveSection(item.id);
                          setAdminMobileSidebarOpen(false);
                        }}
                        className={`w-full px-3 py-2.5 rounded-xl text-left text-xs font-bold flex items-center justify-between transition ${
                          active
                            ? 'bg-[#059669] text-white shadow-xs'
                            : 'text-emerald-100 hover:bg-white/10'
                        }`}
                      >
                        <span>{item.label}</span>
                        {item.badge && (
                          <span className="px-2 py-0.5 rounded-md text-[10px] font-extrabold bg-[#022C22] text-[#A7F3D0]">
                            {item.badge}
                          </span>
                        )}
                      </button>
                    );
                  })}
                </nav>
              </div>

              <div className="pt-3 border-t border-emerald-800 text-[11px] text-[#A7F3D0]/80">
                🟢 Firestore Security Rules Active • ₹150 Debt Cap Enforced
              </div>
            </div>
          </div>
        )}

        {/* 2. MAIN WORKSPACE: Desktop Left Sidebar (>= 768px) + Full-Width Mobile / Multi-Column Desktop Content */}
        <div className="flex flex-1 min-h-[540px]">
          {/* Desktop Sidebar (Hidden on mobile < 768px so dashboard gets 100% full width) */}
          <aside className="hidden md:flex md:w-56 bg-[#042F2E] text-white p-3.5 flex-col justify-between border-r border-emerald-900 shrink-0">
            <div className="space-y-3">
              <div className="px-2 py-1">
                <div className="text-[11px] font-extrabold tracking-wider text-[#A7F3D0]">
                  NAVIGATION MENU
                </div>
                <div className="text-[10px] text-emerald-200/70">
                  Quick Delivery Logistics SaaS
                </div>
              </div>

              <nav className="space-y-1">
                {ADMIN_NAV_ITEMS.map((item) => {
                  const active = adminActiveSection === item.id;
                  return (
                    <button
                      key={item.id}
                      type="button"
                      onClick={() => {
                        setAdminActiveSection(item.id);
                        if (item.id === 'LIVE_TRACKING') {
                          setTimeout(() => {
                            document.getElementById('admin-fleet-radar')?.scrollIntoView({ behavior: 'smooth' });
                          }, 50);
                        }
                      }}
                      className={`w-full px-3 py-2 rounded-xl text-left text-xs font-bold flex items-center justify-between transition ${
                        active
                          ? 'bg-[#059669] text-white shadow-xs'
                          : 'text-emerald-100/90 hover:bg-white/10'
                      }`}
                    >
                      <span>{item.label}</span>
                      {item.badge && (
                        <span
                          className={`px-1.5 py-0.5 rounded-md text-[10px] font-extrabold ${
                            active ? 'bg-white/20 text-white' : 'bg-[#022C22] text-[#A7F3D0]'
                          }`}
                        >
                          {item.badge}
                        </span>
                      )}
                    </button>
                  );
                })}
              </nav>
            </div>

            <div className="p-2.5 rounded-xl bg-[#022C22] border border-emerald-800/80 space-y-1">
              <div className="text-[11px] font-bold text-[#A7F3D0]">Fleet SLA Guard</div>
              <div className="text-[10px] text-emerald-100/80">
                99.4% On-Time • Max 20 kg Parcel Policy • Zero-Trust Rules
              </div>
            </div>
          </aside>

          {/* Main Dashboard Workspace (100% full screen width on mobile) */}
          <div className="flex-1 p-4 sm:p-5 space-y-5 overflow-y-auto">
            {/* 3. KPI STAT CARDS (2-Column Grid on Mobile, 3 or 6 Columns on Desktop — Zero "..." Text Truncation) */}
            <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
              {/* Card 1: Total GMV */}
              <div className="p-3.5 rounded-xl bg-white border border-emerald-500/30 shadow-xs flex flex-col justify-between">
                <div className="flex items-start justify-between gap-2">
                  <span className="text-xs font-extrabold text-slate-700 leading-snug">
                    Total GMV
                  </span>
                  <span className="px-2 py-0.5 rounded text-[10px] font-extrabold bg-emerald-50 text-[#059669] border border-emerald-200 shrink-0">
                    100% GMV
                  </span>
                </div>
                <div className="text-lg sm:text-xl font-extrabold text-[#0F172A] mt-1.5">
                  ₹{totalGrossInr}
                </div>
                <div className="text-[11px] font-semibold text-[#059669] mt-1 leading-snug">
                  {orders.length} Orders • {adminTimeFilter}
                </div>
              </div>

              {/* Card 2: 20% Platform Commission */}
              <div className="p-3.5 rounded-xl bg-white border border-amber-500/30 shadow-xs flex flex-col justify-between">
                <div className="flex items-start justify-between gap-2">
                  <span className="text-xs font-extrabold text-slate-700 leading-snug">
                    20% Platform Commission
                  </span>
                  <span className="px-2 py-0.5 rounded text-[10px] font-extrabold bg-amber-50 text-amber-700 border border-amber-200 shrink-0">
                    20% ADMIN
                  </span>
                </div>
                <div className="text-lg sm:text-xl font-extrabold text-[#0F172A] mt-1.5">
                  ₹{totalCommissionInr}
                </div>
                <div className="text-[11px] font-semibold text-amber-700 mt-1 leading-snug">
                  Net Platform Revenue Share
                </div>
              </div>

              {/* Card 3: 80% Rider Share */}
              <div className="p-3.5 rounded-xl bg-white border border-emerald-500/30 shadow-xs flex flex-col justify-between">
                <div className="flex items-start justify-between gap-2">
                  <span className="text-xs font-extrabold text-slate-700 leading-snug">
                    80% Rider Share
                  </span>
                  <span className="px-2 py-0.5 rounded text-[10px] font-extrabold bg-emerald-50 text-emerald-700 border border-emerald-200 shrink-0">
                    80% RIDER
                  </span>
                </div>
                <div className="text-lg sm:text-xl font-extrabold text-[#0F172A] mt-1.5">
                  ₹{totalRiderShareInr}
                </div>
                <div className="text-[11px] font-semibold text-emerald-700 mt-1 leading-snug">
                  Direct Partner Payout + 100% Tips
                </div>
              </div>

              {/* Card 4: Live Active Orders */}
              <div className="p-3.5 rounded-xl bg-white border border-orange-500/30 shadow-xs flex flex-col justify-between">
                <div className="flex items-start justify-between gap-2">
                  <span className="text-xs font-extrabold text-slate-700 leading-snug">
                    Live Active Orders
                  </span>
                  <span className="px-2 py-0.5 rounded text-[10px] font-extrabold bg-orange-50 text-orange-700 border border-orange-200 shrink-0">
                    DISPATCH
                  </span>
                </div>
                <div className="text-base sm:text-lg font-extrabold text-[#0F172A] mt-1.5">
                  {activeOrdersCount} Active / {deliveredCount} Done
                </div>
                <div className="text-[11px] font-semibold text-orange-600 mt-1 leading-snug">
                  {searchingCount} Searching • {inTransitCount} In Transit
                </div>
              </div>

              {/* Card 5: Online Riders Fleet */}
              <div className="p-3.5 rounded-xl bg-white border border-teal-600/30 shadow-xs flex flex-col justify-between">
                <div className="flex items-start justify-between gap-2">
                  <span className="text-xs font-extrabold text-slate-700 leading-snug">
                    Online Riders Fleet
                  </span>
                  <span className="px-2 py-0.5 rounded text-[10px] font-extrabold bg-teal-50 text-teal-800 border border-teal-200 shrink-0">
                    FLEET GPS
                  </span>
                </div>
                <div className="text-base sm:text-lg font-extrabold text-[#0F172A] mt-1.5">
                  {riderWallet.shiftStatus === 'ONLINE_ACTIVE' ? '1 / 1 Online' : '0 / 1 Online'}
                </div>
                <div className="text-[11px] font-semibold text-teal-700 mt-1 leading-snug">
                  {riderWallet.riderName} • KYC & Selfie Verified
                </div>
              </div>

              {/* Card 6: SLA Compliance */}
              <div className="p-3.5 rounded-xl bg-white border border-emerald-500/30 shadow-xs flex flex-col justify-between">
                <div className="flex items-start justify-between gap-2">
                  <span className="text-xs font-extrabold text-slate-700 leading-snug">
                    SLA Compliance
                  </span>
                  <span className="px-2 py-0.5 rounded text-[10px] font-extrabold bg-emerald-50 text-emerald-700 border border-emerald-200 shrink-0">
                    0 SOS
                  </span>
                </div>
                <div className="text-base sm:text-lg font-extrabold text-[#0F172A] mt-1.5">
                  99.4% On-Time SLA
                </div>
                <div className="text-[11px] font-semibold text-emerald-700 mt-1 leading-snug">
                  Debt ₹{(riderWallet.commissionDebtPaise / 100).toFixed(0)}/₹150 • Bat{' '}
                  {riderWallet.batteryPercent}%
                </div>
              </div>
            </div>

            {adminConfigSavedNotice && (
              <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-300 text-xs font-extrabold text-[#042F2E] flex items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-[#059669] shrink-0" />
                  <span>{adminConfigSavedNotice}</span>
                </div>
                <button
                  type="button"
                  onClick={() => setAdminConfigSavedNotice(null)}
                  className="text-emerald-700 hover:text-emerald-950 p-1"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </div>
            )}

            {/* =====================================================================
                DYNAMIC APP CONTROL & PLAY STORE UPDATE MANAGER (REAL-TIME LIVE SYNC)
               ===================================================================== */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
              {/* 1. INSTANT LIVE CONFIG CONTROL (UPDATES APPS WITHOUT PLAY STORE UPDATE) */}
              <div className="p-4 rounded-2xl bg-white border-2 border-emerald-500/40 shadow-xs space-y-3.5">
                <div className="flex flex-wrap items-start justify-between gap-2 pb-2 border-b border-slate-200">
                  <div>
                    <div className="flex items-center gap-2">
                      <Sliders className="w-4 h-4 text-[#059669]" />
                      <h3 className="text-xs sm:text-sm font-extrabold text-[#042F2E]">
                        App Settings &amp; Pricing Control (Instant Live Config)
                      </h3>
                    </div>
                    <p className="text-[11px] text-slate-500 mt-0.5">
                      Changes sync INSTANTLY in real-time to Customer App &amp; Rider Partner App without Play Store reinstall
                    </p>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    <button
                      type="button"
                      onClick={() => setShowUpiQrModal(true)}
                      className="px-2.5 py-1 rounded-full bg-gradient-to-r from-[#042F2E] to-[#059669] hover:from-[#021f1e] hover:to-[#047857] text-[#A7F3D0] hover:text-white text-[10px] font-extrabold border border-emerald-400/50 shadow-xs flex items-center gap-1.5 cursor-pointer transition"
                    >
                      <QrCode className="w-3.5 h-3.5 text-[#10B981]" />
                      <span>Generate Dynamic UPI QR</span>
                    </button>
                    <span className="px-2.5 py-1 rounded-full bg-emerald-100 text-[#042F2E] text-[10px] font-extrabold border border-emerald-300 shrink-0">
                      ⚡ ZERO-DEPLOY LIVE SYNC
                    </span>
                  </div>
                </div>

                {/* a) Vehicle Base Fare & Per-Km Rates (Bike, Eco Delivery, Mini EV) */}
                <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-2">
                  <div className="text-[11px] font-extrabold uppercase tracking-wider text-slate-700 flex items-center justify-between">
                    <span>a) Vehicle Base Fare &amp; Per-Km Rates (₹)</span>
                    <span className="text-[10px] text-[#059669] font-bold">80% Rider / 20% Admin Split</span>
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                    {([
                      { id: 'BIKE', name: '2-Wheeler Bike' },
                      { id: 'EV', name: 'Eco Delivery' },
                      { id: 'MINI_TRUCK', name: 'Mini EV' },
                    ] as { id: VehicleType; name: string }[]).map((veh) => {
                      const rate = vehiclePricing[veh.id];
                      return (
                        <div
                          key={veh.id}
                          className="p-2.5 rounded-xl bg-white border border-slate-200 space-y-1.5"
                        >
                          <div className="text-xs font-extrabold text-slate-900">{veh.name}</div>
                          <div className="grid grid-cols-2 gap-1.5">
                            <div>
                              <label className="block text-[9px] font-bold uppercase text-slate-500">
                                Base (₹)
                              </label>
                              <input
                                type="number"
                                min="20"
                                max="500"
                                value={rate.baseInr}
                                onChange={(e) => {
                                  const val = Math.max(10, parseInt(e.target.value, 10) || 0);
                                  setVehiclePricing((prev) => ({
                                    ...prev,
                                    [veh.id]: { ...prev[veh.id], baseInr: val },
                                  }));
                                }}
                                className="w-full px-2 py-1 text-xs font-extrabold bg-slate-50 border border-slate-200 rounded-lg text-[#042F2E]"
                              />
                            </div>
                            <div>
                              <label className="block text-[9px] font-bold uppercase text-slate-500">
                                Per Km (₹)
                              </label>
                              <input
                                type="number"
                                min="5"
                                max="100"
                                value={rate.perKmInr}
                                onChange={(e) => {
                                  const val = Math.max(1, parseInt(e.target.value, 10) || 0);
                                  setVehiclePricing((prev) => ({
                                    ...prev,
                                    [veh.id]: { ...prev[veh.id], perKmInr: val },
                                  }));
                                }}
                                className="w-full px-2 py-1 text-xs font-extrabold bg-slate-50 border border-slate-200 rounded-lg text-[#042F2E]"
                              />
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>

                {/* b) Surge Pricing Multiplier & Platform Fee */}
                <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-2">
                  <div className="text-[11px] font-extrabold uppercase tracking-wider text-slate-700">
                    b) Surge Pricing Multiplier &amp; Platform Fee
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-12 gap-2 items-center">
                    <div className="sm:col-span-8 flex flex-wrap gap-1.5">
                      {[
                        { mult: 1.0, label: '1.0x Normal' },
                        { mult: 1.2, label: '1.2x Peak / Rain Surge' },
                        { mult: 1.5, label: '1.5x High Demand' },
                      ].map((s) => (
                        <button
                          key={s.mult}
                          type="button"
                          onClick={() => {
                            setSurgeMultiplier(s.mult);
                            setAdminConfigSavedNotice(
                              `⚡ Live Surge Multiplier updated to ${s.label} across Customer & Rider Apps!`
                            );
                          }}
                          className={`px-2.5 py-1.5 rounded-lg text-xs font-extrabold border transition cursor-pointer ${
                            surgeMultiplier === s.mult
                              ? 'bg-[#042F2E] text-[#A7F3D0] border-[#059669]'
                              : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-100'
                          }`}
                        >
                          {s.label}
                        </button>
                      ))}
                    </div>
                    <div className="sm:col-span-4 flex items-center justify-between gap-2 bg-white px-2.5 py-1.5 rounded-lg border border-slate-200">
                      <span className="text-[10px] font-bold uppercase text-slate-500">
                        Platform Fee (₹)
                      </span>
                      <input
                        type="number"
                        min="0"
                        max="100"
                        value={platformFeeConfigInr}
                        onChange={(e) =>
                          setPlatformFeeConfigInr(Math.max(0, parseInt(e.target.value, 10) || 0))
                        }
                        className="w-16 px-2 py-0.5 text-xs font-extrabold text-right bg-slate-50 border border-slate-200 rounded"
                      />
                    </div>
                  </div>
                </div>

                {/* c) Active Promo Codes & Discount Amounts (e.g., QUICK15, FIRST50) */}
                <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-2">
                  <div className="text-[11px] font-extrabold uppercase tracking-wider text-slate-700 flex items-center justify-between">
                    <span>c) Active Promo Codes &amp; Discount Amounts</span>
                    <span className="text-[10px] text-[#059669] font-bold">
                      {promoCodesConfig.filter((p) => p.active).length} Active in Customer App
                    </span>
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    {promoCodesConfig.map((promo, idx) => (
                      <div
                        key={promo.code}
                        className="p-2 rounded-lg bg-white border border-slate-200 flex items-center justify-between gap-2"
                      >
                        <div className="flex items-center gap-1.5">
                          <input
                            type="checkbox"
                            checked={promo.active}
                            onChange={(e) => {
                              const checked = e.target.checked;
                              setPromoCodesConfig((prev) =>
                                prev.map((item, i) =>
                                  i === idx ? { ...item, active: checked } : item
                                )
                              );
                            }}
                            className="accent-[#059669]"
                          />
                          <span className="font-mono text-xs font-extrabold text-[#042F2E]">
                            {promo.code}
                          </span>
                        </div>
                        <div className="flex items-center gap-1">
                          <span className="text-[10px] text-slate-500 font-bold">-₹</span>
                          <input
                            type="number"
                            min="5"
                            max="200"
                            value={promo.discountInr}
                            onChange={(e) => {
                              const disc = Math.max(5, parseInt(e.target.value, 10) || 0);
                              setPromoCodesConfig((prev) =>
                                prev.map((item, i) =>
                                  i === idx
                                    ? {
                                        ...item,
                                        discountInr: disc,
                                        desc: `Flat ₹${disc} OFF on Hyperlocal Parcel Delivery`,
                                      }
                                    : item
                                )
                              );
                            }}
                            className="w-14 px-1.5 py-0.5 text-xs font-extrabold bg-slate-50 border border-slate-200 rounded text-[#059669]"
                          />
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* Add New Promo Code Row */}
                  <div className="flex flex-wrap items-center gap-1.5 pt-1">
                    <input
                      type="text"
                      value={newPromoCodeInput}
                      onChange={(e) => setNewPromoCodeInput(e.target.value.toUpperCase())}
                      placeholder="NEW CODE (e.g. MONSOON30)"
                      className="flex-1 min-w-[130px] px-2.5 py-1.5 text-xs font-mono font-bold uppercase bg-white border border-slate-200 rounded-lg"
                    />
                    <input
                      type="number"
                      min="5"
                      max="200"
                      value={newPromoDiscountInput}
                      onChange={(e) => setNewPromoDiscountInput(e.target.value)}
                      placeholder="₹ Discount"
                      className="w-20 px-2.5 py-1.5 text-xs font-extrabold bg-white border border-slate-200 rounded-lg"
                    />
                    <button
                      type="button"
                      onClick={() => {
                        const code = newPromoCodeInput.trim().toUpperCase();
                        const disc = Math.max(5, parseInt(newPromoDiscountInput, 10) || 20);
                        if (!code) return;
                        setPromoCodesConfig((prev) => [
                          ...prev.filter((p) => p.code !== code),
                          {
                            code,
                            discountInr: disc,
                            desc: `Flat ₹${disc} OFF on Instant 2-Wheeler & Mini EV Parcels`,
                            active: true,
                          },
                        ]);
                        setNewPromoCodeInput('');
                        setAdminConfigSavedNotice(
                          `🎁 Added live promo coupon ${code} (-₹${disc}) to Customer App!`
                        );
                      }}
                      className="px-3 py-1.5 rounded-lg bg-[#059669] hover:bg-emerald-600 text-white text-xs font-extrabold cursor-pointer"
                    >
                      + Add Coupon
                    </button>
                  </div>
                </div>

                {/* d) Service Availability Toggle (Maintenance Mode) & Live Announcement Banners */}
                <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-2.5">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <span className="text-[11px] font-extrabold uppercase tracking-wider text-slate-700">
                      d) Service Availability &amp; Live Announcement Banners
                    </span>
                    <button
                      type="button"
                      onClick={() => {
                        setServiceAvailable((prev) => {
                          const next = !prev;
                          setAdminConfigSavedNotice(
                            next
                              ? '🟢 Service Availability Enabled: Customer bookings are now LIVE!'
                              : '🔴 Maintenance Mode Activated: Customer new bookings paused!'
                          );
                          return next;
                        });
                      }}
                      className={`px-3 py-1 rounded-full text-xs font-extrabold border transition cursor-pointer ${
                        serviceAvailable
                          ? 'bg-emerald-100 text-[#042F2E] border-emerald-300'
                          : 'bg-red-600 text-white border-red-700'
                      }`}
                    >
                      {serviceAvailable
                        ? '🟢 Bookings Enabled (Active)'
                        : '🔴 Maintenance Mode (Bookings Paused)'}
                    </button>
                  </div>

                  <div className="space-y-1.5">
                    <div>
                      <label className="block text-[10px] font-bold uppercase text-slate-500 mb-0.5">
                        Live Customer App Announcement Banner
                      </label>
                      <input
                        type="text"
                        value={customerAnnouncement}
                        onChange={(e) => setCustomerAnnouncement(e.target.value)}
                        className="w-full px-2.5 py-1.5 text-xs bg-white border border-slate-200 rounded-lg"
                        placeholder="Announcement shown at top of Customer App..."
                      />
                    </div>
                    <div>
                      <label className="block text-[10px] font-bold uppercase text-slate-500 mb-0.5">
                        Live Rider Partner App Announcement Banner
                      </label>
                      <input
                        type="text"
                        value={riderAnnouncement}
                        onChange={(e) => setRiderAnnouncement(e.target.value)}
                        className="w-full px-2.5 py-1.5 text-xs bg-white border border-slate-200 rounded-lg"
                        placeholder="Announcement shown at top of Rider Partner App..."
                      />
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() =>
                      setAdminConfigSavedNotice(
                        `✅ Live App Settings & Pricing synced to Customer App (Bike ₹${vehiclePricing.BIKE.baseInr}+₹${vehiclePricing.BIKE.perKmInr}/km, Surge ${surgeMultiplier.toFixed(1)}x) & Rider Partner App!`
                      )
                    }
                    className="w-full py-2.5 px-4 rounded-xl bg-[#042F2E] hover:bg-emerald-950 text-[#A7F3D0] font-extrabold text-xs shadow-xs transition flex items-center justify-center gap-2 cursor-pointer"
                  >
                    <CheckCircle2 className="w-4 h-4 text-[#10B981]" />
                    <span>Save &amp; Sync Live Config to Customer + Rider Apps Now</span>
                  </button>
                </div>

                {/* e) Shop-Floor Payment Collections & POS Counter (Static & Dynamic UPI QR + Parcel Handover Scanner) */}
                <div className="relative p-3.5 rounded-2xl bg-gradient-to-r from-emerald-50 via-teal-50 to-emerald-50 border-2 border-emerald-300/80 shadow-xs space-y-2.5">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <div className="w-8 h-8 rounded-xl bg-[#042F2E] text-[#A7F3D0] flex items-center justify-center shadow-xs">
                        <QrCode className="w-4 h-4 text-[#10B981]" />
                      </div>
                      <div>
                        <div className="text-[11px] font-black uppercase tracking-wider text-[#042F2E] flex items-center gap-1.5">
                          <span>e) Shop-Floor Collections, POS Standee &amp; Parcel Handover</span>
                        </div>
                        <p className="text-[10px] text-slate-500">
                          Direct merchant UPI settlement • Live optical camera for parcel dispatch verification
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      {/* Floating / Quick Action Badge button */}
                      <button
                        type="button"
                        onClick={() => setShowParcelScannerModal((prev) => !prev)}
                        className="px-3 py-1 rounded-full bg-gradient-to-r from-teal-700 to-emerald-700 hover:from-teal-800 hover:to-emerald-800 text-white text-[11px] font-black shadow-xs flex items-center gap-1.5 border border-emerald-400 cursor-pointer transition transform active:scale-95"
                      >
                        <ScanLine className="w-3.5 h-3.5 text-[#A7F3D0] animate-pulse" />
                        <span>Scan Handover</span>
                      </button>
                      <span className="px-2 py-0.5 rounded-full bg-emerald-200/80 text-[#042F2E] text-[10px] font-black border border-emerald-400">
                        ⚡ 0% UPI
                      </span>
                    </div>
                  </div>

                  {/* Prominent 'Scan Handover' Primary Action Button */}
                  <div className="pt-0.5">
                    <button
                      type="button"
                      onClick={() => setShowParcelScannerModal((prev) => !prev)}
                      className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-[#042F2E] via-teal-900 to-[#059669] hover:from-[#021f1e] hover:to-[#047857] text-[#A7F3D0] hover:text-white text-xs font-black shadow-md transition flex items-center justify-between gap-2 cursor-pointer border-2 border-emerald-400/50 group active:scale-[0.99]"
                    >
                      <div className="flex items-center gap-2.5">
                        <div className="w-8 h-8 rounded-lg bg-emerald-500/20 border border-emerald-400/40 flex items-center justify-center text-white">
                          <ScanLine className="w-4 h-4 text-[#10B981] group-hover:scale-110 transition" />
                        </div>
                        <div className="text-left">
                          <div className="flex items-center gap-1.5 leading-none">
                            <span className="text-sm font-black tracking-tight text-white">
                              Scan Handover
                            </span>
                            <span className="text-[9px] bg-emerald-400/25 text-[#A7F3D0] px-1.5 py-0.5 rounded font-extrabold uppercase tracking-wider">
                              Camera / QR / Barcode
                            </span>
                          </div>
                          <p className="text-[10px] text-emerald-200/80 mt-1 leading-none font-medium">
                            Verify waybill barcodes, parcel tags &amp; customer OTP at dispatch counter
                          </p>
                        </div>
                      </div>

                      <div className="px-2.5 py-1 rounded-lg bg-emerald-500/20 text-[#A7F3D0] group-hover:bg-emerald-500/30 group-hover:text-white text-[10px] font-extrabold uppercase tracking-wider border border-emerald-300/30 shrink-0 flex items-center gap-1">
                        <span>Launch Camera</span>
                        <ArrowRight className="w-3 h-3" />
                      </div>
                    </button>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-0.5">
                    <button
                      type="button"
                      onClick={() => setShowUpiQrModal(true)}
                      className="py-2 px-3 rounded-xl bg-white hover:bg-emerald-50 text-[#042F2E] text-xs font-extrabold shadow-xs transition flex items-center justify-center gap-2 cursor-pointer border border-emerald-300"
                    >
                      <QrCode className="w-3.5 h-3.5 text-[#059669]" />
                      <span>Generate Dynamic UPI QR</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => setShowUpiQrModal(true)}
                      className="py-2 px-3 rounded-xl bg-white hover:bg-slate-100 text-slate-700 text-xs font-extrabold shadow-xs transition flex items-center justify-center gap-2 cursor-pointer border border-slate-200"
                    >
                      <Printer className="w-3.5 h-3.5 text-slate-600" />
                      <span>Counter Standee (Static)</span>
                    </button>
                  </div>
                </div>
              </div>

              {/* 2. PLAY STORE VERSION & FORCE / FLEXIBLE UPDATE TRIGGER */}
              <div className="p-4 rounded-2xl bg-white border-2 border-[#042F2E]/30 shadow-xs space-y-3.5 flex flex-col justify-between">
                <div className="space-y-3.5">
                  <div className="flex flex-wrap items-start justify-between gap-2 pb-2 border-b border-slate-200">
                    <div>
                      <div className="flex items-center gap-2">
                        <Smartphone className="w-4 h-4 text-[#059669]" />
                        <h3 className="text-xs sm:text-sm font-extrabold text-[#042F2E]">
                          Play Store Release &amp; Version Control
                        </h3>
                      </div>
                      <p className="text-[11px] text-slate-500 mt-0.5">
                        Trigger Google Play Flexible Update banners or Mandatory Force Update blocking modals across Customer &amp; Rider apps
                      </p>
                    </div>
                    <span className="px-2.5 py-1 rounded-full bg-[#042F2E] text-[#A7F3D0] text-[10px] font-extrabold shrink-0">
                      ▶ GOOGLE PLAY IN-APP UPDATES
                    </span>
                  </div>

                  {/* a) Latest Customer App Version & Latest Rider App Version */}
                  <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-2.5">
                    <div className="flex items-center justify-between">
                      <span className="text-[11px] font-extrabold uppercase tracking-wider text-slate-700">
                        a) Target Play Store App Versions
                      </span>
                      <span className="text-[10px] font-bold text-slate-500">
                        {playStorePublishedStamp}
                      </span>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                      <div className="p-2.5 rounded-xl bg-white border border-slate-200 space-y-1.5">
                        <div className="flex items-center justify-between">
                          <span className="text-xs font-extrabold text-slate-800">
                            Latest Customer App
                          </span>
                          <span
                            className={`px-2 py-0.5 rounded text-[10px] font-extrabold ${
                              installedCustomerVersion === latestCustomerVersion
                                ? 'bg-emerald-50 text-emerald-700'
                                : 'bg-amber-50 text-amber-800'
                            }`}
                          >
                            Installed: v{installedCustomerVersion}
                          </span>
                        </div>
                        <div className="flex items-center gap-1.5">
                          <input
                            type="text"
                            value={latestCustomerVersion}
                            onChange={(e) => setLatestCustomerVersion(e.target.value)}
                            className="flex-1 px-2.5 py-1.5 text-xs font-mono font-extrabold bg-slate-50 border border-slate-200 rounded-lg text-[#042F2E]"
                            placeholder="e.g. 1.1.0"
                          />
                          <button
                            type="button"
                            onClick={() => {
                              const parts = latestCustomerVersion.split('.').map((n) => parseInt(n, 10) || 0);
                              const nextVer = `${parts[0] || 1}.${(parts[1] || 1) + 1}.0`;
                              setLatestCustomerVersion(nextVer);
                              setCustomerDismissedFlexible(false);
                            }}
                            className="px-2.5 py-1.5 rounded-lg bg-emerald-50 hover:bg-emerald-100 text-[#065F46] border border-emerald-200 text-[11px] font-extrabold cursor-pointer"
                          >
                            + Bump
                          </button>
                        </div>
                      </div>

                      <div className="p-2.5 rounded-xl bg-white border border-slate-200 space-y-1.5">
                        <div className="flex items-center justify-between">
                          <span className="text-xs font-extrabold text-slate-800">
                            Latest Rider App
                          </span>
                          <span
                            className={`px-2 py-0.5 rounded text-[10px] font-extrabold ${
                              installedRiderVersion === latestRiderVersion
                                ? 'bg-emerald-50 text-emerald-700'
                                : 'bg-amber-50 text-amber-800'
                            }`}
                          >
                            Installed: v{installedRiderVersion}
                          </span>
                        </div>
                        <div className="flex items-center gap-1.5">
                          <input
                            type="text"
                            value={latestRiderVersion}
                            onChange={(e) => setLatestRiderVersion(e.target.value)}
                            className="flex-1 px-2.5 py-1.5 text-xs font-mono font-extrabold bg-slate-50 border border-slate-200 rounded-lg text-[#042F2E]"
                            placeholder="e.g. 1.1.0"
                          />
                          <button
                            type="button"
                            onClick={() => {
                              const parts = latestRiderVersion.split('.').map((n) => parseInt(n, 10) || 0);
                              const nextVer = `${parts[0] || 1}.${(parts[1] || 1) + 1}.0`;
                              setLatestRiderVersion(nextVer);
                              setRiderDismissedFlexible(false);
                            }}
                            className="px-2.5 py-1.5 rounded-lg bg-emerald-50 hover:bg-emerald-100 text-[#065F46] border border-emerald-200 text-[11px] font-extrabold cursor-pointer"
                          >
                            + Bump
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* b) What's New / Release Notes Text Input */}
                  <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-1.5">
                    <label className="block text-[11px] font-extrabold uppercase tracking-wider text-slate-700">
                      b) &ldquo;What&apos;s New / Release Notes&rdquo; (Shown on Play Store Prompt)
                    </label>
                    <textarea
                      rows={3}
                      value={releaseNotes}
                      onChange={(e) => setReleaseNotes(e.target.value)}
                      className="w-full px-3 py-2 text-xs bg-white border border-slate-200 rounded-xl text-slate-800 focus:outline-none focus:border-[#059669]"
                      placeholder="Enter release notes for Customer & Rider apps..."
                    />
                  </div>

                  {/* c) Update Mode Selector */}
                  <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-2">
                    <label className="block text-[11px] font-extrabold uppercase tracking-wider text-slate-700">
                      c) Update Mode Selector (Google Play In-App Update Policy)
                    </label>
                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                      {[
                        {
                          id: 'NONE' as const,
                          title: 'No Update Banner',
                          sub: 'Silent / Normal Mode',
                        },
                        {
                          id: 'FLEXIBLE' as const,
                          title: 'Flexible Update',
                          sub: 'Remind User (Dismissible)',
                        },
                        {
                          id: 'FORCE' as const,
                          title: 'Force Update',
                          sub: 'Block Old App Until Updated',
                        },
                      ].map((mode) => {
                        const active = updateMode === mode.id;
                        return (
                          <button
                            key={mode.id}
                            type="button"
                            onClick={() => {
                              setUpdateMode(mode.id);
                              setCustomerDismissedFlexible(false);
                              setRiderDismissedFlexible(false);
                            }}
                            className={`p-2.5 rounded-xl border text-left transition cursor-pointer ${
                              active
                                ? mode.id === 'FORCE'
                                  ? 'border-red-600 bg-red-50 text-red-950 ring-1 ring-red-600'
                                  : 'border-[#059669] bg-emerald-50 text-[#042F2E] ring-1 ring-[#059669]'
                                : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-100'
                            }`}
                          >
                            <div className="text-xs font-extrabold">{mode.title}</div>
                            <div className="text-[10px] opacity-80 mt-0.5">{mode.sub}</div>
                          </button>
                        );
                      })}
                    </div>
                  </div>
                </div>

                {/* d) Publish Update Notice to Apps Button */}
                <div className="space-y-2 pt-2">
                  <button
                    type="button"
                    onClick={() => {
                      setCustomerDismissedFlexible(false);
                      setRiderDismissedFlexible(false);
                      if (
                        installedCustomerVersion === latestCustomerVersion &&
                        installedRiderVersion === latestRiderVersion
                      ) {
                        // Automatically simulate an older installed version if user already clicked Update Now so they can immediately see the banner/modal again
                        setInstalledCustomerVersion('1.0.0');
                        setInstalledRiderVersion('1.0.0');
                      }
                      if (updateMode === 'NONE') {
                        setUpdateMode('FLEXIBLE');
                      }
                      const nowStr = new Date().toLocaleTimeString([], {
                        hour: '2-digit',
                        minute: '2-digit',
                      });
                      setPlayStorePublishedStamp(
                        `Published ${nowStr} • Customer v${latestCustomerVersion} / Rider v${latestRiderVersion}`
                      );
                      setAdminConfigSavedNotice(
                        `🚀 Play Store ${updateMode === 'NONE' ? 'FLEXIBLE' : updateMode} Update Notice (Customer v${latestCustomerVersion} & Rider v${latestRiderVersion}) pushed live to apps!`
                      );
                    }}
                    className="w-full py-3 px-4 rounded-xl bg-[#059669] hover:bg-emerald-600 text-white font-extrabold text-xs sm:text-sm shadow-sm transition flex items-center justify-center gap-2 cursor-pointer"
                  >
                    <Sparkles className="w-4 h-4" />
                    <span>
                      Publish Update Notice to Apps (Customer v{latestCustomerVersion} • Rider v
                      {latestRiderVersion})
                    </span>
                  </button>
                </div>
              </div>
            </div>

            {/* 4. REVENUE ANALYTICS & ORDER STATUS FUNNEL CARDS (Vertically stacked grid-cols-1 on mobile, side-by-side md:grid-cols-2 on desktop) */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Left Card: Revenue (80% Rider / 20% Admin) & Order Volume Analytics */}
              <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-xs space-y-3">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <h3 className="text-xs sm:text-sm font-extrabold text-[#042F2E]">
                      Revenue (80% Rider / 20% Admin) & Order Volume Analytics
                    </h3>
                    <p className="text-[11px] text-slate-500">
                      Total GMV ₹{totalGrossInr} • 80% Rider ₹{totalRiderShareInr} • 20% Admin ₹
                      {totalCommissionInr}
                    </p>
                  </div>
                  <span className="px-2.5 py-1 rounded-lg bg-emerald-50 border border-emerald-200 text-[10px] font-extrabold text-[#059669] shrink-0">
                    80% / 20% Split
                  </span>
                </div>

                {/* Visual Split Bars per Order */}
                <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-2.5">
                  {orders.map((ord) => {
                    const totalInr = Math.round(ord.totalAmountPaise / 100);
                    const rider80Inr = Math.round(ord.riderEarningPaise / 100);
                    const admin20Inr = Math.round(ord.companyCommissionPaise / 100);
                    return (
                      <div key={ord.id} className="space-y-1">
                        <div className="flex items-center justify-between text-xs">
                          <span className="font-bold text-slate-800">
                            #{ord.orderCode} ({ord.vehicleType})
                          </span>
                          <span className="font-semibold text-slate-600">
                            Total ₹{totalInr} •{' '}
                            <strong className="text-[#059669]">80% Rider: ₹{rider80Inr}</strong> •{' '}
                            <strong className="text-amber-600">20% Admin: ₹{admin20Inr}</strong>
                          </span>
                        </div>
                        <div className="w-full h-3 rounded-full bg-slate-200 overflow-hidden flex">
                          <div
                            className="h-full bg-[#059669]"
                            style={{ width: '80%' }}
                            title={`80% Rider Share: ₹${rider80Inr}`}
                          />
                          <div
                            className="h-full bg-amber-500"
                            style={{ width: '20%' }}
                            title={`20% Platform Commission: ₹${admin20Inr}`}
                          />
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Right Card: Order Status Funnel (Clean horizontal progress bars & neat status badges — zero broken text) */}
              <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-xs space-y-3">
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="text-xs sm:text-sm font-extrabold text-[#042F2E]">
                      Order Status Funnel ({orders.length} Total)
                    </h3>
                    <p className="text-[11px] text-slate-500">
                      Real-time stage progression across hyperlocal hubs
                    </p>
                  </div>
                  <span className="px-2.5 py-1 rounded-lg bg-emerald-50 border border-emerald-200 text-[10px] font-extrabold text-[#059669]">
                    LIVE PIPELINE
                  </span>
                </div>

                <div className="space-y-3 pt-1">
                  {[
                    {
                      label: 'Searching',
                      badge: 'SEARCHING RIDER',
                      count: searchingCount,
                      barColor: 'bg-orange-500',
                      badgeStyle: 'bg-orange-50 text-orange-700 border-orange-200',
                    },
                    {
                      label: 'Assigned',
                      badge: 'RIDER ASSIGNED',
                      count: assignedCount,
                      barColor: 'bg-[#059669]',
                      badgeStyle: 'bg-emerald-50 text-[#059669] border-emerald-200',
                    },
                    {
                      label: 'In Transit',
                      badge: 'IN TRANSIT',
                      count: inTransitCount,
                      barColor: 'bg-blue-600',
                      badgeStyle: 'bg-blue-50 text-blue-700 border-blue-200',
                    },
                    {
                      label: 'Delivered',
                      badge: 'DELIVERED',
                      count: deliveredCount,
                      barColor: 'bg-emerald-500',
                      badgeStyle: 'bg-emerald-50 text-emerald-700 border-emerald-200',
                    },
                  ].map((stage) => {
                    const pct = Math.round((stage.count / totalOrdersCount) * 100);
                    return (
                      <div key={stage.label} className="space-y-1">
                        <div className="flex items-center justify-between gap-2 text-xs">
                          <div className="flex items-center gap-2">
                            <span className="font-bold text-slate-800">{stage.label}</span>
                            <span
                              className={`px-2 py-0.5 rounded-md text-[10px] font-extrabold border ${stage.badgeStyle}`}
                            >
                              {stage.badge}
                            </span>
                          </div>
                          <span className="font-extrabold text-slate-700">
                            {stage.count} ({pct}%)
                          </span>
                        </div>
                        <div className="w-full h-2 rounded-full bg-slate-200 overflow-hidden">
                          <div
                            className={`h-full rounded-full transition-all ${stage.barColor}`}
                            style={{ width: `${Math.max(pct, stage.count > 0 ? 12 : 0)}%` }}
                          />
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>

            {/* 4.5. LIVE FLEET GPS COMMAND RADAR & DISPATCH MAP */}
            <div
              id="admin-fleet-radar"
              className={`transition-all duration-300 ${
                adminActiveSection === 'LIVE_TRACKING'
                  ? 'ring-4 ring-emerald-500 rounded-2xl shadow-xl'
                  : ''
              }`}
            >
              <AdminFleetGpsRadar
                orders={orders}
                onSelectOrder={(ord) => setSelectedTrackingOrder(ord as any)}
              />
            </div>

            {/* 5. PROFESSIONAL LIVE ORDERS & STATUS MANAGEMENT SYSTEM */}
            <div
              id="admin-orders-manager"
              className={
                adminActiveSection === 'LIVE_ORDERS'
                  ? 'ring-4 ring-emerald-500 rounded-2xl shadow-xl transition-all duration-300'
                  : 'transition-all duration-300'
              }
            >
              <AdminOrderStatusManager
                orders={orders as any}
                onUpdateOrderStatus={(orderId, newStatus, details) =>
                  handleAdminUpdateOrderStatus(orderId, newStatus as any, details)
                }
                onBulkUpdateStatus={(orderIds, newStatus) =>
                  handleAdminBulkUpdateStatus(orderIds, newStatus as any)
                }
                onTrackOrder={(order) => setSelectedTrackingOrder(order as any)}
                onDownloadGstInvoice={(order) => handleDownloadGstInvoice(order as any)}
              />
            </div>

            {/* Append-Only Company Ledger Audit Trail */}
            <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-xs space-y-2.5">
              <h3 className="text-xs font-extrabold uppercase tracking-wider text-[#042F2E]">
                Append-Only Company Ledger & Settlements ({ledger.length})
              </h3>
              <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
                {ledger.map((item) => (
                  <div
                    key={item.id}
                    className="p-3 rounded-xl border border-slate-200 bg-slate-50 flex items-center justify-between text-xs"
                  >
                    <div>
                      <div className="font-bold text-slate-800">
                        {item.entryType.replace(/_/g, ' ')} •{' '}
                        <span className="text-[#059669]">{item.referenceId}</span>
                      </div>
                      <div className="text-[11px] text-slate-500">{item.description}</div>
                      <div className="text-[10px] text-slate-400 mt-0.5">
                        {item.settlementStatus} • {item.createdAt}
                      </div>
                    </div>
                    <div className="text-sm font-extrabold text-[#059669] shrink-0 ml-3">
                      +₹{(item.amountPaise / 100).toFixed(2)}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  };

  // --- ROLE-BASED ACCESS CONTROL GUARD: REQUIRE AUTHENTICATION ---
  const lockedRoleForDedicatedApp: UserRole | undefined =
    dedicatedAppMode === 'customer'
      ? 'CUSTOMER'
      : dedicatedAppMode === 'rider'
      ? 'RIDER'
      : dedicatedAppMode === 'admin'
      ? 'ADMIN'
      : undefined;

  if (!currentUser || (lockedRoleForDedicatedApp && currentUser.role !== lockedRoleForDedicatedApp)) {
    return <AuthScreen onLoginSuccess={handleLoginSuccess} lockedRole={lockedRoleForDedicatedApp} />;
  }

  return (
    <div className="min-h-screen bg-[#F1F5F9] text-[#0F172A] flex flex-col">
      {/* Top Navigation Header */}
      <header className="bg-[#022C22] text-white px-4 sm:px-6 py-3.5 shadow-md sticky top-0 z-30 border-b border-emerald-900">
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div
              className={`w-9 h-9 rounded-xl flex items-center justify-center font-black text-sm shadow-inner ${
                dedicatedAppMode === 'rider'
                  ? 'bg-amber-500 text-slate-950 border border-amber-300'
                  : dedicatedAppMode === 'admin'
                  ? 'bg-teal-600 text-white border border-teal-300'
                  : 'bg-[#059669] text-white border border-[#A7F3D0]'
              }`}
            >
              {dedicatedAppMode === 'rider' ? 'QP' : dedicatedAppMode === 'admin' ? 'QA' : 'QD'}
            </div>
            <div>
              <h1 className="font-bold text-base sm:text-lg tracking-tight flex items-center gap-2">
                {dedicatedAppMode === 'customer' && 'Quick Delivery'}
                {dedicatedAppMode === 'rider' && 'Quick Delivery Partner'}
                {dedicatedAppMode === 'admin' && 'QD Admin Enterprise'}
                {!dedicatedAppMode && 'Quick Delivery'}
                <span className="text-[11px] font-semibold bg-emerald-500/20 text-[#A7F3D0] border border-emerald-400/30 px-2 py-0.5 rounded-full">
                  {dedicatedAppMode === 'customer' && 'Customer App'}
                  {dedicatedAppMode === 'rider' && 'Rider Partner Fleet'}
                  {dedicatedAppMode === 'admin' && 'Enterprise Logistics SaaS'}
                  {!dedicatedAppMode && 'Enterprise Logistics SaaS'}
                </span>
              </h1>
              <p className="text-xs text-[#A7F3D0]/85">
                {dedicatedAppMode === 'customer' &&
                  'Hyperlocal Parcel Booking • Live OTP Handover • Real-time Tracking'}
                {dedicatedAppMode === 'rider' &&
                  'Fleet Delivery Dispatch • 80% Partner Net Share • Live Radar'}
                {dedicatedAppMode === 'admin' &&
                  'Fleet Operations • Dynamic Pricing • Financial Audit & Settlements'}
                {!dedicatedAppMode &&
                  'Customer Booking • Rider Partner Dispatch • Admin Web Enterprise Command Center'}
              </p>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            {/* If not in dedicated mode, show the "📲 Download 3 Separate Apps / Links" launcher button */}
            {!dedicatedAppMode && (
              <button
                type="button"
                onClick={() => setShowSeparateAppsModal(true)}
                className="px-3 py-1.5 rounded-xl bg-gradient-to-r from-teal-500 to-emerald-600 hover:from-teal-400 hover:to-emerald-500 text-white text-xs font-bold flex items-center gap-1.5 shadow-md border border-teal-300/40 transition cursor-pointer"
                title="Download or share 3 separate direct app links for 3 different phones"
              >
                <Share2 className="w-3.5 h-3.5" />
                <span>📲 3 Separate Apps / Phone Links</span>
              </button>
            )}

            {/* 📲 PWA Direct Install Button for Current Mode */}
            <button
              type="button"
              onClick={handleTriggerInstall}
              className="px-3.5 py-1.5 rounded-xl bg-gradient-to-r from-emerald-400 via-teal-300 to-emerald-400 text-[#022C22] text-xs font-black flex items-center gap-1.5 shadow-md hover:brightness-105 active:scale-95 transition cursor-pointer border border-emerald-200/50"
              title="Install directly to mobile device as standalone Android app"
            >
              <span className="text-sm">📲</span>
              <span className="font-extrabold">{getInstallButtonLabel()}</span>
            </button>

            {/* View Mode Toggle - ONLY VISIBLE IN DEFAULT UNIFIED HUB MODE */}
            {!dedicatedAppMode && (
              <>
                <div className="bg-white/10 p-1 rounded-xl flex items-center gap-1">
                  <button
                    type="button"
                    onClick={() => setViewMode('SPLIT_ALL')}
                    className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition ${
                      viewMode === 'SPLIT_ALL'
                        ? 'bg-[#059669] text-white shadow-xs'
                        : 'text-emerald-100 hover:text-white'
                    }`}
                  >
                    <Columns className="w-3.5 h-3.5" />
                    All 3 Live View
                  </button>
                  <button
                    type="button"
                    onClick={() => setViewMode('TABS')}
                    className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition ${
                      viewMode === 'TABS'
                        ? 'bg-[#059669] text-white shadow-xs'
                        : 'text-emerald-100 hover:text-white'
                    }`}
                  >
                    <LayoutGrid className="w-3.5 h-3.5" />
                    Single App View
                  </button>
                </div>

                {viewMode === 'TABS' && (
                  <div className="bg-white/10 p-1 rounded-xl flex items-center gap-1">
                    {(
                      [
                        ['CUSTOMER', 'Customer'],
                        ['RIDER', 'Rider Partner'],
                        ['ADMIN', 'Admin Web'],
                      ] as const
                    ).map(([tab, label]) => (
                      <button
                        type="button"
                        key={tab}
                        onClick={() => setActiveTab(tab)}
                        className={`px-3 py-1.5 rounded-lg text-xs font-bold transition ${
                          activeTab === tab
                            ? 'bg-[#10B981] text-[#022C22]'
                            : 'text-emerald-100 hover:text-white'
                        }`}
                      >
                        {label}
                      </button>
                    ))}
                  </div>
                )}
              </>
            )}

            {/* Authenticated User Badge & Role-Based Access Controls */}
            <div className="flex items-center gap-2 pl-2 border-l border-emerald-800/80">
              <div
                onClick={() =>
                  setImageUploadModalState({
                    isOpen: true,
                    category: 'PROFILE_PHOTO',
                    title: `Update ${currentUser.role === 'RIDER' ? 'Rider' : currentUser.role === 'ADMIN' ? 'Admin' : 'Customer'} Photo`,
                    subtitle: 'Upload a new profile picture or take a live photo.',
                    currentImageUrl: currentUser.avatarUrl,
                  })
                }
                className="flex items-center gap-2 bg-white/10 hover:bg-white/15 px-2.5 py-1.5 rounded-xl border border-white/15 cursor-pointer transition group"
                title="Click to update profile photo"
              >
                <div className="relative">
                  {currentUser.avatarUrl ? (
                    <img
                      src={currentUser.avatarUrl}
                      alt={currentUser.name}
                      className="w-7 h-7 rounded-lg object-cover border border-emerald-300 shadow-xs"
                    />
                  ) : (
                    <div className="w-7 h-7 rounded-lg bg-[#059669] text-white flex items-center justify-center font-black text-xs border border-emerald-300">
                      {currentUser.avatarText || currentUser.name.slice(0, 2).toUpperCase()}
                    </div>
                  )}
                  <span className="absolute -bottom-1 -right-1 w-3.5 h-3.5 bg-emerald-500 rounded-full flex items-center justify-center text-white opacity-0 group-hover:opacity-100 transition shadow-xs text-[8px]">
                    📷
                  </span>
                </div>
                <div className="hidden sm:block text-left leading-tight">
                  <div className="text-xs font-black text-white truncate max-w-[130px]">
                    {currentUser.name}
                  </div>
                  <div className="text-[10px] text-[#A7F3D0] font-bold">
                    {currentUser.role === 'ADMIN' ? 'Admin Enterprise' : currentUser.role === 'RIDER' ? 'Rider Partner' : 'Customer'}
                  </div>
                </div>
                <span
                  className={`text-[9px] font-black uppercase px-2 py-0.5 rounded-full border ${
                    currentUser.role === 'ADMIN'
                      ? 'bg-emerald-500/30 text-[#A7F3D0] border-emerald-300/40'
                      : currentUser.role === 'RIDER'
                      ? 'bg-amber-500/30 text-amber-200 border-amber-300/40'
                      : 'bg-blue-500/30 text-blue-200 border-blue-300/40'
                  }`}
                >
                  {currentUser.role}
                </span>
              </div>

              {/* Role Switcher Pill (Quick access to test other roles - hidden in standalone APK mode) */}
              {!dedicatedAppMode && (
                <div className="hidden lg:flex items-center bg-white/10 p-0.5 rounded-xl border border-white/10 text-[10px] font-black">
                  <button
                    type="button"
                    onClick={() => handleSwitchRole('CUSTOMER')}
                    className={`px-2 py-1 rounded-lg transition cursor-pointer ${
                      currentUser.role === 'CUSTOMER' ? 'bg-[#10B981] text-[#022C22]' : 'text-slate-300 hover:text-white'
                    }`}
                    title="Switch to Customer Role"
                  >
                    👤 Customer
                  </button>
                  <button
                    type="button"
                    onClick={() => handleSwitchRole('RIDER')}
                    className={`px-2 py-1 rounded-lg transition cursor-pointer ${
                      currentUser.role === 'RIDER' ? 'bg-amber-400 text-slate-950' : 'text-slate-300 hover:text-white'
                    }`}
                    title="Switch to Rider Role"
                  >
                    🏍️ Rider
                  </button>
                  <button
                    type="button"
                    onClick={() => handleSwitchRole('ADMIN')}
                    className={`px-2 py-1 rounded-lg transition cursor-pointer ${
                      currentUser.role === 'ADMIN' ? 'bg-teal-400 text-slate-950' : 'text-slate-300 hover:text-white'
                    }`}
                    title="Switch to Admin Role"
                  >
                    🏢 Admin
                  </button>
                </div>
              )}

              {/* Push Notifications & Real-Time Alerts Hub Button */}
              <button
                type="button"
                onClick={() => setShowNotificationModal(true)}
                className="relative p-2 rounded-xl bg-white/10 hover:bg-white/20 text-[#A7F3D0] hover:text-white border border-white/15 transition cursor-pointer flex items-center justify-center shrink-0"
                title="Push Notifications & Real-Time Alerts Hub"
              >
                <Bell className="w-4 h-4" />
                {unreadNotifCount > 0 && (
                  <span className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-amber-400 text-slate-950 font-black text-[9px] flex items-center justify-center border-2 border-[#042F2E] animate-pulse">
                    {unreadNotifCount > 9 ? '9+' : unreadNotifCount}
                  </span>
                )}
              </button>

              {/* Sign Out Button */}
              <button
                type="button"
                onClick={handleLogout}
                className="p-1.5 rounded-xl bg-white/10 hover:bg-red-500/30 text-slate-300 hover:text-red-200 border border-white/15 transition cursor-pointer flex items-center gap-1"
                title="Sign Out / Switch Account"
              >
                <LogOut className="w-4 h-4" />
                <span className="text-[10px] font-bold hidden sm:inline">Sign Out</span>
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content Area - Role-Based Access Control Routing */}
      <main className="max-w-7xl w-full mx-auto p-3 sm:p-6 flex-1">
        {dedicatedAppMode === 'customer' ? (
          <div className="max-w-xl mx-auto">
            <ErrorBoundary inline fallbackTitle="Customer App Panel" fallbackMessage="Reloading Customer App...">
              {renderCustomerPanel()}
            </ErrorBoundary>
          </div>
        ) : dedicatedAppMode === 'rider' ? (
          <div className="max-w-xl mx-auto">
            <ErrorBoundary inline fallbackTitle="Rider App Panel" fallbackMessage="Reloading Rider App...">
              {renderRiderPanel()}
            </ErrorBoundary>
          </div>
        ) : dedicatedAppMode === 'admin' ? (
          <div className="w-full">
            <ErrorBoundary inline fallbackTitle="Admin Dashboard Panel" fallbackMessage="Reloading Admin Dashboard...">
              {renderAdminPanel()}
            </ErrorBoundary>
          </div>
        ) : currentUser.role === 'CUSTOMER' ? (
          <div className="max-w-xl mx-auto space-y-3">
            <div className="p-3 rounded-2xl bg-white border border-slate-200 shadow-xs flex items-center justify-between text-xs">
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-500" />
                <span className="font-extrabold text-slate-800">
                  Signed in as <strong>{currentUser.name}</strong> (Customer Portal)
                </span>
              </div>
              <button
                type="button"
                onClick={() => handleSwitchRole('RIDER')}
                className="text-[11px] text-[#059669] font-bold hover:underline cursor-pointer"
              >
                Switch to Rider Partner →
              </button>
            </div>
            <ErrorBoundary inline fallbackTitle="Customer App Panel" fallbackMessage="Reloading Customer App...">
              {renderCustomerPanel()}
            </ErrorBoundary>
          </div>
        ) : currentUser.role === 'RIDER' ? (
          <div className="max-w-xl mx-auto space-y-3">
            <div className="p-3 rounded-2xl bg-white border border-slate-200 shadow-xs flex items-center justify-between text-xs">
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-amber-500" />
                <span className="font-extrabold text-slate-800">
                  Signed in as <strong>{currentUser.name}</strong> (Rider Partner Fleet)
                </span>
              </div>
              <button
                type="button"
                onClick={() => handleSwitchRole('ADMIN')}
                className="text-[11px] text-[#059669] font-bold hover:underline cursor-pointer"
              >
                Switch to Admin SaaS →
              </button>
            </div>
            <ErrorBoundary inline fallbackTitle="Rider App Panel" fallbackMessage="Reloading Rider App...">
              {renderRiderPanel()}
            </ErrorBoundary>
          </div>
        ) : dedicatedAppMode === 'customer' ? (
          <div className="max-w-xl mx-auto">
            <ErrorBoundary inline fallbackTitle="Customer App Panel" fallbackMessage="Reloading Customer App...">
              {renderCustomerPanel()}
            </ErrorBoundary>
          </div>
        ) : dedicatedAppMode === 'rider' ? (
          <div className="max-w-xl mx-auto">
            <ErrorBoundary inline fallbackTitle="Rider App Panel" fallbackMessage="Reloading Rider App...">
              {renderRiderPanel()}
            </ErrorBoundary>
          </div>
        ) : dedicatedAppMode === 'admin' ? (
          <div className="w-full">
            <ErrorBoundary inline fallbackTitle="Admin Dashboard Panel" fallbackMessage="Reloading Admin Dashboard...">
              {renderAdminPanel()}
            </ErrorBoundary>
          </div>
        ) : viewMode === 'SPLIT_ALL' ? (
          <div className="space-y-6">
            {/* Full-Width Enterprise Admin Web Command Center */}
            <ErrorBoundary inline fallbackTitle="Admin Dashboard Panel" fallbackMessage="Reloading Admin Dashboard...">
              {renderAdminPanel()}
            </ErrorBoundary>

            {/* Customer & Rider Partner Side-by-Side Live Apps */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-5 items-start">
              <ErrorBoundary inline fallbackTitle="Customer App Panel" fallbackMessage="Reloading Customer App...">
                {renderCustomerPanel()}
              </ErrorBoundary>
              <ErrorBoundary inline fallbackTitle="Rider App Panel" fallbackMessage="Reloading Rider App...">
                {renderRiderPanel()}
              </ErrorBoundary>
            </div>
          </div>
        ) : (
          <div className={activeTab === 'ADMIN' ? 'w-full' : 'max-w-xl mx-auto'}>
            {activeTab === 'CUSTOMER' && (
              <ErrorBoundary inline fallbackTitle="Customer App Panel" fallbackMessage="Reloading Customer App...">
                {renderCustomerPanel()}
              </ErrorBoundary>
            )}
            {activeTab === 'RIDER' && (
              <ErrorBoundary inline fallbackTitle="Rider App Panel" fallbackMessage="Reloading Rider App...">
                {renderRiderPanel()}
              </ErrorBoundary>
            )}
            {activeTab === 'ADMIN' && (
              <ErrorBoundary inline fallbackTitle="Admin Dashboard Panel" fallbackMessage="Reloading Admin Dashboard...">
                {renderAdminPanel()}
              </ErrorBoundary>
            )}
          </div>
        )}
      </main>

      {/* 📲 LAUNCHER HUB MODAL: 3 SEPARATE APPS FOR 3 DIFFERENT PHONES */}
      {showSeparateAppsModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm p-4 flex items-center justify-center animate-in fade-in overflow-y-auto">
          <div className="w-full max-w-2xl rounded-2xl bg-white border-2 border-emerald-500 shadow-2xl overflow-hidden flex flex-col my-auto max-h-[90vh]">
            {/* Modal Header */}
            <div className="bg-gradient-to-r from-[#042F2E] via-[#065F46] to-[#059669] text-white px-5 py-4 flex items-center justify-between shrink-0">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-white/10 border border-emerald-300/40 flex items-center justify-center text-xl shadow-inner">
                  📲
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="font-extrabold text-base sm:text-lg leading-tight">
                      3 Separate Apps for 3 Different Phones
                    </h3>
                    <span className="text-[10px] font-extrabold uppercase tracking-wider bg-emerald-400/20 text-[#A7F3D0] border border-emerald-300/30 px-2 py-0.5 rounded-full">
                      PWA Multi-App Hub
                    </span>
                  </div>
                  <p className="text-xs text-emerald-100/85 mt-0.5">
                    Customer Phone • Rider Partner Phone • Admin Enterprise Phone (Real-time Sync)
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setShowSeparateAppsModal(false)}
                className="p-1.5 rounded-lg bg-white/10 hover:bg-white/20 text-emerald-100 hover:text-white transition cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Modal Body */}
            <div className="p-5 space-y-4 overflow-y-auto">
              <div className="text-xs text-slate-600 leading-relaxed bg-emerald-50/80 p-3 rounded-xl border border-emerald-200">
                💡 <strong>How it works:</strong> Each phone opens its dedicated URL below in{' '}
                <strong>Google Chrome</strong>. Chrome will offer an &quot;Install App&quot; prompt
                or you can tap the top <strong>📲 Install Button</strong> (or Chrome menu{' '}
                <strong>⋮ &gt; Install app / Add to Home screen</strong>). Each phone gets its own
                isolated standalone app icon while sharing the exact same live order bookings and
                database!
              </div>

              {/* 3 Dedicated App Cards */}
              <div className="space-y-3">
                {/* 1. Customer App Card */}
                <div className="p-4 rounded-xl border-2 border-emerald-300 bg-white shadow-xs space-y-2.5">
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-2.5">
                      <div className="w-9 h-9 rounded-lg bg-emerald-100 text-[#042F2E] flex items-center justify-center text-lg font-black shrink-0">
                        🛍️
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-[10px] font-black uppercase tracking-wider bg-emerald-100 text-emerald-800 px-2 py-0.5 rounded">
                            Phone 1: Customer
                          </span>
                          <h4 className="font-extrabold text-sm text-slate-900">Quick Delivery</h4>
                        </div>
                        <p className="text-[11px] text-slate-500 mt-0.5">
                          2-step parcel booking, vehicle selection, live OTP handover &amp; GST invoices
                        </p>
                      </div>
                    </div>
                  </div>

                  <div className="p-2.5 rounded-lg bg-slate-50 border border-slate-200 font-mono text-[11px] text-slate-700 break-all select-all flex items-center justify-between gap-2">
                    <span>
                      {(typeof window !== 'undefined' ? window.location.origin : '') +
                        '/?app=customer'}
                    </span>
                  </div>

                  <div className="flex items-center gap-2 pt-1">
                    <button
                      type="button"
                      onClick={() =>
                        handleCopyLink(
                          'customer',
                          (typeof window !== 'undefined' ? window.location.origin : '') +
                            '/?app=customer'
                        )
                      }
                      className="px-3 py-1.5 rounded-lg bg-[#059669] hover:bg-emerald-600 text-white text-xs font-bold flex items-center gap-1.5 transition cursor-pointer"
                    >
                      {copiedLinkKey === 'customer' ? (
                        <>
                          <Check className="w-3.5 h-3.5" />
                          <span>Copied Customer Link!</span>
                        </>
                      ) : (
                        <>
                          <Copy className="w-3.5 h-3.5" />
                          <span>Copy Link for Phone 1</span>
                        </>
                      )}
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setShowSeparateAppsModal(false);
                        setDedicatedAppMode('customer');
                        setViewMode('TABS');
                        setActiveTab('CUSTOMER');
                        window.history.pushState({ app: 'customer' }, '', '/?app=customer');
                      }}
                      className="px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold flex items-center gap-1.5 transition cursor-pointer"
                    >
                      <ExternalLink className="w-3.5 h-3.5" />
                      <span>Open Customer App</span>
                    </button>
                  </div>
                </div>

                {/* 2. Rider Partner App Card */}
                <div className="p-4 rounded-xl border-2 border-amber-300 bg-white shadow-xs space-y-2.5">
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-2.5">
                      <div className="w-9 h-9 rounded-lg bg-amber-100 text-amber-950 flex items-center justify-center text-lg font-black shrink-0">
                        🛵
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-[10px] font-black uppercase tracking-wider bg-amber-100 text-amber-900 px-2 py-0.5 rounded">
                            Phone 2: Rider Fleet
                          </span>
                          <h4 className="font-extrabold text-sm text-slate-900">
                            QD Rider Partner
                          </h4>
                        </div>
                        <p className="text-[11px] text-slate-500 mt-0.5">
                          On/Off duty toggle, incoming dispatch radar, 80% partner net share &amp; UPI payouts
                        </p>
                      </div>
                    </div>
                  </div>

                  <div className="p-2.5 rounded-lg bg-slate-50 border border-slate-200 font-mono text-[11px] text-slate-700 break-all select-all flex items-center justify-between gap-2">
                    <span>
                      {(typeof window !== 'undefined' ? window.location.origin : '') +
                        '/?app=rider'}
                    </span>
                  </div>

                  <div className="flex items-center gap-2 pt-1">
                    <button
                      type="button"
                      onClick={() =>
                        handleCopyLink(
                          'rider',
                          (typeof window !== 'undefined' ? window.location.origin : '') +
                            '/?app=rider'
                        )
                      }
                      className="px-3 py-1.5 rounded-lg bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold flex items-center gap-1.5 transition cursor-pointer"
                    >
                      {copiedLinkKey === 'rider' ? (
                        <>
                          <Check className="w-3.5 h-3.5" />
                          <span>Copied Rider Link!</span>
                        </>
                      ) : (
                        <>
                          <Copy className="w-3.5 h-3.5" />
                          <span>Copy Link for Phone 2</span>
                        </>
                      )}
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setShowSeparateAppsModal(false);
                        setDedicatedAppMode('rider');
                        setViewMode('TABS');
                        setActiveTab('RIDER');
                        window.history.pushState({ app: 'rider' }, '', '/?app=rider');
                      }}
                      className="px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold flex items-center gap-1.5 transition cursor-pointer"
                    >
                      <ExternalLink className="w-3.5 h-3.5" />
                      <span>Open Rider Partner App</span>
                    </button>
                  </div>
                </div>

                {/* 3. Admin Enterprise Command Center Card */}
                <div className="p-4 rounded-xl border-2 border-teal-400 bg-white shadow-xs space-y-2.5">
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-2.5">
                      <div className="w-9 h-9 rounded-lg bg-teal-100 text-teal-950 flex items-center justify-center text-lg font-black shrink-0">
                        💼
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-[10px] font-black uppercase tracking-wider bg-teal-100 text-teal-900 px-2 py-0.5 rounded">
                            Phone 3 / PC: Dispatch Admin
                          </span>
                          <h4 className="font-extrabold text-sm text-slate-900">
                            QD Admin Enterprise
                          </h4>
                        </div>
                        <p className="text-[11px] text-slate-500 mt-0.5">
                          Enterprise command center, live fleet GPS map, 20% platform share audit &amp; pricing controls
                        </p>
                      </div>
                    </div>
                  </div>

                  <div className="p-2.5 rounded-lg bg-slate-50 border border-slate-200 font-mono text-[11px] text-slate-700 break-all select-all flex items-center justify-between gap-2">
                    <span>
                      {(typeof window !== 'undefined' ? window.location.origin : '') +
                        '/?app=admin'}
                    </span>
                  </div>

                  <div className="flex items-center gap-2 pt-1">
                    <button
                      type="button"
                      onClick={() =>
                        handleCopyLink(
                          'admin',
                          (typeof window !== 'undefined' ? window.location.origin : '') +
                            '/?app=admin'
                        )
                      }
                      className="px-3 py-1.5 rounded-lg bg-teal-700 hover:bg-teal-800 text-white text-xs font-bold flex items-center gap-1.5 transition cursor-pointer"
                    >
                      {copiedLinkKey === 'admin' ? (
                        <>
                          <Check className="w-3.5 h-3.5" />
                          <span>Copied Admin Link!</span>
                        </>
                      ) : (
                        <>
                          <Copy className="w-3.5 h-3.5" />
                          <span>Copy Link for Phone 3</span>
                        </>
                      )}
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setShowSeparateAppsModal(false);
                        setDedicatedAppMode('admin');
                        setViewMode('TABS');
                        setActiveTab('ADMIN');
                        window.history.pushState({ app: 'admin' }, '', '/?app=admin');
                      }}
                      className="px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold flex items-center gap-1.5 transition cursor-pointer"
                    >
                      <ExternalLink className="w-3.5 h-3.5" />
                      <span>Open Admin App</span>
                    </button>
                  </div>
                </div>
              </div>

              {/* CAPACITOR & NATIVE ANDROID APK BUILD COMMANDS */}
              <div className="p-4 rounded-xl border-2 border-slate-800 bg-slate-900 text-slate-100 space-y-2.5">
                <div className="flex items-center justify-between">
                  <span className="text-[11px] font-black uppercase tracking-wider text-emerald-400">
                    📦 Build 3 Separate Android APKs (Capacitor CLI / Gradle)
                  </span>
                  <span className="text-[10px] font-mono bg-emerald-500/20 text-emerald-300 px-2 py-0.5 rounded">
                    capacitor.config.ts
                  </span>
                </div>
                <p className="text-[11px] text-slate-300 leading-relaxed">
                  Each APK compiles with its own locked role (<code className="text-emerald-300">VITE_APP_ROLE</code>), unique Android <code className="text-emerald-300">appId</code>, and custom launcher name:
                </p>
                <div className="grid grid-cols-1 gap-1.5 font-mono text-[11px] bg-slate-950 p-3 rounded-lg border border-slate-800">
                  <div className="text-emerald-400"># 1. Customer APK (com.quickdelivery.customer)</div>
                  <div className="text-slate-200 select-all">npm run apk:customer</div>
                  <div className="text-amber-400 mt-1"># 2. Rider Partner APK (com.quickdelivery.rider)</div>
                  <div className="text-slate-200 select-all">npm run apk:rider</div>
                  <div className="text-teal-400 mt-1"># 3. Admin Enterprise APK (com.quickdelivery.admin)</div>
                  <div className="text-slate-200 select-all">npm run apk:admin</div>
                </div>
              </div>
            </div>

            {/* Modal Footer */}
            <div className="p-4 bg-slate-50 border-t border-slate-200 flex flex-wrap items-center justify-between gap-3 shrink-0">
              <button
                type="button"
                onClick={handleCopyAllLinks}
                className="px-4 py-2 rounded-xl bg-gradient-to-r from-[#042F2E] to-[#059669] hover:from-[#064E3B] hover:to-emerald-600 text-white font-extrabold text-xs shadow-md transition flex items-center gap-1.5 cursor-pointer"
              >
                {copiedAllLinks ? (
                  <>
                    <Check className="w-4 h-4 text-[#A7F3D0]" />
                    <span>✓ All 3 App Links Copied to Clipboard!</span>
                  </>
                ) : (
                  <>
                    <Share2 className="w-4 h-4" />
                    <span>📋 Copy All 3 Links (WhatsApp / Share Ready)</span>
                  </>
                )}
              </button>

              <button
                type="button"
                onClick={() => setShowSeparateAppsModal(false)}
                className="px-4 py-2 rounded-xl bg-white border border-slate-300 hover:bg-slate-100 text-slate-700 font-bold text-xs transition cursor-pointer"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 2-Step PWA Install Guide Modal */}
      {showInstallGuideModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm p-4 flex items-center justify-center">
          <div className="w-full max-w-lg rounded-2xl bg-white border-2 border-emerald-500 shadow-2xl overflow-hidden flex flex-col animate-in fade-in">
            {/* Modal Header */}
            <div className="bg-gradient-to-r from-[#042F2E] via-[#065F46] to-[#059669] text-white px-5 py-4 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-white/10 border border-emerald-300/40 flex items-center justify-center text-xl shadow-inner">
                  📲
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="font-extrabold text-base leading-tight">
                      {dedicatedAppMode === 'customer' && 'Install Quick Delivery (Customer App)'}
                      {dedicatedAppMode === 'rider' && 'Install QD Rider Partner App'}
                      {dedicatedAppMode === 'admin' && 'Install QD Admin Enterprise App'}
                      {!dedicatedAppMode && 'Install Quick Delivery App'}
                    </h3>
                    <span className="text-[10px] font-extrabold uppercase tracking-wider bg-emerald-400/20 text-[#A7F3D0] border border-emerald-300/30 px-2 py-0.5 rounded-full">
                      PWA Standalone
                    </span>
                  </div>
                  <p className="text-xs text-emerald-100/85">
                    100% Android Native Experience • Offline Shell
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setShowInstallGuideModal(false)}
                className="p-1.5 rounded-lg bg-white/10 hover:bg-white/20 text-emerald-100 hover:text-white transition cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Modal Body */}
            <div className="p-5 space-y-4">
              {isAppInstalled ? (
                <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-center space-y-2">
                  <div className="w-12 h-12 rounded-full bg-emerald-100 text-[#059669] flex items-center justify-center mx-auto text-2xl font-black">
                    ✓
                  </div>
                  <h4 className="font-extrabold text-slate-900 text-sm">
                    {dedicatedAppMode === 'customer'
                      ? 'Customer App is Already Installed!'
                      : dedicatedAppMode === 'rider'
                      ? 'Rider Partner App is Already Installed!'
                      : dedicatedAppMode === 'admin'
                      ? 'Admin Enterprise App is Already Installed!'
                      : 'Quick Delivery is Already Installed!'}
                  </h4>
                  <p className="text-xs text-slate-600">
                    You are currently using this app in standalone mode or it has already been added to
                    your device&apos;s home screen.
                  </p>
                </div>
              ) : (
                <>
                  <div className="text-xs text-slate-600 leading-relaxed">
                    Install{' '}
                    <strong>
                      {dedicatedAppMode === 'customer'
                        ? 'Quick Delivery (Customer App)'
                        : dedicatedAppMode === 'rider'
                        ? 'QD Rider Partner App'
                        : dedicatedAppMode === 'admin'
                        ? 'QD Admin Enterprise App'
                        : 'Quick Delivery'}
                    </strong>{' '}
                    directly from your Chrome browser to your phone without app store restrictions.
                    Enjoy full-screen standalone performance:
                  </div>

                  {/* 2-Step Android Chrome Instructions */}
                  <div className="space-y-3">
                    <div className="p-3.5 rounded-xl bg-emerald-50/70 border border-emerald-200 flex items-start gap-3">
                      <div className="w-7 h-7 rounded-lg bg-[#059669] text-white flex items-center justify-center font-black text-xs shrink-0 mt-0.5">
                        1
                      </div>
                      <div>
                        <h4 className="font-bold text-xs text-slate-900">
                          Tap Chrome&apos;s 3-Dot Menu (⋮)
                        </h4>
                        <p className="text-[11px] text-slate-600 mt-0.5">
                          Tap the three vertical dots <strong>(⋮)</strong> in the top right corner of
                          your Chrome mobile browser.
                        </p>
                      </div>
                    </div>

                    <div className="p-3.5 rounded-xl bg-emerald-50/70 border border-emerald-200 flex items-start gap-3">
                      <div className="w-7 h-7 rounded-lg bg-[#059669] text-white flex items-center justify-center font-black text-xs shrink-0 mt-0.5">
                        2
                      </div>
                      <div>
                        <h4 className="font-bold text-xs text-slate-900">
                          Select &quot;Install app&quot; or &quot;Add to Home screen&quot;
                        </h4>
                        <p className="text-[11px] text-slate-600 mt-0.5">
                          Tap <strong>Install app</strong> (or <em>Add to Home screen</em>) and
                          confirm. The app icon will be added to your mobile home screen!
                        </p>
                      </div>
                    </div>
                  </div>

                  {/* iOS Safari Guide */}
                  <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 text-[11px] text-slate-600 flex items-center gap-2">
                    <span className="text-base">🍎</span>
                    <span>
                      <strong>iPhone / iPad Safari:</strong> Tap the <strong>Share</strong> button
                      (⎋), scroll down, and tap <strong>Add to Home Screen</strong>.
                    </span>
                  </div>
                </>
              )}

              {/* Actions */}
              <div className="flex items-center justify-end gap-2.5 pt-2 border-t border-slate-200">
                {deferredInstallPrompt && !isAppInstalled && (
                  <button
                    type="button"
                    onClick={handleTriggerInstall}
                    className="px-4 py-2 rounded-xl bg-[#059669] hover:bg-emerald-600 text-white font-extrabold text-xs shadow-md transition flex items-center gap-1.5 cursor-pointer"
                  >
                    <Download className="w-4 h-4" />
                    <span>Prompt Install Now</span>
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => setShowInstallGuideModal(false)}
                  className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs transition cursor-pointer"
                >
                  Got it, Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 💳 SHOP-FLOOR DYNAMIC & STATIC UPI QR MODAL */}
      <ShopFloorUpiQrModal
        isOpen={showUpiQrModal}
        onClose={() => setShowUpiQrModal(false)}
      />

      {/* 📷 CAMERA-BASED BARCODE & QR SCANNER FOR PARCEL HANDOVER */}
      <ParcelQrScannerModal
        isOpen={showParcelScannerModal}
        onClose={() => setShowParcelScannerModal(false)}
        orders={orders}
        onConfirmHandover={(orderId, note) => {
          setAdminConfigSavedNotice(`✅ Parcel ${orderId}: ${note}`);
        }}
      />

      {/* 🗄️ CLOUD DATABASE & SYNC MANAGER (FIREBASE / SUPABASE) */}
      <DatabaseConfigModal
        isOpen={showDatabaseModal}
        onClose={() => setShowDatabaseModal(false)}
        orders={orders}
        onOrdersRestored={(restored) => {
          setOrders(restored);
          restored.forEach((o) => dbService.saveOrder(o));
        }}
      />

      {/* 📍 LIVE ORDER GPS TRACKING & TELEMETRY MODAL */}
      <LiveOrderTrackingModal
        isOpen={Boolean(selectedTrackingOrder)}
        onClose={() => setSelectedTrackingOrder(null)}
        order={selectedTrackingOrder}
        onOpenChat={(orderId) => {
          setSelectedTrackingOrder(null);
          setCustomerChatModalOpen(true);
        }}
      />

      {/* 🔔 FLOATING REAL-TIME IN-APP NOTIFICATION TOAST */}
      <NotificationToastContainer
        onActionClick={(toast) => {
          if (toast.orderCode) {
            const found = orders.find(
              (o) => o.orderCode.toLowerCase() === toast.orderCode?.toLowerCase()
            );
            if (found) {
              setSelectedTrackingOrder(found);
            }
          }
          if (toast.role === 'RIDER') {
            setRiderSubTab('ACTIVE_TRIP');
          } else if (toast.role === 'CUSTOMER') {
            setCustomerSubTab('LIVE_TRACKING');
          }
        }}
      />

      {/* 🔔 PUSH NOTIFICATION & REAL-TIME ALERTS CENTER MODAL */}
      <NotificationCenterModal
        isOpen={showNotificationModal}
        onClose={() => setShowNotificationModal(false)}
        currentRole={currentUser.role}
        onNavigateToOrder={(code) => {
          const found = orders.find((o) => o.orderCode.toLowerCase() === code.toLowerCase());
          if (found) {
            setSelectedTrackingOrder(found);
          }
        }}
        onNavigateToRiderTrip={() => {
          setRiderSubTab('ACTIVE_TRIP');
        }}
      />

      {/* 📸 IMAGE UPLOAD & DELIVERY PROOF MODAL */}
      {imageUploadModalState?.isOpen && (
        <ImageUploadModal
          isOpen={imageUploadModalState.isOpen}
          onClose={() => setImageUploadModalState(null)}
          onPhotoSelected={(photoUrl, note) => {
            if (imageUploadModalState.category === 'DELIVERY_PROOF') {
              if (imageUploadModalState.orderId) {
                handleSelectDeliveryPhoto(imageUploadModalState.orderId, photoUrl, note);
              }
            } else {
              handleUpdateProfilePhoto(photoUrl);
            }
          }}
          category={imageUploadModalState.category}
          title={imageUploadModalState.title}
          subtitle={imageUploadModalState.subtitle}
          initialImageUrl={imageUploadModalState.currentImageUrl}
        />
      )}
    </div>
  );
}

