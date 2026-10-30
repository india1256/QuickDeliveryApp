#!/usr/bin/env node
import { execSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

const TARGETS = {
  customer: {
    role: 'customer',
    appId: 'com.quickdelivery.customer',
    appName: 'Quick Delivery',
    shortName: 'QuickDelivery',
    description: 'Quick Delivery Customer App - Hyperlocal Parcel Booking & Live Tracking',
    outDir: 'dist-customer',
    themeColor: '#059669',
    backgroundColor: '#042F2E',
    startUrl: './index.html?app=customer',
  },
  rider: {
    role: 'rider',
    appId: 'com.quickdelivery.rider',
    appName: 'Quick Delivery Partner',
    shortName: 'QD Partner',
    description: 'Quick Delivery Partner App - Fleet Dispatch, Live Radar & 80% Earnings',
    outDir: 'dist-rider',
    themeColor: '#D97706',
    backgroundColor: '#0F172A',
    startUrl: './index.html?app=rider',
  },
  admin: {
    role: 'admin',
    appId: 'com.quickdelivery.admin',
    appName: 'Quick Delivery Admin',
    shortName: 'QD Admin',
    description: 'Quick Delivery Admin Enterprise - Real-Time Order & Fleet Control Center',
    outDir: 'dist-admin',
    themeColor: '#0D9488',
    backgroundColor: '#022C22',
    startUrl: './index.html?app=admin',
  },
};

const argTarget = (process.argv[2] || 'all').toLowerCase().trim();
const shouldSyncCapacitor = process.argv.includes('--sync');

function buildSingleTarget(targetKey) {
  const target = TARGETS[targetKey];
  if (!target) {
    console.error(`Unknown target "${targetKey}". Valid targets: customer, rider, admin, all`);
    process.exit(1);
  }

  console.log(`\n============================================================`);
  console.log(`🚀 Building [${target.appName}] (${target.appId})`);
  console.log(`   Role: VITE_APP_ROLE=${target.role} -> ${target.outDir}`);
  console.log(`============================================================`);

  // 1. Run Vite build with role-locked environment variables
  execSync(`npx vite build`, {
    stdio: 'inherit',
    env: {
      ...process.env,
      VITE_APP_ROLE: target.role,
      VITE_OUT_DIR: target.outDir,
    },
  });

  // 2. Customize manifest.json in target output directory for PWABuilder & Capacitor
  const outPath = path.resolve(process.cwd(), target.outDir);
  const manifestPath = path.join(outPath, 'manifest.json');
  if (fs.existsSync(manifestPath)) {
    try {
      const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
      manifest.id = target.appId;
      manifest.name = target.appName;
      manifest.short_name = target.shortName;
      manifest.description = target.description;
      manifest.start_url = target.startUrl;
      manifest.theme_color = target.themeColor;
      manifest.background_color = target.backgroundColor;
      manifest.display = 'standalone';
      fs.writeFileSync(manifestPath, JSON.stringify(manifest, null, 2));
    } catch (err) {
      console.warn(`Warning: Could not update ${manifestPath}:`, err);
    }
  }

  // 3. Customize <title> in index.html of the target output directory
  const indexHtmlPath = path.join(outPath, 'index.html');
  if (fs.existsSync(indexHtmlPath)) {
    const html = fs.readFileSync(indexHtmlPath, 'utf8');
    const updatedHtml = html.replace(
      /<title>.*?<\/title>/i,
      `<title>${target.appName}</title>`
    );
    fs.writeFileSync(indexHtmlPath, updatedHtml);
  }

  // 4. Write active capacitor.config.json for this target
  const capConfig = {
    appId: target.appId,
    appName: target.appName,
    webDir: target.outDir,
    bundledWebRuntime: false,
    backgroundColor: target.backgroundColor,
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

  fs.writeFileSync(
    path.resolve(process.cwd(), 'capacitor.config.json'),
    JSON.stringify(capConfig, null, 2)
  );

  // 5. Mirror into default dist/ directory so standard Capacitor/web tools always see the latest target
  const defaultDist = path.resolve(process.cwd(), 'dist');
  fs.rmSync(defaultDist, { recursive: true, force: true });
  fs.cpSync(outPath, defaultDist, { recursive: true });

  console.log(`✅ Built ${target.appName} into ./${target.outDir} (and synced to ./dist)`);
  console.log(`✅ Updated capacitor.config.json -> appId: "${target.appId}", webDir: "${target.outDir}"`);

  if (shouldSyncCapacitor) {
    const capAndroidAssets = path.resolve(process.cwd(), 'android/app/src/main/assets');
    if (fs.existsSync(capAndroidAssets)) {
      console.log(`🔄 Running npx cap sync android for ${target.appName}...`);
      execSync(`npx cap sync android`, {
        stdio: 'inherit',
        env: {
          ...process.env,
          VITE_APP_ROLE: target.role,
        },
      });
    } else {
      console.log(
        `ℹ️ Capacitor Android shell not yet initialized with 'npx cap add android'. Ready for 'npx cap add android && npx cap sync android'.`
      );
    }
  }
}

if (argTarget === 'all') {
  buildSingleTarget('customer');
  buildSingleTarget('rider');
  buildSingleTarget('admin');
  // Re-build default unified dist so the multi-role hub also remains available in ./dist
  execSync(`npx vite build`, { stdio: 'inherit' });
  console.log(`\n🎉 All 3 standalone Capacitor bundles built successfully:`);
  console.log(`   1. Customer APK Bundle -> ./dist-customer (com.quickdelivery.customer)`);
  console.log(`   2. Rider APK Bundle    -> ./dist-rider    (com.quickdelivery.rider)`);
  console.log(`   3. Admin APK Bundle    -> ./dist-admin    (com.quickdelivery.admin)`);
} else {
  buildSingleTarget(argTarget);
}
