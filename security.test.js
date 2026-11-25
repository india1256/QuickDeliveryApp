import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

// Mock browser localStorage for securityGuard.ts
const memoryStorage = new Map();
globalThis.localStorage = {
  getItem: (key) => (memoryStorage.has(key) ? memoryStorage.get(key) : null),
  setItem: (key, val) => memoryStorage.set(key, String(val)),
  removeItem: (key) => memoryStorage.delete(key),
  clear: () => memoryStorage.clear(),
};

test('1. Firestore Security Rules Audit: verifies default-deny, role isolation, 80/20 arithmetic, XSS rejection, and status transition lockdown', () => {
  const rules = fs.readFileSync('./firestore.rules', 'utf8');
  const blueprint = JSON.parse(fs.readFileSync('./firebase-blueprint.json', 'utf8'));

  // 1a. Global default-deny safety net
  assert.match(rules, /match \/\{document=\*\*\} \{\s*allow read, write: if false;\s*\}/);

  // 1b. Admin verification requires verified email or /admins/{uid}
  assert.match(rules, /request\.auth\.token\.email_verified == true/);

  // 1c. Input sanitization helper rejects XSS and NoSQL injection patterns
  assert.match(rules, /function isSafeString\(text\)/);
  assert.match(rules, /<script/);
  assert.match(rules, /javascript:/);
  assert.match(rules, /\\\$where/);

  // 1d. DeliveryOrder enforces exact 80/20 fare split arithmetic invariant
  assert.match(
    rules,
    /data\.totalAmountPaise == data\.companyCommissionPaise \+ data\.riderEarningPaise/
  );

  // 1e. Non-admin customer order creation must start at PENDING_ASSIGNMENT without pre-assigned rider
  assert.match(rules, /incoming\(\)\.status == 'PENDING_ASSIGNMENT'/);
  assert.match(rules, /!\('assignedRiderUid' in incoming\(\)\)/);

  // 1f. Customer order update cannot self-mark DELIVERED (only CANCELLED before pickup)
  assert.match(
    rules,
    /incoming\(\)\.status == 'CANCELLED' && existing\(\)\.status in \['PENDING_ASSIGNMENT', 'ASSIGNED', 'ARRIVED_AT_PICKUP'\]/
  );

  // 1g. User profiles locked to owner and block non-admin escalation to ADMIN
  assert.match(rules, /match \/user_profiles\/\{userId\}/);
  assert.match(rules, /incoming\(\)\.role == existing\(\)\.role && incoming\(\)\.role in \['CUSTOMER', 'RIDER'\]/);
  assert.ok(blueprint.entities.UserProfile, 'Blueprint must include UserProfile schema');
  assert.ok(
    blueprint.firestore['/user_profiles/{userId}'],
    'Blueprint must include /user_profiles/{userId} path'
  );
});

test('2. Input Sanitization: strips XSS <script>, event handlers, and NoSQL injection operators', async () => {
  const { sanitizeText, sanitizeIdentifier, sanitizePhone, sanitizeEmail } = await import(
    './src/services/securityGuard.ts'
  );

  const maliciousAddress =
    "<script>alert('pwned')</script> Indiranagar $where javascript:steal() onerror=hack() 100ft Road";
  const cleaned = sanitizeText(maliciousAddress, 200);
  assert.equal(cleaned.toLowerCase().includes('<script'), false);
  assert.equal(cleaned.toLowerCase().includes('javascript:'), false);
  assert.equal(cleaned.toLowerCase().includes('onerror='), false);
  assert.equal(cleaned.toLowerCase().includes('$where'), false);
  assert.equal(cleaned.includes('Indiranagar'), true);

  assert.equal(sanitizeIdentifier('ord_qd-5001;<script>'), 'ord_qd-5001__script_');
  assert.equal(sanitizePhone('+91 98765-43210<script>'), '9876543210');
  assert.equal(sanitizeEmail('USER@Example.com<script>'), 'user@example.com');
});

test('3. Role-Bound Authentication Session Token: signs and rejects role escalation or token tampering', async () => {
  const { issueRoleSessionToken, verifyRoleSessionToken } = await import(
    './src/services/securityGuard.ts'
  );

  const sessionToken = issueRoleSessionToken({
    id: 'cust_101',
    role: 'CUSTOMER',
    phone: '9876543210',
    email: 'customer@quickdelivery.in',
  });

  // Valid customer session passes verification
  assert.equal(
    verifyRoleSessionToken({
      id: 'cust_101',
      role: 'CUSTOMER',
      phone: '9876543210',
      email: 'customer@quickdelivery.in',
      sessionToken,
    }),
    true
  );

  // Tampering role to ADMIN with a CUSTOMER token must fail
  assert.equal(
    verifyRoleSessionToken({
      id: 'cust_101',
      role: 'ADMIN',
      phone: '9876543210',
      email: 'customer@quickdelivery.in',
      sessionToken,
    }),
    false
  );

  // Tampering signature characters must fail
  assert.equal(
    verifyRoleSessionToken({
      id: 'cust_101',
      role: 'CUSTOMER',
      phone: '9876543210',
      email: 'customer@quickdelivery.in',
      sessionToken: `${sessionToken}deadbeef`,
    }),
    false
  );
});

