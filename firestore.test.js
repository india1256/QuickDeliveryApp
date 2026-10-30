import { createRequire } from "node:module";
const require = createRequire(import.meta.url);
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const RIDER_UID = "rider_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validOrderPayload(userId = ALICE_UID, overrides = {}) {
  const now = new Date();
  return {
    userId,
    orderCode: "QD-5001",
    customerName: "Aarav Sharma",
    customerPhone: "9876543210",
    pickupAddress: "Indiranagar 100ft Road, Bengaluru",
    dropAddress: "Koramangala 4th Block, Bengaluru",
    vehicleType: "BIKE",
    paymentMethod: "GATEWAY_UPI",
    paymentStatus: "PENDING_GATEWAY_VERIFICATION",
    status: "PENDING_ASSIGNMENT",
    totalAmountPaise: 12000,
    companyCommissionPaise: 2400,
    riderEarningPaise: 9600,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validRiderWalletPayload(userId = RIDER_UID, overrides = {}) {
  const now = new Date();
  return {
    userId,
    riderId: "RIDER-101",
    riderName: "Vikram Singh",
    balancePaise: 148000,
    commissionDebtPaise: 9000,
    maxCommissionDebtPaise: 15000,
    workMode: "FULL_TIME",
    shiftStatus: "ONLINE_ACTIVE",
    batteryPercent: 85,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validLedgerPayload(userId = ALICE_UID, overrides = {}) {
  const now = new Date();
  return {
    userId,
    entryType: "CUSTOMER_GATEWAY_COLLECTION",
    referenceId: "QD-5001",
    amountPaise: 12000,
    isCredit: true,
    settlementStatus: "PENDING_IN_GATEWAY",
    description: "Gateway UPI payment pending server HMAC verification",
    createdAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("1. Unauthenticated caller: cannot read or create delivery_orders", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("delivery_orders").doc("order_1").get());
  await assertFails(
    unauthDb.collection("delivery_orders").doc("order_1").set(validOrderPayload())
  );
});

test("2. Authenticated customer: can create PENDING_GATEWAY_VERIFICATION order, but CANNOT spoof VERIFIED_PAID_TO_COMPANY", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("delivery_orders").doc("order_alice_1").set(validOrderPayload(ALICE_UID))
  );
  // Client cannot self-create an order already marked VERIFIED_PAID_TO_COMPANY
  await assertFails(
    aliceDb.collection("delivery_orders").doc("order_alice_spoof").set(
      validOrderPayload(ALICE_UID, { paymentStatus: "VERIFIED_PAID_TO_COMPANY" })
    )
  );
  // Client cannot self-update an existing order to VERIFIED_PAID_TO_COMPANY
  await assertFails(
    aliceDb.collection("delivery_orders").doc("order_alice_1").update({
      paymentStatus: "VERIFIED_PAID_TO_COMPANY",
      updatedAt: new Date(),
    })
  );
});

test("3. PII Blanket & Query Trust Test: Bob cannot read Alice's order or run unfiltered list query", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("delivery_orders")
      .doc("order_alice_1")
      .set(validOrderPayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("delivery_orders").doc("order_alice_1").get());
  await assertFails(bobDb.collection("delivery_orders").get());
});

test("4. Cross-Device Rider Claim & Proof/GPS Sync: Online rider under ₹150 debt cap can discover, claim, and update order", async () => {
  const createdTs = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("delivery_orders")
      .doc("order_cross_phone")
      .set(validOrderPayload(ALICE_UID, { createdAt: createdTs, updatedAt: createdTs }));
  });

  const riderDb = testEnv.authenticatedContext(RIDER_UID).firestore();
  await assertSucceeds(
    riderDb.collection("rider_wallets").doc(RIDER_UID).set(validRiderWalletPayload(RIDER_UID))
  );

  // Rider can list PENDING_ASSIGNMENT orders
  await assertSucceeds(
    riderDb.collection("delivery_orders").where("status", "==", "PENDING_ASSIGNMENT").get()
  );

  // Rider can claim the order
  await assertSucceeds(
    riderDb.collection("delivery_orders").doc("order_cross_phone").update({
      assignedRiderUid: RIDER_UID,
      assignedRiderId: "RIDER-101",
      status: "ASSIGNED",
      updatedAt: new Date(),
    })
  );

  // Assigned rider can persist Camera Proof and Live GPS coordinates
  await assertSucceeds(
    riderDb.collection("delivery_orders").doc("order_cross_phone").update({
      status: "IN_TRANSIT",
      pickupProofNote: "Parcel Photo Proof Verified (640x480px)",
      riderLat: 12.9716,
      riderLng: 77.5946,
      updatedAt: new Date(),
    })
  );
});

