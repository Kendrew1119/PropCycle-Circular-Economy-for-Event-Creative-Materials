import {after, before, beforeEach, test} from "node:test";
import {readFileSync} from "node:fs";
import assert from "node:assert/strict";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  deleteObject,
  getBytes,
  ref,
  uploadBytes,
} from "firebase/storage";

const PROJECT_ID = "demo-propcycle";
const OWNER_UID = "owner-user";
const CONTACT_UID = "contact-user";
const LISTING_ID = "listing-one";
const IMAGE_PATH =
  `marketplace/${OWNER_UID}/${LISTING_ID}/primary_version-one.jpg`;
const LENDING_ITEM_ID = "lending-item-one";
const LENDING_IMAGE_PATH =
  `lending/${OWNER_UID}/${LENDING_ITEM_ID}/primary_version-one.jpg`;
const JPEG_BYTES = new Uint8Array([0xff, 0xd8, 0xff, 0xd9]);

let testEnvironment;

before(async () => {
  testEnvironment = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    storage: {
      host: "127.0.0.1",
      port: 9199,
      rules: readFileSync(new URL("../storage.rules", import.meta.url), "utf8"),
    },
  });
});

beforeEach(async () => {
  await testEnvironment.clearStorage();
});

after(async () => {
  await testEnvironment.cleanup();
});

function signedIn(uid) {
  return testEnvironment.authenticatedContext(uid).storage();
}

function metadata(
  ownerId = OWNER_UID,
  listingId = LISTING_ID,
  contentType = "image/jpeg",
) {
  return {
    contentType,
    customMetadata: {
      ownerId,
      listingId,
      kind: "marketplace-primary",
    },
  };
}

function lendingMetadata(ownerId = OWNER_UID, itemId = LENDING_ITEM_ID) {
  return {
    contentType: "image/jpeg",
    customMetadata: {
      ownerId,
      itemId,
      kind: "lending-primary",
    },
  };
}

async function seedImage() {
  await assertSucceeds(uploadBytes(
    ref(signedIn(OWNER_UID), IMAGE_PATH),
    JPEG_BYTES,
    metadata(),
  ));
}

test("owner uploads a bounded JPEG and another signed-in user can read it", async () => {
  await seedImage();
  const downloaded = await assertSucceeds(getBytes(
    ref(signedIn(CONTACT_UID), IMAGE_PATH),
    1024,
  ));
  assert.equal(downloaded.byteLength, JPEG_BYTES.byteLength);
});

test("anonymous users cannot read marketplace images", async () => {
  await seedImage();
  const anonymousStorage = testEnvironment.unauthenticatedContext().storage();
  await assertFails(getBytes(ref(anonymousStorage, IMAGE_PATH), 1024));
});

test("non-owners cannot upload, replace, or delete an owner's image", async () => {
  const contactStorage = signedIn(CONTACT_UID);
  await assertFails(uploadBytes(
    ref(contactStorage, IMAGE_PATH),
    JPEG_BYTES,
    metadata(),
  ));
  await seedImage();
  await assertFails(uploadBytes(
    ref(contactStorage, IMAGE_PATH),
    JPEG_BYTES,
    metadata(),
  ));
  await assertFails(deleteObject(ref(contactStorage, IMAGE_PATH)));
});

test("owner can remove an old version after a successful replacement", async () => {
  await seedImage();
  await assertSucceeds(deleteObject(ref(signedIn(OWNER_UID), IMAGE_PATH)));
});

test("uploads reject wrong type, metadata, filename, path, and excessive size", async () => {
  const ownerStorage = signedIn(OWNER_UID);
  await assertFails(uploadBytes(
    ref(ownerStorage, IMAGE_PATH),
    JPEG_BYTES,
    metadata(OWNER_UID, LISTING_ID, "image/png"),
  ));
  await assertFails(uploadBytes(
    ref(ownerStorage, IMAGE_PATH),
    JPEG_BYTES,
    metadata(CONTACT_UID),
  ));
  await assertFails(uploadBytes(
    ref(ownerStorage, `marketplace/${OWNER_UID}/${LISTING_ID}/primary.jpg`),
    JPEG_BYTES,
    metadata(),
  ));
  await assertFails(uploadBytes(
    ref(ownerStorage, `avatars/${OWNER_UID}/profile.jpg`),
    JPEG_BYTES,
    metadata(),
  ));
  await assertFails(uploadBytes(
    ref(ownerStorage, IMAGE_PATH),
    new Uint8Array(4 * 1024 * 1024 + 1),
    metadata(),
  ));
});

test("lending images are owner-written and authenticated-readable", async () => {
  const ownerStorage = signedIn(OWNER_UID);
  const contactStorage = signedIn(CONTACT_UID);
  await assertSucceeds(uploadBytes(
    ref(ownerStorage, LENDING_IMAGE_PATH),
    JPEG_BYTES,
    lendingMetadata(),
  ));
  const downloaded = await assertSucceeds(getBytes(
    ref(contactStorage, LENDING_IMAGE_PATH),
    1024,
  ));
  assert.equal(downloaded.byteLength, JPEG_BYTES.byteLength);
  await assertFails(uploadBytes(
    ref(contactStorage, LENDING_IMAGE_PATH),
    JPEG_BYTES,
    lendingMetadata(),
  ));
  await assertFails(uploadBytes(
    ref(ownerStorage, LENDING_IMAGE_PATH),
    JPEG_BYTES,
    lendingMetadata(CONTACT_UID),
  ));
  await assertSucceeds(deleteObject(ref(ownerStorage, LENDING_IMAGE_PATH)));
});
