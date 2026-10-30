import React, { useEffect, useState } from 'react';
import {
  Bell,
  X,
  Navigation,
  CheckCircle2,
  Package,
  Bike,
  Coins,
  AlertTriangle,
  ArrowRight,
  Volume2
} from 'lucide-react';
import {
  notificationService,
  type ToastItem,
  type NotificationType
} from '../services/notificationService';

interface NotificationToastContainerProps {
  onActionClick?: (toast: ToastItem) => void;
}

export const NotificationToastContainer: React.FC<NotificationToastContainerProps> = ({
  onActionClick,
}) => {
  const [currentToast, setCurrentToast] = useState<ToastItem | null>(null);

  useEffect(() => {
    const unsub = notificationService.subscribeToasts((toast) => {
      setCurrentToast(toast);
    });
    return () => unsub();
  }, []);

  if (!currentToast) return null;

  const getIcon = (type: NotificationType) => {
    switch (type) {
      case 'ORDER_PLACED':
        return <Package className="w-5 h-5 text-emerald-400" />;
      case 'DISPATCH_NEW':
        return <Bell className="w-5 h-5 text-amber-400 animate-bounce" />;
      case 'ORDER_ASSIGNED':
        return <Bike className="w-5 h-5 text-emerald-400" />;
      case 'RIDER_ARRIVED':
        return <Navigation className="w-5 h-5 text-teal-400 animate-pulse" />;
      case 'ORDER_PICKED_UP':
        return <Package className="w-5 h-5 text-blue-400" />;
      case 'ORDER_DELIVERED':
        return <CheckCircle2 className="w-5 h-5 text-emerald-300" />;
      case 'COD_COLLECTED':
        return <Coins className="w-5 h-5 text-amber-300" />;
      case 'SYSTEM_ALERT':
      default:
        return <Bell className="w-5 h-5 text-emerald-400" />;
    }
  };

  const getBorderColor = (type: NotificationType) => {
    switch (type) {
      case 'DISPATCH_NEW':
        return 'border-amber-400 shadow-amber-500/20';
      case 'ORDER_DELIVERED':
        return 'border-emerald-400 shadow-emerald-500/20';
      case 'ORDER_PICKED_UP':
        return 'border-blue-400 shadow-blue-500/20';
      default:
        return 'border-emerald-500/60 shadow-emerald-950/30';
    }
  };

  return (
    <div className="fixed top-4 left-1/2 -translate-x-1/2 sm:translate-x-0 sm:left-auto sm:right-5 z-[9999] max-w-[94vw] sm:max-w-md w-full pointer-events-none animate-in fade-in slide-in-from-top-4 duration-300">
      <div
        className={`pointer-events-auto bg-[#042F2E]/95 backdrop-blur-md text-white rounded-2xl border-2 ${getBorderColor(
          currentToast.type
        )} shadow-2xl p-4 overflow-hidden relative flex flex-col gap-2.5 transition-all`}
      >
        {/* Subtle background glow */}
        <div className="absolute -right-8 -top-8 w-24 h-24 bg-emerald-500/10 rounded-full blur-xl pointer-events-none" />

        <div className="flex items-start justify-between gap-3 relative z-10">
          <div className="flex items-start gap-3 min-w-0">
            <div className="w-10 h-10 rounded-xl bg-white/10 border border-white/15 flex items-center justify-center shrink-0 shadow-inner">
              {getIcon(currentToast.type)}
            </div>
            <div className="min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="font-extrabold text-sm text-white leading-snug">
                  {currentToast.title}
                </span>
                {currentToast.orderCode && (
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-mono font-bold bg-emerald-500/20 text-[#A7F3D0] border border-emerald-400/30">
                    #{currentToast.orderCode}
                  </span>
                )}
                <span className="text-[10px] font-bold text-emerald-200/60">
                  • {currentToast.timeFormatted}
                </span>
              </div>
              <p className="text-xs text-emerald-100/90 mt-1 leading-relaxed">
                {currentToast.message}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={() => notificationService.dismissToast()}
            className="p-1 rounded-lg bg-white/10 hover:bg-white/20 text-emerald-200 hover:text-white transition cursor-pointer shrink-0"
            title="Dismiss notification"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Action button if applicable */}
        <div className="flex items-center justify-between gap-2 pt-2 border-t border-emerald-800/60 relative z-10 text-xs">
          <div className="flex items-center gap-1.5 text-[11px] text-[#A7F3D0] font-bold">
            <Volume2 className="w-3.5 h-3.5 text-emerald-400" />
            <span>Audio &amp; Push Alert Sent</span>
          </div>

          {onActionClick && (
            <button
              type="button"
              onClick={() => {
                onActionClick(currentToast);
                notificationService.dismissToast();
              }}
              className="px-3 py-1 rounded-lg bg-[#059669] hover:bg-emerald-600 text-white font-extrabold flex items-center gap-1 transition shadow-xs cursor-pointer active:scale-95"
            >
              <span>{currentToast.actionLabel || 'View Details'}</span>
              <ArrowRight className="w-3.5 h-3.5 text-[#A7F3D0]" />
            </button>
          )}
        </div>

        {/* Animated Progress Bar */}
        <div className="absolute bottom-0 left-0 right-0 h-1 bg-white/10 overflow-hidden">
          <div className="h-full bg-gradient-to-r from-emerald-400 to-[#A7F3D0] animate-[shrink_6.5s_linear_forwards]" />
        </div>
      </div>
    </div>
  );
};
