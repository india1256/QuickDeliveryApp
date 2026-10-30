export interface RiderGpsLocation {
  lat: number;
  lng: number;
  speedKmh: number;
  headingDeg: number;
  accuracyMeters: number;
  timestamp: number;
  isSimulated: boolean;
}

type LocationCallback = (location: RiderGpsLocation) => void;

class GpsTrackingService {
  private watchId: number | null = null;
  private simulationInterval: number | null = null;
  private isBroadcasting: boolean = false;
  private currentCoord: RiderGpsLocation = {
    lat: 12.9716, // Bangalore Central
    lng: 77.5946,
    speedKmh: 32,
    headingDeg: 45,
    accuracyMeters: 3,
    timestamp: Date.now(),
    isSimulated: true,
  };
  private listeners: Set<LocationCallback> = new Set();

  constructor() {
    // Check saved state
    try {
      const saved = localStorage.getItem('quick_delivery_rider_gps');
      if (saved) {
        this.currentCoord = JSON.parse(saved);
      }
    } catch {}
  }

  public subscribe(cb: LocationCallback): () => void {
    this.listeners.add(cb);
    cb(this.currentCoord);
    return () => {
      this.listeners.delete(cb);
    };
  }

  private notify() {
    this.listeners.forEach((cb) => cb(this.currentCoord));
    try {
      localStorage.setItem('quick_delivery_rider_gps', JSON.stringify(this.currentCoord));
    } catch {}
  }

  public getCurrentLocation(): RiderGpsLocation {
    return this.currentCoord;
  }

  public isTracking(): boolean {
    return this.isBroadcasting;
  }

  // Start real device GPS broadcast or fallback simulation
  public startBroadcasting(options?: { onLocationUpdate?: (loc: RiderGpsLocation) => void }): boolean {
    if (this.isBroadcasting) return true;
    this.isBroadcasting = true;

    // 1. Try native browser Geolocation API
    if (typeof navigator !== 'undefined' && 'geolocation' in navigator) {
      try {
        this.watchId = navigator.geolocation.watchPosition(
          (pos) => {
            const speed = pos.coords.speed ? Math.round(pos.coords.speed * 3.6) : Math.floor(25 + Math.random() * 15);
            const heading = pos.coords.heading || (this.currentCoord.headingDeg + 5) % 360;
            this.currentCoord = {
              lat: pos.coords.latitude,
              lng: pos.coords.longitude,
              speedKmh: speed,
              headingDeg: heading,
              accuracyMeters: Math.round(pos.coords.accuracy || 4),
              timestamp: pos.timestamp,
              isSimulated: false,
            };
            this.notify();
            if (options?.onLocationUpdate) options.onLocationUpdate(this.currentCoord);
          },
          (err) => {
            console.warn('Physical GPS watch note (using autonomous route telemetry):', err.message);
            this.startAutonomousSimulation(options?.onLocationUpdate);
          },
          {
            enableHighAccuracy: true,
            maximumAge: 1000,
            timeout: 5000,
          }
        );
      } catch (e) {
        this.startAutonomousSimulation(options?.onLocationUpdate);
      }
    } else {
      this.startAutonomousSimulation(options?.onLocationUpdate);
    }

    return true;
  }

  // Realistic autonomous route simulation along Bangalore delivery corridors
  private startAutonomousSimulation(onUpdate?: (loc: RiderGpsLocation) => void) {
    if (this.simulationInterval) return;

    let step = 0;
    // Route from Indiranagar (12.9784, 77.6408) to Koramangala (12.9352, 77.6245)
    const baseLat = 12.9784;
    const baseLng = 77.6408;
    const destLat = 12.9352;
    const destLng = 77.6245;

    this.simulationInterval = window.setInterval(() => {
      step = (step + 1) % 100;
      const progress = step / 100;

      const currentLat = baseLat + (destLat - baseLat) * progress + (Math.sin(step / 5) * 0.001);
      const currentLng = baseLng + (destLng - baseLng) * progress + (Math.cos(step / 5) * 0.001);
      const speed = Math.floor(26 + Math.random() * 16);
      const heading = (Math.atan2(destLng - baseLng, destLat - baseLat) * 180) / Math.PI;

      this.currentCoord = {
        lat: parseFloat(currentLat.toFixed(6)),
        lng: parseFloat(currentLng.toFixed(6)),
        speedKmh: speed,
        headingDeg: Math.round((heading + 360) % 360),
        accuracyMeters: Math.floor(2 + Math.random() * 2),
        timestamp: Date.now(),
        isSimulated: true,
      };

      this.notify();
      if (onUpdate) onUpdate(this.currentCoord);
    }, 2000);
  }

  public stopBroadcasting() {
    this.isBroadcasting = false;
    if (this.watchId !== null && typeof navigator !== 'undefined' && 'geolocation' in navigator) {
      navigator.geolocation.clearWatch(this.watchId);
      this.watchId = null;
    }
    if (this.simulationInterval !== null) {
      clearInterval(this.simulationInterval);
      this.simulationInterval = null;
    }
    this.currentCoord.speedKmh = 0;
    this.notify();
  }
}

export const gpsService = new GpsTrackingService();
