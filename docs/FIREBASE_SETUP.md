# PropCycle Firebase setup - simple step-by-step guide

This guide is for Firebase project `propcycle-e5f14` and Android package
`com.propcycle.app`.

## What is already finished

The following items are already correct on this PC:

- `app/google-services.json` exists.
- The JSON package name is `com.propcycle.app`.
- The JSON project ID is `propcycle-e5f14`.
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

Do not add `firebase-analytics`. Firebase shows Analytics as an example, but
PropCycle does not use Analytics in Phase 2A or Phase 2B.

## What you need to do next

Complete Steps 1 and 2 below for the Phase 2A cloud owner checks. The app-side
code and local tests passing does not prove that production Rules/indexes were
deployed or that the two-account live smoke test passed.

For the Phase 2B scanner, also complete the AI Logic and App Check owner steps
in [AI_SCANNER_SETUP.md](AI_SCANNER_SETUP.md). Do not create or paste a raw
Gemini API key.

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
.\firebase-tests\node_modules\.bin\firebase.cmd deploy --only firestore --project propcycle-e5f14
```

This command uploads both files:

- `firestore.rules`
- `firestore.indexes.json`

Wait until PowerShell shows **Deploy complete**.

### 2.5 Check the Firebase console

1. Return to the Firebase console.
2. Open **Firestore Database**.
3. Open the **Rules** tab. The rules should no longer be the temporary test-mode
   rules.
4. Open the **Indexes** tab.
5. Wait until both composite indexes show **Enabled**. An index may show
   **Building** for several minutes.

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

Listing pictures are still placeholders. Cloud Storage and photo upload are not
enabled yet.

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
- Cloud Storage and image upload
- Google Maps, Places, and location
- recycling-centre APIs
- permanent scan history, Room, and WorkManager
- Firebase Remote Config
- Push notifications
- Lending booking, return, and rating logic

The narrow CameraX/Photo Picker/Firebase AI Logic scanner is now Phase 2B. It
does not authorise any item above.

Official Firebase references:

- [Add Firebase to Android](https://firebase.google.com/docs/android/setup)
- [Email/password Authentication](https://firebase.google.com/docs/auth/android/password-auth)
- [Deploy Firestore Security Rules](https://firebase.google.com/docs/firestore/security/get-started)
- [Firebase CLI](https://firebase.google.com/docs/cli)
- [Firebase AI Logic Android setup](https://firebase.google.com/docs/ai-logic/get-started?platform=android)
- [Firebase App Check debug provider](https://firebase.google.com/docs/app-check/android/debug-provider)