test('4. Order Placement Rate Limiting & Anti-Spam Protection: blocks duplicate bursts and mass-order floods', async () => {
  const {
    checkOrderPlacementRateLimit,
    checkEndpointRateLimit,
    resetSecurityRateLimitsForTesting,
  } = await import('./src/services/securityGuard.ts');

  resetSecurityRateLimitsForTesting();

  // First order succeeds
  const first = checkOrderPlacementRateLimit({
    userId: 'cust_spam_test',
    pickupAddress: 'MG Road Metro Station',
    dropAddress: 'Koramangala 4th Block',
    vehicleType: 'BIKE',
    activePendingCount: 0,
  });
  assert.equal(first.allowed, true);

  // Immediate duplicate order on identical route within 8s cooldown is blocked
  const duplicateBurst = checkOrderPlacementRateLimit({
    userId: 'cust_spam_test',
    pickupAddress: 'MG Road Metro Station',
    dropAddress: 'Koramangala 4th Block',
    vehicleType: 'BIKE',
    activePendingCount: 1,
  });
  assert.equal(duplicateBurst.allowed, false);
  assert.match(duplicateBurst.reason || '', /Duplicate order detected/i);

  // Up to 5 distinct orders within 1 minute succeed, 6th rapid order is blocked
  for (let i = 2; i <= 5; i++) {
    const res = checkOrderPlacementRateLimit({
      userId: 'cust_spam_test',
      pickupAddress: `Pickup Point ${i}`,
      dropAddress: `Drop Point ${i}`,
      vehicleType: 'BIKE',
      activePendingCount: 1,
    });
    assert.equal(res.allowed, true);
  }

  const sixthOrder = checkOrderPlacementRateLimit({
    userId: 'cust_spam_test',
    pickupAddress: 'Pickup Point 6',
    dropAddress: 'Drop Point 6',
    vehicleType: 'BIKE',
    activePendingCount: 1,
  });
  assert.equal(sixthOrder.allowed, false);
  assert.match(sixthOrder.reason || '', /Rate limit exceeded/i);

  // Excessive active pending orders (>= 5) is blocked even for a fresh user
  const queueFlood = checkOrderPlacementRateLimit({
    userId: 'cust_fresh_user',
    pickupAddress: 'Indiranagar',
    dropAddress: 'Whitefield',
    vehicleType: 'EV',
    activePendingCount: 5,
  });
  assert.equal(queueFlood.allowed, false);
  assert.match(queueFlood.reason || '', /unassigned orders/i);

  // Endpoint rate limiter blocks excessive auth brute-force (> 6/min)
  for (let i = 1; i <= 6; i++) {
    assert.equal(checkEndpointRateLimit('AUTH_ATTEMPT', 'brute_user').allowed, true);
  }
  const seventhAuth = checkEndpointRateLimit('AUTH_ATTEMPT', 'brute_user');
  assert.equal(seventhAuth.allowed, false);

  resetSecurityRateLimitsForTesting();
});

test('5. Automated Delivery Delay Alert (>10 mins): notifies both Customer and Rider and sets statusLabel to Delayed', async () => {
  const { notificationService } = await import('./src/services/notificationService.ts');

  notificationService.clearAll();

  // Trigger delay alert when estimatedMinutes (34) exceeds scheduledMinutes (20) by 14 mins (> 10 mins)
  const result = notificationService.triggerOrderDelayAlerts({
    orderId: 'ord_qd-7001',
    orderCode: 'QD-7001',
    customerName: 'Aarav Sharma',
    riderName: 'Vikram Singh',
    dropAddress: 'Indiranagar 100ft Road',
    scheduledMinutes: 20,
    estimatedMinutes: 34,
  });

  const allNotifications = notificationService.getNotifications();

  assert.equal(result.delayMinutes, 14);
  assert.equal(result.statusLabel, 'Delayed');
  assert.equal(allNotifications.length, 2, 'Must dispatch notifications to both Customer and Rider');

  const rolesNotified = allNotifications.map((n) => n.role).sort();
  assert.deepEqual(rolesNotified, ['CUSTOMER', 'RIDER']);
  assert.ok(allNotifications.every((n) => n.type === 'ORDER_DELAYED'));
  assert.ok(allNotifications.every((n) => n.message.includes('Delayed')));

  // Verify evaluateOrderDelayState sets isDelayed=true and statusLabel='Delayed' when estimatedMinutes - scheduledMinutes > 10
  const delayedOrder = notificationService.evaluateOrderDelayState({
    id: 'ord_qd-7001',
    orderCode: 'QD-7001',
    status: 'IN_TRANSIT',
    scheduledMinutes: 20,
    estimatedMinutes: 34,
  });
  assert.equal(delayedOrder.isDelayed, true);
  assert.equal(delayedOrder.statusLabel, 'Delayed');
  assert.equal(delayedOrder.delayMinutes, 14);

  // Verify on-time order (<= 10 mins difference) is not marked Delayed
  const onTimeOrder = notificationService.evaluateOrderDelayState({
    id: 'ord_qd-7002',
    orderCode: 'QD-7002',
    status: 'IN_TRANSIT',
    scheduledMinutes: 20,
    estimatedMinutes: 28,
  });
  assert.equal(onTimeOrder.isDelayed, false);
  assert.equal(onTimeOrder.statusLabel, undefined);
});

test('6. Pickup & Delivery Proof Digital Signature Enforcement: requires live camera photo + finger-drawn SVG signature & signer name at both Pickup and Delivery', async () => {
  const { validatePickupProofPayload, validateDeliveryProofPayload } = await import(
    './src/services/securityGuard.ts'
  );

  const sampleSvg =
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 360 140"><path d="M 12 40 L 85 92 L 190 54" stroke="#0F766E" stroke-width="3" fill="none"/></svg>';

  // 6a. Pickup Proof rejects missing signature
  const missingPickupSig = validatePickupProofPayload('ord_qd-8001', {
    riderId: 'RIDER-101',
    verifiedOtp: '4821',
    packagePhotoUrl: 'https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=600',
    senderName: 'Aarav Sharma',
    senderSignatureSvg: '',
  });
  assert.equal(missingPickupSig.success, false);

  // 6b. Pickup Proof succeeds when both camera photo and Sender's digital signature SVG are provided
  const validPickupProof = validatePickupProofPayload('ord_qd-8001', {
    riderId: 'RIDER-101',
    verifiedOtp: '4821',
    packagePhotoUrl: 'https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=600',
    packagePhotoSummary: 'Live Pickup Camera Photo (HD Seal Verified)',
    senderName: 'Aarav Sharma',
    senderSignatureSvg: sampleSvg,
  });
  assert.equal(validPickupProof.success, true);
  assert.equal(validPickupProof.endpoint, '/api/v1/riders/orders/ord_qd-8001/proof-pickup');
  assert.equal(validPickupProof.pickupProof?.senderName, 'Aarav Sharma');
  assert.equal(validPickupProof.pickupProof?.senderSignatureSvg, sampleSvg);

  // 6c. Delivery Proof rejects missing camera photo or missing recipient signature
  const missingDeliverySig = validateDeliveryProofPayload('ord_qd-8001', {
    riderId: 'RIDER-101',
    verifiedOtp: '9134',
    deliveryPhotoUrl: 'https://images.unsplash.com/photo-1566576912321-d58ddd7a6088?w=600',
    recipientName: 'Rohan Verma',
    recipientSignatureSvg: '',
  });
  assert.equal(missingDeliverySig.success, false);

  // 6d. Delivery Proof succeeds when both camera drop photo and Recipient's digital signature SVG are provided
  const validDeliveryProof = validateDeliveryProofPayload('ord_qd-8001', {
    riderId: 'RIDER-101',
    verifiedOtp: '9134',
    deliveryPhotoUrl: 'https://images.unsplash.com/photo-1566576912321-d58ddd7a6088?w=600',
    deliveryPhotoSummary: 'Live Doorstep Drop Camera Photo Verified',
    recipientName: 'Rohan Verma',
    recipientSignatureSvg: sampleSvg,
    codAmountCollectedInr: 120,
  });
  assert.equal(validDeliveryProof.success, true);
  assert.equal(validDeliveryProof.endpoint, '/api/v1/riders/orders/ord_qd-8001/proof-delivery');
  assert.equal(validDeliveryProof.deliveryProof?.recipientName, 'Rohan Verma');
  assert.equal(validDeliveryProof.deliveryProof?.recipientSignatureSvg, sampleSvg);
});

