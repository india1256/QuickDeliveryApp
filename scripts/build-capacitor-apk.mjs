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
    apkFileName: 'QuickDelivery-Customer.apk',
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
    apkFileName: 'QuickDelivery-Rider-Partner.apk',
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
    apkFileName: 'QuickDelivery-Admin-Enterprise.apk',
    themeColor: '#0D9488',
    backgroundColor: '#022C22',
    startUrl: './index.html?app=admin',
  },
  hub: {
    role: '',
    appId: 'com.quickdelivery.hub',
    appName: 'Quick Delivery Hub',
    shortName: 'QD Hub',
    description: 'Quick Delivery 3-in-1 Unified Hub (Customer, Rider & Admin)',
    outDir: 'dist-hub',
    apkFileName: 'QuickDelivery-AllInOne-Hub.apk',
    themeColor: '#059669',
    backgroundColor: '#042F2E',
    startUrl: './index.html',
  },
};

const argTarget = (process.argv[2] || 'all').toLowerCase().trim();
const shouldSyncCapacitor = process.argv.includes('--sync') || process.argv.includes('--apk');
const shouldBuildApk = process.argv.includes('--apk');

function escapeXml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, "\\'");
}

function ensureCapacitorAndroidProject(target) {
  const androidDir = path.resolve(process.cwd(), 'android');
  if (!fs.existsSync(androidDir)) {
    console.log(`📦 Initializing Capacitor Android project via 'npx cap add android'...`);
    execSync(`npx cap add android`, { stdio: 'inherit' });
  }

  // Ensure Gradle wrapper uses a rock-solid public Gradle 8.7 distribution compatible with JDK 17 & 21
  const wrapperPropsPath = path.join(androidDir, 'gradle/wrapper/gradle-wrapper.properties');
  if (fs.existsSync(wrapperPropsPath)) {
    const props = fs.readFileSync(wrapperPropsPath, 'utf8');
    const updatedProps = props.replace(
      /distributionUrl=.*$/m,
      'distributionUrl=https\\://services.gradle.org/distributions/gradle-8.7-bin.zip'
    );
    fs.writeFileSync(wrapperPropsPath, updatedProps);
  }

  // Ensure gradlew is executable
  const gradlewPath = path.join(androidDir, 'gradlew');
  if (fs.existsSync(gradlewPath)) {
    fs.chmodSync(gradlewPath, 0o755);
  }

  // Update applicationId in android/app/build.gradle while preserving namespace for MainActivity.java
  const appBuildGradlePath = path.join(androidDir, 'app/build.gradle');
  if (fs.existsSync(appBuildGradlePath)) {
    let gradleContent = fs.readFileSync(appBuildGradlePath, 'utf8');
    gradleContent = gradleContent.replace(
      /applicationId\s+["'][^"']+["']/,
      `applicationId "${target.appId}"`
    );
    fs.writeFileSync(appBuildGradlePath, gradleContent);
  }

  // Update app_name, title_activity_main, package_name, and custom_url_scheme in strings.xml
  const stringsXmlPath = path.join(androidDir, 'app/src/main/res/values/strings.xml');
  if (fs.existsSync(stringsXmlPath)) {
    const safeName = escapeXml(target.appName);
    const safeId = escapeXml(target.appId);
    const stringsXml = `<?xml version='1.0' encoding='utf-8'?>
<resources>
    <string name="app_name">${safeName}</string>
    <string name="title_activity_main">${safeName}</string>
    <string name="package_name">${safeId}</string>
    <string name="custom_url_scheme">${safeId}</string>
</resources>
`;
    fs.writeFileSync(stringsXmlPath, stringsXml);
  }

  // Copy custom app icon into mipmap folders if public/icon-512.png exists
  const customIcon = path.resolve(process.cwd(), 'public/icon-512.png');
  if (fs.existsSync(customIcon)) {
    const resDir = path.join(androidDir, 'app/src/main/res');
    const mipmapDirs = [
      'mipmap-mdpi',
      'mipmap-hdpi',
      'mipmap-xhdpi',
      'mipmap-xxhdpi',
      'mipmap-xxxhdpi',
    ];
    for (const dir of mipmapDirs) {
      const targetMipmapDir = path.join(resDir, dir);
      if (fs.existsSync(targetMipmapDir)) {
        fs.copyFileSync(customIcon, path.join(targetMipmapDir, 'ic_launcher.png'));
        fs.copyFileSync(customIcon, path.join(targetMipmapDir, 'ic_launcher_round.png'));
        fs.copyFileSync(customIcon, path.join(targetMipmapDir, 'ic_launcher_foreground.png'));
      }
    }
  }
}

function buildSingleTarget(targetKey) {
  const target = TARGETS[targetKey];
  if (!target) {
    console.error(`Unknown target "${targetKey}". Valid targets: customer, rider, admin, hub, all`);
    process.exit(1);
  }

  console.log(`\n============================================================`);
  console.log(`🚀 Building [${target.appName}] (${target.appId})`);
  console.log(`   Role: VITE_APP_ROLE=${target.role || 'unified'} -> ${target.outDir}`);
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
    ensureCapacitorAndroidProject(target);
    console.log(`🔄 Syncing web bundle into Capacitor Android project for ${target.appName}...`);
    execSync(`npx cap copy android`, {
      stdio: 'inherit',
      env: {
        ...process.env,
        VITE_APP_ROLE: target.role,
      },
    });
  }

  if (shouldBuildApk) {
    const androidDir = path.resolve(process.cwd(), 'android');
    const distApksDir = path.resolve(process.cwd(), 'dist-apks');
    fs.mkdirSync(distApksDir, { recursive: true });

    console.log(`🔨 Compiling Android APK for ${target.appName} (${target.appId})...`);
    execSync(`./gradlew assembleDebug --no-daemon --stacktrace`, {
      cwd: androidDir,
      stdio: 'inherit',
    });

    const builtApkPath = path.join(androidDir, 'app/build/outputs/apk/debug/app-debug.apk');
    const outputApkPath = path.join(distApksDir, target.apkFileName);
    if (!fs.existsSync(builtApkPath)) {
      throw new Error(`Expected APK not found at ${builtApkPath}`);
    }
    fs.copyFileSync(builtApkPath, outputApkPath);
    const stats = fs.statSync(outputApkPath);
    console.log(
      `🎉 Successfully packaged ${target.apkFileName} (${(stats.size / (1024 * 1024)).toFixed(2)} MB) -> dist-apks/${target.apkFileName}`
    );
  }
}

if (argTarget === 'all') {
  buildSingleTarget('customer');
  buildSingleTarget('rider');
  buildSingleTarget('admin');
  if (shouldBuildApk) {
    buildSingleTarget('hub');
  } else {
    execSync(`npx vite build`, { stdio: 'inherit' });
  }
  console.log(`\n🎉 All standalone Capacitor bundles built successfully:`);
  console.log(`   1. Customer APK Bundle -> ./dist-customer (com.quickdelivery.customer)`);
  console.log(`   2. Rider APK Bundle    -> ./dist-rider    (com.quickdelivery.rider)`);
  console.log(`   3. Admin APK Bundle    -> ./dist-admin    (com.quickdelivery.admin)`);
} else {
  buildSingleTarget(argTarget);
}
