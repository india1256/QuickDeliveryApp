import React, { useState } from 'react';
import {
  Package,
  MapPin,
  Clock,
  Phone,
  MessageSquare,
  ShieldCheck,
  Lock,
  Share2,
  Copy,
  Check,
  X,
  Navigation,
  Star,
  Banknote,
  Bike,
  Truck,
  Zap,
  ArrowRight
} from 'lucide-react';
import { DeliveryMapComponent } from './DeliveryMapComponent';

interface DeliveryOrder {
  id: string;
  orderCode: string;
  customerName: string;
  customerPhone: string;
  receiverName?: string;
  receiverPhone?: string;
  pickupAddress: string;
  dropAddress: string;
  pickupTag?: string;
  dropTag?: string;
  packageCategory?: string;
  weightKg: number;
  distanceKm: number;
  vehicleType: string;
  paymentMethod: string;
  paymentStatus: string;
  status: string;
  totalAmountPaise: number;
  pickupOtp: string;
  deliveryOtp: string;
  assignedRiderId?: string;
  assignedRiderName?: string;
  assignedRiderRating?: string;
  assignedRiderPlate?: string;
  assignedRiderPhone?: string;
  createdAt: string;
  scheduledMinutes?: number;
  estimatedMinutes?: number;
  delayMinutes?: number;
  isDelayed?: boolean;
  statusLabel?: string;
}

interface LiveOrderTrackingModalProps {
  isOpen: boolean;
  onClose: () => void;
  order: DeliveryOrder | null;
  onOpenChat?: (orderId: string) => void;
}