test('7. Rider Battery Monitoring Service: automatically throttles GPS update frequency to 10s when battery level drops below 20% to extend device uptime', async () => {
  const { batteryMonitoringService } = await import('./src/services/batteryMonitoringService.ts');
  const { gpsService } = await import('./src/services/gpsTrackingService.ts');

  // 7a. Normal battery level (85%, discharging) -> Standard real-time 2s GPS update frequency
  const normalStatus = batteryMonitoringService.simulateBatteryLevel(85, false);
  assert.equal(normalStatus.levelPercent, 85);
  assert.equal(normalStatus.isCharging, false);
  assert.equal(normalStatus.isBatterySaverActive, false);
  assert.equal(normalStatus.isGpsThrottled, false);
  assert.equal(normalStatus.gpsUpdateIntervalMs, 2000);
  assert.equal(gpsService.getUpdateIntervalMs(), 2000);
  assert.equal(gpsService.isThrottled(), false);

  // 7b. Battery drops below 20% (15%, discharging) -> Automatically activates battery saver and reduces GPS update frequency to 10s
  const lowBatteryStatus = batteryMonitoringService.simulateBatteryLevel(15, false);
  assert.equal(lowBatteryStatus.levelPercent, 15);
  assert.equal(lowBatteryStatus.isBatterySaverActive, true);
  assert.equal(lowBatteryStatus.isGpsThrottled, true);
  assert.equal(lowBatteryStatus.gpsUpdateIntervalMs, 10000);
  assert.equal(gpsService.getUpdateIntervalMs(), 10000);
  assert.equal(gpsService.isThrottled(), true);
  assert.match(lowBatteryStatus.gpsFrequencyLabel, /10s/);

  // 7c. Boundary conditions:
  // Exactly 20% is NOT throttled (must drop strictly below 20%)
  const boundary20 = batteryMonitoringService.simulateBatteryLevel(20, false);
  assert.equal(boundary20.isBatterySaverActive, false);
  assert.equal(boundary20.isGpsThrottled, false);
  assert.equal(boundary20.gpsUpdateIntervalMs, 2000);
  assert.equal(gpsService.getUpdateIntervalMs(), 2000);

  // 19% (< 20%) is throttled
  const boundary19 = batteryMonitoringService.simulateBatteryLevel(19, false);
  assert.equal(boundary19.isBatterySaverActive, true);
  assert.equal(boundary19.isGpsThrottled, true);
  assert.equal(boundary19.gpsUpdateIntervalMs, 10000);
  assert.equal(gpsService.getUpdateIntervalMs(), 10000);

  // 7d. Low battery while plugged in / charging (15%, isCharging = true) -> GPS is NOT throttled
  const chargingStatus = batteryMonitoringService.simulateBatteryLevel(15, true);
  assert.equal(chargingStatus.levelPercent, 15);
  assert.equal(chargingStatus.isCharging, true);
  assert.equal(chargingStatus.isBatterySaverActive, false);
  assert.equal(chargingStatus.isGpsThrottled, false);
  assert.equal(chargingStatus.gpsUpdateIntervalMs, 2000);
  assert.equal(gpsService.getUpdateIntervalMs(), 2000);

  // 7e. Restoring healthy battery resets GPS frequency back to 2s
  const restored = batteryMonitoringService.simulateBatteryLevel(100, false);
  assert.equal(restored.isGpsThrottled, false);
  assert.equal(gpsService.getUpdateIntervalMs(), 2000);
});

