# Quick Delivery — Phase 0: Security Specification & Dirty Dozen Payloads

## 1. Data Invariants
1. **Authenticated Ownership (`userId == request.auth.uid`)**: No unauthenticated caller may read, list, create, update, or delete any document in `delivery_orders`, `rider_wallets`, `company_ledger`, or `user_profiles`.
2. **Order Terminal State & Role Transition Locking**:
   - Non-admin callers creating an order must set `status == 'PENDING_ASSIGNMENT'` with no `assignedRiderUid`.
   - Customers updating their own order may only keep the current status or transition to `'CANCELLED'` before pickup (`existing().status in ['PENDING_ASSIGNMENT', 'ASSIGNED', 'ARRIVED_AT_PICKUP']`). Customers cannot self-mark an order `'DELIVERED'` or `'IN_TRANSIT'`.
   - Once a `delivery_orders` document reaches `'DELIVERED'` or `'CANCELLED'`, non-admin callers cannot mutate its status or fields.
3. **80/20 Fare Split Arithmetic Invariant**: Every `delivery_orders` document must satisfy `totalAmountPaise == companyCommissionPaise + riderEarningPaise`.
4. **Rider Commission Debt Cap (`15000` paise / ₹150)**: Every `rider_wallets` document must maintain `maxCommissionDebtPaise == 15000` and `commissionDebtPaise >= 0` with `balancePaise >= -15000`.
5. **Append-Only Company Ledger**: `company_ledger` entries cannot be deleted (`allow delete: if false`) and non-admin users cannot mutate existing ledger entries.
6. **Temporal Integrity**: All `createdAt` and `updatedAt` fields must be server timestamps (`is timestamp && <= request.time`), and `createdAt` is immutable on update.
7. **Zero Blanket List Reads**: Every `allow list` rule enforces `resource.data.userId == request.auth.uid || isAdmin()` (plus `assignedRiderUid == request.auth.uid` for orders).
8. **Input Sanitization & Anti-Spam Rate Limiting**:
   - Firestore rules (`isSafeString`) and client/repository guards (`QuickDeliverySecurityGuard` / `securityGuard.ts`) strip/reject `<script>`, `javascript:`, `onerror=`, `onload=`, `$where`, `$ne`, and `__proto__` payloads.
   - Sliding-window rate limiting caps order placement at 5 orders/minute, 20 orders/hour, max 5 concurrent unassigned orders, and blocks duplicate route bursts within an 8-second cooldown.
9. **Role-Bound Authentication Token Verification**:
   - Session tokens are HMAC-SHA256 signed over `(role, userId/phone, email, iat, exp)` and verified on session restore, local backup import, and role checks to block cross-role privilege escalation (`CUSTOMER` -> `ADMIN`).

## 2. The Dirty Dozen Payloads (Adversarial Test Vectors)
1. **Unauthenticated Order Read**: `unauthDb.collection("delivery_orders").get()` -> Reject (`PERMISSION_DENIED`).
2. **Cross-User Order Read (PII Blanket Test)**: Bob (`bob_456`) attempts `get()` on Alice's (`alice_123`) order -> Reject.
3. **Unfiltered List Query (Query Trust Test)**: Alice queries `collection("delivery_orders").get()` without `.where("userId", "==", "alice_123")` -> Reject.
4. **Identity Spoofing on Order Create**: Alice creates an order with `userId: "bob_456"` -> Reject.
5. **Shadow Update / Ghost Field Injection**: Alice updates her order with `{ isVerifiedAdmin: true }` -> Reject via `affectedKeys().hasOnly(...)`.
6. **Value Poisoning & Customer Status Tampering**: Alice attempts to create or update her own order with `status: "DELIVERED"` or tampered `totalAmountPaise != companyCommissionPaise + riderEarningPaise` -> Reject.
7. **Terminal State Bypass**: Alice attempts to update an order whose existing `status` is `"DELIVERED"` -> Reject.
8. **Immortal Field Tampering**: Alice attempts to change `createdAt` or `orderCode` during an order update -> Reject.
9. **Future Timestamp Spoofing**: Client submits a timestamp in the future (`> request.time`) -> Reject.
10. **Email Spoofing Attack**: Caller has `email == "ravindergupta141@gmail.com"` but `email_verified == false` -> Reject admin privileges.
11. **Rider Wallet Cross-User Mutation**: Bob attempts to modify Alice's `rider_wallets/alice_123` -> Reject.
12. **Company Ledger Deletion / Tampering & User Profile Role Escalation**: Authenticated user attempts to `delete()`/`update()` a `company_ledger` entry or update `/user_profiles/{uid}` with `role: "ADMIN"` -> Reject.
