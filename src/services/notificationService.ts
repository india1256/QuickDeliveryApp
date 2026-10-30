/**
 * Quick Delivery - Push Notification & Real-Time Alert Service
 * Integrates Web Push Notifications (Notification API), Web Audio synthesized sound chimes,
 * device haptic vibration, and real-time in-app notification state management.
 */

export type NotificationRole = 'CUSTOMER' | 'RIDER' | 'ADMIN' | 'ALL';

export type NotificationType =
  | 'ORDER_PLACED'
  | 'DISPATCH_NEW'
  | 'ORDER_ASSIGNED'
  | 'RIDER_ARRIVED'
  | 'ORDER_PICKED_UP'
  | 'ORDER_DELIVERED'
  | 'COD_COLLECTED'
  | 'SYSTEM_ALERT';

export type NotificationSound = 'ORDER_DISPATCH' | 'STATUS_UPDATE' | 'DELIVERED' | 'ALERT';

export interface NotificationItem {
  id: string;
  title: string;
  message: string;
  role: NotificationRole;
  type: NotificationType;
  timestamp: number;
  timeFormatted: string;
  read: boolean;
  orderCode?: string;
  orderId?: string;
  soundType?: NotificationSound;
  actionLabel?: string;
}

export interface ToastItem extends NotificationItem {
  durationMs?: number;
}

type NotificationSubscriber = (notifications: NotificationItem[]) => void;
type ToastSubscriber = (toast: ToastItem | null) => void;

class NotificationService {
  private notifications: NotificationItem[] = [];
  private activeToast: ToastItem | null = null;
  private toastTimeout: any = null;
  private notificationSubscribers: Set<NotificationSubscriber> = new Set();
  private toastSubscribers: Set<ToastSubscriber> = new Set();
  private audioCtx: AudioContext | null = null;
  private isSoundEnabled: boolean = true;

  constructor() {
    this.loadState();
  }

  private loadState() {
    try {
      const soundPref = localStorage.getItem('quick_delivery_sound_enabled');
      if (soundPref !== null) {
        this.isSoundEnabled = soundPref === 'true';
      }

      const stored = localStorage.getItem('quick_delivery_notifications_v2');
      if (stored) {
        const parsed = JSON.parse(stored);
        this.notifications = Array.isArray(parsed)
          ? parsed.filter((n: any) => n && !String(n.id).startsWith('notif_init_'))
          : [];
      } else {
        this.notifications = [];
        this.saveState();
      }
    } catch (err) {
      console.warn('Could not load notification state:', err);
    }
  }

  private saveState() {
    try {
      localStorage.setItem('quick_delivery_notifications_v2', JSON.stringify(this.notifications.slice(0, 50)));
      localStorage.setItem('quick_delivery_sound_enabled', String(this.isSoundEnabled));
    } catch (err) {
      console.warn('Could not save notification state:', err);
    }
  }

  public getNotifications(): NotificationItem[] {
    return [...this.notifications];
  }

  public getUnreadCount(role?: NotificationRole): number {
    return this.notifications.filter((n) => {
      if (n.read) return false;
      if (!role || role === 'ALL') return true;
      return n.role === role || n.role === 'ALL';
    }).length;
  }

  public subscribe(cb: NotificationSubscriber): () => void {
    this.notificationSubscribers.add(cb);
    cb(this.getNotifications());
    return () => {
      this.notificationSubscribers.delete(cb);
    };
  }

  public subscribeToasts(cb: ToastSubscriber): () => void {
    this.toastSubscribers.add(cb);
    cb(this.activeToast);
    return () => {
      this.toastSubscribers.delete(cb);
    };
  }

  private notifySubscribers() {
    const list = this.getNotifications();
    this.notificationSubscribers.forEach((cb) => cb(list));
    this.saveState();
  }

  private notifyToastSubscribers() {
    this.toastSubscribers.forEach((cb) => cb(this.activeToast));
  }

  /**
   * Browser Push Notification Permission
   */
  public getPermissionStatus(): NotificationPermission {
    if (typeof window === 'undefined' || !('Notification' in window)) {
      return 'denied';
    }
    return Notification.permission;
  }

