import { gpsService } from './gpsTrackingService.ts';

export interface BatteryStatus {
  levelPercent: number;
  isCharging: boolean;
  isBatterySaverActive: boolean;
  isGpsThrottled: boolean;
  gpsUpdateIntervalMs: number;
  gpsFrequencyLabel: string;
  isSimulated: boolean;
  criticalThresholdPercent: number;
}

type BatterySubscriber = (status: BatteryStatus) => void;

export class BatteryMonitoringService {
  public static readonly CRITICAL_THRESHOLD_PERCENT = 20;
  public static readonly NORMAL_GPS_INTERVAL_MS = 2000;
  public static readonly THROTTLED_GPS_INTERVAL_MS = 10000;

  private currentStatus: BatteryStatus = {
    levelPercent: 100,
    isCharging: false,
    isBatterySaverActive: false,
    isGpsThrottled: false,
    gpsUpdateIntervalMs: BatteryMonitoringService.NORMAL_GPS_INTERVAL_MS,
    gpsFrequencyLabel: 'Standard Real-Time (2s)',
    isSimulated: false,
    criticalThresholdPercent: BatteryMonitoringService.CRITICAL_THRESHOLD_PERCENT,
  };

  private subscribers: Set<BatterySubscriber> = new Set();
  private batteryManager: any = null;

  constructor() {
    this.initSystemBatteryListener();
  }

  private async initSystemBatteryListener() {
    if (typeof navigator !== 'undefined' && 'getBattery' in (navigator as any)) {
      try {
        const bm = await (navigator as any).getBattery();
        this.batteryManager = bm;
        this.updateFromBatteryManager(bm);

        bm.addEventListener('levelchange', () => {
          if (!this.currentStatus.isSimulated) {
            this.updateFromBatteryManager(bm);
          }
        });

        bm.addEventListener('chargingchange', () => {
          if (!this.currentStatus.isSimulated) {
            this.updateFromBatteryManager(bm);
          }
        });
      } catch (e) {
        // Fallback gracefully on devices/browsers that do not expose Battery API
      }
    }
  }

  private updateFromBatteryManager(bm: any) {
    const level = Math.round((bm.level ?? 1) * 100);
    const charging = Boolean(bm.charging);
    this.applyBatteryState(level, charging, false);
  }

  private computeStatus(
    levelPercent: number,
    isCharging: boolean,
    isSimulated: boolean
  ): BatteryStatus {
    const clampedLevel = Math.max(1, Math.min(100, Math.round(levelPercent)));
    // Battery saver activates when battery level drops below 20% and device is not charging
    const isBatterySaverActive = clampedLevel < BatteryMonitoringService.CRITICAL_THRESHOLD_PERCENT && !isCharging;
    const isGpsThrottled = isBatterySaverActive;
    const gpsUpdateIntervalMs = isGpsThrottled
      ? BatteryMonitoringService.THROTTLED_GPS_INTERVAL_MS
      : BatteryMonitoringService.NORMAL_GPS_INTERVAL_MS;
    const gpsFrequencyLabel = isGpsThrottled
      ? 'Eco Battery-Saver (10s)'
      : 'Standard Real-Time (2s)';

    return {
      levelPercent: clampedLevel,
      isCharging,
      isBatterySaverActive,
      isGpsThrottled,
      gpsUpdateIntervalMs,
      gpsFrequencyLabel,
      isSimulated,
      criticalThresholdPercent: BatteryMonitoringService.CRITICAL_THRESHOLD_PERCENT,
    };
  }

  private applyBatteryState(levelPercent: number, isCharging: boolean, isSimulated: boolean): BatteryStatus {
    const newStatus = this.computeStatus(levelPercent, isCharging, isSimulated);
    this.currentStatus = newStatus;

    // Automatically adapt GPS update frequency to conserve battery uptime
    gpsService.setUpdateFrequency(newStatus.gpsUpdateIntervalMs, newStatus.isGpsThrottled);

    this.notify();
    return newStatus;
  }

  private notify() {
    this.subscribers.forEach((cb) => {
      try {
        cb(this.currentStatus);
      } catch (err) {
        console.error('Battery subscriber callback error:', err);
      }
    });
  }

  public getStatus(): BatteryStatus {
    return this.currentStatus;
  }

  public subscribe(cb: BatterySubscriber): () => void {
    this.subscribers.add(cb);
    cb(this.currentStatus);
    return () => {
      this.subscribers.delete(cb);
    };
  }

  public simulateBatteryLevel(percent: number, isCharging: boolean = false): BatteryStatus {
    return this.applyBatteryState(percent, isCharging, true);
  }

  public resetToSystem(): BatteryStatus {
    if (this.batteryManager) {
      return this.applyBatteryState(
        Math.round((this.batteryManager.level ?? 1) * 100),
        Boolean(this.batteryManager.charging),
        false
      );
    }
    return this.applyBatteryState(100, false, false);
  }
}

export const batteryMonitoringService = new BatteryMonitoringService();