test('8. OTP Email & Root Website Security: OTP emails must NOT link to quickdelivery.co.in and root / must serve public brand page without exposing Admin/Rider APKs', () => {
  const serverSource = fs.readFileSync('./scripts/apk-download-server.mjs', 'utf8');

  // 8a. Suite version synced to 6.7.2
  assert.match(serverSource, /const APP_SUITE_VERSION = '6\.7\.2';/);

  // 8b. Extract send-email-otp handler and verify ZERO clickable quickdelivery.co.in links in OTP text/HTML
  const otpStart = serverSource.indexOf("if (pathname === '/api/auth/send-email-otp' && req.method === 'POST')");
  assert.ok(otpStart > 0, 'send-email-otp handler must exist');
  const otpEnd = serverSource.indexOf('const deliveryInfo = await sendQuickDeliveryEmail(', otpStart);
  assert.ok(otpEnd > otpStart, 'sendQuickDeliveryEmail invocation must exist inside OTP handler');
  const otpBlock = serverSource.slice(otpStart, otpEnd);

  assert.equal(otpBlock.includes('http://quickdelivery.co.in'), false, 'OTP email must not contain http://quickdelivery.co.in');
  assert.equal(otpBlock.includes('https://quickdelivery.co.in'), false, 'OTP email must not contain https://quickdelivery.co.in');
  assert.equal(otpBlock.includes('https://${BRAND_DOMAIN}'), false, 'OTP email must not embed https://${BRAND_DOMAIN}');
  assert.equal(otpBlock.includes('${BRAND_DOMAIN}'), false, 'OTP email must not embed bare ${BRAND_DOMAIN} that Gmail auto-links');
  assert.match(otpBlock, /Quick Delivery Security/);

  // 8c. Root landing page function must NOT link to Rider/Admin APKs or /suite
  const homeStart = serverSource.indexOf('function renderFirebaseHostingHomeHtml(');
  assert.ok(homeStart > 0, 'renderFirebaseHostingHomeHtml must exist');
  const homeEnd = serverSource.indexOf('function configureNginxDirectApkServing(', homeStart);
  assert.ok(homeEnd > homeStart, 'renderFirebaseHostingHomeHtml boundary must exist');
  const homeBlock = serverSource.slice(homeStart, homeEnd);

  assert.equal(homeBlock.includes('QuickDelivery-Admin-Enterprise.apk'), false, 'Public home must not expose Admin APK');
  assert.equal(homeBlock.includes('QuickDelivery-Rider-Partner.apk'), false, 'Public home must not expose Rider APK');
  assert.equal(homeBlock.includes('/admin-apk'), false, 'Public home must not link to /admin-apk');
  assert.equal(homeBlock.includes('/rider-apk'), false, 'Public home must not link to /rider-apk');
  assert.match(homeBlock, /Privacy Policy/);
  assert.match(homeBlock, /Terms of Service/);

  // 8d. Root route '/' on quickdelivery.co.in strictly serves renderFirebaseHostingHomeHtml() and blocks /api/preview/* while /customer, /rider, /admin, /suite routes stay active
  assert.match(serverSource, /const showInteractivePreviewOnRoot =\s*pathname === '\/'[\s\S]*!isPublicBrandDomain/);
  assert.match(serverSource, /pathname === '\/api\/preview\/state' \|\| pathname === '\/api\/preview\/action'[\s\S]*!reqHost\.includes\(BRAND_DOMAIN\)[\s\S]*403/);
  assert.match(serverSource, /const html = \(isRoleApkRoute \|\| showApkSuiteOnRoot\)\s*\?\s*renderDownloadPageHtml\(autoDownload, roleFilter\)\s*:\s*showInteractivePreviewOnRoot\s*\?\s*readInteractivePreviewHtml\(\)\s*:\s*renderFirebaseHostingHomeHtml\(\);/);
  assert.match(serverSource, /pathname === '\/suite'/);
  assert.match(serverSource, /pathname === '\/customer'/);
  assert.match(serverSource, /pathname === '\/rider'/);
  assert.match(serverSource, /pathname === '\/admin'/);
});

