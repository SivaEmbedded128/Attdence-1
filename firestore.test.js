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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user: cannot read or write any user collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("subjects").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("subjects").doc("sub_1").set({
      userId: ALICE_UID,
      name: "Digital Electronics",
      code: "EC301",
      conducted: 40,
      attended: 30,
      requiredPercent: 75.0,
      colorIndex: 0,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated owner: valid profile, subject, timetable, attendance, holiday CRUD succeeds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 5000);
  const now = new Date(Date.now() - 1000);

  // 1. Profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      defaultRequiredPercent: 75.0,
      seededDemoData: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // 2. Subject (NO @DocumentId 'id' field)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("subjects").doc("sub_de").set({
      userId: ALICE_UID,
      name: "Digital Electronics",
      code: "EC301",
      conducted: 40,
      attended: 34,
      requiredPercent: 75.0,
      colorIndex: 1,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Partial subject update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("subjects").doc("sub_de").update({
      conducted: 41,
      attended: 35,
      updatedAt: now,
    })
  );

  // 3. Timetable slot
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("timetable").doc("slot_mon_1").set({
      userId: ALICE_UID,
      subjectId: "sub_de",
      dayOfWeek: 1,
      periodIndex: 1,
      timeSlot: "09:00 - 09:50",
      periodsCount: 1,
      createdAt: past,
      updatedAt: past,
    })
  );

  // 4. Attendance record
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("2026-11-18_sub_de_1").set({
      userId: ALICE_UID,
      subjectId: "sub_de",
      dateIso: "2026-11-18",
      periodIndex: 1,
      periodsCount: 1,
      status: "PRESENT",
      note: "",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Correction update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("2026-11-18_sub_de_1").update({
      status: "ABSENT",
      note: "Updated by student",
      updatedAt: now,
    })
  );

  // 5. Holiday
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("holidays").doc("2026-11-20").set({
      userId: ALICE_UID,
      dateIso: "2026-11-20",
      reason: "Tech Fest",
      createdAt: past,
      updatedAt: past,
    })
  );

  // List queries
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("subjects").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("timetable").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("attendance").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("holidays").get());
});

test("Cross-user isolation: Bob cannot read or write Alice's data", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).collection("subjects").doc("sub_1").set({
      userId: ALICE_UID,
      name: "VLSI Design",
      code: "EC302",
      conducted: 30,
      attended: 25,
      requiredPercent: 75.0,
      colorIndex: 2,
      createdAt: new Date(Date.now() - 5000),
      updatedAt: new Date(Date.now() - 5000),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("subjects").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("subjects").doc("sub_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("subjects").doc("sub_1").update({
      name: "Hacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("subjects").doc("sub_1").delete());
});

test("Negative bounds: reject attended > conducted, ghost 'id' field, future timestamp, invalid status", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 5000);

  // attended > conducted
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("subjects").doc("sub_bad1").set({
      userId: ALICE_UID,
      name: "RF Circuits",
      code: "EC402",
      conducted: 10,
      attended: 12,
      requiredPercent: 75.0,
      colorIndex: 0,
      createdAt: past,
      updatedAt: past,
    })
  );

  // ghost 'id' field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("subjects").doc("sub_bad2").set({
      id: "sub_bad2",
      userId: ALICE_UID,
      name: "RF Circuits",
      code: "EC402",
      conducted: 10,
      attended: 8,
      requiredPercent: 75.0,
      colorIndex: 0,
      createdAt: past,
      updatedAt: past,
    })
  );

  // future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("subjects").doc("sub_bad3").set({
      userId: ALICE_UID,
      name: "RF Circuits",
      code: "EC402",
      conducted: 10,
      attended: 8,
      requiredPercent: 75.0,
      colorIndex: 0,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: past,
    })
  );

  // invalid attendance status
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("2026-11-18_sub_1_1").set({
      userId: ALICE_UID,
      subjectId: "sub_1",
      dateIso: "2026-11-18",
      periodIndex: 1,
      periodsCount: 1,
      status: "LATE",
      note: "",
      createdAt: past,
      updatedAt: past,
    })
  );
});
