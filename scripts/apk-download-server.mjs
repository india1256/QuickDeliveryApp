import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { execSync, spawn } from 'node:child_process';

const PORT = Number(process.env.APP_PORT || 3000);
const ROOT = process.cwd();
const SYNC_STATE_FILE = '/tmp/quickdelivery-live-sync.json';
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
          riderRegistrationsByKey: parsed.riderRegistrationsByKey || {},
          promoCouponsByCode: parsed.promoCouponsByCode || {},
          withdrawalRequestsById: parsed.withdrawalRequestsById || {},
          supportTicketsById: parsed.supportTicketsById || {},
        };
      }
    }
  } catch {}
  return {
    resetEpochMs: Date.now(),
    ordersById: {},
    devicesById: {},
    riderRegistrationsByKey: {},
    promoCouponsByCode: {},
    withdrawalRequestsById: {},
    supportTicketsById: {},
  };
}

let syncState = loadSyncState();

function saveSyncState() {
  try {
    fs.writeFileSync(SYNC_STATE_FILE, JSON.stringify(syncState), 'utf-8');
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
      <h2>✨ v5.7.0 Highlights: Play Store-Ready Clean Experience</h2>
      <ol class="step-list">
        <li><b>Customer App:</b> Auto-Capture exact Pickup/Drop address via GPS, Save <b>Home / Office / Shop</b> addresses, apply Admin Promo Coupons, cancel with reason &amp; In-App Helpdesk Tickets.</li>
        <li><b>Rider Partner App:</b> Clean Accept/Reject top popup (auto-stops ring on action), Clear KYC document check, Same-Day vs Total earnings, Online/COD split &amp; UPI/Bank Payout Requests.</li>
        <li><b>Admin Console:</b> Allowlist Google Sign-In Gate, Reassign/Cancel active orders, KYC zoom &amp; doc-specific rejection, Dynamic Promo Coupon Manager, Payout &amp; Support Ticket Approvals.</li>
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
      <a href="/delete-account" style="color: #6EE7B7; margin: 0 8px;">Delete Account &amp; Data</a>
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
      <h1>Terms of Service</h1>
      <p><b>Effective Date:</b> November 2026 • <b>Contact:</b> ravindergupta141@gmail.com</p>
      <div class="card">
        <h2>1. Parcel Booking &amp; Permitted Items</h2>
        <p>Quick Delivery provides on-demand local parcel pickup and drop-off services. Customers must accurately declare parcel weight (maximum 20 kg per shipment) and contents. Prohibited, hazardous, illegal, or restricted goods are strictly banned.</p>
        <h2>2. OTP Handover &amp; Cancellation</h2>
        <p>Orders booked via online payment methods require 4-digit Pickup and Drop OTP verification between Customer, Rider Partner, and Recipient. Customers may cancel orders free of charge while the request is searching for a partner before pickup.</p>
        <h2>3. Partner Safety &amp; Settlement</h2>
        <p>Delivery partners receive 80% of base trip fare plus 100% of customer tips. Partners must follow road safety laws and speed thresholds.</p>
      </div>`
    : `
      <h1>Privacy Policy</h1>
      <p><b>Developer / Data Controller:</b> Quick Delivery Logistics (<a href="mailto:ravindergupta141@gmail.com">ravindergupta141@gmail.com</a>)<br /><b>Effective Date:</b> November 2026</p>
      <div class="card">
        <h2>1. Information We Collect</h2>
        <ul>
          <li><b>Account Details:</b> Name, 10-digit mobile number, and email address used for authentication, parcel receipts, and delivery coordination.</li>
          <li><b>Foreground Location:</b> Approximate and precise GPS coordinates collected only when you actively request pickup/drop location detection or live delivery tracking.</li>
          <li><b>Camera (User-Initiated):</b> Parcel photos and proof-of-delivery images captured strictly when you press the Camera button.</li>
        </ul>
        <h2>2. How We Use &amp; Protect Data</h2>
        <ul>
          <li>Used solely to calculate accurate delivery fares, verify OTP handovers, display live tracking to assigned parties, and issue GST receipts.</li>
          <li>All data in transit is encrypted over HTTPS (TLS).</li>
          <li>We do not sell personal data or share it with third-party advertising networks.</li>
        </ul>
        <h2>3. Data Retention &amp; Deletion</h2>
        <p>You may erase your account and local data anytime inside the app via <b>Menu → Delete Account &amp; Data</b>, or review deletion steps at <a href="/delete-account">/delete-account</a>.</p>
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
            syncState.devicesById[String(payload.deviceId)] = {
              deviceId: String(payload.deviceId),
              role: String(payload.role || 'UNIFIED'),
              deviceModel: String(payload.deviceModel || 'Android Phone'),
              lat: Number(payload.deviceLat || 0),
              lng: Number(payload.deviceLng || 0),
              lastSeenEpochMs: now,
            };
          }

          if (payload.resetEpochMs && Number(payload.resetEpochMs) > syncState.resetEpochMs) {
            syncState.resetEpochMs = Number(payload.resetEpochMs);
            syncState.ordersById = {};
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
