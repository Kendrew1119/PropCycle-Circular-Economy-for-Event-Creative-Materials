# PropCycle Firebase setup - simple step-by-step guide

This guide is for Firebase project `propcycle-e5f14` and Android package
`com.propcycle.app`.

## What is already finished

The following items are already correct on this PC:

- `app/google-services.json` exists.
- The JSON package name is `com.propcycle.app`.
- The JSON project ID is `propcycle-e5f14`.
- The current local JSON includes Storage bucket
  `propcycle-e5f14.firebasestorage.app`.
- The JSON file is ignored by Git.
- Cloud Firestore was created in Singapore.
- The project uses Groovy Gradle files, not Kotlin Gradle files.
- The Google services Gradle plugin is version `4.5.0`.
- The Firebase Android BoM is version `34.17.0`.
- The app includes Firebase Authentication and Cloud Firestore.
- Firestore rules and indexes are ready in this repository.
- Phase 2A app-side code and local checks are complete.
- Phase 2B scanner dependencies include Firebase AI Logic, CameraX, Android
  Photo Picker, and build-specific App Check providers.
- Phase 2C.1 uses the existing Authentication and Firestore setup for owner-only
  listing edits, withdrawal, and relisting. It does not need another API key.
- Phase 2C.2 adds Firebase Cloud Storage for one optional marketplace photo.
  Its Android code, Firestore/Storage Rules, and local Rules tests are included.
- Phase 2C.2 app/local checks are complete; deployment and live image checks are
  still owner tasks.
- Phase 2E adds lending items, participant-private requests, booked-day locks,
  lending chat, return/rating actions, and one optional protected lending
  image. Its app/local checks are complete; production deployment and the
  two-account/two-device checks remain owner tasks.

Do not add `firebase-analytics`. Firebase shows Analytics as an example, but
PropCycle does not use Analytics in Phase 2A or Phase 2B.

## What you need to do next

Complete Steps 1 and 2 below for the Phase 2A cloud owner checks. The app-side
code and local tests passing does not prove that production Rules/indexes were
deployed or that the two-account live smoke test passed.

For the Phase 2B scanner, also complete the AI Logic and App Check owner steps
in [AI_SCANNER_SETUP.md](AI_SCANNER_SETUP.md). Do not create or paste a raw
Gemini API key.

For Phase 2C.2, the Firebase owner must also enable the default Storage bucket,
download a fresh `google-services.json`, and deploy Storage Rules. Follow the
complete [Marketplace image setup guide](MARKETPLACE_IMAGE_SETUP.md).

For Phase 2E, deploy the reviewed Firestore and Storage Rules again, then follow
the complete [P2P lending setup guide](LENDING_SETUP.md). It explains the item,
request, approval/date-lock, chat, return, rating, privacy, and live security
checks in simple steps.

## Step 1 - Enable email and password login

1. Open <https://console.firebase.google.com/>.
2. Open project **propcycle-e5f14**.
3. In the left menu, open **Authentication**. It may be under **Build** or
   **Security**.
4. If you see a **Get started** button, click it.
5. Open the **Sign-in method** tab.
6. Click **Email/Password**.
7. Turn on the first **Email/Password** switch.
8. Leave **Email link (passwordless sign-in)** off.
9. Click **Save**.

You do not need to create users in the Firebase console. The Register screen in
the app will create test users.

## Step 2 - Upload the Firestore rules and indexes

This step protects the database. Do not leave Firestore in test mode.

### 2.1 Open PowerShell

Open PowerShell in this folder:

```text
C:\Users\B2B\Desktop\mobileapp
```

In File Explorer, open the folder, click the address bar, type `powershell`, and
press Enter.

### 2.2 Check the Firebase command

The Firebase command is already installed in this project's test-tools folder on
this PC. Check it with:

```powershell
.\firebase-tests\node_modules\.bin\firebase.cmd --version
```

If PowerShell prints a version number, continue to Step 2.3.

Only if PowerShell says the file does not exist, run:

```powershell
Set-Location firebase-tests
npm ci
Set-Location ..
```

`firebase-tests` contains only Firebase Security Rules test tools. It is not a
React Native or Expo application.

### 2.3 Sign in to Firebase

This PC has an old saved Firebase login, but its access token has expired. Run:

```powershell
.\firebase-tests\node_modules\.bin\firebase.cmd login --reauth
```

