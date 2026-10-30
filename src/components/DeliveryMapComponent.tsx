import React, { useState, useEffect, useMemo, useRef } from 'react';
import {
  Navigation,
  MapPin,
  Compass,
  Crosshair,
  ExternalLink,
  Layers,
  RotateCw,
  Bike,
  Car,
  Clock,
  Zap,
  ShieldCheck,
  Radio,
  CheckCircle2,
  AlertCircle,
  Eye,
} from 'lucide-react';
import { gpsService, type RiderGpsLocation } from '../services/gpsTrackingService';

export interface DeliveryMapProps {
  orderId: string;
  orderCode: string;
  status: 'PENDING_ASSIGNMENT' | 'ASSIGNED' | 'PICKED_UP' | 'IN_TRANSIT' | 'DELIVERED' | string;
  pickupAddress: string;
  dropAddress: string;
  pickupTag?: string;
  dropTag?: string;
  vehicleType?: 'BIKE' | 'SCOOTER' | 'AUTO' | 'EV_VAN' | 'EV' | 'MINI_TRUCK' | string;
  riderName?: string;
  riderPhone?: string;
  riderPlate?: string;
  riderRating?: string;
  riderGps?: RiderGpsLocation | null;
  isRiderView?: boolean;
  compact?: boolean;
  className?: string;
}

