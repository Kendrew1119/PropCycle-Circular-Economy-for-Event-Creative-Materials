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
const LENDING_ITEM_ID = "lending-item-one";
const LENDING_REQUEST_ID = "lending-request-one";
const LENDING_THREAD_ID =
  `lending_${LENDING_ITEM_ID}_${OWNER_UID}_${CONTACT_UID}`;

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
    demoImageKey: "",
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

function marketplaceRatingData(
  recipientUid = OWNER_UID,
  raterUid = CONTACT_UID,
  contextListingId = LISTING_ID,
  score = 5,
) {
  return {
    raterUid,
    recipientUid,
    contextListingId,
    score,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function lendingItemData(ownerId = OWNER_UID) {
  return {
    ownerId,
    title: "Portable LED Lights",
    titleNormalized: "portable led lights",
    description: "A compact light set for community events.",
    category: "event_gear",
    condition: "good",
    pickupMethod: "meetup",
    areaLabel: "Petaling Jaya",
    maxBorrowDays: 7,
    depositMinor: 5000,
    latitude: 3.11,
    longitude: 101.64,
    imageUrl: null,
    demoImageKey: "",
    status: "available",
    createdAt: Timestamp.fromMillis(1000),
    updatedAt: Timestamp.fromMillis(1000),
  };
}

function lendingRequestData() {
  return {
    itemId: LENDING_ITEM_ID,
    itemTitle: "Portable LED Lights",
    ownerUid: OWNER_UID,
    borrowerUid: CONTACT_UID,
    participantIds: [OWNER_UID, CONTACT_UID],
    startDate: "2026-09-10",
    endDate: "2026-09-11",
    dayKeys: ["2026-09-10", "2026-09-11"],
    status: "pending",
    lockToken: "",
    returnReported: false,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

async function seedLendingData({ includeRequest = false } = {}) {
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
        displayName: "Borrower",
        createdAt: timestamp,
        updatedAt: timestamp,
      }),
      setDoc(
        doc(database, "lendingItems", LENDING_ITEM_ID),
        lendingItemData(),
      ),
    ]);
    if (includeRequest) {
      const request = lendingRequestData();
      request.createdAt = timestamp;
      request.updatedAt = timestamp;
      await setDoc(
        doc(database, "lendingRequests", LENDING_REQUEST_ID),
        request,
      );
    }
  });
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

