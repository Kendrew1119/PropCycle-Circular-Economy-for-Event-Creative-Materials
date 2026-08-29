# Built-in demo image guide

PropCycle includes 12 sample item illustrations inside the Android app. These
samples make Marketplace and Lending demonstrations possible without enabling
Firebase Cloud Storage or paying for photo storage.

## What works without Cloud Storage

You can choose a built-in sample for:

- a new Marketplace listing;
- an edited Marketplace listing;
- a new Lending item; and
- an edited Lending item.

The available samples are cardboard box, plastic bottles, metal cans, fabric
rolls, wooden pallet, craft materials, event banner, fairy lights, folding
chairs, speakers, display stand, and storage crates.

The illustration files are packaged inside the APK. Firestore stores only a
small name such as `folding_chairs` in `demoImageKey`. It does not upload an
image file and does not use Firebase Storage space.

Every teammate who installs the same app version has the same illustrations.
When one user publishes a listing with a sample, another user with the same or
a newer app version can see that sample after Firestore loads the listing.

## Setup needed

The built-in images do not need a Storage bucket, a Storage billing upgrade, or
a Storage rule deployment.

The listing itself still uses Firebase Authentication and Cloud Firestore.
Complete these steps:

1. Put your own ignored `google-services.json` in the `app` folder.
2. Sign in to a test account in PropCycle.
3. Make sure a matching `/users/{uid}` profile exists. Normal registration in
   the app creates this profile.
4. Ask the Firebase maintainer to deploy the current `firestore.rules`. The
   current Rules allow only the 12 approved `demoImageKey` values.
5. Build and install the app normally.

Do not commit `google-services.json`, API keys, passwords, or service-account
files. The sample illustrations themselves are safe to keep in Git because
they are application assets, not secrets.

## Marketplace demonstration

1. Sign in.
2. Open **Market**.
3. Open **Create listing**, or edit a listing that you own.
4. Press **Add photo**.
5. Press **Choose built-in demo image**.
6. Choose one of the 12 samples.
7. Check that the sample appears in the preview.
8. Complete the required listing fields.
9. Press **Publish** or **Save changes**.
10. Open the Market list and listing detail page. Check that the same sample
    appears in both places.

## Lending demonstration

1. Sign in.
2. Open **Lend Resource**.
3. Press the image action.
4. Press **Choose built-in demo image**.
5. Choose one of the 12 samples.
6. Complete the title, description, category, condition, pickup method, area,
   and maximum borrowing days.
7. Press **Publish lending item** or **Save changes**.
8. Open the Lending list, map item list, and detail page. Check that the same
   sample appears.

## Other image choices

Marketplace keeps three choices:

- built-in demo image;
- camera photo; and
- device photo.

Lending keeps two choices:

- built-in demo image; and
- device photo.

A camera or device photo still needs Firebase Cloud Storage when the item is
saved. If Storage is not enabled, use a built-in demo image. Selecting a sample
replaces the pending personal photo, and selecting a personal photo replaces
the sample. PropCycle never saves both image types on one item.

## Remove or replace a sample

Open the owner edit form. You can choose another sample, choose a personal
photo, or clear the current image. The change is applied only after you press
**Save changes**.

## Checks before a classroom demo

- Use two test accounts.
- Publish one Marketplace listing and one Lending item with different samples.
- Confirm both accounts can see the correct samples.
- Confirm a user who is not the owner cannot edit the item.
- Turn off the network after the content has loaded. Do not claim a new cloud
  save succeeded while offline.
- Keep personal photos out of the test if Storage is not enabled.
- Do not claim that the built-in illustration is a real photo uploaded by the
  user. It is prepared demo content.

## If Firestore says permission denied

The most likely cause is that the current `firestore.rules` were not deployed.
The Firebase maintainer should first run the local Rules tests. After review,
deploy only the Firestore Rules and indexes to the correct project. Cloud
Storage does not need to be enabled for this demo-image path.