export const DeliveryMapComponent: React.FC<DeliveryMapProps> = ({
  orderId,
  orderCode,
  status,
  pickupAddress,
  dropAddress,
  pickupTag = 'Pickup',
  dropTag = 'Drop',
  vehicleType = 'BIKE',
  riderName = 'Vikram Singh',
  riderPhone = '9811223344',
  riderPlate = 'KA-01-EQ-4821',
  riderRating = '4.9★',
  riderGps = null,
  isRiderView = false,
  compact = false,
  className = '',
}) => {
  // Map View Mode: 'STREET' | 'RADAR' | 'SATELLITE'
  const [mapTheme, setMapTheme] = useState<'STREET' | 'RADAR' | 'SATELLITE'>('STREET');
  const [zoomLevel, setZoomLevel] = useState<number>(1);
  const [isFollowingRider, setIsFollowingRider] = useState<boolean>(true);
  const [simulatedProgress, setSimulatedProgress] = useState<number>(() => {
    switch (status) {
      case 'PENDING_ASSIGNMENT':
        return 0;
      case 'ASSIGNED':
        return 0.15;
      case 'PICKED_UP':
        return 0.4;
      case 'IN_TRANSIT':
        return 0.65;
      case 'DELIVERED':
        return 1.0;
      default:
        return 0.5;
    }
  });

  const [liveSpeedKmh, setLiveSpeedKmh] = useState<number>(32);
  const [gpsAccuracyMeters, setGpsAccuracyMeters] = useState<number>(3);
  const [googleMapsEmbedError, setGoogleMapsEmbedError] = useState<boolean>(false);
  const [showNativeMapsPrompt, setShowNativeMapsPrompt] = useState<boolean>(false);
  const [lastRefreshedAt, setLastRefreshedAt] = useState<string>('Just now');
  const [liveGps, setLiveGps] = useState<RiderGpsLocation>(() => riderGps || gpsService.getCurrentLocation());

  // Subscribe to real-time GPS broadcasting service
  useEffect(() => {
    if (riderGps) {
      setLiveGps(riderGps);
      if (riderGps.speedKmh > 0) setLiveSpeedKmh(riderGps.speedKmh);
      if (riderGps.accuracyMeters > 0) setGpsAccuracyMeters(riderGps.accuracyMeters);
      return;
    }

    const unsub = gpsService.subscribe((loc) => {
      setLiveGps(loc);
      if (loc.speedKmh > 0) {
        setLiveSpeedKmh(loc.speedKmh);
      }
      if (loc.accuracyMeters > 0) {
        setGpsAccuracyMeters(loc.accuracyMeters);
      }
      setLastRefreshedAt('Live GPS • Just now');
    });

    return () => unsub();
  }, [riderGps]);

  // Animation frame loop for continuous smooth marker micro-movement
  useEffect(() => {
    let animId: number;
    let baseProgress = 0.5;

    switch (status) {
      case 'PENDING_ASSIGNMENT':
        baseProgress = 0.05;
        break;
      case 'ASSIGNED':
        baseProgress = 0.22;
        break;
      case 'PICKED_UP':
        baseProgress = 0.45;
        break;
      case 'IN_TRANSIT':
        baseProgress = 0.72;
        break;
      case 'DELIVERED':
        baseProgress = 1.0;
        break;
    }

    if (status === 'DELIVERED') {
      setSimulatedProgress(1.0);
      setLiveSpeedKmh(0);
      return;
    }

    const interval = setInterval(() => {
      // Gentle jitter & micro-progression to indicate active live GPS ping
      setSimulatedProgress((prev) => {
        const jitter = (Math.random() - 0.5) * 0.008;
        const target = Math.min(0.96, Math.max(0.04, baseProgress + jitter));
        return target;
      });

      // Realistic speed fluctuation if not broadcasting fixed speed
      if (!gpsService.isTracking()) {
        if (status === 'IN_TRANSIT') {
          setLiveSpeedKmh(Math.floor(28 + Math.random() * 14));
        } else if (status === 'ASSIGNED' || status === 'PICKED_UP') {
          setLiveSpeedKmh(Math.floor(20 + Math.random() * 10));
        } else {
          setLiveSpeedKmh(0);
        }
        setGpsAccuracyMeters(Math.floor(2 + Math.random() * 2));
      }
      setLastRefreshedAt('Live • 1s ago');
    }, 2400);

    return () => {
      clearInterval(interval);
      cancelAnimationFrame(animId);
    };
  }, [status]);

  // SVG Coordinates calculation along a bezier curve
  // Route goes from Pickup (P0) through Curve Handles (P1, P2) to Drop (P3)
  const routePoints = useMemo(() => {
    const p0 = { x: 45, y: 155 }; // Pickup (Bottom-Left)
    const p1 = { x: 120, y: 70 };
    const p2 = { x: 230, y: 140 };
    const p3 = { x: 335, y: 45 }; // Drop (Top-Right)
    return { p0, p1, p2, p3 };
  }, []);

  // Compute position of rider along cubic bezier curve: B(t)
  const riderPos = useMemo(() => {
    const t = simulatedProgress;
    const { p0, p1, p2, p3 } = routePoints;
    const u = 1 - t;
    const tt = t * t;
    const uu = u * u;
    const uuu = uu * u;
    const ttt = tt * t;

    const x = uuu * p0.x + 3 * uu * t * p1.x + 3 * u * tt * p2.x + ttt * p3.x;
    const y = uuu * p0.y + 3 * uu * t * p1.y + 3 * u * tt * p2.y + ttt * p3.y;

    // Heading tangent
    const dx = 3 * uu * (p1.x - p0.x) + 6 * u * t * (p2.x - p1.x) + 3 * tt * (p3.x - p2.x);
    const dy = 3 * uu * (p1.y - p0.y) + 6 * u * t * (p2.y - p1.y) + 3 * tt * (p3.y - p2.y);
    const angleDeg = (Math.atan2(dy, dx) * 180) / Math.PI;

    return { x, y, angle: angleDeg };
  }, [simulatedProgress, routePoints]);

  // Derived telemetry metrics
  const totalTripKm = 5.8;
  const remainingKm = useMemo(() => {
    if (status === 'DELIVERED') return 0;
    const rem = (totalTripKm * (1 - simulatedProgress)).toFixed(1);
    return Math.max(0.1, parseFloat(rem));
  }, [status, simulatedProgress]);

  const etaMinutes = useMemo(() => {
    if (status === 'DELIVERED') return 0;
    return Math.max(1, Math.round(remainingKm * 3.2));
  }, [status, remainingKm]);

  // Safe external navigation launcher (Works seamlessly in standalone PWA on Android & iOS)
  const handleOpenGoogleMapsNative = () => {
    const destination = encodeURIComponent(dropAddress || 'Bangalore, Karnataka');
    const origin = encodeURIComponent(pickupAddress || 'Indiranagar, Bangalore');
    // Standard Google Maps Universal URL schema supported by Android Intent & Mobile Safari/Chrome PWA
    const googleMapsUrl = `https://www.google.com/maps/dir/?api=1&origin=${origin}&destination=${destination}&travelmode=two_wheeler`;

    try {
      window.open(googleMapsUrl, '_blank', 'noopener,noreferrer');
    } catch {
      window.location.href = googleMapsUrl;
    }
  };

  return (
    <div
      className={`rounded-2xl border overflow-hidden shadow-md flex flex-col transition-all ${
        mapTheme === 'RADAR'
          ? 'bg-[#08131e] border-teal-900/80 text-white'
          : mapTheme === 'SATELLITE'
          ? 'bg-[#111922] border-slate-800 text-white'
          : 'bg-white border-slate-200 text-slate-900'
      } ${className}`}
    >
      {/* Top Map Header & Live Telemetry Pill Bar */}
      <div
        className={`px-3.5 py-2.5 border-b flex flex-wrap items-center justify-between gap-2 text-xs select-none ${
          mapTheme === 'RADAR'
            ? 'bg-[#0b1b2b] border-teal-900/60'
            : mapTheme === 'SATELLITE'
            ? 'bg-[#182330] border-slate-800'
            : 'bg-slate-50/95 border-slate-200'
        }`}
      >
        <div className="flex items-center gap-2">
          <div className="relative flex items-center justify-center">
            <span
              className={`w-2.5 h-2.5 rounded-full ${
                status === 'DELIVERED'
                  ? 'bg-emerald-500'
                  : 'bg-emerald-400 animate-ping absolute opacity-75'
              }`}
            />
            <span
              className={`w-2.5 h-2.5 rounded-full ${
                status === 'DELIVERED' ? 'bg-emerald-500' : 'bg-emerald-500'
              }`}
            />
          </div>

          <div className="flex items-center gap-1.5 flex-wrap">
            <span className="font-extrabold uppercase tracking-wider text-[11px] text-[#059669]">
              {isRiderView ? 'Rider Turn-by-Turn GPS Radar' : 'Live Order Tracking Map'}
            </span>
            <span
              className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                status === 'DELIVERED'
                  ? 'bg-emerald-100 text-emerald-800 border border-emerald-200'
                  : status === 'IN_TRANSIT'
                  ? 'bg-blue-100 text-blue-800 border border-blue-200'
                  : 'bg-amber-100 text-amber-900 border border-amber-200'
              }`}
            >
              {(status || 'PENDING_ASSIGNMENT').replace(/_/g, ' ')}
            </span>
            <span className="px-2 py-0.5 rounded-full text-[10px] font-mono font-bold bg-emerald-50 text-emerald-900 border border-emerald-300/80 flex items-center gap-1">
              <Radio className="w-2.5 h-2.5 text-emerald-600 animate-pulse" />
              {(liveGps?.lat ?? 12.9716).toFixed(4)}°N, {(liveGps?.lng ?? 77.5946).toFixed(4)}°E • {liveSpeedKmh ?? 0} km/h
            </span>
          </div>
        </div>

        {/* Action Controls: Theme Switcher & Native Google Maps Launch */}
        <div className="flex items-center gap-1.5 shrink-0">
          <div className="flex rounded-lg border border-slate-300 dark:border-slate-700 p-0.5 text-[10px] font-bold">
            <button
              type="button"
              onClick={() => setMapTheme('STREET')}
              className={`px-2 py-0.5 rounded transition ${
                mapTheme === 'STREET'
                  ? 'bg-[#059669] text-white shadow-xs'
                  : 'text-slate-600 dark:text-slate-300 hover:text-slate-900'
              }`}
              title="Street Vector GPS Mode"
            >
              Street
            </button>
            <button
              type="button"
              onClick={() => setMapTheme('RADAR')}
              className={`px-2 py-0.5 rounded transition ${
                mapTheme === 'RADAR'
                  ? 'bg-teal-600 text-white shadow-xs'
                  : 'text-slate-600 dark:text-slate-300 hover:text-slate-900'
              }`}
              title="Tactical Dark Radar Mode"
            >
              Radar
            </button>
            <button
              type="button"
              onClick={() => setMapTheme('SATELLITE')}
              className={`px-2 py-0.5 rounded transition ${
                mapTheme === 'SATELLITE'
                  ? 'bg-slate-700 text-white shadow-xs'
                  : 'text-slate-600 dark:text-slate-300 hover:text-slate-900'
              }`}
              title="Satellite City View"
            >
              Sat
            </button>
          </div>

          <button
            type="button"
            onClick={handleOpenGoogleMapsNative}
            className="px-2.5 py-1 rounded-lg bg-[#059669] hover:bg-emerald-600 text-white text-[11px] font-extrabold flex items-center gap-1 transition shadow-xs cursor-pointer active:scale-95"
            title="Open native Google Maps application on phone"
          >
            <ExternalLink className="w-3 h-3" />
            <span className="hidden sm:inline">Google Maps App</span>
            <span className="sm:hidden">Maps</span>
          </button>
        </div>
      </div>

      {/* Main Interactive Map Canvas (100% Mobile PWA Crash-Proof Vector Canvas) */}
      <div
        className={`relative w-full overflow-hidden select-none transition-colors ${
          compact ? 'h-48' : 'h-64 sm:h-72'
        } ${
          mapTheme === 'RADAR'
            ? 'bg-[#06121e]'
            : mapTheme === 'SATELLITE'
            ? 'bg-[#0d1620]'
            : 'bg-[#f0f5f5]'
        }`}
      >
        {/* Subtle Map Cartography Grid & Road Networks */}
        <svg
          viewBox="0 0 380 200"
          className="w-full h-full object-cover transition-transform duration-300"
          style={{ transform: `scale(${zoomLevel})` }}
          preserveAspectRatio="xMidYMid slice"
        >
          <defs>
            {/* Grid Pattern */}
            <pattern
              id={`map-grid-${orderId}`}
              width="24"
              height="24"
              patternUnits="userSpaceOnUse"
            >
              <path
                d="M 24 0 L 0 0 0 24"
                fill="none"
                stroke={
                  mapTheme === 'RADAR'
                    ? '#0e2b3d'
                    : mapTheme === 'SATELLITE'
                    ? '#162230'
                    : '#e2e8f0'
                }
                strokeWidth="0.8"
              />
            </pattern>

            {/* Glowing Polyline Gradient for Trip Route */}
            <linearGradient id={`route-gradient-${orderId}`} x1="0%" y1="100%" x2="100%" y2="0%">
              <stop offset="0%" stopColor="#10B981" />
              <stop offset="60%" stopColor="#059669" />
              <stop offset="100%" stopColor="#3B82F6" />
            </linearGradient>

            {/* Radar Pulsing Animation Filter */}
            <filter id="glow" x="-20%" y="-20%" width="140%" height="140%">
              <feGaussianBlur stdDeviation="2.5" result="blur" />
              <feComposite in="SourceGraphic" in2="blur" operator="over" />
            </filter>
          </defs>

          {/* Grid Background */}
          <rect width="380" height="200" fill={`url(#map-grid-${orderId})`} />

          {/* Background City Blocks & Secondary Road Network */}
          <g opacity={mapTheme === 'RADAR' ? '0.25' : mapTheme === 'SATELLITE' ? '0.35' : '0.45'}>
            {/* Secondary Street Arteries */}
            <path
              d="M 10 30 Q 90 40 180 25 T 370 20"
              fill="none"
              stroke={mapTheme === 'RADAR' ? '#14b8a6' : '#cbd5e1'}
              strokeWidth="3.5"
            />
            <path
              d="M 20 185 Q 140 160 220 180 T 360 170"
              fill="none"
              stroke={mapTheme === 'RADAR' ? '#14b8a6' : '#cbd5e1'}
              strokeWidth="3"
            />
            <path
              d="M 80 10 L 100 190"
              fill="none"
              stroke={mapTheme === 'RADAR' ? '#0d9488' : '#e2e8f0'}
              strokeWidth="2.5"
            />
            <path
              d="M 280 10 L 290 190"
              fill="none"
              stroke={mapTheme === 'RADAR' ? '#0d9488' : '#e2e8f0'}
              strokeWidth="2.5"
            />
            <path
              d="M 190 20 C 180 80, 210 130, 200 190"
              fill="none"
              stroke={mapTheme === 'RADAR' ? '#0d9488' : '#e2e8f0'}
              strokeWidth="2"
            />

            {/* Urban Sector Outlines / Neighborhoods */}
            <rect
              x="55"
              y="40"
              width="50"
              height="35"
              rx="4"
              fill={mapTheme === 'RADAR' ? '#092535' : '#f1f5f9'}
              stroke={mapTheme === 'RADAR' ? '#134e4a' : '#cbd5e1'}
              strokeWidth="1"
            />
            <rect
              x="130"
              y="90"
              width="70"
              height="45"
              rx="4"
              fill={mapTheme === 'RADAR' ? '#092535' : '#f1f5f9'}
              stroke={mapTheme === 'RADAR' ? '#134e4a' : '#cbd5e1'}
              strokeWidth="1"
            />
            <rect
              x="230"
              y="35"
              width="60"
              height="45"
              rx="4"
              fill={mapTheme === 'RADAR' ? '#092535' : '#f1f5f9'}
              stroke={mapTheme === 'RADAR' ? '#134e4a' : '#cbd5e1'}
              strokeWidth="1"
            />
            <rect
              x="270"
              y="110"
              width="75"
              height="40"
              rx="4"
              fill={mapTheme === 'RADAR' ? '#092535' : '#f1f5f9'}
              stroke={mapTheme === 'RADAR' ? '#134e4a' : '#cbd5e1'}
              strokeWidth="1"
            />
          </g>

          {/* PRIMARY HYPERLOCAL GPS ROUTE POLYLINE (Pickup -> Drop) */}
          <g>
            {/* Soft Route Shadow / Glow */}
            <path
              d={`M ${routePoints.p0.x} ${routePoints.p0.y} C ${routePoints.p1.x} ${routePoints.p1.y}, ${routePoints.p2.x} ${routePoints.p2.y}, ${routePoints.p3.x} ${routePoints.p3.y}`}
              fill="none"
              stroke={mapTheme === 'RADAR' ? '#059669' : '#10B981'}
              strokeWidth="9"
              strokeOpacity="0.22"
              strokeLinecap="round"
            />

            {/* Main Road Route Casing */}
            <path
              d={`M ${routePoints.p0.x} ${routePoints.p0.y} C ${routePoints.p1.x} ${routePoints.p1.y}, ${routePoints.p2.x} ${routePoints.p2.y}, ${routePoints.p3.x} ${routePoints.p3.y}`}
              fill="none"
              stroke={mapTheme === 'RADAR' ? '#042F2E' : '#ffffff'}
              strokeWidth="6"
              strokeLinecap="round"
            />

            {/* Inner Route Polyline with Live Navigation Dash */}
            <path
              d={`M ${routePoints.p0.x} ${routePoints.p0.y} C ${routePoints.p1.x} ${routePoints.p1.y}, ${routePoints.p2.x} ${routePoints.p2.y}, ${routePoints.p3.x} ${routePoints.p3.y}`}
              fill="none"
              stroke={`url(#route-gradient-${orderId})`}
              strokeWidth="4"
              strokeLinecap="round"
              strokeDasharray={status === 'DELIVERED' ? 'none' : '6 4'}
              className={status === 'DELIVERED' ? '' : 'animate-pulse'}
            />
          </g>

          {/* PICKUP POINT PIN (Green Tag) */}
          <g transform={`translate(${routePoints.p0.x}, ${routePoints.p0.y})`}>
            {/* Ground Radar Pulse */}
            <circle r="12" fill="#10B981" fillOpacity="0.2" />
            <circle r="6" fill="#10B981" stroke="#ffffff" strokeWidth="2" />
            <circle r="2.5" fill="#065F46" />

            {/* Pickup Label Badge */}
            <g transform="translate(-32, -26)">
              <rect
                width="64"
                height="18"
                rx="5"
                fill="#065F46"
                stroke="#A7F3D0"
                strokeWidth="1"
                filter="url(#glow)"
              />
              <text
                x="32"
                y="12"
                fill="#ffffff"
                fontSize="8.5"
                fontWeight="bold"
                textAnchor="middle"
              >
                📍 {(pickupTag || 'Pickup').slice(0, 10)}
              </text>
            </g>
          </g>

          {/* DROP POINT PIN (Red / Destination Flag) */}
          <g transform={`translate(${routePoints.p3.x}, ${routePoints.p3.y})`}>
            <circle r="14" fill="#EF4444" fillOpacity="0.2" className="animate-ping" />
            <circle r="7" fill="#EF4444" stroke="#ffffff" strokeWidth="2" />
            <circle r="3" fill="#ffffff" />

            {/* Drop Label Badge */}
            <g transform="translate(-32, -26)">
              <rect
                width="64"
                height="18"
                rx="5"
                fill="#991B1B"
                stroke="#FECACA"
                strokeWidth="1"
                filter="url(#glow)"
              />
              <text
                x="32"
                y="12"
                fill="#ffffff"
                fontSize="8.5"
                fontWeight="bold"
                textAnchor="middle"
              >
                🏁 {(dropTag || 'Drop').slice(0, 10)}
              </text>
            </g>
          </g>

          {/* LIVE ANIMATED RIDER / VEHICLE MARKER */}
          {status !== 'DELIVERED' && (
            <g
              transform={`translate(${riderPos.x}, ${riderPos.y})`}
              className="transition-transform duration-700 ease-out"
            >
              {/* Radar Beacon Wave Rings */}
              <circle r="22" fill="#059669" fillOpacity="0.12" className="animate-ping" />
              <circle r="14" fill="#059669" fillOpacity="0.25" />

              {/* Rider Vehicle Disk with Heading Rotation */}
              <g transform={`rotate(${riderPos.angle})`}>
                <circle
                  r="10"
                  fill="#042F2E"
                  stroke="#A7F3D0"
                  strokeWidth="2"
                  filter="url(#glow)"
                />
                {/* Direction Pointer Arrow */}
                <polygon points="0,-12 5,-7 -5,-7" fill="#10B981" />
              </g>

              {/* Vehicle Symbol Icon */}
              <g transform="translate(-6, -6)">
                <svg
                  width="12"
                  height="12"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="#ffffff"
                  strokeWidth="2.5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <circle cx="18.5" cy="17.5" r="3.5" />
                  <circle cx="5.5" cy="17.5" r="3.5" />
                  <circle cx="15" cy="5" r="1" />
                  <path d="M12 17.5V14l-3-3 4-3 2 3h2" />
                </svg>
              </g>

              {/* Floating Live Telemetry Badge over Rider */}
              <g transform="translate(-38, -26)">
                <rect
                  width="76"
                  height="16"
                  rx="4"
                  fill="#042F2E"
                  stroke="#10B981"
                  strokeWidth="1"
                />
                <text
                  x="38"
                  y="11"
                  fill="#A7F3D0"
                  fontSize="7.5"
                  fontWeight="bold"
                  textAnchor="middle"
                >
                  🛵 {(riderName || 'Rider').split(' ')[0]} • {liveSpeedKmh ?? 0} km/h
                </text>
              </g>
            </g>
          )}

          {/* DELIVERED COMPLETED BADGE AT DESTINATION */}
          {status === 'DELIVERED' && (
            <g transform={`translate(${routePoints.p3.x}, ${routePoints.p3.y - 36})`}>
              <rect
                x="-45"
                y="-10"
                width="90"
                height="22"
                rx="6"
                fill="#065F46"
                stroke="#A7F3D0"
                strokeWidth="1.5"
              />
              <text
                x="0"
                y="5"
                fill="#ffffff"
                fontSize="9"
                fontWeight="extrabold"
                textAnchor="middle"
              >
                ✓ PARCEL DELIVERED
              </text>
            </g>
          )}
        </svg>

        {/* Map Float Overlay: Zoom & Recenter Controls */}
        <div className="absolute right-3 top-3 flex flex-col gap-1.5 z-10">
          <button
            type="button"
            onClick={() => setZoomLevel((z) => Math.min(1.8, +(z + 0.2).toFixed(1)))}
            className="w-7 h-7 rounded-lg bg-white/90 dark:bg-slate-900/90 text-slate-800 dark:text-white font-extrabold text-sm flex items-center justify-center border border-slate-300 dark:border-slate-700 shadow-xs hover:bg-white active:scale-95 transition"
            title="Zoom In"
          >
            +
          </button>
          <button
            type="button"
            onClick={() => setZoomLevel((z) => Math.max(0.8, +(z - 0.2).toFixed(1)))}
            className="w-7 h-7 rounded-lg bg-white/90 dark:bg-slate-900/90 text-slate-800 dark:text-white font-extrabold text-sm flex items-center justify-center border border-slate-300 dark:border-slate-700 shadow-xs hover:bg-white active:scale-95 transition"
            title="Zoom Out"
          >
            −
          </button>
          <button
            type="button"
            onClick={() => {
              setZoomLevel(1);
              setIsFollowingRider(true);
            }}
            className="w-7 h-7 rounded-lg bg-white/90 dark:bg-slate-900/90 text-[#059669] font-extrabold text-xs flex items-center justify-center border border-slate-300 dark:border-slate-700 shadow-xs hover:bg-white active:scale-95 transition"
            title="Recenter Map View"
          >
            <Crosshair className="w-3.5 h-3.5" />
          </button>
        </div>

        {/* Bottom-Left Live GPS Telemetry Floating Badge */}
        <div className="absolute left-3 bottom-3 z-10 flex flex-wrap items-center gap-1.5">
          <div className="px-2.5 py-1 rounded-lg bg-slate-950/85 backdrop-blur-xs border border-white/20 text-white flex items-center gap-2 text-[10px] font-extrabold shadow-sm">
            <Radio className="w-3 h-3 text-emerald-400 animate-pulse" />
            <span>
              {status === 'DELIVERED'
                ? 'Route Complete • 0 km'
                : `${remainingKm} km remaining • ~${etaMinutes} mins ETA`}
            </span>
          </div>

          <div className="hidden sm:flex px-2 py-1 rounded-lg bg-emerald-950/85 backdrop-blur-xs border border-emerald-500/40 text-[#A7F3D0] text-[10px] font-mono font-bold items-center gap-1.5 shadow-sm">
            <Zap className="w-3 h-3 text-amber-400" />
            <span>{(liveGps?.lat ?? 12.9716).toFixed(4)}°, {(liveGps?.lng ?? 77.5946).toFixed(4)}° • ±{gpsAccuracyMeters ?? 3}m</span>
          </div>
        </div>
      </div>

      {/* Bottom Route Details & Standalone Mobile PWA Navigation Bar */}
      <div
        className={`p-3 border-t flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs ${
          mapTheme === 'RADAR'
            ? 'bg-[#081726] border-teal-900/60'
            : mapTheme === 'SATELLITE'
            ? 'bg-[#15202c] border-slate-800'
            : 'bg-slate-50/70 border-slate-200'
        }`}
      >
        {/* Addresses Summary */}
        <div className="space-y-1 min-w-0 flex-1">
          <div className="flex items-start gap-1.5 truncate">
            <span className="w-2 h-2 rounded-full bg-emerald-500 mt-1 shrink-0" />
            <span className="text-[11px] font-medium truncate">
              <strong className="font-extrabold">{pickupTag}:</strong> {pickupAddress}
            </span>
          </div>
          <div className="flex items-start gap-1.5 truncate">
            <span className="w-2 h-2 rounded-full bg-red-500 mt-1 shrink-0" />
            <span className="text-[11px] font-medium truncate">
              <strong className="font-extrabold">{dropTag}:</strong> {dropAddress}
            </span>
          </div>
        </div>

        {/* Live Status & Quick External App Launcher */}
        <div className="flex items-center gap-2 shrink-0">
          <div className="text-right hidden sm:block">
            <div className="text-[10px] uppercase font-bold text-slate-400">Rider Partner</div>
            <div className="text-xs font-extrabold text-[#059669]">
              {riderName} ({riderPlate})
            </div>
          </div>

          <button
            type="button"
            onClick={handleOpenGoogleMapsNative}
            className="w-full sm:w-auto px-3 py-2 rounded-xl bg-gradient-to-r from-[#042F2E] to-[#059669] hover:from-[#064e3b] hover:to-emerald-600 text-white font-extrabold text-xs flex items-center justify-center gap-1.5 shadow-sm active:scale-95 transition cursor-pointer"
          >
            <Navigation className="w-3.5 h-3.5 text-[#A7F3D0]" />
            <span>Turn-by-Turn GPS (Google Maps)</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default DeliveryMapComponent;