test("5. Rider Wallet: enforces ₹150 (15000 paise) commissionDebtPaise cap directly in Firestore rules", async () => {
  const riderDb = testEnv.authenticatedContext(RIDER_UID).firestore();
  await assertSucceeds(
    riderDb.collection("rider_wallets").doc(RIDER_UID).set(validRiderWalletPayload(RIDER_UID))
  );
  // Reject commissionDebtPaise > 15000 (₹150.00) on direct client update
  await assertFails(
    riderDb.collection("rider_wallets").doc(RIDER_UID).update({
      commissionDebtPaise: 15500,
      updatedAt: new Date(),
    })
  );
  // Reject invalid maxCommissionDebtPaise != 15000
  await assertFails(
    riderDb
      .collection("rider_wallets")
      .doc(RIDER_UID)
      .set(validRiderWalletPayload(RIDER_UID, { maxCommissionDebtPaise: 99999 }))
  );
});

test("6. Company Ledger: append-only creation succeeds for pending/receivable, rejects client-forged settled status", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("company_ledger").doc("ledger_1").set(validLedgerPayload(ALICE_UID))
  );
  // Client cannot forge SETTLED_TO_BANK or VERIFIED_LEDGER_FINAL directly
  await assertFails(
    aliceDb.collection("company_ledger").doc("ledger_forged").set(
      validLedgerPayload(ALICE_UID, { settlementStatus: "SETTLED_TO_BANK" })
    )
  );
  await assertFails(aliceDb.collection("company_ledger").doc("ledger_1").delete());
  await assertFails(
    aliceDb.collection("company_ledger").doc("ledger_1").update({
      settlementStatus: "SETTLED_TO_BANK",
    })
  );
});

test("7. Email Spoofing Test: unverified admin email cannot bypass owner isolation", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("delivery_orders")
      .doc("order_alice_2")
      .set(validOrderPayload(ALICE_UID));
  });

  const spoofedAdminDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: "ravindergupta141@gmail.com",
      email_verified: false,
    })
    .firestore();
  await assertFails(
    spoofedAdminDb.collection("delivery_orders").doc("order_alice_2").get()
  );

  const verifiedAdminDb = testEnv
    .authenticatedContext("admin_uid", {
      email: "ravindergupta141@gmail.com",
      email_verified: true,
    })
    .firestore();
  await assertSucceeds(
    verifiedAdminDb.collection("delivery_orders").doc("order_alice_2").get()
  );
});

