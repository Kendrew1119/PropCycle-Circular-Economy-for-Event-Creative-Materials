# PropCycle lending setup and live test guide

This guide is for Phase 2E. It explains how to make the P2P Equipment Lending
module work with the real Firebase project.

Use simple test data only. Do not enter a home address, identity number, bank
detail, password, or private meeting place in a public lending item.

## 1. What is already in the app

The Android code already supports:

- create and edit a lending item;
- withdraw and relist an owner's item;
- one optional protected JPEG photo;
- a searchable real-time lending list;
- an optional map point rounded to two decimal places;
- manual title or area search without location permission;
- a borrowing date request of 1 to 31 inclusive days;
- owner approval or rejection;
- protection against two approved requests using the same item date;
- pickup confirmation, return report, return confirmation, and rating;
- participant-only lending chat;
- lending updates in the existing Notifications page.

PropCycle does not collect a lending fee or deposit. An optional deposit is
only information. The users arrange it outside the app after they agree in
chat.

## 2. What the Firebase owner must do

The Firebase project owner must complete these steps once:

1. Enable Email/Password in Firebase Authentication.
2. Create Cloud Firestore in the correct project.
3. Enable the default Cloud Storage bucket.
4. Deploy the reviewed `firestore.rules`, `firestore.indexes.json`, and
   `storage.rules` files from this repository.
5. Add approved teammates to the Firebase project with the smallest suitable
   role.

Every teammate must download their own `google-services.json`. Do not send this
file in a public message. Do not commit it to Git, even when the GitHub
repository is private.

## 3. Check the Android Firebase file

For each teammate:

1. Open <https://console.firebase.google.com/>.
2. Open project **propcycle-e5f14**.
3. Click the gear icon, then **Project settings**.
4. In **Your apps**, choose the Android app with package
   `com.propcycle.app`.
5. Download a fresh `google-services.json`.
6. Put it at this exact local path:

```text
mobileapp\app\google-services.json
```

7. Do not rename the file.
8. Run this check from the repository root:

```powershell
git check-ignore -v app/google-services.json
```

The command must show that Git ignores the file. If it prints nothing, stop and
fix `.gitignore` before continuing.

## 4. Enable Firebase services

### 4.1 Email and password login

1. In Firebase Console, open **Authentication**.
2. Open **Sign-in method**.
3. Select **Email/Password**.
4. Turn on the first Email/Password option.
5. Leave passwordless email link off unless the team plans it separately.
6. Click **Save**.

### 4.2 Cloud Firestore

1. Open **Firestore Database**.
2. Confirm the database exists in the intended project.
3. Confirm it is not left with open test-mode rules.
4. Do not create lending collections manually. The app creates the documents
   after the reviewed rules are deployed.

### 4.3 Cloud Storage

1. Open **Storage** in Firebase Console.
2. Click **Get started** if Storage is not enabled.
3. Use the default bucket for this Firebase project.
4. Billing may be required by Firebase. Review the displayed cost information
   before continuing.
5. Download a fresh `google-services.json` after the bucket is available.

The lending module stores a photo only under this protected shape:

```text
lending/{ownerUid}/{itemId}/primary_{version}.jpg
```

Only the owner can upload, replace, or delete the image. A signed-in user can
read it for the lending screen. The rules reject a wrong type, wrong metadata,
wrong path, or a file larger than 4 MiB.

## 5. Deploy the reviewed rules

Do this from the repository root, not from the `app` folder.

### 5.1 Use Android Studio's Java 21 for Firebase tools

