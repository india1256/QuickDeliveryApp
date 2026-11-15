import http from 'node:http';
import https from 'node:https';
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { execSync, spawn } from 'node:child_process';

const PORT = Number(process.env.APP_PORT || 3000);
const ROOT = process.cwd();
const SYNC_STATE_FILE = '/tmp/quickdelivery-live-sync.json';
const BACKUP_DIR = path.join(ROOT, 'data', 'backups');
const CLOUD_RUN_MAX_SAFE_APK_BYTES = 33000000; // < 31.5 MiB (well below Cloud Run's 32 MiB = 33,554,432 limit)

const APK_FILE_NAMES = [
  'QuickDelivery-Customer.apk',
  'QuickDelivery-Rider-Partner.apk',
  'QuickDelivery-Admin-Enterprise.apk',
];

function isValidStandaloneApk(filePath) {
  try {
    if (!fs.existsSync(filePath)) return false;
    const sz = fs.statSync(filePath).size;
    return sz > 1000000 && sz < CLOUD_RUN_MAX_SAFE_APK_BYTES;
  } catch {
    return false;
  }
}

function resolveSourceApkPath(requestedFileName = '') {
  const candidates = [
    requestedFileName ? path.join('/var/www/assets/apks', requestedFileName) : '',
    requestedFileName ? path.join(ROOT, 'public', 'apks', requestedFileName) : '',
  ].filter(Boolean);

  for (const candidate of candidates) {
    if (isValidStandaloneApk(candidate)) {
      return candidate;
    }
  }
  return null;
}

function getApkSizeBytes(fileName = 'QuickDelivery-Customer.apk') {
  const apkPath = resolveSourceApkPath(fileName);
  if (!apkPath) return 32175104;
  try {
    return fs.statSync(apkPath).size;
  } catch {
    return 32175104;
  }
}

function getApkSizeMb(fileName = 'QuickDelivery-Customer.apk') {
  const bytes = getApkSizeBytes(fileName);
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

const DEMO_ORDER_IDS = new Set([
  'QD-7001',
  'QD-6204',
  'QD-5910',
  'QD-5402',
  'ORD-1001',
  'ORD-1002',
]);

function loadSyncState() {
  try {
    if (fs.existsSync(SYNC_STATE_FILE)) {
      const parsed = JSON.parse(fs.readFileSync(SYNC_STATE_FILE, 'utf-8'));
      if (parsed && typeof parsed === 'object') {
        const cleanOrders = {};
        for (const [k, v] of Object.entries(parsed.ordersById || {})) {
          if (!DEMO_ORDER_IDS.has(k)) {
            cleanOrders[k] = v;
          }
        }
        return {
          resetEpochMs: Number(parsed.resetEpochMs || 0),
          ordersById: cleanOrders,
          devicesById: parsed.devicesById || {},
          fcmTokensByDevice: parsed.fcmTokensByDevice || {},
          riderRegistrationsByKey: parsed.riderRegistrationsByKey || {},
          promoCouponsByCode: parsed.promoCouponsByCode || {},
          withdrawalRequestsById: parsed.withdrawalRequestsById || {},
          supportTicketsById: parsed.supportTicketsById || {},
          remoteFeatureFlags: parsed.remoteFeatureFlags || null,
          chatMessagesById: parsed.chatMessagesById || {},
          waitTimerStartByOrder: parsed.waitTimerStartByOrder || {},
          riderLiveCoordsByOrder: parsed.riderLiveCoordsByOrder || {},
          codSettlementsById: parsed.codSettlementsById || {},
          disputeRecordsById: parsed.disputeRecordsById || {},
          riderDocExpiriesById: parsed.riderDocExpiriesById || {},
          paymentsByOrder: parsed.paymentsByOrder || {},
        };
      }
    }
  } catch {}
  return {
    resetEpochMs: Date.now(),
    ordersById: {},
    devicesById: {},
    fcmTokensByDevice: {},
    riderRegistrationsByKey: {},
    promoCouponsByCode: {},
    withdrawalRequestsById: {},
    supportTicketsById: {},
    remoteFeatureFlags: null,
    chatMessagesById: {},
    waitTimerStartByOrder: {},
    riderLiveCoordsByOrder: {},
    codSettlementsById: {},
    disputeRecordsById: {},
    riderDocExpiriesById: {},
    paymentsByOrder: {},
  };
}

let syncState = loadSyncState();
let lastDailyBackupWriteMs = 0;

function writeDailyBackupSnapshot(force = false) {
  const now = Date.now();
  if (!force && now - lastDailyBackupWriteMs < 15000) return;
  lastDailyBackupWriteMs = now;
  try {
    fs.mkdirSync(BACKUP_DIR, { recursive: true });
    const dayStr = new Date(now).toISOString().slice(0, 10);
    const outPath = path.join(BACKUP_DIR, `qd-backup-${dayStr}.json`);
    fs.writeFileSync(outPath, JSON.stringify(syncState, null, 2), 'utf-8');
  } catch {}
}

function syncWithMongoDataApiIfConfigured() {
  const dataApiUrl = process.env.MONGODB_DATA_API_URL || '';
  const dataApiKey = process.env.MONGODB_DATA_API_KEY || '';
  if (!dataApiUrl || !dataApiKey) return;
  try {
    const u = new URL(dataApiUrl);
    const payload = JSON.stringify({
      collection: 'quickdelivery_snapshots',
      database: process.env.MONGODB_DB_NAME || 'quickdelivery',
      dataSource: process.env.MONGODB_CLUSTER || 'Cluster0',
      filter: { _id: 'live_state_v1' },
      update: { $set: { state: syncState, updatedAt: new Date().toISOString() } },
      upsert: true,
    });
    const req = https.request(
      {
        hostname: u.hostname,
        port: u.port || 443,
        path: u.pathname + (u.search || ''),
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'api-key': dataApiKey,
          'Content-Length': Buffer.byteLength(payload),
        },
      },
      res => res.resume()
    );
    req.on('error', () => {});
    req.write(payload);
    req.end();
  } catch {}
}

function sendFcmPushNotifications({ tokens, title, body, data = {} }) {
  const fcmServerKey = process.env.FCM_SERVER_KEY || '';
  if (!fcmServerKey || !Array.isArray(tokens) || tokens.length === 0) return;
  try {
    const cleanTokens = tokens.filter(t => typeof t === 'string' && t.length > 20 && !t.startsWith('fcm_qd_'));
    if (cleanTokens.length === 0) return;
    const payload = JSON.stringify({
      registration_ids: cleanTokens,
      priority: 'high',
      notification: { title, body, sound: 'default' },
      data,
    });
    const req = https.request(
      {
        hostname: 'fcm.googleapis.com',
        path: '/fcm/send',
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `key=${fcmServerKey}`,
          'Content-Length': Buffer.byteLength(payload),
        },
      },
      res => res.resume()
    );
    req.on('error', () => {});
    req.write(payload);
    req.end();
  } catch {}
}

function saveSyncState() {
  try {
    fs.writeFileSync(SYNC_STATE_FILE, JSON.stringify(syncState), 'utf-8');
    writeDailyBackupSnapshot(false);
    syncWithMongoDataApiIfConfigured();
  } catch {}
}

