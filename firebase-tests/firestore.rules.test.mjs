import { after, before, beforeEach, test } from "node:test";
import { readFileSync } from "node:fs";
import assert from "node:assert/strict";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  Timestamp,
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  limit,
  orderBy,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
  writeBatch,
} from "firebase/firestore";

const PROJECT_ID = "demo-propcycle";
const OWNER_UID = "owner-user";
const CONTACT_UID = "contact-user";
const OUTSIDER_UID = "outsider-user";
const LISTING_ID = "listing-one";
const THREAD_ID = `marketplace_${LISTING_ID}_${OWNER_UID}_${CONTACT_UID}`;

let testEnvironment;

before(async () => {
  testEnvironment = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host: "127.0.0.1",
      port: 8080,
      rules: readFileSync(new URL("../firestore.rules", import.meta.url), "utf8"),
    },
  });
});

beforeEach(async () => {
  await testEnvironment.clearFirestore();
});

after(async () => {
  await testEnvironment.cleanup();
});

function signedIn(uid) {
  return testEnvironment.authenticatedContext(uid, {
    email: `${uid}@example.test`,
  }).firestore();
}

function listingData(ownerId = OWNER_UID) {
  return {
    ownerId,
    title: "Reusable Event Backdrop",
    titleNormalized: "reusable event backdrop",
    description: "A modular backdrop ready for another event.",
    category: "decoration",
    condition: "good",
    transactionIntent: "donation",
    fulfilmentMethod: "pickup",
    priceMinor: 0,
    exchangeTerms: "",
    imageUrl: null,
    status: "available",
    createdAt: Timestamp.fromMillis(1000),
    updatedAt: Timestamp.fromMillis(1000),
  };
}

function threadData() {
  return {
    contextType: "marketplace",
    contextId: LISTING_ID,
    contextTitle: "Reusable Event Backdrop",
    ownerUid: OWNER_UID,
    contactUid: CONTACT_UID,
    participantIds: [OWNER_UID, CONTACT_UID],
    lastMessageId: "",
    lastMessageText: "",
    lastMessageSenderId: "",
    lastMessageAt: serverTimestamp(),
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

async function seedBaseData({ includeThread = false } = {}) {
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    const database = context.firestore();
    const timestamp = Timestamp.fromMillis(1000);
    await Promise.all([
      setDoc(doc(database, "users", OWNER_UID), {
        displayName: "Owner",
        createdAt: timestamp,
        updatedAt: timestamp,
      }),
      setDoc(doc(database, "users", CONTACT_UID), {
        displayName: "Contact",
        createdAt: timestamp,
        updatedAt: timestamp,
      }),
      setDoc(doc(database, "marketplaceListings", LISTING_ID), listingData()),
    ]);
    if (includeThread) {
      await setDoc(doc(database, "chatThreads", THREAD_ID), {
        contextType: "marketplace",
        contextId: LISTING_ID,
        contextTitle: "Reusable Event Backdrop",
        ownerUid: OWNER_UID,
        contactUid: CONTACT_UID,
        participantIds: [OWNER_UID, CONTACT_UID],
        lastMessageId: "",
        lastMessageText: "",
        lastMessageSenderId: "",
        lastMessageAt: timestamp,
        createdAt: timestamp,
        updatedAt: timestamp,
      });
    }
  });
}