test('9. Maximum Anti-Fake-Order Security & Phone Storage Permission Audit: blocks fake Indian mobile patterns, self-order fraud, customer status forgery, and enables storage permissions', () => {
  const serverSource = fs.readFileSync('./scripts/apk-download-server.mjs', 'utf8');
  const manifestSource = fs.readFileSync('./app/src/main/AndroidManifest.xml', 'utf8');
  const guardSource = fs.readFileSync('./core/src/main/java/com/example/security/QuickDeliverySecurityGuard.kt', 'utf8');

  // 9a. Storage permissions present in Manifest
  assert.match(manifestSource, /android\.permission\.READ_MEDIA_IMAGES/);
  assert.match(manifestSource, /android\.permission\.READ_EXTERNAL_STORAGE/);
  assert.match(manifestSource, /android\.permission\.WRITE_EXTERNAL_STORAGE/);

  // 9b. QuickDeliverySecurityGuard blocks blank phone fallback and validates real names/phones/self-order
  assert.equal(guardSource.includes('sanitizePhoneNumber(customerPhone).ifBlank { "9876543210" }'), false);
  assert.match(guardSource, /fun isPlausibleHumanName\(/);
  assert.match(guardSource, /fun isSameActorSelfOrder\(/);
  assert.match(guardSource, /\^\[6-9\]\[0-9\]\{9\}\$/);

  // 9c. Server enforces Indian ^[6-9]\d{9}$, blocks self-order, locks customer role transitions, and protects Admin reset
  assert.match(serverSource, /function isFakeOrInvalidIndianPhone10\(/);
  assert.match(serverSource, /Self-order fraud/);
  assert.match(serverSource, /Blocked CUSTOMER role status forgery/);
  assert.match(serverSource, /MAX_NEW_ORDERS_PER_MIN_PER_IP/);
  assert.match(serverSource, /Documents\/QuickDelivery/);
});

test('10. Strict Order-Only Chatbot & Post-Sign-Up App Guide with Skip: verifies strict order scope, bypass refusal, and Sign-Up walkthrough Skip wiring', () => {
  const botSource = fs.readFileSync(
    './core/src/main/java/com/example/ui/components/OrderChatbotAndOnboardingComponents.kt',
    'utf8'
  );
  const authSource = fs.readFileSync(
    './core/src/main/java/com/example/ui/components/SpeedIntroAuthAndMotivationComponents.kt',
    'utf8'
  );
  const customerSource = fs.readFileSync(
    './app/src/main/java/com/example/ui/screens/CustomerAppScreen.kt',
    'utf8'
  );
  const riderSource = fs.readFileSync(
    './core/src/main/java/com/example/ui/rider/RiderPartnerCompactScreen.kt',
    'utf8'
  );

  // 10a. OrderAssistantBotEngine enforces strict order-only scope and rejects off-topic / OTP bypass probes
  assert.match(botSource, /object OrderAssistantBotEngine/);
  assert.match(botSource, /fun isOrderRelatedQuery\(/);
  assert.match(botSource, /fun isSecurityBypassProbe\(/);
  assert.match(botSource, /EXPLICIT_OFF_TOPIC_KEYWORDS/);
  assert.match(botSource, /SECURITY_BYPASS_KEYWORDS/);
  assert.match(botSource, /only answer order-related questions/);

  // 10b. QuickDeliveryNewUserAppGuideScreen provides multi-step details + clear Skip buttons
  assert.match(botSource, /fun QuickDeliveryNewUserAppGuideScreen\(/);
  assert.match(botSource, /_app_guide_skip_top_btn/);
  assert.match(botSource, /_app_guide_skip_btn/);
  assert.match(botSource, /_app_guide_next_btn/);

  // 10c. Sign-Up marks pending guide while existing Sign-In dismisses it
  assert.match(authSource, /fun markNewSignUpAppGuidePending\(/);
  assert.match(authSource, /fun hasPendingNewSignUpAppGuide\(/);
  assert.match(authSource, /fun dismissNewSignUpAppGuide\(/);

  // 10d. Both CustomerAppScreen and RiderPartnerCompactScreen wire Order Chatbot & New Sign-Up Guide
  assert.match(customerSource, /QuickDeliveryOrderChatbotDialog/);
  assert.match(customerSource, /QuickDeliveryNewUserAppGuideScreen/);
  assert.match(customerSource, /customer_top_bar_order_bot_btn/);
  assert.match(riderSource, /QuickDeliveryOrderChatbotDialog/);
  assert.match(riderSource, /QuickDeliveryNewUserAppGuideScreen/);
  assert.match(riderSource, /rider_top_header_order_bot_btn/);
});

test('11. Live Server PoC Verification: blocks unverified GOOGLE_SIGNIN credential overwrite, non-admin RBAC escalation, Admin brute-force, blocked customer numbers, unpaid payment spoofs, and OTP bypasses', async () => {
  const { spawn } = await import('node:child_process');
  const crypto = await import('node:crypto');
  const path = await import('node:path');
  const testPort = 39876;
  const testSecret = 'QD_LIVE_SYNC_GUARD_2026_quickdelivery.co.in';
  const tempStateFile = path.join('/tmp', `qd-sec-poc-${Date.now()}.json`);

  const signHeaders = (deviceId, role) => {
    const ts = Date.now();
    const canonical = `${deviceId}|${role}|${ts}`;
    const sig = crypto.createHmac('sha256', testSecret).update(canonical).digest('hex');
    return {
      'Content-Type': 'application/json',
      'X-QD-Sync-Ts': String(ts),
      'X-QD-Sync-Sig': sig,
    };
  };

  const child = spawn('node', ['scripts/apk-download-server.mjs'], {
    env: {
      ...process.env,
      APP_PORT: String(testPort),
      SYNC_STATE_FILE: tempStateFile,
    },
    stdio: ['ignore', 'pipe', 'pipe'],
  });

  try {
    await new Promise((resolve, reject) => {
      const timeout = setTimeout(() => reject(new Error('Test server startup timed out')), 8000);
      child.stdout.on('data', (chunk) => {
        if (String(chunk).includes(`http://0.0.0.0:${testPort}`)) {
          clearTimeout(timeout);
          resolve();
        }
      });
      child.on('exit', (code) => {
        clearTimeout(timeout);
        reject(new Error(`Server exited early with code ${code}`));
      });
    });

    const baseUrl = `http://127.0.0.1:${testPort}`;

    // 11a. PoC: Unverified GOOGLE_SIGNIN credential overwrite MUST be rejected (HTTP 403)
    const saltHex = crypto.randomBytes(16).toString('hex');
    let hashBuf = Buffer.from(`QD_ADMIN_V1|${saltHex}|StrongPass99`, 'utf8');
    for (let i = 0; i < 10000; i++) {
      hashBuf = crypto.createHash('sha256').update(hashBuf).digest();
    }
    const forgedCredRes = await fetch(`${baseUrl}/api/admin/credentials/save`, {
      method: 'POST',
      headers: signHeaders('attacker-dev-1', 'ADMIN'),
      body: JSON.stringify({
        deviceId: 'attacker-dev-1',
        userId: 'attacker_admin',
        recoveryEmail: 'ravindergupta141@gmail.com',
        saltHex,
        hashHex: hashBuf.toString('hex'),
        authProof: 'GOOGLE_SIGNIN',
      }),
    });
    assert.equal(forgedCredRes.status, 403, 'Forged GOOGLE_SIGNIN without verified ID token must return 403');

    // Legitimate first-time setup with SETTINGS_INITIAL_SETUP initializes creds once, then second overwrite is rejected (403)
    const initSave = await fetch(`${baseUrl}/api/admin/credentials/save`, {
      method: 'POST',
      headers: signHeaders('admin-dev-1', 'ADMIN'),
      body: JSON.stringify({
        deviceId: 'admin-dev-1',
        userId: 'owner_admin',
        recoveryEmail: 'ravindergupta141@gmail.com',
        saltHex,
        hashHex: hashBuf.toString('hex'),
        authProof: 'SETTINGS_INITIAL_SETUP',
      }),
    });
    assert.equal(initSave.status, 200);

    const secondOverwrite = await fetch(`${baseUrl}/api/admin/credentials/save`, {
      method: 'POST',
      headers: signHeaders('attacker-dev-2', 'ADMIN'),
      body: JSON.stringify({
        deviceId: 'attacker-dev-2',
        userId: 'hijack_admin',
        recoveryEmail: 'ravindergupta141@gmail.com',
        saltHex,
        hashHex: hashBuf.toString('hex'),
        authProof: 'SETTINGS_INITIAL_SETUP',
      }),
    });
    assert.equal(secondOverwrite.status, 403, 'Repeated SETTINGS_INITIAL_SETUP must be rejected once configured');

    // 11b. PoC: Server-side Admin brute-force lockout after 5 wrong password attempts -> 6th returns HTTP 429
    for (let attempt = 1; attempt <= 5; attempt++) {
      const wrongLogin = await fetch(`${baseUrl}/api/admin/credentials/verify`, {
        method: 'POST',
        headers: signHeaders('brute-dev-1', 'ADMIN'),
        body: JSON.stringify({
          deviceId: 'brute-dev-1',
          identifier: 'owner_admin',
          password: `WrongPass${attempt}`,
        }),
      });
      assert.equal(wrongLogin.status, attempt === 5 ? 429 : 401);
    }
    const lockedLogin = await fetch(`${baseUrl}/api/admin/credentials/verify`, {
      method: 'POST',
      headers: signHeaders('brute-dev-1', 'ADMIN'),
      body: JSON.stringify({
        deviceId: 'brute-dev-1',
        identifier: 'owner_admin',
        password: 'StrongPass99',
      }),
    });
    assert.equal(lockedLogin.status, 429, 'Locked out Admin verify must reject even correct password during cooldown');

    // 11c. PoC: Non-admin (RIDER / CUSTOMER) RBAC escalation attempts in /api/live-sync are stripped/blocked
    const escalationRes = await fetch(`${baseUrl}/api/live-sync`, {
      method: 'POST',
      headers: signHeaders('rogue-rider-1', 'RIDER'),
      body: JSON.stringify({
        deviceId: 'rogue-rider-1',
        role: 'RIDER',
        riderRegistrations: [
          {
            riderKey: '9820112233',
            riderId: 'RIDER-ROGUE',
            name: 'Rogue Rider',
            phone: '9820112233',
            approvalStatus: 'APPROVED',
            updatedAtEpochMs: Date.now(),
          },
        ],
        withdrawalRequests: [
          {
            requestId: 'WD-ROGUE-1',
            riderId: 'RIDER-ROGUE',
            amountInr: 9500,
            payoutDestination: 'rogue@upi',
            status: 'PAID',
            updatedAtEpochMs: Date.now(),
          },
        ],
        codSettlements: [
          {
            settlementId: 'SET-ROGUE-1',
            riderId: 'RIDER-ROGUE',
            amountInr: 200,
            status: 'VERIFIED',
            updatedAtEpochMs: Date.now(),
          },
        ],
      }),
    });
    assert.equal(escalationRes.status, 200);
    const escPayload = await escalationRes.json();
    assert.equal(
      escPayload.riderRegistrations.some((r) => r.riderKey === '9820112233' && r.approvalStatus === 'APPROVED'),
      false,
      'Non-admin RIDER must NOT self-approve KYC'
    );
    assert.equal(
      escPayload.withdrawalRequests.some((w) => w.requestId === 'WD-ROGUE-1' && w.status === 'PAID'),
      false,
      'Non-admin RIDER must NOT self-mark withdrawal PAID'
    );
    assert.equal(
      escPayload.codSettlements.some((s) => s.settlementId === 'SET-ROGUE-1' && s.status === 'VERIFIED'),
      false,
      'Non-admin RIDER must NOT self-verify COD settlement'
    );

    // 11d. PoC: Admin adds blocked customer phone + enables Pickup/Drop OTP -> server blocks order & OTP bypass & unpaid payment spoof
    const adminFlagsRes = await fetch(`${baseUrl}/api/live-sync`, {
      method: 'POST',
      headers: signHeaders('admin-dev-1', 'ADMIN'),
      body: JSON.stringify({
        deviceId: 'admin-dev-1',
        role: 'ADMIN',
        remoteFeatureFlags: {
          flagsSchemaVersion: 7,
          pickupDropOtpEnabled: true,
          blockedCustomerPhonesCsv: '9811122233',
          updatedAtEpochMs: Date.now(),
        },
        orders: [
          {
            orderId: 'QD-SEC-101',
            customerName: 'Aarav Patel',
            customerPhone: '9822233344',
            pickupFullAddress: 'MG Road Metro Station Gate 2',
            dropFullAddress: 'Indiranagar 100ft Road Block 3',
            vehicleType: 'BIKE',
            orderStatus: 'ARRIVED_AT_PICKUP',
            statusRank: 3,
            totalAmount: '₹100',
            platformCommission: '₹20',
            riderEarning: '₹80',
            pickupOtp: '4321',
            deliveryOtp: '8765',
          },
        ],
      }),
    });
    assert.equal(adminFlagsRes.status, 200);

    // Blocked phone order + unpaid payment spoof + wrong OTP bypass
    const fraudRes = await fetch(`${baseUrl}/api/live-sync`, {
      method: 'POST',
      headers: signHeaders('cust-dev-9', 'CUSTOMER'),
      body: JSON.stringify({
        deviceId: 'cust-dev-9',
        role: 'CUSTOMER',
        orders: [
          {
            orderId: 'QD-BLK-999',
            customerName: 'Blocked Fraud',
            customerPhone: '9811122233',
            pickupFullAddress: 'Koramangala 4th Block',
            dropFullAddress: 'Whitefield Main Road',
            vehicleType: 'BIKE',
            totalAmount: '₹120',
            platformCommission: '₹24',
            riderEarning: '₹96',
          },
          {
            orderId: 'QD-SPF-888',
            customerName: 'Rohan Sharma',
            customerPhone: '9833344455',
            pickupFullAddress: 'HSR Layout Sector 2',
            dropFullAddress: 'BTM Layout Stage 1',
            vehicleType: 'BIKE',
            paymentMethod: 'UPI_INSTANT_PAY',
            paymentStatus: 'VERIFIED_PAID_TO_COMPANY',
            isPaidToCompany: true,
            gatewayPaymentId: '',
            totalAmount: '₹100',
            platformCommission: '₹20',
            riderEarning: '₹80',
          },
        ],
      }),
    });
    assert.equal(fraudRes.status, 200);
    const fraudBody = await fraudRes.json();
    assert.equal(
      fraudBody.orders.some((o) => o.orderId === 'QD-BLK-999'),
      false,
      'Order from Admin-blocked phone MUST be rejected by server'
    );
    assert.ok(
      Array.isArray(fraudBody.rejectedOrders) &&
        fraudBody.rejectedOrders.some((r) => r.orderId === 'QD-BLK-999' && typeof r.reason === 'string' && r.reason.length > 0),
      'Server must return explicit rejection reason so Customer UI knows why order was blocked'
    );
    const spoofedOrder = fraudBody.orders.find((o) => o.orderId === 'QD-SPF-888');
    assert.ok(spoofedOrder, 'Valid customer order created without forged payment flag');
    assert.equal(spoofedOrder.isPaidToCompany, false, 'Unpaid payment spoof without gatewayPaymentId MUST be downgraded');

    // Wrong Pickup OTP transition must be blocked
    const badOtpRes = await fetch(`${baseUrl}/api/live-sync`, {
      method: 'POST',
      headers: signHeaders('rider-dev-5', 'RIDER'),
      body: JSON.stringify({
        deviceId: 'rider-dev-5',
        role: 'RIDER',
        orders: [
          {
            orderId: 'QD-SEC-101',
            customerName: 'Aarav Patel',
            customerPhone: '9822233344',
            pickupFullAddress: 'MG Road Metro Station Gate 2',
            dropFullAddress: 'Indiranagar 100ft Road Block 3',
            vehicleType: 'BIKE',
            orderStatus: 'PICKED_UP',
            statusRank: 4,
            totalAmount: '₹100',
            platformCommission: '₹20',
            riderEarning: '₹80',
            pickupProof: { verifiedOtp: '0000' },
          },
        ],
      }),
    });
    const badOtpBody = await badOtpRes.json();
    const targetOrd = badOtpBody.orders.find((o) => o.orderId === 'QD-SEC-101');
    assert.equal(targetOrd.orderStatus, 'ARRIVED_AT_PICKUP', 'Wrong Pickup OTP must NOT advance status to PICKED_UP');
    assert.ok(badOtpBody.securityStats.blockedFakeOrders >= 1);
    assert.ok(Array.isArray(badOtpBody.securityStats.recentBlockedEvents));
    assert.ok(badOtpBody.securityStats.recentBlockedEvents.length >= 1);
    // Verify live HTTP endpoints for /brand-logo.jpg, /about, and /terms parcel damage policy
    const logoRes = await fetch(`${baseUrl}/brand-logo.jpg`);
    assert.equal(logoRes.status, 200, '/brand-logo.jpg must serve official logo');
    const logoBytes = await logoRes.arrayBuffer();
    assert.ok(logoBytes.byteLength > 1000, 'Brand logo bytes must be non-empty');

    const aboutRes = await fetch(`${baseUrl}/about`);
    assert.equal(aboutRes.status, 200, '/about must render official app info page');
    const aboutHtml = await aboutRes.text();
    assert.match(aboutHtml, /About Quick Delivery/);
    assert.match(aboutHtml, /brand-logo\.jpg/);

    const termsRes = await fetch(`${baseUrl}/terms`);
    assert.equal(termsRes.status, 200);
    const termsHtml = await termsRes.text();
    assert.match(termsHtml, /Parcel Tampering, Theft, Loss/);
  } finally {
    child.kill('SIGTERM');
    try {
      fs.unlinkSync(tempStateFile);
    } catch {}
  }
});

test('12. Official Branded Email, Invoice Logo, Parcel Damage Policy & Full Admin Radar Orders Verification', () => {
  const serverSource = fs.readFileSync('./scripts/apk-download-server.mjs', 'utf8');
  const pdfSource = fs.readFileSync('./core/src/main/java/com/example/security/QuickDeliveryPdfGenerator.kt', 'utf8');
  const radarSource = fs.readFileSync('./core/src/main/java/com/example/ui/components/AdminLiveVectorCityMapCanvas.kt', 'utf8');
  const adminSource = fs.readFileSync('./core/src/main/java/com/example/ui/components/AdminEnterpriseDashboardSuite.kt', 'utf8');
  const customerSource = fs.readFileSync('./app/src/main/java/com/example/ui/screens/CustomerAppScreen.kt', 'utf8');

  // Branded email wrapper applied globally in sendQuickDeliveryEmail
  assert.match(serverSource, /function buildOfficialQuickDeliveryEmail\(/);
  assert.match(serverSource, /brand-logo\.jpg/);
  assert.match(serverSource, /App Info &amp; How It Works/);
  assert.match(serverSource, /Terms &amp; Parcel Damage Policy/);

  // Brand logo in PDF Receipts + Customer Invoice Dialog
  assert.match(pdfSource, /loadCircularBrandLogoBitmap/);
  assert.match(pdfSource, /cute_scooter_bunny_logo_1794655841710/);
  assert.match(customerSource, /QuickDeliveryCuteBunnyLogo\(/);

  // Admin Radar renders all live orders + Admin Overview shows all filtered orders directly below
  assert.equal(radarSource.includes('activeOrders.take(4)'), false, 'Radar must not hard-cap active orders at 4');
  assert.match(radarSource, /activeOrders\.take\(12\)/);
  assert.equal(adminSource.includes('orders = filteredRecentOrders.take(5)'), false, 'Admin dashboard must show all filtered orders below Radar');
});

test('13. Post-Delivery Rider Phone Masking, Admin Chat Audit, Strong Policies & Stage 3 Upgrades', async () => {
  const { spawn } = await import('node:child_process');
  const crypto = await import('node:crypto');
  const path = await import('node:path');
  const os = await import('node:os');

  const serverSource = fs.readFileSync('./scripts/apk-download-server.mjs', 'utf8');
  const guardSource = fs.readFileSync('./core/src/main/java/com/example/security/QuickDeliverySecurityGuard.kt', 'utf8');
  const sheetsSource = fs.readFileSync('./core/src/main/java/com/example/ui/components/AdminExecutiveThemeAndSheets.kt', 'utf8');
  const riderSource = fs.readFileSync('./core/src/main/java/com/example/ui/rider/RiderPartnerCompactScreen.kt', 'utf8');
  const customerSource = fs.readFileSync('./app/src/main/java/com/example/ui/screens/CustomerAppScreen.kt', 'utf8');
  const customerEnhSource = fs.readFileSync('./app/src/main/java/com/example/ui/screens/CustomerEnhancementCards.kt', 'utf8');
  const policySource = fs.readFileSync('./core/src/main/java/com/example/ui/components/SpeedIntroAuthAndMotivationComponents.kt', 'utf8');

  // Client & Server security masking + Stage 3 upgrades
  assert.match(guardSource, /fun maskCustomerPhoneForRider\(/);
  assert.match(riderSource, /rider_post_delivery_masked_phone_/);
  assert.match(riderSource, /rider_report_damaged_parcel_btn_/);
  assert.match(sheetsSource, /OrderLiveChatManager\.messagesByOrder/);
  assert.match(sheetsSource, /admin_order_chat_transcript_/);
  assert.match(sheetsSource, /admin_order_theft_recovery_btn_/);
  assert.match(sheetsSource, /admin_order_illegal_customer_hold_btn_/);
  assert.match(customerSource, /customer_book_again_btn_/);
  assert.match(customerEnhSource, /Parcel Damage Protection Cover/);
  assert.match(policySource, /100% of the shipment value \+ loss compensation/);
  assert.match(policySource, /100% legal, criminal, and financial responsibility rests solely on that Customer/);

  // Live Server check: RIDER sees unmasked customerPhone while IN_TRANSIT, masked ******XXXX once DELIVERED
  const testPort = 43990 + Math.floor(Math.random() * 400);
  const tempStateFile = path.join(os.tmpdir(), `qd-sec13-state-${Date.now()}.json`);
  fs.writeFileSync(
    tempStateFile,
    JSON.stringify({
      ordersById: {
        'QD-MASK-LIVE': {
          orderId: 'QD-MASK-LIVE',
          orderStatus: 'IN_TRANSIT',
          customerName: 'Kiran Rao',
          customerPhone: '9845099887',
          receiverPhone: '9845011223',
          updatedAtEpochMs: Date.now(),
        },
        'QD-MASK-DONE': {
          orderId: 'QD-MASK-DONE',
          orderStatus: 'DELIVERED',
          customerName: 'Neha Verma',
          customerPhone: '9845099887',
          receiverPhone: '9845011223',
          updatedAtEpochMs: Date.now() - 1000,
        },
      },
      devicesById: {},
      securityStats: {},
    }),
    'utf8'
  );

  const child = spawn('node', ['scripts/apk-download-server.mjs', '--serve'], {
    cwd: process.cwd(),
    env: {
      ...process.env,
      APP_PORT: String(testPort),
      SYNC_STATE_FILE: tempStateFile,
      PUBLIC_BRAND_ORIGIN: 'https://quickdelivery.co.in',
    },
    stdio: ['ignore', 'pipe', 'pipe'],
  });

  const signHeaders = (deviceId, role) => {
    const ts = Date.now();
    const sig = crypto
      .createHmac('sha256', 'QD_LIVE_SYNC_GUARD_2026_quickdelivery.co.in')
      .update(`${deviceId}|${role}|${ts}`)
      .digest('hex');
    return {
      'Content-Type': 'application/json',
      'X-QD-App-Role': role,
      'X-QD-Sync-Ts': String(ts),
      'X-QD-Sync-Sig': sig,
    };
  };

  try {
    await new Promise((resolve, reject) => {
      const timeout = setTimeout(() => reject(new Error('Test 13 server startup timed out')), 8000);
      child.stdout.on('data', (chunk) => {
        if (String(chunk).includes(`http://0.0.0.0:${testPort}`)) {
          clearTimeout(timeout);
          resolve();
        }
      });
      child.on('exit', (code) => {
        clearTimeout(timeout);
        reject(new Error(`Server exited early with code ${code}`));
      });
    });

    const baseUrl = `http://127.0.0.1:${testPort}`;

    const riderSyncRes = await fetch(`${baseUrl}/api/live-sync`, {
      method: 'POST',
      headers: signHeaders('rider-phone-mask-1', 'RIDER'),
      body: JSON.stringify({ deviceId: 'rider-phone-mask-1', role: 'RIDER' }),
    });
    assert.equal(riderSyncRes.status, 200);
    const riderData = await riderSyncRes.json();
    const liveOrd = riderData.orders.find((o) => o.orderId === 'QD-MASK-LIVE');
    const doneOrd = riderData.orders.find((o) => o.orderId === 'QD-MASK-DONE');
    assert.equal(liveOrd.customerPhone, '9845099887', 'Live order must keep full phone for Rider');
    assert.equal(doneOrd.customerPhone, '******9887', 'Delivered order must mask Customer phone for Rider');
    assert.equal(doneOrd.receiverPhone, '******1223', 'Delivered order must mask Receiver phone for Rider');

    const adminSyncRes = await fetch(`${baseUrl}/api/live-sync`, {
      method: 'POST',
      headers: signHeaders('admin-phone-audit-1', 'ADMIN'),
      body: JSON.stringify({ deviceId: 'admin-phone-audit-1', role: 'ADMIN' }),
    });
    const adminData = await adminSyncRes.json();
    const adminDoneOrd = adminData.orders.find((o) => o.orderId === 'QD-MASK-DONE');
    assert.equal(adminDoneOrd.customerPhone, '9845099887', 'Admin audit keeps full number');
    assert.ok(adminData.serverAppUpdate, '/api/live-sync must expose serverAppUpdate manifest');
    assert.equal(adminData.serverAppUpdate.latestVersionName, '6.7.2');
    assert.equal(adminData.serverAppUpdate.latestVersionCode, 85);
    assert.equal(adminData.serverAppUpdate.forceUpdateRequired, true);

    const manifestRes = await fetch(`${baseUrl}/api/app-update-manifest`);
    assert.equal(manifestRes.status, 200);
    const manifestData = await manifestRes.json();
    assert.equal(manifestData.ok, true);
    assert.equal(manifestData.latestVersionName, '6.7.2');
    assert.equal(manifestData.latestVersionCode, 85);
    assert.equal(manifestData.forceUpdateRequired, true);
    assert.match(manifestData.urls.CUSTOMER, /\/customer-apk$/);
    assert.match(manifestData.urls.RIDER, /\/rider-apk$/);
    assert.match(manifestData.urls.ADMIN, /\/admin-apk$/);

    assert.match(serverSource, /100% financial recovery of the shipment amount/);
  } finally {
    child.kill('SIGTERM');
    try {
      fs.unlinkSync(tempStateFile);
    } catch {}
  }
});
