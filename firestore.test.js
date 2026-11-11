const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
const DATABASE_ID = "ai-studio-android-ludoroya-0cb0be7e-d15a-4230-88e1-c0113581aaea";

let testEnv;

before(async () => {
  const rules = fs.readFileSync("firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: "127.0.0.1",
      port: 8085,
      databaseId: DATABASE_ID,
    },
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

test("unauthenticated user cannot read user profile", async () => {
  const unauthContext = testEnv.unauthenticatedContext();
  const db = unauthContext.firestore(DATABASE_ID);
  await assertFails(db.collection("users").doc("user_123").get());
});

test("authenticated user can create own profile", async () => {
  const authContext = testEnv.authenticatedContext("user_123");
  const db = authContext.firestore(DATABASE_ID);
  const now = new Date();
  await assertSucceeds(
    db.collection("users").doc("user_123").set({
      userId: "user_123",
      displayName: "Player One",
      avatarId: "avatar_king",
      coinBalance: 500,
      winningsBalance: 100,
      gamesWon: 0,
      gamesPlayed: 0,
      createdAt: now,
    })
  );
});

test("authenticated user cannot update someone else's profile", async () => {
  const authContext = testEnv.authenticatedContext("user_hacker");
  const db = authContext.firestore(DATABASE_ID);
  await assertFails(
    db.collection("users").doc("user_123").update({
      displayName: "Hacked",
    })
  );
});

test("authenticated user can create match and message", async () => {
  const authContext = testEnv.authenticatedContext("user_123");
  const db = authContext.firestore(DATABASE_ID);
  const now = new Date();
  await assertSucceeds(
    db.collection("matches").doc("match_1").set({
      matchId: "match_1",
      creatorId: "user_123",
      status: "waiting",
      gameMode: "multiplayer_quick",
      entryFee: 50,
      prizePool: 90,
      playerIds: ["user_123"],
      currentTurnPlayerId: "user_123",
      lastDiceRoll: 0,
      boardStateJson: "{}",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    db.collection("matches").doc("match_1").collection("messages").doc("msg_1").set({
      messageId: "msg_1",
      matchId: "match_1",
      senderId: "user_123",
      senderName: "Player One",
      avatarId: "avatar_king",
      text: "Good luck!",
      createdAt: now,
    })
  );
});