A browser window will open. Sign in with the Google account that owns or can edit
project `propcycle-e5f14`.

When the browser says login is successful, return to PowerShell and continue to
the next command.

### 2.4 Deploy the rules and indexes

Run:

```powershell
.\firebase-tests\node_modules\.bin\firebase.cmd deploy --only firestore,storage --project propcycle-e5f14
```

This command uploads these reviewed files:

- `firestore.rules`
- `firestore.indexes.json`
- `storage.rules`

Wait until PowerShell shows **Deploy complete**.

### 2.5 Check the Firebase console

1. Return to the Firebase console.
2. Open **Firestore Database**.
3. Open the **Rules** tab. The rules should no longer be the temporary test-mode
   rules.
4. Open the **Indexes** tab.
5. Wait until both composite indexes show **Enabled**. An index may show
   **Building** for several minutes.
6. Open **Storage > Rules** and confirm the marketplace image rules are present.

The two required indexes are:

- `marketplaceListings`: `status` ascending and `createdAt` descending.
- `chatThreads`: `participantIds` array-contains and `updatedAt` descending.

Do not manually create Firestore collections. The app creates them when you
register, publish a listing, and start a chat.

## Phase 2B owner step - enable AI Logic and App Check

This is a different shared-service setup from Firestore deployment.

1. Open Firebase project `propcycle-e5f14`.
2. Open **AI Services > AI Logic > Get started**.
3. Choose the **Gemini Developer API**.
4. Complete the guided setup.
5. Open **AI Services > AI Logic > Settings**.
6. Under **Authenticated-users mode**, choose **Enforce authenticated-users
   mode**, then press **Confirm**.
7. Run a debug app and find the device token in Logcat by filtering
   `DebugAppCheckProvider`.
8. In Firebase Console, open **Security > App Check > Apps >
   com.propcycle.app > Manage debug tokens**.
9. Add that developer's token with a clear device name.
10. Open the App Check APIs/Products view and verify the Firebase AI Logic row.
11. Test one live scanner request, check valid request metrics, and review
     current quota/budget alerts.

The AI Logic guided setup automatically enables App Check enforcement for AI
Logic. A developer's first debug request can be rejected until that developer
registers their debug token.

Each developer registers their own token. No token is committed or shared.
Firebase AI Logic is not emulated, so a real AI request is a deliberate manual
test. Debug builds use the debug provider only. Release builds use Play
Integrity only and still require release SHA-256/provider/signed-APK owner
setup later.

The complete click-by-click steps, camera/emulator setup, privacy notes, manual
test checklist, release gate, and troubleshooting are in
[AI_SCANNER_SETUP.md](AI_SCANNER_SETUP.md).

## Step 3 - Build the connected app

From the project root, run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

A successful build ends with:

```text
BUILD SUCCESSFUL
```

The APK will be here:

```text
app\build\outputs\apk\debug\app-debug.apk
```

You can also open the project in Android Studio, select the `app` configuration,
and press Run.

## Step 4 - Test registration and login

1. Open PropCycle.
2. Press **Go!**.
3. Press **Register**.
4. Enter a display name, a test email, and a password.
5. Submit the form.
6. The app should open Home.
7. In the Firebase console, open **Authentication** and then **Users**. The new
   email should appear.
8. Open **Firestore Database** and then **Data**. A `users` collection should
   appear with one profile document.
9. To log out from Home, press the hamburger button and choose **Market** from
   the fan. On the Market screen, press its hamburger button and choose
   **Log out**.
10. Sign in again with the same email and password.

Use test email accounts. Do not use a real personal password.

## Step 5 - Test the marketplace

While signed in as Account A:

1. Open **Market** from the Home hamburger fan or from another screen's review
   menu.
2. Press the hamburger button (the three horizontal lines), then press
   **Create listing**.
3. Enter a title and the required listing details.
4. Publish the listing.
5. Return to Market. The listing should appear.
6. In Firebase Console > Firestore Database > Data, confirm that the
   `marketplaceListings` collection exists.

Phase 2C.2 can add one optional listing photo after the Firebase owner completes
the [Storage setup](MARKETPLACE_IMAGE_SETUP.md). A text-only listing remains
valid when no photo is chosen.

### 5A - Test owner edit, withdraw, and relist

