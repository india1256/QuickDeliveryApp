import React, { useState, useMemo } from 'react';
import {
  Clock,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  Truck,
  Bike,
  Navigation,
  Search,
  Download,
  Phone,
  User,
  ShieldCheck,
  RefreshCw,
  Eye,
  Check,
  X,
  FileText,
  Sparkles,
  ArrowRight,
  MapPin,
  Banknote,
  RotateCcw,
  Layers,
  ChevronDown,
  ChevronUp
} from 'lucide-react';
import { DeliveryMapComponent } from './DeliveryMapComponent';

export interface CustomerOrder {
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
  weightBracket?: string;
  weightKg: number;
  distanceKm: number;
  vehicleType: 'BIKE' | 'EV' | 'MINI_TRUCK';
  paymentMethod: string;
  paymentStatus: string;
  status: string;
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
  pickupPhotoUrl?: string;
  deliveryProofNote?: string;
  deliveryPhotoUrl?: string;
  createdAt: string;
  cancellationReason?: string;
  scheduledMinutes?: number;
  estimatedMinutes?: number;
  delayMinutes?: number;
  isDelayed?: boolean;
  statusLabel?: string;
  delayNotificationSent?: boolean;
}

interface CustomerOrderHistoryViewProps {
  orders: CustomerOrder[];
  onTrackOrder: (order: CustomerOrder) => void;
  onDownloadGstInvoice: (order: CustomerOrder) => void;
  onReOrder?: (order: CustomerOrder) => void;
  onCancelOrder?: (orderId: string, reason: string) => void;
  onJumpToBooking?: () => void;
}

