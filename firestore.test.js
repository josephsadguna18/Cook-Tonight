const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
const DATABASE_ID = "ai-studio-android-cooktoni-d8be1338-ecd5-4cb3-be72-8359630123f9";

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085,
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

test("unauthenticated user cannot read cached recipes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore({ databaseId: DATABASE_ID });
  const docRef = unauthDb.doc("users/alice/cached_recipes/italian");
  await assertFails(docRef.get());
});

test("authenticated user can create own cached recipe group", async () => {
  const authDb = testEnv.authenticatedContext("alice").firestore({ databaseId: DATABASE_ID });
  const docRef = authDb.doc("users/alice/cached_recipes/italian");
  await assertSucceeds(docRef.set({
    query: "italian",
    cuisine: "Italian",
    recipesJson: JSON.stringify([{ name: "Pasta Carbonara" }]),
    userId: "alice",
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("user cannot read another user's cached recipe group", async () => {
  const bobDb = testEnv.authenticatedContext("bob").firestore({ databaseId: DATABASE_ID });
  const docRef = bobDb.doc("users/alice/cached_recipes/italian");
  await assertFails(docRef.get());
});

test("user cannot create cached recipe with mismatched userId", async () => {
  const authDb = testEnv.authenticatedContext("alice").firestore({ databaseId: DATABASE_ID });
  const docRef = authDb.doc("users/alice/cached_recipes/italian");
  await assertFails(docRef.set({
    query: "italian",
    cuisine: "Italian",
    recipesJson: "[]",
    userId: "bob",
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("user can save a recipe and read it", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore({ databaseId: DATABASE_ID });
  const docRef = aliceDb.doc("users/alice/saved_recipes/rec_1");
  await assertSucceeds(docRef.set({
    recipeId: "rec_1",
    name: "Butter Chicken",
    cuisine: "Indian",
    time: "45 mins",
    serves: "4",
    steps: ["Marinate chicken", "Cook sauce"],
    ingredients: [{ name: "Chicken", quantity: "500g" }],
    userId: "alice",
    createdAt: new Date(),
    updatedAt: new Date()
  }));

  await assertSucceeds(docRef.get());
});
