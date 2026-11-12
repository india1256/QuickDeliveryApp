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