test("marketplace seller ratings are bounded, authentic, editable, and non-deletable", async () => {
  await seedBaseData();
  const contactDatabase = signedIn(CONTACT_UID);
  const ownerDatabase = signedIn(OWNER_UID);
  const outsiderDatabase = signedIn(OUTSIDER_UID);
  const contactRating = doc(
    contactDatabase,
    "users",
    OWNER_UID,
    "marketplaceRatings",
    CONTACT_UID,
  );

  await assertSucceeds(setDoc(contactRating, marketplaceRatingData()));
  await assertSucceeds(updateDoc(contactRating, {
    score: 4,
    updatedAt: serverTimestamp(),
  }));
  await assertFails(deleteDoc(contactRating));

  await assertFails(setDoc(
    doc(ownerDatabase, "users", OWNER_UID, "marketplaceRatings", OWNER_UID),
    marketplaceRatingData(OWNER_UID, OWNER_UID),
  ));
  await assertFails(setDoc(
    doc(outsiderDatabase, "users", OWNER_UID, "marketplaceRatings", CONTACT_UID),
    marketplaceRatingData(),
  ));
  await assertFails(setDoc(
    doc(outsiderDatabase, "users", OWNER_UID, "marketplaceRatings", OUTSIDER_UID),
    marketplaceRatingData(OWNER_UID, OUTSIDER_UID, LISTING_ID, 6),
  ));

  const boundedRatings = query(
    collection(contactDatabase, "users", OWNER_UID, "marketplaceRatings"),
    limit(100),
  );
  const snapshot = await assertSucceeds(getDocs(boundedRatings));
  assert.equal(snapshot.size, 1);
  await assertFails(getDocs(collection(
    contactDatabase,
    "users",
    OWNER_UID,
    "marketplaceRatings",
  )));
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

  await assertFails(setDoc(
    doc(ownerDatabase, "marketplaceListings", "unsafe.listing"),
    valid,
  ));

  const prematureImage = {
    ...valid,
    imageUrl: "https://example.test/not-enabled.jpg",
  };
  await assertFails(setDoc(
    doc(ownerDatabase, "marketplaceListings", "premature-image"),
    prematureImage,
  ));

  const listingWithImage = {
    ...valid,
    imageUrl: "gs://demo-propcycle.firebasestorage.app/marketplace/"
      + `${OWNER_UID}/listing-with-image/primary_version-one.jpg`,
  };
  await assertSucceeds(setDoc(
    doc(ownerDatabase, "marketplaceListings", "listing-with-image"),
    listingWithImage,
  ));

  await assertSucceeds(setDoc(
    doc(ownerDatabase, "marketplaceListings", "listing-with-demo-image"),
    {...valid, demoImageKey: "cardboard_box"},
  ));
  await assertFails(setDoc(
    doc(ownerDatabase, "marketplaceListings", "listing-with-forged-demo"),
    {...valid, demoImageKey: "../../private_photo"},
  ));
  await assertFails(setDoc(
    doc(ownerDatabase, "marketplaceListings", "listing-with-two-images"),
    {
      ...valid,
      demoImageKey: "event_banner",
      imageUrl: "gs://demo-propcycle.firebasestorage.app/marketplace/"
        + `${OWNER_UID}/listing-with-two-images/primary_version-one.jpg`,
    },
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

test("only the owner can edit allowed listing fields", async () => {
  await seedBaseData();
  const ownerDatabase = signedIn(OWNER_UID);
  const contactDatabase = signedIn(CONTACT_UID);
  const ownerListing = doc(ownerDatabase, "marketplaceListings", LISTING_ID);
  const contactListing = doc(contactDatabase, "marketplaceListings", LISTING_ID);

  await assertFails(updateDoc(contactListing, {
    status: "withdrawn",
    updatedAt: serverTimestamp(),
  }));
  await assertSucceeds(updateDoc(ownerListing, {
    title: "Reusable Green Event Backdrop",
    titleNormalized: "reusable green event backdrop",
    description: "Updated details from the listing owner.",
    category: "decoration",
    condition: "like_new",
    transactionIntent: "sale",
    fulfilmentMethod: "meetup",
    priceMinor: 2500,
    exchangeTerms: "",
    updatedAt: serverTimestamp(),
  }));
  await assertFails(updateDoc(contactListing, {
    title: "Forged edit",
    titleNormalized: "forged edit",
    updatedAt: serverTimestamp(),
  }));
});

test("listing owner can withdraw and relist while public visibility follows status", async () => {
  await seedBaseData();
  const ownerDatabase = signedIn(OWNER_UID);
  const contactDatabase = signedIn(CONTACT_UID);
  const ownerListing = doc(ownerDatabase, "marketplaceListings", LISTING_ID);
  const contactListing = doc(contactDatabase, "marketplaceListings", LISTING_ID);

  await assertSucceeds(updateDoc(ownerListing, {
    status: "withdrawn",
    updatedAt: serverTimestamp(),
  }));
  await assertSucceeds(getDoc(ownerListing));
  await assertFails(getDoc(contactListing));

  const publicQuery = query(
    collection(contactDatabase, "marketplaceListings"),
    where("status", "==", "available"),
    orderBy("createdAt", "desc"),
    limit(50),
  );
  const hiddenSnapshot = await assertSucceeds(getDocs(publicQuery));
  assert.equal(hiddenSnapshot.size, 0);
  await assertFails(setDoc(
    doc(contactDatabase, "chatThreads", THREAD_ID),
    threadData(),
  ));

  await assertSucceeds(updateDoc(ownerListing, {
    status: "available",
    updatedAt: serverTimestamp(),
  }));
  await assertSucceeds(getDoc(contactListing));
  const visibleSnapshot = await assertSucceeds(getDocs(publicQuery));
  assert.equal(visibleSnapshot.size, 1);
});

test("listing updates validate image ownership, unknown fields, and status", async () => {
  await seedBaseData();
  const ownerDatabase = signedIn(OWNER_UID);
  const contactDatabase = signedIn(CONTACT_UID);
  const ownerListing = doc(ownerDatabase, "marketplaceListings", LISTING_ID);
  const contactListing = doc(contactDatabase, "marketplaceListings", LISTING_ID);

  const validImageUrl = "gs://demo-propcycle.firebasestorage.app/marketplace/"
    + `${OWNER_UID}/${LISTING_ID}/primary_version-one.jpg`;
  await assertSucceeds(updateDoc(ownerListing, {
    imageUrl: validImageUrl,
    updatedAt: serverTimestamp(),
  }));
  await assertFails(updateDoc(contactListing, {
    imageUrl: validImageUrl,
    updatedAt: serverTimestamp(),
  }));

  await assertFails(updateDoc(ownerListing, {
    imageUrl: "https://example.test/forged.jpg",
    updatedAt: serverTimestamp(),
  }));
  await assertFails(updateDoc(ownerListing, {
    imageUrl: "gs://demo-propcycle.firebasestorage.app/marketplace/"
      + `${CONTACT_UID}/${LISTING_ID}/primary_forged.jpg`,
    updatedAt: serverTimestamp(),
  }));
  await assertFails(updateDoc(ownerListing, {
    imageUrl: "gs://demo-propcycle.firebasestorage.app/marketplace/"
      + `${OWNER_UID}/different-listing/primary_forged.jpg`,
    updatedAt: serverTimestamp(),
  }));
  await assertFails(updateDoc(ownerListing, {
    hiddenAdminNote: "not allowed",
    updatedAt: serverTimestamp(),
  }));
  await assertFails(updateDoc(ownerListing, {
    status: "completed",
    updatedAt: serverTimestamp(),
  }));
  await assertFails(deleteDoc(ownerListing));
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

test("lending item creation is owner-only and validates privacy-safe fields", async () => {
  await seedLendingData();
  const ownerDatabase = signedIn(OWNER_UID);
  const contactDatabase = signedIn(CONTACT_UID);
  const valid = lendingItemData();
  valid.createdAt = serverTimestamp();
  valid.updatedAt = serverTimestamp();
  await assertSucceeds(setDoc(
    doc(ownerDatabase, "lendingItems", "second-lending-item"),
    valid,
  ));

  await assertSucceeds(setDoc(
    doc(ownerDatabase, "lendingItems", "lending-with-demo-image"),
    {...valid, demoImageKey: "folding_chairs"},
  ));
  await assertFails(setDoc(
    doc(ownerDatabase, "lendingItems", "lending-with-forged-demo"),
    {...valid, demoImageKey: "unknown_sample"},
  ));
  await assertFails(setDoc(
    doc(ownerDatabase, "lendingItems", "lending-with-two-images"),
    {
      ...valid,
      demoImageKey: "speaker_set",
      imageUrl: "gs://demo-propcycle.firebasestorage.app/lending/"
        + `${OWNER_UID}/lending-with-two-images/primary_version-one.jpg`,
    },
  ));

  const forged = lendingItemData(OWNER_UID);
  forged.createdAt = serverTimestamp();
  forged.updatedAt = serverTimestamp();
  await assertFails(setDoc(
    doc(contactDatabase, "lendingItems", "forged-lending-item"),
    forged,
  ));

  const precise = lendingItemData();
  precise.latitude = "3.123456";
  precise.createdAt = serverTimestamp();
  precise.updatedAt = serverTimestamp();
  await assertFails(setDoc(
    doc(ownerDatabase, "lendingItems", "invalid-location-item"),
    precise,
  ));
});

test("lending browse is bounded and withdrawn items stay owner-only", async () => {
  await seedLendingData();
  const contactDatabase = signedIn(CONTACT_UID);
  const ownerDatabase = signedIn(OWNER_UID);
  await assertSucceeds(getDocs(query(
    collection(contactDatabase, "lendingItems"),
    where("status", "==", "available"),
    limit(50),
  )));
  await assertFails(getDocs(collection(contactDatabase, "lendingItems")));
  await assertSucceeds(updateDoc(
    doc(ownerDatabase, "lendingItems", LENDING_ITEM_ID),
    {status: "withdrawn", updatedAt: serverTimestamp()},
  ));
  await assertSucceeds(getDoc(
    doc(ownerDatabase, "lendingItems", LENDING_ITEM_ID),
  ));
  await assertFails(getDoc(
    doc(contactDatabase, "lendingItems", LENDING_ITEM_ID),
  ));
});

test("borrower creates a bounded lending request and outsiders cannot read it", async () => {
  await seedLendingData();
  const contactDatabase = signedIn(CONTACT_UID);
  const ownerDatabase = signedIn(OWNER_UID);
  const outsiderDatabase = signedIn(OUTSIDER_UID);
  await assertSucceeds(setDoc(
    doc(contactDatabase, "lendingRequests", LENDING_REQUEST_ID),
    lendingRequestData(),
  ));
  await assertSucceeds(getDoc(
    doc(ownerDatabase, "lendingRequests", LENDING_REQUEST_ID),
  ));
  await assertFails(getDoc(
    doc(outsiderDatabase, "lendingRequests", LENDING_REQUEST_ID),
  ));

  const forged = lendingRequestData();
  forged.ownerUid = OUTSIDER_UID;
  forged.participantIds = [OUTSIDER_UID, CONTACT_UID];
  await assertFails(setDoc(
    doc(contactDatabase, "lendingRequests", "forged-request"),
    forged,
  ));
});

test("lending approval requires owner-created date locks", async () => {
  await seedLendingData({includeRequest: true});
  const ownerDatabase = signedIn(OWNER_UID);
  const contactDatabase = signedIn(CONTACT_UID);
  const token = "12345678901234567890123456789012";
  await assertFails(updateDoc(
    doc(ownerDatabase, "lendingRequests", LENDING_REQUEST_ID),
    {status: "approved", lockToken: token, updatedAt: serverTimestamp()},
  ));
  const batch = writeBatch(ownerDatabase);
  batch.update(doc(ownerDatabase, "lendingRequests", LENDING_REQUEST_ID), {
    status: "approved",
    lockToken: token,
    updatedAt: serverTimestamp(),
  });
  for (const day of ["2026-09-10", "2026-09-11"]) {
    batch.set(
      doc(ownerDatabase, "lendingItems", LENDING_ITEM_ID, "bookedDays", day),
      {
        requestId: LENDING_REQUEST_ID,
        lockToken: token,
        date: day,
        updatedAt: serverTimestamp(),
      },
    );
  }
  await assertSucceeds(batch.commit());
  await assertFails(updateDoc(
    doc(contactDatabase, "lendingRequests", LENDING_REQUEST_ID),
    {status: "active", updatedAt: serverTimestamp()},
  ));

  const cancelBatch = writeBatch(contactDatabase);
  cancelBatch.update(
    doc(contactDatabase, "lendingRequests", LENDING_REQUEST_ID),
    {status: "cancelled", updatedAt: serverTimestamp()},
  );
  for (const day of ["2026-09-10", "2026-09-11"]) {
    cancelBatch.delete(doc(
      contactDatabase,
      "lendingItems",
      LENDING_ITEM_ID,
      "bookedDays",
      day,
    ));
  }
  await assertSucceeds(cancelBatch.commit());
});

test("lending return and rating lifecycle is participant-scoped", async () => {
  await seedLendingData({includeRequest: true});
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    await updateDoc(
      doc(context.firestore(), "lendingRequests", LENDING_REQUEST_ID),
      {
        status: "active",
        lockToken: "12345678901234567890123456789012",
        updatedAt: Timestamp.fromMillis(2000),
      },
    );
  });
  const contactDatabase = signedIn(CONTACT_UID);
  const ownerDatabase = signedIn(OWNER_UID);
  await assertSucceeds(updateDoc(
    doc(contactDatabase, "lendingRequests", LENDING_REQUEST_ID),
    {returnReported: true, updatedAt: serverTimestamp()},
  ));
  await assertSucceeds(updateDoc(
    doc(ownerDatabase, "lendingRequests", LENDING_REQUEST_ID),
    {status: "returned", updatedAt: serverTimestamp()},
  ));

  const ratingBatch = writeBatch(contactDatabase);
  ratingBatch.set(
    doc(
      contactDatabase,
      "lendingRatings",
      `${LENDING_REQUEST_ID}_${CONTACT_UID}`,
    ),
    {
      requestId: LENDING_REQUEST_ID,
      itemId: LENDING_ITEM_ID,
      raterUid: CONTACT_UID,
      recipientUid: OWNER_UID,
      score: 5,
      comment: "Helpful owner and item as described.",
      createdAt: serverTimestamp(),
    },
  );
  ratingBatch.update(
    doc(contactDatabase, "lendingRequests", LENDING_REQUEST_ID),
    {status: "rated", updatedAt: serverTimestamp()},
  );
  await assertSucceeds(ratingBatch.commit());
  await assertFails(updateDoc(
    doc(ownerDatabase, "lendingRatings", `${LENDING_REQUEST_ID}_${CONTACT_UID}`),
    {score: 1},
  ));
});

test("only the borrower creates the canonical lending chat thread", async () => {
  await seedLendingData();
  const contactDatabase = signedIn(CONTACT_UID);
  const ownerDatabase = signedIn(OWNER_UID);
  const data = threadData();
  data.contextType = "lending";
  data.contextId = LENDING_ITEM_ID;
  data.contextTitle = "Portable LED Lights";
  await assertSucceeds(setDoc(
    doc(contactDatabase, "chatThreads", LENDING_THREAD_ID),
    data,
  ));
  await assertFails(setDoc(
    doc(ownerDatabase, "chatThreads", `${LENDING_THREAD_ID}-forged`),
    data,
  ));
});