test("8. Real-Time Order Chat: Customer and Assigned Rider can exchange chat messages, unauthorized users and role spoofing are rejected", async () => {
  const createdTs = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("delivery_orders")
      .doc("order_chat_1")
      .set(
        validOrderPayload(ALICE_UID, {
          assignedRiderUid: RIDER_UID,
          assignedRiderId: "RIDER-101",
          status: "IN_TRANSIT",
          createdAt: createdTs,
          updatedAt: createdTs,
        })
      );
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const riderDb = testEnv.authenticatedContext(RIDER_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  // Customer (Alice) can send a CUSTOMER chat message
  await assertSucceeds(
    aliceDb
      .collection("delivery_orders")
      .doc("order_chat_1")
      .collection("chat_messages")
      .doc("msg_1")
      .set({
        orderId: "order_chat_1",
        senderUid: ALICE_UID,
        senderRole: "CUSTOMER",
        senderName: "Aarav Sharma",
        messageText: "Please call when you reach the main gate.",
        deliveryStage: "IN_TRANSIT",
        createdAt: new Date(),
      })
  );

  // Assigned Rider can read/list chat messages and reply with a RIDER chat message
  await assertSucceeds(
    riderDb
      .collection("delivery_orders")
      .doc("order_chat_1")
      .collection("chat_messages")
      .get()
  );
  await assertSucceeds(
    riderDb
      .collection("delivery_orders")
      .doc("order_chat_1")
      .collection("chat_messages")
      .doc("msg_2")
      .set({
        orderId: "order_chat_1",
        senderUid: RIDER_UID,
        senderRole: "RIDER",
        senderName: "Vikram Singh",
        messageText: "Sure sir, reaching in 3 minutes!",
        deliveryStage: "IN_TRANSIT",
        createdAt: new Date(),
      })
  );

  // Customer cannot spoof RIDER role
  await assertFails(
    aliceDb
      .collection("delivery_orders")
      .doc("order_chat_1")
      .collection("chat_messages")
      .doc("msg_spoof")
      .set({
        orderId: "order_chat_1",
        senderUid: ALICE_UID,
        senderRole: "RIDER",
        senderName: "Fake Rider",
        messageText: "Spoofed message",
        createdAt: new Date(),
      })
  );

  // Unrelated Bob and unauthenticated caller cannot read or send chat messages
  await assertFails(
    bobDb
      .collection("delivery_orders")
      .doc("order_chat_1")
      .collection("chat_messages")
      .get()
  );
  await assertFails(
    bobDb
      .collection("delivery_orders")
      .doc("order_chat_1")
      .collection("chat_messages")
      .doc("msg_bob")
      .set({
        orderId: "order_chat_1",
        senderUid: BOB_UID,
        senderRole: "CUSTOMER",
        senderName: "Bob",
        messageText: "Intruder message",
        createdAt: new Date(),
      })
  );
  await assertFails(
    unauthDb
      .collection("delivery_orders")
      .doc("order_chat_1")
      .collection("chat_messages")
      .doc("msg_1")
      .get()
  );

  // Chat messages are immutable (cannot update or delete)
  await assertFails(
    aliceDb
      .collection("delivery_orders")
      .doc("order_chat_1")
      .collection("chat_messages")
      .doc("msg_1")
      .delete()
  );
});

test("9. Step 51 Multi-Rider Fair Claim, Duplicate Claim Prevention & Pre-Pickup Reassignment", async () => {
  const RIDER_2_UID = "rider_102_uid";
  const createdTs = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("delivery_orders")
      .doc("order_fair_dispatch_1")
      .set(validOrderPayload(ALICE_UID, { createdAt: createdTs, updatedAt: createdTs }));
  });

  const rider1Db = testEnv.authenticatedContext(RIDER_UID).firestore();
  const rider2Db = testEnv.authenticatedContext(RIDER_2_UID).firestore();

  await assertSucceeds(
    rider1Db.collection("rider_wallets").doc(RIDER_UID).set(validRiderWalletPayload(RIDER_UID, { riderId: "RIDER-101" }))
  );
  await assertSucceeds(
    rider2Db.collection("rider_wallets").doc(RIDER_2_UID).set(validRiderWalletPayload(RIDER_2_UID, { riderId: "RIDER-102", riderName: "Aman Verma" }))
  );

  // Rider 1 claims the PENDING_ASSIGNMENT order
  await assertSucceeds(
    rider1Db.collection("delivery_orders").doc("order_fair_dispatch_1").update({
      assignedRiderUid: RIDER_UID,
      assignedRiderId: "RIDER-101",
      status: "ASSIGNED",
      updatedAt: new Date(),
    })
  );

  // Rider 2 CANNOT overwrite or double-claim an already ASSIGNED order
  await assertFails(
    rider2Db.collection("delivery_orders").doc("order_fair_dispatch_1").update({
      assignedRiderUid: RIDER_2_UID,
      assignedRiderId: "RIDER-102",
      status: "ASSIGNED",
      updatedAt: new Date(),
    })
  );

  // Rider 1 releases the order before pickup back to PENDING_ASSIGNMENT -> Rider 2 can now claim it
  const firebase = require("@firebase/rules-unit-testing");
  await assertSucceeds(
    rider1Db.collection("delivery_orders").doc("order_fair_dispatch_1").set({
      ...validOrderPayload(ALICE_UID, {
        createdAt: createdTs,
        updatedAt: new Date(),
        status: "PENDING_ASSIGNMENT",
      }),
    })
  );

  await assertSucceeds(
    rider2Db.collection("delivery_orders").doc("order_fair_dispatch_1").update({
      assignedRiderUid: RIDER_2_UID,
      assignedRiderId: "RIDER-102",
      status: "ASSIGNED",
      updatedAt: new Date(),
    })
  );
});