function renderDownloadPageHtml(autoDownloadFile = '', singleRoleFilter = '') {
  const safeAuto = autoDownloadFile.replace(/[^A-Za-z0-9._-]/g, '');
  const sizeMb = getApkSizeMb(safeAuto || 'QuickDelivery-Customer.apk');

  const roleUpper = String(singleRoleFilter || '').toUpperCase();
  const showCustomer = !roleUpper || roleUpper === 'CUSTOMER';
  const showRider = !roleUpper || roleUpper === 'RIDER';
  const showAdmin = !roleUpper || roleUpper === 'ADMIN';

  const pageHeading =
    roleUpper === 'CUSTOMER'
      ? 'Quick Delivery — Customer App'
      : roleUpper === 'RIDER'
      ? 'Partner Quick Delivery — Rider App'
      : roleUpper === 'ADMIN'
      ? 'Quick Delivery Admin — Console App'
      : 'Quick Delivery — Standalone App Downloads (1 Link = 1 App)';

  const customerCardHtml = showCustomer
    ? `
      <div class="apk-card" style="border-color: #10B981;">
        <div class="apk-title" style="color: #6EE7B7;">Quick Delivery (Customer App) • ${getApkSizeMb('QuickDelivery-Customer.apk')}</div>
        <div class="apk-desc">Installs ONLY the <b>Quick Delivery</b> customer booking app. Instant parcel booking, Auto-Capture exact address, UPI/COD checkout, GST receipts &amp; live tracking.</div>
        <a class="dl-btn" style="background: #059669;" href="/dl/QuickDelivery-Customer.apk" download="QuickDelivery-Customer.apk" onclick="onDirectApkClick('QuickDelivery-Customer.apk')">
          Download Quick Delivery (Customer APK)
        </a>
        <a class="direct-link" href="/dl/QuickDelivery-Customer.apk" download="QuickDelivery-Customer.apk">
          Direct Download: /dl/QuickDelivery-Customer.apk
        </a>
      </div>`
    : '';

  const riderCardHtml = showRider
    ? `
      <div class="apk-card" style="border-color: #F59E0B;">
        <div class="apk-title" style="color: #FCD34D;">Partner Quick Delivery (Rider App) • ${getApkSizeMb('QuickDelivery-Rider-Partner.apk')}</div>
        <div class="apk-desc">Installs ONLY the <b>Partner Quick Delivery</b> app. Accept/Reject delivery requests, OTP-verified pickup/drop, same-day earnings &amp; partner wallet.</div>
        <a class="dl-btn" style="background: #D97706;" href="/dl/QuickDelivery-Rider-Partner.apk" download="QuickDelivery-Rider-Partner.apk" onclick="onDirectApkClick('QuickDelivery-Rider-Partner.apk')">
          Download Partner Quick Delivery (Rider APK)
        </a>
        <a class="direct-link" href="/dl/QuickDelivery-Rider-Partner.apk" download="QuickDelivery-Rider-Partner.apk">
          Direct Download: /dl/QuickDelivery-Rider-Partner.apk
        </a>
      </div>`
    : '';

  const adminCardHtml = showAdmin
    ? `
      <div class="apk-card" style="border-color: #14B8A6;">
        <div class="apk-title" style="color: #5EEAD4;">Quick Delivery Admin (Operations Console) • ${getApkSizeMb('QuickDelivery-Admin-Enterprise.apk')}</div>
        <div class="apk-desc">Installs ONLY the <b>Quick Delivery Admin</b> console. Live fleet GPS monitoring, order dispatch oversight, commission settlement &amp; SOS alerts.</div>
        <a class="dl-btn" style="background: #0D9488;" href="/dl/QuickDelivery-Admin-Enterprise.apk" download="QuickDelivery-Admin-Enterprise.apk" onclick="onDirectApkClick('QuickDelivery-Admin-Enterprise.apk')">
          Download Quick Delivery Admin (Admin APK)
        </a>
        <a class="direct-link" href="/dl/QuickDelivery-Admin-Enterprise.apk" download="QuickDelivery-Admin-Enterprise.apk">
          Direct Download: /dl/QuickDelivery-Admin-Enterprise.apk
        </a>
      </div>`
    : '';

  return `<!DOCTYPE html>
<html lang="hi">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>${pageHeading}</title>
  <style>
    * { box-sizing: border-box; }
    body {
      margin: 0;
      padding: 14px;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      background: #022C22;
      color: #F8FAFC;
    }
    .container {
      max-width: 680px;
      margin: 0 auto;
    }
    .header-box {
      background: linear-gradient(135deg, #065F46, #042F2E);
      border: 2px solid #34D399;
      border-radius: 18px;
      padding: 16px;
      margin-bottom: 14px;
      box-shadow: 0 10px 25px rgba(0,0,0,0.35);
    }
    .badge {
      display: inline-block;
      background: #10B981;
      color: #022C22;
      font-weight: 900;
      font-size: 11px;
      padding: 4px 10px;
      border-radius: 999px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    h1 {
      margin: 8px 0 6px 0;
      font-size: 19px;
      color: #ECFDF5;
    }
    p {
      margin: 0;
      font-size: 13px;
      color: #A7F3D0;
      line-height: 1.45;
    }
    #progress-panel {
      display: none;
      background: #0F172A;
      border: 2px solid #10B981;
      border-radius: 16px;
      padding: 16px;
      margin-bottom: 16px;
      box-shadow: 0 8px 24px rgba(0,0,0,0.4);
    }
    .prog-title {
      font-size: 15px;
      font-weight: 800;
      color: #34D399;
      margin-bottom: 8px;
    }
    .prog-bar-bg {
      width: 100%;
      height: 16px;
      background: #1E293B;
      border-radius: 999px;
      overflow: hidden;
      margin-bottom: 8px;
      border: 1px solid #334155;
    }
    .prog-bar-fill {
      height: 100%;
      width: 100%;
      background: linear-gradient(90deg, #10B981, #38BDF8);
      transition: width 0.2s ease;
    }
    .prog-stats {
      font-size: 12px;
      color: #E2E8F0;
      font-weight: 700;
      line-height: 1.45;
    }
    .grid {
      display: grid;
      grid-template-columns: 1fr;
      gap: 12px;
      margin-bottom: 16px;
    }
    .apk-card {
      background: #0F172A;
      border: 1.5px solid #1E293B;
      border-radius: 16px;
      padding: 15px;
      box-shadow: 0 4px 12px rgba(0,0,0,0.25);
    }
    .apk-title {
      font-size: 16px;
      font-weight: 800;
      margin-bottom: 4px;
    }
    .apk-desc {
      font-size: 12px;
      color: #94A3B8;
      margin-bottom: 12px;
      line-height: 1.4;
    }
    .dl-btn {
      display: block;
      width: 100%;
      text-align: center;
      padding: 14px;
      border-radius: 12px;
      font-weight: 900;
      font-size: 14px;
      text-decoration: none;
      color: #FFFFFF;
      border: none;
      cursor: pointer;
      box-shadow: 0 4px 10px rgba(0,0,0,0.3);
      margin-bottom: 8px;
    }
    .direct-link {
      display: block;
      text-align: center;
      font-size: 11px;
      color: #93C5FD;
      text-decoration: underline;
    }
    .guide-box {
      background: #0F172A;
      border: 1.5px solid #34D399;
      border-radius: 16px;
      padding: 16px;
      margin-bottom: 16px;
    }
    .guide-box h2 {
      margin: 0 0 8px 0;
      font-size: 15px;
      color: #6EE7B7;
    }
    .step-list {
      margin: 0;
      padding-left: 18px;
      font-size: 13px;
      color: #E2E8F0;
      line-height: 1.55;
    }
    .live-box {
      background: #042F2E;
      border: 1.5px solid #10B981;
      border-radius: 16px;
      padding: 16px;
    }
    .live-box h2 {
      margin: 0 0 8px 0;
      font-size: 15px;
      color: #A7F3D0;
    }
  </style>
</head>
<body>
  <div class="container">
    <div class="header-box">
      <span class="badge">Direct Instant APK Download • 1 Link = 1 Single App</span>
      <h1>${pageHeading}</h1>
      <p>Ab koi "Connecting to server" ya waiting nahi! Niche green button dabayein ya auto-download start hone dein — <b>Chrome seedha .apk file (${sizeMb}) download karega</b>.</p>
    </div>

    <div id="progress-panel">
      <div class="prog-title" id="prog-title">✅ Direct APK Download Started!</div>
      <div class="prog-bar-bg">
        <div class="prog-bar-fill" id="prog-fill"></div>
      </div>
      <div class="prog-stats" id="prog-stats">Chrome notification / Downloads me APK download ho raha hai...</div>
    </div>

    <div class="grid">
      ${customerCardHtml}
      ${riderCardHtml}
      ${adminCardHtml}
    </div>

    <div class="guide-box">
      <h2>✨ v6.1.3 Production Enterprise &amp; Quick Commerce Upgrade</h2>
      <ol class="step-list">
        <li><b>Customer App:</b> Porter-style Box Capacity Guide (Bike/Scooter/Auto dimensions) + Loading Helper (+₹60), Swiggy/Zomato Animated Route Polyline &amp; "Arriving in X mins" Map, 1-Tap Receiver Live Tracking &amp; Drop OTP Share, and Instant Order Dispute Filing.</li>
        <li><b>Rider Partner App:</b> Smart Same-Route Order Batching (+₹25 Clubbing Bonus), Customer Helper Request badge, GPS+Time Photo Watermark, and Instant UPI/IMPS vs Weekly Auto-Payout selector.</li>
        <li><b>Admin Console:</b> Live Zone Supply-vs-Demand Alert + 1-Tap Surge Trigger, Rider COD Cash Settlement Ledger, Dispute &amp; Instant Wallet Refund Desk, Rider DL/RC/Insurance Expiry Tracker, and Peak-Hour Auto-Surge Scheduler.</li>
      </ol>
    </div>

    <div class="live-box">
      <h2>Live Dispatch &amp; Fleet Sync Monitor</h2>
      <div id="live-sync-summary" style="font-size: 13px; color: #ECFDF5;">
        Connecting to live dispatch status...
      </div>
    </div>

    <div style="margin-top: 16px; text-align: center; font-size: 12px; color: #A7F3D0;">
      <a href="/privacy-policy" style="color: #6EE7B7; margin: 0 8px;">Privacy Policy</a> •
      <a href="/terms" style="color: #6EE7B7; margin: 0 8px;">Terms of Service</a> •
      <a href="/delete-account" style="color: #6EE7B7; margin: 0 8px;">Delete Account &amp; Data</a> •
      <a href="/api/backup-export" style="color: #6EE7B7; margin: 0 8px;">Cloud Backup (.json)</a>
    </div>
  </div>

  <script>
    const AUTO_DOWNLOAD_FILE = ${JSON.stringify(safeAuto)};

    function onDirectApkClick(fileName) {
      const panel = document.getElementById('progress-panel');
      const titleEl = document.getElementById('prog-title');
      const statsEl = document.getElementById('prog-stats');
      if (panel && titleEl && statsEl) {
        panel.style.display = 'block';
        titleEl.textContent = 'Downloading ' + fileName + ' (' + '${sizeMb}' + ')...';
        statsEl.textContent = 'Check your Chrome notification bar or Downloads folder — ' + fileName + ' is downloading directly.';
      }
    }

    if (AUTO_DOWNLOAD_FILE) {
      window.addEventListener('DOMContentLoaded', () => {
        onDirectApkClick(AUTO_DOWNLOAD_FILE);
        setTimeout(() => {
          window.location.href = '/dl/' + AUTO_DOWNLOAD_FILE;
        }, 150);
      });
    }

    async function refreshLiveSync() {
      try {
        const res = await fetch('/api/live-sync');
        if (!res.ok) return;
        const data = await res.json();
        const devices = data.connectedDevices || [];
        const orders = data.orders || [];
        const el = document.getElementById('live-sync-summary');
        if (!el) return;
        const devHtml = devices.length
          ? devices.map(d => '<b>' + d.role + '</b> (' + d.deviceModel + ')').join(' &nbsp;|&nbsp; ')
          : 'System Online — Ready for Customer, Rider Partner & Admin connections.';
        const ordHtml = orders.length
          ? '<ul style="margin:8px 0 0 0;padding-left:18px;">' + orders.map(o =>
              '<li><b>' + o.orderId + '</b> — ' + o.orderStatus + ' | ' + o.totalAmount + '</li>'
            ).join('') + '</ul>'
          : '<div style="margin-top:6px;color:#A7F3D0;">No active orders in queue.</div>';
        el.innerHTML = '<div><b>Connected Devices (' + devices.length + '):</b> ' + devHtml + '</div>' + ordHtml;
      } catch (e) {}
    }
    setInterval(refreshLiveSync, 3000);
    refreshLiveSync();
  </script>
</body>
</html>`;
}