export const LiveOrderTrackingModal: React.FC<LiveOrderTrackingModalProps> = ({
  isOpen,
  onClose,
  order,
  onOpenChat,
}) => {
  const [copiedLink, setCopiedLink] = useState(false);
  const [copiedOtp, setCopiedOtp] = useState<string | null>(null);

  if (!isOpen || !order) return null;

  const isCod =
    order.paymentMethod === 'CASH_ON_DELIVERY' ||
    order.paymentMethod === 'CASH_AT_DROP' ||
    order.paymentMethod === 'CASH_AT_PICKUP';

  const getTimelineStepIndex = (status: string): number => {
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

  const currentStep = getTimelineStepIndex(order.status);
  const totalAmountInr = (order.totalAmountPaise / 100).toFixed(0);

  const TIMELINE_STAGES = [
    { label: 'Order Placed', sub: 'Confirmed & Routed' },
    { label: 'Rider Assigned', sub: order.assignedRiderName || 'Nearby Partner' },
    { label: 'Picked Up', sub: 'Verified via OTP' },
    { label: 'In Transit', sub: 'On the way to drop' },
    { label: 'Delivered', sub: 'Handover complete' },
  ];

  const handleCopyTrackingLink = async () => {
    const url = `${window.location.origin}${window.location.pathname}?app=customer&tracking=${order.orderCode || order.id}`;
    try {
      await navigator.clipboard.writeText(url);
      setCopiedLink(true);
      setTimeout(() => setCopiedLink(false), 2500);
    } catch {}
  };

  const handleCopyOtp = async (otp: string, type: string) => {
    try {
      await navigator.clipboard.writeText(otp);
      setCopiedOtp(type);
      setTimeout(() => setCopiedOtp(null), 2500);
    } catch {}
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm p-3 sm:p-4 flex items-center justify-center animate-in fade-in overflow-y-auto">
      <div className="w-full max-w-3xl rounded-2xl bg-white border-2 border-emerald-500 shadow-2xl overflow-hidden flex flex-col my-auto max-h-[92vh]">
        
        {/* Header */}
        <div className="bg-gradient-to-r from-[#042F2E] via-[#065F46] to-[#059669] text-white px-5 py-4 flex items-center justify-between shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-white/10 border border-emerald-300/40 flex items-center justify-center text-xl shadow-inner">
              <Navigation className="w-6 h-6 text-[#A7F3D0] animate-pulse" />
            </div>
            <div>
              <div className="flex items-center gap-2 flex-wrap">
                <h3 className="font-extrabold text-base sm:text-lg leading-tight">
                  Live Order GPS Tracking
                </h3>
                <span className="text-[10px] font-extrabold uppercase tracking-wider bg-emerald-400/20 text-[#A7F3D0] border border-emerald-300/30 px-2 py-0.5 rounded-full">
                  #{order.orderCode || order.id}
                </span>
                {(order.isDelayed ||
                  order.statusLabel === 'Delayed' ||
                  ((order.estimatedMinutes || 0) - (order.scheduledMinutes || 20) > 10 &&
                    order.status !== 'DELIVERED' &&
                    order.status !== 'CANCELLED')) && (
                  <span className="text-[10px] font-black uppercase tracking-wider bg-rose-500 text-white border border-rose-200 px-2.5 py-0.5 rounded-full animate-pulse">
                    ⚠️ Delayed
                  </span>
                )}
              </div>
              <p className="text-xs text-emerald-100/85 mt-0.5">
                Real-time door-to-door rider telemetry, delivery OTPs &amp; instant chat
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-lg bg-white/10 hover:bg-white/20 text-emerald-100 hover:text-white transition cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="p-4 sm:p-5 overflow-y-auto space-y-4 flex-1 bg-slate-50/70">
          {(order.isDelayed ||
            order.statusLabel === 'Delayed' ||
            ((order.estimatedMinutes || 0) - (order.scheduledMinutes || 20) > 10 &&
              order.status !== 'DELIVERED' &&
              order.status !== 'CANCELLED')) && (
            <div className="p-3 rounded-xl bg-rose-50 border-2 border-rose-300 flex items-center justify-between gap-3 text-rose-950">
              <div className="flex items-center gap-2.5">
                <Clock className="w-5 h-5 text-rose-600 shrink-0" />
                <div>
                  <div className="text-xs font-black uppercase tracking-wider text-rose-900">
                    ⚠️ Status: Delayed (Exceeds Scheduled SLA by &gt;10 mins)
                  </div>
                  <div className="text-xs font-semibold text-rose-800">
                    Scheduled: {order.scheduledMinutes || 20} mins • Estimated: {order.estimatedMinutes || 32} mins (+
                    {Math.max(11, (order.estimatedMinutes || 32) - (order.scheduledMinutes || 20))} mins delay). Automated alert sent to Customer &amp; Rider.
                  </div>
                </div>
              </div>
              <span className="px-2.5 py-1 rounded-full bg-rose-600 text-white text-[10px] font-black uppercase tracking-wider shrink-0">
                Delayed
              </span>
            </div>
          )}
          
          {/* Cash on Delivery (COD) Banner if applicable */}
          {isCod && (
            <div className="p-3 rounded-xl bg-amber-500/15 border-2 border-amber-400/60 flex items-center justify-between gap-3 text-amber-950">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-amber-500 text-white flex items-center justify-center font-bold">
                  <Banknote className="w-5 h-5" />
                </div>
                <div>
                  <div className="text-xs font-black uppercase tracking-wider text-amber-900">
                    💰 Cash on Delivery (COD) Order
                  </div>
                  <div className="text-xs font-extrabold text-slate-800">
                    Total cash payable to rider at doorstep: <span className="text-amber-800 font-black text-sm">₹{totalAmountInr}</span>
                  </div>
                </div>
              </div>
              <span className="px-2.5 py-1 rounded-full bg-amber-100 border border-amber-300 text-amber-900 text-[10px] font-black uppercase tracking-wider">
                {order.paymentStatus === 'CASH_COLLECTED_AT_DOORSTEP' ? '✅ Cash Paid' : '⏳ Pending Handover'}
              </span>
            </div>
          )}

          {/* 5-Stage Animated Tracking Stepper */}
          <div className="p-3.5 rounded-2xl bg-white border border-slate-200 shadow-xs space-y-3">
            <div className="flex items-center justify-between text-xs font-extrabold text-slate-800">
              <span>Delivery Status Timeline</span>
              <span className="text-[#059669] font-black uppercase text-[11px]">
                {order.status.replace(/_/g, ' ')}
              </span>
            </div>

            <div className="grid grid-cols-5 gap-1 text-center relative">
              {TIMELINE_STAGES.map((stg, idx) => {
                const isPassed = idx <= currentStep;
                const isCurrent = idx === currentStep;
                return (
                  <div key={stg.label} className="flex flex-col items-center">
                    <div
                      className={`w-7 h-7 sm:w-8 sm:h-8 rounded-full flex items-center justify-center text-xs font-black transition ${
                        isCurrent
                          ? 'bg-[#059669] text-white ring-4 ring-emerald-200 animate-pulse'
                          : isPassed
                          ? 'bg-[#042F2E] text-[#A7F3D0]'
                          : 'bg-slate-200 text-slate-400'
                      }`}
                    >
                      {isPassed && !isCurrent ? '✓' : idx + 1}
                    </div>
                    <span
                      className={`text-[10px] mt-1 font-bold leading-tight ${
                        isPassed ? 'text-[#042F2E]' : 'text-slate-400'
                      }`}
                    >
                      {stg.label}
                    </span>
                    <span className="text-[8px] text-slate-400 leading-none mt-0.5 hidden sm:block">
                      {stg.sub}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>

          {/* GPS Route Map Component */}
          <DeliveryMapComponent
            orderId={order.id}
            orderCode={order.orderCode}
            status={order.status as any}
            pickupAddress={order.pickupAddress}
            dropAddress={order.dropAddress}
            pickupTag={order.pickupTag || 'Pickup'}
            dropTag={order.dropTag || 'Drop'}
            vehicleType={order.vehicleType as any}
            riderName={order.assignedRiderName || 'Vikram Singh'}
            riderPlate={order.assignedRiderPlate || 'KA-01-EQ-4821'}
            riderPhone={order.assignedRiderPhone || '9811223344'}
            riderRating={order.assignedRiderRating || '4.9★'}
            isRiderView={false}
            compact={false}
          />

          {/* Security OTP Badges (Pickup OTP & Drop OTP) */}
          <div className="grid grid-cols-2 gap-3 text-xs">
            <div className="p-3 rounded-xl bg-amber-500/10 border border-amber-300 flex items-center justify-between">
              <div>
                <div className="text-amber-800 text-[10px] font-bold uppercase tracking-wider">
                  Pickup Handover OTP
                </div>
                <div className="text-xl font-black text-amber-900 tracking-widest mt-0.5">
                  {order.pickupOtp}
                </div>
                <div className="text-[10px] text-slate-500 mt-0.5">Share with Rider at pickup</div>
              </div>
              <button
                type="button"
                onClick={() => handleCopyOtp(order.pickupOtp, 'pickup')}
                className="p-2 rounded-lg bg-white/80 hover:bg-white text-slate-700 shadow-xs cursor-pointer"
                title="Copy Pickup OTP"
              >
                {copiedOtp === 'pickup' ? <Check className="w-4 h-4 text-emerald-600" /> : <Copy className="w-4 h-4 text-slate-500" />}
              </button>
            </div>

            <div className="p-3 rounded-xl bg-emerald-500/10 border border-emerald-300 flex items-center justify-between">
              <div>
                <div className="text-emerald-800 text-[10px] font-bold uppercase tracking-wider">
                  Doorstep Delivery OTP
                </div>
                <div className="text-xl font-black text-emerald-900 tracking-widest mt-0.5">
                  {order.deliveryOtp}
                </div>
                <div className="text-[10px] text-slate-500 mt-0.5">Share at final package handover</div>
              </div>
              <button
                type="button"
                onClick={() => handleCopyOtp(order.deliveryOtp, 'delivery')}
                className="p-2 rounded-lg bg-white/80 hover:bg-white text-slate-700 shadow-xs cursor-pointer"
                title="Copy Delivery OTP"
              >
                {copiedOtp === 'delivery' ? <Check className="w-4 h-4 text-emerald-600" /> : <Copy className="w-4 h-4 text-slate-500" />}
              </button>
            </div>
          </div>

          {/* Assigned Rider Profile & Communication */}
          <div className="p-3.5 rounded-2xl bg-white border border-slate-200 shadow-xs flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="w-11 h-11 rounded-xl bg-[#042F2E] text-[#A7F3D0] border border-emerald-400/50 flex items-center justify-center font-black text-base shadow-xs">
                {order.assignedRiderName ? order.assignedRiderName.slice(0, 2).toUpperCase() : 'VS'}
              </div>
              <div>
                <div className="flex items-center gap-1.5">
                  <span className="text-xs font-extrabold text-slate-900">
                    {order.assignedRiderName || 'Vikram Singh (Delivery Partner)'}
                  </span>
                  <span className="px-1.5 py-0.5 rounded bg-amber-100 text-amber-800 text-[10px] font-extrabold flex items-center gap-0.5">
                    <Star className="w-3 h-3 fill-amber-500 text-amber-500" />
                    {order.assignedRiderRating || '4.9★'}
                  </span>
                </div>
                <div className="text-[11px] text-slate-500 mt-0.5">
                  Vehicle: <strong>{order.assignedRiderPlate || 'KA-01-EQ-4821'}</strong> ({order.vehicleType})
                </div>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <a
                href={`tel:${order.assignedRiderPhone || '9811223344'}`}
                className="px-3 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-extrabold flex items-center gap-1.5 shadow-xs transition"
              >
                <Phone className="w-3.5 h-3.5" />
                <span>Call Rider</span>
              </a>

              {onOpenChat && (
                <button
                  type="button"
                  onClick={() => onOpenChat(order.id)}
                  className="px-3 py-2 rounded-xl bg-[#042F2E] hover:bg-emerald-950 text-[#A7F3D0] text-xs font-extrabold flex items-center gap-1.5 shadow-xs transition cursor-pointer"
                >
                  <MessageSquare className="w-3.5 h-3.5" />
                  <span>Live Chat</span>
                </button>
              )}
            </div>
          </div>

          {/* Route Details */}
          <div className="p-3.5 rounded-2xl bg-white border border-slate-200 shadow-xs space-y-2 text-xs">
            <div className="flex items-start gap-2 text-slate-800">
              <MapPin className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
              <div>
                <strong>Pickup:</strong> {order.pickupAddress}
              </div>
            </div>
            <div className="flex items-start gap-2 text-slate-800">
              <MapPin className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
              <div>
                <strong>Drop:</strong> {order.dropAddress}
              </div>
            </div>
          </div>

        </div>

        {/* Footer */}
        <div className="bg-slate-100 border-t border-slate-200 px-5 py-3 flex items-center justify-between shrink-0">
          <button
            type="button"
            onClick={handleCopyTrackingLink}
            className="text-xs font-extrabold text-[#059669] hover:text-emerald-800 flex items-center gap-1.5 cursor-pointer"
          >
            {copiedLink ? <Check className="w-4 h-4 text-emerald-600" /> : <Share2 className="w-4 h-4" />}
            <span>{copiedLink ? 'Tracking Link Copied!' : 'Share Live Tracking Link'}</span>
          </button>

          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-xl bg-slate-900 hover:bg-black text-white text-xs font-extrabold cursor-pointer"
          >
            Done &amp; Close
          </button>
        </div>

      </div>
    </div>
  );
};
