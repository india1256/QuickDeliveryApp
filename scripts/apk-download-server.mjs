import http from 'node:http';
import https from 'node:https';
import net from 'node:net';
import tls from 'node:tls';
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { execSync, spawn } from 'node:child_process';

const IS_AISTUDIO_CONTAINER = fs.existsSync('/etc/nginx/user_auth_verification.lua') || Number(process.env.NGINX_PORT || -1) === Number(process.env.PORT || -2);
const PORT = Number(process.env.APP_PORT || (IS_AISTUDIO_CONTAINER ? (process.env.DEFAULT_APP_PORT || 3000) : (process.env.PORT || 3000)));
const ROOT = process.cwd();
const BRAND_DOMAIN = 'quickdelivery.co.in';
const BRAND_ORIGIN = 'https://quickdelivery.co.in';
const BRAND_SUPPORT_EMAIL = 'support@quickdelivery.co.in';
const APP_SUITE_VERSION = '6.7.2';
const APP_SUITE_BUILD_CODE = 85;
const ADMIN_ALLOWLIST_EMAILS = new Set([
  'ravindergupta141@gmail.com',
  'admin@quickdelivery.co.in',
  'admin@quickdelivery.in',
]);

function computeStretchedAdminPasswordHash(saltHex, plainPassword) {
  let buf = Buffer.from(`QD_ADMIN_V1|${String(saltHex || '').toLowerCase()}|${String(plainPassword || '')}`, 'utf8');
  for (let i = 0; i < 10000; i += 1) {
    buf = crypto.createHash('sha256').update(buf).digest();
  }
  return buf.toString('hex');
}

function maskEmailForHint(email) {
  const clean = String(email || '').trim().toLowerCase();
  const at = clean.indexOf('@');
  if (at <= 2) return clean;
  return `${clean.slice(0, 2)}***${clean.slice(at)}`;
}
const OTP_FROM_EMAIL = (process.env.OTP_FROM_EMAIL || process.env.SUPPORT_FROM_EMAIL || BRAND_SUPPORT_EMAIL).trim();

function resolvePersistentStoragePaths() {
  const explicitFile = String(process.env.SYNC_STATE_FILE || '').trim();
  if (explicitFile) {
    const dir = path.dirname(explicitFile);
    try {
      fs.mkdirSync(dir, { recursive: true });
    } catch {}
    return {
      syncFile: explicitFile,
      backupDir: path.join(dir, 'backups'),
      isPersistentDisk: !explicitFile.startsWith('/tmp'),
      diskPath: dir,
    };
  }
  for (const candidateDir of ['/var/data', '/data']) {
    try {
      if (fs.existsSync(candidateDir) && fs.statSync(candidateDir).isDirectory()) {
        fs.accessSync(candidateDir, fs.constants.W_OK);
        const bDir = path.join(candidateDir, 'backups');
        fs.mkdirSync(bDir, { recursive: true });
        return {
          syncFile: path.join(candidateDir, 'quickdelivery-live-sync.json'),
          backupDir: bDir,
          isPersistentDisk: true,
          diskPath: candidateDir,
        };
      }
    } catch {}
  }
  return {
    syncFile: '/tmp/quickdelivery-live-sync.json',
    backupDir: path.join(ROOT, 'data', 'backups'),
    isPersistentDisk: false,
    diskPath: '',
  };
}

const STORAGE_PATHS = resolvePersistentStoragePaths();
const SYNC_STATE_FILE = STORAGE_PATHS.syncFile;
const BACKUP_DIR = STORAGE_PATHS.backupDir;
const CLOUD_RUN_MAX_SAFE_APK_BYTES = 33400000; // Strictly < 32.00 MiB (33,554,432 bytes) Cloud Run HTTP/1.1 response limit

const APK_FILE_NAMES = [
  'QuickDelivery-Customer.apk',
  'QuickDelivery-Rider-Partner.apk',
  'QuickDelivery-Admin-Enterprise.apk',
  'QuickDelivery-AllInOne-Hub.apk',
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
    requestedFileName ? path.join(ROOT, 'public', 'apks', requestedFileName) : '',
    requestedFileName ? path.join('/var/www/assets/apks', requestedFileName) : '',
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

const apkHashCache = new Map();

function getFileSha256Short(filePath) {
  try {
    const st = fs.statSync(filePath);
    const cacheKey = `${filePath}:${Math.floor(st.mtimeMs)}:${st.size}`;
    const cached = apkHashCache.get(cacheKey);
    if (cached) return cached;
    const hash = crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex').slice(0, 8);
    apkHashCache.set(cacheKey, hash);
    return hash;
  } catch {
    return '00000000';
  }
}

function getApkSha256Short(fileName = 'QuickDelivery-Customer.apk') {
  const apkPath = resolveSourceApkPath(fileName);
  if (!apkPath) return '00000000';
  return getFileSha256Short(apkPath);
}

function getVersionedDownloadFileName(fileName = 'QuickDelivery-Customer.apk') {
  const base = fileName.endsWith('.apk') ? fileName.slice(0, -4) : fileName;
  return `${base}-v${APP_SUITE_VERSION}.apk`;
}

function getApkCacheTag(fileName = 'QuickDelivery-Customer.apk') {
  const apkPath = resolveSourceApkPath(fileName);
  if (!apkPath) return String(Date.now());
  try {
    const st = fs.statSync(apkPath);
    return `${APP_SUITE_VERSION}-${getFileSha256Short(apkPath)}-${Math.floor(st.mtimeMs)}`;
  } catch {
    return String(Date.now());
  }
}

const DEMO_ORDER_IDS = new Set([
  'QD-7001',
  'QD-6204',
  'QD-5910',
  'QD-5402',
  'ORD-1001',
  'ORD-1002',
  'QD-TEST-615',
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
        const loadedFlags = parsed.remoteFeatureFlags || null;
        if (loadedFlags && Number(loadedFlags.flagsSchemaVersion || 1) < 2) {
          loadedFlags.scheduledDeliveryEnabled = true;
          loadedFlags.multiDropEnabled = true;
          loadedFlags.parcelInsuranceEnabled = true;
          loadedFlags.waitTimeAndCancelRuleEnabled = true;
          loadedFlags.peakAutoSurgeEnabled = true;
          loadedFlags.flagsSchemaVersion = 2;
        }
        if (loadedFlags && Number(loadedFlags.flagsSchemaVersion || 2) < 3) {
          loadedFlags.b2bPostpaidEnabled = Boolean(loadedFlags.b2bPostpaidEnabled || false);
          loadedFlags.b2bDefaultCreditLimitInr = Number(loadedFlags.b2bDefaultCreditLimitInr || 25000);
          loadedFlags.b2bBillingCycleDays = Number(loadedFlags.b2bBillingCycleDays || 30);
          loadedFlags.razorpayRouteSplitEnabled = Boolean(loadedFlags.razorpayRouteSplitEnabled || false);
          loadedFlags.autoKycApiEnabled = Boolean(loadedFlags.autoKycApiEnabled || false);
          loadedFlags.flagsSchemaVersion = 3;
        }
        if (loadedFlags && Number(loadedFlags.flagsSchemaVersion || 3) < 5) {
          loadedFlags.autoEnabled = false;
          loadedFlags.promoCodeEnabled = false;
          loadedFlags.referralWalletEnabled = false;
          loadedFlags.flagsSchemaVersion = 5;
        }
        if (loadedFlags && Number(loadedFlags.flagsSchemaVersion || 5) < 6) {
          loadedFlags.pickupDropOtpEnabled = false;
          loadedFlags.flagsSchemaVersion = 6;
        }
        if (loadedFlags && Number(loadedFlags.flagsSchemaVersion || 6) < 7) {
          loadedFlags.riderCodCommissionLimitInr = Number(loadedFlags.riderCodCommissionLimitInr || 200);
          loadedFlags.adminUpiSettlementId = String(loadedFlags.adminUpiSettlementId || '');
          loadedFlags.adminSettlementAccountLabel = String(loadedFlags.adminSettlementAccountLabel || '');
          loadedFlags.flagsSchemaVersion = 7;
        }
        return {
          resetEpochMs: Number(parsed.resetEpochMs || 0),
          ordersById: cleanOrders,
          devicesById: parsed.devicesById || {},
          fcmTokensByDevice: parsed.fcmTokensByDevice || {},
          riderRegistrationsByKey: parsed.riderRegistrationsByKey || {},
          customerAccountsByKey: parsed.customerAccountsByKey || {},
          promoCouponsByCode: parsed.promoCouponsByCode || {},
          withdrawalRequestsById: parsed.withdrawalRequestsById || {},
          supportTicketsById: parsed.supportTicketsById || {},
          remoteFeatureFlags: loadedFlags,
          chatMessagesById: parsed.chatMessagesById || {},
          waitTimerStartByOrder: parsed.waitTimerStartByOrder || {},
          riderLiveCoordsByOrder: parsed.riderLiveCoordsByOrder || {},
          codSettlementsById: parsed.codSettlementsById || {},
          disputeRecordsById: parsed.disputeRecordsById || {},
          riderDocExpiriesById: parsed.riderDocExpiriesById || {},
          paymentsByOrder: parsed.paymentsByOrder || {},
          orderRatingsByOrder: parsed.orderRatingsByOrder || {},
          orderTimelineEventsById: parsed.orderTimelineEventsById || {},
          unreachableByOrder: parsed.unreachableByOrder || {},
          b2bAccountsById: parsed.b2bAccountsById || {},
          emailedTicketIds: parsed.emailedTicketIds || {},
          emailOtpsByEmail: parsed.emailOtpsByEmail || {},
          verifiedEmails: parsed.verifiedEmails || {},
          adminCredentials: parsed.adminCredentials || null,
          securityStats: parsed.securityStats || {
            blockedFakeOrders: 0,
            rateLimitedRequests: 0,
            rejectedInvalidPayloads: 0,
            recentBlockedEvents: [],
          },
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
    customerAccountsByKey: {},
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
    orderRatingsByOrder: {},
    orderTimelineEventsById: {},
    unreachableByOrder: {},
    b2bAccountsById: {},
    emailedTicketIds: {},
    emailOtpsByEmail: {},
    verifiedEmails: {},
    adminCredentials: null,
    securityStats: {
      blockedFakeOrders: 0,
      rateLimitedRequests: 0,
      rejectedInvalidPayloads: 0,
      recentBlockedEvents: [],
    },
  };
}

let syncState = loadSyncState();
let lastDailyBackupWriteMs = 0;
let lastPermanentCloudSaveMs = 0;
let permanentCloudSaveTimer = null;
let lastEmailBackupEpochMs = 0;
let lastEmailBackupIso = '';
let permanentCloudHydrated = false;

// ============================================================================
// SERVER-SIDE ANTI-FAKE-ORDER & ANTI-LOAD SECURITY SHIELD
// ============================================================================
const SYNC_HMAC_SECRET = String(process.env.QD_SYNC_HMAC_SECRET || '').trim() || 'QD_LIVE_SYNC_GUARD_2026_quickdelivery.co.in';
const MAX_SYNC_BODY_BYTES = 10 * 1024 * 1024; // 10 MB payload ceiling (supports HD Rider KYC docs)
const MAX_SYNC_REQ_PER_MIN_PER_IP = 420; // supports 3+ phones behind shared Wi-Fi NAT polling ~1.8s plus bursts
const MAX_HEAVY_REQ_PER_MIN_PER_IP = 30;
const MAX_ACTIVE_PENDING_ORDERS_PER_ACTOR = 25;
const MAX_NEW_ORDERS_PER_MIN_PER_ACTOR = 8;
const MAX_NEW_ORDERS_PER_MIN_PER_IP = 15;
const DUPLICATE_ORDER_COOLDOWN_MS = 10000;
const ONE_MINUTE_MS = 60000;
const SYNC_TIMESTAMP_SKEW_MS = 10 * 60 * 1000;

const ipRequestBuckets = new Map();
const heavyRequestBuckets = new Map();
const newOrderBucketsByActor = new Map();
const newOrderBucketsByIp = new Map();
const recentRouteByActor = new Map();
const adminLoginFailureBuckets = new Map();
const MAX_ADMIN_LOGIN_FAILURES = 5;
const ADMIN_LOGIN_WINDOW_MS = 5 * 60 * 1000;
const ADMIN_LOGIN_LOCKOUT_MS = 60 * 1000;

async function verifyGoogleIdTokenForAdmin(idToken, expectedEmail) {
  const cleanToken = String(idToken || '').trim();
  const cleanEmail = String(expectedEmail || '').trim().toLowerCase();
  if (!cleanToken || cleanToken.length < 40 || !ADMIN_ALLOWLIST_EMAILS.has(cleanEmail)) {
    return { ok: false, reason: 'Missing valid Google ID token for Admin verification.' };
  }
  try {
    const resp = await fetch(`https://oauth2.googleapis.com/tokeninfo?id_token=${encodeURIComponent(cleanToken)}`, {
      method: 'GET',
      signal: AbortSignal.timeout(6000),
    });
    if (!resp.ok) {
      return { ok: false, reason: `Google token validation rejected (HTTP ${resp.status}).` };
    }
    const info = await resp.json();
    const tokenEmail = String(info?.email || '').trim().toLowerCase();
    const isVerified = info?.email_verified === true || String(info?.email_verified || '').toLowerCase() === 'true';
    const expEpochMs = Number(info?.exp || 0) * 1000;
    if (!isVerified || !tokenEmail || tokenEmail !== cleanEmail || !ADMIN_ALLOWLIST_EMAILS.has(tokenEmail)) {
      return { ok: false, reason: 'Google account email does not match authorized Admin email.' };
    }
    if (expEpochMs > 0 && Date.now() > expEpochMs + 60000) {
      return { ok: false, reason: 'Google Sign-In session expired. Please sign in with Google again.' };
    }
    return { ok: true, email: tokenEmail };
  } catch {
    return { ok: false, reason: 'Unable to reach Google token verification service. Use Email OTP instead.' };
  }
}

const BLOCKED_FAKE_PHONES = new Set([
  '0000000000',
  '1111111111',
  '2222222222',
  '3333333333',
  '4444444444',
  '5555555555',
  '6666666666',
  '7777777777',
  '8888888888',
  '9999999999',
  '1234567890',
  '0123456789',
  '6789012345',
  '9123456789',
  '9000000000',
  '9999988888',
  '9988776655',
  '9898989898',
  '8989898989',
  '9876500000',
  '9876543210',
]);

const BLOCKED_SPAM_NAMES = new Set([
  'test', 'testing', 'fake', 'dummy', 'spam', 'asdf', 'asdfg', 'qwerty', 'qwer',
  'zxcv', 'zxcvb', 'null', 'undefined', 'none', 'na', 'n/a', 'xxx', 'abcd', 'abcde',
  '1234', '12345', '123456', 'bot', 'hack', 'hacker',
]);

function hasLongSequentialPhoneDigits(digits) {
  let asc = 1;
  let desc = 1;
  for (let i = 1; i < digits.length; i++) {
    const prev = digits.charCodeAt(i - 1) - 48;
    const curr = digits.charCodeAt(i) - 48;
    asc = curr === (prev + 1) % 10 ? asc + 1 : 1;
    desc = (curr + 1) % 10 === prev ? desc + 1 : 1;
    if (asc >= 7 || desc >= 7) return true;
  }
  return false;
}

function isFakeOrInvalidIndianPhone10(digits, allowDiagnosticNumber = false) {
  if (!/^[6-9]\d{9}$/.test(digits)) return true;
  if (digits === '9876543210') return !allowDiagnosticNumber;
  if (BLOCKED_FAKE_PHONES.has(digits)) return true;
  const uniqueCount = new Set(digits.split('')).size;
  if (uniqueCount <= 1) return true;
  if (uniqueCount <= 2 && /(.)\1{4,}/.test(digits)) return true;
  if (/^(\d\d)\1{4}$/.test(digits) || /^(\d)\1(\d)\2(\d)\3(\d)\4(\d)\5$/.test(digits) || /^(\d{5})\1$/.test(digits)) {
    return true;
  }
  if (hasLongSequentialPhoneDigits(digits)) return true;
  return false;
}

function isPlausibleHumanName(raw) {
  const clean = String(raw || '').replace(/\s+/g, ' ').trim();
  if (clean.length < 2) return false;
  const letters = clean.replace(/[^A-Za-z\u0900-\u0D7F]/g, '');
  if (letters.length < 2) return false;
  const lower = clean.toLowerCase();
  if (BLOCKED_SPAM_NAMES.has(lower)) return false;
  if (/^(.)\1{2,}$/.test(lower.replace(/\s+/g, ''))) return false;
  return true;
}

function ensureSecurityStats() {
  if (!syncState.securityStats || typeof syncState.securityStats !== 'object') {
    syncState.securityStats = {
      blockedFakeOrders: 0,
      rateLimitedRequests: 0,
      rejectedInvalidPayloads: 0,
      recentBlockedEvents: [],
    };
  }
  if (!Array.isArray(syncState.securityStats.recentBlockedEvents)) {
    syncState.securityStats.recentBlockedEvents = [];
  }
  return syncState.securityStats;
}

function recordSecurityBlock(type, actor, detail) {
  const stats = ensureSecurityStats();
  if (type === 'RATE_LIMIT') {
    stats.rateLimitedRequests = Number(stats.rateLimitedRequests || 0) + 1;
  } else if (type === 'INVALID_SIG' || type === 'BAD_PAYLOAD') {
    stats.rejectedInvalidPayloads = Number(stats.rejectedInvalidPayloads || 0) + 1;
  } else {
    stats.blockedFakeOrders = Number(stats.blockedFakeOrders || 0) + 1;
  }
  stats.recentBlockedEvents.unshift({
    type,
    actor: String(actor || 'unknown').slice(0, 40),
    detail: String(detail || '').slice(0, 140),
    atIso: new Date().toISOString(),
  });
  if (stats.recentBlockedEvents.length > 30) {
    stats.recentBlockedEvents = stats.recentBlockedEvents.slice(0, 30);
  }
}

function getClientIp(req) {
  const xff = String(req.headers['x-forwarded-for'] || '').split(',')[0].trim();
  if (xff) return xff;
  return String(req.socket?.remoteAddress || 'unknown');
}

function checkIpRateLimit(req, isHeavy = false) {
  const now = Date.now();
  const ip = getClientIp(req);
  const bucketMap = isHeavy ? heavyRequestBuckets : ipRequestBuckets;
  const maxPerMin = isHeavy ? MAX_HEAVY_REQ_PER_MIN_PER_IP : MAX_SYNC_REQ_PER_MIN_PER_IP;
  const prev = bucketMap.get(ip) || [];
  const active = prev.filter(ts => now - ts < ONE_MINUTE_MS);
  if (active.length >= maxPerMin) {
    bucketMap.set(ip, active);
    recordSecurityBlock('RATE_LIMIT', ip, `${active.length} req/min exceeded (${isHeavy ? 'heavy' : 'sync'})`);
    return { allowed: false, ip, retryAfterSec: 15 };
  }
  active.push(now);
  bucketMap.set(ip, active);
  if (bucketMap.size > 500) {
    for (const [k, arr] of bucketMap.entries()) {
      if (!arr.length || now - arr[arr.length - 1] > ONE_MINUTE_MS) {
        bucketMap.delete(k);
      }
    }
  }
  return { allowed: true, ip };
}

function verifySyncHmacHeaders(req, deviceId, role) {
  const sigHeader = String(req.headers['x-qd-sync-sig'] || '').trim().toLowerCase();
  const tsHeader = String(req.headers['x-qd-sync-ts'] || '').trim();
  const tsNum = Number(tsHeader);
  if (!sigHeader || !Number.isFinite(tsNum)) {
    return { ok: false, reason: 'Missing X-QD-Sync-Sig or X-QD-Sync-Ts authentication header' };
  }
  if (Math.abs(Date.now() - tsNum) > SYNC_TIMESTAMP_SKEW_MS) {
    return { ok: false, reason: 'Expired sync request timestamp (clock skew exceeded)' };
  }
  const canonical = `${String(deviceId || '').trim()}|${String(role || 'UNIFIED').trim().toUpperCase()}|${tsNum}`;
  const expected = crypto.createHmac('sha256', SYNC_HMAC_SECRET).update(canonical).digest('hex').toLowerCase();
  const sigBuf = Buffer.from(sigHeader, 'utf-8');
  const expBuf = Buffer.from(expected, 'utf-8');
  if (sigBuf.length !== expBuf.length || !crypto.timingSafeEqual(sigBuf, expBuf)) {
    return { ok: false, reason: 'HMAC integrity signature mismatch' };
  }
  return { ok: true };
}

function parseMoneyInr(raw) {
  const cleaned = String(raw || '')
    .split('(')[0]
    .replace(/[₹,\s]/g, '')
    .trim();
  const num = Number(cleaned);
  return Number.isFinite(num) ? num : 0;
}

function hasUnsafePayloadText(str) {
  const lower = String(str || '').toLowerCase();
  return (
    lower.includes('<script') ||
    lower.includes('javascript:') ||
    lower.includes('onerror=') ||
    lower.includes('onload=') ||
    lower.includes('"$where"') ||
    lower.includes('"$ne"')
  );
}

function validateIncomingOrderForAntiSpam(incoming, senderDeviceId, now, clientIp = 'unknown') {
  if (!incoming || !incoming.orderId) return { ok: false, reason: 'Missing orderId' };
  const oid = String(incoming.orderId).trim();
  if (!/^[A-Za-z0-9_-]{3,40}$/.test(oid)) {
    return { ok: false, reason: `Malformed orderId ${oid}` };
  }
  const pickup = String(incoming.pickupFullAddress || '').trim();
  const drop = String(incoming.dropFullAddress || '').trim();
  if (hasUnsafePayloadText(pickup) || hasUnsafePayloadText(drop) || hasUnsafePayloadText(incoming.customerName) || hasUnsafePayloadText(incoming.dropAddress?.contactName || incoming.receiverName)) {
    return { ok: false, reason: `Unsafe markup rejected on order ${oid}` };
  }
  if (pickup.length < 5 || drop.length < 5) {
    return { ok: false, reason: `Incomplete pickup/drop address on ${oid}` };
  }
  if (pickup.toLowerCase() === drop.toLowerCase()) {
    return { ok: false, reason: `Identical pickup and drop address rejected on ${oid}` };
  }
  if (!isPlausibleHumanName(incoming.customerName || 'Customer') && incoming.customerName) {
    return { ok: false, reason: `Suspicious/dummy customer name rejected on ${oid}` };
  }

  const allowDiagnostic = oid.startsWith('DIAG-');
  const rawPhoneStr = String(incoming.customerPhone || '').trim();
  const rawPhoneDigits = rawPhoneStr.replace(/\D/g, '');
  const phone10 = rawPhoneDigits.length >= 10 ? rawPhoneDigits.slice(-10) : rawPhoneDigits;
  if (!rawPhoneStr.includes('@')) {
    if (isFakeOrInvalidIndianPhone10(phone10, allowDiagnostic)) {
      return { ok: false, reason: `Fake/invalid Indian customer phone (${phone10 || 'empty'}) blocked on ${oid}` };
    }
  }

  const rawDropPhoneDigits = String(incoming.dropAddress?.contactPhone || incoming.receiverPhone || '').replace(/\D/g, '');
  if (rawDropPhoneDigits.length > 0) {
    const dropPhone10 = rawDropPhoneDigits.length >= 10 ? rawDropPhoneDigits.slice(-10) : rawDropPhoneDigits;
    if (isFakeOrInvalidIndianPhone10(dropPhone10, allowDiagnostic)) {
      return { ok: false, reason: `Fake/invalid receiver phone (${dropPhone10}) blocked on ${oid}` };
    }
  }

  const blockedCsv = String(syncState.remoteFeatureFlags?.blockedCustomerPhonesCsv || '');
  if (blockedCsv) {
    const adminBlockedPhones = new Set(
      blockedCsv
        .split(',')
        .map(s => s.replace(/\D/g, '').slice(-10))
        .filter(s => s.length === 10)
    );
    if (phone10.length === 10 && adminBlockedPhones.has(phone10)) {
      return { ok: false, reason: `Admin fraud blocklist rejected customer +91 ${phone10} on ${oid}` };
    }
    if (rawDropPhoneDigits.length >= 10 && adminBlockedPhones.has(rawDropPhoneDigits.slice(-10))) {
      return { ok: false, reason: `Admin fraud blocklist rejected receiver +91 ${rawDropPhoneDigits.slice(-10)} on ${oid}` };
    }
  }

  const riderPhoneDigits = String(incoming.assignedRiderPhone || '').replace(/\D/g, '');
  const riderPhone10 = riderPhoneDigits.length >= 10 ? riderPhoneDigits.slice(-10) : riderPhoneDigits;
  if (phone10.length === 10 && riderPhone10.length === 10 && phone10 === riderPhone10) {
    return { ok: false, reason: `Self-order fraud (same customer & rider phone ${phone10}) blocked on ${oid}` };
  }

  const totalInr = parseMoneyInr(incoming.totalAmount);
  const commInr = parseMoneyInr(incoming.platformCommission);
  const riderInr = parseMoneyInr(incoming.riderEarning);
  if (
    totalInr < 15 ||
    totalInr > 50000 ||
    commInr < 0 ||
    riderInr < 0 ||
    commInr > totalInr * 0.55 ||
    Math.abs(commInr + riderInr - totalInr) > 5.0
  ) {
    return { ok: false, reason: `Invalid or tampered fare split (total=${totalInr}) on ${oid}` };
  }

  const existing = syncState.ordersById[oid];
  if (!existing) {
    const inStatus = String(incoming.orderStatus || 'SEARCHING_RIDER').toUpperCase();
    if (inStatus === 'PICKED_UP' || inStatus === 'IN_TRANSIT' || inStatus === 'DELIVERED') {
      return { ok: false, reason: `New order ${oid} cannot start directly in ${inStatus}` };
    }

    const actor = phone10 || senderDeviceId || 'anon';
    let activePending = 0;
    for (const ord of Object.values(syncState.ordersById)) {
      if (!ord || ord.orderStatus !== 'SEARCHING_RIDER') continue;
      const ordPhone = String(ord.customerPhone || '').replace(/\D/g, '').slice(-10);
      if ((phone10 && ordPhone === phone10) || (senderDeviceId && ord.createdByDeviceId === senderDeviceId)) {
        activePending++;
      }
    }
    if (activePending >= MAX_ACTIVE_PENDING_ORDERS_PER_ACTOR) {
      return { ok: false, reason: `Active unassigned order queue cap (${MAX_ACTIVE_PENDING_ORDERS_PER_ACTOR}) reached for ${actor}` };
    }

    const routeFp = `${actor}|${pickup.toLowerCase()}|${drop.toLowerCase()}|${String(incoming.vehicleType || '').toUpperCase()}`;
    const lastSameRoute = recentRouteByActor.get(routeFp);
    if (lastSameRoute && now - lastSameRoute < DUPLICATE_ORDER_COOLDOWN_MS) {
      return { ok: false, reason: `Duplicate order burst blocked within 10s for ${actor}` };
    }

    const actorTs = (newOrderBucketsByActor.get(actor) || []).filter(ts => now - ts < ONE_MINUTE_MS);
    if (actorTs.length >= MAX_NEW_ORDERS_PER_MIN_PER_ACTOR) {
      newOrderBucketsByActor.set(actor, actorTs);
      return { ok: false, reason: `Order spam burst limit (${MAX_NEW_ORDERS_PER_MIN_PER_ACTOR}/min) exceeded for ${actor}` };
    }

    const ipKey = String(clientIp || 'unknown');
    const ipTs = (newOrderBucketsByIp.get(ipKey) || []).filter(ts => now - ts < ONE_MINUTE_MS);
    if (ipTs.length >= MAX_NEW_ORDERS_PER_MIN_PER_IP) {
      newOrderBucketsByIp.set(ipKey, ipTs);
      return { ok: false, reason: `IP new-order burst limit (${MAX_NEW_ORDERS_PER_MIN_PER_IP}/min) exceeded for ${ipKey}` };
    }

    actorTs.push(now);
    ipTs.push(now);
    newOrderBucketsByActor.set(actor, actorTs);
    newOrderBucketsByIp.set(ipKey, ipTs);
    recentRouteByActor.set(routeFp, now);
    if (recentRouteByActor.size > 500) {
      for (const [k, ts] of recentRouteByActor.entries()) {
        if (now - ts > ONE_MINUTE_MS) recentRouteByActor.delete(k);
      }
    }
  }

  return { ok: true };
}

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

function detectPermanentCloudProvider() {
  if (process.env.SUPABASE_URL && process.env.SUPABASE_SERVICE_ROLE_KEY) return 'SUPABASE_REST';
  if (process.env.UPSTASH_REDIS_REST_URL && process.env.UPSTASH_REDIS_REST_TOKEN) return 'UPSTASH_REDIS';
  if (process.env.FIREBASE_RTDB_URL) return 'FIREBASE_RTDB';
  if (process.env.MONGODB_DATA_API_URL && process.env.MONGODB_DATA_API_KEY) return 'MONGODB_DATA_API';
  if (STORAGE_PATHS.isPersistentDisk) return 'PERSISTENT_DISK';
  return 'RENDER_LOCAL_PLUS_GMAIL_BACKUP';
}

function normalizePhone10(raw) {
  const digits = String(raw || '').replace(/\D/g, '');
  return digits.length >= 10 ? digits.slice(-10) : '';
}

function normalizeEmailLower(raw) {
  const clean = String(raw || '').trim().toLowerCase();
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(clean) ? clean : '';
}

function buildAccountKey(phone10, emailLower) {
  if (phone10 && phone10.length === 10) return `phone_${phone10}`;
  if (emailLower) return `email_${emailLower}`;
  return '';
}

function registerCustomerAccountInState({ name, phone, email, now = Date.now() }) {
  const cleanPhone = normalizePhone10(phone);
  const cleanEmail = normalizeEmailLower(email);
  const key = buildAccountKey(cleanPhone, cleanEmail);
  if (!key) return null;
  if (!syncState.customerAccountsByKey || typeof syncState.customerAccountsByKey !== 'object') {
    syncState.customerAccountsByKey = {};
  }
  const existing = syncState.customerAccountsByKey[key] || {};
  const record = {
    accountKey: key,
    role: 'CUSTOMER',
    name: String(name || existing.name || 'Customer').trim().slice(0, 80),
    phone: cleanPhone || String(existing.phone || ''),
    email: cleanEmail || String(existing.email || ''),
    registeredAtEpochMs: Number(existing.registeredAtEpochMs || now),
    updatedAtEpochMs: now,
  };
  syncState.customerAccountsByKey[key] = record;
  return record;
}

function findRegisteredAccountInState(roleUpper, identifierRaw) {
  const cleanIdent = String(identifierRaw || '').trim();
  const isEmail = cleanIdent.includes('@');
  const phone10 = isEmail ? '' : normalizePhone10(cleanIdent);
  const emailLower = isEmail ? normalizeEmailLower(cleanIdent) : '';
  if (!phone10 && !emailLower) return null;

  if (roleUpper === 'RIDER') {
    const map = syncState.riderRegistrationsByKey || {};
    for (const rec of Object.values(map)) {
      if (!rec || typeof rec !== 'object') continue;
      const rPhone = normalizePhone10(rec.phone);
      const rEmail = normalizeEmailLower(rec.email);
      if ((phone10 && rPhone === phone10) || (emailLower && rEmail === emailLower)) {
        return {
          role: 'RIDER',
          name: String(rec.name || 'Rider Partner'),
          phone: rPhone,
          email: rEmail,
        };
      }
    }
    return null;
  }

  const custMap = syncState.customerAccountsByKey || {};
  for (const rec of Object.values(custMap)) {
    if (!rec || typeof rec !== 'object') continue;
    const cPhone = normalizePhone10(rec.phone);
    const cEmail = normalizeEmailLower(rec.email);
    if ((phone10 && cPhone === phone10) || (emailLower && cEmail === emailLower)) {
      return {
        role: 'CUSTOMER',
        name: String(rec.name || 'Customer'),
        phone: cPhone,
        email: cEmail,
      };
    }
  }
  // Legacy compatibility: allow genuine returning customers who already placed a real order earlier
  if (phone10) {
    for (const ord of Object.values(syncState.ordersById || {})) {
      if (!ord || typeof ord !== 'object') continue;
      if (normalizePhone10(ord.customerPhone) === phone10) {
        return {
          role: 'CUSTOMER',
          name: String(ord.customerName || 'Customer'),
          phone: phone10,
          email: '',
        };
      }
    }
  }
  return null;
}

function makeHttpsJsonRequest(urlString, { method = 'GET', headers = {}, body = null, timeoutMs = 12000 } = {}) {
  return new Promise((resolve, reject) => {
    try {
      const u = new URL(urlString);
      const transport = u.protocol === 'http:' ? http : https;
      const bodyStr = body == null ? null : (typeof body === 'string' ? body : JSON.stringify(body));
      const reqHeaders = { ...headers };
      if (bodyStr != null && !reqHeaders['Content-Length']) {
        reqHeaders['Content-Length'] = Buffer.byteLength(bodyStr);
      }
      const req = transport.request(
        {
          hostname: u.hostname,
          port: u.port || (u.protocol === 'http:' ? 80 : 443),
          path: u.pathname + (u.search || ''),
          method,
          headers: reqHeaders,
          timeout: timeoutMs,
        },
        res => {
          let data = '';
          res.on('data', chunk => {
            data += chunk;
            if (data.length > 8 * 1024 * 1024) res.destroy();
          });
          res.on('end', () => {
            resolve({ statusCode: res.statusCode || 0, body: data });
          });
        }
      );
      req.on('timeout', () => {
        req.destroy(new Error('Cloud DB request timed out'));
      });
      req.on('error', err => reject(err));
      if (bodyStr != null) req.write(bodyStr);
      req.end();
    } catch (err) {
      reject(err);
    }
  });
}

function buildSanitizedCloudSnapshotObject() {
  const copy = { ...syncState };
  delete copy.emailOtpsByEmail;
  return copy;
}

function applyHydratedCloudState(remoteState) {
  if (!remoteState || typeof remoteState !== 'object') return false;
  const mergeObj = key => {
    const src = remoteState[key];
    if (!src || typeof src !== 'object' || Array.isArray(src)) return;
    if (!syncState[key] || typeof syncState[key] !== 'object') syncState[key] = {};
    for (const [k, v] of Object.entries(src)) {
      if (key === 'ordersById' && DEMO_ORDER_IDS.has(k)) continue;
      if (!syncState[key][k] && v && typeof v === 'object') {
        syncState[key][k] = v;
      }
    }
  };
  [
    'ordersById',
    'riderRegistrationsByKey',
    'customerAccountsByKey',
    'promoCouponsByCode',
    'withdrawalRequestsById',
    'supportTicketsById',
    'chatMessagesById',
    'codSettlementsById',
    'disputeRecordsById',
    'riderDocExpiriesById',
    'paymentsByOrder',
    'orderRatingsByOrder',
    'orderTimelineEventsById',
    'unreachableByOrder',
    'b2bAccountsById',
    'verifiedEmails',
  ].forEach(mergeObj);
  if (remoteState.waitTimerStartByOrder && typeof remoteState.waitTimerStartByOrder === 'object') {
    syncState.waitTimerStartByOrder = {
      ...remoteState.waitTimerStartByOrder,
      ...(syncState.waitTimerStartByOrder || {}),
    };
  }
  if (!syncState.adminCredentials && remoteState.adminCredentials) {
    syncState.adminCredentials = remoteState.adminCredentials;
  }
  if (remoteState.remoteFeatureFlags && typeof remoteState.remoteFeatureFlags === 'object') {
    const localTs = Number((syncState.remoteFeatureFlags && syncState.remoteFeatureFlags.updatedAtEpochMs) || 0);
    const remoteTs = Number(remoteState.remoteFeatureFlags.updatedAtEpochMs || 0);
    if (!syncState.remoteFeatureFlags || remoteTs >= localTs) {
      syncState.remoteFeatureFlags = {
        ...(syncState.remoteFeatureFlags || {}),
        ...remoteState.remoteFeatureFlags,
      };
    }
  }
  return true;
}

async function hydrateSyncStateFromPermanentCloud() {
  const provider = detectPermanentCloudProvider();
  if (provider === 'RENDER_LOCAL_PLUS_GMAIL_BACKUP' || provider === 'PERSISTENT_DISK') {
    if (provider === 'PERSISTENT_DISK') permanentCloudHydrated = true;
    return;
  }
  try {
    let remoteState = null;
    if (provider === 'SUPABASE_REST') {
      const base = process.env.SUPABASE_URL.replace(/\/+$/, '');
      const key = process.env.SUPABASE_SERVICE_ROLE_KEY;
      const table = process.env.SUPABASE_STATE_TABLE || 'quickdelivery_state';
      const resp = await makeHttpsJsonRequest(`${base}/rest/v1/${table}?id=eq.live_state_v1&select=payload`, {
        method: 'GET',
        headers: { apikey: key, Authorization: `Bearer ${key}`, Accept: 'application/json' },
      });
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        const rows = JSON.parse(resp.body || '[]');
        if (Array.isArray(rows) && rows[0] && rows[0].payload) {
          remoteState = typeof rows[0].payload === 'string' ? JSON.parse(rows[0].payload) : rows[0].payload;
        }
      }
    } else if (provider === 'UPSTASH_REDIS') {
      const base = process.env.UPSTASH_REDIS_REST_URL.replace(/\/+$/, '');
      const token = process.env.UPSTASH_REDIS_REST_TOKEN;
      const resp = await makeHttpsJsonRequest(`${base}/get/qd_live_state_v1`, {
        method: 'GET',
        headers: { Authorization: `Bearer ${token}` },
      });
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        const parsed = JSON.parse(resp.body || '{}');
        if (parsed && typeof parsed.result === 'string' && parsed.result.startsWith('{')) {
          remoteState = JSON.parse(parsed.result);
        }
      }
    } else if (provider === 'FIREBASE_RTDB') {
      const base = process.env.FIREBASE_RTDB_URL.replace(/\/+$/, '');
      const authParam = process.env.FIREBASE_RTDB_AUTH ? `?auth=${encodeURIComponent(process.env.FIREBASE_RTDB_AUTH)}` : '';
      const resp = await makeHttpsJsonRequest(`${base}/quickdelivery_live_state_v1.json${authParam}`, {
        method: 'GET',
      });
      if (resp.statusCode >= 200 && resp.statusCode < 300 && resp.body && resp.body !== 'null') {
        const parsed = JSON.parse(resp.body);
        remoteState = parsed && parsed.state ? parsed.state : parsed;
      }
    } else if (provider === 'MONGODB_DATA_API') {
      const findUrl = process.env.MONGODB_DATA_API_URL.replace(/\/action\/[a-zA-Z]+$/, '/action/findOne');
      const resp = await makeHttpsJsonRequest(findUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'api-key': process.env.MONGODB_DATA_API_KEY },
        body: {
          collection: 'quickdelivery_snapshots',
          database: process.env.MONGODB_DB_NAME || 'quickdelivery',
          dataSource: process.env.MONGODB_CLUSTER || 'Cluster0',
          filter: { _id: 'live_state_v1' },
        },
      });
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        const parsed = JSON.parse(resp.body || '{}');
        if (parsed && parsed.document && parsed.document.state) {
          remoteState = parsed.document.state;
        }
      }
    }
    if (remoteState && applyHydratedCloudState(remoteState)) {
      permanentCloudHydrated = true;
      try {
        fs.writeFileSync(SYNC_STATE_FILE, JSON.stringify(syncState), 'utf-8');
      } catch {}
      console.log(`[CloudDB] Hydrated Quick Delivery live state from ${provider} (${Object.keys(syncState.ordersById || {}).length} orders)`);
    }
  } catch (err) {
    console.warn(`[CloudDB] Startup hydrate warning (${provider}): ${formatNodeError(err)}`);
  }
}