The Android app source remains Java 17. Current Firebase Emulator and command
tools need Java 21 on this PC. Android Studio already includes it.

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
java -version
```

The output should start with Java 21.

### 5.2 Sign in and select the project

```powershell
.\firebase-tests\node_modules\.bin\firebase.cmd login
.\firebase-tests\node_modules\.bin\firebase.cmd use propcycle-e5f14
```

Check the project name carefully. Never deploy these rules to another team
project by accident.

### 5.3 Run local Rules tests first

```powershell
Set-Location .\firebase-tests
npm install
npm run test:rules
Set-Location ..
```

All tests must pass. The test uses the local Firebase Emulator Suite. It does
not write to the real Firebase project.

### 5.4 Deploy

```powershell
.\firebase-tests\node_modules\.bin\firebase.cmd deploy --only firestore:rules,firestore:indexes,storage
```

Wait for **Deploy complete**. Then check the Rules tabs in Firestore and
Storage. The console should show the new lending paths and the deployment time.

Local tests are not proof of production deployment. Record the deployment time
and the Firebase project name for the team report.

## 6. Optional lending map setup

The lending list and manual search work without a Maps key. The map panel needs
the same restricted Android Maps key used by the Recycling Centre phase.

Each developer keeps the key only in ignored `secrets.properties`:

```properties
MAPS_API_KEY=replace_with_your_restricted_android_key
```

Follow `RECYCLE_MAP_SETUP.md` for billing, Maps SDK enablement, Android package
restriction, SHA-1 restriction, and API restriction. Never put a real Maps key
in Java, XML, `local.defaults.properties`, a screenshot, or Git.

The lending page does not need Places search. It maps only the approximate
points that item owners chose to publish.

## 7. Build the app

Open the repository root in Android Studio. Do not open only the `app` folder.

For a command-line debug build:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

The APK is created here:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## 8. Prepare two test accounts

Use two different email accounts made only for testing:

- Account A is the item owner.
- Account B is the borrower.

The strongest live check uses two Android devices. If only one device is
available, sign out fully before changing accounts and record that the
two-device check is still pending.

Do not share passwords between teammates. Do not create real financial or
identity data for this test.

## 9. Complete live test

### 9.1 Owner publishes an item

On Device A:

1. Sign in as Account A.
2. Open **Lend Out**.
3. Tap **Lend an item**.
4. Enter a title, description, category, condition, maximum days, pickup
   method, and a public area such as `Petaling Jaya`.
5. Leave the deposit empty first. Confirm the app does not require payment.
6. Choose one test photo. Confirm the photo prepares before the item is saved.
7. Optional: tap **Attach current area**.
8. Allow approximate location only. Confirm the app accepts it.
9. Publish the item.
10. Confirm the item appears in the list.
11. If the Maps key is set, confirm the approximate marker appears.

Repeat once with location permission denied. The owner must still be able to
publish using the typed area only.

### 9.2 Borrower browses and chats

On Device B:

1. Sign in as Account B.
2. Open **Lend Out**.
3. Search by part of the title.
4. Search by the typed area.
5. Open the item detail.
6. Confirm the optional deposit is clearly labelled as outside PropCycle.
7. Tap **Chat with owner**.
8. Send a short test message.
9. Confirm only Account A and Account B can read the conversation.

### 9.3 Borrower sends a date request

On Device B:

1. Open the item detail.
2. Tap the borrowing request button.
3. Choose a valid start and end date.
4. Confirm a same-day request is allowed.
5. Confirm a request over the item's maximum days is rejected.
6. Send a valid request.
7. Open **Notifications** and confirm the request is shown.

### 9.4 Owner approves and collision protection works

On Device A:

1. Open **Notifications**.
2. Confirm the pending request is shown.
3. Approve it.
4. Create another request for the same item and overlapping date using another
   borrower test account if available.
5. Try to approve the overlapping request.
6. Confirm Firebase rejects the second approval because one or more dates are
   already booked.

Also create a different request and test **Reject**.

### 9.5 Pickup, return, and rating

1. On Device A, confirm the approved request was picked up.
2. On Device B, open Notifications and report the item as returned.
3. On Device A, confirm the returned item was received.
4. On Device B, give a score from 1 to 5 and an optional short comment.
5. Reopen the item detail and confirm the owner trust summary updates.
6. Confirm the same completed request cannot create a second rating.

### 9.6 Owner management

On Device A:

1. Open the item detail.
2. Edit the title or description and save.
3. Replace the photo and confirm the new photo appears.
4. Withdraw the item.
5. Confirm Device B can no longer find it in public browse.
6. Relist it.
7. Confirm it becomes visible again.

## 10. Security checks

Confirm these behaviours with the two accounts:

- Account B cannot edit, withdraw, or relist Account A's item.
- Account A cannot borrow its own item.
- A third account cannot read a request document or conversation that it does
  not participate in.
- A borrower cannot approve a request.
- An owner cannot report a return for the borrower.
- A rating is allowed only after the owner confirms the return.
- Anonymous users cannot read a lending photo.
- The app never shows a payment-success message.

## 11. What remains intentionally unavailable

Phase 2E does not include:

- payment or deposit collection;
- required rental fees;
- routes, directions, or travel time;
- background location or tracking;
- precise private addresses;
- push notifications outside the app;
- identity verification, insurance, penalties, or moderation backend;
- multiple lending photos;
- automatic reminders or scheduled server actions.

## 12. Troubleshooting

### The app says Firebase setup is required

- Check that `app\google-services.json` exists.
- Check that its package name is `com.propcycle.app`.
- Sync Gradle and rebuild the `app` configuration.

### Permission denied after saving or approving

- Confirm the newest `firestore.rules` and `storage.rules` were deployed to
  `propcycle-e5f14`.
- Confirm the user is signed in.
- Confirm the item belongs to that user.
- Confirm the request is still in the expected state.

### Photo upload fails

- Confirm Firebase Storage is enabled.
- Confirm the Storage Rules were deployed.
- Choose a JPEG smaller than 4 MiB.
- Check the internet connection.

### Map setup is required

- The list still works. Search by title or area.
- For the map, follow `RECYCLE_MAP_SETUP.md` and use a restricted Android key in
  ignored `secrets.properties`.

### Firebase tools say Java 21 is required

Run the Java 21 commands in Step 5.1. This changes Java only in the current
PowerShell window. The Android application code remains Java 17.

## 13. Evidence checklist

Record these items before marking the live phase complete:

- [ ] Firebase project name checked before deployment.
- [ ] Firestore Rules deployment time recorded.
- [ ] Storage Rules deployment time recorded.
- [ ] Two signed-in accounts used.
- [ ] Create, edit, withdraw, and relist passed.
- [ ] Photo upload, display, replacement, and owner-only cleanup passed.
- [ ] Manual search passed without location permission.
- [ ] Approximate map point passed with the restricted key.
- [ ] Request, approve, reject, cancel, and collision checks passed.
- [ ] Pickup, return report, return confirmation, and rating passed.
- [ ] Lending chat remained participant-only.
- [ ] No secret file or key was committed.

Until these live checks are recorded, report Phase 2E as **app-side and local
verification complete; production deployment and real-device checks pending**.
