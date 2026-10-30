import React, { useState, useEffect, useMemo } from 'react';
import {
  Navigation,
  MapPin,
  Bike,
  Truck,
  Car,
  Crosshair,
  Compass,
  Layers,
  Radio,
  Clock,
  Phone,
  MessageSquare,
  ShieldCheck,
  Zap,
  Filter,
  Eye,
  RotateCw,
  Search,
  ExternalLink,
  Activity
} from 'lucide-react';

interface FleetOrder {
  id: string;
  orderCode: string;
  status: string;
  customerName: string;
  pickupAddress: string;
  dropAddress: string;
  vehicleType: string;
  assignedRiderName?: string;
  assignedRiderPlate?: string;
  assignedRiderPhone?: string;
  assignedRiderRating?: string;
  riderLat?: number;
  riderLng?: number;
  totalAmountPaise: number;
}

interface AdminFleetGpsRadarProps {
  orders: FleetOrder[];
  onSelectOrder?: (order: FleetOrder) => void;
}

export const AdminFleetGpsRadar: React.FC<AdminFleetGpsRadarProps> = ({
  orders,
  onSelectOrder,
}) => {
  const [mapTheme, setMapTheme] = useState<'TACTICAL_RADAR' | 'METRO_STREET' | 'SATELLITE'>('TACTICAL_RADAR');
  const [selectedVehicleId, setSelectedVehicleId] = useState<string | null>(null);
  const [vehicleFilter, setVehicleFilter] = useState<'ALL' | 'BIKE' | 'EV' | 'MINI_TRUCK'>('ALL');
  const [searchFilter, setSearchFilter] = useState<string>('');
  const [radarPulse, setRadarPulse] = useState<number>(0);
  const [isLiveTelemetryActive, setIsLiveTelemetryActive] = useState<boolean>(true);

  // Filter active fleet deliveries (assigned, arrived, in transit, etc.)
  const activeFleet = useMemo(() => {
    return orders.filter((o) => {
      const isOngoing = o.status !== 'DELIVERED';
      const matchesVehicle = vehicleFilter === 'ALL' || o.vehicleType === vehicleFilter;
      const matchesSearch =
        !searchFilter ||
        o.orderCode.toLowerCase().includes(searchFilter.toLowerCase()) ||
        (o.assignedRiderName && o.assignedRiderName.toLowerCase().includes(searchFilter.toLowerCase())) ||
        o.customerName.toLowerCase().includes(searchFilter.toLowerCase());
      return isOngoing && matchesVehicle && matchesSearch;
    });
  }, [orders, vehicleFilter, searchFilter]);

  // Dynamic simulation tick for micro-movements on admin radar
  useEffect(() => {
    if (!isLiveTelemetryActive) return;
    const interval = setInterval(() => {
      setRadarPulse((prev) => (prev + 1) % 360);
    }, 1800);
    return () => clearInterval(interval);
  }, [isLiveTelemetryActive]);

  // Normalized map coordinates calculation for canvas
  const fleetPositions = useMemo(() => {
    // Metro landmarks & corridors in Bangalore
    const landmarks = [
      { name: 'Indiranagar Hub', x: 260, y: 140 },
      { name: 'MG Road Central', x: 200, y: 170 },
      { name: 'Koramangala 4th Block', x: 240, y: 250 },
      { name: 'HSR Layout Sector 1', x: 280, y: 310 },
      { name: 'Whitefield Tech Park', x: 370, y: 130 },
      { name: 'Electronic City Phase 1', x: 260, y: 390 },
      { name: 'Hebbal Flyover', x: 190, y: 70 },
    ];

    // Compute synthetic canvas coordinates for each order
    return activeFleet.map((ord, idx) => {
      const seed = (idx + 1) * 37;
      const startLm = landmarks[seed % landmarks.length];
      const endLm = landmarks[(seed + 3) % landmarks.length];

      // Progress along vector
      const progress = ord.status === 'IN_TRANSIT' ? 0.6 : ord.status === 'ARRIVED_AT_PICKUP' ? 0.15 : 0.4;
      const jitterX = Math.sin(radarPulse * 0.05 + idx) * 6;
      const jitterY = Math.cos(radarPulse * 0.05 + idx) * 6;

      const currentX = startLm.x + (endLm.x - startLm.x) * progress + jitterX;
      const currentY = startLm.y + (endLm.y - startLm.y) * progress + jitterY;

      return {
        order: ord,
        startX: startLm.x,
        startY: startLm.y,
        endX: endLm.x,
        endY: endLm.y,
        currentX,
        currentY,
        speedKmh: ord.status === 'IN_TRANSIT' ? Math.floor(32 + (seed % 14)) : 0,
        batteryPercent: Math.max(40, 98 - (seed % 50)),
      };
    });
  }, [activeFleet, radarPulse]);

  const selectedItem = fleetPositions.find((f) => f.order.id === selectedVehicleId) || fleetPositions[0];

  return (
    <div className="bg-white rounded-2xl border-2 border-[#042F2E]/30 shadow-md overflow-hidden flex flex-col">
      
      {/* Top Radar Bar */}
      <div className="bg-gradient-to-r from-[#042F2E] via-[#065F46] to-[#059669] text-white px-4 py-3 flex flex-wrap items-center justify-between gap-2.5">
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-xl bg-white/10 border border-emerald-300/40 flex items-center justify-center text-white shrink-0 shadow-inner">
            <Radio className="w-5 h-5 text-[#A7F3D0] animate-pulse" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="font-extrabold text-sm sm:text-base leading-tight">
                Live Fleet GPS Command Radar
              </h3>
              <span className="text-[10px] font-extrabold uppercase tracking-wider bg-emerald-400/20 text-[#A7F3D0] border border-emerald-300/30 px-2 py-0.5 rounded-full">
                {activeFleet.length} Active Vehicles
              </span>
            </div>
            <p className="text-[11px] text-emerald-100/80 mt-0.5">
              Simultaneous multi-vehicle telemetry • RTK High-Precision GPS • Live Speed &amp; Heading
            </p>
          </div>
        </div>

        {/* Radar Controls */}
        <div className="flex flex-wrap items-center gap-1.5 text-xs">
          {/* Theme Switcher */}
          <div className="bg-black/30 p-0.5 rounded-xl border border-white/10 flex items-center text-[10px] font-black">
            <button
              type="button"
              onClick={() => setMapTheme('TACTICAL_RADAR')}
              className={`px-2.5 py-1 rounded-lg transition ${
                mapTheme === 'TACTICAL_RADAR' ? 'bg-[#10B981] text-[#022C22]' : 'text-slate-300 hover:text-white'
              }`}
            >
              Radar (Dark)
            </button>
            <button
              type="button"
              onClick={() => setMapTheme('METRO_STREET')}
              className={`px-2.5 py-1 rounded-lg transition ${
                mapTheme === 'METRO_STREET' ? 'bg-[#10B981] text-[#022C22]' : 'text-slate-300 hover:text-white'
              }`}
            >
              Street (Light)
            </button>
          </div>

          <button
            type="button"
            onClick={() => setIsLiveTelemetryActive((prev) => !prev)}
            className={`px-2.5 py-1.5 rounded-xl border text-[11px] font-extrabold flex items-center gap-1.5 transition cursor-pointer ${
              isLiveTelemetryActive
                ? 'bg-emerald-500/20 border-emerald-300/40 text-[#A7F3D0]'
                : 'bg-white/10 border-white/20 text-slate-300'
            }`}
          >
            <Activity className={`w-3.5 h-3.5 ${isLiveTelemetryActive ? 'animate-spin' : ''}`} />
            <span>{isLiveTelemetryActive ? 'Live Telemetry ON' : 'Telemetry Paused'}</span>
          </button>
        </div>
      </div>

      {/* Filter Strip */}
      <div className="p-3 bg-slate-50 border-b border-slate-200 flex flex-wrap items-center justify-between gap-2 text-xs">
        <div className="flex items-center gap-1.5 flex-wrap">
          <span className="text-[10px] font-black uppercase tracking-wider text-slate-400">
            Vehicle Fleet:
          </span>
          {(['ALL', 'BIKE', 'EV', 'MINI_TRUCK'] as const).map((vt) => (
            <button
              key={vt}
              type="button"
              onClick={() => setVehicleFilter(vt)}
              className={`px-2.5 py-1 rounded-lg font-extrabold transition cursor-pointer text-xs ${
                vehicleFilter === vt
                  ? 'bg-[#042F2E] text-[#A7F3D0]'
                  : 'bg-white text-slate-700 border border-slate-200 hover:bg-slate-100'
              }`}
            >
              {vt === 'ALL' ? 'All (Fleet)' : vt === 'BIKE' ? '🏍️ 2-Wheeler' : vt === 'EV' ? '⚡ Mini EV' : '🚚 Mini Truck'}
            </button>
          ))}
        </div>

        <div className="relative">
          <Search className="w-3.5 h-3.5 text-slate-400 absolute left-2.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchFilter}
            onChange={(e) => setSearchFilter(e.target.value)}
            placeholder="Search rider, order ID or customer..."
            className="pl-8 pr-3 py-1 text-xs bg-white border border-slate-300 rounded-lg outline-none focus:border-[#059669] w-48 sm:w-64"
          />
        </div>
      </div>

      {/* Grid: Main Radar Canvas + Side Telemetry Panel */}
      <div className="grid grid-cols-1 lg:grid-cols-12 items-stretch min-h-[420px]">
        
        {/* Main Radar Canvas View (Col 8) */}
        <div
          className={`lg:col-span-8 relative overflow-hidden flex items-center justify-center p-2 sm:p-4 select-none ${
            mapTheme === 'TACTICAL_RADAR' ? 'bg-[#031A19]' : 'bg-[#E5E9EC]'
          }`}
        >
          {/* High-Tech Vector Grid Canvas */}
          <svg
            viewBox="0 0 500 440"
            className="w-full h-full max-h-[460px] drop-shadow-sm"
          >
            <defs>
              {/* Grid pattern */}
              <pattern id="radarGrid" width="40" height="40" patternUnits="userSpaceOnUse">
                <path
                  d="M 40 0 L 0 0 0 40"
                  fill="none"
                  stroke={mapTheme === 'TACTICAL_RADAR' ? '#043634' : '#CBD5E1'}
                  strokeWidth="1"
                />
              </pattern>

              {/* Pulsing beacon glow */}
              <radialGradient id="vehiclePulse" cx="50%" cy="50%" r="50%">
                <stop offset="0%" stopColor="#10B981" stopOpacity="0.8" />
                <stop offset="60%" stopColor="#059669" stopOpacity="0.3" />
                <stop offset="100%" stopColor="#059669" stopOpacity="0" />
              </radialGradient>
            </defs>

            {/* Background Grid */}
            <rect width="500" height="440" fill="url(#radarGrid)" />

            {/* Major Arterial Roads / Expressways in Vector Canvas */}
            <g stroke={mapTheme === 'TACTICAL_RADAR' ? '#064E4B' : '#94A3B8'} strokeWidth="4" strokeLinecap="round" opacity="0.6">
              <line x1="40" y1="220" x2="460" y2="220" /> {/* Inner Ring Road */}
              <line x1="250" y1="30" x2="250" y2="410" /> {/* Hosur / Bellary Highway */}
              <line x1="60" y1="80" x2="440" y2="360" />  {/* Outer Ring Road Express */}
            </g>

            {/* City Landmark Labels */}
            <g fontSize="9" fontWeight="bold" fill={mapTheme === 'TACTICAL_RADAR' ? '#4FD1C5' : '#64748B'} opacity="0.7">
              <text x="50" y="70">HEBBAL</text>
              <text x="210" y="160">MG ROAD</text>
              <text x="270" y="130">INDIRANAGAR</text>
              <text x="250" y="270">KORAMANGALA</text>
              <text x="360" y="120">WHITEFIELD</text>
              <text x="270" y="380">ELECTRONIC CITY</text>
            </g>

            {/* Render Each Active Delivery: Route Lines & Moving Rider Vehicle */}
            {fleetPositions.map((f, i) => {
              const isSelected = selectedVehicleId === f.order.id;
              const routeColor = f.order.vehicleType === 'EV' ? '#10B981' : f.order.vehicleType === 'MINI_TRUCK' ? '#3B82F6' : '#F59E0B';

              return (
                <g key={f.order.id} className="cursor-pointer" onClick={() => setSelectedVehicleId(f.order.id)}>
                  {/* Active delivery transit polyline */}
                  <line
                    x1={f.startX}
                    y1={f.startY}
                    x2={f.endX}
                    y2={f.endY}
                    stroke={routeColor}
                    strokeWidth={isSelected ? '3' : '2'}
                    strokeDasharray={isSelected ? '4,4' : '3,3'}
                    opacity={isSelected ? 0.9 : 0.4}
                  />

                  {/* Pickup Pin */}
                  <circle cx={f.startX} cy={f.startY} r="4" fill="#10B981" />
                  {/* Drop Pin */}
                  <circle cx={f.endX} cy={f.endY} r="4" fill="#EF4444" />

                  {/* Vehicle Pulsing Wave (if selected or moving) */}
                  <circle
                    cx={f.currentX}
                    cy={f.currentY}
                    r={isSelected ? '18' : '12'}
                    fill="url(#vehiclePulse)"
                    className="animate-pulse"
                  />

                  {/* Vehicle Marker */}
                  <g transform={`translate(${f.currentX - 10}, ${f.currentY - 10})`}>
                    <rect
                      width="20"
                      height="20"
                      rx="6"
                      fill={isSelected ? '#059669' : '#042F2E'}
                      stroke={isSelected ? '#A7F3D0' : '#10B981'}
                      strokeWidth="2"
                    />
                    <text
                      x="10"
                      y="14"
                      textAnchor="middle"
                      fontSize="10"
                      fill="#FFFFFF"
                      fontWeight="bold"
                    >
                      {f.order.vehicleType === 'MINI_TRUCK' ? '🚚' : f.order.vehicleType === 'EV' ? '⚡' : '🏍️'}
                    </text>
                  </g>

                  {/* Label Pill */}
                  <rect
                    x={f.currentX - 24}
                    y={f.currentY - 22}
                    width="48"
                    height="12"
                    rx="3"
                    fill="#000000"
                    opacity="0.8"
                  />
                  <text
                    x={f.currentX}
                    y={f.currentY - 13}
                    textAnchor="middle"
                    fill="#A7F3D0"
                    fontSize="7"
                    fontWeight="bold"
                  >
                    #{f.order.orderCode}
                  </text>
                </g>
              );
            })}
          </svg>

          {/* Floating Radar Legend & Compass */}
          <div className="absolute top-3 left-3 bg-black/60 backdrop-blur-md px-3 py-2 rounded-xl text-white border border-white/10 text-[10px] space-y-1">
            <div className="flex items-center gap-1.5 font-bold text-[#A7F3D0]">
              <Radio className="w-3.5 h-3.5 animate-pulse text-[#10B981]" />
              <span>Bangalore Hyperlocal Dispatch Grid</span>
            </div>
            <div className="flex items-center gap-2 text-slate-300">
              <span className="flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-emerald-400" /> Pickup
              </span>
              <span className="flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-red-400" /> Drop
              </span>
              <span className="flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-amber-400" /> Moving
              </span>
            </div>
          </div>

          <div className="absolute bottom-3 right-3 bg-black/60 backdrop-blur-md px-2.5 py-1.5 rounded-xl text-white border border-white/10 text-[10px] font-mono flex items-center gap-1.5">
            <Compass className="w-3.5 h-3.5 text-[#10B981] animate-spin" />
            <span>N 12°58&apos;23&quot; E 77°35&apos;45&quot;</span>
          </div>

        </div>

        {/* Selected Vehicle Telemetry Drawer (Col 4) */}
        <div className="lg:col-span-4 bg-slate-50 border-t lg:border-t-0 lg:border-l border-slate-200 p-4 flex flex-col justify-between space-y-3">
          
          {selectedItem ? (
            <div className="space-y-3">
              <div className="flex items-start justify-between">
                <div>
                  <span className="px-2 py-0.5 rounded-md bg-[#042F2E] text-[#A7F3D0] font-mono font-black text-xs">
                    #{selectedItem.order.orderCode}
                  </span>
                  <div className="text-xs font-black text-slate-900 mt-1">
                    {selectedItem.order.assignedRiderName || 'Vikram Singh (Rider Partner)'}
                  </div>
                  <div className="text-[11px] text-slate-500">
                    Plate: <strong>{selectedItem.order.assignedRiderPlate || 'KA-01-EQ-4821'}</strong>
                  </div>
                </div>

                <span className="px-2 py-1 rounded-full bg-emerald-100 text-[#042F2E] border border-emerald-300 text-[10px] font-black uppercase">
                  {selectedItem.order.status.replace(/_/g, ' ')}
                </span>
              </div>

              {/* Real-time Telemetry Metrics */}
              <div className="grid grid-cols-2 gap-2 text-center">
                <div className="p-2 rounded-xl bg-white border border-slate-200">
                  <div className="text-[10px] font-bold text-slate-400 uppercase">Live Speed</div>
                  <div className="text-sm font-black text-[#059669] mt-0.5">
                    {selectedItem.speedKmh} km/h
                  </div>
                </div>
                <div className="p-2 rounded-xl bg-white border border-slate-200">
                  <div className="text-[10px] font-bold text-slate-400 uppercase">Battery &amp; Signal</div>
                  <div className="text-sm font-black text-slate-800 mt-0.5">
                    ⚡ {selectedItem.batteryPercent}% • 5G
                  </div>
                </div>
              </div>

              {/* Waypoint Addresses */}
              <div className="p-2.5 rounded-xl bg-white border border-slate-200 text-xs space-y-1.5">
                <div className="flex items-start gap-1.5 text-slate-700">
                  <MapPin className="w-3.5 h-3.5 text-emerald-600 shrink-0 mt-0.5" />
                  <div className="leading-tight">
                    <span className="text-[10px] font-bold text-emerald-700 block uppercase">Pickup</span>
                    {selectedItem.order.pickupAddress}
                  </div>
                </div>
                <div className="flex items-start gap-1.5 text-slate-700 pt-1 border-t border-slate-100">
                  <MapPin className="w-3.5 h-3.5 text-red-600 shrink-0 mt-0.5" />
                  <div className="leading-tight">
                    <span className="text-[10px] font-bold text-red-700 block uppercase">Drop</span>
                    {selectedItem.order.dropAddress}
                  </div>
                </div>
              </div>

              {/* Customer Info */}
              <div className="text-xs text-slate-600 bg-emerald-50/70 p-2 rounded-xl border border-emerald-200">
                <span>Customer: <strong>{selectedItem.order.customerName}</strong></span>
                <span className="block text-[10px] text-emerald-800 font-bold mt-0.5">
                  Fare: ₹{(selectedItem.order.totalAmountPaise / 100).toFixed(0)} • 80% Rider Share Allocated
                </span>
              </div>
            </div>
          ) : (
            <div className="p-6 text-center text-slate-400 text-xs space-y-2">
              <Crosshair className="w-8 h-8 text-slate-300 mx-auto" />
              <div>No active deliveries selected. Tap any vehicle on the map to inspect live radar telemetry.</div>
            </div>
          )}

          {/* Quick Actions */}
          {selectedItem && (
            <div className="pt-2 border-t border-slate-200 flex flex-wrap gap-1.5">
              {onSelectOrder && (
                <button
                  type="button"
                  onClick={() => onSelectOrder(selectedItem.order)}
                  className="flex-1 py-2 px-3 rounded-xl bg-[#042F2E] hover:bg-emerald-950 text-[#A7F3D0] text-xs font-extrabold flex items-center justify-center gap-1.5 cursor-pointer"
                >
                  <Eye className="w-3.5 h-3.5 text-[#10B981]" />
                  <span>Full Tracking Dossier</span>
                </button>
              )}
            </div>
          )}

        </div>

      </div>

    </div>
  );
};
