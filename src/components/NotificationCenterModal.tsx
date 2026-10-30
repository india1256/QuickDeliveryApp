import React, { useState, useEffect } from 'react';
import {
  Bell,
  X,
  Check,
  CheckCheck,
  Trash2,
  Volume2,
  VolumeX,
  Send,
  Navigation,
  Package,
  Bike,
  Coins,
  CheckCircle2,
  ShieldCheck,
  AlertTriangle,
  ExternalLink,
  Smartphone,
  Radio
} from 'lucide-react';
import {
  notificationService,
  type NotificationItem,
  type NotificationRole,
  type NotificationType
} from '../services/notificationService';

interface NotificationCenterModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentRole: 'CUSTOMER' | 'RIDER' | 'ADMIN';
  onNavigateToOrder?: (orderCode: string) => void;
  onNavigateToRiderTrip?: () => void;
}

export const NotificationCenterModal: React.FC<NotificationCenterModalProps> = ({
  isOpen,
  onClose,
  currentRole,
  onNavigateToOrder,
  onNavigateToRiderTrip,
}) => {
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [selectedRoleFilter, setSelectedRoleFilter] = useState<NotificationRole>('ALL');
  const [permissionStatus, setPermissionStatus] = useState<NotificationPermission>(() =>
    notificationService.getPermissionStatus()
  );
  const [soundEnabled, setSoundEnabled] = useState<boolean>(() =>
    notificationService.isSoundActive()
  );
  const [testSentMessage, setTestSentMessage] = useState<string | null>(null);

  useEffect(() => {
    const unsub = notificationService.subscribe((list) => {
      setNotifications(list);
    });
    return () => unsub();
  }, []);

  useEffect(() => {
    setPermissionStatus(notificationService.getPermissionStatus());
  }, [isOpen]);

  if (!isOpen) return null;

  const handleRequestPush = async () => {
    const res = await notificationService.requestPermission();
    setPermissionStatus(res);
  };

  const handleToggleSound = () => {
    const next = notificationService.toggleSound();
    setSoundEnabled(next);
  };

  const handleTriggerTest = (type: 'DISPATCH' | 'PICKUP' | 'DELIVERY' | 'COD') => {
    if (type === 'DISPATCH') {
      notificationService.sendNotification({
        title: '🔔 New Dispatch Available!',
        message: 'Order #QD-9921 from Indiranagar 100ft Rd to Whitefield (7.4 km • ₹78 payout). Tap to accept!',
        role: 'RIDER',
        type: 'DISPATCH_NEW',
        orderCode: 'QD-9921',
        soundType: 'ORDER_DISPATCH',
        actionLabel: 'Accept Trip',
      });
      setTestSentMessage('Sent Rider Dispatch Alert with 3-tone chime!');
    } else if (type === 'PICKUP') {
      notificationService.sendNotification({
        title: '📦 Parcel Picked Up by Vikram Singh',
        message: 'Handover OTP verified. Your courier is now on the way to recipient.',
        role: 'CUSTOMER',
        type: 'ORDER_PICKED_UP',
        orderCode: 'QD-4821',
        soundType: 'STATUS_UPDATE',
        actionLabel: 'Track GPS',
      });
      setTestSentMessage('Sent Customer Pickup Alert with double chime!');
    } else if (type === 'DELIVERY') {
      notificationService.sendNotification({
        title: '🎉 Delivery Completed & Confirmed!',
        message: 'Order #QD-4821 was successfully delivered to doorstep. Drop OTP verified.',
        role: 'CUSTOMER',
        type: 'ORDER_DELIVERED',
        orderCode: 'QD-4821',
        soundType: 'DELIVERED',
        actionLabel: 'View Invoice',
      });
      setTestSentMessage('Sent Delivery Complete Alert with celebratory chord!');
    } else if (type === 'COD') {
      notificationService.sendNotification({
        title: '💵 Cash Handover Confirmed',
        message: '₹140 cash on delivery received by rider partner for #QD-5192.',
        role: 'CUSTOMER',
        type: 'COD_COLLECTED',
        orderCode: 'QD-5192',
        soundType: 'STATUS_UPDATE',
        actionLabel: 'Receipt',
      });
      setTestSentMessage('Sent COD Cash Collection Alert!');
    }
    setTimeout(() => setTestSentMessage(null), 3500);
  };

  const filteredNotifications = notifications.filter((n) => {
    if (selectedRoleFilter === 'ALL') return true;
    return n.role === selectedRoleFilter || n.role === 'ALL';
  });

  const unreadCount = notificationService.getUnreadCount(selectedRoleFilter);

  const getIcon = (type: NotificationType) => {
    switch (type) {
      case 'ORDER_PLACED':
        return <Package className="w-4 h-4 text-emerald-600" />;
      case 'DISPATCH_NEW':
        return <Bell className="w-4 h-4 text-amber-600" />;
      case 'ORDER_ASSIGNED':
        return <Bike className="w-4 h-4 text-emerald-600" />;
      case 'RIDER_ARRIVED':
        return <Navigation className="w-4 h-4 text-teal-600" />;
      case 'ORDER_PICKED_UP':
        return <Package className="w-4 h-4 text-blue-600" />;
      case 'ORDER_DELIVERED':
        return <CheckCircle2 className="w-4 h-4 text-emerald-600" />;
      case 'COD_COLLECTED':
        return <Coins className="w-4 h-4 text-amber-600" />;
      case 'SYSTEM_ALERT':
      default:
        return <ShieldCheck className="w-4 h-4 text-emerald-600" />;
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm p-3 sm:p-4 flex items-center justify-center animate-in fade-in overflow-y-auto">
      <div className="w-full max-w-2xl rounded-2xl bg-white border-2 border-emerald-500 shadow-2xl overflow-hidden flex flex-col my-auto max-h-[92vh]">
        
        {/* Header */}
        <div className="bg-gradient-to-r from-[#042F2E] via-[#065F46] to-[#059669] text-white px-5 py-4 flex items-center justify-between shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-white/10 border border-emerald-300/40 flex items-center justify-center text-xl shadow-inner relative">
              <Bell className="w-6 h-6 text-[#A7F3D0]" />
              {unreadCount > 0 && (
                <span className="absolute -top-1 -right-1 w-5 h-5 rounded-full bg-amber-400 text-slate-950 font-black text-[10px] flex items-center justify-center border-2 border-[#042F2E]">
                  {unreadCount}
                </span>
              )}
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="font-extrabold text-base sm:text-lg leading-tight">
                  Push Notifications &amp; Alerts
                </h3>
                <span className="text-[10px] font-extrabold uppercase tracking-wider bg-emerald-400/20 text-[#A7F3D0] border border-emerald-300/30 px-2 py-0.5 rounded-full">
                  Real-time
                </span>
              </div>
              <p className="text-xs text-emerald-100/85 mt-0.5">
                Instant delivery status updates, rider dispatch chimes &amp; OTP handovers
              </p>
            </div>
          </div>

          <div className="flex items-center gap-1.5">
            {/* Audio Toggle */}
            <button
              type="button"
              onClick={handleToggleSound}
              className={`p-2 rounded-xl border transition cursor-pointer ${
                soundEnabled
                  ? 'bg-emerald-500/20 border-emerald-400/50 text-[#A7F3D0] hover:bg-emerald-500/30'
                  : 'bg-white/10 border-white/20 text-slate-300 hover:text-white'
              }`}
              title={soundEnabled ? 'Mute synthesized sound chimes' : 'Enable synthesized sound chimes'}
            >
              {soundEnabled ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4" />}
            </button>

            {/* Close */}
            <button
              type="button"
              onClick={onClose}
              className="p-2 rounded-xl bg-white/10 hover:bg-white/20 text-emerald-100 hover:text-white transition cursor-pointer"
              title="Close modal"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Content Body */}
        <div className="p-4 sm:p-5 overflow-y-auto space-y-4 flex-1 bg-slate-50/70">
          
          {/* 1. Browser Web Push Notification Status Banner */}
          <div className="p-3.5 rounded-xl border-2 bg-white shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="flex items-start gap-3">
              <div
                className={`w-9 h-9 rounded-lg flex items-center justify-center shrink-0 text-white font-bold ${
                  permissionStatus === 'granted'
                    ? 'bg-emerald-600'
                    : permissionStatus === 'denied'
                    ? 'bg-red-500'
                    : 'bg-amber-500'
                }`}
              >
                <Smartphone className="w-5 h-5" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h4 className="text-xs font-black text-slate-900">
                    Browser Web Push Notifications
                  </h4>
                  <span
                    className={`text-[9px] font-black uppercase px-2 py-0.5 rounded-full border ${
                      permissionStatus === 'granted'
                        ? 'bg-emerald-50 text-emerald-700 border-emerald-300'
                        : permissionStatus === 'denied'
                        ? 'bg-red-50 text-red-700 border-red-300'
                        : 'bg-amber-50 text-amber-700 border-amber-300'
                    }`}
                  >
                    {permissionStatus === 'granted'
                      ? '🟢 Active'
                      : permissionStatus === 'denied'
                      ? '⚠️ Blocked'
                      : '🔔 Action Required'}
                  </span>
                </div>
                <p className="text-[11px] text-slate-500 mt-0.5">
                  {permissionStatus === 'granted'
                    ? 'Receive immediate push banners on desktop/mobile even when the app is minimized or backgrounded.'
                    : permissionStatus === 'denied'
                    ? 'Push permission was denied in your browser settings. To enable, click the lock icon in the URL bar.'
                    : 'Enable browser notifications to hear instant dispatch pings and delivery confirmations.'}
                </p>
              </div>
            </div>

            {permissionStatus !== 'granted' && permissionStatus !== 'denied' && (
              <button
                type="button"
                onClick={handleRequestPush}
                className="px-3.5 py-2 rounded-xl bg-gradient-to-r from-[#042F2E] to-[#059669] hover:from-[#021f1e] hover:to-[#047857] text-[#A7F3D0] hover:text-white text-xs font-black flex items-center justify-center gap-1.5 shadow-sm transition cursor-pointer shrink-0 active:scale-95"
              >
                <Bell className="w-3.5 h-3.5 text-[#10B981]" />
                <span>Enable Push Alerts</span>
              </button>
            )}

            {permissionStatus === 'granted' && (
              <span className="text-[11px] text-emerald-700 font-extrabold bg-emerald-50 border border-emerald-200 px-3 py-1.5 rounded-xl shrink-0 flex items-center gap-1.5">
                <Check className="w-3.5 h-3.5" />
                <span>Web Push Active</span>
              </span>
            )}
          </div>

          {/* 2. Interactive Simulation & Quick Test Panel */}
          <div className="p-3.5 rounded-xl bg-gradient-to-r from-emerald-50/80 via-teal-50/50 to-emerald-50/80 border border-emerald-200/80 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-black text-[#042F2E] flex items-center gap-1.5">
                <Radio className="w-3.5 h-3.5 text-[#059669] animate-pulse" />
                <span>Live Alert Simulation Triggers (Test Audio &amp; Toasts)</span>
              </span>
              {testSentMessage && (
                <span className="text-[10px] font-bold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full animate-in fade-in">
                  ✓ {testSentMessage}
                </span>
              )}
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-1">
              <button
                type="button"
                onClick={() => handleTriggerTest('DISPATCH')}
                className="p-2 rounded-lg bg-white border border-amber-300 hover:bg-amber-50 text-amber-900 text-[11px] font-bold flex flex-col items-center gap-1 text-center transition cursor-pointer shadow-xs active:scale-95"
              >
                <span className="text-base">🔔</span>
                <span>Rider Dispatch</span>
                <span className="text-[9px] text-amber-700/80 font-normal">3-Tone Chime</span>
              </button>

              <button
                type="button"
                onClick={() => handleTriggerTest('PICKUP')}
                className="p-2 rounded-lg bg-white border border-blue-300 hover:bg-blue-50 text-blue-900 text-[11px] font-bold flex flex-col items-center gap-1 text-center transition cursor-pointer shadow-xs active:scale-95"
              >
                <span className="text-base">📦</span>
                <span>Order Picked Up</span>
                <span className="text-[9px] text-blue-700/80 font-normal">Customer Alert</span>
              </button>

              <button
                type="button"
                onClick={() => handleTriggerTest('DELIVERY')}
                className="p-2 rounded-lg bg-white border border-emerald-300 hover:bg-emerald-50 text-emerald-900 text-[11px] font-bold flex flex-col items-center gap-1 text-center transition cursor-pointer shadow-xs active:scale-95"
              >
                <span className="text-base">🎉</span>
                <span>Delivered Proof</span>
                <span className="text-[9px] text-emerald-700/80 font-normal">Celebratory 4-Note</span>
              </button>

              <button
                type="button"
                onClick={() => handleTriggerTest('COD')}
                className="p-2 rounded-lg bg-white border border-slate-300 hover:bg-slate-100 text-slate-900 text-[11px] font-bold flex flex-col items-center gap-1 text-center transition cursor-pointer shadow-xs active:scale-95"
              >
                <span className="text-base">💵</span>
                <span>COD Collected</span>
                <span className="text-[9px] text-slate-600 font-normal">Cash Handover</span>
              </button>
            </div>
          </div>

          {/* 3. Filter Pills & Bulk Actions */}
          <div className="flex flex-wrap items-center justify-between gap-2 pt-1">
            <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar">
              {(
                [
                  ['ALL', 'All Alerts'],
                  ['CUSTOMER', 'Customer'],
                  ['RIDER', 'Rider Partner'],
                  ['ADMIN', 'Admin / Fleet'],
                ] as const
              ).map(([roleKey, label]) => {
                const isActive = selectedRoleFilter === roleKey;
                return (
                  <button
                    key={roleKey}
                    type="button"
                    onClick={() => setSelectedRoleFilter(roleKey)}
                    className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer ${
                      isActive
                        ? 'bg-[#042F2E] text-white shadow-xs'
                        : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-100'
                    }`}
                  >
                    {label}
                  </button>
                );
              })}
            </div>

            <div className="flex items-center gap-2 text-xs">
              {unreadCount > 0 && (
                <button
                  type="button"
                  onClick={() => notificationService.markAllAsRead(selectedRoleFilter)}
                  className="px-2.5 py-1 text-emerald-700 hover:text-emerald-900 font-bold flex items-center gap-1 transition cursor-pointer"
                >
                  <CheckCheck className="w-3.5 h-3.5" />
                  <span>Mark Read</span>
                </button>
              )}
              {notifications.length > 0 && (
                <button
                  type="button"
                  onClick={() => notificationService.clearAll()}
                  className="px-2.5 py-1 text-slate-500 hover:text-red-600 font-bold flex items-center gap-1 transition cursor-pointer"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  <span>Clear</span>
                </button>
              )}
            </div>
          </div>

          {/* 4. Notification Items Feed */}
          <div className="space-y-2.5 max-h-[380px] overflow-y-auto pr-1">
            {filteredNotifications.length === 0 ? (
              <div className="text-center py-10 bg-white rounded-xl border border-dashed border-slate-300 p-6 space-y-2">
                <Bell className="w-8 h-8 text-slate-300 mx-auto" />
                <p className="text-xs font-bold text-slate-600">No alerts in this category</p>
                <p className="text-[11px] text-slate-400">
                  New order status changes, dispatch notices, and OTP verifications will appear here in real-time.
                </p>
              </div>
            ) : (
              filteredNotifications.map((notif) => (
                <div
                  key={notif.id}
                  onClick={() => notificationService.markAsRead(notif.id)}
                  className={`p-3.5 rounded-xl border transition flex items-start justify-between gap-3 cursor-pointer ${
                    notif.read
                      ? 'bg-white border-slate-200 opacity-90'
                      : 'bg-emerald-50/60 border-emerald-300/80 shadow-xs ring-1 ring-emerald-200/50'
                  }`}
                >
                  <div className="flex items-start gap-3 min-w-0">
                    <div
                      className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 ${
                        notif.read ? 'bg-slate-100 text-slate-500' : 'bg-emerald-100 text-emerald-700'
                      }`}
                    >
                      {getIcon(notif.type)}
                    </div>
                    <div className="min-w-0 space-y-0.5">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span
                          className={`text-xs font-black ${
                            notif.read ? 'text-slate-800' : 'text-[#042F2E]'
                          }`}
                        >
                          {notif.title}
                        </span>
                        {notif.orderCode && (
                          <span className="px-1.5 py-0.5 rounded text-[9px] font-mono font-bold bg-slate-100 text-slate-700 border border-slate-200">
                            #{notif.orderCode}
                          </span>
                        )}
                        {!notif.read && (
                          <span className="w-2 h-2 rounded-full bg-emerald-500 shrink-0" />
                        )}
                      </div>
                      <p className="text-xs text-slate-600 leading-snug">
                        {notif.message}
                      </p>
                      <div className="text-[10px] text-slate-400 pt-0.5 flex items-center gap-2">
                        <span>{notif.timeFormatted}</span>
                        <span>•</span>
                        <span className="uppercase font-bold text-[9px] tracking-wider text-slate-500">
                          {notif.role}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Contextual Action Link */}
                  {notif.orderCode && onNavigateToOrder && (
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        onNavigateToOrder(notif.orderCode!);
                        onClose();
                      }}
                      className="px-2.5 py-1 rounded-lg bg-emerald-100 hover:bg-emerald-200 text-[#042F2E] text-[11px] font-bold shrink-0 transition"
                    >
                      Track
                    </button>
                  )}
                </div>
              ))
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="p-3.5 bg-slate-100 border-t border-slate-200 flex items-center justify-between text-xs">
          <div className="flex items-center gap-2 text-slate-500 text-[11px]">
            <span>🔊 Web Audio Synthesized Chimes Active</span>
            <span>•</span>
            <span>📳 Haptic Vibrator Ready</span>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-xl bg-[#042F2E] hover:bg-[#021f1e] text-white text-xs font-bold transition cursor-pointer"
          >
            Done
          </button>
        </div>
      </div>
    </div>
  );
};
