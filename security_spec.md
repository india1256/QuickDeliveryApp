# Quick Delivery — Phase 0: Security Specification & Dirty Dozen Payloads

## 1. Data Invariants
1. **Authenticated Ownership (`userId == request.auth.uid`)**: No unauthenticated caller may read, list, create, update, or delete any document in `delivery_orders`, `rider_wallets`, or `company_ledger`.
2. **Order Terminal State Locking**: Once a `delivery_orders` document reaches `DELIVERED` or `CANCELLED`, non-admin callers cannot mutate its status or fields.
3. **Rider Commission Debt Cap (`15000` paise / ₹150)**: Every `rider_wallets` document must maintain `maxCommissionDebtPaise == 15000` and `commissionDebtPaise >= 0` with `balancePaise >= -15000`.
4. **Append-Only Company Ledger**: `company_ledger` entries cannot be deleted (`allow delete: if false`) and non-admin users cannot mutate existing ledger entries.
5. **Temporal Integrity**: All `createdAt` and `updatedAt` fields must be server timestamps (`is timestamp && <= request.time`), and `createdAt` is immutable on update.
6. **Zero Blanket List Reads**: Every `allow list` rule enforces `resource.data.userId == request.auth.uid || isAdmin()` (plus `assignedRiderUid == request.auth.uid` for orders).

## 2. The Dirty Dozen Payloads (Adversarial Test Vectors)
1. **Unauthenticated Order Read**: `unauthDb.collection("delivery_orders").get()` -> Reject (`PERMISSION_DENIED`).
2. **Cross-User Order Read (PII Blanket Test)**: Bob (`bob_456`) attempts `get()` on Alice's (`alice_123`) order -> Reject.
3. **Unfiltered List Query (Query Trust Test)**: Alice queries `collection("delivery_orders").get()` without `.where("userId", "==", "alice_123")` -> Reject.
4. **Identity Spoofing on Order Create**: Alice creates an order with `userId: "bob_456"` -> Reject.
5. **Shadow Update / Ghost Field Injection**: Alice updates her order with `{ isVerifiedAdmin: true }` -> Reject via `affectedKeys().hasOnly(...)`.
6. **Value Poisoning on Update**: Alice updates `status` with an integer `999` or invalid enum `"HACKED"` -> Reject via `isValidDeliveryOrder(incoming())`.
7. **Terminal State Bypass**: Alice attempts to update an order whose existing `status` is `"DELIVERED"` -> Reject.
8. **Immortal Field Tampering**: Alice attempts to change `createdAt` or `orderCode` during an order update -> Reject.
9. **Future Timestamp Spoofing**: Client submits a timestamp in the future (`> request.time`) -> Reject.
10. **Email Spoofing Attack**: Caller has `email == "ravindergupta141@gmail.com"` but `email_verified == false` -> Reject admin privileges.
11. **Rider Wallet Cross-User Mutation**: Bob attempts to modify Alice's `rider_wallets/alice_123` -> Reject.
12. **Company Ledger Deletion / Tampering**: Authenticated user attempts to `delete()` or `update()` a `company_ledger` entry -> Reject.
