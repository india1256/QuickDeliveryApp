# Security Summary & Walkthrough

## Architecture & Protections Audited
The Quick Delivery platform spans the Node.js synchronization/API backend (`scripts/apk-download-server.mjs`), Firestore security rules (`firestore.rules`), shared security/auth managers (`core/src/main/java/com/example/security/`), and the standalone Admin/Customer/Rider Android modules.

### Remediated Vulnerabilities
1. **Admin Credential Recovery Verification**:
   - **Issue**: Previously, `POST /api/admin/credentials/save` accepted `authProof: 'GOOGLE_SIGNIN'` without validating a cryptographic Google OIDC ID Token or recent Email OTP.
   - **Fix**: Verified Google OIDC `id_token` server-side via Google's `tokeninfo` endpoint (confirming `email_verified`, unexpired `exp`, exact email match, and allowlist membership) or an active verified Email OTP; updated `GoogleSignInGate.kt` and `AdminMainActivity.kt` to transmit the signed `googleIdToken`.
2. **Cross-Role Privilege Escalation in Live Sync**:
   - **Issue**: `POST /api/live-sync` restricted `remoteFeatureFlags` and `promoCoupons` to `ADMIN`/`UNIFIED`, but allowed non-admin `CUSTOMER`/`RIDER` senders to submit pre-approved statuses on `riderRegistrations`, `withdrawalRequests`, `codSettlements`, `disputeRecords`, and `b2bAccounts`.
   - **Fix**: Added strict server-side role-based authorization checks blocking non-admin status transitions to `APPROVED`, `PAID`, `VERIFIED`, `REFUNDED`, or `REJECTED`, preserving existing administrative decisions, and recording blocked attempts in `securityStats.recentBlockedEvents`.
3. **Admin Brute-Force Lockout & Strong Password Policy**:
   - **Issue**: Server password verification relied only on broad IP rate limiting (30 req/min), and client UI allowed 6-character weak passwords (`123456`).
   - **Fix**: Added per-IP/device failure tracking on `/api/admin/credentials/verify` locking out for 60 seconds after 5 wrong tries, strengthened `AdminCredentialsManager.validatePassword` to require 8+ alphanumeric characters excluding common passwords, and added background 3-minute PIN auto-relock in `AdminMainActivity`.
4. **Server Enforcement of Fraud Blocklist, Payment Integrity, & Pickup/Drop OTPs**:
   - **Issue**: Admin's `blockedCustomerPhonesCsv` fraud blocklist was only enforced client-side, and `POST /api/live-sync` accepted prepaid customer status flags without gateway transaction IDs or strict server-side OTP checks when `pickupDropOtpEnabled` was active.
   - **Fix**: Enforced `blockedCustomerPhonesCsv` filtering on both customer and receiver phone numbers inside `validateIncomingOrderForAntiSpam`, downgraded unpaid `CUSTOMER` `VERIFIED_PAID_TO_COMPANY` spoofs missing `gatewayPaymentId`, enforced server-side Pickup/Drop OTP verification when `pickupDropOtpEnabled` is true, and updated `/api/payments/razorpay-webhook` to mark `isPaidToCompany = true` alongside verified gateway IDs.
5. **Admin Console Security Firewall Upgrade**:
   - Added a live Security Audit Trail inside Admin Settings (`AdminEnterpriseDashboardSuite.kt`) surfacing server-blocked fake orders, rate-limited requests, rejected tamper attempts, and 1-tap fraud phone blocking.

## PoC Verification
- Live isolated HTTP security exploit verification in `security.test.js` Test 11:
  - Confirmed forged `GOOGLE_SIGNIN` credential overwrite without verified Google ID token returns `HTTP 403`.
  - Confirmed repeated `SETTINGS_INITIAL_SETUP` overwrite after initialization returns `HTTP 403`.
  - Confirmed 5 failed Admin login attempts trigger server `HTTP 429` lockout rejecting even valid credentials during cooldown.
  - Confirmed `RIDER`-role `/api/live-sync` submissions attempting `approvalStatus: 'APPROVED'`, withdrawal `status: 'PAID'`, and COD settlement `status: 'VERIFIED'` are blocked from elevating status.
  - Confirmed Admin-blocked phone numbers are rejected server-side (`QD-BLK-999` absent), unpaid `CUSTOMER` payment spoofs (`QD-SPF-888`) are downgraded to `isPaidToCompany = false`, and invalid Pickup OTP attempts (`0000` != `4321`) fail to advance status to `PICKED_UP`.
- Verified pure JVM password policy unit test (`AdminSecurityPolicyTest.kt`) and Node test suite (`node --test security.test.js` — 11/11 passed).
