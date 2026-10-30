import type { CapacitorConfig } from '@capacitor/cli';

type AppTargetRole = 'customer' | 'rider' | 'admin';

const rawRole = (process.env.VITE_APP_ROLE || 'customer').toLowerCase().trim();
const targetRole: AppTargetRole =
  rawRole === 'rider' || rawRole === 'partner'
    ? 'rider'
    : rawRole === 'admin' || rawRole === 'enterprise'
    ? 'admin'
    : 'customer';

const ROLE_CONFIGS: Record<
  AppTargetRole,
  { appId: string; appName: string; webDir: string; backgroundColor: string }
> = {
  customer: {
    appId: 'com.quickdelivery.customer',
    appName: 'Quick Delivery',
    webDir: 'dist-customer',
    backgroundColor: '#042F2E',
  },
  rider: {
    appId: 'com.quickdelivery.rider',
    appName: 'Quick Delivery Partner',
    webDir: 'dist-rider',
    backgroundColor: '#0F172A',
  },
  admin: {
    appId: 'com.quickdelivery.admin',
    appName: 'Quick Delivery Admin',
    webDir: 'dist-admin',
    backgroundColor: '#022C22',
  },
};

const activeTarget = ROLE_CONFIGS[targetRole];

const config: CapacitorConfig = {
  appId: activeTarget.appId,
  appName: activeTarget.appName,
  webDir: activeTarget.webDir,
  bundledWebRuntime: false,
  backgroundColor: activeTarget.backgroundColor,
  android: {
    allowMixedContent: true,
    captureInput: true,
    webContentsDebuggingEnabled: true,
  },
  server: {
    androidScheme: 'https',
    cleartext: true,
  },
};

export default config;
