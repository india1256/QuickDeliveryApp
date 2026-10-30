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
