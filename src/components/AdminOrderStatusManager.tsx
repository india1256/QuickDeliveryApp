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
  Filter,
  Download,
  Phone,
  User,
  ShieldCheck,
  RefreshCw,
  Eye,
  ChevronDown,
  Check,
  X,
  FileText,
  Sparkles,
  ArrowRight,
  MapPin,
  Banknote,
  Send,
  Layers,
  SlidersHorizontal
} from 'lucide-react';
import { DeliveryMapComponent } from './DeliveryMapComponent';

export type OrderStatusKey =
  | 'PENDING_ASSIGNMENT'
  | 'ASSIGNED'
  | 'ARRIVED_AT_PICKUP'
  | 'PICKED_UP'
  | 'IN_TRANSIT'
  | 'DELIVERED'
  | 'CANCELLED';

export interface ManagedOrder {
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
  status: OrderStatusKey;
  totalAmountPaise: number;
  companyCommissionPaise: number;
  riderEarningPaise: number;
  pickupOtp: string;
  deliveryOtp: string;
  assignedRiderId?: string;
  assignedRiderName?: string;
  assignedRiderRating?: string;
  assignedRiderPlate?: string;
  pickupPhotoCaptured?: boolean;
  deliveryProofNote?: string;
  createdAt: string;
  cancellationReason?: string;
  scheduledMinutes?: number;
  estimatedMinutes?: number;
  delayMinutes?: number;
  isDelayed?: boolean;
  statusLabel?: string;
  delayNotificationSent?: boolean;
  statusHistory?: Array<{
    status: OrderStatusKey;
    timestamp: string;
    updatedBy: string;
    note?: string;
  }>;
}

interface AdminOrderStatusManagerProps {
  orders: ManagedOrder[];
  onUpdateOrderStatus: (
    orderId: string,
    newStatus: OrderStatusKey,
    details?: {
      assignedRiderName?: string;
      assignedRiderId?: string;
      note?: string;
      cancellationReason?: string;
    }
  ) => void;
  onUpdateOrderEta?: (orderId: string, newEstimatedMinutes: number) => void;
  onBulkUpdateStatus?: (orderIds: string[], newStatus: OrderStatusKey) => void;
  onTrackOrder: (order: ManagedOrder) => void;
  onDownloadGstInvoice: (order: ManagedOrder) => void;
  onSpawnOrder?: () => void;
}

// Available Riders for quick assignment
const AVAILABLE_RIDERS = [
  { id: 'RIDER-101', name: 'Vikram Singh', phone: '9845012345', plate: 'KA-01-EQ-4821', rating: '4.9★', vehicle: 'EV' },
  { id: 'RIDER-102', name: 'Suresh Kumar', phone: '9845098765', plate: 'KA-03-MR-1904', rating: '4.8★', vehicle: 'BIKE' },
  { id: 'RIDER-103', name: 'Rajesh Nair', phone: '9822001122', plate: 'KA-05-TR-8812', rating: '4.95★', vehicle: 'MINI_TRUCK' },
];

