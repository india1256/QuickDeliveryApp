import { getApps } from 'firebase/app';
import { dbService } from './databaseService';

export interface DiagnosticResult {
  isFirebaseAppInitialized: boolean;
  isFirestoreConnected: boolean;
  firestoreProjectId: string;
  serverSyncEndpoint: string;
  serverHealthStatus: 'ONLINE' | 'WAKING_UP' | 'OFFLINE';
  serverLatencyMs: number;
  activeUserSession: string;
  checkedAtIso: string;
  recommendations: string[];
}

export class FirebaseDiagnosticService {
  public static async runDiagnostics(): Promise<DiagnosticResult> {
    const apps = getApps();
    const isFirebaseAppInitialized = apps.length > 0;
    const config = dbService.getSavedConfig();
    const isFirestoreConnected = dbService.isConnected();
    const firestoreProjectId = config?.projectId || 'Not Configured';

    let serverHealthStatus: 'ONLINE' | 'WAKING_UP' | 'OFFLINE' = 'OFFLINE';
    let serverLatencyMs = 0;
    let serverSyncEndpoint = '/api/live-sync';
    const recommendations: string[] = [];

    const startMs = Date.now();
    try {
      const resp = await fetch('/api/health', {
        method: 'GET',
        headers: { Accept: 'application/json' },
        signal: AbortSignal.timeout(5000),
      });
      serverLatencyMs = Date.now() - startMs;
      if (resp.ok) {
        const json = await resp.json();
        if (json.ok) {
          serverHealthStatus = 'ONLINE';
        } else {
          serverHealthStatus = 'WAKING_UP';
        }
      } else {
        serverHealthStatus = 'WAKING_UP';
      }
    } catch {
      serverHealthStatus = 'OFFLINE';
      serverLatencyMs = 0;
    }

    let activeUserSession = 'Guest / Unauthenticated';
    try {
      const authRaw = localStorage.getItem('quick_delivery_auth_user');
      if (authRaw) {
        const parsed = JSON.parse(authRaw);
        if (parsed && parsed.email) {
          activeUserSession = `${parsed.role || 'USER'}: ${parsed.email}`;
        }
      }
    } catch {}

    if (!isFirebaseAppInitialized) {
      recommendations.push('Firebase SDK is not initialized. Check firebase configuration.');
    }
    if (!isFirestoreConnected) {
      recommendations.push('Firestore database reference is inactive. Local storage fallback is active.');
    }
    if (serverHealthStatus === 'OFFLINE') {
      recommendations.push('Live sync server is offline or unreachable. Check network connection and server status.');
    } else if (serverHealthStatus === 'WAKING_UP') {
      recommendations.push('Cloud server is waking up from idle state. Retrying in a few seconds...');
    }

    if (recommendations.length === 0) {
      recommendations.push('All diagnostic checks passed successfully! Real-time sync is fully operational.');
    }

    return {
      isFirebaseAppInitialized,
      isFirestoreConnected,
      firestoreProjectId,
      serverSyncEndpoint,
      serverHealthStatus,
      serverLatencyMs,
      activeUserSession,
      checkedAtIso: new Date().toLocaleTimeString(),
      recommendations,
    };
  }
}
