import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { execSync } from 'node:child_process';

const PORT = Number(process.env.APP_PORT || 3000);
const ROOT = process.cwd();
const SYNC_STATE_FILE = '/tmp/quickdelivery-live-sync.json';

const APK_FILE_NAMES = [
  'QuickDelivery-Customer.apk',
  'QuickDelivery-Rider-Partner.apk',
  'QuickDelivery-Admin-Enterprise.apk',
  'QuickDelivery-AllInOne-Hub.apk',
];

function resolveSourceApkPath(requestedFileName = '') {
  const candidates = [
    requestedFileName ? path.join(ROOT, 'public', 'apks', requestedFileName) : '',
    requestedFileName ? path.join('/var/www/assets/apks', requestedFileName) : '',
    path.join(ROOT, '.build-outputs', 'app-debug.apk'),
    path.join(ROOT, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk'),
  ].filter(Boolean);

  for (const candidate of candidates) {
    try {
      if (fs.existsSync(candidate) && fs.statSync(candidate).size > 100000) {
        return candidate;
      }
    } catch {}
  }
  return null;
}

function getApkSizeBytes(fileName = 'QuickDelivery-Customer.apk') {
  const apkPath = resolveSourceApkPath(fileName);
  if (!apkPath) return 33837830;
  try {
    return fs.statSync(apkPath).size;
  } catch {
    return 33837830;
  }
}

function getApkSizeMb(fileName = 'QuickDelivery-Customer.apk') {
  const bytes = getApkSizeBytes(fileName);
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function loadSyncState() {
  try {
    if (fs.existsSync(SYNC_STATE_FILE)) {
      const parsed = JSON.parse(fs.readFileSync(SYNC_STATE_FILE, 'utf-8'));
      if (parsed && typeof parsed === 'object') {
        return {
          resetEpochMs: Number(parsed.resetEpochMs || 0),
          ordersById: parsed.ordersById || {},
          devicesById: parsed.devicesById || {},
        };
      }
    }
  } catch {}
  return {
    resetEpochMs: 0,
    ordersById: {},
    devicesById: {},
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
  const sizeBytes = getApkSizeBytes(safeAuto || 'QuickDelivery-Customer.apk');

  const roleUpper = String(singleRoleFilter || '').toUpperCase();
  const showCustomer = !roleUpper || roleUpper === 'CUSTOMER';
  const showRider = !roleUpper || roleUpper === 'RIDER';
  const showAdmin = !roleUpper || roleUpper === 'ADMIN';

  const pageHeading =
    roleUpper === 'CUSTOMER'
      ? '🛍️ Customer App Download (Sirf 1 App: QD Customer)'
      : roleUpper === 'RIDER'
      ? '🛵 Rider Partner App Download (Sirf 1 App: QD Rider Partner)'
      : roleUpper === 'ADMIN'
      ? '🛡️ Admin Console App Download (Sirf 1 App: QD Admin Console)'
      : '📲 Quick Delivery — 3 Alag-Alag Standalone Apps (1 Link = 1 App)';

  const customerCardHtml = showCustomer
    ? `
      <div class="apk-card" style="border-color: #10B981;">
        <div class="apk-title" style="color: #6EE7B7;">🛍️ App 1 (Customer Only): QuickDelivery-Customer.apk (${getApkSizeMb('QuickDelivery-Customer.apk')})</div>
        <div class="apk-desc">Is link se <b>sirf 1 App (QD Customer)</b> download aur install hoga. Customer Parcel Booking, Live GPS Tracking &amp; OTP.</div>
        <button class="dl-btn" style="background: #059669;" onclick="startChunkedApkDownload('QuickDelivery-Customer.apk')">
          📥 Download ONLY Customer App (${getApkSizeMb('QuickDelivery-Customer.apk')})
        </button>
        <a class="direct-link" href="/dl/QuickDelivery-Customer.apk" download="QuickDelivery-Customer.apk">
          Direct Single-App Link: /dl/QuickDelivery-Customer.apk
        </a>
      </div>`
    : '';

  const riderCardHtml = showRider
    ? `
      <div class="apk-card" style="border-color: #F59E0B;">
        <div class="apk-title" style="color: #FCD34D;">🛵 App 2 (Rider Only): QuickDelivery-Rider-Partner.apk (${getApkSizeMb('QuickDelivery-Rider-Partner.apk')})</div>
        <div class="apk-desc">Is link se <b>sirf 1 App (QD Rider Partner)</b> download aur install hoga. Order Accept, Pickup/Drop OTP &amp; Partner Wallet.</div>
        <button class="dl-btn" style="background: #D97706;" onclick="startChunkedApkDownload('QuickDelivery-Rider-Partner.apk')">
          📥 Download ONLY Rider Partner App (${getApkSizeMb('QuickDelivery-Rider-Partner.apk')})
        </button>
        <a class="direct-link" href="/dl/QuickDelivery-Rider-Partner.apk" download="QuickDelivery-Rider-Partner.apk">
          Direct Single-App Link: /dl/QuickDelivery-Rider-Partner.apk
        </a>
      </div>`
    : '';

  const adminCardHtml = showAdmin
    ? `
      <div class="apk-card" style="border-color: #14B8A6;">
        <div class="apk-title" style="color: #5EEAD4;">🛡️ App 3 (Admin Only): QuickDelivery-Admin-Enterprise.apk (${getApkSizeMb('QuickDelivery-Admin-Enterprise.apk')})</div>
        <div class="apk-desc">Is link se <b>sirf 1 App (QD Admin Console)</b> download aur install hoga. Live Fleet GPS Tracking, Permissions &amp; Order Control.</div>
        <button class="dl-btn" style="background: #0D9488;" onclick="startChunkedApkDownload('QuickDelivery-Admin-Enterprise.apk')">
          📥 Download ONLY Admin Console App (${getApkSizeMb('QuickDelivery-Admin-Enterprise.apk')})
        </button>
        <a class="direct-link" href="/dl/QuickDelivery-Admin-Enterprise.apk" download="QuickDelivery-Admin-Enterprise.apk">
          Direct Single-App Link: /dl/QuickDelivery-Admin-Enterprise.apk
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
      border: 2px solid #38BDF8;
      border-radius: 16px;
      padding: 16px;
      margin-bottom: 16px;
      box-shadow: 0 8px 24px rgba(0,0,0,0.4);
    }
    .prog-title {
      font-size: 15px;
      font-weight: 800;
      color: #38BDF8;
      margin-bottom: 8px;
    }
    .prog-bar-bg {
      width: 100%;
      height: 18px;
      background: #1E293B;
      border-radius: 999px;
      overflow: hidden;
      margin-bottom: 8px;
      border: 1px solid #334155;
    }
    .prog-bar-fill {
      height: 100%;
      width: 0%;
      background: linear-gradient(90deg, #10B981, #38BDF8);
      transition: width 0.15s ease;
    }
    .prog-stats {
      font-size: 12px;
      color: #E2E8F0;
      font-weight: 700;
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
      padding: 13px 14px;
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
      <span class="badge">1 Link = 1 Single Application • Separate Package IDs</span>
      <h1>${pageHeading}</h1>
      <p>Ab har application ka apna <b>alag package ID</b> aur <b>alag link</b> hai. Ek APK install karne par phone me <b>sirf wahi 1 application</b> install hoga (4 apps nahi)!</p>
    </div>

    <div id="progress-panel">
      <div class="prog-title" id="prog-title">🚀 Downloading APK...</div>
      <div class="prog-bar-bg">
        <div class="prog-bar-fill" id="prog-fill"></div>
      </div>
      <div class="prog-stats" id="prog-stats">Starting download (0% — 0.0 MB / ${sizeMb})...</div>
    </div>

    <div class="grid">
      ${customerCardHtml}
      ${riderCardHtml}
      ${adminCardHtml}
    </div>

    <div class="guide-box">
      <h2>⚠️ Pehle 4 Apps Kyu Dikhe The? (Important Note)</h2>
      <ol class="step-list">
        <li>Agar aapke phone me purana <b>Quick Delivery</b> app pehle se installed hai (jisme 4 icons aaye the), toh us purane app ko ek baar <b>Uninstall</b> kar dein.</li>
        <li>Ab upar diye gaye 3 alag-alag APK me se:
          <br>• <b>Phone 1</b> me sirf <b>Customer App</b> install karein (Sirf <b>QD Customer</b> icon aayega).
          <br>• <b>Phone 2</b> me sirf <b>Rider Partner App</b> install karein (Sirf <b>QD Rider Partner</b> icon aayega).
          <br>• <b>Phone 3</b> me sirf <b>Admin Console App</b> install karein (Sirf <b>QD Admin Console</b> icon aayega).
        </li>
      </ol>
    </div>

    <div class="live-box">
      <h2>🟢 Live Multi-Phone Cloud Sync Monitor (Auto-Updates Every 2s)</h2>
      <div id="live-sync-summary" style="font-size: 13px; color: #ECFDF5;">
        Waiting for phones to connect...
      </div>
    </div>
  </div>

  <script>
    const EXPECTED_TOTAL_BYTES = ${sizeBytes};
    const AUTO_DOWNLOAD_FILE = ${JSON.stringify(safeAuto)};
    let isDownloading = false;

    async function fetchChunkWithRetry(url, start, end, maxRetries = 12) {
      for (let attempt = 1; attempt <= maxRetries; attempt++) {
        try {
          const res = await fetch(url, {
            method: 'GET',
            headers: {
              'Range': 'bytes=' + start + '-' + end,
              'Cache-Control': 'no-transform'
            }
          });
          if (res.status === 206 || res.status === 200) {
            const buf = await res.arrayBuffer();
            if (buf && buf.byteLength > 0) {
              return { buffer: buf, status: res.status };
            }
          }
        } catch (err) {}
        await new Promise(r => setTimeout(r, Math.min(1000 * attempt, 3000)));
      }
      throw new Error('Network connection interrupted while fetching chunk.');
    }

    async function startChunkedApkDownload(fileName) {
      if (isDownloading) return;
      isDownloading = true;

      const panel = document.getElementById('progress-panel');
      const titleEl = document.getElementById('prog-title');
      const fillEl = document.getElementById('prog-fill');
      const statsEl = document.getElementById('prog-stats');

      panel.style.display = 'block';
      panel.scrollIntoView({ behavior: 'smooth', block: 'center' });
      titleEl.textContent = '🚀 Downloading ' + fileName + ' (Single App APK)...';
      fillEl.style.width = '2%';
      statsEl.textContent = 'Connecting to server... Do not close this tab.';

      const url = '/dl/' + fileName;
      const CHUNK_SIZE = 2 * 1024 * 1024;
      let totalSize = EXPECTED_TOTAL_BYTES;

      try {
        try {
          const headRes = await fetch(url, { method: 'HEAD' });
          const len = Number(headRes.headers.get('content-length') || 0);
          if (len > 1000000) totalSize = len;
        } catch (e) {}

        const chunks = [];
        let downloaded = 0;

        while (downloaded < totalSize) {
          const end = Math.min(downloaded + CHUNK_SIZE - 1, totalSize - 1);
          const result = await fetchChunkWithRetry(url, downloaded, end, 12);

          if (downloaded === 0) {
            const view = new Uint8Array(result.buffer);
            if (view.length < 4 || view[0] !== 0x50 || view[1] !== 0x4B) {
              throw new Error('Server warming up, retrying direct APK stream...');
            }
          }

          chunks.push(result.buffer);
          downloaded += result.buffer.byteLength;

          const pct = Math.min(100, Math.round((downloaded / totalSize) * 100));
          const dlMb = (downloaded / (1024 * 1024)).toFixed(1);
          const totMb = (totalSize / (1024 * 1024)).toFixed(1);
          fillEl.style.width = pct + '%';
          statsEl.textContent = 'Downloaded: ' + pct + '% (' + dlMb + ' MB / ' + totMb + ' MB) — Auto-Resume Active';

          if (result.status === 200 && result.buffer.byteLength >= totalSize) {
            break;
          }
        }

        titleEl.textContent = '✅ 100% Complete! Saving ' + fileName + ' to your phone...';
        fillEl.style.width = '100%';
        statsEl.textContent = 'APK verified (' + (totalSize / (1024 * 1024)).toFixed(1) + ' MB). Check your browser Downloads to install!';

        const blob = new Blob(chunks, { type: 'application/vnd.android.package-archive' });
        const blobUrl = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = blobUrl;
        a.download = fileName;
        document.body.appendChild(a);
        a.click();
        setTimeout(() => {
          document.body.removeChild(a);
          URL.revokeObjectURL(blobUrl);
        }, 60000);
      } catch (err) {
        titleEl.textContent = '⚠️ Switching to Direct Stream Download...';
        statsEl.textContent = 'Starting direct download for ' + fileName + '...';
        window.location.href = '/dl/' + fileName;
      } finally {
        isDownloading = false;
      }
    }

    if (AUTO_DOWNLOAD_FILE) {
      window.addEventListener('DOMContentLoaded', () => {
        setTimeout(() => startChunkedApkDownload(AUTO_DOWNLOAD_FILE), 400);
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
          ? devices.map(d => '📱 <b>' + d.role + '</b> (' + d.deviceModel + ')').join(' &nbsp;|&nbsp; ')
          : 'Ready — Open the installed APK on Phone 1, Phone 2, and Phone 3 to see them live here!';
        const ordHtml = orders.length
          ? '<ul style="margin:8px 0 0 0;padding-left:18px;">' + orders.map(o =>
              '<li><b>' + o.orderId + '</b> — Status: <b>' + o.orderStatus + '</b> | Pickup OTP: <b>' + o.pickupOtp + '</b> | Delivery OTP: <b>' + o.deliveryOtp + '</b> | Fare: ' + o.totalAmount + '</li>'
            ).join('') + '</ul>'
          : '<div style="margin-top:6px;color:#A7F3D0;">No active live orders yet — book an order on Phone 1 (Customer App) to watch it sync live!</div>';
        el.innerHTML = '<div><b>Connected Phones (' + devices.length + '):</b> ' + devHtml + '</div>' + ordHtml;
      } catch (e) {}
    }
    setInterval(refreshLiveSync, 2000);
    refreshLiveSync();
  </script>
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
        const sourceApk = resolveSourceApkPath(fileName);
        if (!sourceApk || !fs.existsSync(sourceApk)) continue;
        const srcStat = fs.statSync(sourceApk);
        const dest = path.join(apkDir, fileName);
        try {
          let needsCopy = true;
          if (fs.existsSync(dest)) {
            const destStat = fs.statSync(dest);
            if (destStat.size === srcStat.size && Math.abs(destStat.mtimeMs - srcStat.mtimeMs) < 1000) {
              needsCopy = false;
            }
          }
          if (needsCopy) {
            fs.copyFileSync(sourceApk, dest);
            fs.chmodSync(dest, 0o644);
          }
          fs.utimesSync(dest, srcStat.atime, srcStat.mtime);
        } catch {}
      }
    }

    const htmlAll = renderDownloadPageHtml('', '');
    const htmlCustomer = renderDownloadPageHtml('QuickDelivery-Customer.apk', 'CUSTOMER');
    const htmlRider = renderDownloadPageHtml('QuickDelivery-Rider-Partner.apk', 'RIDER');
    const htmlAdmin = renderDownloadPageHtml('QuickDelivery-Admin-Enterprise.apk', 'ADMIN');

    if (fs.existsSync('/var/www/assets')) {
      fs.writeFileSync('/var/www/assets/warmup.html', htmlAll, 'utf-8');
      fs.writeFileSync('/var/www/assets/customer-apk.html', htmlCustomer, 'utf-8');
      fs.writeFileSync('/var/www/assets/rider-apk.html', htmlRider, 'utf-8');
      fs.writeFileSync('/var/www/assets/admin-apk.html', htmlAdmin, 'utf-8');
    }
    try {
      fs.mkdirSync(path.join(ROOT, 'public'), { recursive: true });
      fs.writeFileSync(path.join(ROOT, 'public', 'apk-download.html'), htmlAll, 'utf-8');
      fs.writeFileSync(path.join(ROOT, 'public', 'customer-apk.html'), htmlCustomer, 'utf-8');
      fs.writeFileSync(path.join(ROOT, 'public', 'rider-apk.html'), htmlRider, 'utf-8');
      fs.writeFileSync(path.join(ROOT, 'public', 'admin-apk.html'), htmlAdmin, 'utf-8');
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

configureNginxDirectApkServing();

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
        if (body.length > 2 * 1024 * 1024) req.destroy();
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
              lastSeenEpochMs: now,
            };
          }

          if (payload.resetEpochMs && Number(payload.resetEpochMs) > syncState.resetEpochMs) {
            syncState.resetEpochMs = Number(payload.resetEpochMs);
            syncState.ordersById = {};
          }

          if (Array.isArray(payload.orders)) {
            const senderDeviceId = String(payload.deviceId || '');
            const senderRole = String(payload.role || 'UNIFIED');

            for (const incoming of payload.orders) {
              if (!incoming || !incoming.orderId) continue;
              const oid = String(incoming.orderId);
              const existing = syncState.ordersById[oid];
              if (!existing) {
                const isDemoOrder = oid === 'QD-6204' || oid === 'QD-7001';
                if (!isDemoOrder && incoming.orderStatus === 'RIDER_ASSIGNED' && senderRole !== 'RIDER') {
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

                // Prevent Customer phone's auto-assign timer from skipping Rider's Accept button
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