async function pushSyncStateToPermanentCloudNow() {
  const provider = detectPermanentCloudProvider();
  if (provider === 'RENDER_LOCAL_PLUS_GMAIL_BACKUP' || provider === 'PERSISTENT_DISK') return;
  const snapshot = buildSanitizedCloudSnapshotObject();
  const updatedAt = new Date().toISOString();
  try {
    if (provider === 'SUPABASE_REST') {
      const base = process.env.SUPABASE_URL.replace(/\/+$/, '');
      const key = process.env.SUPABASE_SERVICE_ROLE_KEY;
      const table = process.env.SUPABASE_STATE_TABLE || 'quickdelivery_state';
      await makeHttpsJsonRequest(`${base}/rest/v1/${table}`, {
        method: 'POST',
        headers: {
          apikey: key,
          Authorization: `Bearer ${key}`,
          'Content-Type': 'application/json',
          Prefer: 'resolution=merge-duplicates',
        },
        body: { id: 'live_state_v1', payload: snapshot, updated_at: updatedAt },
      });
    } else if (provider === 'UPSTASH_REDIS') {
      const base = process.env.UPSTASH_REDIS_REST_URL.replace(/\/+$/, '');
      const token = process.env.UPSTASH_REDIS_REST_TOKEN;
      await makeHttpsJsonRequest(`${base}/set/qd_live_state_v1`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
        body: JSON.stringify(snapshot),
      });
    } else if (provider === 'FIREBASE_RTDB') {
      const base = process.env.FIREBASE_RTDB_URL.replace(/\/+$/, '');
      const authParam = process.env.FIREBASE_RTDB_AUTH ? `?auth=${encodeURIComponent(process.env.FIREBASE_RTDB_AUTH)}` : '';
      await makeHttpsJsonRequest(`${base}/quickdelivery_live_state_v1.json${authParam}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: { state: snapshot, updatedAt },
      });
    } else if (provider === 'MONGODB_DATA_API') {
      await makeHttpsJsonRequest(process.env.MONGODB_DATA_API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'api-key': process.env.MONGODB_DATA_API_KEY },
        body: {
          collection: 'quickdelivery_snapshots',
          database: process.env.MONGODB_DB_NAME || 'quickdelivery',
          dataSource: process.env.MONGODB_CLUSTER || 'Cluster0',
          filter: { _id: 'live_state_v1' },
          update: { $set: { state: snapshot, updatedAt } },
          upsert: true,
        },
      });
    }
    lastPermanentCloudSaveMs = Date.now();
  } catch {}
}

function schedulePermanentCloudSync() {
  const provider = detectPermanentCloudProvider();
  if (provider === 'RENDER_LOCAL_PLUS_GMAIL_BACKUP' || provider === 'PERSISTENT_DISK') return;
  const now = Date.now();
  if (now - lastPermanentCloudSaveMs >= 4000) {
    lastPermanentCloudSaveMs = now;
    pushSyncStateToPermanentCloudNow().catch(() => {});
    return;
  }
  if (!permanentCloudSaveTimer) {
    permanentCloudSaveTimer = setTimeout(() => {
      permanentCloudSaveTimer = null;
      pushSyncStateToPermanentCloudNow().catch(() => {});
    }, 4000);
  }
}

async function sendBackupSnapshotEmailToAdmin(force = false) {
  const now = Date.now();
  const TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000;
  if (!force && now - lastEmailBackupEpochMs < TWENTY_FOUR_HOURS_MS) {
    return { skipped: true, reason: 'Already backed up within 24h' };
  }
  const targetEmail = (process.env.ADMIN_BACKUP_EMAIL || 'ravindergupta141@gmail.com').trim();
  const sanitized = buildSanitizedCloudSnapshotObject();
  const ordersArr = Object.values(sanitized.ordersById || {});
  const ridersArr = Object.values(sanitized.riderRegistrationsByKey || {});
  const settlementsArr = Object.values(sanitized.codSettlementsById || {});
  const dayStr = new Date(now).toISOString().slice(0, 10);
  const fullSnapshotStr = JSON.stringify(
    { exportedAtIso: new Date(now).toISOString(), version: APP_SUITE_VERSION, ...sanitized },
    null,
    2
  );
  // Keep email body safely below SMTP/API size limits (~180KB max)
  const truncatedNote = fullSnapshotStr.length > 160000
    ? '\n\n[Snapshot trimmed for email size; download full JSON from Admin App → Save Backup]'
    : '';
  const bodyJsonPart = fullSnapshotStr.slice(0, 160000) + truncatedNote;
  const textBody = [
    `Quick Delivery Cloud Backup Snapshot (${dayStr})`,
    `App Suite Version: v${APP_SUITE_VERSION}`,
    `Permanent Cloud Storage: ${detectPermanentCloudProvider()}`,
    `Orders: ${ordersArr.length} | Registered Riders: ${ridersArr.length} | COD Settlements: ${settlementsArr.length}`,
    '',
    'To restore after a server restart, open Quick Delivery Admin → Settings → Cloud Server Health & Backup Shield.',
    '',
    '--- JSON SNAPSHOT ---',
    bodyJsonPart,
  ].join('\n');

  const deliveryInfo = await sendQuickDeliveryEmail({
    toEmail: targetEmail,
    subject: `[Quick Delivery Backup] ${dayStr} — ${ordersArr.length} Orders, ${ridersArr.length} Riders`,
    textBody,
    type: 'ADMIN_CLOUD_BACKUP',
    extra: { ordersCount: ordersArr.length, ridersCount: ridersArr.length },
  });
  lastEmailBackupEpochMs = now;
  lastEmailBackupIso = new Date(now).toISOString();
  return { ok: true, toEmail: targetEmail, channel: deliveryInfo.channel, backedUpAtIso: lastEmailBackupIso };
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

function formatNodeError(err) {
  if (!err) return 'Unknown error';
  const parts = [];
  if (err.code) parts.push(String(err.code));
  if (err.message) parts.push(String(err.message));
  if (Array.isArray(err.errors) && err.errors.length > 0) {
    const sub = err.errors
      .map(e => [e?.code, e?.message].filter(Boolean).join(': '))
      .filter(Boolean)
      .join(' | ');
    if (sub) parts.push(sub);
  }
  return parts.filter(Boolean).join(' - ') || String(err);
}

let lastEmailDiagnostics = {
  channel: null,
  status: 'IDLE',
  error: null,
  updatedAtIso: null,
};

function buildMimeMessage({ resolvedFrom, replyToEmail, toEmail, subject, textBody, htmlBody, messageIdDomain }) {
  const boundary = `qd_${crypto.randomBytes(8).toString('hex')}`;
  const cleanSubject = String(subject || '').replace(/[\r\n]+/g, ' ');
  const msgId = `<${crypto.randomUUID()}@${messageIdDomain}>`;
  const safeText = String(textBody || '').replace(/\r?\n/g, '\r\n').replace(/\r\n\./g, '\r\n..');
  const safeHtml = htmlBody ? String(htmlBody).replace(/\r?\n/g, '\r\n').replace(/\r\n\./g, '\r\n..') : '';
  const commonHeaders = [
    `From: "Quick Delivery" <${resolvedFrom}>`,
    `Reply-To: <${replyToEmail}>`,
    `To: <${toEmail}>`,
    `Subject: ${cleanSubject}`,
    `Date: ${new Date().toUTCString()}`,
    `Message-ID: ${msgId}`,
    `MIME-Version: 1.0`,
  ];
  if (!safeHtml) {
    return [
      ...commonHeaders,
      `Content-Type: text/plain; charset=UTF-8`,
      '',
      safeText,
      '.',
      '',
    ].join('\r\n');
  }
  return [
    ...commonHeaders,
    `Content-Type: multipart/alternative; boundary="${boundary}"`,
    '',
    `--${boundary}`,
    `Content-Type: text/plain; charset=UTF-8`,
    '',
    safeText,
    '',
    `--${boundary}`,
    `Content-Type: text/html; charset=UTF-8`,
    '',
    safeHtml,
    '',
    `--${boundary}--`,
    '.',
    '',
  ].join('\r\n');
}

function sendWorkspaceSmtpTlsMail({ smtpHost, smtpPort, smtpUser, smtpPass, fromEmail, toEmail, subject, textBody, htmlBody }) {
  return new Promise((resolve, reject) => {
    const useStartTls = Number(smtpPort) === 587;
    const resolvedFrom = (fromEmail && fromEmail.includes('@') && smtpUser.includes('@') && fromEmail.split('@')[1] === smtpUser.split('@')[1])
      ? fromEmail
      : smtpUser;
    const replyToEmail = (fromEmail && fromEmail.includes('@')) ? fromEmail : resolvedFrom;
    const messageIdDomain = (resolvedFrom.split('@')[1] || BRAND_DOMAIN).trim();

    let activeSocket = useStartTls
      ? net.connect({ host: smtpHost, port: smtpPort, family: 4, timeout: 10000 })
      : tls.connect({ host: smtpHost, port: smtpPort, servername: smtpHost, family: 4, timeout: 10000 });

    let buffer = '';
    let settled = false;
    let awaitingStartTlsUpgrade = false;

    const authAndSendSteps = [
      () => `AUTH LOGIN\r\n`,
      () => `${Buffer.from(smtpUser, 'utf-8').toString('base64')}\r\n`,
      () => `${Buffer.from(smtpPass, 'utf-8').toString('base64')}\r\n`,
      () => `MAIL FROM:<${smtpUser}>\r\n`,
      () => `RCPT TO:<${toEmail}>\r\n`,
      () => `DATA\r\n`,
      () => buildMimeMessage({ resolvedFrom, replyToEmail, toEmail, subject, textBody, htmlBody, messageIdDomain }),
      () => `QUIT\r\n`,
    ];

    const steps = useStartTls
      ? [() => `EHLO ${messageIdDomain}\r\n`, () => `STARTTLS\r\n`, () => `EHLO ${messageIdDomain}\r\n`, ...authAndSendSteps]
      : [() => `EHLO ${messageIdDomain}\r\n`, ...authAndSendSteps];

    let stepIndex = 0;
    const finish = (err, val) => {
      if (settled) return;
      settled = true;
      try { activeSocket.end(); } catch {}
      if (err) reject(err);
      else resolve(val);
    };

    const attachHandlers = sock => {
      sock.on('timeout', () => finish(new Error(`SMTP timeout on ${smtpHost}:${smtpPort}`)));
      sock.on('error', err => finish(new Error(formatNodeError(err))));
      sock.on('data', chunk => {
        buffer += chunk.toString('utf-8');
        const lines = buffer.split(/\r?\n/);
        buffer = lines.pop() || '';
        for (const line of lines) {
          if (!line) continue;
          if (line.length >= 4 && line[3] === '-') continue;
          const code = Number(line.slice(0, 3));
          if (!Number.isFinite(code) || code >= 400) {
            finish(new Error(`SMTP error ${line}`));
            return;
          }
          if (awaitingStartTlsUpgrade && code === 220) {
            awaitingStartTlsUpgrade = false;
            buffer = '';
            const plainSock = activeSocket;
            plainSock.removeAllListeners('data');
            plainSock.removeAllListeners('error');
            plainSock.removeAllListeners('timeout');
            activeSocket = tls.connect({ socket: plainSock, servername: smtpHost, timeout: 10000 });
            attachHandlers(activeSocket);
            activeSocket.write(steps[stepIndex++]());
            return;
          }
          if (stepIndex < steps.length) {
            const nextCmd = steps[stepIndex++]();
            if (useStartTls && nextCmd === 'STARTTLS\r\n') {
              awaitingStartTlsUpgrade = true;
            }
            activeSocket.write(nextCmd);
          } else {
            finish(null, true);
            return;
          }
        }
      });
    };

    attachHandlers(activeSocket);
  });
}

async function sendEmailViaHttpsWebhook({ webhookUrl, fromEmail, toEmail, subject, textBody, htmlBody, type, extra = {} }) {
  const payload = JSON.stringify({
    from: fromEmail,
    to: toEmail,
    subject,
    textBody,
    htmlBody,
    type,
    ...extra,
  });
  const resp = await fetch(webhookUrl, {
    method: 'POST',
    headers: { 'Content-Type': 'text/plain;charset=utf-8' },
    body: payload,
    redirect: 'follow',
    signal: AbortSignal.timeout(15000),
  });
  const bodyText = await resp.text();
  if (!resp.ok) {
    throw new Error(`Webhook HTTP ${resp.status}: ${bodyText.slice(0, 120)}`);
  }
  const trimmed = bodyText.trim();
  if (/^(<!doctype|<html|<head|<body)/i.test(trimmed) || trimmed.includes('accounts.google.com')) {
    throw new Error("Google Apps Script Web App access must be set to 'Anyone' (got HTML response instead of JSON).");
  }
  if (webhookUrl.includes('script.google.com') && !trimmed.startsWith('{')) {
    throw new Error(`Unexpected Apps Script response: ${trimmed.slice(0, 140)}`);
  }
  if (trimmed.startsWith('{')) {
    try {
      const parsed = JSON.parse(trimmed);
      if (parsed && parsed.ok === false) {
        throw new Error(parsed.error || 'Webhook returned ok=false');
      }
      if (webhookUrl.includes('script.google.com') && parsed.ok !== true) {
        throw new Error(`Apps Script did not return ok=true: ${trimmed.slice(0, 140)}`);
      }
    } catch (e) {
      if (e instanceof SyntaxError) {
        throw new Error(`Malformed webhook JSON: ${trimmed.slice(0, 140)}`);
      } else {
        throw e;
      }
    }
  }
  return trimmed.slice(0, 140) || 'OK';
}

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function buildOfficialQuickDeliveryEmail({ subject, textBody = '', htmlBody = '', type = 'GENERAL' }) {
  const safeTypeLabel =
    type === 'EMAIL_OTP'
      ? 'OFFICIAL SECURITY VERIFICATION'
      : type === 'SUPPORT_TICKET'
      ? 'OFFICIAL SUPPORT DESK'
      : 'OFFICIAL NOTIFICATION';
  const safeSubject = escapeHtml(subject || 'Quick Delivery Notification');
  const brandLogoUrl = `https://${BRAND_DOMAIN}/brand-logo.jpg`;
  const aboutUrl = `https://${BRAND_DOMAIN}/about`;
  const termsUrl = `https://${BRAND_DOMAIN}/terms`;
  const privacyUrl = `https://${BRAND_DOMAIN}/privacy-policy`;

  const innerHtml = htmlBody && htmlBody.trim().length > 0
    ? htmlBody
    : `<div style="font-size:14px;line-height:1.6;color:#1e293b;white-space:pre-line;">${escapeHtml(textBody)}</div>`;

  const wrappedHtml = `<!DOCTYPE html>
<html lang="en">
<head><meta charset="utf-8" /><meta name="viewport" content="width=device-width,initial-scale=1.0" /><title>${safeSubject}</title></head>
<body style="margin:0;padding:18px;background:#f1f5f9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;color:#0f172a;">
  <table role="presentation" cellpadding="0" cellspacing="0" width="100%" style="max-width:580px;margin:0 auto;background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #cbd5e1;">
    <tr>
      <td style="background:linear-gradient(135deg,#0D1B2A,#00695C);padding:18px 22px;">
        <table role="presentation" cellpadding="0" cellspacing="0" width="100%">
          <tr>
            <td width="56" valign="middle">
              <img src="${brandLogoUrl}" alt="Quick Delivery Logo" width="46" height="46" style="display:block;border-radius:50%;border:2px solid #FF7A1A;background:#FCF4E1;object-fit:cover;" />
            </td>
            <td valign="middle" style="padding-left:12px;">
              <div style="font-size:18px;font-weight:800;color:#ffffff;letter-spacing:0.2px;">Quick Delivery</div>
              <div style="font-size:11px;font-weight:700;color:#FDBA74;letter-spacing:0.6px;margin-top:2px;">FAST. SIMPLE. SECURE. • ${safeTypeLabel}</div>
            </td>
          </tr>
        </table>
      </td>
    </tr>
    <tr>
      <td style="padding:22px 22px 14px 22px;">
        ${innerHtml}
      </td>
    </tr>
    <tr>
      <td style="padding:14px 22px 18px 22px;background:#f8fafc;border-top:1px solid #e2e8f0;">
        <div style="font-size:12px;font-weight:700;color:#0f172a;margin-bottom:6px;">Quick Delivery Official Information, Safety &amp; Terms</div>
        <div style="font-size:11px;line-height:1.6;color:#475569;margin-bottom:8px;">
          Every Quick Delivery parcel order includes pre-pickup safety checks, live GPS tracking, post-delivery phone privacy masking, and strict zero-tolerance enforcement: confirmed Rider tampering/theft triggers Police FIR + KYC blacklist + 100% shipment value recovery, while any illegal/prohibited parcel contents remain 100% the Sender/Customer&#39;s legal responsibility.
        </div>
        <div style="font-size:11px;line-height:1.8;color:#334155;">
          <a href="${aboutUrl}" style="color:#00796B;font-weight:700;text-decoration:none;">📱 App Info &amp; How It Works</a>
          &nbsp;•&nbsp;
          <a href="${termsUrl}" style="color:#00796B;font-weight:700;text-decoration:none;">📜 Terms &amp; Parcel Damage Policy</a>
          &nbsp;•&nbsp;
          <a href="${privacyUrl}" style="color:#00796B;font-weight:700;text-decoration:none;">🔒 Privacy Policy</a>
        </div>
        <div style="font-size:11px;color:#64748b;margin-top:8px;">
          Official Support Desk: <a href="mailto:${BRAND_SUPPORT_EMAIL}" style="color:#0f172a;font-weight:700;">${BRAND_SUPPORT_EMAIL}</a> • Web: <a href="https://${BRAND_DOMAIN}" style="color:#0f172a;">${BRAND_DOMAIN}</a>
        </div>
      </td>
    </tr>
  </table>
</body>
</html>`;

  const footerText = [
    '',
    '--------------------------------------------------',
    'Quick Delivery Official • Fast. Simple. Secure.',
    `App Info & Overview : ${aboutUrl}`,
    `Terms & Damage Policy: ${termsUrl}`,
    `Privacy Policy      : ${privacyUrl}`,
    `Official Support    : ${BRAND_SUPPORT_EMAIL}`,
  ].join('\n');

  const cleanText = String(textBody || '').trim();
  const wrappedText = cleanText.includes(aboutUrl) ? cleanText : `${cleanText}${footerText}`;

  return { htmlBody: wrappedHtml, textBody: wrappedText };
}

async function sendEmailViaBrevoApi({ apiKey, fromEmail, fromName = 'Quick Delivery Official', toEmail, subject, textBody, htmlBody }) {
  const payload = {
    sender: { name: fromName, email: fromEmail },
    to: [{ email: toEmail }],
    replyTo: { email: fromEmail, name: fromName },
    subject,
    textContent: textBody,
    ...(htmlBody ? { htmlContent: htmlBody } : {}),
  };
  const resp = await fetch('https://api.brevo.com/v3/smtp/email', {
    method: 'POST',
    headers: {
      accept: 'application/json',
      'content-type': 'application/json',
      'api-key': apiKey,
    },
    body: JSON.stringify(payload),
    signal: AbortSignal.timeout(15000),
  });
  const bodyText = await resp.text();
  if (!resp.ok) {
    throw new Error(`Brevo API HTTP ${resp.status}: ${bodyText.slice(0, 220)}`);
  }
  return bodyText.trim().slice(0, 140) || 'OK';
}

async function sendEmailViaResendApi({ apiKey, fromEmail, fromName = 'Quick Delivery Official', toEmail, subject, textBody, htmlBody }) {
  const payload = {
    from: `${fromName} <${fromEmail}>`,
    to: [toEmail],
    reply_to: fromEmail,
    subject,
    text: textBody,
    ...(htmlBody ? { html: htmlBody } : {}),
  };
  const resp = await fetch('https://api.resend.com/emails', {
    method: 'POST',
    headers: {
      accept: 'application/json',
      'content-type': 'application/json',
      authorization: `Bearer ${apiKey}`,
    },
    body: JSON.stringify(payload),
    signal: AbortSignal.timeout(15000),
  });
  const bodyText = await resp.text();
  if (!resp.ok) {
    throw new Error(`Resend API HTTP ${resp.status}: ${bodyText.slice(0, 220)}`);
  }
  return bodyText.trim().slice(0, 140) || 'OK';
}

async function sendQuickDeliveryEmail({ toEmail, subject, textBody, htmlBody = '', type = 'GENERAL', extra = {} }) {
  const resendApiKey = String(process.env.RESEND_API_KEY || '').trim();
  const brevoApiKey = String(process.env.BREVO_API_KEY || '').trim();
  const smtpUser = String(process.env.SUPPORT_SMTP_USER || '').trim();
  const rawSmtpPass = String(process.env.SUPPORT_SMTP_APP_PASSWORD || process.env.SUPPORT_SMTP_PASS || '').trim();
  const smtpHost = String(process.env.SUPPORT_SMTP_HOST || 'smtp.gmail.com').trim();
  const smtpPass = smtpHost.includes('gmail.com') ? rawSmtpPass.replace(/\s+/g, '') : rawSmtpPass;
  const smtpPort = Number(String(process.env.SUPPORT_SMTP_PORT || '465').trim()) || 465;
  const webhookUrl = String(process.env.SUPPORT_EMAIL_WEBHOOK_URL || '').trim();
  const fromEmail = OTP_FROM_EMAIL;
  const fromName = String(process.env.OTP_FROM_NAME || 'Quick Delivery Official').trim() || 'Quick Delivery Official';
  const branded = buildOfficialQuickDeliveryEmail({ subject, textBody, htmlBody, type });
  const finalTextBody = branded.textBody;
  const finalHtmlBody = branded.htmlBody;

  // 1. Authoritative Production Transactional Email API: Brevo (300/day free, HTTPS port 443)
  if (brevoApiKey) {
    try {
      const responsePreview = await sendEmailViaBrevoApi({
        apiKey: brevoApiKey,
        fromEmail,
        fromName,
        toEmail,
        subject,
        textBody: finalTextBody,
        htmlBody: finalHtmlBody,
      });
      lastEmailDiagnostics = { channel: 'BREVO_API', status: 'OK', responsePreview, error: null, updatedAtIso: new Date().toISOString() };
      return { deliveryChannel: 'BREVO_API', fromEmail };
    } catch (brevoErr) {
      const msg = formatNodeError(brevoErr);
      lastEmailDiagnostics = { channel: 'BREVO_API', status: 'ERROR', error: msg, updatedAtIso: new Date().toISOString() };
      throw new Error(`Brevo email send failed: ${msg}`);
    }
  }

  // 2. Authoritative Production Transactional Email API: Resend (HTTPS port 443)
  if (resendApiKey) {
    try {
      const responsePreview = await sendEmailViaResendApi({
        apiKey: resendApiKey,
        fromEmail,
        fromName,
        toEmail,
        subject,
        textBody: finalTextBody,
        htmlBody: finalHtmlBody,
      });
      lastEmailDiagnostics = { channel: 'RESEND_API', status: 'OK', responsePreview, error: null, updatedAtIso: new Date().toISOString() };
      return { deliveryChannel: 'RESEND_API', fromEmail };
    } catch (resendErr) {
      const msg = formatNodeError(resendErr);
      lastEmailDiagnostics = { channel: 'RESEND_API', status: 'ERROR', error: msg, updatedAtIso: new Date().toISOString() };
      throw new Error(`Resend email send failed: ${msg}`);
    }
  }

  // 3. Fallback: HTTPS Webhook (Google Apps Script, port 443)
  if (webhookUrl) {
    try {
      const responsePreview = await sendEmailViaHttpsWebhook({ webhookUrl, fromEmail, toEmail, subject, textBody: finalTextBody, htmlBody: finalHtmlBody, type, extra });
      lastEmailDiagnostics = { channel: 'WEBHOOK', status: 'OK', responsePreview, error: null, updatedAtIso: new Date().toISOString() };
      return { deliveryChannel: 'WEBHOOK', fromEmail };
    } catch (whErr) {
      const whMsg = formatNodeError(whErr);
      lastEmailDiagnostics = { channel: 'WEBHOOK', status: 'ERROR', error: whMsg, updatedAtIso: new Date().toISOString() };
      if (!smtpUser || !smtpPass) {
        throw new Error(`Email webhook send failed: ${whMsg}`);
      }
    }
  }

  if (smtpUser && smtpPass) {
    const portsToTry = smtpPort === 587 ? [587, 465] : [smtpPort, 587];
    let lastSmtpErr = null;
    for (const port of portsToTry) {
      try {
        await sendWorkspaceSmtpTlsMail({
          smtpHost,
          smtpPort: port,
          smtpUser,
          smtpPass,
          fromEmail,
          toEmail,
          subject,
          textBody: finalTextBody,
          htmlBody: finalHtmlBody,
        });
        lastEmailDiagnostics = { channel: `SMTP_${port}`, status: 'OK', error: null, updatedAtIso: new Date().toISOString() };
        return {
          deliveryChannel: 'SMTP',
          fromEmail: smtpUser.includes('@') ? smtpUser : fromEmail,
        };
      } catch (err) {
        lastSmtpErr = formatNodeError(err);
        // Do not retry alternate port if Gmail explicitly rejected credentials (535)
        if (lastSmtpErr.includes('SMTP error 535') || lastSmtpErr.includes('SMTP error 534')) {
          break;
        }
      }
    }
    lastEmailDiagnostics = { channel: 'SMTP', status: 'ERROR', error: lastSmtpErr, updatedAtIso: new Date().toISOString() };
    const isPortBlocked = /EACCES|ECONNREFUSED|ETIMEDOUT|ENETUNREACH|EHOSTUNREACH|timeout/i.test(lastSmtpErr || '');
    if (isPortBlocked && !webhookUrl) {
      throw new Error(
        `Cloud host blocked SMTP (${lastSmtpErr}). Add SUPPORT_EMAIL_WEBHOOK_URL (Google Apps Script HTTPS) in Render Environment.`
      );
    }
    throw new Error(`Email SMTP send failed: ${lastSmtpErr}`);
  }

  const errMsg = `Email sender not configured. Set SUPPORT_EMAIL_WEBHOOK_URL (or SMTP vars) in Render Environment.`;
  lastEmailDiagnostics = { channel: 'NONE', status: 'UNCONFIGURED', error: errMsg, updatedAtIso: new Date().toISOString() };
  const err = new Error(errMsg);
  err.statusCode = 503;
  throw err;
}

function dispatchSupportTicketToWorkspaceEmail(ticket) {
  if (!ticket || !ticket.ticketId) return;
  const tid = String(ticket.ticketId);
  if (!syncState.emailedTicketIds) syncState.emailedTicketIds = {};
  if (syncState.emailedTicketIds[tid]) return;

  const targetEmail = process.env.SUPPORT_TO_EMAIL || BRAND_SUPPORT_EMAIL;
  const subject = `[Quick Delivery Ticket #${tid}] ${ticket.senderRole || 'USER'}: ${ticket.category || 'Support'}`;
  const textBody = [
    `New Support Ticket Logged on ${BRAND_DOMAIN}`,
    `--------------------------------------------------`,
    `Ticket ID   : ${tid}`,
    `Raised By   : ${ticket.senderRole || 'USER'} (${ticket.senderName || 'User'})`,
    `Phone       : ${ticket.senderPhone || 'Not provided'}`,
    `Order ID    : ${ticket.orderId ? '#' + ticket.orderId : 'N/A'}`,
    `Category    : ${ticket.category || 'General'}`,
    `Created At  : ${ticket.createdAtLabel || new Date().toISOString()}`,
    `--------------------------------------------------`,
    `Message:`,
    `${ticket.message || ''}`,
    ``,
    `Open Quick Delivery Admin App -> Complaints & Support to reply live.`,
  ].join('\n');

  syncState.emailedTicketIds[tid] = {
    ticketId: tid,
    to: targetEmail,
    status: 'SENDING',
    updatedAtIso: new Date().toISOString(),
  };

  sendQuickDeliveryEmail({
    toEmail: targetEmail,
    subject,
    textBody,
    type: 'SUPPORT_TICKET',
    extra: { ticket },
  })
    .then(info => {
      syncState.emailedTicketIds[tid] = {
        ticketId: tid,
        to: targetEmail,
        status: `SENT_${info.deliveryChannel}`,
        updatedAtIso: new Date().toISOString(),
      };
      saveSyncState();
    })
    .catch(err => {
      syncState.emailedTicketIds[tid] = {
        ticketId: tid,
        to: targetEmail,
        status: `ERROR: ${formatNodeError(err)}`,
        updatedAtIso: new Date().toISOString(),
      };
      saveSyncState();
    });
}

function saveSyncState() {
  try {
    fs.writeFileSync(SYNC_STATE_FILE, JSON.stringify(syncState), 'utf-8');
    writeDailyBackupSnapshot(false);
    schedulePermanentCloudSync();
  } catch {}
}

function renderDownloadPageHtml(autoDownloadFile = '', singleRoleFilter = '') {
  const safeAuto = autoDownloadFile.replace(/[^A-Za-z0-9._-]/g, '');
  const sizeMb = getApkSizeMb(safeAuto || 'QuickDelivery-Customer.apk');
  const custTag = getApkCacheTag('QuickDelivery-Customer.apk');
  const riderTag = getApkCacheTag('QuickDelivery-Rider-Partner.apk');
  const adminTag = getApkCacheTag('QuickDelivery-Admin-Enterprise.apk');
  const hubTag = getApkCacheTag('QuickDelivery-AllInOne-Hub.apk');
  const autoTag = getApkCacheTag(safeAuto || 'QuickDelivery-Customer.apk');

  const custSha = getApkSha256Short('QuickDelivery-Customer.apk');
  const riderSha = getApkSha256Short('QuickDelivery-Rider-Partner.apk');
  const adminSha = getApkSha256Short('QuickDelivery-Admin-Enterprise.apk');
  const hubSha = getApkSha256Short('QuickDelivery-AllInOne-Hub.apk');
  const custDlName = getVersionedDownloadFileName('QuickDelivery-Customer.apk');
  const riderDlName = getVersionedDownloadFileName('QuickDelivery-Rider-Partner.apk');
  const adminDlName = getVersionedDownloadFileName('QuickDelivery-Admin-Enterprise.apk');
  const hubDlName = getVersionedDownloadFileName('QuickDelivery-AllInOne-Hub.apk');

  const roleUpper = String(singleRoleFilter || '').toUpperCase();
  const showCustomer = !roleUpper || roleUpper === 'CUSTOMER';
  const showRider = !roleUpper || roleUpper === 'RIDER';
  const showAdmin = !roleUpper || roleUpper === 'ADMIN';
  const showHub = !roleUpper || roleUpper === 'HUB' || roleUpper === 'UNIFIED';

  const pageHeading =
    roleUpper === 'CUSTOMER'
      ? `Quick Delivery — Customer App (v${APP_SUITE_VERSION})`
      : roleUpper === 'RIDER'
      ? `Partner Quick Delivery — Rider App (v${APP_SUITE_VERSION})`
      : roleUpper === 'ADMIN'
      ? `Quick Delivery Admin — Console App (v${APP_SUITE_VERSION})`
      : roleUpper === 'HUB' || roleUpper === 'UNIFIED'
      ? `Quick Delivery — All-in-One 3-App Hub (v${APP_SUITE_VERSION})`
      : `Quick Delivery — Standalone App Downloads (v${APP_SUITE_VERSION})`;

  const customerCardHtml = showCustomer
    ? `
      <div class="apk-card" style="border-color: #10B981;">
        <div class="apk-title" style="color: #6EE7B7;">Quick Delivery (Customer App) • v${APP_SUITE_VERSION} • ${getApkSizeMb('QuickDelivery-Customer.apk')}</div>
        <div class="apk-desc">Installs ONLY the <b>Quick Delivery</b> customer booking app (Build <b>#${custSha}</b>). Instant parcel booking, clean Email OTP verification, UPI/COD checkout, GST receipts &amp; live tracking.</div>
        <a class="dl-btn" style="background: #059669;" href="/dl/QuickDelivery-Customer.apk?v=${custTag}" download="${custDlName}" onclick="onDirectApkClick('${custDlName}')">
          Download v${APP_SUITE_VERSION} Customer APK (Build #${custSha})
        </a>
        <a class="direct-link" href="/dl/QuickDelivery-Customer.apk?v=${custTag}" download="${custDlName}">
          Direct Download: ${custDlName}
        </a>
      </div>`
    : '';

  const riderCardHtml = showRider
    ? `
      <div class="apk-card" style="border-color: #F59E0B;">
        <div class="apk-title" style="color: #FCD34D;">Partner Quick Delivery (Rider App) • v${APP_SUITE_VERSION} • ${getApkSizeMb('QuickDelivery-Rider-Partner.apk')}</div>
        <div class="apk-desc">Installs ONLY the <b>Partner Quick Delivery</b> app (Build <b>#${riderSha}</b>). Accept/Reject delivery requests, clean Email OTP verification, OTP-verified pickup/drop, same-day earnings &amp; partner wallet.</div>
        <a class="dl-btn" style="background: #D97706;" href="/dl/QuickDelivery-Rider-Partner.apk?v=${riderTag}" download="${riderDlName}" onclick="onDirectApkClick('${riderDlName}')">
          Download v${APP_SUITE_VERSION} Rider APK (Build #${riderSha})
        </a>
        <a class="direct-link" href="/dl/QuickDelivery-Rider-Partner.apk?v=${riderTag}" download="${riderDlName}">
          Direct Download: ${riderDlName}
        </a>
      </div>`
    : '';

  const adminCardHtml = showAdmin
    ? `
      <div class="apk-card" style="border-color: #14B8A6;">
        <div class="apk-title" style="color: #5EEAD4;">Quick Delivery Admin (Operations Console) • v${APP_SUITE_VERSION} • ${getApkSizeMb('QuickDelivery-Admin-Enterprise.apk')}</div>
        <div class="apk-desc">Installs ONLY the <b>Quick Delivery Admin</b> console (Build <b>#${adminSha}</b>). Live fleet GPS monitoring, order dispatch oversight, commission settlement &amp; SOS alerts.</div>
        <a class="dl-btn" style="background: #0D9488;" href="/dl/QuickDelivery-Admin-Enterprise.apk?v=${adminTag}" download="${adminDlName}" onclick="onDirectApkClick('${adminDlName}')">
          Download v${APP_SUITE_VERSION} Admin APK (Build #${adminSha})
        </a>
        <a class="direct-link" href="/dl/QuickDelivery-Admin-Enterprise.apk?v=${adminTag}" download="${adminDlName}">
          Direct Download: ${adminDlName}
        </a>
      </div>`
    : '';

  const allInOneCardHtml = showHub
    ? `
      <div class="apk-card" style="border-color: #8B5CF6; background: linear-gradient(180deg, #1E1B4B 0%, #0F172A 100%);">
        <div class="apk-title" style="color: #C4B5FD;">📲 Quick Delivery All-in-One 3-App Hub • v${APP_SUITE_VERSION} • ${getApkSizeMb('QuickDelivery-AllInOne-Hub.apk')}</div>
        <div class="apk-desc">Installs the unified <b>3-in-1 Test Hub</b> (Build <b>#${hubSha}</b>) with the top <b>Customer / Rider / Admin</b> switcher inside one app — check all 3 apps &amp; live sync on a single phone.</div>
        <a class="dl-btn" style="background: #7C3AED;" href="/dl/QuickDelivery-AllInOne-Hub.apk?v=${hubTag}" download="${hubDlName}" onclick="onDirectApkClick('${hubDlName}')">
          Download v${APP_SUITE_VERSION} All-in-One 3-App APK (Build #${hubSha})
        </a>
        <a class="direct-link" href="/dl/QuickDelivery-AllInOne-Hub.apk?v=${hubTag}" download="${hubDlName}">
          Direct Download: ${hubDlName}
        </a>
      </div>`
    : '';

  return `<!DOCTYPE html>
<html lang="hi">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <link rel="canonical" href="${BRAND_ORIGIN}${roleUpper === 'CUSTOMER' ? '/customer-apk' : roleUpper === 'RIDER' ? '/rider-apk' : roleUpper === 'ADMIN' ? '/admin-apk' : '/'}" />
  <title>${pageHeading} • ${BRAND_DOMAIN}</title>
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
  </style>
</head>
<body>
  <div class="container">
    <div class="header-box">
      <div style="display:flex;justify-content:space-between;align-items:center;gap:8px;flex-wrap:wrap;margin-bottom:6px;">
        <span class="badge">Direct Instant APK Download • 1 Link = 1 Single App</span>
        <a href="/?view=app" style="color:#6EE7B7;font-size:12px;font-weight:800;text-decoration:none;background:rgba(16,185,129,0.18);border:1px solid #34D399;padding:4px 10px;border-radius:8px;">← Live 3-App Preview</a>
      </div>
      <h1>${pageHeading}</h1>
      <p>Niche green button dabayein ya auto-download start hone dein — <b>Chrome seedha .apk file (${sizeMb}) download karega</b>.</p>
    </div>

    <div id="progress-panel">
      <div class="prog-title" id="prog-title">✅ Direct APK Download Started!</div>
      <div class="prog-bar-bg">
        <div class="prog-bar-fill" id="prog-fill"></div>
      </div>
      <div class="prog-stats" id="prog-stats">Chrome notification / Downloads me APK download ho raha hai...</div>
    </div>

    <div class="grid">
      ${allInOneCardHtml}
      ${customerCardHtml}
      ${riderCardHtml}
      ${adminCardHtml}
    </div>

    <div class="guide-box">
      <h2>✨ Quick Delivery Suite v${APP_SUITE_VERSION}</h2>
      <ol class="step-list">
        <li><b>Quick Delivery (Customer):</b> Fast doorstep parcel booking, profile DP, saved addresses, phonebook receiver picker, live tracking &amp; OTP delivery verification.</li>
        <li><b>Partner Quick Delivery (Rider):</b> Instant nearby pickup alerts, profile DP, duty switch, wallet &amp; trip earnings history, and direct bank payout requests.</li>
        <li><b>Quick Delivery Admin:</b> Live orders &amp; rider KYC approval desk, payout management, fare &amp; feature switches, and cloud backup export.</li>
      </ol>
    </div>

    <div style="margin-top: 16px; text-align: center; font-size: 12px; color: #A7F3D0; line-height: 1.8;">
      <div><b>Official Domain:</b> <a href="${BRAND_ORIGIN}" style="color: #6EE7B7;">${BRAND_DOMAIN}</a> • <b>Support:</b> <a href="mailto:${BRAND_SUPPORT_EMAIL}" style="color: #6EE7B7;">${BRAND_SUPPORT_EMAIL}</a></div>
      <a href="/privacy-policy" style="color: #6EE7B7; margin: 0 8px;">Privacy Policy</a> •
      <a href="/terms" style="color: #6EE7B7; margin: 0 8px;">Terms of Service</a> •
      <a href="/delete-account" style="color: #6EE7B7; margin: 0 8px;">Delete Account &amp; Data</a> •
      <a href="/?view=public" style="color: #93C5FD; margin: 0 8px;">Public Brand Portal</a>
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
          window.location.href = '/dl/' + AUTO_DOWNLOAD_FILE + '?v=' + ${JSON.stringify(autoTag)};
        }, 150);
      });
    }
  </script>
</body>
</html>`;
}

function renderLegalPageHtml(kind = 'privacy') {
  const isTerms = kind === 'terms';
  const isDelete = kind === 'delete';
  const isAbout = kind === 'about' || kind === 'support';
  const slug = isDelete ? '/delete-account' : isTerms ? '/terms' : isAbout ? '/about' : '/privacy-policy';
  const title = isDelete
    ? `Quick Delivery (${BRAND_DOMAIN}) — Account & Data Deletion Request`
    : isTerms
    ? `Quick Delivery (${BRAND_DOMAIN}) — Terms of Service & Parcel Protection Policy`
    : isAbout
    ? `Quick Delivery (${BRAND_DOMAIN}) — Official App Info, Safety & Terms`
    : `Quick Delivery (${BRAND_DOMAIN}) — Privacy Policy`;

  const bodyContent = isDelete
    ? `
      <h1>Account &amp; Data Deletion</h1>
      <p><b>Domain:</b> <a href="${BRAND_ORIGIN}">${BRAND_ORIGIN}</a> • <b>Web URL:</b> <a href="${BRAND_ORIGIN}/delete-account">${BRAND_ORIGIN}/delete-account</a><br /><b>Applies to:</b> Quick Delivery, Partner Quick Delivery, and Quick Delivery Admin (com.aistudio.quickdelivery.*)</p>
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
        <p>To request permanent erasure of any cloud order logs or partner records stored on our dispatch servers, email <a href="mailto:${BRAND_SUPPORT_EMAIL}?subject=Quick%20Delivery%20Account%20Deletion%20Request">${BRAND_SUPPORT_EMAIL}</a> from your registered email address with your 10-digit mobile number. Verified requests are processed within 7 business days.</p>
      </div>`
    : isAbout
    ? `
      <h1>About Quick Delivery — Official App &amp; Support Center</h1>
      <p><b>Official Brand Portal:</b> <a href="${BRAND_ORIGIN}">${BRAND_ORIGIN}</a> • <b>Official Support:</b> <a href="mailto:${BRAND_SUPPORT_EMAIL}">${BRAND_SUPPORT_EMAIL}</a></p>
      <div class="card">
        <h2>How Quick Delivery Works</h2>
        <ul>
          <li><b>1. Transparent Booking &amp; Fare Seal:</b> Enter pickup &amp; drop locations, choose Bike, Scooter, Auto, or Mini-Truck (up to 20 kg), and lock an upfront fare with GST invoice support.</li>
          <li><b>2. Pre-Pickup Photo &amp; GPS Verification:</b> Capture parcel safety photos before dispatch, track the assigned verified Rider on live map telemetry, and share the 4-digit Pickup OTP only at physical handover.</li>
          <li><b>3. Verified Delivery Proof &amp; Digital Receipt:</b> Delivery closes only after 4-digit Drop OTP / digital recipient signature and proof capture, generating an official Quick Delivery PDF Tax Invoice.</li>
        </ul>
      </div>
      <div class="card">
        <h2>Parcel Safety, Tampering, Theft &amp; Accident Protection</h2>
        <ul>
          <li><b>Zero-Tolerance Tamper, Theft &amp; 100% Value Recovery:</b> Opening, replacing, stealing, or mishandling a sealed parcel results in immediate Rider account termination, national Aadhaar/DL/RC KYC blacklisting, payout hold, formal Police FIR with GPS &amp; photo evidence, and <b>100% shipment value + loss recovery from the Rider Partner</b>.</li>
          <li><b>Illegal / Prohibited Goods (100% Customer Responsibility):</b> Narcotics, weapons, explosives, contraband, or stolen goods are strictly banned. Any illegal shipment is 100% the legal &amp; criminal responsibility of the Customer (Sender) — zero liability falls on Quick Delivery or the Rider.</li>
          <li><b>Road Accident or Transit Damage:</b> Partners must report incidents immediately with live photos via In-App Support or <a href="mailto:${BRAND_SUPPORT_EMAIL}">${BRAND_SUPPORT_EMAIL}</a>. Verified claims are audited by Admin Operations within 24–48 hours.</li>
        </ul>
        <p style="margin-top:10px;"><a href="/terms">Read Complete Terms of Service →</a> • <a href="/privacy-policy">Read Privacy Policy →</a></p>
      </div>`
    : isTerms
    ? `
      <h1>Terms of Service — Quick Delivery Suite</h1>
      <p><b>Official Website:</b> <a href="${BRAND_ORIGIN}">${BRAND_ORIGIN}</a> • <b>Effective Date:</b> November 2026<br /><b>Contact &amp; Support Email:</b> <a href="mailto:${BRAND_SUPPORT_EMAIL}">${BRAND_SUPPORT_EMAIL}</a></p>
      <div class="card">
        <h2>1. Mandatory Consent, Account Isolation &amp; Permissions</h2>
        <p>Users must read and explicitly accept the Privacy Policy (<a href="${BRAND_ORIGIN}/privacy-policy">${BRAND_ORIGIN}/privacy-policy</a>) &amp; Terms (<a href="${BRAND_ORIGIN}/terms">${BRAND_ORIGIN}/terms</a>) before signing up. Each customer account is strictly isolated by its 10-digit mobile number or verified email so order history, live tracking, and saved addresses remain private to that account. Location, Camera, and Notification permissions are required post-login to operate parcel pickup, GPS tracking, and proof-of-delivery features.</p>
        <h2>2. Parcel Booking &amp; Illegal / Prohibited Items (100% Customer Liability)</h2>
        <p>Quick Delivery (${BRAND_DOMAIN}) provides on-demand local parcel pickup and drop-off services (maximum 20 kg per shipment). Narcotics, illegal drugs, weapons, explosives, flammable, stolen, or prohibited contraband goods are strictly banned. <b>If a Customer ships any illegal or prohibited item, 100% legal, criminal, and civil responsibility rests solely with that Customer (Sender) — Quick Delivery Logistics and the assigned Rider Partner bear zero liability for concealed illegal contents.</b></p>
        <h2>3. OTP Handover, Privacy Phone Masking &amp; Cancellation</h2>
        <p>Orders require 4-digit Pickup and Drop OTP + digital signature verification. To safeguard user privacy, Customer mobile numbers are visible to the assigned Rider only while an order is live and are automatically masked to the last 4 digits (<code>******XXXX</code>) immediately after delivery or cancellation, while in-app Rider–Customer chat remains auditable by Admin Operations. Pre-pickup cancellations after partner dispatch and wait time beyond the free grace window may incur policy fees when enabled by Admin.</p>
        <h2>4. Partner Safety, KYC &amp; Fair Settlement</h2>
        <p>Delivery partners receive 80% of base trip fare plus 100% of customer tips. Partners must maintain valid KYC documents, wear helmets/safety gear, obey traffic laws, and report any open/damaged parcel before pickup via the Pre-Pickup Damage Report button.</p>
        <h2>5. Parcel Tampering, Theft, Loss, 100% Recovery &amp; Accident Protection Policy</h2>
        <p>Delivery Partners must transport sealed parcels safely without opening, altering, substituting, or withholding contents. <b>(a) Tampering, Fraud or Theft:</b> Any confirmed parcel tampering, theft, or intentional non-delivery triggers immediate permanent account termination, national Aadhaar/DL/RC KYC blacklisting, pending wallet payout freeze, formal law-enforcement Police FIR filing using timestamped GPS, camera proofs, and chat logs, <b>AND 100% financial recovery of the shipment amount &amp; loss directly payable by the Rider Partner</b>. <b>(b) Road Accident / Unintentional Transit Damage:</b> Partners must promptly notify Admin Support &amp; capture accident/parcel damage photos in-app; Quick Delivery Logistics audits pre-pickup vs post-incident photos to process verified damage claims and Optional Parcel Protection Cover settlements transparently via <a href="mailto:${BRAND_SUPPORT_EMAIL}">${BRAND_SUPPORT_EMAIL}</a>.</p>
      </div>`
    : `
      <h1>Privacy Policy — Quick Delivery Suite</h1>
      <p><b>Official Website:</b> <a href="${BRAND_ORIGIN}">${BRAND_ORIGIN}</a> • <b>Policy URL:</b> <a href="${BRAND_ORIGIN}/privacy-policy">${BRAND_ORIGIN}/privacy-policy</a><br /><b>Apps Covered:</b> Quick Delivery (Customer), Partner Quick Delivery (Rider), and Quick Delivery Admin<br /><b>Developer / Data Controller:</b> Quick Delivery Logistics (<a href="mailto:${BRAND_SUPPORT_EMAIL}">${BRAND_SUPPORT_EMAIL}</a>)<br /><b>Effective Date:</b> November 2026</p>
      <div class="card">
        <h2>1. Account-Isolated Information We Collect</h2>
        <ul>
          <li><b>Account &amp; Partner Identity:</b> Name, 10-digit mobile number, Google email address, and (for Riders) KYC documents strictly isolated per account for booking history, GST receipts, and payouts.</li>
          <li><b>Foreground Location (Required Post-Login):</b> Approximate and precise GPS coordinates collected only while booking, actively navigating, or tracking an active order. No background spying occurs when offline.</li>
          <li><b>Camera &amp; Media Storage (Required Post-Login):</b> Parcel pickup/drop proof photos and Rider selfie check-ins captured when triggered by you, plus encrypted local backups of your profile, saved addresses, and order history stored safely in Phone Storage (Documents/QuickDelivery).</li>
          <li><b>Notifications (Required Post-Login):</b> Push and local tray alerts strictly for order status updates, OTPs, and live Rider-Customer chat.</li>
        </ul>
        <h2>2. How We Use &amp; Protect Data</h2>
        <ul>
          <li>Used solely to calculate transparent delivery fares, verify OTP &amp; digital signature handovers, display live rider ETA, generate GST/Earning PDFs, and verify online payments.</li>
          <li>All traffic in transit is encrypted over HTTPS (TLS) with Firebase Zero-Trust rules and HMAC-SHA256 integrity verification.</li>
          <li>We never sell personal data or share it with third-party advertising networks.</li>
        </ul>
        <h2>3. Data Retention &amp; Account Deletion</h2>
        <p>You can permanently delete your account and profile anytime inside the app via <b>Menu → Delete Account &amp; Data</b>, review web deletion instructions at <a href="${BRAND_ORIGIN}/delete-account">${BRAND_ORIGIN}/delete-account</a>, or email <a href="mailto:${BRAND_SUPPORT_EMAIL}">${BRAND_SUPPORT_EMAIL}</a>.</p>
      </div>`;

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <link rel="canonical" href="${BRAND_ORIGIN}${slug}" />
  <title>${title}</title>
  <style>
    body { margin: 0; padding: 20px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #042F2E; color: #F8FAFC; line-height: 1.6; }
    .wrap { max-width: 720px; margin: 0 auto; }
    .brand-bar { display: flex; align-items: center; gap: 12px; background: #064E3B; border: 1px solid #34D399; border-radius: 14px; padding: 12px 16px; margin-bottom: 14px; }
    .brand-bar img { width: 46px; height: 46px; border-radius: 50%; border: 2px solid #FF7A1A; background: #FCF4E1; object-fit: cover; }
    .card { background: #0F172A; border: 1px solid #1E293B; border-radius: 14px; padding: 18px; margin-top: 14px; }
    h1 { color: #6EE7B7; font-size: 22px; margin-bottom: 6px; }
    h2 { color: #38BDF8; font-size: 16px; margin: 12px 0 6px; }
    a { color: #6EE7B7; }
    .nav { margin-top: 18px; font-size: 13px; }
  </style>
</head>
<body>
  <div class="wrap">
    <div class="brand-bar">
      <img src="/brand-logo.jpg" alt="Quick Delivery Official Logo" />
      <div>
        <div style="font-size:17px;font-weight:800;color:#ECFDF5;">Quick Delivery Official</div>
        <div style="font-size:11px;font-weight:700;color:#FDBA74;">FAST. SIMPLE. SECURE. • ${BRAND_DOMAIN}</div>
      </div>
    </div>
    ${bodyContent}
    <div class="nav">
      <a href="/">Home (${BRAND_DOMAIN})</a> •
      <a href="/about">App Info &amp; Support</a> •
      <a href="/privacy-policy">Privacy Policy</a> •
      <a href="/terms">Terms of Service</a> •
      <a href="/delete-account">Delete Account</a>
    </div>
  </div>
</body>
</html>`;
}

function renderFirebaseHostingHomeHtml() {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <link rel="canonical" href="${BRAND_ORIGIN}/" />
  <title>Quick Delivery — Fast. Simple. Secure. Local Parcel Delivery</title>
  <style>
    * { box-sizing: border-box; }
    body { margin: 0; padding: 20px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #022C22; color: #F8FAFC; line-height: 1.55; }
    .wrap { max-width: 740px; margin: 0 auto; }
    .hero { background: linear-gradient(135deg, #065F46, #042F2E); border: 2px solid #34D399; border-radius: 18px; padding: 24px; margin-bottom: 16px; }
    .hero-head { display: flex; align-items: center; gap: 14px; margin-bottom: 8px; }
    .hero-logo { width: 58px; height: 58px; border-radius: 50%; border: 2.5px solid #FF7A1A; background: #FCF4E1; object-fit: cover; flex-shrink: 0; }
    .badge { display: inline-block; background: #10B981; color: #022C22; font-weight: 800; font-size: 11px; padding: 4px 10px; border-radius: 999px; text-transform: uppercase; letter-spacing: 0.4px; }
    h1 { margin: 4px 0 4px; font-size: 26px; color: #ECFDF5; }
    .card { background: #0F172A; border: 1px solid #1E293B; border-radius: 14px; padding: 18px; margin-bottom: 12px; }
    h2 { margin: 0 0 8px; font-size: 16px; color: #6EE7B7; }
    p { margin: 6px 0; color: #CBD5E1; font-size: 14px; }
    a { color: #6EE7B7; }
    .pill { display: inline-block; background: #059669; color: #fff; text-decoration: none; font-weight: 700; padding: 10px 14px; border-radius: 10px; margin: 4px 6px 4px 0; font-size: 13px; }
    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 10px; margin-top: 10px; }
    .feat { background: #1E293B; border-radius: 10px; padding: 12px; font-size: 13px; color: #E2E8F0; }
  </style>
</head>
<body>
  <div class="wrap">
    <div class="hero">
      <div class="hero-head">
        <img class="hero-logo" src="/brand-logo.jpg" alt="Quick Delivery Official Logo" />
        <div>
          <span class="badge">Official Brand Portal • ${BRAND_DOMAIN}</span>
          <h1>Quick Delivery</h1>
        </div>
      </div>
      <p style="color:#D1FAE5;font-size:15px;"><b>FAST. SIMPLE. SECURE.</b> On-demand local doorstep parcel pickup &amp; delivery up to 20 kg with live GPS tracking, verified handover proofs, and instant transparent fares.</p>
    </div>
    <div class="card">
      <h2>Why Choose Quick Delivery?</h2>
      <div class="grid">
        <div class="feat"><b>⚡ Rapid Doorstep Pickup</b><br/>Bike, Scooter, Auto &amp; Mini-Truck options for every parcel up to 20 kg.</div>
        <div class="feat"><b>📍 Live GPS Tracking</b><br/>Track every pickup and drop milestone with verified photo &amp; signature proof.</div>
        <div class="feat"><b>💳 Transparent Pricing</b><br/>Upfront itemized fares with UPI, Card, Net Banking &amp; Cash on Delivery.</div>
      </div>
    </div>
    <div class="card">
      <h2>Customer Support &amp; Legal Compliance</h2>
      <a class="pill" href="/about">App Info &amp; Safety</a>
      <a class="pill" href="/privacy-policy" style="background:#0F766E;">Privacy Policy</a>
      <a class="pill" href="/terms" style="background:#0D9488;">Terms of Service</a>
      <a class="pill" href="/delete-account" style="background:#B91C1C;">Delete Account &amp; Data</a>
      <p style="margin-top:12px;font-size:13px;color:#94A3B8;">Need help with a parcel or account? Contact <a href="mailto:${BRAND_SUPPORT_EMAIL}">${BRAND_SUPPORT_EMAIL}</a></p>
    </div>
  </div>
</body>
</html>`;
}

function readInteractivePreviewHtml() {
  const previewPath = path.join(ROOT, 'public', 'preview-3apps.html');
  try {
    if (fs.existsSync(previewPath)) {
      return fs.readFileSync(previewPath, 'utf-8');
    }
  } catch {}
  return renderDownloadPageHtml('', '');
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
            let needsCopy = true;
            if (isValidStandaloneApk(dest)) {
              if (getFileSha256Short(pubApk) === getFileSha256Short(dest)) {
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

    const htmlPublicHome = renderFirebaseHostingHomeHtml();
    const htmlAll = renderDownloadPageHtml('', '');
    const htmlCustomer = renderDownloadPageHtml('QuickDelivery-Customer.apk', 'CUSTOMER');
    const htmlRider = renderDownloadPageHtml('QuickDelivery-Rider-Partner.apk', 'RIDER');
    const htmlAdmin = renderDownloadPageHtml('QuickDelivery-Admin-Enterprise.apk', 'ADMIN');
    const htmlHub = renderDownloadPageHtml('QuickDelivery-AllInOne-Hub.apk', 'HUB');
    const htmlPrivacy = renderLegalPageHtml('privacy');
    const htmlTerms = renderLegalPageHtml('terms');
    const htmlDelete = renderLegalPageHtml('delete');
    const htmlAbout = renderLegalPageHtml('about');
    const logoSrc = path.join(ROOT, 'app', 'src', 'main', 'res', 'drawable', 'cute_scooter_bunny_logo_1794655841710.jpg');

    if (fs.existsSync('/var/www/assets')) {
      fs.writeFileSync(
        '/var/www/assets/warmup.html',
        IS_AISTUDIO_CONTAINER ? readInteractivePreviewHtml() : htmlPublicHome,
        'utf-8'
      );
      fs.writeFileSync('/var/www/assets/suite.html', htmlAll, 'utf-8');
      fs.writeFileSync('/var/www/assets/customer-apk.html', htmlCustomer, 'utf-8');
      fs.writeFileSync('/var/www/assets/rider-apk.html', htmlRider, 'utf-8');
      fs.writeFileSync('/var/www/assets/admin-apk.html', htmlAdmin, 'utf-8');
      fs.writeFileSync('/var/www/assets/hub-apk.html', htmlHub, 'utf-8');
      fs.writeFileSync('/var/www/assets/privacy-policy.html', htmlPrivacy, 'utf-8');
      fs.writeFileSync('/var/www/assets/terms.html', htmlTerms, 'utf-8');
      fs.writeFileSync('/var/www/assets/delete-account.html', htmlDelete, 'utf-8');
      fs.writeFileSync('/var/www/assets/about.html', htmlAbout, 'utf-8');
      if (fs.existsSync(logoSrc)) {
        try { fs.copyFileSync(logoSrc, '/var/www/assets/brand-logo.jpg'); } catch {}
      }
    }
    try {
      fs.mkdirSync(path.join(ROOT, 'public'), { recursive: true });
      fs.writeFileSync(path.join(ROOT, 'public', 'apk-download.html'), htmlAll, 'utf-8');
      fs.writeFileSync(path.join(ROOT, 'public', 'customer-apk.html'), htmlCustomer, 'utf-8');
      fs.writeFileSync(path.join(ROOT, 'public', 'rider-apk.html'), htmlRider, 'utf-8');
      fs.writeFileSync(path.join(ROOT, 'public', 'admin-apk.html'), htmlAdmin, 'utf-8');
      fs.writeFileSync(path.join(ROOT, 'public', 'hub-apk.html'), htmlHub, 'utf-8');
      fs.writeFileSync(path.join(ROOT, 'public', 'privacy-policy.html'), htmlPrivacy, 'utf-8');
      fs.writeFileSync(path.join(ROOT, 'public', 'terms.html'), htmlTerms, 'utf-8');
      fs.writeFileSync(path.join(ROOT, 'public', 'delete-account.html'), htmlDelete, 'utf-8');
      fs.writeFileSync(path.join(ROOT, 'public', 'about.html'), htmlAbout, 'utf-8');
      if (fs.existsSync(logoSrc)) {
        fs.copyFileSync(logoSrc, path.join(ROOT, 'public', 'brand-logo.jpg'));
      }

      // Generate Firebase Hosting static bundle (dist/) ready for `firebase deploy --only hosting` on quickdelivery.co.in
      const distDir = path.join(ROOT, 'dist');
      fs.mkdirSync(distDir, { recursive: true });
      fs.writeFileSync(path.join(distDir, 'index.html'), renderFirebaseHostingHomeHtml(), 'utf-8');
      fs.writeFileSync(path.join(distDir, 'privacy-policy.html'), htmlPrivacy, 'utf-8');
      fs.writeFileSync(path.join(distDir, 'terms.html'), htmlTerms, 'utf-8');
      fs.writeFileSync(path.join(distDir, 'delete-account.html'), htmlDelete, 'utf-8');
      fs.writeFileSync(path.join(distDir, 'about.html'), htmlAbout, 'utf-8');
      if (fs.existsSync(logoSrc)) {
        fs.copyFileSync(logoSrc, path.join(distDir, 'brand-logo.jpg'));
      }
    } catch {}

    const apkBlock = `
        location = /dl/QuickDelivery-Customer.apk {
            alias /var/www/assets/apks/QuickDelivery-Customer.apk;
            types { }
            default_type application/vnd.android.package-archive;
            add_header Content-Disposition 'attachment; filename="${getVersionedDownloadFileName('QuickDelivery-Customer.apk')}"';
            add_header Cache-Control "public, max-age=60, must-revalidate";
            add_header Access-Control-Allow-Origin "*";
            add_header Access-Control-Expose-Headers "Content-Length, Content-Range, Accept-Ranges";
        }

        location = /dl/QuickDelivery-Rider-Partner.apk {
            alias /var/www/assets/apks/QuickDelivery-Rider-Partner.apk;
            types { }
            default_type application/vnd.android.package-archive;
            add_header Content-Disposition 'attachment; filename="${getVersionedDownloadFileName('QuickDelivery-Rider-Partner.apk')}"';
            add_header Cache-Control "public, max-age=60, must-revalidate";
            add_header Access-Control-Allow-Origin "*";
            add_header Access-Control-Expose-Headers "Content-Length, Content-Range, Accept-Ranges";
        }

        location = /dl/QuickDelivery-Admin-Enterprise.apk {
            alias /var/www/assets/apks/QuickDelivery-Admin-Enterprise.apk;
            types { }
            default_type application/vnd.android.package-archive;
            add_header Content-Disposition 'attachment; filename="${getVersionedDownloadFileName('QuickDelivery-Admin-Enterprise.apk')}"';
            add_header Cache-Control "public, max-age=60, must-revalidate";
            add_header Access-Control-Allow-Origin "*";
            add_header Access-Control-Expose-Headers "Content-Length, Content-Range, Accept-Ranges";
        }

        location = /dl/QuickDelivery-AllInOne-Hub.apk {
            alias /var/www/assets/apks/QuickDelivery-AllInOne-Hub.apk;
            types { }
            default_type application/vnd.android.package-archive;
            add_header Content-Disposition 'attachment; filename="${getVersionedDownloadFileName('QuickDelivery-AllInOne-Hub.apk')}"';
            add_header Cache-Control "public, max-age=60, must-revalidate";
            add_header Access-Control-Allow-Origin "*";
            add_header Access-Control-Expose-Headers "Content-Length, Content-Range, Accept-Ranges";
        }

        location = /customer {
            alias /var/www/assets/customer-apk.html;
            default_type text/html;
            add_header Cache-Control "no-store, no-cache, must-revalidate";
        }

        location = /customer-apk {
            alias /var/www/assets/customer-apk.html;
            default_type text/html;
            add_header Cache-Control "no-store, no-cache, must-revalidate";
        }

        location = /rider {
            alias /var/www/assets/rider-apk.html;
            default_type text/html;
            add_header Cache-Control "no-store, no-cache, must-revalidate";
        }

        location = /rider-apk {
            alias /var/www/assets/rider-apk.html;
            default_type text/html;
            add_header Cache-Control "no-store, no-cache, must-revalidate";
        }

        location = /admin {
            alias /var/www/assets/admin-apk.html;
            default_type text/html;
            add_header Cache-Control "no-store, no-cache, must-revalidate";
        }

        location = /admin-apk {
            alias /var/www/assets/admin-apk.html;
            default_type text/html;
            add_header Cache-Control "no-store, no-cache, must-revalidate";
        }

        location = /hub {
            alias /var/www/assets/hub-apk.html;
            default_type text/html;
            add_header Cache-Control "no-store, no-cache, must-revalidate";
        }

        location = /hub-apk {
            alias /var/www/assets/hub-apk.html;
            default_type text/html;
            add_header Cache-Control "no-store, no-cache, must-revalidate";
        }

        location = /suite {
            alias /var/www/assets/suite.html;
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
            add_header Cache-Control "public, max-age=60, must-revalidate";
            add_header Access-Control-Allow-Origin "*";
            add_header Access-Control-Expose-Headers "Content-Length, Content-Range, Accept-Ranges";
        }

        location /api/live-sync {
            proxy_pass http://localhost:3000/api/live-sync;
            proxy_set_header Host localhost:3000;
            add_header Access-Control-Allow-Origin "*";
            add_header Access-Control-Allow-Methods "GET, POST, OPTIONS";
            add_header Access-Control-Allow-Headers "Content-Type, Accept, X-QD-App-Client, X-QD-App-Role, X-QD-Sync-Ts, X-QD-Sync-Sig";
        }

        location = /api/health {
            proxy_pass http://localhost:3000/api/health;
            proxy_set_header Host localhost:3000;
            add_header Access-Control-Allow-Origin "*";
        }

        location = /api/app-update-manifest {
            proxy_pass http://localhost:3000/api/app-update-manifest;
            proxy_set_header Host localhost:3000;
            add_header Access-Control-Allow-Origin "*";
        }

        location = /api/support-email-status {
            proxy_pass http://localhost:3000/api/support-email-status;
            proxy_set_header Host localhost:3000;
            add_header Access-Control-Allow-Origin "*";
        }

        location = /api/backup-export {
            proxy_pass http://localhost:3000/api/backup-export;
            proxy_set_header Host localhost:3000;
            add_header Access-Control-Allow-Origin "*";
            add_header Access-Control-Allow-Headers "Content-Type, Accept, Authorization, X-QD-App-Client, X-QD-Device-Id, X-QD-App-Role, X-QD-Sync-Ts, X-QD-Sync-Sig";
        }

        location = /api/backup-restore {
            proxy_pass http://localhost:3000/api/backup-restore;
            proxy_set_header Host localhost:3000;
            add_header Access-Control-Allow-Origin "*";
            add_header Access-Control-Allow-Methods "POST, OPTIONS";
            add_header Access-Control-Allow-Headers "Content-Type, Accept, Authorization, X-QD-App-Client, X-QD-Device-Id, X-QD-App-Role, X-QD-Sync-Ts, X-QD-Sync-Sig";
        }

        location = /api/backup-email {
            proxy_pass http://localhost:3000/api/backup-email;
            proxy_set_header Host localhost:3000;
            add_header Access-Control-Allow-Origin "*";
            add_header Access-Control-Allow-Methods "POST, OPTIONS";
            add_header Access-Control-Allow-Headers "Content-Type, Accept, Authorization, X-QD-App-Client, X-QD-Device-Id, X-QD-App-Role, X-QD-Sync-Ts, X-QD-Sync-Sig";
        }

        location /api/payments/ {
            proxy_pass http://localhost:3000/api/payments/;
            proxy_set_header Host localhost:3000;
            add_header Access-Control-Allow-Origin "*";
            add_header Access-Control-Allow-Methods "GET, POST, OPTIONS";
            add_header Access-Control-Allow-Headers "Content-Type, Accept, x-razorpay-signature";
        }

        location /api/auth/ {
            proxy_pass http://localhost:3000/api/auth/;
            proxy_set_header Host localhost:3000;
            add_header Access-Control-Allow-Origin "*";
            add_header Access-Control-Allow-Methods "GET, POST, OPTIONS";
            add_header Access-Control-Allow-Headers "Content-Type, Accept, X-QD-App-Client, X-QD-App-Role, X-QD-Sync-Ts, X-QD-Sync-Sig";
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
  const hubApk = path.join('/var/www/assets/apks', 'QuickDelivery-AllInOne-Hub.apk');
  if (
    isValidStandaloneApk(customerApk) &&
    isValidStandaloneApk(riderApk) &&
    isValidStandaloneApk(adminApk) &&
    isValidStandaloneApk(hubApk)
  ) {
    return;
  }
  const pubCust = path.join(ROOT, 'public', 'apks', 'QuickDelivery-Customer.apk');
  const pubRider = path.join(ROOT, 'public', 'apks', 'QuickDelivery-Rider-Partner.apk');
  const pubAdmin = path.join(ROOT, 'public', 'apks', 'QuickDelivery-Admin-Enterprise.apk');
  const pubHub = path.join(ROOT, 'public', 'apks', 'QuickDelivery-AllInOne-Hub.apk');
  if (
    isValidStandaloneApk(pubCust) &&
    isValidStandaloneApk(pubRider) &&
    isValidStandaloneApk(pubAdmin) &&
    isValidStandaloneApk(pubHub)
  ) {
    configureNginxDirectApkServing();
    return;
  }
  const hasBaseApk =
    fs.existsSync(path.join(ROOT, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk')) ||
    fs.existsSync(path.join(ROOT, '.build-outputs', 'app-debug.apk'));
  if (!hasBaseApk) {
    configureNginxDirectApkServing();
    return;
  }
  try {
    const child = spawn('python3', ['scripts/build-standalone-role-apks.py'], {
      cwd: ROOT,
      stdio: 'ignore',
    });
    child.on('error', () => {});
    child.on('exit', () => {
      configureNginxDirectApkServing();
    });
  } catch {}
}

configureNginxDirectApkServing();
ensureStandaloneRoleApksBuilt();

if (process.argv.includes('--configure-nginx') || (process.env.GITHUB_ACTIONS === 'true' && !process.argv.includes('--serve'))) {
  process.exit(0);
}

function maskPhoneLast4(rawPhone) {
  const clean = String(rawPhone || '').trim();
  if (!clean) return '';
  const digits = clean.replace(/\D+/g, '');
  return digits.length >= 4 ? `******${digits.slice(-4)}` : '******';
}

function maskOrderPiiForRole(orderObj, viewerRole = 'UNIFIED') {
  if (!orderObj || typeof orderObj !== 'object') return orderObj;
  const normalizedRole = String(viewerRole || 'UNIFIED').trim().toUpperCase();
  const status = String(orderObj.orderStatus || '').trim().toUpperCase();
  const isTerminal = status === 'DELIVERED' || status === 'CANCELLED';
  if (normalizedRole !== 'RIDER' || !isTerminal) {
    return orderObj;
  }
  const cloned = { ...orderObj };
  if (cloned.customerPhone) cloned.customerPhone = maskPhoneLast4(cloned.customerPhone);
  if (cloned.receiverPhone) cloned.receiverPhone = maskPhoneLast4(cloned.receiverPhone);
  if (cloned.pickupAddress && typeof cloned.pickupAddress === 'object' && cloned.pickupAddress.contactPhone) {
    cloned.pickupAddress = { ...cloned.pickupAddress, contactPhone: maskPhoneLast4(cloned.pickupAddress.contactPhone) };
  }
  if (cloned.dropAddress && typeof cloned.dropAddress === 'object' && cloned.dropAddress.contactPhone) {
    cloned.dropAddress = { ...cloned.dropAddress, contactPhone: maskPhoneLast4(cloned.dropAddress.contactPhone) };
  }
  return cloned;
}

function buildServerAppUpdateManifest() {
  const adminFlags = syncState.remoteFeatureFlags || {};
  const adminCode = Number(adminFlags.latestAppVersionCode || 0);
  const adminName = String(adminFlags.latestAppVersionName || '').trim();
  const useAdminTarget = adminCode > APP_SUITE_BUILD_CODE;
  return {
    latestVersionCode: useAdminTarget ? adminCode : APP_SUITE_BUILD_CODE,
    latestVersionName: useAdminTarget ? (adminName || APP_SUITE_VERSION) : APP_SUITE_VERSION,
    forceUpdateRequired: useAdminTarget ? Boolean(adminFlags.forceUpdateRequired) : true,
    updateNotes:
      (useAdminTarget ? String(adminFlags.updateNotes || '').trim() : '') ||
      `Quick Delivery v${APP_SUITE_VERSION} is required (startup stability fix, automatic mandatory update gate & live sync improvements).`,
    customerUpdateUrl: `${BRAND_ORIGIN}/customer-apk`,
    riderUpdateUrl: `${BRAND_ORIGIN}/rider-apk`,
    adminUpdateUrl: `${BRAND_ORIGIN}/admin-apk`,
    unifiedUpdateUrl: `${BRAND_ORIGIN}/suite`,
    urls: {
      CUSTOMER: `${BRAND_ORIGIN}/customer-apk`,
      RIDER: `${BRAND_ORIGIN}/rider-apk`,
      ADMIN: `${BRAND_ORIGIN}/admin-apk`,
      UNIFIED: `${BRAND_ORIGIN}/suite`,
    },
    directDownloadUrls: {
      CUSTOMER: `${BRAND_ORIGIN}/dl/QuickDelivery-Customer.apk`,
      RIDER: `${BRAND_ORIGIN}/dl/QuickDelivery-Rider-Partner.apk`,
      ADMIN: `${BRAND_ORIGIN}/dl/QuickDelivery-Admin-Enterprise.apk`,
      UNIFIED: `${BRAND_ORIGIN}/dl/QuickDelivery-AllInOne-Hub.apk`,
    },
    publishedAtIso: new Date().toISOString(),
  };
}

function buildSyncResponsePayload(viewerRole = 'UNIFIED') {
  const now = Date.now();
  const connectedDevices = [];
  for (const [devId, devInfo] of Object.entries(syncState.devicesById)) {
    if (now - Number(devInfo.lastSeenEpochMs || 0) <= 60000) {
      connectedDevices.push(devInfo);
    } else {
      delete syncState.devicesById[devId];
    }
  }
  const normalizedRole = String(viewerRole || 'UNIFIED').trim().toUpperCase();
  return {
    ok: true,
    serverTimeEpochMs: now,
    resetEpochMs: syncState.resetEpochMs,
    serverAppUpdate: buildServerAppUpdateManifest(),
    securityStats: ensureSecurityStats(),
    connectedDevices,
    orders: Object.values(syncState.ordersById)
      .sort((a, b) => Number(b.updatedAtEpochMs || 0) - Number(a.updatedAtEpochMs || 0))
      .map(ord => maskOrderPiiForRole(ord, normalizedRole)),
    riderRegistrations: Object.values(syncState.riderRegistrationsByKey || {}),
    registeredCustomers: Object.values(syncState.customerAccountsByKey || {}),
    promoCoupons: Object.values(syncState.promoCouponsByCode || {}),
    withdrawalRequests: Object.values(syncState.withdrawalRequestsById || {}),
    supportTickets: Object.values(syncState.supportTicketsById || {}),
    codSettlements: Object.values(syncState.codSettlementsById || {}),
    disputeRecords: Object.values(syncState.disputeRecordsById || {}),
    riderDocExpiries: Object.values(syncState.riderDocExpiriesById || {}),
    orderRatings: Object.values(syncState.orderRatingsByOrder || {}),
    orderTimelineEvents: Object.values(syncState.orderTimelineEventsById || {}).sort(
      (a, b) => Number(a.createdAtEpochMs || 0) - Number(b.createdAtEpochMs || 0)
    ),
    unreachableByOrder: Object.values(syncState.unreachableByOrder || {}),
    b2bAccounts: Object.values(syncState.b2bAccountsById || {}),
    remoteFeatureFlags: syncState.remoteFeatureFlags || null,
    waitTimerStartByOrder: syncState.waitTimerStartByOrder || {},
    riderLiveCoordsByOrder: syncState.riderLiveCoordsByOrder || {},
    chatMessages: Object.values(syncState.chatMessagesById || {}).sort(
      (a, b) => Number(a.createdAtEpochMs || 0) - Number(b.createdAtEpochMs || 0)
    ),
  };
}

const server = http.createServer((req, res) => {
  try {
    const rawUrl = req.url || '/';
    const [pathname, queryString = ''] = rawUrl.split('?');

  if (req.method === 'OPTIONS') {
    res.writeHead(204, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Accept, Range, X-QD-App-Client, X-QD-App-Role, X-QD-Sync-Ts, X-QD-Sync-Sig',
    });
    res.end();
    return;
  }

  if (pathname === '/api/live-sync') {
    const rateCheck = checkIpRateLimit(req, false);
    if (!rateCheck.allowed) {
      saveSyncState();
      res.writeHead(429, {
        'Content-Type': 'application/json; charset=utf-8',
        'Retry-After': String(rateCheck.retryAfterSec),
        'Access-Control-Allow-Origin': '*',
      });
      res.end(
        JSON.stringify({
          ok: false,
          error: 'Rate limit exceeded. Too many requests from this IP.',
          retryAfterSec: rateCheck.retryAfterSec,
        })
      );
      return;
    }

    if (req.method === 'POST') {
      let body = '';
      let payloadTooLarge = false;
      req.on('data', chunk => {
        if (payloadTooLarge) return;
        body += chunk;
        if (body.length > MAX_SYNC_BODY_BYTES) {
          payloadTooLarge = true;
          recordSecurityBlock('BAD_PAYLOAD', getClientIp(req), 'Payload exceeded 10 MB ceiling');
          res.writeHead(413, {
            'Content-Type': 'application/json; charset=utf-8',
            'Access-Control-Allow-Origin': '*',
          });
          res.end(JSON.stringify({ ok: false, error: 'Payload too large (max 10 MB).' }));
          req.destroy();
        }
      });
      req.on('end', () => {
        if (payloadTooLarge) return;
        const rejectedOrdersForResponse = [];
        try {
          const payload = JSON.parse(body || '{}');
          const now = Date.now();
          const devIdForAuth = String(payload.deviceId || '');
          const roleForAuth = String(payload.role || 'UNIFIED');
          const sigCheck = verifySyncHmacHeaders(req, devIdForAuth, roleForAuth);
          if (!sigCheck.ok) {
            recordSecurityBlock('INVALID_SIG', getClientIp(req), sigCheck.reason);
            saveSyncState();
            res.writeHead(401, {
              'Content-Type': 'application/json; charset=utf-8',
              'Access-Control-Allow-Origin': '*',
            });
            res.end(
              JSON.stringify({
                ok: false,
                error: sigCheck.reason,
                securityStats: ensureSecurityStats(),
              })
            );
            return;
          }

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

          const normalizedSenderRole = String(payload.role || 'UNIFIED').trim().toUpperCase();
          const isAdminOrUnified = normalizedSenderRole === 'ADMIN' || normalizedSenderRole === 'UNIFIED';
          const senderIpForAudit = getClientIp(req);

          if (isAdminOrUnified && payload.resetEpochMs && Number(payload.resetEpochMs) > syncState.resetEpochMs) {
            syncState.resetEpochMs = Number(payload.resetEpochMs);
            syncState.ordersById = {};
            syncState.chatMessagesById = {};
            syncState.orderRatingsByOrder = {};
            syncState.orderTimelineEventsById = {};
            syncState.unreachableByOrder = {};
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
                const reqStatus = String(reg.approvalStatus || 'NEEDS_KYC').trim().toUpperCase();
                if (!isAdminOrUnified) {
                  const prevStatus = String(existingReg?.approvalStatus || 'NEEDS_KYC').trim().toUpperCase();
                  const tryingPrivilegeEscalation =
                    (reqStatus === 'APPROVED' || reqStatus === 'REJECTED') && reqStatus !== prevStatus;
                  if (tryingPrivilegeEscalation) {
                    recordSecurityBlock(
                      'PRIVILEGE_ESCALATION_BLOCKED',
                      payload.deviceId || senderIpForAudit,
                      `Non-admin (${normalizedSenderRole}) attempted Rider KYC ${reqStatus} on ${rKey}`
                    );
                    continue;
                  }
                  const safeStatus = existingReg ? prevStatus : (reqStatus === 'PENDING_APPROVAL' ? 'PENDING_APPROVAL' : 'NEEDS_KYC');
                  syncState.riderRegistrationsByKey[rKey] = {
                    ...(existingReg || {}),
                    ...reg,
                    approvalStatus: safeStatus,
                    adminRemarks: existingReg?.adminRemarks || '',
                  };
                } else {
                  syncState.riderRegistrationsByKey[rKey] = {
                    ...(existingReg || {}),
                    ...reg,
                  };
                }
              }
            }
          }

          if (Array.isArray(payload.registeredCustomers)) {
            if (!syncState.customerAccountsByKey) {
              syncState.customerAccountsByKey = {};
            }
            for (const acc of payload.registeredCustomers) {
              if (!acc || typeof acc !== 'object') continue;
              const cleanPhone = normalizePhone10(acc.phone);
              const cleanEmail = normalizeEmailLower(acc.email);
              const cKey = buildAccountKey(cleanPhone, cleanEmail);
              if (!cKey) continue;
              const prev = syncState.customerAccountsByKey[cKey];
              if (!prev || Number(acc.updatedAtEpochMs || 0) >= Number(prev.updatedAtEpochMs || 0)) {
                syncState.customerAccountsByKey[cKey] = {
                  ...(prev || {}),
                  accountKey: cKey,
                  role: 'CUSTOMER',
                  name: String(acc.name || (prev && prev.name) || 'Customer').slice(0, 80),
                  phone: cleanPhone || String((prev && prev.phone) || ''),
                  email: cleanEmail || String((prev && prev.email) || ''),
                  registeredAtEpochMs: Number((prev && prev.registeredAtEpochMs) || acc.registeredAtEpochMs || now),
                  updatedAtEpochMs: Number(acc.updatedAtEpochMs || now),
                };
              }
            }
          }

          if (isAdminOrUnified && Array.isArray(payload.promoCoupons)) {
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
                const reqWStatus = String(w.status || 'PENDING').trim().toUpperCase();
                if (!isAdminOrUnified) {
                  const prevWStatus = String(prev?.status || 'PENDING').trim().toUpperCase();
                  if ((reqWStatus === 'PAID' || reqWStatus === 'APPROVED' || reqWStatus === 'REJECTED') && reqWStatus !== prevWStatus) {
                    recordSecurityBlock(
                      'PRIVILEGE_ESCALATION_BLOCKED',
                      payload.deviceId || senderIpForAudit,
                      `Non-admin (${normalizedSenderRole}) attempted Withdrawal ${reqWStatus} on ${wid}`
                    );
                    continue;
                  }
                  if (prev && (prevWStatus === 'PAID' || prevWStatus === 'REJECTED')) continue;
                  syncState.withdrawalRequestsById[wid] = { ...w, status: 'PENDING', adminNote: prev?.adminNote || '' };
                } else {
                  syncState.withdrawalRequestsById[wid] = w;
                }
              }
            }
          }

          if (Array.isArray(payload.supportTickets)) {
            if (!syncState.supportTicketsById) syncState.supportTicketsById = {};
            for (const t of payload.supportTickets) {
              if (!t || !t.ticketId) continue;
              const tid = String(t.ticketId);
              const prev = syncState.supportTicketsById[tid];
              const isBrandNewTicket = !prev;
              if (!prev || Number(t.updatedAtEpochMs || 0) >= Number(prev.updatedAtEpochMs || 0)) {
                syncState.supportTicketsById[tid] = t;
              }
              if (isBrandNewTicket || !(syncState.emailedTicketIds && syncState.emailedTicketIds[tid])) {
                dispatchSupportTicketToWorkspaceEmail(syncState.supportTicketsById[tid]);
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
                const reqSStatus = String(s.status || 'PENDING_ADMIN').trim().toUpperCase();
                if (!isAdminOrUnified) {
                  const prevSStatus = String(prev?.status || 'PENDING_ADMIN').trim().toUpperCase();
                  if ((reqSStatus === 'VERIFIED' || reqSStatus === 'APPROVED') && reqSStatus !== prevSStatus) {
                    recordSecurityBlock(
                      'PRIVILEGE_ESCALATION_BLOCKED',
                      payload.deviceId || senderIpForAudit,
                      `Non-admin (${normalizedSenderRole}) attempted COD Settlement ${reqSStatus} on ${sid}`
                    );
                    continue;
                  }
                  if (prev && prevSStatus === 'VERIFIED') continue;
                  syncState.codSettlementsById[sid] = { ...s, status: 'PENDING_ADMIN' };
                } else {
                  syncState.codSettlementsById[sid] = s;
                }
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
                const reqDStatus = String(d.status || 'OPEN').trim().toUpperCase();
                if (!isAdminOrUnified) {
                  const prevDStatus = String(prev?.status || 'OPEN').trim().toUpperCase();
                  if ((reqDStatus === 'REFUNDED' || reqDStatus === 'REJECTED') && reqDStatus !== prevDStatus) {
                    recordSecurityBlock(
                      'PRIVILEGE_ESCALATION_BLOCKED',
                      payload.deviceId || senderIpForAudit,
                      `Non-admin (${normalizedSenderRole}) attempted Dispute ${reqDStatus} on ${did}`
                    );
                    continue;
                  }
                  if (prev && (prevDStatus === 'REFUNDED' || prevDStatus === 'REJECTED')) continue;
                  syncState.disputeRecordsById[did] = { ...d, status: 'OPEN' };
                } else {
                  syncState.disputeRecordsById[did] = d;
                }
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

          if (isAdminOrUnified && payload.remoteFeatureFlags && typeof payload.remoteFeatureFlags === 'object') {
            const incomingFlags = { ...payload.remoteFeatureFlags };
            if (Number(incomingFlags.flagsSchemaVersion || 1) < 2) {
              incomingFlags.scheduledDeliveryEnabled = true;
              incomingFlags.multiDropEnabled = true;
              incomingFlags.parcelInsuranceEnabled = true;
              incomingFlags.waitTimeAndCancelRuleEnabled = true;
              incomingFlags.peakAutoSurgeEnabled = true;
              incomingFlags.flagsSchemaVersion = 2;
            }
            if (Number(incomingFlags.flagsSchemaVersion || 2) < 3) {
              incomingFlags.b2bPostpaidEnabled = Boolean(incomingFlags.b2bPostpaidEnabled || false);
              incomingFlags.b2bDefaultCreditLimitInr = Number(incomingFlags.b2bDefaultCreditLimitInr || 25000);
              incomingFlags.b2bBillingCycleDays = Number(incomingFlags.b2bBillingCycleDays || 30);
              incomingFlags.razorpayRouteSplitEnabled = Boolean(incomingFlags.razorpayRouteSplitEnabled || false);
              incomingFlags.autoKycApiEnabled = Boolean(incomingFlags.autoKycApiEnabled || false);
              incomingFlags.flagsSchemaVersion = 3;
            }
            if (Number(incomingFlags.flagsSchemaVersion || 3) < 5) {
              incomingFlags.autoEnabled = false;
              incomingFlags.promoCodeEnabled = false;
              incomingFlags.referralWalletEnabled = false;
              incomingFlags.flagsSchemaVersion = 5;
            }
            if (Number(incomingFlags.flagsSchemaVersion || 5) < 6) {
              incomingFlags.pickupDropOtpEnabled = false;
              incomingFlags.flagsSchemaVersion = 6;
            }
            if (Number(incomingFlags.flagsSchemaVersion || 6) < 7) {
              incomingFlags.riderCodCommissionLimitInr = Number(incomingFlags.riderCodCommissionLimitInr || 200);
              incomingFlags.adminUpiSettlementId = String(incomingFlags.adminUpiSettlementId || '');
              incomingFlags.adminSettlementAccountLabel = String(incomingFlags.adminSettlementAccountLabel || '');
              incomingFlags.flagsSchemaVersion = 7;
            }
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

          if (Array.isArray(payload.orderRatings)) {
            if (!syncState.orderRatingsByOrder) syncState.orderRatingsByOrder = {};
            for (const r of payload.orderRatings) {
              if (!r || !r.orderId) continue;
              const oid = String(r.orderId);
              const prev = syncState.orderRatingsByOrder[oid];
              if (!prev || Number(r.updatedAtEpochMs || 0) >= Number(prev.updatedAtEpochMs || 0)) {
                syncState.orderRatingsByOrder[oid] = {
                  ...(prev || {}),
                  ...r,
                  tipAmountInr: Math.max(Number(r.tipAmountInr || 0), Number((prev && prev.tipAmountInr) || 0)),
                };
              }
            }
          }

          if (Array.isArray(payload.orderTimelineEvents)) {
            if (!syncState.orderTimelineEventsById) syncState.orderTimelineEventsById = {};
            for (const evt of payload.orderTimelineEvents) {
              if (!evt || !evt.eventId || !evt.orderId) continue;
              const eid = String(evt.eventId);
              if (!syncState.orderTimelineEventsById[eid]) {
                syncState.orderTimelineEventsById[eid] = evt;
              }
            }
            const allEvents = Object.values(syncState.orderTimelineEventsById);
            if (allEvents.length > 1000) {
              allEvents.sort((a, b) => Number(a.createdAtEpochMs || 0) - Number(b.createdAtEpochMs || 0));
              const nextMap = {};
              for (const ke of allEvents.slice(-1000)) nextMap[ke.eventId] = ke;
              syncState.orderTimelineEventsById = nextMap;
            }
          }

          if (Array.isArray(payload.unreachableByOrder)) {
            if (!syncState.unreachableByOrder) syncState.unreachableByOrder = {};
            for (const rec of payload.unreachableByOrder) {
              if (!rec || !rec.orderId) continue;
              const oid = String(rec.orderId);
              const prev = syncState.unreachableByOrder[oid];
              if (!prev || Number(rec.updatedAtEpochMs || 0) >= Number(prev.updatedAtEpochMs || 0)) {
                syncState.unreachableByOrder[oid] = rec;
              }
            }
          }

          if (Array.isArray(payload.b2bAccounts)) {
            if (!syncState.b2bAccountsById) syncState.b2bAccountsById = {};
            for (const acc of payload.b2bAccounts) {
              if (!acc || !acc.accountId) continue;
              const aid = String(acc.accountId);
              const prev = syncState.b2bAccountsById[aid];
              if (!prev || Number(acc.updatedAtEpochMs || 0) >= Number(prev.updatedAtEpochMs || 0)) {
                const reqB2bStatus = String(acc.status || 'PENDING_ADMIN_APPROVAL').trim().toUpperCase();
                if (!isAdminOrUnified) {
                  const prevB2bStatus = String(prev?.status || 'PENDING_ADMIN_APPROVAL').trim().toUpperCase();
                  if ((reqB2bStatus === 'APPROVED' || reqB2bStatus === 'SUSPENDED') && reqB2bStatus !== prevB2bStatus) {
                    recordSecurityBlock(
                      'PRIVILEGE_ESCALATION_BLOCKED',
                      payload.deviceId || senderIpForAudit,
                      `Non-admin (${normalizedSenderRole}) attempted B2B ${reqB2bStatus} on ${aid}`
                    );
                    continue;
                  }
                  if (prev && prevB2bStatus === 'APPROVED') continue;
                  syncState.b2bAccountsById[aid] = {
                    ...acc,
                    status: 'PENDING_ADMIN_APPROVAL',
                    approvedCreditLimitInr: 0,
                  };
                } else {
                  syncState.b2bAccountsById[aid] = acc;
                }
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
            const senderRole = String(payload.role || 'UNIFIED').trim().toUpperCase();
            const clientIp = getClientIp(req);

            for (const incoming of payload.orders) {
              if (!incoming || !incoming.orderId) continue;
              const oid = String(incoming.orderId);
              if (DEMO_ORDER_IDS.has(oid)) continue;
              const antiSpamCheck = validateIncomingOrderForAntiSpam(incoming, senderDeviceId, now, clientIp);
              if (!antiSpamCheck.ok) {
                console.warn('[LIVE-SYNC BLOCKED]', oid, antiSpamCheck.reason);
                recordSecurityBlock('FAKE_ORDER_BLOCKED', incoming.customerPhone || senderDeviceId, antiSpamCheck.reason);
                rejectedOrdersForResponse.push({ orderId: oid, reason: String(antiSpamCheck.reason || 'Blocked by firewall') });
                continue;
              }
              const existing = syncState.ordersById[oid];
              if (!existing) {
                const cleanGatewayPayId = String(incoming.gatewayPaymentId || '').trim();
                const isPrepaidSpoofByCustomer =
                  senderRole === 'CUSTOMER' &&
                  (incoming.isPaidToCompany === true || incoming.paymentStatus === 'VERIFIED_PAID_TO_COMPANY') &&
                  cleanGatewayPayId.length < 6;
                if (isPrepaidSpoofByCustomer) {
                  recordSecurityBlock('FAKE_ORDER_BLOCKED', incoming.customerPhone || senderDeviceId, `Blocked unpaid CUSTOMER payment spoof on ${oid}`);
                }
                const sanitizedNewOrder = isPrepaidSpoofByCustomer
                  ? { ...incoming, isPaidToCompany: false, paymentStatus: 'UNPAID_CASH_DUE' }
                  : incoming;
                if (sanitizedNewOrder.orderStatus === 'RIDER_ASSIGNED' && senderRole !== 'RIDER' && senderRole !== 'ADMIN' && senderRole !== 'UNIFIED') {
                  syncState.ordersById[oid] = {
                    ...sanitizedNewOrder,
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
                    ...sanitizedNewOrder,
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
                const inStatus = String(incoming.orderStatus || existing.orderStatus).toUpperCase();
                const exStatus = String(existing.orderStatus || 'SEARCHING_RIDER').toUpperCase();
                let inRank = Number(incoming.statusRank || 1);
                const exRank = Number(existing.statusRank || 1);
                const exRejected = Array.isArray(existing.rejectedByRiderIds) ? existing.rejectedByRiderIds : [];
                const inRejected = Array.isArray(incoming.rejectedByRiderIds) ? incoming.rejectedByRiderIds : [];
                const mergedRejected = Array.from(new Set([...exRejected, ...inRejected]));

                // Final terminal states cannot be reversed or rewritten
                if (exStatus === 'DELIVERED' || exStatus === 'CANCELLED') {
                  if (inStatus !== exStatus) continue;
                }

                // A Customer-role device cannot forge Rider delivery status transitions
                const isRiderLifecycleStatus = ['RIDER_ASSIGNED', 'ARRIVED_AT_PICKUP', 'PICKED_UP', 'IN_TRANSIT', 'DELIVERED'].includes(inStatus);
                if (senderRole === 'CUSTOMER' && isRiderLifecycleStatus && inStatus !== exStatus) {
                  recordSecurityBlock('FAKE_ORDER_BLOCKED', senderDeviceId, `Blocked CUSTOMER role status forgery (${exStatus} -> ${inStatus}) on ${oid}`);
                  continue;
                }

                // When Pickup/Drop OTP protection is enabled on server, verify OTP match on status advancement
                if (!isAdminOrUnified && syncState.remoteFeatureFlags?.pickupDropOtpEnabled) {
                  const expectedPickupOtp = String(existing.pickupOtp || '').trim();
                  const expectedDeliveryOtp = String(existing.deliveryOtp || '').trim();
                  if (inStatus === 'PICKED_UP' && exRank < 4 && /^\d{4}$/.test(expectedPickupOtp)) {
                    const proofOtp = String(incoming.pickupProof?.verifiedOtp || '').trim();
                    if (proofOtp !== expectedPickupOtp) {
                      recordSecurityBlock('FAKE_ORDER_BLOCKED', senderDeviceId, `Blocked invalid Pickup OTP bypass on ${oid}`);
                      continue;
                    }
                  }
                  if (inStatus === 'DELIVERED' && exRank < 6 && /^\d{4}$/.test(expectedDeliveryOtp)) {
                    const proofOtp = String(incoming.deliveryProof?.verifiedOtp || '').trim();
                    if (proofOtp !== expectedDeliveryOtp) {
                      recordSecurityBlock('FAKE_ORDER_BLOCKED', senderDeviceId, `Blocked invalid Delivery OTP bypass on ${oid}`);
                      continue;
                    }
                  }
                }

                const isRiderRejectionRollback =
                  inStatus === 'SEARCHING_RIDER' &&
                  mergedRejected.length > exRejected.length &&
                  ['SEARCHING_RIDER', 'RIDER_ASSIGNED', 'ARRIVED_AT_PICKUP'].includes(exStatus);

                const staleAssignForRejectedRider =
                  exStatus === 'SEARCHING_RIDER' &&
                  inStatus === 'RIDER_ASSIGNED' &&
                  incoming.assignedRiderId &&
                  mergedRejected.includes(incoming.assignedRiderId);

                if (
                  exStatus === 'SEARCHING_RIDER' &&
                  inStatus === 'RIDER_ASSIGNED' &&
                  existing.createdByDeviceId &&
                  senderDeviceId === existing.createdByDeviceId &&
                  senderRole !== 'RIDER'
                ) {
                  continue;
                }

                if (staleAssignForRejectedRider) {
                  continue;
                }

                if (isRiderRejectionRollback) {
                  const rolledBack = {
                    ...existing,
                    ...incoming,
                    customerPhone: existing.customerPhone,
                    pickupFullAddress: existing.pickupFullAddress,
                    dropFullAddress: existing.dropFullAddress,
                    totalAmount: existing.totalAmount,
                    platformCommission: existing.platformCommission,
                    riderEarning: existing.riderEarning,
                    orderStatus: 'SEARCHING_RIDER',
                    statusRank: 1,
                    assignedRiderId: '',
                    assignedRiderName: '',
                    assignedRiderPhone: '',
                    assignedRiderVehicle: '',
                    rejectedByRiderIds: mergedRejected,
                    createdByDeviceId: existing.createdByDeviceId || senderDeviceId,
                    updatedAtEpochMs: now,
                  };
                  if (existing.pickupProof && !incoming.pickupProof) rolledBack.pickupProof = existing.pickupProof;
                  if (existing.deliveryProof && !incoming.deliveryProof) rolledBack.deliveryProof = existing.deliveryProof;
                  if (syncState.waitTimerStartByOrder && syncState.waitTimerStartByOrder[oid]) {
                    delete syncState.waitTimerStartByOrder[oid];
                  }
                  syncState.ordersById[oid] = rolledBack;

                  if (syncState.fcmTokensByDevice) {
                    const customerTokens = Object.values(syncState.fcmTokensByDevice)
                      .filter(entry => entry && entry.role === 'CUSTOMER')
                      .map(entry => entry.token);
                    sendFcmPushNotifications({
                      tokens: customerTokens,
                      title: `Rider Declined (#${oid}) — Searching New Rider`,
                      body: rolledBack.latestRiderQuickNote || 'Finding another nearby verified Rider Partner.',
                      data: { orderId: oid, orderStatus: 'SEARCHING_RIDER', targetRole: 'CUSTOMER' },
                    });
                  }
                  continue;
                }

                const merged = inRank >= exRank ? { ...existing, ...incoming } : { ...incoming, ...existing };
                // Enforce financial, route, and customer identity immutability once created
                merged.customerPhone = existing.customerPhone;
                merged.customerName = existing.customerName;
                merged.pickupFullAddress = existing.pickupFullAddress;
                merged.dropFullAddress = existing.dropFullAddress;
                merged.totalAmount = existing.totalAmount;
                merged.platformCommission = existing.platformCommission;
                merged.riderEarning = existing.riderEarning;
                if (existing.isPaidToCompany) {
                  merged.isPaidToCompany = true;
                  merged.paymentStatus = existing.paymentStatus;
                }
                merged.rejectedByRiderIds = mergedRejected;
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

        const senderRoleHeader = String(req.headers['x-qd-app-role'] || 'UNIFIED').trim().toUpperCase();
        let responseRole = senderRoleHeader;
        try {
          const parsedRole = JSON.parse(body || '{}').role;
          if (parsedRole) responseRole = String(parsedRole).trim().toUpperCase();
        } catch {}
        const syncResp = buildSyncResponsePayload(responseRole);
        syncResp.rejectedOrders = Array.isArray(rejectedOrdersForResponse) ? rejectedOrdersForResponse : [];
        const out = JSON.stringify(syncResp);
        res.writeHead(200, {
          'Content-Type': 'application/json; charset=utf-8',
          'Cache-Control': 'no-store',
          'Access-Control-Allow-Origin': '*',
        });
        res.end(out);
      });
      return;
    }

    // Public GET /api/live-sync returns ONLY non-PII aggregate health counters.
    // Full operational state is exclusively available via HMAC-authenticated POST /api/live-sync.
    const fullSnap = buildSyncResponsePayload();
    const secStats = ensureSecurityStats();
    const publicSummary = JSON.stringify({
      ok: true,
      serverTimeEpochMs: fullSnap.serverTimeEpochMs,
      connectedDevicesCount: fullSnap.connectedDevices.length,
      connectedRoles: Array.from(new Set(fullSnap.connectedDevices.map(d => d.role))),
      activeOrdersCount: fullSnap.orders.filter(o => o && o.orderStatus !== 'DELIVERED' && o.orderStatus !== 'CANCELLED').length,
      securityStats: {
        blockedFakeOrders: Number(secStats.blockedFakeOrders || 0),
        rateLimitedRequests: Number(secStats.rateLimitedRequests || 0),
        rejectedInvalidPayloads: Number(secStats.rejectedInvalidPayloads || 0),
      },
    });
    res.writeHead(200, {
      'Content-Type': 'application/json; charset=utf-8',
      'Cache-Control': 'no-store',
      'Access-Control-Allow-Origin': '*',
    });
    res.end(publicSummary);
    return;
  }

  if (pathname === '/api/backup-export') {
    const rateCheck = checkIpRateLimit(req, true);
    if (!rateCheck.allowed) {
      res.writeHead(429, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
      res.end(JSON.stringify({ ok: false, error: 'Too many backup requests. Please wait shortly.' }));
      return;
    }
    const backupParams = new URLSearchParams(queryString);
    const adminBackupKey = String(process.env.ADMIN_BACKUP_KEY || '').trim();
    const authHeader = String(req.headers.authorization || '').trim();
    const bearerKey = authHeader.toLowerCase().startsWith('bearer ') ? authHeader.slice(7).trim() : '';
    const queryKey = String(backupParams.get('key') || '').trim();
    const providedKey = bearerKey || queryKey;
    let isAuthorized = false;

    if (adminBackupKey && providedKey) {
      const a = Buffer.from(providedKey, 'utf-8');
      const b = Buffer.from(adminBackupKey, 'utf-8');
      if (a.length === b.length && crypto.timingSafeEqual(a, b)) {
        isAuthorized = true;
      }
    }

    if (!isAuthorized) {
      const qDeviceId = String(backupParams.get('deviceId') || req.headers['x-qd-device-id'] || '').trim();
      const qTs = String(backupParams.get('ts') || req.headers['x-qd-sync-ts'] || '').trim();
      const qSig = String(backupParams.get('sig') || req.headers['x-qd-sync-sig'] || '').trim();
      if (qDeviceId && qTs && qSig) {
        const fakeReq = { headers: { 'x-qd-sync-ts': qTs, 'x-qd-sync-sig': qSig } };
        const sigCheck = verifySyncHmacHeaders(fakeReq, qDeviceId, 'ADMIN');
        if (sigCheck.ok) {
          isAuthorized = true;
        }
      }
    }

    if (!isAuthorized) {
      recordSecurityBlock('INVALID_SIG', getClientIp(req), 'Unauthorized /api/backup-export attempt blocked');
      res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' });
      res.end(JSON.stringify({ ok: false, error: 'Admin authentication required to export cloud backups.' }));
      return;
    }

    writeDailyBackupSnapshot(true);
    const dayStr = new Date().toISOString().slice(0, 10);
    // Never export ephemeral OTP hashes in backups
    const sanitizedState = { ...syncState };
    delete sanitizedState.emailOtpsByEmail;
    const snapshot = JSON.stringify(
      {
        exportedAtIso: new Date().toISOString(),
        version: APP_SUITE_VERSION,
        ...sanitizedState,
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
    });
    if (req.method === 'HEAD') {
      res.end();
      return;
    }
    res.end(buf);
    return;
  }

  if (pathname === '/api/backup-restore' && req.method === 'POST') {
    const rateCheck = checkIpRateLimit(req, true);
    if (!rateCheck.allowed) {
      res.writeHead(429, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
      res.end(JSON.stringify({ ok: false, error: 'Too many restore requests. Please wait shortly.' }));
      return;
    }
    let rawBody = '';
    req.on('data', chunk => {
      rawBody += chunk;
      if (rawBody.length > 4 * 1024 * 1024) req.destroy();
    });
    req.on('end', () => {
      try {
        const parsedBody = JSON.parse(rawBody || '{}');
        const qDeviceId = String(parsedBody.deviceId || req.headers['x-qd-device-id'] || '').trim();
        const qTs = String(req.headers['x-qd-sync-ts'] || '').trim();
        const qSig = String(req.headers['x-qd-sync-sig'] || '').trim();
        const adminBackupKey = String(process.env.ADMIN_BACKUP_KEY || '').trim();
        const authHeader = String(req.headers.authorization || '').trim();
        const bearerKey = authHeader.toLowerCase().startsWith('bearer ') ? authHeader.slice(7).trim() : '';

        let isAuthorized = false;
        if (adminBackupKey && bearerKey) {
          const a = Buffer.from(bearerKey, 'utf-8');
          const b = Buffer.from(adminBackupKey, 'utf-8');
          if (a.length === b.length && crypto.timingSafeEqual(a, b)) {
            isAuthorized = true;
          }
        }
        if (!isAuthorized && qDeviceId && qTs && qSig) {
          const fakeReq = { headers: { 'x-qd-sync-ts': qTs, 'x-qd-sync-sig': qSig } };
          const sigCheck = verifySyncHmacHeaders(fakeReq, qDeviceId, 'ADMIN');
          if (sigCheck.ok) isAuthorized = true;
        }
        if (!isAuthorized) {
          recordSecurityBlock('INVALID_SIG', getClientIp(req), 'Unauthorized /api/backup-restore attempt blocked');
          res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' });
          res.end(JSON.stringify({ ok: false, error: 'Admin HMAC authentication required to restore backups.' }));
          return;
        }

        const snapshot = parsedBody.snapshot && typeof parsedBody.snapshot === 'object' ? parsedBody.snapshot : parsedBody;
        if (!snapshot || typeof snapshot !== 'object') {
          res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
          res.end(JSON.stringify({ ok: false, error: 'Missing valid backup snapshot object.' }));
          return;
        }

        const mergeMap = (targetKey) => {
          const src = snapshot[targetKey];
          if (!src || typeof src !== 'object' || Array.isArray(src)) return 0;
          if (!syncState[targetKey] || typeof syncState[targetKey] !== 'object') syncState[targetKey] = {};
          let count = 0;
          for (const [k, v] of Object.entries(src)) {
            if (targetKey === 'ordersById' && DEMO_ORDER_IDS.has(k)) continue;
            if (v && typeof v === 'object') {
              syncState[targetKey][k] = v;
              count += 1;
            }
          }
          return count;
        };

        const restoredOrders = mergeMap('ordersById');
        const restoredRiders = mergeMap('riderRegistrationsByKey');
        mergeMap('customerAccountsByKey');
        mergeMap('promoCouponsByCode');
        mergeMap('withdrawalRequestsById');
        mergeMap('supportTicketsById');
        mergeMap('chatMessagesById');
        mergeMap('codSettlementsById');
        mergeMap('disputeRecordsById');
        mergeMap('orderRatingsByOrder');
        mergeMap('b2bAccountsById');
        mergeMap('riderDocExpiriesById');
        if (snapshot.waitTimerStartByOrder && typeof snapshot.waitTimerStartByOrder === 'object') {
          syncState.waitTimerStartByOrder = {
            ...(syncState.waitTimerStartByOrder || {}),
            ...snapshot.waitTimerStartByOrder,
          };
        }
        if (snapshot.remoteFeatureFlags && typeof snapshot.remoteFeatureFlags === 'object') {
          syncState.remoteFeatureFlags = {
            ...(syncState.remoteFeatureFlags || {}),
            ...snapshot.remoteFeatureFlags,
            updatedAtEpochMs: Date.now(),
          };
        }
        saveSyncState();
        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' });
        res.end(JSON.stringify({
          ok: true,
          version: APP_SUITE_VERSION,
          restoredOrders,
          restoredRiders,
          restoredAtIso: new Date().toISOString(),
        }));
      } catch (err) {
        res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ ok: false, error: err && err.message ? err.message : 'Invalid backup JSON payload' }));
      }
    });
    return;
  }

  if (pathname === '/api/backup-email' && req.method === 'POST') {
    const rateCheck = checkIpRateLimit(req, true);
    if (!rateCheck.allowed) {
      res.writeHead(429, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
      res.end(JSON.stringify({ ok: false, error: 'Too many backup email requests. Please wait shortly.' }));
      return;
    }
    let rawBody = '';
    req.on('data', chunk => {
      rawBody += chunk;
      if (rawBody.length > 64 * 1024) req.destroy();
    });
    req.on('end', async () => {
      try {
        const parsedBody = JSON.parse(rawBody || '{}');
        const qDeviceId = String(parsedBody.deviceId || req.headers['x-qd-device-id'] || '').trim();
        const qTs = String(req.headers['x-qd-sync-ts'] || '').trim();
        const qSig = String(req.headers['x-qd-sync-sig'] || '').trim();
        const sigCheck = qDeviceId && qTs && qSig
          ? verifySyncHmacHeaders({ headers: { 'x-qd-sync-ts': qTs, 'x-qd-sync-sig': qSig } }, qDeviceId, 'ADMIN')
          : { ok: false };
        if (!sigCheck.ok) {
          recordSecurityBlock('INVALID_SIG', getClientIp(req), 'Unauthorized /api/backup-email attempt blocked');
          res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' });
          res.end(JSON.stringify({ ok: false, error: 'Admin HMAC authentication required to email backup.' }));
          return;
        }
        const info = await sendBackupSnapshotEmailToAdmin(true);
        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' });
        res.end(JSON.stringify({
          ok: true,
          message: `Backup emailed to ${info.toEmail} via ${info.channel}`,
          toEmail: info.toEmail,
          backedUpAtIso: info.backedUpAtIso,
        }));
      } catch (err) {
        res.writeHead(502, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' });
        res.end(JSON.stringify({ ok: false, error: formatNodeError(err) }));
      }
    });
    return;
  }

  if (pathname === '/api/auth/send-email-otp' && req.method === 'POST') {
    const rateCheck = checkIpRateLimit(req, true);
    if (!rateCheck.allowed) {
      res.writeHead(429, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
      res.end(JSON.stringify({ ok: false, error: 'Too many OTP requests. Please wait 15 seconds and retry.' }));
      return;
    }
    let body = '';
    req.on('data', chunk => {
      body += chunk;
      if (body.length > 64 * 1024) req.destroy();
    });
    req.on('end', async () => {
      try {
        const parsed = JSON.parse(body || '{}');
        const rawEmail = String(parsed.email || '').trim().toLowerCase();
        const role = String(parsed.role || 'CUSTOMER').trim().toUpperCase();
        const deviceId = String(parsed.deviceId || '');
        const sigCheck = verifySyncHmacHeaders(req, deviceId, role);
        if (!sigCheck.ok) {
          res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({ ok: false, error: sigCheck.reason }));
          return;
        }
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(rawEmail)) {
          res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({ ok: false, error: 'Please enter a valid Email Address.' }));
          return;
        }
        if (!syncState.emailOtpsByEmail) syncState.emailOtpsByEmail = {};
        const now = Date.now();
        const existingOtp = syncState.emailOtpsByEmail[rawEmail];
        if (existingOtp && now - Number(existingOtp.createdAtEpochMs || 0) < 25000) {
          const waitSec = Math.ceil((25000 - (now - Number(existingOtp.createdAtEpochMs || 0))) / 1000);
          res.writeHead(429, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({ ok: false, error: `Please wait ${waitSec}s before requesting another Email OTP.` }));
          return;
        }

        const otpCode = String(crypto.randomInt(100000, 1000000));
        const otpHash = crypto.createHmac('sha256', SYNC_HMAC_SECRET).update(`${rawEmail}|${otpCode}`).digest('hex');
        const expiresInSec = 600;
        const roleLabel = role === 'ADMIN' ? 'Admin Console' : role === 'RIDER' ? 'Rider Partner' : 'Customer';
        const subject = `${otpCode} is your Quick Delivery ${roleLabel} verification code`;
        const textBody = [
          `Hello,`,
          ``,
          `Your Quick Delivery (${roleLabel}) Official Verification OTP is: ${otpCode}`,
          ``,
          `This code is valid for 10 minutes. Never share this OTP with any caller or third party.`,
          `Safety & Terms Notice: All shipments include GPS tracking, camera proof, 4-digit handover OTP, and zero-tolerance parcel tamper protection.`,
          `Need help? Email ${BRAND_SUPPORT_EMAIL}`,
          ``,
          `Regards,`,
          `Quick Delivery Security Team`,
        ].join('\n');
        const htmlBody = `
          <div style="font-family:Arial,sans-serif;color:#0f172a;">
            <h2 style="margin:0 0 8px 0;color:#00796B;font-size:18px;">Official Account Verification</h2>
            <p style="margin:0 0 14px 0;font-size:14px;color:#475569;">Use the 6-digit verification code below to authorize your <b>Quick Delivery ${roleLabel}</b> session:</p>
            <div style="background:#f0fdfa;border:2px dashed #14b8a6;border-radius:12px;padding:16px;text-align:center;margin-bottom:16px;">
              <span style="font-size:32px;font-weight:800;letter-spacing:7px;color:#0f172a;">${otpCode}</span>
            </div>
            <p style="margin:0 0 10px 0;font-size:12px;color:#475569;">Valid for <b>10 minutes</b>. Do not share this code with anyone, even callers claiming to be support.</p>
            <div style="background:#fff7ed;border:1px solid #fed7aa;border-radius:10px;padding:10px 12px;font-size:11px;color:#7c2d12;">
              <b>Quick Delivery Security &amp; Support:</b> Write to <a href="mailto:${BRAND_SUPPORT_EMAIL}" style="color:#9a3412;font-weight:700;">${BRAND_SUPPORT_EMAIL}</a> or tap the App Info &amp; Terms links below anytime.
            </div>
          </div>
        `.trim();

        try {
          const deliveryInfo = await sendQuickDeliveryEmail({
            toEmail: rawEmail,
            subject,
            textBody,
            htmlBody,
            type: 'EMAIL_OTP',
            extra: { role: roleLabel },
          });
          // Only persist OTP and enforce 25s resend cooldown AFTER delivery succeeds
          syncState.emailOtpsByEmail[rawEmail] = {
            otpHash,
            role,
            attemptsLeft: 5,
            createdAtEpochMs: Date.now(),
            expiresAtEpochMs: Date.now() + expiresInSec * 1000,
          };
          saveSyncState();
          res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({
            ok: true,
            deliveryChannel: deliveryInfo.deliveryChannel,
            fromEmail: deliveryInfo.fromEmail,
            expiresInSec,
            message: `OTP sent to ${rawEmail} from ${OTP_FROM_EMAIL}. Check Inbox or Spam.`,
          }));
          return;
        } catch (sendErr) {
          const errStatus = Number(sendErr?.statusCode) || 502;
          res.writeHead(errStatus, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({
            ok: false,
            error: formatNodeError(sendErr),
          }));
          return;
        }
      } catch {
        res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
        res.end(JSON.stringify({ ok: false, error: 'Invalid OTP request payload.' }));
      }
    });
    return;
  }

  if (pathname === '/api/auth/verify-email-otp' && req.method === 'POST') {
    const rateCheck = checkIpRateLimit(req, true);
    if (!rateCheck.allowed) {
      res.writeHead(429, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
      res.end(JSON.stringify({ ok: false, error: 'Rate limit exceeded. Try again shortly.' }));
      return;
    }
    let body = '';
    req.on('data', chunk => {
      body += chunk;
      if (body.length > 64 * 1024) req.destroy();
    });
    req.on('end', () => {
      try {
        const parsed = JSON.parse(body || '{}');
        const rawEmail = String(parsed.email || '').trim().toLowerCase();
        const enteredOtp = String(parsed.otp || '').replace(/\D/g, '').trim();
        const role = String(parsed.role || 'CUSTOMER').trim().toUpperCase();
        const deviceId = String(parsed.deviceId || '');
        const sigCheck = verifySyncHmacHeaders(req, deviceId, role);
        if (!sigCheck.ok) {
          res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({ ok: false, error: sigCheck.reason }));
          return;
        }
        if (!syncState.emailOtpsByEmail) syncState.emailOtpsByEmail = {};
        const rec = syncState.emailOtpsByEmail[rawEmail];
        const now = Date.now();
        if (!rec) {
          res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({ ok: false, error: 'No active OTP found for this email. Please tap Send Email OTP.' }));
          return;
        }
        if (now > Number(rec.expiresAtEpochMs || 0)) {
          delete syncState.emailOtpsByEmail[rawEmail];
          saveSyncState();
          res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({ ok: false, error: 'OTP expired (10 min limit). Please request a new Email OTP.' }));
          return;
        }
        if (Number(rec.attemptsLeft || 0) <= 0) {
          delete syncState.emailOtpsByEmail[rawEmail];
          saveSyncState();
          res.writeHead(429, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({ ok: false, error: 'Maximum wrong attempts reached. Request a fresh OTP.' }));
          return;
        }
        const expectedHash = crypto.createHmac('sha256', SYNC_HMAC_SECRET).update(`${rawEmail}|${enteredOtp}`).digest('hex');
        const expectedBuf = Buffer.from(expectedHash, 'utf-8');
        const storedBuf = Buffer.from(String(rec.otpHash || ''), 'utf-8');
        const otpMatches = enteredOtp.length === 6 && expectedBuf.length === storedBuf.length && crypto.timingSafeEqual(expectedBuf, storedBuf);
        if (!otpMatches) {
          rec.attemptsLeft = Math.max(0, Number(rec.attemptsLeft || 1) - 1);
          saveSyncState();
          res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({ ok: false, error: `Invalid OTP. ${rec.attemptsLeft} attempts left.` }));
          return;
        }
        delete syncState.emailOtpsByEmail[rawEmail];
        if (!syncState.verifiedEmails) syncState.verifiedEmails = {};
        syncState.verifiedEmails[rawEmail] = { verifiedAtEpochMs: now, role };
        saveSyncState();
        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
        res.end(JSON.stringify({ ok: true, verified: true, email: rawEmail, message: 'Email verified successfully.' }));
      } catch {
        res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
        res.end(JSON.stringify({ ok: false, error: 'Invalid OTP verification payload.' }));
      }
    });
    return;
  }

  if (pathname === '/api/auth/register-account' && req.method === 'POST') {
    const rateCheck = checkIpRateLimit(req, true);
    if (!rateCheck.allowed) {
      res.writeHead(429, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
      res.end(JSON.stringify({ ok: false, error: 'Too many signup requests. Please wait shortly.' }));
      return;
    }
    let body = '';
    req.on('data', chunk => {
      body += chunk;
      if (body.length > 32 * 1024) req.destroy();
    });
    req.on('end', () => {
      try {
        const parsed = JSON.parse(body || '{}');
        const role = String(parsed.role || 'CUSTOMER').trim().toUpperCase();
        const deviceId = String(parsed.deviceId || '');
        const sigCheck = verifySyncHmacHeaders(req, deviceId, role);
        if (!sigCheck.ok) {
          res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({ ok: false, error: sigCheck.reason }));
          return;
        }
        const cleanPhone = normalizePhone10(parsed.phone);
        const cleanEmail = normalizeEmailLower(parsed.email);
        const cleanName = String(parsed.name || '').trim().slice(0, 80);
        if (!cleanPhone && !cleanEmail) {
          res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({ ok: false, error: 'Valid 10-digit mobile number or email is required.' }));
          return;
        }
        if (cleanPhone && (BLOCKED_FAKE_PHONES.has(cleanPhone) || new Set(cleanPhone.split('')).size <= 1)) {
          res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({ ok: false, error: 'Please enter a valid 10-digit Indian mobile number.' }));
          return;
        }
        const now = Date.now();
        let savedAccount = null;
        if (role === 'RIDER') {
          const rKey = buildAccountKey(cleanPhone, cleanEmail);
          if (!syncState.riderRegistrationsByKey) syncState.riderRegistrationsByKey = {};
          const prev = syncState.riderRegistrationsByKey[rKey] || {};
          syncState.riderRegistrationsByKey[rKey] = {
            ...prev,
            riderKey: rKey,
            riderId: prev.riderId || `RIDER-KYC-${(cleanPhone || String(now)).slice(-4)}`,
            name: cleanName || prev.name || 'Rider Partner',
            phone: cleanPhone || prev.phone || '',
            email: cleanEmail || prev.email || '',
            approvalStatus: prev.approvalStatus || 'NEEDS_KYC',
            updatedAtEpochMs: now,
          };
          savedAccount = { role: 'RIDER', name: syncState.riderRegistrationsByKey[rKey].name, phone: cleanPhone, email: cleanEmail };
        } else {
          savedAccount = registerCustomerAccountInState({ name: cleanName, phone: cleanPhone, email: cleanEmail, now });
        }
        saveSyncState();
        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
        res.end(JSON.stringify({ ok: true, registered: true, account: savedAccount, message: 'Account registered permanently.' }));
      } catch {
        res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
        res.end(JSON.stringify({ ok: false, error: 'Invalid account registration payload.' }));
      }
    });
    return;
  }

  if (pathname === '/api/auth/check-existing-account' && req.method === 'POST') {
    const rateCheck = checkIpRateLimit(req, true);
    if (!rateCheck.allowed) {
      res.writeHead(429, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
      res.end(JSON.stringify({ ok: false, error: 'Too many login checks. Please wait shortly.' }));
      return;
    }
    let body = '';
    req.on('data', chunk => {
      body += chunk;
      if (body.length > 32 * 1024) req.destroy();
    });
    req.on('end', () => {
      try {
        const parsed = JSON.parse(body || '{}');
        const role = String(parsed.role || 'CUSTOMER').trim().toUpperCase();
        const deviceId = String(parsed.deviceId || '');
        const sigCheck = verifySyncHmacHeaders(req, deviceId, role);
        if (!sigCheck.ok) {
          res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({ ok: false, error: sigCheck.reason }));
          return;
        }
        const identifier = String(parsed.identifier || '').trim();
        const found = findRegisteredAccountInState(role, identifier);
        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
        if (found) {
          res.end(JSON.stringify({
            ok: true,
            exists: true,
            account: found,
            message: 'Registered account verified.',
          }));
        } else {
          res.end(JSON.stringify({
            ok: true,
            exists: false,
            message: 'Account not found. Please tap New User Sign Up first.',
          }));
        }
      } catch {
        res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
        res.end(JSON.stringify({ ok: false, error: 'Invalid existing account check payload.' }));
      }
    });
    return;
  }

  if (pathname === '/api/admin/credentials/status' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => {
      body += chunk;
      if (body.length > 32 * 1024) req.destroy();
    });
    req.on('end', () => {
      try {
        const parsed = JSON.parse(body || '{}');
        const deviceId = String(parsed.deviceId || '');
        const sigCheck = verifySyncHmacHeaders(req, deviceId, 'ADMIN');
        if (!sigCheck.ok) {
          res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({ ok: false, error: sigCheck.reason }));
          return;
        }
        const creds = syncState.adminCredentials;
        const configured = Boolean(creds && creds.userId && creds.saltHex && creds.hashHex);
        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
        res.end(JSON.stringify({
          ok: true,
          configured,
          userId: configured ? creds.userId : '',
          recoveryEmailMasked: configured ? maskEmailForHint(creds.recoveryEmail) : '',
        }));
      } catch {
        res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
        res.end(JSON.stringify({ ok: false, error: 'Invalid admin credentials status request.' }));
      }
    });
    return;
  }

  if (pathname === '/api/admin/credentials/save' && req.method === 'POST') {
    const rateCheck = checkIpRateLimit(req, true);
    if (!rateCheck.allowed) {
      res.writeHead(429, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
      res.end(JSON.stringify({ ok: false, error: 'Too many requests. Please wait shortly.' }));
      return;
    }
    let body = '';
    req.on('data', chunk => {
      body += chunk;
      if (body.length > 32 * 1024) req.destroy();
    });
    req.on('end', async () => {
      try {
        const parsed = JSON.parse(body || '{}');
        const deviceId = String(parsed.deviceId || '');
        const sigCheck = verifySyncHmacHeaders(req, deviceId, 'ADMIN');
        if (!sigCheck.ok) {
          recordSecurityBlock('INVALID_SIG', getClientIp(req), `Admin credentials save sig failed: ${sigCheck.reason}`);
          saveSyncState();
          res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({ ok: false, error: sigCheck.reason }));
          return;
        }
        const userId = String(parsed.userId || '').trim().toLowerCase();
        const recoveryEmail = String(parsed.recoveryEmail || '').trim().toLowerCase();
        const saltHex = String(parsed.saltHex || '').trim().toLowerCase();
        const hashHex = String(parsed.hashHex || '').trim().toLowerCase();
        const authProof = String(parsed.authProof || '').trim().toUpperCase();
        const currentPassword = String(parsed.currentPassword || '');
        const googleIdToken = String(parsed.googleIdToken || '').trim();

        if (!/^[a-z0-9._-]{4,32}$/.test(userId)) {
          res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({ ok: false, error: 'User ID must be 4-32 characters (letters, numbers, . _ -).' }));
          return;
        }
        if (!ADMIN_ALLOWLIST_EMAILS.has(recoveryEmail)) {
          recordSecurityBlock('INVALID_SIG', getClientIp(req), `Non-allowlisted Admin email blocked: ${recoveryEmail}`);
          saveSyncState();
          res.writeHead(403, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({ ok: false, error: 'Only allowlisted Admin emails can configure Admin login.' }));
          return;
        }
        if (!/^[0-9a-f]{32}$/.test(saltHex) || !/^[0-9a-f]{64}$/.test(hashHex)) {
          res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({ ok: false, error: 'Invalid password hash format.' }));
          return;
        }

        const existing = syncState.adminCredentials;
        const verifiedRec = syncState.verifiedEmails && syncState.verifiedEmails[recoveryEmail];
        const hasRecentOtp = Boolean(verifiedRec && (Date.now() - Number(verifiedRec.verifiedAtEpochMs || 0)) <= 15 * 60 * 1000);
        let authorizedToSave = false;
        let authRejectReason = 'Verification required (Email OTP, Google ID Token, or Current Password) to update Admin credentials.';

        if (authProof === 'EMAIL_OTP' && hasRecentOtp) {
          authorizedToSave = true;
        } else if (authProof === 'CURRENT_PASSWORD') {
          if (existing && existing.saltHex && existing.hashHex) {
            const checkHash = computeStretchedAdminPasswordHash(existing.saltHex, currentPassword);
            const a = Buffer.from(checkHash, 'utf8');
            const b = Buffer.from(String(existing.hashHex), 'utf8');
            authorizedToSave = a.length === b.length && crypto.timingSafeEqual(a, b);
          } else if (!existing) {
            authorizedToSave = true;
          }
        } else if (authProof === 'SETTINGS_INITIAL_SETUP' && !existing) {
          authorizedToSave = true;
        } else if (authProof === 'GOOGLE_SIGNIN') {
          if (hasRecentOtp) {
            authorizedToSave = true;
          } else if (googleIdToken) {
            const gCheck = await verifyGoogleIdTokenForAdmin(googleIdToken, recoveryEmail);
            authorizedToSave = gCheck.ok;
            if (!gCheck.ok) authRejectReason = gCheck.reason;
          } else {
            authRejectReason = 'Google ID token verification or Email OTP is required to save Admin credentials.';
          }
        }

        if (!authorizedToSave) {
          recordSecurityBlock('INVALID_SIG', getClientIp(req), `Admin credentials overwrite rejected (${authProof})`);
          saveSyncState();
          res.writeHead(403, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({ ok: false, error: authRejectReason }));
          return;
        }

        syncState.adminCredentials = {
          userId,
          recoveryEmail,
          saltHex,
          hashHex,
          updatedAtEpochMs: Date.now(),
        };
        saveSyncState();
        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
        res.end(JSON.stringify({ ok: true, userId, message: 'Admin User ID & Password saved securely.' }));
      } catch {
        res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
        res.end(JSON.stringify({ ok: false, error: 'Invalid save admin credentials payload.' }));
      }
    });
    return;
  }

  if (pathname === '/api/admin/credentials/verify' && req.method === 'POST') {
    const rateCheck = checkIpRateLimit(req, true);
    if (!rateCheck.allowed) {
      res.writeHead(429, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
      res.end(JSON.stringify({ ok: false, error: 'Too many login attempts. Please wait 30 seconds.' }));
      return;
    }
    let body = '';
    req.on('data', chunk => {
      body += chunk;
      if (body.length > 32 * 1024) req.destroy();
    });
    req.on('end', () => {
      try {
        const parsed = JSON.parse(body || '{}');
        const deviceId = String(parsed.deviceId || '');
        const sigCheck = verifySyncHmacHeaders(req, deviceId, 'ADMIN');
        if (!sigCheck.ok) {
          recordSecurityBlock('INVALID_SIG', getClientIp(req), `Admin verify sig failed: ${sigCheck.reason}`);
          saveSyncState();
          res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({ ok: false, error: sigCheck.reason }));
          return;
        }
        const now = Date.now();
        const lockKey = `${getClientIp(req)}|${deviceId || 'unknown'}`;
        const failRec = adminLoginFailureBuckets.get(lockKey);
        if (failRec && failRec.lockedUntilMs > now) {
          const waitSec = Math.ceil((failRec.lockedUntilMs - now) / 1000);
          res.writeHead(429, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({ ok: false, error: `Admin login locked due to failed tries. Retry in ${waitSec}s.` }));
          return;
        }

        const existing = syncState.adminCredentials;
        if (!existing || !existing.userId || !existing.saltHex || !existing.hashHex) {
          res.writeHead(404, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({ ok: false, error: 'No Admin User ID & Password set yet. Please complete First-Time Setup.' }));
          return;
        }
        const identifier = String(parsed.identifier || '').trim().toLowerCase();
        const password = String(parsed.password || '');
        const idMatches = identifier === existing.userId || identifier === existing.recoveryEmail;
        const computed = computeStretchedAdminPasswordHash(existing.saltHex, password);
        const a = Buffer.from(computed, 'utf8');
        const b = Buffer.from(String(existing.hashHex), 'utf8');
        const hashMatches = a.length === b.length && crypto.timingSafeEqual(a, b);
        if (!idMatches || !hashMatches) {
          const activeFails = failRec && (now - failRec.firstFailAtMs) < ADMIN_LOGIN_WINDOW_MS ? failRec.count + 1 : 1;
          const firstFailAtMs = failRec && (now - failRec.firstFailAtMs) < ADMIN_LOGIN_WINDOW_MS ? failRec.firstFailAtMs : now;
          const lockedUntilMs = activeFails >= MAX_ADMIN_LOGIN_FAILURES ? now + ADMIN_LOGIN_LOCKOUT_MS : 0;
          adminLoginFailureBuckets.set(lockKey, { count: activeFails, firstFailAtMs, lockedUntilMs });
          if (lockedUntilMs > now) {
            recordSecurityBlock('RATE_LIMIT', lockKey, `Admin password brute-force lockout (${activeFails} failed attempts)`);
            saveSyncState();
          }
          res.writeHead(lockedUntilMs > now ? 429 : 401, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({
            ok: false,
            error: lockedUntilMs > now
              ? 'Too many wrong Admin login attempts. Locked for 60 seconds.'
              : 'Incorrect Admin User ID or Password.',
          }));
          return;
        }
        adminLoginFailureBuckets.delete(lockKey);
        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
        res.end(JSON.stringify({
          ok: true,
          userId: existing.userId,
          recoveryEmail: existing.recoveryEmail,
          saltHex: existing.saltHex,
          hashHex: existing.hashHex,
          updatedAtEpochMs: Number(existing.updatedAtEpochMs || Date.now()),
          message: 'Admin verified.',
        }));
      } catch {
        res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
        res.end(JSON.stringify({ ok: false, error: 'Invalid admin verify payload.' }));
      }
    });
    return;
  }

  if (pathname === '/api/payments/create-order' && req.method === 'POST') {
    const rateCheck = checkIpRateLimit(req, true);
    if (!rateCheck.allowed) {
      res.writeHead(429, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
      res.end(JSON.stringify({ ok: false, error: 'Too many payment requests. Please wait shortly.' }));
      return;
    }
    let body = '';
    req.on('data', chunk => {
      body += chunk;
      if (body.length > 256 * 1024) req.destroy();
    });
    req.on('end', () => {
      try {
        const parsed = JSON.parse(body || '{}');
        const deviceId = String(parsed.deviceId || '');
        const role = String(parsed.role || 'CUSTOMER').trim().toUpperCase();
        const sigCheck = verifySyncHmacHeaders(req, deviceId, role);
        if (!sigCheck.ok) {
          res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({ ok: false, error: sigCheck.reason }));
          return;
        }
        const orderId = String(parsed.orderId || `QD-${Date.now() % 10000}`).replace(/[^A-Za-z0-9_-]/g, '').slice(0, 40);
        const amountInr = Number(parsed.amountInr || 0);
        if (!Number.isFinite(amountInr) || amountInr < 1 || amountInr > 500000) {
          res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
          res.end(JSON.stringify({ ok: false, error: 'Amount must be between ₹1 and ₹5,00,000.' }));
          return;
        }
        const amountPaise = Math.round(amountInr * 100);
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
      const secret = String(process.env.RAZORPAY_WEBHOOK_SECRET || '').trim();
      if (!secret) {
        recordSecurityBlock('INVALID_SIG', getClientIp(req), 'Rejected payment webhook: RAZORPAY_WEBHOOK_SECRET not configured');
        res.writeHead(503, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ ok: false, error: 'Payment webhook secret not configured on server.' }));
        return;
      }
      const sigHeader = String(req.headers['x-razorpay-signature'] || '').trim();
      const expected = crypto.createHmac('sha256', secret).update(rawBody).digest('hex');
      const sigBuf = Buffer.from(sigHeader, 'utf-8');
      const expBuf = Buffer.from(expected, 'utf-8');
      if (!sigHeader || sigBuf.length !== expBuf.length || !crypto.timingSafeEqual(sigBuf, expBuf)) {
        recordSecurityBlock('INVALID_SIG', getClientIp(req), 'Invalid Razorpay webhook signature');
        res.writeHead(401, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ ok: false, error: 'Invalid webhook signature' }));
        return;
      }
      try {
        const evt = JSON.parse(rawBody || '{}');
        const paymentEntity = evt?.payload?.payment?.entity || {};
        const qdOrderId = String(paymentEntity?.notes?.qdOrderId || evt.orderId || '');
        if (qdOrderId && syncState.ordersById[qdOrderId]) {
          syncState.ordersById[qdOrderId].paymentStatus = 'VERIFIED_PAID_TO_COMPANY';
          syncState.ordersById[qdOrderId].isPaidToCompany = true;
          if (paymentEntity?.id) {
            syncState.ordersById[qdOrderId].gatewayPaymentId = String(paymentEntity.id);
          }
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

  if (pathname === '/api/app-update-manifest') {
    const manifestJson = JSON.stringify(
      {
        ok: true,
        ...buildServerAppUpdateManifest(),
      },
      null,
      2
    );
    res.writeHead(200, {
      'Content-Type': 'application/json; charset=utf-8',
      'Cache-Control': 'no-store',
      'Access-Control-Allow-Origin': '*',
    });
    res.end(manifestJson);
    return;
  }

  if (pathname === '/api/health') {
    const syncPayload = buildSyncResponsePayload();
    const rolesConnected = Array.from(new Set(syncPayload.connectedDevices.map(d => d.role)));
    const activeSearchingOrders = syncPayload.orders.filter(o => o && o.orderStatus === 'SEARCHING_RIDER').length;
    const apkBuilds = Object.fromEntries(
      APK_FILE_NAMES.map(name => {
        const p = resolveSourceApkPath(name);
        return [
          name,
          p
            ? {
                sizeBytes: fs.statSync(p).size,
                sha256Short: getFileSha256Short(p),
                mtimeIso: fs.statSync(p).mtime.toISOString(),
              }
            : null,
        ];
      })
    );
    const secStats = ensureSecurityStats();
    const healthJson = JSON.stringify(
      {
        ok: true,
        service: 'QuickDelivery Live Dispatch & APK Server',
        version: APP_SUITE_VERSION,
        domain: BRAND_DOMAIN,
        targetSupportEmail: process.env.SUPPORT_TO_EMAIL || BRAND_SUPPORT_EMAIL,
        serverTimeIso: new Date().toISOString(),
        apkBuilds,
        connectedDevicesCount: syncPayload.connectedDevices.length,
        connectedRoles: rolesConnected,
        totalOrders: syncPayload.orders.length,
        activeSearchingOrders,
        registeredRiders: syncPayload.riderRegistrations.length,
        registeredCustomers: syncPayload.registeredCustomers.length,
        withdrawalRequestsCount: syncPayload.withdrawalRequests.length,
        permanentCloudProvider: detectPermanentCloudProvider(),
        persistentDiskPath: STORAGE_PATHS.diskPath,
        permanentCloudHydrated,
        lastEmailBackupIso,
        securityStats: {
          blockedFakeOrders: Number(secStats.blockedFakeOrders || 0),
          rateLimitedRequests: Number(secStats.rateLimitedRequests || 0),
          rejectedInvalidPayloads: Number(secStats.rejectedInvalidPayloads || 0),
        },
      },
      null,
      2
    );
    res.writeHead(200, {
      'Content-Type': 'application/json; charset=utf-8',
      'Cache-Control': 'no-store',
      'Access-Control-Allow-Origin': '*',
    });
    res.end(healthJson);
    return;
  }

  if (pathname === '/api/support-email-status') {
    const dispatches = Object.values(syncState.emailedTicketIds || {});
    const brevoConfigured = Boolean(process.env.BREVO_API_KEY);
    const resendConfigured = Boolean(process.env.RESEND_API_KEY);
    const webhookConfigured = Boolean(process.env.SUPPORT_EMAIL_WEBHOOK_URL);
    const smtpConfigured = Boolean(process.env.SUPPORT_SMTP_USER && (process.env.SUPPORT_SMTP_APP_PASSWORD || process.env.SUPPORT_SMTP_PASS));
    const activeProvider = brevoConfigured
      ? 'BREVO_API'
      : resendConfigured
      ? 'RESEND_API'
      : webhookConfigured
      ? 'WEBHOOK'
      : smtpConfigured
      ? 'SMTP'
      : 'NONE';
    const statusPayload = JSON.stringify(
      {
        ok: true,
        targetEmail: process.env.SUPPORT_TO_EMAIL || BRAND_SUPPORT_EMAIL,
        fromEmail: OTP_FROM_EMAIL,
        activeProvider,
        brevoConfigured,
        resendConfigured,
        smtpConfigured,
        webhookConfigured,
        lastEmailDiagnostics,
        totalTickets: Object.keys(syncState.supportTicketsById || {}).length,
        emailedTickets: dispatches.length,
        recentDispatches: dispatches.slice(-15),
      },
      null,
      2
    );
    res.writeHead(200, {
      'Content-Type': 'application/json; charset=utf-8',
      'Cache-Control': 'no-store',
      'Access-Control-Allow-Origin': '*',
    });
    res.end(statusPayload);
    return;
  }

  if (pathname.startsWith('/apks/') || pathname.startsWith('/dl/') || pathname.endsWith('.apk')) {
    const requestedFile = path.basename(pathname).replace(/[^A-Za-z0-9._-]/g, '') || 'QuickDelivery-Customer.apk';
    const finalFileName = requestedFile.endsWith('.apk') ? requestedFile : `${requestedFile}.apk`;
    const apkFilePath = resolveSourceApkPath(finalFileName);

    if (apkFilePath && fs.existsSync(apkFilePath)) {
      const stat = fs.statSync(apkFilePath);
      const total = stat.size;
      const downloadFileName = getVersionedDownloadFileName(finalFileName);
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
            'Content-Disposition': `attachment; filename="${downloadFileName}"`,
            'Cache-Control': 'no-store, no-cache, must-revalidate',
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
        'Content-Disposition': `attachment; filename="${downloadFileName}"`,
        'Content-Length': String(total),
        'Accept-Ranges': 'bytes',
        'Cache-Control': 'no-store, no-cache, must-revalidate',
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

  if (pathname === '/brand-logo.jpg' || pathname === '/icon-192.png' || pathname === '/icon-512.png' || pathname === '/icon.svg') {
    const assetCandidates = pathname === '/brand-logo.jpg'
      ? [
          { file: path.join(ROOT, 'app', 'src', 'main', 'res', 'drawable', 'cute_scooter_bunny_logo_1794655841710.jpg'), mime: 'image/jpeg' },
          { file: path.join(ROOT, 'public', 'icon-192.png'), mime: 'image/png' },
        ]
      : [
          {
            file: path.join(ROOT, 'public', pathname.slice(1)),
            mime: pathname.endsWith('.svg') ? 'image/svg+xml; charset=utf-8' : 'image/png',
          },
        ];
    for (const candidate of assetCandidates) {
      if (fs.existsSync(candidate.file)) {
        const assetBytes = fs.readFileSync(candidate.file);
        res.writeHead(200, {
          'Content-Type': candidate.mime,
          'Content-Length': String(assetBytes.length),
          'Cache-Control': 'public, max-age=86400',
          'Access-Control-Allow-Origin': '*',
        });
        if (req.method === 'HEAD') {
          res.end();
          return;
        }
        res.end(assetBytes);
        return;
      }
    }
  }

  if (
    pathname === '/privacy-policy' ||
    pathname === '/terms' ||
    pathname === '/delete-account' ||
    pathname === '/about' ||
    pathname === '/support'
  ) {
    const kind =
      pathname === '/terms'
        ? 'terms'
        : pathname === '/delete-account'
        ? 'delete'
        : pathname === '/about' || pathname === '/support'
        ? 'about'
        : 'privacy';
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
  const isRoleApkRoute =
    pathname === '/customer' ||
    pathname === '/customer-apk' ||
    pathname === '/rider' ||
    pathname === '/rider-apk' ||
    pathname === '/admin' ||
    pathname === '/admin-apk' ||
    pathname === '/hub' ||
    pathname === '/hub-apk' ||
    pathname === '/suite' ||
    pathname === '/all-apks' ||
    Boolean(autoDownload) ||
    Boolean(roleFilter);

  if (pathname === '/customer' || pathname === '/customer-apk' || autoDownload.includes('Customer')) {
    autoDownload = 'QuickDelivery-Customer.apk';
    roleFilter = 'CUSTOMER';
  } else if (pathname === '/rider' || pathname === '/rider-apk' || autoDownload.includes('Rider')) {
    autoDownload = 'QuickDelivery-Rider-Partner.apk';
    roleFilter = 'RIDER';
  } else if (pathname === '/admin' || pathname === '/admin-apk' || autoDownload.includes('Admin')) {
    autoDownload = 'QuickDelivery-Admin-Enterprise.apk';
    roleFilter = 'ADMIN';
  } else if (
    pathname === '/hub' ||
    pathname === '/hub-apk' ||
    autoDownload.includes('AllInOne') ||
    autoDownload.includes('Hub')
  ) {
    autoDownload = 'QuickDelivery-AllInOne-Hub.apk';
    roleFilter = 'HUB';
  }

  if (pathname === '/api/preview/state' || pathname === '/api/preview/action') {
    const reqHost = String(req.headers['x-forwarded-host'] || req.headers.host || '').toLowerCase();
    const isAllowedPreviewHost =
      !reqHost.includes(BRAND_DOMAIN) &&
      (IS_AISTUDIO_CONTAINER || reqHost.includes('localhost') || reqHost.includes('127.0.0.1') || reqHost.includes('.run.app'));
    if (!isAllowedPreviewHost) {
      res.writeHead(403, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify({ ok: false, error: 'Preview sandbox disabled on public domain.' }));
      return;
    }

    if (pathname === '/api/preview/state') {
      const snap = buildSyncResponsePayload();
      res.writeHead(200, {
        'Content-Type': 'application/json; charset=utf-8',
        'Cache-Control': 'no-store',
      });
      res.end(JSON.stringify(snap));
      return;
    }

    if (req.method === 'POST') {
      let body = '';
      req.on('data', chunk => {
        if (body.length < 256 * 1024) body += chunk;
      });
      req.on('end', () => {
        try {
          const input = JSON.parse(body || '{}');
          const act = String(input.action || '');
          const now = Date.now();
          let message = '';

          if (!syncState.remoteFeatureFlags) {
            syncState.remoteFeatureFlags = {
              flagsSchemaVersion: 7,
              bikeBaseInr: 45,
              bikePerKmInr: 12,
              scooterBaseInr: 40,
              scooterPerKmInr: 10,
              autoBaseInr: 85,
              autoPerKmInr: 16,
              weightBaseInr: 10,
              riderCodCommissionLimitInr: 200,
              promoCodeEnabled: true,
              scheduledDeliveryEnabled: true,
              multiDropEnabled: true,
              parcelInsuranceEnabled: true,
              overSpeedAlarmEnabled: true,
              batteryGuardEnabled: true,
              updatedAtEpochMs: now,
            };
          }

          if (act === 'BOOK_ORDER') {
            const fl = syncState.remoteFeatureFlags;
            const veh = String(input.vehicleType || 'BIKE').toUpperCase();
            const dist = Math.max(1, Number(input.distanceKm || 5));
            const base =
              veh === 'SCOOTER'
                ? Number(fl.scooterBaseInr || 40)
                : veh === 'AUTO'
                ? Number(fl.autoBaseInr || 85)
                : Number(fl.bikeBaseInr || 45);
            const perKm =
              veh === 'SCOOTER'
                ? Number(fl.scooterPerKmInr || 10)
                : veh === 'AUTO'
                ? Number(fl.autoPerKmInr || 16)
                : Number(fl.bikePerKmInr || 12);
            const wb = String(input.weightBracket || 'Up to 5kg');
            const wFee = wb === '15-20kg' ? Number(fl.weightBaseInr || 10) * 2 : wb === '5-15kg' ? Number(fl.weightBaseInr || 10) : 0;
            const sub = base + Math.round(dist * perKm) + wFee;
            let disc = 0;
            const code = String(input.promoCode || '').trim().toUpperCase();
            if (code && syncState.promoCouponsByCode && syncState.promoCouponsByCode[code]) {
              const cp = syncState.promoCouponsByCode[code];
              disc = Math.min(Number(cp.maxDiscountInr || 50), Math.round(sub * (Number(cp.percentOff || 15) / 100)));
            }
            const total = Math.max(25, sub - disc);
            const riderEarn = Math.round(total * 0.8);
            const oid = `QD-${String(now).slice(-5)}`;
            const pOtp = String(1000 + Math.floor(Math.random() * 9000));
            const dOtp = String(1000 + Math.floor(Math.random() * 9000));
            syncState.ordersById[oid] = {
              orderId: oid,
              orderStatus: 'SEARCHING_RIDER',
              statusRank: 1,
              customerName: String(input.customerName || 'Customer').slice(0, 60),
              customerPhone: String(input.customerPhone || '9876543210').slice(0, 15),
              receiverName: String(input.receiverName || 'Receiver').slice(0, 60),
              receiverPhone: String(input.receiverPhone || '9845098450').slice(0, 15),
              pickupFullAddress: String(input.pickupAddress || 'MG Road').slice(0, 140),
              dropFullAddress: String(input.dropAddress || 'Koramangala').slice(0, 140),
              vehicleType: veh,
              packageCategory: String(input.packageCategory || 'Documents'),
              weightBracket: wb,
              distanceKm: dist,
              paymentMode: String(input.paymentMethod || 'UPI_INSTANT_PAY'),
              totalFare: `₹${total}`,
              riderEarning: `₹${riderEarn}`,
              pickupOtp: pOtp,
              deliveryOtp: dOtp,
              rejectedByRiderIds: [],
              createdByDeviceId: 'PREVIEW_HUB',
              updatedAtEpochMs: now,
            };
            message = `✅ Order #${oid} booked (₹${total})! Switch to Rider tab to accept.`;
          } else if (act === 'CANCEL_ORDER') {
            const o = syncState.ordersById[String(input.orderId || '')];
            if (o) {
              o.orderStatus = 'CANCELLED';
              o.cancellationReason = String(input.reason || 'Cancelled');
              o.statusRank = 7;
              o.updatedAtEpochMs = now;
              message = `Order #${o.orderId} cancelled`;
            }
          } else if (act === 'RIDER_TOGGLE_DUTY') {
            const rId = String(input.riderId || 'RIDER-PREVIEW-01');
            if (input.online) {
              syncState.devicesById[rId] = { deviceId: rId, role: 'RIDER', deviceModel: 'Preview Rider', lastSeenEpochMs: now };
              message = '🟢 Rider is now ON DUTY';
            } else {
              delete syncState.devicesById[rId];
              message = '⚪ Rider is now OFFLINE';
            }
          } else if (act === 'RIDER_REGISTER_KYC') {
            const key = String(input.phone || input.riderId || 'RIDER-PREVIEW-01');
            if (!syncState.riderRegistrationsByKey) syncState.riderRegistrationsByKey = {};
            syncState.riderRegistrationsByKey[key] = {
              riderKey: key,
              riderId: String(input.riderId || 'RIDER-PREVIEW-01'),
              riderName: String(input.riderName || 'Rider'),
              mobile: String(input.phone || ''),
              vehicleType: String(input.vehicleType || 'BIKE'),
              vehicleNumber: String(input.vehicleNumber || 'KA-01-QD-4021'),
              status: 'PENDING_APPROVAL',
              updatedAtEpochMs: now,
            };
            message = '🪪 Rider KYC submitted! Check Admin → Rider KYC tab.';
          } else if (act === 'RIDER_ACCEPT_ORDER') {
            const o = syncState.ordersById[String(input.orderId || '')];
            if (o && o.orderStatus === 'SEARCHING_RIDER') {
              o.orderStatus = 'RIDER_ASSIGNED';
              o.statusRank = 2;
              o.assignedRiderId = String(input.riderId || 'RIDER-PREVIEW-01');
              o.assignedRiderName = String(input.riderName || 'Vikram Singh');
              o.assignedRiderVehicle = String(input.riderVehicle || 'KA-01-QD-4021');
              o.updatedAtEpochMs = now;
              message = `✅ Accepted #${o.orderId}! Proceed to pickup.`;
            }
          } else if (act === 'RIDER_REJECT_ORDER') {
            const o = syncState.ordersById[String(input.orderId || '')];
            if (o) {
              o.orderStatus = 'SEARCHING_RIDER';
              o.statusRank = 1;
              o.assignedRiderId = '';
              o.assignedRiderName = '';
              o.rejectedByRiderIds = Array.from(new Set([...(o.rejectedByRiderIds || []), String(input.riderId || 'RIDER-PREVIEW-01')]));
              o.updatedAtEpochMs = now;
              message = `Rejected #${o.orderId} (rolled back to Searching)`;
            }
          } else if (act === 'RIDER_ARRIVE_PICKUP') {
            const o = syncState.ordersById[String(input.orderId || '')];
            if (o) {
              o.orderStatus = 'ARRIVED_AT_PICKUP';
              o.statusRank = 3;
              o.updatedAtEpochMs = now;
              message = `📍 Marked Arrived at Pickup for #${o.orderId}`;
            }
          } else if (act === 'RIDER_VERIFY_PICKUP_OTP') {
            const o = syncState.ordersById[String(input.orderId || '')];
            if (!o || String(input.otp || '').trim() !== String(o.pickupOtp)) {
              res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
              res.end(JSON.stringify({ ok: false, error: 'Invalid 4-digit Pickup OTP! Check Customer tab.' }));
              return;
            }
            o.orderStatus = 'PICKED_UP';
            o.statusRank = 4;
            o.pickupProof = { senderName: String(input.senderName || 'Sender'), verifiedAtEpochMs: now };
            o.updatedAtEpochMs = now;
            message = `✅ Pickup OTP verified! Order #${o.orderId} picked up.`;
          } else if (act === 'RIDER_START_TRANSIT') {
            const o = syncState.ordersById[String(input.orderId || '')];
            if (o) {
              o.orderStatus = 'IN_TRANSIT';
              o.statusRank = 5;
              o.updatedAtEpochMs = now;
              message = `🚀 Order #${o.orderId} is now IN TRANSIT`;
            }
          } else if (act === 'RIDER_VERIFY_DROP_OTP') {
            const o = syncState.ordersById[String(input.orderId || '')];
            if (!o || String(input.otp || '').trim() !== String(o.deliveryOtp)) {
              res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
              res.end(JSON.stringify({ ok: false, error: 'Invalid 4-digit Drop OTP! Check Customer tab.' }));
              return;
            }
            o.orderStatus = 'DELIVERED';
            o.statusRank = 6;
            o.deliveryProof = { receiverName: String(input.receiverName || 'Receiver'), verifiedAtEpochMs: now };
            o.updatedAtEpochMs = now;
            message = `🎉 Order #${o.orderId} DELIVERED! Rider payout credited.`;
          } else if (act === 'RIDER_WITHDRAW_PAYOUT') {
            const reqId = `WD-${String(now).slice(-5)}`;
            if (!syncState.withdrawalRequestsById) syncState.withdrawalRequestsById = {};
            syncState.withdrawalRequestsById[reqId] = {
              requestId: reqId,
              riderId: String(input.riderId || 'RIDER-PREVIEW-01'),
              riderName: String(input.riderName || 'Vikram Singh'),
              amountInr: Number(input.amountInr || 100),
              upiId: String(input.upiId || 'rider@upi'),
              status: 'PENDING',
              updatedAtEpochMs: now,
            };
            message = `⚡ Payout request #${reqId} submitted to Admin!`;
          } else if (act === 'SEND_CHAT') {
            const mid = `MSG-${now}`;
            if (!syncState.chatMessagesById) syncState.chatMessagesById = {};
            syncState.chatMessagesById[mid] = {
              messageId: mid,
              orderId: String(input.orderId || ''),
              senderRole: String(input.senderRole || 'CUSTOMER'),
              senderName: String(input.senderName || 'User'),
              messageText: String(input.messageText || ''),
              timestampLabel: new Date(now).toLocaleTimeString(),
              createdAtEpochMs: now,
            };
            message = '💬 Message sent';
          } else if (act === 'SUBMIT_SUPPORT_TICKET') {
            const tid = `TKT-${String(now).slice(-5)}`;
            if (!syncState.supportTicketsById) syncState.supportTicketsById = {};
            syncState.supportTicketsById[tid] = {
              ticketId: tid,
              role: String(input.role || 'CUSTOMER'),
              orderId: String(input.orderId || ''),
              message: String(input.message || ''),
              contactName: String(input.contactName || ''),
              contactPhone: String(input.contactPhone || ''),
              status: 'OPEN',
              updatedAtEpochMs: now,
            };
            message = `🛟 Ticket #${tid} logged in Admin Console!`;
          } else if (act === 'RATE_ORDER') {
            const oid = String(input.orderId || '');
            if (!syncState.orderRatingsByOrder) syncState.orderRatingsByOrder = {};
            syncState.orderRatingsByOrder[oid] = {
              orderId: oid,
              stars: Number(input.stars || 5),
              tipAmountInr: Number(input.tipAmountInr || 0),
              feedback: String(input.feedback || ''),
              updatedAtEpochMs: now,
            };
            message = `⭐ Rated #${oid} with ₹${Number(input.tipAmountInr || 0)} tip!`;
          } else if (act === 'ADMIN_UPDATE_FLAGS') {
            syncState.remoteFeatureFlags = {
              ...(syncState.remoteFeatureFlags || {}),
              flagsSchemaVersion: 7,
              bikeBaseInr: Number(input.bikeBaseInr || 45),
              bikePerKmInr: Number(input.bikePerKmInr || 12),
              scooterBaseInr: Number(input.scooterBaseInr || 40),
              scooterPerKmInr: Number(input.scooterPerKmInr || 10),
              autoBaseInr: Number(input.autoBaseInr || 85),
              autoPerKmInr: Number(input.autoPerKmInr || 16),
              weightBaseInr: Number(input.weightBaseInr || 10),
              riderCodCommissionLimitInr: Number(input.riderCodCommissionLimitInr || 200),
              promoCodeEnabled: Boolean(input.promoCodeEnabled),
              scheduledDeliveryEnabled: Boolean(input.scheduledDeliveryEnabled),
              parcelInsuranceEnabled: Boolean(input.parcelInsuranceEnabled),
              overSpeedAlarmEnabled: Boolean(input.overSpeedAlarmEnabled),
              batteryGuardEnabled: Boolean(input.batteryGuardEnabled),
              updatedAtEpochMs: now,
            };
            message = '✅ Pricing & Feature Flags synced across all 3 apps!';
          } else if (act === 'ADMIN_SAVE_COUPON') {
            const code = String(input.code || 'SAVE20').trim().toUpperCase();
            if (!syncState.promoCouponsByCode) syncState.promoCouponsByCode = {};
            syncState.promoCouponsByCode[code] = {
              code,
              percentOff: Number(input.percentOff || 20),
              maxDiscountInr: Number(input.maxDiscountInr || 40),
              active: true,
              updatedAtEpochMs: now,
            };
            message = `🏷️ Coupon ${code} synced to Customer App!`;
          } else if (act === 'ADMIN_REVIEW_RIDER') {
            const rKey = String(input.riderKey || '');
            if (syncState.riderRegistrationsByKey && syncState.riderRegistrationsByKey[rKey]) {
              syncState.riderRegistrationsByKey[rKey].status = String(input.status || 'APPROVED');
              syncState.riderRegistrationsByKey[rKey].updatedAtEpochMs = now;
              message = `Rider KYC marked ${syncState.riderRegistrationsByKey[rKey].status}`;
            }
          } else if (act === 'ADMIN_APPROVE_WITHDRAWAL') {
            const wid = String(input.requestId || '');
            if (syncState.withdrawalRequestsById && syncState.withdrawalRequestsById[wid]) {
              syncState.withdrawalRequestsById[wid].status = 'PAID';
              syncState.withdrawalRequestsById[wid].updatedAtEpochMs = now;
              message = `💸 Payout #${wid} marked PAID`;
            }
          } else if (act === 'ADMIN_RESOLVE_TICKET') {
            const tid = String(input.ticketId || '');
            if (syncState.supportTicketsById && syncState.supportTicketsById[tid]) {
              syncState.supportTicketsById[tid].status = 'RESOLVED';
              syncState.supportTicketsById[tid].updatedAtEpochMs = now;
              message = `✅ Ticket #${tid} resolved`;
            }
          } else if (act === 'ADMIN_RESET_ORDERS') {
            syncState.ordersById = {};
            syncState.chatMessagesById = {};
            syncState.resetEpochMs = now;
            message = '🧹 All test orders reset';
          }

          saveSyncState();
          res.writeHead(200, {
            'Content-Type': 'application/json; charset=utf-8',
            'Cache-Control': 'no-store',
          });
          res.end(JSON.stringify({ ok: true, message, state: buildSyncResponsePayload() }));
        } catch (err) {
          res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
          res.end(JSON.stringify({ ok: false, error: 'Invalid preview action payload' }));
        }
      });
      return;
    }
  }

  const rawHost = String(req.headers['x-forwarded-host'] || req.headers.host || '').toLowerCase();
  const isPublicBrandDomain = rawHost.includes(BRAND_DOMAIN);
  const viewMode = (params.get('view') || '').toLowerCase();
  const isAiStudioOrLocal =
    IS_AISTUDIO_CONTAINER || rawHost.includes('localhost') || rawHost.includes('127.0.0.1') || rawHost.includes('.run.app');
  const showInteractivePreviewOnRoot =
    pathname === '/' &&
    viewMode !== 'public' &&
    viewMode !== 'apks' &&
    !isPublicBrandDomain &&
    isAiStudioOrLocal;
  const showApkSuiteOnRoot =
    pathname === '/' &&
    viewMode === 'apks' &&
    !isPublicBrandDomain &&
    isAiStudioOrLocal;

  // Root URL on quickdelivery.co.in MUST show the official public brand page — NEVER expose
  // internal Rider/Admin views or APK download links on the live customer domain.
  // In AI Studio Preview / localhost, root / shows the Interactive 3-App Preview Hub
  // unless ?view=apks (4-APK download center) or ?view=public (public brand portal) is passed.
  const html = (isRoleApkRoute || showApkSuiteOnRoot)
    ? renderDownloadPageHtml(autoDownload, roleFilter)
    : showInteractivePreviewOnRoot
    ? readInteractivePreviewHtml()
    : renderFirebaseHostingHomeHtml();
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
  } catch (err) {
    console.error('Server request error:', err);
    try {
      res.writeHead(500, { 'Content-Type': 'text/plain; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
      res.end('Quick Delivery Server Error: ' + (err?.message || String(err)));
    } catch {}
  }
});

server.on('error', () => {
  process.exit(0);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Quick Delivery Mobile APK & Live Sync Server listening on http://0.0.0.0:${PORT} (Cloud: ${detectPermanentCloudProvider()})`);
  hydrateSyncStateFromPermanentCloud().catch(() => {});
  // Schedule automatic 24-hour backup email to Admin Gmail (safe no-op if email provider env is unset)
  const DAILY_BACKUP_INTERVAL_MS = 24 * 60 * 60 * 1000;
  setInterval(() => {
    if (Object.keys(syncState.ordersById || {}).length > 0 || Object.keys(syncState.riderRegistrationsByKey || {}).length > 0) {
      sendBackupSnapshotEmailToAdmin(false).catch(() => {});
    }
  }, DAILY_BACKUP_INTERVAL_MS).unref();
});