test("10. Security Audit: Customer cannot spoof DELIVERED status, forge fare split arithmetic, or inject XSS script payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // 10a. Cannot create an order directly in DELIVERED or ASSIGNED state
  await assertFails(
    aliceDb.collection("delivery_orders").doc("order_spoof_delivered").set(
      validOrderPayload(ALICE_UID, { status: "DELIVERED" })
    )
  );

  // 10b. Cannot create an order with tampered 80/20 split (totalAmountPaise != companyCommissionPaise + riderEarningPaise)
  await assertFails(
    aliceDb.collection("delivery_orders").doc("order_bad_math").set(
      validOrderPayload(ALICE_UID, {
        totalAmountPaise: 12000,
        companyCommissionPaise: 100,
        riderEarningPaise: 100,
      })
    )
  );

  // 10c. Cannot inject XSS <script> tags into pickupAddress or customerName
  await assertFails(
    aliceDb.collection("delivery_orders").doc("order_xss_1").set(
      validOrderPayload(ALICE_UID, {
        pickupAddress: "<script>alert('xss')</script> MG Road",
      })
    )
  );

  // 10d. Create valid PENDING_ASSIGNMENT order -> Customer can cancel before pickup, but CANNOT mark DELIVERED
  await assertSucceeds(
    aliceDb.collection("delivery_orders").doc("order_alice_lifecycle").set(
      validOrderPayload(ALICE_UID)
    )
  );
  await assertFails(
    aliceDb.collection("delivery_orders").doc("order_alice_lifecycle").update({
      status: "DELIVERED",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("delivery_orders").doc("order_alice_lifecycle").update({
      status: "CANCELLED",
      updatedAt: new Date(),
    })
  );
});

test("11. User Profiles Security: Owner-only access, XSS rejection, and non-admin ADMIN role escalation blocking", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Alice can create her own CUSTOMER profile
  await assertSucceeds(
    aliceDb.collection("user_profiles").doc(ALICE_UID).set({
      id: ALICE_UID,
      name: "Aarav Sharma",
      phone: "9876543210",
      role: "CUSTOMER",
      updatedAt: new Date().toISOString(),
    })
  );

  // Bob cannot read or overwrite Alice's profile
  await assertFails(bobDb.collection("user_profiles").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("user_profiles").doc(ALICE_UID).update({
      name: "Hacked by Bob",
    })
  );

  // Alice cannot escalate her own role from CUSTOMER to ADMIN
  await assertFails(
    aliceDb.collection("user_profiles").doc(ALICE_UID).update({
      role: "ADMIN",
      updatedAt: new Date().toISOString(),
    })
  );

  // Bob cannot self-register as ADMIN
  await assertFails(
    bobDb.collection("user_profiles").doc(BOB_UID).set({
      id: BOB_UID,
      name: "Bob Intruder",
      phone: "9876500000",
      role: "ADMIN",
      updatedAt: new Date().toISOString(),
    })
  );
});