export const CustomerOrderHistoryView: React.FC<CustomerOrderHistoryViewProps> = ({
  orders,
  onTrackOrder,
  onDownloadGstInvoice,
  onReOrder,
  onCancelOrder,
  onJumpToBooking,
}) => {
  const [activeTab, setActiveTab] = useState<'ALL' | 'ACTIVE' | 'DELIVERED' | 'CANCELLED'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedTimelineId, setExpandedTimelineId] = useState<string | null>(null);
  const [cancelModalOrder, setCancelModalOrder] = useState<CustomerOrder | null>(null);
  const [cancelReason, setCancelReason] = useState('Change of plan / Booked by mistake');

  // Metrics
  const metrics = useMemo(() => {
    let activeCount = 0;
    let deliveredCount = 0;
    let cancelledCount = 0;
    let totalSpentPaise = 0;

    orders.forEach((o) => {
      totalSpentPaise += o.totalAmountPaise;
      if (o.status === 'DELIVERED') {
        deliveredCount++;
      } else if (o.status === 'CANCELLED') {
        cancelledCount++;
      } else {
        activeCount++;
      }
    });

    return {
      totalCount: orders.length,
      activeCount,
      deliveredCount,
      cancelledCount,
      totalSpentInr: Math.round(totalSpentPaise / 100),
    };
  }, [orders]);

  // Filtered orders
  const filteredOrders = useMemo(() => {
    return orders.filter((o) => {
      // Tab filter
      if (activeTab === 'ACTIVE') {
        if (o.status === 'DELIVERED' || o.status === 'CANCELLED') return false;
      } else if (activeTab === 'DELIVERED') {
        if (o.status !== 'DELIVERED') return false;
      } else if (activeTab === 'CANCELLED') {
        if (o.status !== 'CANCELLED') return false;
      }

      // Search filter
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase().trim();
        const matches =
          o.orderCode.toLowerCase().includes(q) ||
          o.pickupAddress.toLowerCase().includes(q) ||
          o.dropAddress.toLowerCase().includes(q) ||
          (o.packageCategory && o.packageCategory.toLowerCase().includes(q)) ||
          (o.receiverName && o.receiverName.toLowerCase().includes(q));
        if (!matches) return false;
      }

      return true;
    });
  }, [orders, activeTab, searchQuery]);

  const getStatusBadge = (status: string, order?: CustomerOrder) => {
    const isTerminal = status === 'DELIVERED' || status === 'CANCELLED';
    const schedMins = order?.scheduledMinutes || 20;
    const estMins = order?.estimatedMinutes || schedMins;
    const isDelayedOrder =
      !isTerminal &&
      (Boolean(order?.isDelayed) ||
        order?.statusLabel === 'Delayed' ||
        estMins - schedMins > 10);

    if (isDelayedOrder) {
      return {
        label: 'Delayed',
        badgeClass: 'bg-rose-100 text-rose-900 border-rose-300',
        dotClass: 'bg-rose-600 animate-ping',
      };
    }

    switch (status) {
      case 'PENDING_ASSIGNMENT':
        return {
          label: 'Searching Fleet Rider',
          badgeClass: 'bg-amber-100 text-amber-900 border-amber-300',
          dotClass: 'bg-amber-500 animate-pulse',
        };
      case 'ASSIGNED':
        return {
          label: 'Rider Assigned',
          badgeClass: 'bg-blue-100 text-blue-900 border-blue-300',
          dotClass: 'bg-blue-500',
        };
      case 'ARRIVED_AT_PICKUP':
        return {
          label: 'Rider Arrived at Pickup',
          badgeClass: 'bg-indigo-100 text-indigo-900 border-indigo-300',
          dotClass: 'bg-indigo-500',
        };
      case 'PICKED_UP':
      case 'IN_TRANSIT':
        return {
          label: 'Out for Delivery (In Transit)',
          badgeClass: 'bg-purple-100 text-purple-900 border-purple-300',
          dotClass: 'bg-purple-500 animate-ping',
        };
      case 'DELIVERED':
        return {
          label: 'Delivered Successfully',
          badgeClass: 'bg-emerald-100 text-emerald-900 border-emerald-300',
          dotClass: 'bg-emerald-600',
        };
      case 'CANCELLED':
        return {
          label: 'Cancelled',
          badgeClass: 'bg-red-100 text-red-900 border-red-300',
          dotClass: 'bg-red-600',
        };
      default:
        return {
          label: status.replace(/_/g, ' '),
          badgeClass: 'bg-slate-100 text-slate-800 border-slate-300',
          dotClass: 'bg-slate-400',
        };
    }
  };

  const handleConfirmCancel = () => {
    if (!cancelModalOrder || !onCancelOrder) return;
    onCancelOrder(cancelModalOrder.id, cancelReason);
    setCancelModalOrder(null);
  };

  return (
    <div className="space-y-4">
      {/* 1. CUSTOMER ORDER METRICS SUMMARY */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
        <button
          type="button"
          onClick={() => setActiveTab('ALL')}
          className={`p-3 rounded-2xl border text-left transition cursor-pointer ${
            activeTab === 'ALL'
              ? 'bg-[#042F2E] text-white border-[#059669] shadow-sm'
              : 'bg-white text-slate-800 border-slate-200 hover:border-slate-300'
          }`}
        >
          <div className="text-[10px] font-bold uppercase tracking-wider opacity-75">All Orders</div>
          <div className="text-xl font-extrabold mt-0.5">{metrics.totalCount}</div>
          <div className="text-[11px] opacity-90 mt-0.5">₹{metrics.totalSpentInr} Total Spent</div>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('ACTIVE')}
          className={`p-3 rounded-2xl border text-left transition cursor-pointer ${
            activeTab === 'ACTIVE'
              ? 'bg-purple-600 text-white border-purple-700 shadow-sm'
              : 'bg-purple-50/70 text-purple-900 border-purple-200 hover:border-purple-300'
          }`}
        >
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold uppercase tracking-wider">Active Deliveries</span>
            {metrics.activeCount > 0 && <span className="w-2 h-2 rounded-full bg-purple-500 animate-ping" />}
          </div>
          <div className="text-xl font-extrabold mt-0.5">{metrics.activeCount}</div>
          <div className="text-[11px] mt-0.5 font-medium">Live GPS Tracking</div>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('DELIVERED')}
          className={`p-3 rounded-2xl border text-left transition cursor-pointer ${
            activeTab === 'DELIVERED'
              ? 'bg-emerald-600 text-white border-emerald-700 shadow-sm'
              : 'bg-emerald-50/70 text-emerald-900 border-emerald-200 hover:border-emerald-300'
          }`}
        >
          <div className="text-[10px] font-bold uppercase tracking-wider">Completed</div>
          <div className="text-xl font-extrabold mt-0.5">{metrics.deliveredCount}</div>
          <div className="text-[11px] mt-0.5 font-medium">Delivered Safely</div>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('CANCELLED')}
          className={`p-3 rounded-2xl border text-left transition cursor-pointer ${
            activeTab === 'CANCELLED'
              ? 'bg-red-600 text-white border-red-700 shadow-sm'
              : 'bg-red-50/70 text-red-900 border-red-200 hover:border-red-300'
          }`}
        >
          <div className="text-[10px] font-bold uppercase tracking-wider">Cancelled</div>
          <div className="text-xl font-extrabold mt-0.5">{metrics.cancelledCount}</div>
          <div className="text-[11px] mt-0.5 font-medium">Refund / Voided</div>
        </button>
      </div>

      {/* 2. SEARCH & FILTER HEADER */}
      <div className="bg-white p-3 rounded-2xl border border-slate-200 shadow-xs space-y-3">
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-2.5">
          {/* Tabs */}
          <div className="flex flex-wrap items-center gap-1.5">
            <button
              type="button"
              onClick={() => setActiveTab('ALL')}
              className={`px-3 py-1.5 rounded-xl text-xs font-extrabold transition cursor-pointer ${
                activeTab === 'ALL'
                  ? 'bg-[#042F2E] text-white shadow-xs'
                  : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
              }`}
            >
              All ({orders.length})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('ACTIVE')}
              className={`px-3 py-1.5 rounded-xl text-xs font-extrabold transition cursor-pointer ${
                activeTab === 'ACTIVE'
                  ? 'bg-purple-600 text-white shadow-xs'
                  : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
              }`}
            >
              Active ({metrics.activeCount})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('DELIVERED')}
              className={`px-3 py-1.5 rounded-xl text-xs font-extrabold transition cursor-pointer ${
                activeTab === 'DELIVERED'
                  ? 'bg-[#059669] text-white shadow-xs'
                  : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
              }`}
            >
              Delivered ({metrics.deliveredCount})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('CANCELLED')}
              className={`px-3 py-1.5 rounded-xl text-xs font-extrabold transition cursor-pointer ${
                activeTab === 'CANCELLED'
                  ? 'bg-red-600 text-white shadow-xs'
                  : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
              }`}
            >
              Cancelled ({metrics.cancelledCount})
            </button>
          </div>

          {onJumpToBooking && (
            <button
              type="button"
              onClick={onJumpToBooking}
              className="px-3.5 py-1.5 rounded-xl bg-[#059669] hover:bg-emerald-600 text-white font-extrabold text-xs transition flex items-center justify-center gap-1.5 cursor-pointer shadow-xs"
            >
              <Sparkles className="w-3.5 h-3.5" />
              <span>+ Book New Parcel</span>
            </button>
          )}
        </div>

        {/* Search */}
        <div className="relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search past orders by #QD code, destination address, item type..."
            className="w-full pl-9 pr-3 py-2 text-xs rounded-xl bg-slate-50 border border-slate-200 focus:outline-none focus:border-[#059669]"
          />
        </div>
      </div>

      {/* 3. ORDER HISTORY LIST */}
      <div className="space-y-3">
        {filteredOrders.length === 0 ? (
          <div className="p-10 text-center bg-white rounded-2xl border border-slate-200 space-y-3">
            <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center mx-auto text-slate-400">
              <Layers className="w-6 h-6" />
            </div>
            <div>
              <h4 className="text-sm font-extrabold text-slate-800">No Orders in this Section</h4>
              <p className="text-xs text-slate-500 max-w-sm mx-auto mt-1">
                You haven&apos;t placed any orders matching this filter yet. Ready to send a parcel?
              </p>
            </div>
            {onJumpToBooking && (
              <button
                type="button"
                onClick={onJumpToBooking}
                className="px-4 py-2 rounded-xl bg-[#059669] text-white text-xs font-bold hover:bg-emerald-600 transition"
              >
                Send a Parcel Now
              </button>
            )}
          </div>
        ) : (
          filteredOrders.map((order) => {
            const badge = getStatusBadge(order.status, order);
            const totalInr = Math.round(order.totalAmountPaise / 100);
            const isDelivered = order.status === 'DELIVERED';
            const isCancelled = order.status === 'CANCELLED';
            const isActive = !isDelivered && !isCancelled;
            const isTimelineOpen = expandedTimelineId === order.id;
            const canCancel = order.status === 'PENDING_ASSIGNMENT' || order.status === 'ASSIGNED';

            return (
              <div
                key={order.id}
                className="p-4 rounded-2xl bg-white border border-slate-200 shadow-xs hover:border-emerald-300 transition space-y-3"
              >
                {/* Header: Code, Badge, Timestamp */}
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="px-2.5 py-1 rounded-md bg-[#042F2E] text-white font-extrabold text-xs">
                      #{order.orderCode}
                    </span>
                    <div
                      className={`px-2.5 py-0.5 rounded-full text-[11px] font-extrabold border flex items-center gap-1.5 ${badge.badgeClass}`}
                    >
                      <span className={`w-2 h-2 rounded-full ${badge.dotClass}`} />
                      <span>{badge.label}</span>
                    </div>
                    {isActive && order.scheduledMinutes && (
                      <span
                        className={`text-[10px] font-extrabold px-2 py-0.5 rounded-full border ${
                          badge.label === 'Delayed'
                            ? 'bg-rose-50 text-rose-800 border-rose-300'
                            : 'bg-emerald-50 text-emerald-800 border-emerald-200'
                        }`}
                      >
                        Sched: {order.scheduledMinutes}m • Est: {order.estimatedMinutes || order.scheduledMinutes}m
                        {badge.label === 'Delayed'
                          ? ` (+${Math.max(0, (order.estimatedMinutes || 0) - order.scheduledMinutes)}m Delay)`
                          : ''}
                      </span>
                    )}
                    <span className="text-[11px] text-slate-500">{order.createdAt}</span>
                  </div>

                  <div className="text-right">
                    <div className="text-sm font-extrabold text-slate-900">₹{totalInr}</div>
                    <div className="text-[10px] text-slate-500">
                      {order.paymentMethod.replace(/_/g, ' ')}
                    </div>
                  </div>
                </div>

                {/* Route Information */}
                <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-2 text-xs">
                  <div className="flex items-start gap-2">
                    <MapPin className="w-3.5 h-3.5 text-emerald-600 shrink-0 mt-0.5" />
                    <div>
                      <span className="font-extrabold text-slate-800">
                        Pickup ({order.pickupTag || 'Location'}):{' '}
                      </span>
                      <span className="text-slate-600">{order.pickupAddress}</span>
                    </div>
                  </div>

                  <div className="flex items-start gap-2">
                    <Navigation className="w-3.5 h-3.5 text-orange-600 shrink-0 mt-0.5" />
                    <div>
                      <span className="font-extrabold text-slate-800">
                        Drop ({order.dropTag || 'Destination'}):{' '}
                      </span>
                      <span className="text-slate-600">{order.dropAddress}</span>
                      {order.receiverName && (
                        <span className="text-[11px] text-slate-500 block mt-0.5">
                          Recipient: {order.receiverName} (+91 {order.receiverPhone || order.customerPhone})
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                {/* OTPs and Assigned Rider (When Active) */}
                <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="px-2 py-0.5 rounded-md bg-slate-100 font-bold text-slate-700">
                      {order.packageCategory || 'Parcel'} • {order.weightKg} kg • {order.vehicleType}
                    </span>

                    {/* Active OTP Badges */}
                    {isActive && (
                      <div className="flex items-center gap-1.5">
                        <span className="px-2 py-0.5 rounded-md bg-emerald-50 text-emerald-800 border border-emerald-200 font-bold text-[11px]">
                          Pickup OTP: <strong className="font-mono">{order.pickupOtp}</strong>
                        </span>
                        <span className="px-2 py-0.5 rounded-md bg-amber-50 text-amber-800 border border-amber-200 font-bold text-[11px]">
                          Delivery OTP: <strong className="font-mono">{order.deliveryOtp}</strong>
                        </span>
                      </div>
                    )}

                    {order.assignedRiderName && (
                      <span className="text-slate-600 font-medium flex items-center gap-1">
                        <User className="w-3 h-3 text-slate-400" />
                        <span>Rider: <strong>{order.assignedRiderName}</strong></span>
                        {order.assignedRiderPlate && (
                          <span className="text-[10px] text-slate-500">({order.assignedRiderPlate})</span>
                        )}
                      </span>
                    )}
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-2 flex-wrap">
                    {/* Live GPS Track */}
                    <button
                      type="button"
                      onClick={() => onTrackOrder(order)}
                      className="px-2.5 py-1.5 rounded-xl bg-gradient-to-r from-[#042F2E] to-[#059669] hover:from-[#021f1e] hover:to-[#047857] text-[#A7F3D0] hover:text-white text-xs font-extrabold flex items-center gap-1.5 transition cursor-pointer shadow-xs"
                    >
                      <Navigation className="w-3.5 h-3.5 text-[#10B981]" />
                      <span>Live Track</span>
                    </button>

                    {/* Timeline Toggle */}
                    <button
                      type="button"
                      onClick={() => setExpandedTimelineId(isTimelineOpen ? null : order.id)}
                      className="px-2.5 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold transition flex items-center gap-1 cursor-pointer"
                    >
                      <Clock className="w-3.5 h-3.5" />
                      <span>{isTimelineOpen ? 'Hide Steps' : 'Timeline'}</span>
                      {isTimelineOpen ? <ChevronUp className="w-3 h-3" /> : <ChevronDown className="w-3 h-3" />}
                    </button>

                    {/* Book Again (Re-Order) */}
                    {onReOrder && (
                      <button
                        type="button"
                        onClick={() => onReOrder(order)}
                        className="px-2.5 py-1.5 rounded-xl bg-emerald-50 hover:bg-emerald-100 text-[#065F46] border border-emerald-200 text-xs font-bold transition flex items-center gap-1 cursor-pointer"
                      >
                        <RotateCcw className="w-3 h-3 text-[#059669]" />
                        <span>Book Again</span>
                      </button>
                    )}

                    {/* Download GST Invoice */}
                    <button
                      type="button"
                      onClick={() => onDownloadGstInvoice(order)}
                      title="Download GST Tax Invoice"
                      className="p-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 transition cursor-pointer"
                    >
                      <Download className="w-3.5 h-3.5" />
                    </button>

                    {/* Cancel button if pending */}
                    {canCancel && onCancelOrder && (
                      <button
                        type="button"
                        onClick={() => setCancelModalOrder(order)}
                        className="px-2.5 py-1.5 rounded-xl bg-red-50 hover:bg-red-100 text-red-700 border border-red-200 text-xs font-bold transition cursor-pointer"
                      >
                        Cancel
                      </button>
                    )}
                  </div>
                </div>

                {/* Delivery Proof Photo if Available */}
                {order.deliveryPhotoUrl && (
                  <div className="p-3 rounded-xl bg-emerald-50/70 border border-emerald-200 flex items-center gap-3">
                    <img
                      src={order.deliveryPhotoUrl}
                      alt="Delivery Handover Proof"
                      className="w-14 h-14 rounded-lg object-cover border border-emerald-300 shrink-0"
                    />
                    <div className="text-xs">
                      <div className="font-extrabold text-emerald-950 flex items-center gap-1">
                        <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                        <span>Verified Delivery Handover Proof</span>
                      </div>
                      <div className="text-[11px] text-emerald-800 font-medium mt-0.5">
                        {order.deliveryProofNote || 'Handover photo confirmed by rider partner.'}
                      </div>
                    </div>
                  </div>
                )}

                {/* Expanded Step-by-Step Timeline */}
                {isTimelineOpen && (
                  <div className="pt-3 border-t border-slate-200 space-y-2">
                    <div className="text-[11px] font-extrabold uppercase tracking-wider text-slate-700">
                      Delivery Journey Milestones
                    </div>
                    <div className="grid grid-cols-1 sm:grid-cols-4 gap-2 text-xs">
                      {/* Step 1: Placed */}
                      <div className="p-2.5 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
                        <div className="flex items-center gap-1.5 text-emerald-700 font-bold">
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          <span>1. Placed</span>
                        </div>
                        <div className="text-[11px] text-slate-500">Order logged at {order.createdAt}</div>
                      </div>

                      {/* Step 2: Assigned */}
                      <div className={`p-2.5 rounded-xl border space-y-1 ${
                        order.status !== 'PENDING_ASSIGNMENT'
                          ? 'bg-slate-50 border-slate-200'
                          : 'bg-amber-50/70 border-amber-200'
                      }`}>
                        <div className="flex items-center gap-1.5 font-bold">
                          {order.status !== 'PENDING_ASSIGNMENT' ? (
                            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-700" />
                          ) : (
                            <Clock className="w-3.5 h-3.5 text-amber-600 animate-spin" />
                          )}
                          <span className={order.status !== 'PENDING_ASSIGNMENT' ? 'text-emerald-700' : 'text-amber-800'}>
                            2. Rider Assigned
                          </span>
                        </div>
                        <div className="text-[11px] text-slate-500">
                          {order.assignedRiderName || 'Finding nearby partner'}
                        </div>
                      </div>

                      {/* Step 3: Picked Up */}
                      <div className={`p-2.5 rounded-xl border space-y-1 ${
                        ['PICKED_UP', 'IN_TRANSIT', 'DELIVERED'].includes(order.status)
                          ? 'bg-slate-50 border-slate-200'
                          : 'bg-slate-50/50 border-slate-200 opacity-60'
                      }`}>
                        <div className="flex items-center gap-1.5 font-bold">
                          {['PICKED_UP', 'IN_TRANSIT', 'DELIVERED'].includes(order.status) ? (
                            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-700" />
                          ) : (
                            <Clock className="w-3.5 h-3.5 text-slate-400" />
                          )}
                          <span className={['PICKED_UP', 'IN_TRANSIT', 'DELIVERED'].includes(order.status) ? 'text-emerald-700' : 'text-slate-600'}>
                            3. In Transit
                          </span>
                        </div>
                        <div className="text-[11px] text-slate-500">
                          {order.pickupPhotoCaptured ? 'Pickup photo taken' : 'Handover verified'}
                        </div>
                      </div>

                      {/* Step 4: Delivered */}
                      <div className={`p-2.5 rounded-xl border space-y-1 ${
                        isDelivered
                          ? 'bg-emerald-50 border-emerald-200'
                          : isCancelled
                          ? 'bg-red-50 border-red-200'
                          : 'bg-slate-50/50 border-slate-200 opacity-60'
                      }`}>
                        <div className="flex items-center gap-1.5 font-bold">
                          {isDelivered ? (
                            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-700" />
                          ) : isCancelled ? (
                            <XCircle className="w-3.5 h-3.5 text-red-600" />
                          ) : (
                            <Clock className="w-3.5 h-3.5 text-slate-400" />
                          )}
                          <span className={isDelivered ? 'text-emerald-800' : isCancelled ? 'text-red-700' : 'text-slate-600'}>
                            {isCancelled ? '4. Cancelled' : '4. Delivered'}
                          </span>
                        </div>
                        <div className="text-[11px] text-slate-500">
                          {isCancelled
                            ? (order.cancellationReason || 'Voided')
                            : isDelivered
                            ? (order.deliveryProofNote || 'Delivery OTP confirmed')
                            : 'Pending drop'}
                        </div>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>

      {/* 4. CANCEL ORDER CONFIRMATION MODAL */}
      {cancelModalOrder && (
        <div className="fixed inset-0 z-50 bg-slate-950/70 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="w-full max-w-md bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="bg-red-600 px-5 py-4 text-white flex items-center justify-between">
              <div>
                <h3 className="font-extrabold text-base flex items-center gap-2">
                  <AlertTriangle className="w-4 h-4 text-white" />
                  <span>Cancel Order #{cancelModalOrder.orderCode}</span>
                </h3>
                <p className="text-xs text-red-100 mt-0.5">
                  Are you sure you want to cancel this delivery request?
                </p>
              </div>
              <button
                type="button"
                onClick={() => setCancelModalOrder(null)}
                className="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-white"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="p-5 space-y-4">
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 text-xs space-y-1">
                <div className="font-bold text-slate-800">
                  {cancelModalOrder.pickupAddress} → {cancelModalOrder.dropAddress}
                </div>
                <div className="text-slate-500">
                  Total Fare: ₹{Math.round(cancelModalOrder.totalAmountPaise / 100)} ({cancelModalOrder.paymentMethod.replace(/_/g, ' ')})
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-extrabold text-slate-800">
                  Reason for Cancellation:
                </label>
                <select
                  value={cancelReason}
                  onChange={(e) => setCancelReason(e.target.value)}
                  className="w-full p-2.5 text-xs rounded-xl bg-slate-50 border border-slate-200 focus:outline-none focus:border-red-500"
                >
                  <option value="Change of plan / Booked by mistake">Change of plan / Booked by mistake</option>
                  <option value="Recipient not available at address">Recipient not available at address</option>
                  <option value="Need to change pickup or drop location">Need to change pickup or drop location</option>
                  <option value="Rider taking longer than expected">Rider taking longer than expected</option>
                  <option value="Other reason">Other reason</option>
                </select>
              </div>

              <div className="text-[11px] text-slate-500 bg-amber-50 p-2.5 rounded-xl border border-amber-200">
                Notice: If prepaid via UPI/Card, any platform refund will be instantly credited to your Quick Wallet.
              </div>

              <div className="flex items-center justify-end gap-2.5 pt-2">
                <button
                  type="button"
                  onClick={() => setCancelModalOrder(null)}
                  className="px-4 py-2 rounded-xl border border-slate-300 text-slate-700 text-xs font-bold hover:bg-slate-50 transition"
                >
                  Keep Order
                </button>
                <button
                  type="button"
                  onClick={handleConfirmCancel}
                  className="px-5 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-xs font-extrabold shadow-sm transition"
                >
                  Confirm Cancellation
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
