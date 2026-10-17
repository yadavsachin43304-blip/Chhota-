const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_owner_123";
const BOB_UID = "bob_intruder_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validTaskPayload(userId, overrides = {}) {
  const now = new Date();
  return {
    userId,
    title: "Chrome खोल और OpenAI वेबसाइट खोल",
    userRequest: "छोटू, Chrome खोल और OpenAI की वेबसाइट खोल।",
    intentCategory: "BROWSER_AUTOMATION",
    riskLevel: "LOW",
    status: "COMPLETED",
    requiresApproval: false,
    approvedByOwner: true,
    affectedResource: "https://openai.com",
    reversible: true,
    verificationResult: "VERIFIED_INTENT_DISPATCHED",
    planSummary: "1. Authenticate owner -> 2. Parse URL -> 3. Launch browser intent",
    executionTarget: "LOCAL_DEVICE",
    createdAt: now,
    updatedAt: now,
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

test("1. Unauthenticated caller cannot read or create tasks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("tasks").get());
  await assertFails(unauthDb.collection("tasks").doc("task_1").set(validTaskPayload(ALICE_UID)));
});

test("2. Authenticated owner can create and read their own task", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("tasks").doc("task_alice_1").set(validTaskPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("tasks").doc("task_alice_1").get());
});

test("3. Cross-user isolation: Bob cannot read or delete Alice's task", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("tasks").doc("task_alice_1").set(validTaskPayload(ALICE_UID)));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("tasks").doc("task_alice_1").get());
  await assertFails(bobDb.collection("tasks").doc("task_alice_1").delete());
});

test("4. Query Enforcer: Authenticated owner can query tasks with userId filter, fails without filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("tasks").doc("task_alice_1").set(validTaskPayload(ALICE_UID)));

  await assertSucceeds(aliceDb.collection("tasks").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("tasks").get());
});

test("5. Identity spoofing & shadow field injection rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("tasks").doc("spoof_1").set(validTaskPayload(BOB_UID)));
  await assertFails(
    aliceDb.collection("tasks").doc("ghost_1").set(validTaskPayload(ALICE_UID, { bypassSecurity: true }))
  );
});

test("6. Append-only security logs can be created by owner but never updated or deleted", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const logRef = aliceDb.collection("security_logs").doc("log_1");
  await assertSucceeds(
    logRef.set({
      userId: ALICE_UID,
      eventType: "AUTH_VERIFIED",
      severity: "INFO",
      actorIdentity: "Owner (Biometric + Google)",
      description: "Owner session verified.",
      taskRefId: "task_alice_1",
      createdAt: new Date(),
    })
  );
  await assertFails(logRef.update({ severity: "CRITICAL" }));
  await assertFails(logRef.delete());
});