export const AdminOrderStatusManager: React.FC<AdminOrderStatusManagerProps> = ({
  orders,
  onUpdateOrderStatus,
  onUpdateOrderEta,
  onBulkUpdateStatus,
  onTrackOrder,
  onDownloadGstInvoice,
  onSpawnOrder,
}) => {
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'PENDING' | 'ACCEPTED' | 'OUT_FOR_DELIVERY' | 'DELIVERED' | 'CANCELLED'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState<'NEWEST' | 'FARE_HIGH' | 'DISTANCE'>('NEWEST');
  const [paymentFilter, setPaymentFilter] = useState<'ALL' | 'COD' | 'PREPAID'>('ALL');
  const [selectedOrderIds, setSelectedOrderIds] = useState<string[]>([]);
  const [expandedMapOrderId, setExpandedMapOrderId] = useState<string | null>(null);
  const [activeDetailsModalOrder, setActiveDetailsModalOrder] = useState<ManagedOrder | null>(null);

  // Status transition dialog state
  const [statusChangeDialog, setStatusChangeDialog] = useState<{
    order: ManagedOrder;
    targetStatus: OrderStatusKey;
    selectedRider: string;
    note: string;
    cancellationReason: string;
  } | null>(null);

  // Grouping helper
  const normalizeStatus = (status: OrderStatusKey): 'PENDING' | 'ACCEPTED' | 'OUT_FOR_DELIVERY' | 'DELIVERED' | 'CANCELLED' => {
    if (status === 'PENDING_ASSIGNMENT') return 'PENDING';
    if (status === 'ASSIGNED' || status === 'ARRIVED_AT_PICKUP') return 'ACCEPTED';
    if (status === 'PICKED_UP' || status === 'IN_TRANSIT') return 'OUT_FOR_DELIVERY';
    if (status === 'DELIVERED') return 'DELIVERED';
    return 'CANCELLED';
  };

  // Metrics calculation
  const metrics = useMemo(() => {
    let pendingCount = 0;
    let pendingPaise = 0;
    let acceptedCount = 0;
    let acceptedPaise = 0;
    let outCount = 0;
    let outPaise = 0;
    let deliveredCount = 0;
    let deliveredPaise = 0;
    let cancelledCount = 0;
    let cancelledPaise = 0;

    orders.forEach((o) => {
      const g = normalizeStatus(o.status);
      if (g === 'PENDING') {
        pendingCount++;
        pendingPaise += o.totalAmountPaise;
      } else if (g === 'ACCEPTED') {
        acceptedCount++;
        acceptedPaise += o.totalAmountPaise;
      } else if (g === 'OUT_FOR_DELIVERY') {
        outCount++;
        outPaise += o.totalAmountPaise;
      } else if (g === 'DELIVERED') {
        deliveredCount++;
        deliveredPaise += o.totalAmountPaise;
      } else if (g === 'CANCELLED') {
        cancelledCount++;
        cancelledPaise += o.totalAmountPaise;
      }
    });

    const totalPaise = orders.reduce((sum, o) => sum + o.totalAmountPaise, 0);

    return {
      totalCount: orders.length,
      totalInr: Math.round(totalPaise / 100),
      pendingCount,
      pendingInr: Math.round(pendingPaise / 100),
      acceptedCount,
      acceptedInr: Math.round(acceptedPaise / 100),
      outCount,
      outInr: Math.round(outPaise / 100),
      deliveredCount,
      deliveredInr: Math.round(deliveredPaise / 100),
      cancelledCount,
      cancelledInr: Math.round(cancelledPaise / 100),
    };
  }, [orders]);

  // Filtering
  const filteredOrders = useMemo(() => {
    return orders
      .filter((o) => {
        // Status filter
        if (statusFilter !== 'ALL') {
          const g = normalizeStatus(o.status);
          if (g !== statusFilter) return false;
        }

        // Payment filter
        if (paymentFilter === 'COD') {
          if (!o.paymentMethod.toLowerCase().includes('cash')) return false;
        } else if (paymentFilter === 'PREPAID') {
          if (o.paymentMethod.toLowerCase().includes('cash')) return false;
        }

        // Search query
        if (searchQuery.trim()) {
          const q = searchQuery.toLowerCase().trim();
          const matches =
            o.orderCode.toLowerCase().includes(q) ||
            o.customerName.toLowerCase().includes(q) ||
            o.customerPhone.includes(q) ||
            (o.assignedRiderName && o.assignedRiderName.toLowerCase().includes(q)) ||
            o.pickupAddress.toLowerCase().includes(q) ||
            o.dropAddress.toLowerCase().includes(q) ||
            (o.receiverName && o.receiverName.toLowerCase().includes(q));
          if (!matches) return false;
        }

        return true;
      })
      .sort((a, b) => {
        if (sortBy === 'FARE_HIGH') return b.totalAmountPaise - a.totalAmountPaise;
        if (sortBy === 'DISTANCE') return b.distanceKm - a.distanceKm;
        // Default NEWEST
        return (b.id || '').localeCompare(a.id || '');
      });
  }, [orders, statusFilter, paymentFilter, searchQuery, sortBy]);

  // Status Badge Helper
  const getStatusBadge = (status: OrderStatusKey, order?: ManagedOrder) => {
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
        bg: 'bg-rose-100 text-rose-900 border-rose-300',
        dot: 'bg-rose-600 animate-ping',
      };
    }

    switch (status) {
      case 'PENDING_ASSIGNMENT':
        return {
          label: 'Pending Assignment',
          bg: 'bg-amber-100 text-amber-900 border-amber-300',
          dot: 'bg-amber-500',
        };
      case 'ASSIGNED':
        return {
          label: 'Accepted (Assigned)',
          bg: 'bg-blue-100 text-blue-900 border-blue-300',
          dot: 'bg-blue-500',
        };
      case 'ARRIVED_AT_PICKUP':
        return {
          label: 'At Pickup Location',
          bg: 'bg-indigo-100 text-indigo-900 border-indigo-300',
          dot: 'bg-indigo-500',
        };
      case 'PICKED_UP':
      case 'IN_TRANSIT':
        return {
          label: 'Out for Delivery',
          bg: 'bg-purple-100 text-purple-900 border-purple-300',
          dot: 'bg-purple-500 animate-pulse',
        };
      case 'DELIVERED':
        return {
          label: 'Delivered',
          bg: 'bg-emerald-100 text-emerald-900 border-emerald-300',
          dot: 'bg-emerald-600',
        };
      case 'CANCELLED':
        return {
          label: 'Cancelled',
          bg: 'bg-red-100 text-red-900 border-red-300',
          dot: 'bg-red-600',
        };
      default:
        return {
          label: status,
          bg: 'bg-slate-100 text-slate-800 border-slate-300',
          dot: 'bg-slate-400',
        };
    }
  };

  const handleOpenStatusDialog = (order: ManagedOrder, targetStatus: OrderStatusKey) => {
    setStatusChangeDialog({
      order,
      targetStatus,
      selectedRider: order.assignedRiderName || AVAILABLE_RIDERS[0].name,
      note: '',
      cancellationReason: 'Customer requested cancellation before dispatch',
    });
  };

  const handleConfirmStatusChange = () => {
    if (!statusChangeDialog) return;
    const { order, targetStatus, selectedRider, note, cancellationReason } = statusChangeDialog;

    const riderObj = AVAILABLE_RIDERS.find((r) => r.name === selectedRider);

    onUpdateOrderStatus(order.id, targetStatus, {
      assignedRiderName: targetStatus === 'PENDING_ASSIGNMENT' ? undefined : (selectedRider || order.assignedRiderName),
      assignedRiderId: targetStatus === 'PENDING_ASSIGNMENT' ? undefined : (riderObj?.id || order.assignedRiderId),
      note,
      cancellationReason: targetStatus === 'CANCELLED' ? cancellationReason : undefined,
    });

    setStatusChangeDialog(null);
  };

  const toggleSelectAll = () => {
    if (selectedOrderIds.length === filteredOrders.length) {
      setSelectedOrderIds([]);
    } else {
      setSelectedOrderIds(filteredOrders.map((o) => o.id));
    }
  };

  const toggleSelectOrder = (id: string) => {
    setSelectedOrderIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  const handleExecuteBulkAction = (newStatus: OrderStatusKey) => {
    if (!selectedOrderIds.length) return;
    if (onBulkUpdateStatus) {
      onBulkUpdateStatus(selectedOrderIds, newStatus);
    } else {
      selectedOrderIds.forEach((id) => onUpdateOrderStatus(id, newStatus));
    }
    setSelectedOrderIds([]);
  };

  return (
    <div className="space-y-4">
      {/* 1. REAL-TIME STATUS KPI SUMMARY CARDS */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-2.5">
        {/* All Orders */}
        <button
          type="button"
          onClick={() => setStatusFilter('ALL')}
          className={`p-3 rounded-xl border text-left transition cursor-pointer ${
            statusFilter === 'ALL'
              ? 'bg-[#042F2E] text-white border-[#059669] shadow-sm'
              : 'bg-white text-slate-800 border-slate-200 hover:border-slate-300'
          }`}
        >
          <div className="text-[10px] font-bold uppercase tracking-wider opacity-75">All Orders</div>
          <div className="text-xl font-extrabold mt-0.5">{metrics.totalCount}</div>
          <div className="text-[11px] font-medium opacity-90 mt-0.5">₹{metrics.totalInr} Total</div>
        </button>

        {/* 1. Pending Assignment */}
        <button
          type="button"
          onClick={() => setStatusFilter('PENDING')}
          className={`p-3 rounded-xl border text-left transition cursor-pointer ${
            statusFilter === 'PENDING'
              ? 'bg-amber-600 text-white border-amber-700 shadow-sm'
              : 'bg-amber-50/70 text-amber-900 border-amber-200 hover:border-amber-300'
          }`}
        >
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold uppercase tracking-wider">Pending</span>
            <span className="w-2 h-2 rounded-full bg-amber-500 animate-ping" />
          </div>
          <div className="text-xl font-extrabold mt-0.5">{metrics.pendingCount}</div>
          <div className="text-[11px] font-medium mt-0.5">₹{metrics.pendingInr} • Searching</div>
        </button>

        {/* 2. Accepted / Assigned */}
        <button
          type="button"
          onClick={() => setStatusFilter('ACCEPTED')}
          className={`p-3 rounded-xl border text-left transition cursor-pointer ${
            statusFilter === 'ACCEPTED'
              ? 'bg-blue-600 text-white border-blue-700 shadow-sm'
              : 'bg-blue-50/70 text-blue-900 border-blue-200 hover:border-blue-300'
          }`}
        >
          <div className="text-[10px] font-bold uppercase tracking-wider">Accepted</div>
          <div className="text-xl font-extrabold mt-0.5">{metrics.acceptedCount}</div>
          <div className="text-[11px] font-medium mt-0.5">₹{metrics.acceptedInr} • Rider On-Way</div>
        </button>

        {/* 3. Out for Delivery */}
        <button
          type="button"
          onClick={() => setStatusFilter('OUT_FOR_DELIVERY')}
          className={`p-3 rounded-xl border text-left transition cursor-pointer ${
            statusFilter === 'OUT_FOR_DELIVERY'
              ? 'bg-purple-600 text-white border-purple-700 shadow-sm'
              : 'bg-purple-50/70 text-purple-900 border-purple-200 hover:border-purple-300'
          }`}
        >
          <div className="text-[10px] font-bold uppercase tracking-wider">Out for Delivery</div>
          <div className="text-xl font-extrabold mt-0.5">{metrics.outCount}</div>
          <div className="text-[11px] font-medium mt-0.5">₹{metrics.outInr} • In Transit</div>
        </button>

        {/* 4. Delivered */}
        <button
          type="button"
          onClick={() => setStatusFilter('DELIVERED')}
          className={`p-3 rounded-xl border text-left transition cursor-pointer ${
            statusFilter === 'DELIVERED'
              ? 'bg-emerald-600 text-white border-emerald-700 shadow-sm'
              : 'bg-emerald-50/70 text-emerald-900 border-emerald-200 hover:border-emerald-300'
          }`}
        >
          <div className="text-[10px] font-bold uppercase tracking-wider">Delivered</div>
          <div className="text-xl font-extrabold mt-0.5">{metrics.deliveredCount}</div>
          <div className="text-[11px] font-medium mt-0.5">₹{metrics.deliveredInr} • Settled</div>
        </button>

        {/* 5. Cancelled */}
        <button
          type="button"
          onClick={() => setStatusFilter('CANCELLED')}
          className={`p-3 rounded-xl border text-left transition cursor-pointer ${
            statusFilter === 'CANCELLED'
              ? 'bg-red-600 text-white border-red-700 shadow-sm'
              : 'bg-red-50/70 text-red-900 border-red-200 hover:border-red-300'
          }`}
        >
          <div className="text-[10px] font-bold uppercase tracking-wider">Cancelled</div>
          <div className="text-xl font-extrabold mt-0.5">{metrics.cancelledCount}</div>
          <div className="text-[11px] font-medium mt-0.5">₹{metrics.cancelledInr} • Void</div>
        </button>
      </div>

      {/* 2. SEARCH, FILTER BAR & BULK ACTION CONTROLS */}
      <div className="bg-white p-3.5 rounded-2xl border border-slate-200 shadow-xs space-y-3">
        <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3">
          {/* Search Box */}
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search by Order #, Customer, Rider, Area (e.g. Koramangala, Indiranagar)..."
              className="w-full pl-9 pr-3 py-2 text-xs rounded-xl bg-slate-50 border border-slate-200 focus:outline-none focus:border-[#059669] focus:bg-white text-slate-900"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => setSearchQuery('')}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          {/* Filters & Sorting */}
          <div className="flex flex-wrap items-center gap-2">
            {/* Payment Filter */}
            <select
              value={paymentFilter}
              onChange={(e) => setPaymentFilter(e.target.value as any)}
              className="py-1.5 px-2.5 text-xs font-semibold rounded-xl bg-slate-50 border border-slate-200 text-slate-700"
            >
              <option value="ALL">Payment: All</option>
              <option value="COD">Cash on Delivery (COD)</option>
              <option value="PREPAID">Prepaid (UPI / Card)</option>
            </select>

            {/* Sort Filter */}
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as any)}
              className="py-1.5 px-2.5 text-xs font-semibold rounded-xl bg-slate-50 border border-slate-200 text-slate-700"
            >
              <option value="NEWEST">Sort: Newest First</option>
              <option value="FARE_HIGH">Sort: Highest Fare</option>
              <option value="DISTANCE">Sort: Longest Distance</option>
            </select>

            {/* Create Demo Order shortcut */}
            {onSpawnOrder && (
              <button
                type="button"
                onClick={onSpawnOrder}
                className="py-1.5 px-3 rounded-xl bg-[#059669] hover:bg-emerald-600 text-white text-xs font-bold transition flex items-center gap-1.5 cursor-pointer shadow-xs"
              >
                <Sparkles className="w-3.5 h-3.5" />
                <span>+ Inject Order</span>
              </button>
            )}
          </div>
        </div>

        {/* Bulk Action Bar when items selected */}
        {selectedOrderIds.length > 0 && (
          <div className="p-2.5 rounded-xl bg-emerald-50 border border-emerald-200 flex flex-wrap items-center justify-between gap-2 text-xs">
            <div className="flex items-center gap-2">
              <span className="font-extrabold text-[#042F2E]">
                {selectedOrderIds.length} orders selected
              </span>
              <button
                type="button"
                onClick={() => setSelectedOrderIds([])}
                className="text-[11px] text-emerald-700 underline font-semibold"
              >
                Clear selection
              </button>
            </div>
            <div className="flex flex-wrap items-center gap-1.5">
              <span className="text-[11px] text-slate-600 font-semibold mr-1">Batch Update Status:</span>
              <button
                type="button"
                onClick={() => handleExecuteBulkAction('ASSIGNED')}
                className="px-2.5 py-1 rounded-lg bg-blue-600 text-white font-bold hover:bg-blue-700 transition"
              >
                Set Accepted
              </button>
              <button
                type="button"
                onClick={() => handleExecuteBulkAction('IN_TRANSIT')}
                className="px-2.5 py-1 rounded-lg bg-purple-600 text-white font-bold hover:bg-purple-700 transition"
              >
                Set Out for Delivery
              </button>
              <button
                type="button"
                onClick={() => handleExecuteBulkAction('DELIVERED')}
                className="px-2.5 py-1 rounded-lg bg-emerald-600 text-white font-bold hover:bg-emerald-700 transition"
              >
                Set Delivered
              </button>
              <button
                type="button"
                onClick={() => handleExecuteBulkAction('CANCELLED')}
                className="px-2.5 py-1 rounded-lg bg-red-600 text-white font-bold hover:bg-red-700 transition"
              >
                Set Cancelled
              </button>
            </div>
          </div>
        )}
      </div>

      {/* 3. ORDER STATUS MANAGEMENT TABLE & CARD LIST */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        {/* Table Header Controls */}
        <div className="px-4 py-3 bg-slate-50 border-b border-slate-200 flex items-center justify-between text-xs">
          <div className="flex items-center gap-3">
            <label className="flex items-center gap-2 cursor-pointer font-bold text-slate-700 select-none">
              <input
                type="checkbox"
                checked={selectedOrderIds.length > 0 && selectedOrderIds.length === filteredOrders.length}
                onChange={toggleSelectAll}
                className="w-4 h-4 rounded text-[#059669] focus:ring-[#059669] cursor-pointer"
              />
              <span>Select All ({filteredOrders.length})</span>
            </label>
            <span className="text-slate-400">|</span>
            <span className="text-slate-500 font-medium">
              Showing {filteredOrders.length} of {orders.length} orders
            </span>
          </div>

          <div className="text-[11px] text-slate-500 font-medium hidden sm:block">
            Real-time synchronization across Customer, Rider & Firebase Firestore active
          </div>
        </div>

        {filteredOrders.length === 0 ? (
          <div className="p-10 text-center space-y-3">
            <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center mx-auto text-slate-400">
              <Layers className="w-6 h-6" />
            </div>
            <div>
              <h4 className="text-sm font-extrabold text-slate-800">No Orders Match Current Filters</h4>
              <p className="text-xs text-slate-500 max-w-sm mx-auto mt-1">
                Try switching the status tab to &ldquo;All Orders&rdquo; or clearing the search query to see active and past dispatches.
              </p>
            </div>
            <button
              type="button"
              onClick={() => {
                setStatusFilter('ALL');
                setSearchQuery('');
                setPaymentFilter('ALL');
              }}
              className="px-4 py-2 rounded-xl bg-slate-900 text-white text-xs font-bold hover:bg-slate-800 transition"
            >
              Reset All Filters
            </button>
          </div>
        ) : (
          <div className="divide-y divide-slate-200">
            {filteredOrders.map((order) => {
              const badge = getStatusBadge(order.status, order);
              const totalInr = Math.round(order.totalAmountPaise / 100);
              const rider80Inr = Math.round(order.riderEarningPaise / 100);
              const admin20Inr = Math.round(order.companyCommissionPaise / 100);
              const isSelected = selectedOrderIds.includes(order.id);
              const isMapExpanded = expandedMapOrderId === order.id;
              const isTerminalOrder = order.status === 'DELIVERED' || order.status === 'CANCELLED';
              const schedMins = order.scheduledMinutes || 20;
              const estMins = order.estimatedMinutes || schedMins;

              return (
                <div
                  key={order.id}
                  className={`p-4 transition ${
                    isSelected ? 'bg-emerald-50/40' : 'hover:bg-slate-50/70'
                  }`}
                >
                  <div className="space-y-3">
                    {/* Top Row: Checkbox, Order Code, Status Badge & Quick Status Selector */}
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5">
                      <div className="flex items-center gap-2.5 flex-wrap">
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => toggleSelectOrder(order.id)}
                          className="w-4 h-4 rounded text-[#059669] focus:ring-[#059669] cursor-pointer"
                        />
                        <span className="px-2.5 py-1 rounded-md bg-[#042F2E] text-white text-xs font-extrabold tracking-wide">
                          #{order.orderCode}
                        </span>
                        <div
                          className={`px-2.5 py-0.5 rounded-full text-[11px] font-extrabold border flex items-center gap-1.5 ${badge.bg}`}
                        >
                          <span className={`w-2 h-2 rounded-full ${badge.dot}`} />
                          <span>{badge.label}</span>
                        </div>
                        {!isTerminalOrder && (
                          <span
                            className={`text-[10px] font-extrabold px-2 py-0.5 rounded-full border ${
                              badge.label === 'Delayed'
                                ? 'bg-rose-50 text-rose-800 border-rose-300'
                                : 'bg-emerald-50 text-emerald-800 border-emerald-200'
                            }`}
                          >
                            SLA: {schedMins}m • Est: {estMins}m
                            {badge.label === 'Delayed' ? ` (+${estMins - schedMins}m Delay)` : ''}
                          </span>
                        )}
                        <span className="text-[11px] font-semibold text-slate-500">
                          {order.createdAt}
                        </span>
                        <span className="text-[11px] font-bold text-slate-600 bg-slate-100 px-2 py-0.5 rounded-md">
                          {order.vehicleType} • {order.weightKg} kg • {order.distanceKm} km
                        </span>
                      </div>

                      {/* FAST STATUS TRANSITION BUTTONS FOR ADMIN */}
                      <div className="flex items-center gap-1.5 flex-wrap self-end sm:self-auto">
                        <span className="text-[11px] font-extrabold text-slate-600 mr-1">Update Status:</span>

                        {/* Status 1: Pending */}
                        <button
                          type="button"
                          onClick={() => handleOpenStatusDialog(order, 'PENDING_ASSIGNMENT')}
                          disabled={order.status === 'PENDING_ASSIGNMENT'}
                          title="Mark Pending / Searching Fleet"
                          className={`px-2 py-1 rounded-lg text-[10px] font-extrabold transition cursor-pointer ${
                            order.status === 'PENDING_ASSIGNMENT'
                              ? 'bg-amber-500 text-white cursor-default shadow-xs'
                              : 'bg-amber-50 hover:bg-amber-100 text-amber-800 border border-amber-200'
                          }`}
                        >
                          Pending
                        </button>

                        {/* Status 2: Accepted (Assigned) */}
                        <button
                          type="button"
                          onClick={() => handleOpenStatusDialog(order, 'ASSIGNED')}
                          disabled={order.status === 'ASSIGNED' || order.status === 'ARRIVED_AT_PICKUP'}
                          title="Assign Rider & Mark Accepted"
                          className={`px-2 py-1 rounded-lg text-[10px] font-extrabold transition cursor-pointer ${
                            order.status === 'ASSIGNED' || order.status === 'ARRIVED_AT_PICKUP'
                              ? 'bg-blue-600 text-white cursor-default shadow-xs'
                              : 'bg-blue-50 hover:bg-blue-100 text-blue-800 border border-blue-200'
                          }`}
                        >
                          Accepted
                        </button>

                        {/* Status 3: Out for Delivery */}
                        <button
                          type="button"
                          onClick={() => handleOpenStatusDialog(order, 'IN_TRANSIT')}
                          disabled={order.status === 'IN_TRANSIT' || order.status === 'PICKED_UP'}
                          title="Mark Out for Delivery / In Transit"
                          className={`px-2 py-1 rounded-lg text-[10px] font-extrabold transition cursor-pointer ${
                            order.status === 'IN_TRANSIT' || order.status === 'PICKED_UP'
                              ? 'bg-purple-600 text-white cursor-default shadow-xs'
                              : 'bg-purple-50 hover:bg-purple-100 text-purple-800 border border-purple-200'
                          }`}
                        >
                          Out for Delivery
                        </button>

                        {/* Status 4: Delivered */}
                        <button
                          type="button"
                          onClick={() => handleOpenStatusDialog(order, 'DELIVERED')}
                          disabled={order.status === 'DELIVERED'}
                          title="Mark Delivered & Release Rider Payout"
                          className={`px-2 py-1 rounded-lg text-[10px] font-extrabold transition cursor-pointer ${
                            order.status === 'DELIVERED'
                              ? 'bg-emerald-600 text-white cursor-default shadow-xs'
                              : 'bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-200'
                          }`}
                        >
                          Delivered
                        </button>

                        {/* Status 5: Cancelled */}
                        <button
                          type="button"
                          onClick={() => handleOpenStatusDialog(order, 'CANCELLED')}
                          disabled={order.status === 'CANCELLED'}
                          title="Cancel Order & Void Charges"
                          className={`px-2 py-1 rounded-lg text-[10px] font-extrabold transition cursor-pointer ${
                            order.status === 'CANCELLED'
                              ? 'bg-red-600 text-white cursor-default shadow-xs'
                              : 'bg-red-50 hover:bg-red-100 text-red-800 border border-red-200'
                          }`}
                        >
                          Cancelled
                        </button>

                        {/* Admin SLA Delay / ETA Trigger (>10 min threshold) */}
                        {!isTerminalOrder && onUpdateOrderEta && (
                          <button
                            type="button"
                            onClick={() =>
                              onUpdateOrderEta(
                                order.id,
                                badge.label === 'Delayed' ? schedMins : schedMins + 12
                              )
                            }
                            title="Simulate or clear >10 min delivery delay (triggers Customer & Rider alerts + 'Delayed' label)"
                            className={`px-2 py-1 rounded-lg text-[10px] font-extrabold transition cursor-pointer ${
                              badge.label === 'Delayed'
                                ? 'bg-rose-600 text-white shadow-xs hover:bg-rose-700'
                                : 'bg-rose-50 hover:bg-rose-100 text-rose-800 border border-rose-200'
                            }`}
                          >
                            {badge.label === 'Delayed' ? 'Reset On-Time' : '⏱️ +12m Delay (>10m SLA)'}
                          </button>
                        )}
                      </div>
                    </div>

                    {/* Route Details Card */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-2 text-xs bg-slate-50 p-2.5 rounded-xl border border-slate-200">
                      <div className="flex items-start gap-2">
                        <MapPin className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                        <div>
                          <div className="font-extrabold text-slate-800">
                            PICKUP ({order.pickupTag || 'Location'}):
                          </div>
                          <div className="text-slate-600">{order.pickupAddress}</div>
                          <div className="text-[11px] text-slate-500 mt-0.5">
                            Customer: <strong className="text-slate-700">{order.customerName}</strong> (+91 {order.customerPhone}) • OTP: <code className="bg-slate-200 px-1 py-0.5 rounded font-mono font-bold text-slate-800">{order.pickupOtp}</code>
                          </div>
                        </div>
                      </div>

                      <div className="flex items-start gap-2">
                        <Navigation className="w-4 h-4 text-orange-600 shrink-0 mt-0.5" />
                        <div>
                          <div className="font-extrabold text-slate-800">
                            DROP ({order.dropTag || 'Destination'}):
                          </div>
                          <div className="text-slate-600">{order.dropAddress}</div>
                          <div className="text-[11px] text-slate-500 mt-0.5">
                            Receiver: <strong className="text-slate-700">{order.receiverName || 'Self / Recipient'}</strong> (+91 {order.receiverPhone || order.customerPhone}) • Drop OTP: <code className="bg-slate-200 px-1 py-0.5 rounded font-mono font-bold text-slate-800">{order.deliveryOtp}</code>
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* Rider Info, Split Earnings & Operational Actions */}
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-1">
                      {/* Financials & Rider Details */}
                      <div className="flex flex-wrap items-center gap-2 text-xs">
                        <span className="px-2.5 py-1 rounded-lg bg-slate-900 text-white font-extrabold">
                          Total: ₹{totalInr}
                        </span>
                        <span className="px-2.5 py-1 rounded-lg bg-emerald-50 text-emerald-800 border border-emerald-200 font-extrabold">
                          80% Rider: ₹{rider80Inr}
                        </span>
                        <span className="px-2.5 py-1 rounded-lg bg-amber-50 text-amber-900 border border-amber-200 font-extrabold">
                          20% Admin: ₹{admin20Inr}
                        </span>

                        <span className={`px-2 py-0.5 rounded-md text-[11px] font-bold ${
                          order.paymentMethod.toLowerCase().includes('cash')
                            ? 'bg-amber-100 text-amber-900 border border-amber-300'
                            : 'bg-emerald-100 text-emerald-900 border border-emerald-300'
                        }`}>
                          {order.paymentMethod.replace(/_/g, ' ')}
                        </span>

                        <span className="text-slate-600 font-semibold flex items-center gap-1">
                          <User className="w-3.5 h-3.5 text-slate-400" />
                          <span>Rider:</span>
                          <strong className="text-slate-800">
                            {order.assignedRiderName || 'Unassigned (Fleet Radar Active)'}
                          </strong>
                          {order.assignedRiderPlate && (
                            <span className="text-[10px] text-slate-500">({order.assignedRiderPlate})</span>
                          )}
                        </span>
                      </div>

                      {/* Action Buttons */}
                      <div className="flex items-center gap-1.5 flex-wrap self-end sm:self-auto">
                        <button
                          type="button"
                          onClick={() => setExpandedMapOrderId(isMapExpanded ? null : order.id)}
                          className={`px-2.5 py-1.5 rounded-lg text-xs font-bold flex items-center gap-1 transition cursor-pointer ${
                            isMapExpanded
                              ? 'bg-[#042F2E] text-[#A7F3D0]'
                              : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
                          }`}
                        >
                          <Navigation className="w-3.5 h-3.5" />
                          <span>{isMapExpanded ? 'Hide Map' : 'GPS Route'}</span>
                        </button>

                        <button
                          type="button"
                          onClick={() => onTrackOrder(order)}
                          className="px-2.5 py-1.5 rounded-lg bg-[#059669] hover:bg-emerald-600 text-white text-xs font-bold flex items-center gap-1 transition cursor-pointer shadow-xs"
                        >
                          <Eye className="w-3.5 h-3.5" />
                          <span>Full Radar</span>
                        </button>

                        <button
                          type="button"
                          onClick={() => setActiveDetailsModalOrder(order)}
                          className="px-2.5 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-bold flex items-center gap-1 transition cursor-pointer"
                        >
                          <FileText className="w-3.5 h-3.5" />
                          <span>Timeline & Audit</span>
                        </button>

                        <button
                          type="button"
                          onClick={() => onDownloadGstInvoice(order)}
                          title="Download Official SAC 996812 GST Invoice"
                          className="p-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 transition cursor-pointer"
                        >
                          <Download className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>

                    {/* Expandable Embedded GPS Radar Map */}
                    {isMapExpanded && (
                      <div className="pt-2 border-t border-slate-200 space-y-1.5">
                        <div className="flex items-center justify-between text-[11px] font-extrabold text-[#042F2E]">
                          <span>Live Fleet GPS Monitoring: #{order.orderCode}</span>
                          <span className="text-slate-500 font-medium">
                            {order.pickupAddress} → {order.dropAddress}
                          </span>
                        </div>
                        <DeliveryMapComponent
                          orderId={order.id}
                          orderCode={order.orderCode}
                          status={order.status}
                          pickupAddress={order.pickupAddress}
                          dropAddress={order.dropAddress}
                          pickupTag={order.pickupTag || 'Pickup'}
                          dropTag={order.dropTag || 'Drop'}
                          vehicleType={order.vehicleType}
                          riderName={order.assignedRiderName || 'Vikram Singh'}
                          compact={false}
                        />
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* 4. STATUS CHANGE CONFIRMATION / REASSIGNMENT DIALOG MODAL */}
      {statusChangeDialog && (
        <div className="fixed inset-0 z-50 bg-slate-950/70 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="w-full max-w-lg bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            {/* Modal Header */}
            <div className="bg-gradient-to-r from-[#042F2E] via-[#065F46] to-[#059669] px-5 py-4 text-white flex items-center justify-between">
              <div>
                <h3 className="font-extrabold text-base flex items-center gap-2">
                  <RefreshCw className="w-4 h-4 text-[#A7F3D0]" />
                  <span>Update Order #{statusChangeDialog.order.orderCode} Status</span>
                </h3>
                <p className="text-xs text-[#A7F3D0] mt-0.5">
                  Change status in real time across Customer, Rider & Firestore Database
                </p>
              </div>
              <button
                type="button"
                onClick={() => setStatusChangeDialog(null)}
                className="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-white"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Modal Content */}
            <div className="p-5 space-y-4">
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-1 text-xs">
                <div className="flex justify-between">
                  <span className="text-slate-500 font-medium">Current Status:</span>
                  <span className="font-extrabold text-slate-800">
                    {getStatusBadge(statusChangeDialog.order.status).label}
                  </span>
                </div>
                <div className="flex justify-between items-center pt-1 border-t border-slate-200">
                  <span className="text-slate-500 font-medium">Target Status:</span>
                  <span className="px-2.5 py-0.5 rounded-full font-extrabold bg-[#059669] text-white">
                    {getStatusBadge(statusChangeDialog.targetStatus).label}
                  </span>
                </div>
              </div>

              {/* Rider Selection when transitioning to ACCEPTED or OUT_FOR_DELIVERY */}
              {(statusChangeDialog.targetStatus === 'ASSIGNED' ||
                statusChangeDialog.targetStatus === 'IN_TRANSIT') && (
                <div className="space-y-1.5">
                  <label className="text-xs font-extrabold text-slate-800">
                    Assign Rider Partner:
                  </label>
                  <select
                    value={statusChangeDialog.selectedRider}
                    onChange={(e) =>
                      setStatusChangeDialog((prev) =>
                        prev ? { ...prev, selectedRider: e.target.value } : null
                      )
                    }
                    className="w-full p-2.5 text-xs font-semibold rounded-xl bg-slate-50 border border-slate-200 focus:outline-none focus:border-[#059669]"
                  >
                    {AVAILABLE_RIDERS.map((r) => (
                      <option key={r.id} value={r.name}>
                        {r.name} ({r.vehicle} • {r.plate} • {r.rating})
                      </option>
                    ))}
                  </select>
                </div>
              )}

              {/* Cancellation Reason when transitioning to CANCELLED */}
              {statusChangeDialog.targetStatus === 'CANCELLED' && (
                <div className="space-y-1.5">
                  <label className="text-xs font-extrabold text-red-700">
                    Cancellation Reason:
                  </label>
                  <select
                    value={statusChangeDialog.cancellationReason}
                    onChange={(e) =>
                      setStatusChangeDialog((prev) =>
                        prev ? { ...prev, cancellationReason: e.target.value } : null
                      )
                    }
                    className="w-full p-2.5 text-xs font-semibold rounded-xl bg-red-50/50 border border-red-200 focus:outline-none focus:border-red-500"
                  >
                    <option value="Customer requested cancellation before dispatch">
                      Customer requested cancellation before dispatch
                    </option>
                    <option value="Customer address unreachable or incorrect phone">
                      Customer address unreachable or incorrect phone
                    </option>
                    <option value="Heavy rain / extreme weather transit disruption">
                      Heavy rain / extreme weather transit disruption
                    </option>
                    <option value="Package exceeds weight/safety limit">
                      Package exceeds weight/safety limit
                    </option>
                    <option value="Admin voided test dispatch">
                      Admin voided test dispatch
                    </option>
                  </select>
                </div>
              )}

              {/* Optional Administrative Note */}
              <div className="space-y-1.5">
                <label className="text-xs font-extrabold text-slate-700">
                  Audit Log Note (Optional):
                </label>
                <input
                  type="text"
                  value={statusChangeDialog.note}
                  onChange={(e) =>
                    setStatusChangeDialog((prev) =>
                      prev ? { ...prev, note: e.target.value } : null
                    )
                  }
                  placeholder="e.g. Verified with merchant, authorized emergency bypass..."
                  className="w-full p-2.5 text-xs rounded-xl bg-slate-50 border border-slate-200 focus:outline-none focus:border-[#059669]"
                />
              </div>

              {/* Status explanation notice */}
              <div className="text-[11px] text-slate-500 bg-slate-50 p-2.5 rounded-xl border border-slate-200 leading-relaxed">
                {statusChangeDialog.targetStatus === 'DELIVERED' && (
                  <span className="text-emerald-700 font-semibold">
                    ✓ Marking this order Delivered will credit 80% net earnings (₹
                    {Math.round(statusChangeDialog.order.riderEarningPaise / 100)}) to the Rider&apos;s
                    wallet and notify the Customer with delivery confirmation.
                  </span>
                )}
                {statusChangeDialog.targetStatus === 'CANCELLED' && (
                  <span className="text-red-700 font-semibold">
                    ⚠️ Cancelling will release the assigned Rider back to On-Duty Radar and send a
                    cancellation alert to the customer.
                  </span>
                )}
                {statusChangeDialog.targetStatus === 'IN_TRANSIT' && (
                  <span className="text-purple-700 font-semibold">
                    🛵 Out for Delivery status initiates live GPS broadcaster and updates the
                    Customer live tracking timeline.
                  </span>
                )}
                {statusChangeDialog.targetStatus === 'ASSIGNED' && (
                  <span className="text-blue-700 font-semibold">
                    📋 Assigns the order to the selected Rider Partner and issues pickup dispatch
                    instructions.
                  </span>
                )}
                {statusChangeDialog.targetStatus === 'PENDING_ASSIGNMENT' && (
                  <span className="text-amber-700 font-semibold">
                    ⏳ Moves order back to Pending queue for unassigned fleet dispatch.
                  </span>
                )}
              </div>

              {/* Modal Buttons */}
              <div className="flex items-center justify-end gap-2.5 pt-2">
                <button
                  type="button"
                  onClick={() => setStatusChangeDialog(null)}
                  className="px-4 py-2 rounded-xl border border-slate-300 text-slate-700 text-xs font-bold hover:bg-slate-50 transition"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleConfirmStatusChange}
                  className="px-5 py-2 rounded-xl bg-[#059669] hover:bg-emerald-600 text-white text-xs font-extrabold shadow-sm transition"
                >
                  Apply & Broadcast Status
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 5. TIMELINE & AUDIT HISTORY MODAL */}
      {activeDetailsModalOrder && (
        <div className="fixed inset-0 z-50 bg-slate-950/70 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="w-full max-w-xl bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="bg-[#042F2E] px-5 py-4 text-white flex items-center justify-between">
              <div>
                <h3 className="font-extrabold text-base flex items-center gap-2">
                  <FileText className="w-4 h-4 text-[#A7F3D0]" />
                  <span>Order Audit & Status Timeline: #{activeDetailsModalOrder.orderCode}</span>
                </h3>
                <p className="text-xs text-[#A7F3D0] mt-0.5">
                  Complete lifecycle history, OTPs & payout verification
                </p>
              </div>
              <button
                type="button"
                onClick={() => setActiveDetailsModalOrder(null)}
                className="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-white"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="p-5 space-y-4 max-h-[80vh] overflow-y-auto">
              {/* Order Status Badge & General Info */}
              <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50 border border-slate-200 text-xs">
                <div>
                  <div className="font-extrabold text-slate-900">
                    Status: {getStatusBadge(activeDetailsModalOrder.status).label}
                  </div>
                  <div className="text-slate-500 mt-0.5">Created at: {activeDetailsModalOrder.createdAt}</div>
                </div>
                <div className="text-right">
                  <div className="font-extrabold text-slate-900">
                    ₹{Math.round(activeDetailsModalOrder.totalAmountPaise / 100)}
                  </div>
                  <div className="text-emerald-700 font-bold">
                    80% Rider: ₹{Math.round(activeDetailsModalOrder.riderEarningPaise / 100)}
                  </div>
                </div>
              </div>

              {/* Handover OTPs */}
              <div className="grid grid-cols-2 gap-2 text-xs">
                <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200">
                  <div className="text-emerald-800 font-bold uppercase text-[10px]">Pickup Handover OTP</div>
                  <div className="text-lg font-mono font-extrabold text-[#042F2E] mt-0.5">
                    {activeDetailsModalOrder.pickupOtp}
                  </div>
                  <div className="text-[10px] text-emerald-700">Verified at merchant/sender</div>
                </div>
                <div className="p-3 rounded-xl bg-amber-50 border border-amber-200">
                  <div className="text-amber-800 font-bold uppercase text-[10px]">Delivery Handover OTP</div>
                  <div className="text-lg font-mono font-extrabold text-amber-900 mt-0.5">
                    {activeDetailsModalOrder.deliveryOtp}
                  </div>
                  <div className="text-[10px] text-amber-700">Required for final delivery drop</div>
                </div>
              </div>

              {/* Status Progression Timeline */}
              <div className="space-y-2">
                <h4 className="text-xs font-extrabold uppercase tracking-wider text-slate-700">
                  Order Progression Audit Trail
                </h4>
                <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 space-y-3 text-xs">
                  {/* Step 1 */}
                  <div className="flex items-start gap-2.5">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                    <div>
                      <div className="font-bold text-slate-800">1. Order Placed by Customer</div>
                      <div className="text-slate-500 text-[11px]">
                        Customer: {activeDetailsModalOrder.customerName} (+91 {activeDetailsModalOrder.customerPhone})
                      </div>
                    </div>
                  </div>

                  {/* Step 2 */}
                  <div className="flex items-start gap-2.5">
                    {activeDetailsModalOrder.status !== 'PENDING_ASSIGNMENT' ? (
                      <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                    ) : (
                      <Clock className="w-4 h-4 text-amber-500 shrink-0 mt-0.5" />
                    )}
                    <div>
                      <div className="font-bold text-slate-800">2. Rider Assigned / Accepted</div>
                      <div className="text-slate-500 text-[11px]">
                        Rider: {activeDetailsModalOrder.assignedRiderName || 'Awaiting acceptance'} • {activeDetailsModalOrder.assignedRiderPlate || ''}
                      </div>
                    </div>
                  </div>

                  {/* Step 3 */}
                  <div className="flex items-start gap-2.5">
                    {['PICKED_UP', 'IN_TRANSIT', 'DELIVERED'].includes(activeDetailsModalOrder.status) ? (
                      <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                    ) : (
                      <Clock className="w-4 h-4 text-slate-400 shrink-0 mt-0.5" />
                    )}
                    <div>
                      <div className="font-bold text-slate-800">3. Parcel Picked Up & In Transit</div>
                      <div className="text-slate-500 text-[11px]">
                        {activeDetailsModalOrder.pickupPhotoCaptured ? '✓ Photo handover verified' : 'Pickup OTP authenticated'}
                      </div>
                    </div>
                  </div>

                  {/* Step 4 */}
                  <div className="flex items-start gap-2.5">
                    {activeDetailsModalOrder.status === 'DELIVERED' ? (
                      <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                    ) : activeDetailsModalOrder.status === 'CANCELLED' ? (
                      <XCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
                    ) : (
                      <Clock className="w-4 h-4 text-slate-400 shrink-0 mt-0.5" />
                    )}
                    <div>
                      <div className="font-bold text-slate-800">
                        {activeDetailsModalOrder.status === 'CANCELLED'
                          ? '4. Order Cancelled'
                          : '4. Delivery Complete & Handover'}
                      </div>
                      <div className="text-slate-500 text-[11px]">
                        {activeDetailsModalOrder.status === 'CANCELLED'
                          ? `Reason: ${activeDetailsModalOrder.cancellationReason || 'Admin or customer cancelled'}`
                          : activeDetailsModalOrder.status === 'DELIVERED'
                          ? `Delivered safely. ${activeDetailsModalOrder.deliveryProofNote || 'Drop OTP validated.'}`
                          : 'Awaiting destination drop'}
                      </div>
                    </div>
                  </div>
                </div>

                {/* Delivery Proof Photo in Admin Audit Modal */}
                {activeDetailsModalOrder.deliveryPhotoUrl && (
                  <div className="p-3.5 rounded-xl bg-emerald-50/70 border border-emerald-200 space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-extrabold text-emerald-950 flex items-center gap-1.5">
                        <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                        <span>Delivery Proof Photo (Geotagged &amp; Verified)</span>
                      </span>
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-emerald-200 text-emerald-950">
                        HD POD
                      </span>
                    </div>
                    <div className="rounded-lg overflow-hidden border border-emerald-300 bg-black/5 max-h-48 flex items-center justify-center">
                      <img
                        src={activeDetailsModalOrder.deliveryPhotoUrl}
                        alt="Delivery Proof"
                        className="max-h-48 w-full object-cover"
                      />
                    </div>
                    {activeDetailsModalOrder.deliveryProofNote && (
                      <p className="text-[11px] text-emerald-800 font-medium">
                        Note: {activeDetailsModalOrder.deliveryProofNote}
                      </p>
                    )}
                  </div>
                )}
              </div>

              <div className="flex items-center justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => onDownloadGstInvoice(activeDetailsModalOrder)}
                  className="px-4 py-2 rounded-xl bg-[#059669] text-white text-xs font-bold hover:bg-emerald-600 transition flex items-center gap-1.5"
                >
                  <Download className="w-3.5 h-3.5" />
                  <span>Download GST Invoice</span>
                </button>
                <button
                  type="button"
                  onClick={() => setActiveDetailsModalOrder(null)}
                  className="px-4 py-2 rounded-xl bg-slate-900 text-white text-xs font-bold hover:bg-slate-800 transition"
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