  public async requestPermission(): Promise<NotificationPermission> {
    if (typeof window === 'undefined' || !('Notification' in window)) {
      return 'denied';
    }
    try {
      const res = await Notification.requestPermission();
      if (res === 'granted') {
        this.playSynthesizedSound('STATUS_UPDATE');
        this.sendNotification({
          title: '🔔 Push Notifications Enabled!',
          message: 'You will now receive instant push alerts for orders, pickups, and deliveries.',
          role: 'ALL',
          type: 'SYSTEM_ALERT',
          soundType: 'STATUS_UPDATE',
        });
      }
      return res;
    } catch (err) {
      console.warn('Error requesting notification permission:', err);
      return 'denied';
    }
  }

  /**
   * Sound & Vibration Synthesizer (Web Audio API)
   */
  public isSoundActive(): boolean {
    return this.isSoundEnabled;
  }

  public toggleSound(enabled?: boolean): boolean {
    this.isSoundEnabled = enabled !== undefined ? enabled : !this.isSoundEnabled;
    this.saveState();
    if (this.isSoundEnabled) {
      this.playSynthesizedSound('STATUS_UPDATE');
    }
    return this.isSoundEnabled;
  }

  public playSynthesizedSound(type: NotificationSound) {
    if (!this.isSoundEnabled || typeof window === 'undefined') return;

    try {
      const AudioCtxClass = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioCtxClass) return;

      if (!this.audioCtx || this.audioCtx.state === 'suspended') {
        this.audioCtx = new AudioCtxClass();
      }

      const ctx = this.audioCtx;
      if (ctx.state === 'suspended') {
        ctx.resume();
      }

      const now = ctx.currentTime;

      if (type === 'ORDER_DISPATCH') {
        // High-priority 3-note ascending alert for Rider Dispatch (C5 -> E5 -> G5)
        [523.25, 659.25, 783.99].forEach((freq, idx) => {
          const osc = ctx.createOscillator();
          const gain = ctx.createGain();
          osc.type = 'triangle';
          osc.frequency.setValueAtTime(freq, now + idx * 0.12);
          gain.gain.setValueAtTime(0, now + idx * 0.12);
          gain.gain.linearRampToValueAtTime(0.25, now + idx * 0.12 + 0.02);
          gain.gain.exponentialRampToValueAtTime(0.001, now + idx * 0.12 + 0.16);
          osc.connect(gain);
          gain.connect(ctx.destination);
          osc.start(now + idx * 0.12);
          osc.stop(now + idx * 0.12 + 0.18);
        });
      } else if (type === 'DELIVERED') {
        // Celebratory 4-note chord (C5 -> E5 -> G5 -> C6)
        [523.25, 659.25, 783.99, 1046.5].forEach((freq, idx) => {
          const osc = ctx.createOscillator();
          const gain = ctx.createGain();
          osc.type = 'sine';
          osc.frequency.setValueAtTime(freq, now + idx * 0.09);
          gain.gain.setValueAtTime(0, now + idx * 0.09);
          gain.gain.linearRampToValueAtTime(0.22, now + idx * 0.09 + 0.02);
          gain.gain.exponentialRampToValueAtTime(0.001, now + idx * 0.09 + 0.25);
          osc.connect(gain);
          gain.connect(ctx.destination);
          osc.start(now + idx * 0.09);
          osc.stop(now + idx * 0.09 + 0.28);
        });
      } else if (type === 'ALERT') {
        // Warning 2-pulse ping (880Hz -> 440Hz)
        [880, 440].forEach((freq, idx) => {
          const osc = ctx.createOscillator();
          const gain = ctx.createGain();
          osc.type = 'sawtooth';
          osc.frequency.setValueAtTime(freq, now + idx * 0.14);
          gain.gain.setValueAtTime(0, now + idx * 0.14);
          gain.gain.linearRampToValueAtTime(0.18, now + idx * 0.14 + 0.02);
          gain.gain.exponentialRampToValueAtTime(0.001, now + idx * 0.14 + 0.15);
          osc.connect(gain);
          gain.connect(ctx.destination);
          osc.start(now + idx * 0.14);
          osc.stop(now + idx * 0.14 + 0.16);
        });
      } else {
        // Gentle pleasant double chime for status updates (587.33 D5 -> 880 A5)
        [587.33, 880].forEach((freq, idx) => {
          const osc = ctx.createOscillator();
          const gain = ctx.createGain();
          osc.type = 'sine';
          osc.frequency.setValueAtTime(freq, now + idx * 0.1);
          gain.gain.setValueAtTime(0, now + idx * 0.1);
          gain.gain.linearRampToValueAtTime(0.2, now + idx * 0.1 + 0.02);
          gain.gain.exponentialRampToValueAtTime(0.001, now + idx * 0.1 + 0.2);
          osc.connect(gain);
          gain.connect(ctx.destination);
          osc.start(now + idx * 0.1);
          osc.stop(now + idx * 0.1 + 0.22);
        });
      }

      // Haptic feedback vibration on mobile devices
      if ('vibrate' in navigator) {
        if (type === 'ORDER_DISPATCH') {
          navigator.vibrate([180, 80, 180, 80, 240]);
        } else if (type === 'DELIVERED') {
          navigator.vibrate([100, 50, 100, 50, 200]);
        } else {
          navigator.vibrate([120, 60, 120]);
        }
      }
    } catch (e) {
      console.warn('Audio synthesis note failed:', e);
    }
  }

  /**
   * Send a real-time Notification across System, Web Push, and Floating In-App Toast
   */
  public sendNotification(params: {
    title: string;
    message: string;
    role: NotificationRole;
    type: NotificationType;
    orderCode?: string;
    orderId?: string;
    soundType?: NotificationSound;
    actionLabel?: string;
  }): NotificationItem {
    const sound = params.soundType || (params.type === 'DISPATCH_NEW' ? 'ORDER_DISPATCH' : params.type === 'ORDER_DELIVERED' ? 'DELIVERED' : 'STATUS_UPDATE');

    const newItem: NotificationItem = {
      id: `notif_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
      title: params.title,
      message: params.message,
      role: params.role,
      type: params.type,
      timestamp: Date.now(),
      timeFormatted: 'Just now',
      read: false,
      orderCode: params.orderCode,
      orderId: params.orderId,
      soundType: sound,
      actionLabel: params.actionLabel,
    };

    // Prepend to in-memory store
    this.notifications = [newItem, ...this.notifications];
    this.notifySubscribers();

    // Play Sound & Haptics
    this.playSynthesizedSound(sound);

    // Show In-App Floating Toast
    this.triggerToast(newItem);

    // Trigger Native HTML5 Web Push Notification if permission granted
    if (typeof window !== 'undefined' && 'Notification' in window && Notification.permission === 'granted') {
      try {
        new Notification(params.title, {
          body: params.message,
          icon: '/favicon.ico',
          tag: params.orderCode || 'quick-delivery-alert',
        });
      } catch (err) {
        console.warn('Native notification trigger failed:', err);
      }
    }

    return newItem;
  }

  private triggerToast(item: NotificationItem) {
    if (this.toastTimeout) {
      clearTimeout(this.toastTimeout);
    }
    this.activeToast = { ...item, durationMs: 6500 };
    this.notifyToastSubscribers();

    this.toastTimeout = setTimeout(() => {
      this.dismissToast();
    }, 6500);
  }

  public dismissToast() {
    if (this.toastTimeout) {
      clearTimeout(this.toastTimeout);
      this.toastTimeout = null;
    }
    this.activeToast = null;
    this.notifyToastSubscribers();
  }

  public markAsRead(id: string) {
    this.notifications = this.notifications.map((n) => (n.id === id ? { ...n, read: true } : n));
    this.notifySubscribers();
  }

  public markAllAsRead(role?: NotificationRole) {
    this.notifications = this.notifications.map((n) => {
      if (!role || role === 'ALL' || n.role === role || n.role === 'ALL') {
        return { ...n, read: true };
      }
      return n;
    });
    this.notifySubscribers();
  }

  public clearAll() {
    this.notifications = [];
    this.notifySubscribers();
  }
}

export const notificationService = new NotificationService();