Keep Account A signed in and keep the listing detail page open:

1. Confirm **Manage your listing** appears. A different user must never see
   these owner buttons.
2. Press **Edit details**.
3. Change the title, description, category, condition, transaction type,
   fulfilment method, and the matching price or exchange terms.
4. Press **Save changes**. The detail page should show the new values.
5. In Firestore, confirm `ownerId` and `createdAt` did not change. `imageUrl`
   changes only when the owner chose a valid replacement photo. `updatedAt`
   should have a newer server time.
6. Press **Withdraw**, read the confirmation, and confirm it.
7. The detail page should show **Withdrawn** and the button should change to
   **Relist**.
8. Log in as Account B. Account A's withdrawn listing must not appear in Market.
   Account B must not be able to open it from an old direct link or start a new
   chat for it.
9. Return to Account A on the still-open detail page and press **Relist**.
10. Return to Account B. Refresh Market and confirm the listing appears again.

For the conflict check, open Account A's edit page on two devices. Save a change
on Device 1, then try to save the older form on Device 2. Device 2 must ask the
user to reopen the listing; it must not overwrite Device 1 silently.

Existing chats are kept when a listing is withdrawn. Only creation of a new chat
is blocked.

## Step 6 - Test chat with two accounts

Chat needs two different users because an owner cannot chat with their own listing.

1. Keep Account A's marketplace listing.
2. Return to the Market screen, open its hamburger menu, and log out from
   Account A.
3. Register Account B with a different email.
4. Open Market and open Account A's listing.
5. Press **Chat**.
6. Send a short message.
7. In Firestore, confirm that these collections now exist:
   - `chatThreads`
   - `chatThreads/{threadId}/messages`
8. Return to the Market screen, open its hamburger menu, and log out from
   Account B.
9. Sign in as Account A.
10. From Home, open **Market** from the fan. Open the Market hamburger menu,
    press **Messages**, open the conversation, and reply.

Only Account A and Account B should be able to open that conversation.

## Common errors

### The app says Firebase setup is required

- Check that the filename is exactly `google-services.json`.
- Check that it is inside the `app` folder, not the project root.
- In Android Studio, click **Build > Clean Project**, then run the app again.

### Registration says the provider is disabled

Complete Step 1 and enable Email/Password Authentication.

### The app shows PERMISSION_DENIED

Complete Step 2. The repository rules have not been deployed, or the Firebase
console still has different rules.

### The app shows FAILED_PRECONDITION or asks for an index

Open Firebase Console > Firestore Database > Indexes. Wait until both indexes are
Enabled. If they are missing, repeat the deploy command in Step 2.4.

### PowerShell says firebase.cmd does not exist

Run:

```powershell
Set-Location firebase-tests
npm ci
Set-Location ..
```

Then repeat Step 2.3.

### Firebase login uses the wrong Google account

Run:

```powershell
.\firebase-tests\node_modules\.bin\firebase.cmd logout
.\firebase-tests\node_modules\.bin\firebase.cmd login
```

Choose the account that has access to `propcycle-e5f14`.

## Features that are still deferred

These services are not needed now and should remain disabled:

- Firebase Analytics
- Google Maps, Places, and location
- recycling-centre APIs
- permanent scan history, Room, and WorkManager
- Firebase Remote Config
- Push notifications
- Lending booking, return, and rating logic

Cloud Storage is enabled only for the narrow Phase 2C.2 marketplace path. It
does not authorise avatars, lending images, scan history, multiple listing
images, or any item above.

Official Firebase references:

- [Add Firebase to Android](https://firebase.google.com/docs/android/setup)
- [Email/password Authentication](https://firebase.google.com/docs/auth/android/password-auth)
- [Deploy Firestore Security Rules](https://firebase.google.com/docs/firestore/security/get-started)
- [Firebase CLI](https://firebase.google.com/docs/cli)
- [Firebase AI Logic Android setup](https://firebase.google.com/docs/ai-logic/get-started?platform=android)
- [Firebase App Check debug provider](https://firebase.google.com/docs/app-check/android/debug-provider)
- [Cloud Storage Android setup](https://firebase.google.com/docs/storage/android/start)
- [Cloud Storage Android uploads](https://firebase.google.com/docs/storage/android/upload-files)
- [Cloud Storage Security Rules](https://firebase.google.com/docs/storage/security)