test("a user creates a minimal profile but cannot write another user's profile", async () => {
  const ownerDatabase = signedIn(OWNER_UID);
  await assertSucceeds(setDoc(doc(ownerDatabase, "users", OWNER_UID), {
    displayName: "Owner",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));

  await assertFails(setDoc(doc(ownerDatabase, "users", CONTACT_UID), {
    displayName: "Forged Contact",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));
});

test("profiles reject private or forged fields and preserve creation time", async () => {
  const newUid = "new-profile-user";
  const database = signedIn(newUid);
  await assertFails(setDoc(doc(database, "users", newUid), {
    uid: newUid,
    displayName: "Extra Field",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));
  await assertFails(setDoc(doc(database, "users", newUid), {
    email: "private@example.test",
    displayName: "Private Field",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));

  await seedBaseData();
  const ownerDatabase = signedIn(OWNER_UID);
  await assertSucceeds(updateDoc(doc(ownerDatabase, "users", OWNER_UID), {
    displayName: "Updated Owner",
    updatedAt: serverTimestamp(),
  }));
  await assertFails(updateDoc(doc(ownerDatabase, "users", OWNER_UID), {
    createdAt: Timestamp.fromMillis(9999),
    updatedAt: serverTimestamp(),
  }));
});

test("an authenticated user can directly read a public display profile but cannot enumerate", async () => {
  await seedBaseData();
  const contactDatabase = signedIn(CONTACT_UID);
  await assertSucceeds(getDoc(doc(contactDatabase, "users", OWNER_UID)));
  await assertFails(getDocs(collection(contactDatabase, "users")));
});

test("a valid listing write succeeds while a forged owner fails", async () => {
  await seedBaseData();
  const ownerDatabase = signedIn(OWNER_UID);
  const valid = listingData();
  valid.createdAt = serverTimestamp();
  valid.updatedAt = serverTimestamp();
  await assertSucceeds(setDoc(
    doc(ownerDatabase, "marketplaceListings", "new-listing"),
    valid,
  ));

  const forged = {...valid, ownerId: CONTACT_UID};
  await assertFails(setDoc(
    doc(ownerDatabase, "marketplaceListings", "forged-listing"),
    forged,
  ));

  const zeroPriceSale = {
    ...valid,
    transactionIntent: "sale",
    priceMinor: 0,
    exchangeTerms: "",
  };
  await assertFails(setDoc(
    doc(ownerDatabase, "marketplaceListings", "zero-price-sale"),
    zeroPriceSale,
  ));

  const invalidCategory = {
    ...valid,
    category: "furniture",
  };
  await assertFails(setDoc(
    doc(ownerDatabase, "marketplaceListings", "invalid-category"),
    invalidCategory,
  ));

  const existingListing = doc(
    ownerDatabase,
    "marketplaceListings",
    LISTING_ID,
  );
  await assertFails(updateDoc(existingListing, {
    ownerId: CONTACT_UID,
    updatedAt: serverTimestamp(),
  }));
  await assertFails(updateDoc(existingListing, {
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));
});

test("the bounded available-listing query succeeds and an unbounded query fails", async () => {
  await seedBaseData();
  const contactDatabase = signedIn(CONTACT_UID);
  const allowed = query(
    collection(contactDatabase, "marketplaceListings"),
    where("status", "==", "available"),
    orderBy("createdAt", "desc"),
    limit(50),
  );
  const snapshot = await assertSucceeds(getDocs(allowed));
  assert.equal(snapshot.size, 1);
  await assertFails(getDocs(collection(contactDatabase, "marketplaceListings")));
});

test("only the contact can create the canonical listing thread", async () => {
  await seedBaseData();
  const contactDatabase = signedIn(CONTACT_UID);
  const outsiderDatabase = signedIn(OUTSIDER_UID);
  await assertFails(setDoc(
    doc(outsiderDatabase, "chatThreads", THREAD_ID),
    threadData(),
  ));

  await assertFails(setDoc(
    doc(contactDatabase, "chatThreads", THREAD_ID),
    {...threadData(), participantIds: [CONTACT_UID, OWNER_UID]},
  ));
  await assertFails(setDoc(
    doc(contactDatabase, "chatThreads", THREAD_ID),
    {...threadData(), contextTitle: "Forged listing title"},
  ));
  await assertSucceeds(setDoc(
    doc(contactDatabase, "chatThreads", THREAD_ID),
    threadData(),
  ));
});

test("the exact bounded client thread and message queries succeed", async () => {
  await seedBaseData({includeThread: true});
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    const database = context.firestore();
    await Promise.all([
      setDoc(doc(database, "chatThreads", THREAD_ID, "messages", "message-operation-0001"), {
        senderId: CONTACT_UID,
        text: "First message",
        clientOperationId: "message-operation-0001",
        sentAt: Timestamp.fromMillis(2000),
      }),
      setDoc(doc(database, "chatThreads", THREAD_ID, "messages", "message-operation-0002"), {
        senderId: OWNER_UID,
        text: "Second message",
        clientOperationId: "message-operation-0002",
        sentAt: Timestamp.fromMillis(3000),
      }),
    ]);
  });

  const contactDatabase = signedIn(CONTACT_UID);
  const threadCollection = collection(contactDatabase, "chatThreads");
  const clientThreadQuery = query(
    threadCollection,
    where("participantIds", "array-contains", CONTACT_UID),
    orderBy("updatedAt", "desc"),
    limit(50),
  );
  const threadSnapshot = await assertSucceeds(getDocs(clientThreadQuery));
  assert.equal(threadSnapshot.size, 1);
  assert.equal(threadSnapshot.docs[0].id, THREAD_ID);
  await assertFails(getDocs(query(
    threadCollection,
    where("participantIds", "array-contains", CONTACT_UID),
    orderBy("updatedAt", "desc"),
  )));

  const messageCollection = collection(
    contactDatabase,
    "chatThreads",
    THREAD_ID,
    "messages",
  );
  const clientMessageQuery = query(
    messageCollection,
    orderBy("sentAt", "asc"),
    limit(100),
  );
  const messageSnapshot = await assertSucceeds(getDocs(clientMessageQuery));
  assert.equal(messageSnapshot.size, 2);
  assert.equal(messageSnapshot.docs[0].data().text, "First message");
  assert.equal(messageSnapshot.docs[1].data().text, "Second message");
  await assertFails(getDocs(query(
    messageCollection,
    orderBy("sentAt", "asc"),
  )));
});

test("a message and its parent preview must be committed together", async () => {
  await seedBaseData({includeThread: true});
  const contactDatabase = signedIn(CONTACT_UID);
  const messageRef = doc(collection(
    contactDatabase,
    "chatThreads",
    THREAD_ID,
    "messages",
  ));
  const message = {
    senderId: CONTACT_UID,
    text: "Hi, is this still available?",
    clientOperationId: messageRef.id,
    sentAt: serverTimestamp(),
  };

  await assertFails(setDoc(messageRef, message));

  const batch = writeBatch(contactDatabase);
  batch.set(messageRef, message);
  batch.update(doc(contactDatabase, "chatThreads", THREAD_ID), {
    lastMessageId: messageRef.id,
    lastMessageText: message.text,
    lastMessageSenderId: CONTACT_UID,
    lastMessageAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
  await assertSucceeds(batch.commit());

  const outsiderDatabase = signedIn(OUTSIDER_UID);
  await assertFails(getDoc(doc(outsiderDatabase, "chatThreads", THREAD_ID)));
  await assertFails(updateDoc(doc(outsiderDatabase, "chatThreads", THREAD_ID), {
    lastMessageText: "forged",
  }));

  await assertFails(getDocs(query(
    collection(outsiderDatabase, "chatThreads", THREAD_ID, "messages"),
    orderBy("sentAt", "asc"),
    limit(100),
  )));
  await assertFails(updateDoc(messageRef, {text: "edited"}));
  await assertFails(deleteDoc(messageRef));
});

test("rules reject unauthenticated reads and malformed message batches", async () => {
  await seedBaseData({includeThread: true});
  const anonymousDatabase = testEnvironment.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(anonymousDatabase, "users", OWNER_UID)));
  await assertFails(getDoc(doc(
    anonymousDatabase,
    "marketplaceListings",
    LISTING_ID,
  )));
  await assertFails(getDoc(doc(anonymousDatabase, "chatThreads", THREAD_ID)));

  const contactDatabase = signedIn(CONTACT_UID);
  const malformedMessages = [
    {text: "forged sender", senderId: OWNER_UID},
    {text: "   ", senderId: CONTACT_UID},
    {text: "x".repeat(2001), senderId: CONTACT_UID},
  ];

  for (const malformed of malformedMessages) {
    const messageRef = doc(collection(
      contactDatabase,
      "chatThreads",
      THREAD_ID,
      "messages",
    ));
    const batch = writeBatch(contactDatabase);
    batch.set(messageRef, {
      senderId: malformed.senderId,
      text: malformed.text,
      clientOperationId: messageRef.id,
      sentAt: serverTimestamp(),
    });
    batch.update(doc(contactDatabase, "chatThreads", THREAD_ID), {
      lastMessageId: messageRef.id,
      lastMessageText: malformed.text,
      lastMessageSenderId: malformed.senderId,
      lastMessageAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
    await assertFails(batch.commit());
  }
});