function renderLegalPageHtml(kind = 'privacy') {
  const isTerms = kind === 'terms';
  const isDelete = kind === 'delete';
  const title = isDelete
    ? 'Quick Delivery — Account & Data Deletion Request'
    : isTerms
    ? 'Quick Delivery — Terms of Service'
    : 'Quick Delivery — Privacy Policy';

  const bodyContent = isDelete
    ? `
      <h1>Account &amp; Data Deletion</h1>
      <p><b>Applies to:</b> Quick Delivery, Partner Quick Delivery, and Quick Delivery Admin (com.aistudio.quickdelivery.*)</p>
      <div class="card">
        <h2>Option 1: Instant In-App Deletion</h2>
        <ol>
          <li>Open <b>Quick Delivery</b> (Customer) or <b>Partner Quick Delivery</b> (Rider).</li>
          <li>Open the top-left <b>Menu (☰)</b> drawer.</li>
          <li>Tap <b>Delete Account &amp; Data</b> and confirm.</li>
          <li>All local profile information, authentication tokens, and session backups are permanently erased immediately.</li>
        </ol>
      </div>
      <div class="card">
        <h2>Option 2: Web &amp; Server Data Deletion Request</h2>
        <p>To request permanent erasure of any cloud order logs or partner records stored on our dispatch servers, email <a href="mailto:ravindergupta141@gmail.com?subject=Quick%20Delivery%20Account%20Deletion%20Request">ravindergupta141@gmail.com</a> from your registered email address with your 10-digit mobile number. Verified requests are processed within 7 business days.</p>
      </div>`
    : isTerms
    ? `
      <h1>Terms of Service — Quick Delivery Suite</h1>
      <p><b>Effective Date:</b> November 2026 • <b>Contact:</b> ravindergupta141@gmail.com</p>
      <div class="card">
        <h2>1. Mandatory Consent, Account Isolation &amp; Permissions</h2>
        <p>Users must read and explicitly accept the Privacy Policy &amp; Terms before signing up. Each customer account is strictly isolated by its 10-digit mobile number or verified email so order history, live tracking, and saved addresses remain private to that account. Location, Camera, and Notification permissions are required post-login to operate parcel pickup, GPS tracking, and proof-of-delivery features.</p>
        <h2>2. Parcel Booking &amp; Prohibited Items</h2>
        <p>Quick Delivery provides on-demand local parcel pickup and drop-off services (maximum 20 kg per shipment). Prohibited, illegal, flammable, hazardous, stolen, or restricted goods are strictly banned; the sender bears full legal responsibility for declared contents.</p>
        <h2>3. OTP Handover, Wait-Time &amp; Cancellation</h2>
        <p>Orders require 4-digit Pickup and Drop OTP + digital signature verification. Pre-pickup cancellations after partner dispatch and pickup wait time beyond the free grace window may incur policy fees when enabled by Admin.</p>
        <h2>4. Partner Safety, KYC &amp; Fair Settlement</h2>
        <p>Delivery partners receive 80% of base trip fare plus 100% of customer tips. Partners must maintain valid KYC documents, wear helmets/safety gear, obey traffic laws, and upload genuine camera delivery proofs.</p>
      </div>`
    : `
      <h1>Privacy Policy — Quick Delivery Suite</h1>
      <p><b>Apps Covered:</b> Quick Delivery (Customer), Partner Quick Delivery (Rider), and Quick Delivery Admin<br /><b>Developer / Data Controller:</b> Quick Delivery Logistics (<a href="mailto:ravindergupta141@gmail.com">ravindergupta141@gmail.com</a>)<br /><b>Effective Date:</b> November 2026</p>
      <div class="card">
        <h2>1. Account-Isolated Information We Collect</h2>
        <ul>
          <li><b>Account &amp; Partner Identity:</b> Name, 10-digit mobile number, email address, and (for Riders) KYC documents strictly isolated per account for booking history, GST receipts, and payouts.</li>
          <li><b>Foreground Location (Required Post-Login):</b> Approximate and precise GPS coordinates collected only while booking, actively navigating, or tracking an active order. No background spying occurs when offline.</li>
          <li><b>Camera (Required Post-Login, User-Initiated):</b> Parcel pickup/drop proof photos and Rider selfie check-ins captured only when triggered by you. Zero broad storage permissions are requested.</li>
          <li><b>Notifications (Required Post-Login):</b> Push and local tray alerts strictly for order status updates, OTPs, and live Rider-Customer chat.</li>
        </ul>
        <h2>2. How We Use &amp; Protect Data</h2>
        <ul>
          <li>Used solely to calculate transparent delivery fares, verify OTP &amp; digital signature handovers, display live rider ETA, generate GST/Earning PDFs, and verify online payments.</li>
          <li>All traffic in transit is encrypted over HTTPS (TLS) with HMAC-SHA256 fare and session integrity sealing.</li>
          <li>We never sell personal data or share it with third-party advertising networks.</li>
        </ul>
        <h2>3. Data Retention &amp; Account Deletion</h2>
        <p>You can permanently delete your account and local profile anytime inside the app via <b>Menu → Delete Account &amp; Data</b>, or review web deletion instructions at <a href="/delete-account">/delete-account</a>.</p>
      </div>`;

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>${title}</title>
  <style>
    body { margin: 0; padding: 20px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #042F2E; color: #F8FAFC; line-height: 1.6; }
    .wrap { max-width: 720px; margin: 0 auto; }
    .card { background: #0F172A; border: 1px solid #1E293B; border-radius: 14px; padding: 18px; margin-top: 14px; }
    h1 { color: #6EE7B7; font-size: 22px; margin-bottom: 6px; }
    h2 { color: #38BDF8; font-size: 16px; margin: 12px 0 6px; }
    a { color: #6EE7B7; }
    .nav { margin-top: 18px; font-size: 13px; }
  </style>
</head>
<body>
  <div class="wrap">
    ${bodyContent}
    <div class="nav">
      <a href="/download">App Downloads</a> •
      <a href="/privacy-policy">Privacy Policy</a> •
      <a href="/terms">Terms of Service</a> •
      <a href="/delete-account">Delete Account</a>
    </div>
  </div>
</body>
</html>`;
}

function configureNginxDirectApkServing() {
  try {
    const luaPath = '/etc/nginx/user_auth_verification.lua';
    if (fs.existsSync(luaPath)) {
      const luaContent = fs.readFileSync(luaPath, 'utf-8');
      if (!luaContent.startsWith('if true then return end')) {
        fs.writeFileSync(luaPath, `if true then return end\n${luaContent}`, 'utf-8');
      }
    }

    if (fs.existsSync('/var/www/assets')) {
      const apkDir = '/var/www/assets/apks';
      fs.mkdirSync(apkDir, { recursive: true });
      for (const fileName of APK_FILE_NAMES) {
        const pubApk = path.join(ROOT, 'public', 'apks', fileName);
        const dest = path.join(apkDir, fileName);
        if (isValidStandaloneApk(pubApk)) {
          try {
            const srcStat = fs.statSync(pubApk);
            let needsCopy = true;
            if (isValidStandaloneApk(dest)) {
              const destStat = fs.statSync(dest);
              if (destStat.size === srcStat.size) {
                needsCopy = false;
              }
            }
            if (needsCopy) {
              fs.copyFileSync(pubApk, dest);
              fs.chmodSync(dest, 0o644);
            }
          } catch {}
        }
      }
    }

    const htmlAll = renderDownloadPageHtml('', '');
    const htmlCustomer = renderDownloadPageHtml('QuickDelivery-Customer.apk', 'CUSTOMER');
    const htmlRider = renderDownloadPageHtml('QuickDelivery-Rider-Partner.apk', 'RIDER');
    const htmlAdmin = renderDownloadPageHtml('QuickDelivery-Admin-Enterprise.apk', 'ADMIN');
    const htmlPrivacy = renderLegalPageHtml('privacy');
    const htmlTerms = renderLegalPageHtml('terms');
    const htmlDelete = renderLegalPageHtml('delete');

    if (fs.existsSync('/var/www/assets')) {
      fs.writeFileSync('/var/www/assets/warmup.html', htmlAll, 'utf-8');
      fs.writeFileSync('/var/www/assets/customer-apk.html', htmlCustomer, 'utf-8');
      fs.writeFileSync('/var/www/assets/rider-apk.html', htmlRider, 'utf-8');
      fs.writeFileSync('/var/www/assets/admin-apk.html', htmlAdmin, 'utf-8');
      fs.writeFileSync('/var/www/assets/privacy-policy.html', htmlPrivacy, 'utf-8');
      fs.writeFileSync('/var/www/assets/terms.html', htmlTerms, 'utf-8');
      fs.writeFileSync('/var/www/assets/delete-account.html', htmlDelete, 'utf-8');
    }
    try {
      fs.mkdirSync(path.join(ROOT, 'public'), { recursive: true });
      fs.writeFileSync(path.join(ROOT, 'public', 'apk-download.html'), htmlAll, 'utf-8');
      fs.writeFileSync(path.join(ROOT, 'public', 'customer-apk.html'), htmlCustomer, 'utf-8');
      fs.writeFileSync(path.join(ROOT, 'public', 'rider-apk.html'), htmlRider, 'utf-8');
      fs.writeFileSync(path.join(ROOT, 'public', 'admin-apk.html'), htmlAdmin, 'utf-8');
      fs.writeFileSync(path.join(ROOT, 'public', 'privacy-policy.html'), htmlPrivacy, 'utf-8');
      fs.writeFileSync(path.join(ROOT, 'public', 'terms.html'), htmlTerms, 'utf-8');
      fs.writeFileSync(path.join(ROOT, 'public', 'delete-account.html'), htmlDelete, 'utf-8');
    } catch {}

    const apkBlock = `
        location = /dl/QuickDelivery-Customer.apk {
            alias /var/www/assets/apks/QuickDelivery-Customer.apk;
            types { }
            default_type application/vnd.android.package-archive;
            add_header Content-Disposition 'attachment; filename="QuickDelivery-Customer.apk"';
            add_header Cache-Control "no-cache";
            add_header Access-Control-Allow-Origin "*";
            add_header Access-Control-Expose-Headers "Content-Length, Content-Range, Accept-Ranges";
        }

        location = /dl/QuickDelivery-Rider-Partner.apk {
            alias /var/www/assets/apks/QuickDelivery-Rider-Partner.apk;
            types { }
            default_type application/vnd.android.package-archive;
            add_header Content-Disposition 'attachment; filename="QuickDelivery-Rider-Partner.apk"';
            add_header Cache-Control "no-cache";
            add_header Access-Control-Allow-Origin "*";
            add_header Access-Control-Expose-Headers "Content-Length, Content-Range, Accept-Ranges";
        }

        location = /dl/QuickDelivery-Admin-Enterprise.apk {
            alias /var/www/assets/apks/QuickDelivery-Admin-Enterprise.apk;
            types { }
            default_type application/vnd.android.package-archive;
            add_header Content-Disposition 'attachment; filename="QuickDelivery-Admin-Enterprise.apk"';
            add_header Cache-Control "no-cache";
            add_header Access-Control-Allow-Origin "*";
            add_header Access-Control-Expose-Headers "Content-Length, Content-Range, Accept-Ranges";
        }

        location = /customer-apk {
            alias /var/www/assets/customer-apk.html;
            default_type text/html;
            add_header Cache-Control "no-store, no-cache, must-revalidate";
        }

        location = /rider-apk {
            alias /var/www/assets/rider-apk.html;
            default_type text/html;
            add_header Cache-Control "no-store, no-cache, must-revalidate";
        }

        location = /admin-apk {
            alias /var/www/assets/admin-apk.html;
            default_type text/html;
            add_header Cache-Control "no-store, no-cache, must-revalidate";
        }

        location = /privacy-policy {
            alias /var/www/assets/privacy-policy.html;
            default_type text/html;
            add_header Cache-Control "no-store, no-cache, must-revalidate";
        }

        location = /terms {
            alias /var/www/assets/terms.html;
            default_type text/html;
            add_header Cache-Control "no-store, no-cache, must-revalidate";
        }

        location = /delete-account {
            alias /var/www/assets/delete-account.html;
            default_type text/html;
            add_header Cache-Control "no-store, no-cache, must-revalidate";
        }

        location /apks/ {
            root /var/www/assets;
            types { }
            default_type application/vnd.android.package-archive;
            add_header Content-Disposition "attachment";
            add_header Cache-Control "no-cache";
            add_header Access-Control-Allow-Origin "*";
            add_header Access-Control-Expose-Headers "Content-Length, Content-Range, Accept-Ranges";
        }

        location /api/live-sync {
            proxy_pass http://localhost:3000/api/live-sync;
            proxy_set_header Host localhost:3000;
            add_header Access-Control-Allow-Origin "*";
            add_header Access-Control-Allow-Methods "GET, POST, OPTIONS";
            add_header Access-Control-Allow-Headers "Content-Type, Accept";
        }

        location = /api/backup-export {
            proxy_pass http://localhost:3000/api/backup-export;
            proxy_set_header Host localhost:3000;
            add_header Access-Control-Allow-Origin "*";
        }

        location /api/payments/ {
            proxy_pass http://localhost:3000/api/payments/;
            proxy_set_header Host localhost:3000;
            add_header Access-Control-Allow-Origin "*";
            add_header Access-Control-Allow-Methods "GET, POST, OPTIONS";
            add_header Access-Control-Allow-Headers "Content-Type, Accept, x-razorpay-signature";
        }

        location = /download {
            alias /var/www/assets/warmup.html;
            default_type text/html;
            add_header Cache-Control "no-store, no-cache, must-revalidate";
        }
`;
    for (const confPath of ['/etc/nginx/nginx.conf', '/etc/nginx/nginx.conf.template']) {
      if (fs.existsSync(confPath)) {
        let content = fs.readFileSync(confPath, 'utf-8');
        content = content.replace(/location = \/dl\/QuickDelivery-Customer\.apk \{[\s\S]*?location = \/download \{[\s\S]*?\}/g, '');
        content = content.replace(/location \/apks\/ \{[\s\S]*?location = \/download \{[\s\S]*?\}/g, '');
        if (!content.includes('location = /dl/QuickDelivery-Customer.apk')) {
          content = content.replace(
            'location /warmup.html {',
            `${apkBlock}\n        location /warmup.html {`
          );
        }
        fs.writeFileSync(confPath, content, 'utf-8');
      }
    }
    try {
      execSync('nginx -s reload 2>/dev/null || true');
    } catch {}
  } catch {}
}

function ensureStandaloneRoleApksBuilt() {
  const customerApk = path.join('/var/www/assets/apks', 'QuickDelivery-Customer.apk');
  const riderApk = path.join('/var/www/assets/apks', 'QuickDelivery-Rider-Partner.apk');
  const adminApk = path.join('/var/www/assets/apks', 'QuickDelivery-Admin-Enterprise.apk');
  if (isValidStandaloneApk(customerApk) && isValidStandaloneApk(riderApk) && isValidStandaloneApk(adminApk)) {
    return;
  }
  const pubCust = path.join(ROOT, 'public', 'apks', 'QuickDelivery-Customer.apk');
  const pubRider = path.join(ROOT, 'public', 'apks', 'QuickDelivery-Rider-Partner.apk');
  const pubAdmin = path.join(ROOT, 'public', 'apks', 'QuickDelivery-Admin-Enterprise.apk');
  if (isValidStandaloneApk(pubCust) && isValidStandaloneApk(pubRider) && isValidStandaloneApk(pubAdmin)) {
    configureNginxDirectApkServing();
    return;
  }
  const child = spawn('python3', ['scripts/build-standalone-role-apks.py'], {
    cwd: ROOT,
    stdio: 'ignore',
  });
  child.on('exit', () => {
    configureNginxDirectApkServing();
  });
}

configureNginxDirectApkServing();
ensureStandaloneRoleApksBuilt();

if (process.argv.includes('--configure-nginx')) {
  process.exit(0);
}

function buildSyncResponsePayload() {
  const now = Date.now();
  const connectedDevices = [];
  for (const [devId, devInfo] of Object.entries(syncState.devicesById)) {
    if (now - Number(devInfo.lastSeenEpochMs || 0) <= 30000) {
      connectedDevices.push(devInfo);
    } else {
      delete syncState.devicesById[devId];
    }
  }
  return {
    ok: true,
    resetEpochMs: syncState.resetEpochMs,
    connectedDevices,
    orders: Object.values(syncState.ordersById),
    riderRegistrations: Object.values(syncState.riderRegistrationsByKey || {}),
    promoCoupons: Object.values(syncState.promoCouponsByCode || {}),
    withdrawalRequests: Object.values(syncState.withdrawalRequestsById || {}),
    supportTickets: Object.values(syncState.supportTicketsById || {}),
    codSettlements: Object.values(syncState.codSettlementsById || {}),
    disputeRecords: Object.values(syncState.disputeRecordsById || {}),
    riderDocExpiries: Object.values(syncState.riderDocExpiriesById || {}),
    remoteFeatureFlags: syncState.remoteFeatureFlags || null,
    waitTimerStartByOrder: syncState.waitTimerStartByOrder || {},
    riderLiveCoordsByOrder: syncState.riderLiveCoordsByOrder || {},
    chatMessages: Object.values(syncState.chatMessagesById || {}).sort(
      (a, b) => Number(a.createdAtEpochMs || 0) - Number(b.createdAtEpochMs || 0)
    ),
  };
}

const server = http.createServer((req, res) => {
  const rawUrl = req.url || '/';
  const [pathname, queryString = ''] = rawUrl.split('?');

  if (req.method === 'OPTIONS') {
    res.writeHead(204, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Accept, Range',
    });
    res.end();
    return;
  }

  if (pathname === '/api/live-sync') {
    if (req.method === 'POST') {
      let body = '';
      req.on('data', chunk => {
        body += chunk;
        if (body.length > 8 * 1024 * 1024) req.destroy();
      });
      req.on('end', () => {
        try {
          const payload = JSON.parse(body || '{}');
          const now = Date.now();

          if (payload.deviceId) {
            const devId = String(payload.deviceId);
            const roleVal = String(payload.role || 'UNIFIED');
            syncState.devicesById[devId] = {
              deviceId: devId,
              role: roleVal,
              deviceModel: String(payload.deviceModel || 'Android Phone'),
              lat: Number(payload.deviceLat || 0),
              lng: Number(payload.deviceLng || 0),
              lastSeenEpochMs: now,
            };
            if (payload.fcmToken && String(payload.fcmToken).length > 12) {
              if (!syncState.fcmTokensByDevice) syncState.fcmTokensByDevice = {};
              syncState.fcmTokensByDevice[devId] = {
                token: String(payload.fcmToken),
                role: roleVal,
                updatedAtEpochMs: now,
              };
            }
          }

          if (payload.resetEpochMs && Number(payload.resetEpochMs) > syncState.resetEpochMs) {
            syncState.resetEpochMs = Number(payload.resetEpochMs);
            syncState.ordersById = {};
            syncState.chatMessagesById = {};
          }

          if (Array.isArray(payload.riderRegistrations)) {
            if (!syncState.riderRegistrationsByKey) {
              syncState.riderRegistrationsByKey = {};
            }
            for (const reg of payload.riderRegistrations) {
              if (!reg || !reg.riderKey) continue;
              const rKey = String(reg.riderKey);
              const existingReg = syncState.riderRegistrationsByKey[rKey];
              if (!existingReg || Number(reg.updatedAtEpochMs || 0) >= Number(existingReg.updatedAtEpochMs || 0)) {
                syncState.riderRegistrationsByKey[rKey] = {
                  ...(existingReg || {}),
                  ...reg,
                };
              }
            }
          }

          if (Array.isArray(payload.promoCoupons)) {
            if (!syncState.promoCouponsByCode) syncState.promoCouponsByCode = {};
            for (const c of payload.promoCoupons) {
              if (!c || !c.code) continue;
              const code = String(c.code).trim().toUpperCase();
              const prev = syncState.promoCouponsByCode[code];
              if (!prev || Number(c.updatedAtEpochMs || 0) >= Number(prev.updatedAtEpochMs || 0)) {
                syncState.promoCouponsByCode[code] = c;
              }
            }
          }

          if (Array.isArray(payload.withdrawalRequests)) {
            if (!syncState.withdrawalRequestsById) syncState.withdrawalRequestsById = {};
            for (const w of payload.withdrawalRequests) {
              if (!w || !w.requestId) continue;
              const wid = String(w.requestId);
              const prev = syncState.withdrawalRequestsById[wid];
              if (!prev || Number(w.updatedAtEpochMs || 0) >= Number(prev.updatedAtEpochMs || 0)) {
                syncState.withdrawalRequestsById[wid] = w;
              }
            }
          }

          if (Array.isArray(payload.supportTickets)) {
            if (!syncState.supportTicketsById) syncState.supportTicketsById = {};
            for (const t of payload.supportTickets) {
              if (!t || !t.ticketId) continue;
              const tid = String(t.ticketId);
              const prev = syncState.supportTicketsById[tid];
              if (!prev || Number(t.updatedAtEpochMs || 0) >= Number(prev.updatedAtEpochMs || 0)) {
                syncState.supportTicketsById[tid] = t;
              }
            }
          }

          if (Array.isArray(payload.codSettlements)) {
            if (!syncState.codSettlementsById) syncState.codSettlementsById = {};
            for (const s of payload.codSettlements) {
              if (!s || !s.settlementId) continue;
              const sid = String(s.settlementId);
              const prev = syncState.codSettlementsById[sid];
              if (!prev || Number(s.updatedAtEpochMs || 0) >= Number(prev.updatedAtEpochMs || 0)) {
                syncState.codSettlementsById[sid] = s;
              }
            }
          }

          if (Array.isArray(payload.disputeRecords)) {
            if (!syncState.disputeRecordsById) syncState.disputeRecordsById = {};
            for (const d of payload.disputeRecords) {
              if (!d || !d.disputeId) continue;
              const did = String(d.disputeId);
              const prev = syncState.disputeRecordsById[did];
              if (!prev || Number(d.updatedAtEpochMs || 0) >= Number(prev.updatedAtEpochMs || 0)) {
                syncState.disputeRecordsById[did] = d;
              }
            }
          }

          if (Array.isArray(payload.riderDocExpiries)) {
            if (!syncState.riderDocExpiriesById) syncState.riderDocExpiriesById = {};
            for (const e of payload.riderDocExpiries) {
              if (!e || !e.riderId) continue;
              const eid = String(e.riderId);
              const prev = syncState.riderDocExpiriesById[eid];
              if (!prev || Number(e.updatedAtEpochMs || 0) >= Number(prev.updatedAtEpochMs || 0)) {
                syncState.riderDocExpiriesById[eid] = e;
              }
            }
          }

          if (payload.remoteFeatureFlags && typeof payload.remoteFeatureFlags === 'object') {
            const incomingFlags = payload.remoteFeatureFlags;
            const prevFlags = syncState.remoteFeatureFlags;
            const inTs = Number(incomingFlags.updatedAtEpochMs || 0);
            const prevTs = Number((prevFlags && prevFlags.updatedAtEpochMs) || 0);
            if (!prevFlags || (inTs > 0 && inTs >= prevTs)) {
              syncState.remoteFeatureFlags = {
                ...(prevFlags || {}),
                ...incomingFlags,
              };
            }
          }

          if (payload.waitTimerStartByOrder && typeof payload.waitTimerStartByOrder === 'object') {
            if (!syncState.waitTimerStartByOrder) syncState.waitTimerStartByOrder = {};
            for (const [oid, ts] of Object.entries(payload.waitTimerStartByOrder)) {
              const numTs = Number(ts || 0);
              if (numTs > 0 && (!syncState.waitTimerStartByOrder[oid] || numTs < syncState.waitTimerStartByOrder[oid])) {
                syncState.waitTimerStartByOrder[oid] = numTs;
              }
            }
          }

          if (payload.riderLiveCoordsByOrder && typeof payload.riderLiveCoordsByOrder === 'object') {
            if (!syncState.riderLiveCoordsByOrder) syncState.riderLiveCoordsByOrder = {};
            for (const [oid, coords] of Object.entries(payload.riderLiveCoordsByOrder)) {
              if (coords && (Number(coords.lat || 0) !== 0 || Number(coords.lng || 0) !== 0)) {
                syncState.riderLiveCoordsByOrder[oid] = {
                  lat: Number(coords.lat || 0),
                  lng: Number(coords.lng || 0),
                  updatedAtEpochMs: now,
                };
              }
            }
          }

          if (Array.isArray(payload.chatMessages)) {
            if (!syncState.chatMessagesById) syncState.chatMessagesById = {};
            const newRemoteChatMsgs = [];
            for (const m of payload.chatMessages) {
              if (!m || !m.messageId || !m.orderId || !m.messageText) continue;
              const mid = String(m.messageId);
              if (mid.startsWith('msg_seed_')) continue;
              if (!syncState.chatMessagesById[mid]) {
                const record = {
                  messageId: mid,
                  orderId: String(m.orderId),
                  senderUid: String(m.senderUid || 'sync_uid'),
                  senderRole: String(m.senderRole || 'RIDER'),
                  senderName: String(m.senderName || 'Partner'),
                  messageText: String(m.messageText),
                  deliveryStage: String(m.deliveryStage || 'IN_TRANSIT'),
                  timestampLabel: String(m.timestampLabel || 'Just now'),
                  createdAtEpochMs: Number(m.createdAtEpochMs || now),
                  syncedToFirestore: Boolean(m.syncedToFirestore),
                };
                syncState.chatMessagesById[mid] = record;
                newRemoteChatMsgs.push(record);
              }
            }
            if (newRemoteChatMsgs.length > 0 && syncState.fcmTokensByDevice) {
              const latestMsg = newRemoteChatMsgs[newRemoteChatMsgs.length - 1];
              const targetRole = latestMsg.senderRole === 'RIDER' ? 'CUSTOMER' : 'RIDER';
              const targetTokens = Object.values(syncState.fcmTokensByDevice)
                .filter(entry => entry && entry.role === targetRole)
                .map(entry => entry.token);
              sendFcmPushNotifications({
                tokens: targetTokens,
                title: `New Chat Message (#${latestMsg.orderId})`,
                body: `${latestMsg.senderName}: ${latestMsg.messageText}`,
                data: { orderId: latestMsg.orderId, type: 'CHAT' },
              });
            }
            const allMsgs = Object.values(syncState.chatMessagesById);
            if (allMsgs.length > 500) {
              allMsgs.sort((a, b) => Number(a.createdAtEpochMs || 0) - Number(b.createdAtEpochMs || 0));
              const kept = allMsgs.slice(-500);
              const nextMap = {};
              for (const km of kept) nextMap[km.messageId] = km;
              syncState.chatMessagesById = nextMap;
            }
          }

          if (Array.isArray(payload.orders)) {
            const senderDeviceId = String(payload.deviceId || '');
            const senderRole = String(payload.role || 'UNIFIED');

            for (const incoming of payload.orders) {
              if (!incoming || !incoming.orderId) continue;
              const oid = String(incoming.orderId);
              if (DEMO_ORDER_IDS.has(oid)) continue;
              const existing = syncState.ordersById[oid];
              if (!existing) {
                if (incoming.orderStatus === 'RIDER_ASSIGNED' && senderRole !== 'RIDER') {
                  syncState.ordersById[oid] = {
                    ...incoming,
                    orderStatus: 'SEARCHING_RIDER',
                    statusRank: 1,
                    assignedRiderId: '',
                    assignedRiderName: '',
                    assignedRiderPhone: '',
                    assignedRiderVehicle: '',
                    createdByDeviceId: senderDeviceId,
                    updatedAtEpochMs: now,
                  };
                } else {
                  syncState.ordersById[oid] = {
                    ...incoming,
                    createdByDeviceId: senderDeviceId,
                    updatedAtEpochMs: now,
                  };
                }
                if (syncState.ordersById[oid].orderStatus === 'SEARCHING_RIDER' && syncState.fcmTokensByDevice) {
                  const riderTokens = Object.values(syncState.fcmTokensByDevice)
                    .filter(entry => entry && entry.role === 'RIDER')
                    .map(entry => entry.token);
                  sendFcmPushNotifications({
                    tokens: riderTokens,
                    title: `New Parcel Request #${oid}`,
                    body: `Pickup: ${incoming.pickupFullAddress || 'Nearby'} • Payout ${incoming.riderEarning || ''}`,
                    data: { orderId: oid, orderStatus: 'SEARCHING_RIDER', targetRole: 'RIDER' },
                  });
                }
              } else {
                let inRank = Number(incoming.statusRank || 1);
                const exRank = Number(existing.statusRank || 1);

                if (
                  existing.orderStatus === 'SEARCHING_RIDER' &&
                  incoming.orderStatus === 'RIDER_ASSIGNED' &&
                  existing.createdByDeviceId &&
                  senderDeviceId === existing.createdByDeviceId &&
                  senderRole !== 'RIDER'
                ) {
                  continue;
                }

                const merged = inRank >= exRank ? { ...existing, ...incoming } : { ...incoming, ...existing };
                if (existing.pickupProof && !incoming.pickupProof) merged.pickupProof = existing.pickupProof;
                if (incoming.pickupProof) merged.pickupProof = incoming.pickupProof;
                if (existing.deliveryProof && !incoming.deliveryProof) merged.deliveryProof = existing.deliveryProof;
                if (incoming.deliveryProof) merged.deliveryProof = incoming.deliveryProof;
                if (existing.assignedRiderId && !incoming.assignedRiderId && merged.orderStatus !== 'SEARCHING_RIDER') {
                  merged.assignedRiderId = existing.assignedRiderId;
                  merged.assignedRiderName = existing.assignedRiderName;
                  merged.assignedRiderPhone = existing.assignedRiderPhone;
                  merged.assignedRiderVehicle = existing.assignedRiderVehicle;
                }
                merged.createdByDeviceId = existing.createdByDeviceId || senderDeviceId;
                merged.statusRank = Math.max(inRank, exRank);
                merged.updatedAtEpochMs = now;
                syncState.ordersById[oid] = merged;
              }
            }
          }
          saveSyncState();
        } catch {}

        const out = JSON.stringify(buildSyncResponsePayload());
        res.writeHead(200, {
          'Content-Type': 'application/json; charset=utf-8',
          'Cache-Control': 'no-store',
          'Access-Control-Allow-Origin': '*',
        });
        res.end(out);
      });
      return;
    }

    const out = JSON.stringify(buildSyncResponsePayload());
    res.writeHead(200, {
      'Content-Type': 'application/json; charset=utf-8',
      'Cache-Control': 'no-store',
      'Access-Control-Allow-Origin': '*',
    });
    res.end(out);
    return;
  }

  if (pathname === '/api/backup-export') {
    writeDailyBackupSnapshot(true);
    const dayStr = new Date().toISOString().slice(0, 10);
    const snapshot = JSON.stringify(
      {
        exportedAtIso: new Date().toISOString(),
        version: '6.1.3',
        ...syncState,
      },
      null,
      2
    );
    const buf = Buffer.from(snapshot, 'utf-8');
    res.writeHead(200, {
      'Content-Type': 'application/json; charset=utf-8',
      'Content-Disposition': `attachment; filename="quickdelivery-backup-${dayStr}.json"`,
      'Content-Length': String(buf.length),
      'Cache-Control': 'no-store',
      'Access-Control-Allow-Origin': '*',
    });
    if (req.method === 'HEAD') {
      res.end();
      return;
    }
    res.end(buf);
    return;
  }

  if (pathname === '/api/payments/create-order' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => {
      body += chunk;
      if (body.length > 256 * 1024) req.destroy();
    });
    req.on('end', () => {
      try {
        const parsed = JSON.parse(body || '{}');
        const orderId = String(parsed.orderId || `QD-${Date.now() % 10000}`);
        const amountInr = Number(parsed.amountInr || 0);
        const amountPaise = Math.max(100, Math.round(amountInr * 100));
        const provider = String(
          parsed.provider || (syncState.remoteFeatureFlags && syncState.remoteFeatureFlags.gatewayProvider) || 'RAZORPAY'
        ).toUpperCase();
        const gatewayOrderId = `${provider.toLowerCase()}_ord_${crypto.randomBytes(6).toString('hex')}`;
        if (!syncState.paymentsByOrder) syncState.paymentsByOrder = {};
        syncState.paymentsByOrder[orderId] = {
          orderId,
          provider,
          gatewayOrderId,
          amountInr,
          amountPaise,
          status: 'CREATED',
          createdAtIso: new Date().toISOString(),
        };
        saveSyncState();
        res.writeHead(200, {
          'Content-Type': 'application/json; charset=utf-8',
          'Access-Control-Allow-Origin': '*',
        });
        res.end(JSON.stringify({ ok: true, orderId, provider, gatewayOrderId, amountPaise, currency: 'INR' }));
      } catch {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ ok: false, error: 'Invalid payment order request' }));
      }
    });
    return;
  }

  if (pathname === '/api/payments/razorpay-webhook' && req.method === 'POST') {
    let rawBody = '';
    req.on('data', chunk => {
      rawBody += chunk;
      if (rawBody.length > 512 * 1024) req.destroy();
    });
    req.on('end', () => {
      const secret = process.env.RAZORPAY_WEBHOOK_SECRET || '';
      const sigHeader = String(req.headers['x-razorpay-signature'] || '');
      if (secret) {
        const expected = crypto.createHmac('sha256', secret).update(rawBody).digest('hex');
        const sigBuf = Buffer.from(sigHeader, 'utf-8');
        const expBuf = Buffer.from(expected, 'utf-8');
        if (sigBuf.length !== expBuf.length || !crypto.timingSafeEqual(sigBuf, expBuf)) {
          res.writeHead(401, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ ok: false, error: 'Invalid webhook signature' }));
          return;
        }
      }
      try {
        const evt = JSON.parse(rawBody || '{}');
        const paymentEntity = evt?.payload?.payment?.entity || {};
        const qdOrderId = String(paymentEntity?.notes?.qdOrderId || evt.orderId || '');
        if (qdOrderId && syncState.ordersById[qdOrderId]) {
          syncState.ordersById[qdOrderId].paymentStatus = 'VERIFIED_PAID_TO_COMPANY';
          syncState.ordersById[qdOrderId].updatedAtEpochMs = Date.now();
          saveSyncState();
        }
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ ok: true, received: true, orderId: qdOrderId || null }));
      } catch {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ ok: false, error: 'Malformed webhook payload' }));
      }
    });
    return;
  }

  if (pathname.startsWith('/apks/') || pathname.startsWith('/dl/') || pathname.endsWith('.apk')) {
    const requestedFile = path.basename(pathname).replace(/[^A-Za-z0-9._-]/g, '') || 'QuickDelivery-Customer.apk';
    const finalFileName = requestedFile.endsWith('.apk') ? requestedFile : `${requestedFile}.apk`;
    const apkFilePath = resolveSourceApkPath(finalFileName);

    if (apkFilePath && fs.existsSync(apkFilePath)) {
      const stat = fs.statSync(apkFilePath);
      const total = stat.size;
      const range = req.headers.range;

      if (range) {
        const parts = range.replace(/bytes=/, '').split('-');
        const start = parseInt(parts[0], 10);
        const end = parts[1] ? parseInt(parts[1], 10) : total - 1;
        if (!isNaN(start) && !isNaN(end) && start >= 0 && end < total && start <= end) {
          res.writeHead(206, {
            'Content-Range': `bytes ${start}-${end}/${total}`,
            'Accept-Ranges': 'bytes',
            'Content-Length': String(end - start + 1),
            'Content-Type': 'application/vnd.android.package-archive',
            'Content-Disposition': `attachment; filename="${finalFileName}"`,
            'Cache-Control': 'public, max-age=86400',
            'Access-Control-Allow-Origin': '*',
          });
          if (req.method === 'HEAD') {
            res.end();
            return;
          }
          fs.createReadStream(apkFilePath, { start, end }).pipe(res);
          return;
        }
      }

      res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Disposition': `attachment; filename="${finalFileName}"`,
        'Content-Length': String(total),
        'Accept-Ranges': 'bytes',
        'Cache-Control': 'public, max-age=86400',
        'Access-Control-Allow-Origin': '*',
      });
      if (req.method === 'HEAD') {
        res.end();
        return;
      }
      fs.createReadStream(apkFilePath).pipe(res);
      return;
    }
  }

  if (pathname === '/privacy-policy' || pathname === '/terms' || pathname === '/delete-account') {
    const kind = pathname === '/terms' ? 'terms' : pathname === '/delete-account' ? 'delete' : 'privacy';
    const html = renderLegalPageHtml(kind);
    const buf = Buffer.from(html, 'utf-8');
    res.writeHead(200, {
      'Content-Type': 'text/html; charset=utf-8',
      'Content-Length': String(buf.length),
      'Cache-Control': 'no-store',
    });
    if (req.method === 'HEAD') {
      res.end();
      return;
    }
    res.end(buf);
    return;
  }

  const params = new URLSearchParams(queryString);
  let autoDownload = params.get('download') || '';
  let roleFilter = params.get('role') || '';
  if (pathname === '/customer' || pathname === '/customer-apk' || autoDownload.includes('Customer')) {
    autoDownload = 'QuickDelivery-Customer.apk';
    roleFilter = 'CUSTOMER';
  } else if (pathname === '/rider' || pathname === '/rider-apk' || autoDownload.includes('Rider')) {
    autoDownload = 'QuickDelivery-Rider-Partner.apk';
    roleFilter = 'RIDER';
  } else if (pathname === '/admin' || pathname === '/admin-apk' || autoDownload.includes('Admin')) {
    autoDownload = 'QuickDelivery-Admin-Enterprise.apk';
    roleFilter = 'ADMIN';
  }
  const html = renderDownloadPageHtml(autoDownload, roleFilter);
  const buf = Buffer.from(html, 'utf-8');
  res.writeHead(200, {
    'Content-Type': 'text/html; charset=utf-8',
    'Content-Length': String(buf.length),
    'Cache-Control': 'no-store',
  });
  if (req.method === 'HEAD') {
    res.end();
    return;
  }
  res.end(buf);
});

server.on('error', () => {
  process.exit(0);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Quick Delivery Mobile APK & Live Sync Server listening on http://0.0.0.0:${PORT}`);
});
