import React, { useState, useMemo } from 'react';
import {
  CheckCircle2,
  Clock,
  Navigation,
  MapPin,
  IndianRupee,
  Search,
  Filter,
  Download,
  Phone,
  ShieldCheck,
  Bike,
  Truck,
  Zap,
  Banknote,
  Star,
  FileText,
  AlertTriangle,
  Layers,
  ChevronRight,
  Eye,
  Check
} from 'lucide-react';
import { DeliveryMapComponent } from './DeliveryMapComponent';

export interface RiderTrip {
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
  pickupOtp: string;
  deliveryOtp: string;
  assignedRiderId?: string;
  assignedRiderName?: string;
  assignedRiderRating?: string;
  pickupPhotoCaptured?: boolean;
  pickupPhotoUrl?: string;
  deliveryProofNote?: string;
  deliveryPhotoUrl?: string;
  createdAt: string;
  cancellationReason?: string;
}

interface RiderTripHistoryViewProps {
  orders: RiderTrip[];
  currentRiderId?: string;
  onTrackOrder: (trip: RiderTrip) => void;
  onDownloadGstInvoice: (trip: RiderTrip) => void;
}

export const RiderTripHistoryView: React.FC<RiderTripHistoryViewProps> = ({
  orders,
  currentRiderId = 'RIDER-101',
  onTrackOrder,
  onDownloadGstInvoice,
}) => {
  const [filterTab, setFilterTab] = useState<'ALL' | 'COMPLETED' | 'ACTIVE' | 'CANCELLED'>('COMPLETED');
  const [paymentFilter, setPaymentFilter] = useState<'ALL' | 'COD' | 'ONLINE'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTripDetails, setSelectedTripDetails] = useState<RiderTrip | null>(null);
  const [expandedMapId, setExpandedMapId] = useState<string | null>(null);

  // Filter trips for this rider (or all if demo)
  const riderOrders = useMemo(() => {
    return orders.filter((o) => {
      // If order is assigned to this rider, or in unassigned list during demo
      if (o.assignedRiderId && o.assignedRiderId === currentRiderId) return true;
      if (!o.assignedRiderId && o.status === 'DELIVERED') return true;
      return true;
    });
  }, [orders, currentRiderId]);

  // Metrics
  const metrics = useMemo(() => {
    let completedCount = 0;
    let completedEarningsPaise = 0;
    let codCashPaise = 0;
    let totalDist = 0;

    riderOrders.forEach((o) => {
      if (o.status === 'DELIVERED') {
        completedCount++;
        completedEarningsPaise += o.riderEarningPaise;
        totalDist += o.distanceKm;
        if (o.paymentMethod.toLowerCase().includes('cash')) {
          codCashPaise += o.totalAmountPaise;
        }
      }
    });

    return {
      completedCount,
      earningsInr: Math.round(completedEarningsPaise / 100),
      codCashInr: Math.round(codCashPaise / 100),
      avgDistance: completedCount > 0 ? (totalDist / completedCount).toFixed(1) : '0.0',
    };
  }, [riderOrders]);

  // Filtered list
  const filteredTrips = useMemo(() => {
    return riderOrders.filter((trip) => {
      // Tab filter
      if (filterTab === 'COMPLETED') {
        if (trip.status !== 'DELIVERED') return false;
      } else if (filterTab === 'ACTIVE') {
        if (trip.status === 'DELIVERED' || trip.status === 'CANCELLED') return false;
      } else if (filterTab === 'CANCELLED') {
        if (trip.status !== 'CANCELLED') return false;
      }

      // Payment filter
      if (paymentFilter === 'COD') {
        if (!trip.paymentMethod.toLowerCase().includes('cash')) return false;
      } else if (paymentFilter === 'ONLINE') {
        if (trip.paymentMethod.toLowerCase().includes('cash')) return false;
      }

      // Search query
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase().trim();
        const matches =
          trip.orderCode.toLowerCase().includes(q) ||
          trip.customerName.toLowerCase().includes(q) ||
          trip.pickupAddress.toLowerCase().includes(q) ||
          trip.dropAddress.toLowerCase().includes(q);
        if (!matches) return false;
      }

      return true;
    });
  }, [riderOrders, filterTab, paymentFilter, searchQuery]);

  return (
    <div className="space-y-4">
      {/* 1. RIDER CAREER & HISTORY SUMMARY STATS */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
        <div className="p-3.5 rounded-2xl bg-emerald-50 border border-emerald-200">
          <div className="text-[10px] font-bold uppercase tracking-wider text-emerald-800">
            Completed Trips
          </div>
          <div className="text-xl font-extrabold text-[#042F2E] mt-0.5">
            {metrics.completedCount} Deliveries
          </div>
          <div className="text-[11px] font-semibold text-emerald-700 flex items-center gap-1 mt-0.5">
            <CheckCircle2 className="w-3.5 h-3.5" />
            <span>100% Verified Handover</span>
          </div>
        </div>

        <div className="p-3.5 rounded-2xl bg-gradient-to-br from-[#042F2E] to-[#065F46] text-white">
          <div className="text-[10px] font-bold uppercase tracking-wider text-[#A7F3D0]">
            80% Net Earnings
          </div>
          <div className="text-xl font-extrabold text-white mt-0.5">
            ₹{metrics.earningsInr}
          </div>
          <div className="text-[11px] text-[#A7F3D0] mt-0.5">
            Direct Instant UPI Payout
          </div>
        </div>

        <div className="p-3.5 rounded-2xl bg-amber-50 border border-amber-200">
          <div className="text-[10px] font-bold uppercase tracking-wider text-amber-800">
            COD Cash Handled
          </div>
          <div className="text-xl font-extrabold text-amber-900 mt-0.5">
            ₹{metrics.codCashInr}
          </div>
          <div className="text-[11px] font-semibold text-amber-700 mt-0.5">
            Cash-in-Hand Verified
          </div>
        </div>

        <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200">
          <div className="text-[10px] font-bold uppercase tracking-wider text-slate-500">
            Avg Distance & Rating
          </div>
          <div className="text-xl font-extrabold text-slate-800 mt-0.5">
            {metrics.avgDistance} km • 4.9★
          </div>
          <div className="text-[11px] font-semibold text-amber-600 mt-0.5">
            Top Rated Rider Partner
          </div>
        </div>
      </div>

      {/* 2. FILTER TABS & SEARCH CONTROLS */}
      <div className="bg-white p-3 rounded-2xl border border-slate-200 shadow-xs space-y-3">
        {/* Filter Tabs */}
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex flex-wrap items-center gap-1.5">
            <button
              type="button"
              onClick={() => setFilterTab('COMPLETED')}
              className={`px-3 py-1.5 rounded-xl text-xs font-extrabold transition cursor-pointer ${
                filterTab === 'COMPLETED'
                  ? 'bg-[#059669] text-white shadow-xs'
                  : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
              }`}
            >
              Completed Deliveries ({riderOrders.filter((o) => o.status === 'DELIVERED').length})
            </button>
            <button
              type="button"
              onClick={() => setFilterTab('ALL')}
              className={`px-3 py-1.5 rounded-xl text-xs font-extrabold transition cursor-pointer ${
                filterTab === 'ALL'
                  ? 'bg-[#042F2E] text-white shadow-xs'
                  : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
              }`}
            >
              All Trips ({riderOrders.length})
            </button>
            <button
              type="button"
              onClick={() => setFilterTab('ACTIVE')}
              className={`px-3 py-1.5 rounded-xl text-xs font-extrabold transition cursor-pointer ${
                filterTab === 'ACTIVE'
                  ? 'bg-purple-600 text-white shadow-xs'
                  : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
              }`}
            >
              Active / In-Transit ({riderOrders.filter((o) => o.status !== 'DELIVERED' && o.status !== 'CANCELLED').length})
            </button>
            <button
              type="button"
              onClick={() => setFilterTab('CANCELLED')}
              className={`px-3 py-1.5 rounded-xl text-xs font-extrabold transition cursor-pointer ${
                filterTab === 'CANCELLED'
                  ? 'bg-red-600 text-white shadow-xs'
                  : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
              }`}
            >
              Cancelled ({riderOrders.filter((o) => o.status === 'CANCELLED').length})
            </button>
          </div>

          {/* Payment filter dropdown */}
          <select
            value={paymentFilter}
            onChange={(e) => setPaymentFilter(e.target.value as any)}
            className="py-1 px-2.5 text-xs font-semibold rounded-xl bg-slate-50 border border-slate-200 text-slate-700"
          >
            <option value="ALL">All Payment Modes</option>
            <option value="COD">Cash on Delivery</option>
            <option value="ONLINE">Prepaid UPI / Card</option>
          </select>
        </div>

        {/* Search */}
        <div className="relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search trip history by Order #, destination, or customer..."
            className="w-full pl-9 pr-3 py-2 text-xs rounded-xl bg-slate-50 border border-slate-200 focus:outline-none focus:border-[#059669]"
          />
        </div>
      </div>

      {/* 3. TRIP LIST */}
      <div className="space-y-3">
        {filteredTrips.length === 0 ? (
          <div className="p-8 text-center bg-white rounded-2xl border border-slate-200 space-y-2">
            <div className="w-10 h-10 rounded-full bg-slate-100 flex items-center justify-center mx-auto text-slate-400">
              <Layers className="w-5 h-5" />
            </div>
            <div className="text-xs font-bold text-slate-700">No Trips Found in This Filter</div>
            <div className="text-[11px] text-slate-500">
              Switch filter to &ldquo;All Trips&rdquo; or complete an active delivery to see it logged here.
            </div>
          </div>
        ) : (
          filteredTrips.map((trip) => {
            const riderShareInr = Math.round(trip.riderEarningPaise / 100);
            const totalInr = Math.round(trip.totalAmountPaise / 100);
            const isDelivered = trip.status === 'DELIVERED';
            const isCod = trip.paymentMethod.toLowerCase().includes('cash');
            const isMapOpen = expandedMapId === trip.id;

            return (
              <div
                key={trip.id}
                className="p-4 rounded-2xl bg-white border border-slate-200 shadow-xs hover:border-emerald-300 transition space-y-3"
              >
                {/* Header: Order Code, Date, Status */}
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <span className="px-2.5 py-1 rounded-md bg-[#042F2E] text-white font-extrabold text-xs">
                      #{trip.orderCode}
                    </span>
                    <span
                      className={`px-2 py-0.5 rounded-full text-[10px] font-extrabold ${
                        isDelivered
                          ? 'bg-emerald-100 text-emerald-900 border border-emerald-300'
                          : trip.status === 'CANCELLED'
                          ? 'bg-red-100 text-red-900 border border-red-300'
                          : 'bg-purple-100 text-purple-900 border border-purple-300'
                      }`}
                    >
                      {trip.status.replace(/_/g, ' ')}
                    </span>
                    <span className="text-[11px] text-slate-500">{trip.createdAt}</span>
                  </div>

                  {/* Rider Net Payout */}
                  <div className="flex items-center gap-2">
                    <span className="px-3 py-1 rounded-xl bg-emerald-50 border border-emerald-200 text-[#042F2E] font-extrabold text-xs">
                      80% Net Earned: <strong className="text-emerald-700 text-sm">₹{riderShareInr}</strong>
                    </span>
                  </div>
                </div>

                {/* Route */}
                <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-1.5 text-xs">
                  <div className="flex items-start gap-2">
                    <MapPin className="w-3.5 h-3.5 text-emerald-600 shrink-0 mt-0.5" />
                    <div>
                      <span className="font-extrabold text-slate-700">Pickup: </span>
                      <span className="text-slate-600">{trip.pickupAddress}</span>
                    </div>
                  </div>
                  <div className="flex items-start gap-2">
                    <Navigation className="w-3.5 h-3.5 text-orange-600 shrink-0 mt-0.5" />
                    <div>
                      <span className="font-extrabold text-slate-700">Drop: </span>
                      <span className="text-slate-600">{trip.dropAddress}</span>
                    </div>
                  </div>
                </div>

                {/* Metadata details row */}
                <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="px-2 py-0.5 rounded-md bg-slate-100 font-bold text-slate-700">
                      {trip.vehicleType} • {trip.weightKg} kg • {trip.distanceKm} km
                    </span>
                    <span
                      className={`px-2 py-0.5 rounded-md font-bold text-[11px] ${
                        isCod
                          ? 'bg-amber-100 text-amber-900 border border-amber-300'
                          : 'bg-blue-100 text-blue-900 border border-blue-300'
                      }`}
                    >
                      {isCod ? `💵 Cash on Delivery (₹${totalInr} Collected)` : '💳 Prepaid via UPI'}
                    </span>
                    {isDelivered && (
                      <span className="px-2 py-0.5 rounded-md bg-emerald-100 text-emerald-800 text-[11px] font-bold flex items-center gap-1">
                        <Check className="w-3 h-3 text-emerald-700" />
                        <span>Delivery OTP Handover Verified</span>
                      </span>
                    )}
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => setExpandedMapId(isMapOpen ? null : trip.id)}
                      className="px-2.5 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold transition flex items-center gap-1 cursor-pointer"
                    >
                      <Navigation className="w-3.5 h-3.5" />
                      <span>{isMapOpen ? 'Hide Route' : 'GPS Route'}</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => onTrackOrder(trip)}
                      className="px-2.5 py-1.5 rounded-lg bg-[#059669] hover:bg-emerald-600 text-white text-xs font-bold transition flex items-center gap-1 cursor-pointer shadow-xs"
                    >
                      <Eye className="w-3.5 h-3.5" />
                      <span>Radar View</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => onDownloadGstInvoice(trip)}
                      title="Download GST Invoice"
                      className="p-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 transition cursor-pointer"
                    >
                      <Download className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>

                {/* Delivery Proof Photo if Captured */}
                {trip.deliveryPhotoUrl && (
                  <div className="p-2.5 rounded-xl bg-emerald-50/70 border border-emerald-200 flex items-center gap-3">
                    <img
                      src={trip.deliveryPhotoUrl}
                      alt="Delivery Proof"
                      className="w-14 h-14 rounded-lg object-cover border border-emerald-300 shrink-0"
                    />
                    <div className="text-xs">
                      <div className="font-extrabold text-emerald-950 flex items-center gap-1">
                        <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                        <span>Delivery Proof Photo Verified</span>
                      </div>
                      <div className="text-[11px] text-emerald-800 font-medium mt-0.5">
                        {trip.deliveryProofNote || 'Handover authenticated with photo & Drop OTP.'}
                      </div>
                    </div>
                  </div>
                )}

                {/* Expanded GPS Route */}
                {isMapOpen && (
                  <div className="pt-2 border-t border-slate-200 space-y-1.5">
                    <div className="text-[11px] font-extrabold text-[#042F2E]">
                      Trip GPS Path: #{trip.orderCode} ({trip.distanceKm} km)
                    </div>
                    <DeliveryMapComponent
                      orderId={trip.id}
                      orderCode={trip.orderCode}
                      status={trip.status as any}
                      pickupAddress={trip.pickupAddress}
                      dropAddress={trip.dropAddress}
                      pickupTag={trip.pickupTag || 'Pickup'}
                      dropTag={trip.dropTag || 'Drop'}
                      vehicleType={trip.vehicleType}
                      riderName={trip.assignedRiderName || 'Vikram Singh'}
                      compact={true}
                    />
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
