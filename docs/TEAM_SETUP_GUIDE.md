# PropCycle teammate setup and run guide

This guide explains how a teammate can clone the private repository, open the
correct Android project, connect to the shared development Firebase project,
build the app, run it, verify the implemented features, and find the detailed
Firebase, AI scanner, marketplace-image, and Recycling Centre map setup guides.

The instructions use Windows PowerShell because the current team machines use
Windows. Run commands from the repository root unless a step says otherwise.

## Start here: what is implemented

PropCycle is a native Android project. It is not a React Native or Expo project.

The project currently contains:

- Java 17 application and unit-test source;
- Android Views and XML layouts;
- one Gradle module named `app`;
- Firebase Authentication email/password accounts;
- Cloud Firestore user profiles and marketplace listings;
- listing-linked conversation discovery and participant-only real-time text
  chat;
- CameraX preview/capture and one-image Android Photo Picker input;
- authenticated Firebase AI Logic/Gemini structured analysis with build-specific
  App Check;
- one optional marketplace photo with Firebase Cloud Storage, authenticated
  display, and owner-only replacement;
- one functional Recycling Centre map/list with Places Text Search, one-time
  foreground location, and manual area fallback;
- Firestore and Storage Security Rules, composite indexes, and emulator tests; and
- the full twenty-screen proposal UI, with unrelated modules still using static
  content.

The currently approved visual direction uses the light-colour interface theme.
On Home, the hamburger control opens the three-destination fan for **Market**,
**Share**, and **Map**.

Phase 2A app-side code and local verification are complete. Production
Firestore Rules/index deployment and the two-account live cloud smoke test
remain deliberate owner checks.

Phase 2C.2 app-side code and local checks are also complete. The Firebase owner
still confirms billing/bucket setup, deploys the reviewed Storage and Firestore
Rules, and performs the documented real-device image checks.

Phase 2D app-side code and local checks are complete. The Google Cloud owner
still enables billing and the two required APIs, provides a properly restricted
Android key, and completes the real-device checklist in
`docs/RECYCLE_MAP_SETUP.md`.

Phase 2B adds only the existing Scanner and AI Result journey. Phase 2C.1 adds
marketplace owner editing/withdraw/relist, Phase 2C.2 adds one marketplace
photo, and Phase 2D makes only the Recycling Centre map/list functional. The
following integrations remain deliberately excluded: lending/marketplace maps,
routes, background location, multiple images, avatars, lending
images, permanent scan history, Room, Remote Config, WorkManager, push
notifications, lending transactions, ratings, and presence.

## Read this safety box before setup

Never commit or send any of these files through Git:

- `app/google-services.json`
- `.firebaserc`
- `local.properties`
- `secrets.properties`
- `.env` or any `.env.*` file
- a service-account JSON file
- a signing keystore or its passwords
- unrestricted API keys, App Check debug tokens, access tokens, or personal
  passwords

Do not create a service-account key for normal Android or Firebase CLI work.
Use your own Google account to sign in to Firebase CLI.

`google-services.json` is Android client configuration. This repository keeps it
local and ignored by policy. Each teammate must download their own copy from the
approved Firebase project and must not force-add it to Git.

## Who completes which steps

| Person | Required work |
|---|---|
| Every teammate | Obtain GitHub access, clone, install Android tools, open the repository root, obtain `google-services.json`, create their ignored restricted-key `secrets.properties`, build, run, and test their own changes. |
| Firebase/Google Cloud project owner | Add approved teammates, enable only the approved Firebase services, enable the Phase 2C.2 Storage bucket/billing, complete Firebase AI Logic setup, and enable/restrict the Phase 2D Maps/Places services. |
| Assigned Firebase maintainer | Run the Rules test suite and deliberately deploy reviewed Firestore Rules, Storage Rules, and indexes. |
| Every scanner developer | Register their own App Check debug token. Never share or commit it. |
| Nobody | Commit credentials, share a service account, restore Expo/React Native, or enable deferred services without approval. |

The Firebase console and Firestore database are shared. Do not deploy Rules,
delete users, or delete cloud data merely to test whether your local setup works.

## 1. Ask for access before cloning

GitHub access and Firebase access are separate.

Ask the repository owner for:

1. access to the private GitHub repository;
2. access to Firebase project ID `propcycle-e5f14`; and
3. confirmation that the registered Android package is `com.propcycle.app`.

Give the owner your GitHub username and the Google email address you will use for
Firebase. Do not send a GitHub token, Google password, or Firebase service
account.

If you only need to review the static UI, GitHub access is enough. Real login,
marketplace, chat, and AI scanner testing also needs the local Firebase
configuration from the approved project. A live scanner request additionally
needs AI Logic enabled and that developer's App Check debug token registered.
A live in-app Recycling Centre map/search additionally needs the ignored,
restricted Maps key described in `docs/RECYCLE_MAP_SETUP.md`.

## 2. Install the required desktop tools

### 2.1 Git

Install Git for Windows from <https://git-scm.com/download/win>.

Open a new PowerShell window and check it:

```powershell
git --version
```

GitHub may open a browser or Git Credential Manager when you clone. Sign in with
the GitHub account that the owner added to the private repository. Never put a
personal access token directly into the repository URL.

### 2.2 Android Studio

Install Android Studio Quail 2 (`2026.1.2`) or a newer stable Android Studio
release that officially supports Android Gradle Plugin 9.3.

The project is pinned to:

- Android Gradle Plugin `9.3.0`;
- Gradle Wrapper `9.5.1`;
- Java 17 source compatibility;
- Android API 36 for compile and target; and
- Android API 24 as the minimum supported device version.

Android Studio includes JetBrains Runtime (JBR). Use that bundled runtime for
Gradle. On the current standard Windows installation it is located at:

```text
C:\Program Files\Android\Android Studio\jbr
```

The bundled JBR can be Java 21 even though the app source is Java 17. That is
normal. The runtime that starts Gradle and the Java language level used to
compile the app are different settings.

Do not install a separate system Gradle. The checked-in `gradlew.bat` file always
selects the project's required Gradle version.

### 2.3 Android SDK Platform 36

In Android Studio:

1. Open **Tools > SDK Manager**.
2. Open **SDK Platforms**.
3. Select **Android SDK Platform 36**.
4. Open **SDK Tools**.
5. Select **Android SDK Build-Tools 36.0.0**, **Android SDK Platform-Tools**, and
   **Android SDK Command-line Tools (latest)**.
6. Select **Android Emulator** if you will use a virtual device.
7. Click **Apply**, accept the Android SDK licences, and wait for installation to
   finish.

Android Studio normally creates an ignored root `local.properties` containing
your SDK path. Do not copy another teammate's `local.properties` and do not
commit yours.

### 2.4 Node.js for Firebase tools only

Node.js is not used to build the Android app. It is used only inside
`firebase-tests` for Firebase CLI and Firestore Security Rules tests.

Install a currently supported Node.js release from <https://nodejs.org/>. The
locked Firebase CLI accepts Node 20 or newer. Then check:

```powershell
node --version
npm.cmd --version
```

Do not run `npm install` at the repository root. There is no React Native or Expo
application. All Node commands in this guide run inside `firebase-tests`.

## 3. Clone the private GitHub repository

Choose a normal development folder. Do not clone into Android Studio's
installation directory, the Android SDK, or another copy of PropCycle.

```powershell
Set-Location C:\Users\YOUR_WINDOWS_NAME\Desktop
git clone https://github.com/Kendrew1119/PropCycle-Circular-Economy-for-Event-Creative-Materials.git PropCycle
Set-Location .\PropCycle
git status
```

Replace `YOUR_WINDOWS_NAME` with your Windows account name, or choose another
folder you own.

If GitHub says **Repository not found**, the usual cause is missing private-repo
access or signing in with the wrong GitHub account. Ask the repository owner to
check your collaborator access. Do not request another teammate's credentials.

## 4. Confirm that PowerShell is at the real project root

This is the most important setup check.

The correct folder contains all four of these entries:

```text
settings.gradle
build.gradle
gradlew.bat
app\
```

Check them:

```powershell
@('settings.gradle', 'build.gradle', 'gradlew.bat', 'app') |
    ForEach-Object { "$_ = $(Test-Path -LiteralPath $_)" }
```

Every line must end in `True`.

The correct folder is the repository root, for example:

```text
C:\Users\YOUR_WINDOWS_NAME\Desktop\PropCycle
```

This folder is wrong:

```text
C:\Users\YOUR_WINDOWS_NAME\Desktop\PropCycle\app
```

`app` is a Gradle subproject. It is not a standalone Android Studio project.

## 5. Open the correct folder in Android Studio

1. Close any PropCycle project that Android Studio already has open.
2. At the Android Studio welcome screen, click **Open**.
3. Select the repository root that contains `settings.gradle` and
   `gradlew.bat`.
4. Do not select the `app` folder.
5. Trust the project when Android Studio asks.
6. Wait for Gradle Sync and indexing to finish.

In the Gradle tool window, the expected structure is one top-level project named
`PropCycle` with `app` below it. There must not be a separate top-level `app`
Gradle project.

### Set the Gradle JDK

Open:

**File > Settings > Build, Execution, Deployment > Build Tools > Gradle**

Set **Gradle JDK** to either:

- `GRADLE_LOCAL_JAVA_HOME`; or
- the JBR inside your Android Studio installation.

Do not select an old Java 8, 11, or 16 installation.

## 6. Run a baseline build before Firebase setup

The project intentionally compiles when `app/google-services.json` is absent.
Firebase screens will show a setup-required message, but the static proposal
screens remain reviewable. This baseline separates Android/Gradle problems from
Firebase configuration problems.

Open PowerShell at the repository root:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug
```

If Android Studio is installed elsewhere, change `JAVA_HOME` to its `jbr`
folder.

A successful build ends with `BUILD SUCCESSFUL`. The APK is written to:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## 7. Download the ignored Firebase Android configuration

Do this separately on every developer computer.

1. Open <https://console.firebase.google.com/>.
2. Select project **propcycle-e5f14**. Check the project ID, not only the display
   name.
3. Click the gear icon and open **Project settings**.
4. Open the **General** tab.
5. Under **Your apps**, select the Android app whose package name is
   `com.propcycle.app`.
6. Click **Download google-services.json**.
7. Move the downloaded file to this exact path inside your clone:

   ```text
   PropCycle\app\google-services.json
   ```

The filename must be exactly `google-services.json`. Windows or the browser may
download `google-services (1).json`; rename it before continuing. Do not place
the file beside the root `build.gradle`.

### Verify only the safe identity fields

From the repository root:

```powershell
$firebaseConfig = Get-Content .\app\google-services.json -Raw | ConvertFrom-Json
$firebaseConfig.project_info.project_id
$firebaseConfig.client.client_info.android_client_info.package_name
```

Expected output:

```text
propcycle-e5f14
com.propcycle.app
```

Do not print or paste the entire JSON into an issue, report, chat, or terminal
log.

Confirm that Git ignores the file:

```powershell
git check-ignore -v .\app\google-services.json
git status --short
```

The first command should identify `.gitignore`. The file must not appear as a
change that can be committed. Never use `git add -f` on it.

After adding the file, click **File > Sync Project with Gradle Files** in Android
Studio or run the debug build again.

## 8. Verify the shared Firebase console setup

These settings are shared by the whole team. They normally need to be completed
once by the project owner, not once per computer.

### Required Authentication setting

In Firebase Console:

1. Open **Authentication**.
2. Open **Sign-in method**.
3. Open **Email/Password**.
4. Enable the first **Email/Password** switch.
5. Leave **Email link (passwordless sign-in)** disabled for Phase 2A.
6. Save.

Do not manually create normal test users in the console. The app's Register
screen creates them.

Email-enumeration protection is recommended before testing with people outside
the development team. It is not a reason to delay local development.

### Required Firestore database

Open **Firestore Database** and confirm that the database already exists in the
approved Singapore location. Do not create a second database or a second
Firebase project for the same shared development environment.

Do not leave permissive test-mode Rules in place. The reviewed repository Rules
must be tested and deployed using the later steps in this guide.

Do not manually create collections. The application creates these paths when
the relevant actions succeed:

```text
users/{uid}
marketplaceListings/{listingId}
chatThreads/{threadId}
chatThreads/{threadId}/messages/{messageId}
```

### Required Phase 2B Firebase AI Logic and App Check setting

Firebase AI Logic is a shared live service. It has no Local Emulator Suite
emulator.

The Firebase owner must:

1. open project `propcycle-e5f14`;
2. open **AI Services > AI Logic > Get started**;
3. choose the **Gemini Developer API**;
4. complete the guided setup;
5. open **AI Services > AI Logic > Settings**;
6. under **Authenticated-users mode**, choose **Enforce authenticated-users
   mode**, then press **Confirm**;
7. open **Security > App Check** and verify the Firebase AI Logic API row; and
8. review quota and budget alerts.

The AI Logic guided setup automatically enables App Check enforcement for AI
Logic. A developer's first debug request can be rejected until that developer
registers their debug token.

Every developer then runs the debug app, finds their own token in Logcat by
filtering `DebugAppCheckProvider`, and registers it under **Security > App
Check > Apps > com.propcycle.app > Manage debug tokens**.

Do not create a raw Gemini API key. Do not commit or share an App Check debug
token. Debug builds use the debug provider; release builds use Play Integrity
and still need the owner's release SHA-256/provider/signed-APK checks.

Follow [AI_SCANNER_SETUP.md](AI_SCANNER_SETUP.md) for the complete simple
step-by-step procedure, camera/emulator setup, manual tests, quota/privacy
checks, release gate, and exact troubleshooting.

## 9. Install the repository-local Firebase tools

From the repository root:

```powershell
Set-Location .\firebase-tests
npm.cmd ci
Set-Location ..
.\firebase-tests\node_modules\.bin\firebase.cmd --version
```

Use `npm ci`, not `npm install`, because the repository includes a lockfile. The
generated `firebase-tests/node_modules` folder is ignored and must not be
committed.

The first installation or emulator run may download packages and the Firestore
emulator. It needs an internet connection. If installation fails halfway, check
the Node version and network, then run `npm.cmd ci` again. Do not copy another
developer's `node_modules` folder and do not bypass package install scripts.

## 10. Bind this clone and sign in to Firebase CLI

The committed `.firebaserc.example` contains only the approved, non-secret
development project ID. Copy it to the ignored local filename:

```powershell
Copy-Item .\.firebaserc.example .\.firebaserc
Get-Content .\.firebaserc
```

The output must identify `propcycle-e5f14` as `default`. Do not change it to a
personal Firebase project. `.firebaserc` is intentionally ignored; do not
force-add it to Git.

The example file does not grant Firebase access and does not replace
`app/google-services.json`. Every teammate still needs:

- their Google account added to Firebase project `propcycle-e5f14`; and
- their own downloaded `app/google-services.json` for `com.propcycle.app`.

Now sign in with your personal Google account. These commands do not need a
service account:

```powershell
.\firebase-tests\node_modules\.bin\firebase.cmd login
.\firebase-tests\node_modules\.bin\firebase.cmd projects:list
.\firebase-tests\node_modules\.bin\firebase.cmd use
```

`projects:list` must include `propcycle-e5f14`, and `use` must report that
project as active.

If the wrong Google account opens, use:

```powershell
.\firebase-tests\node_modules\.bin\firebase.cmd logout
.\firebase-tests\node_modules\.bin\firebase.cmd login --reauth
```

Do not continue to cloud deployment until `projects:list` shows
`propcycle-e5f14` and the project owner has authorised you to deploy.

## 11. Run Firestore and Storage Security Rules tests locally

Rules tests run against local Firestore and Storage emulators with a demo
project. They do not read or change cloud Firebase data, and they do not require
`google-services.json`.

Stop any Firebase emulators already using port 8080 or 9199, then run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
Set-Location .\firebase-tests
npm.cmd run test:rules
Set-Location ..
```

The command starts the Firestore and Storage emulators, runs both Node Rules
test files, prints the results, and shuts the emulators down. Do not deploy if
any test fails.

## 12. Deploy reviewed Firestore/Storage Rules and indexes

This is a shared cloud change. Only the assigned Firebase maintainer should run
it after peer review and a passing Rules suite.

From the repository root:

```powershell
.\firebase-tests\node_modules\.bin\firebase.cmd deploy --only firestore,storage --project propcycle-e5f14
```

The explicit project ID protects against deploying to a different active alias.
Wait for `Deploy complete`.

Then check Firebase Console:

1. Open **Firestore Database > Rules** and confirm the deployed Rules are no
   longer temporary test-mode Rules.
2. Open **Firestore Database > Indexes**.
3. Wait until both composite indexes show **Enabled**.
4. Open **Storage > Rules** and confirm the owner-only marketplace image rules
   are deployed.

The required indexes are:

- `marketplaceListings`: `status` ascending, then `createdAt` descending;
- `chatThreads`: `participantIds` array-contains, then `updatedAt` descending.

An index may show **Building** for several minutes. Queries that need it can fail
until its state is **Enabled**.

## 13. Create an Android Emulator

In Android Studio:

1. Open **Tools > Device Manager**.
2. Click **Add a new device** or **Create virtual device**.
3. Choose a normal phone profile, such as a Pixel.
4. Choose an API 36 Google APIs or Google Play system image.
5. Download the image if necessary.
6. Finish the AVD setup and start it.
7. Wait until the Android home screen is fully visible.

If the emulator cannot start, enable CPU virtualisation in the PC firmware and
Windows virtualisation support, then follow Android Studio's displayed
recommendation.

## 14. Create or check the Android run configuration

After a successful Gradle Sync, Android Studio normally creates an `app` run
configuration automatically.

1. In the top toolbar, select **app**.
2. Select the running emulator or connected phone.
3. Click **Run**.

If `app` is missing:

1. First confirm that you opened the repository root and that Gradle Sync
   succeeded.
2. Open **Run > Edit Configurations**.
3. Click **+** and choose **Android App**.
4. Name it `app`.
5. Select the `app` module.
6. Set launch to **Default Activity**.
7. Save and run.

If the module selector is empty, do not invent a module or add a second Gradle
project. Fix the project-root or Gradle Sync error first.

## 15. Run on a physical Android phone

The app supports Android 7.0/API 24 and newer.

1. Enable Developer options on the phone.
2. Enable USB debugging.
3. Connect the phone with a data-capable USB cable.
4. Accept the debugging-authorisation prompt on the phone.
5. Install the manufacturer's Windows USB driver if Device Manager still does
   not show the phone.
6. Select the phone in Android Studio and run the `app` configuration.

Use the shared cloud development Firebase project for physical-device testing.
The optional local Firebase mode described next uses the Android Emulator's
special `10.0.2.2` host address and is not configured for a physical phone.

## 16. Optional: run the app with local Firebase emulators

Use this when you want disposable local Auth and Firestore data. It does not
replace the Rules test command and it does not deploy anything to the cloud.

Requirements:

- `app/google-services.json` is still required to supply the Android Firebase
  app identity;
- use a standard Android Emulator, not a physical device;
- ports 8080, 9099, and 4000 must be free.

In PowerShell window 1 at the repository root:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\firebase-tests\node_modules\.bin\firebase.cmd emulators:start --only auth,firestore --project propcycle-e5f14
```

Leave that window running. Open the local Emulator Suite UI at
<http://localhost:4000/>.

In PowerShell window 2 at the repository root, with the Android Emulator already
started:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:installDebug -PuseFirebaseEmulators=true
```

Launch PropCycle from the Android Emulator. The debug app connects to Auth on
host port 9099 and Firestore on host port 8080 through `10.0.2.2`.

When finished, press `Ctrl+C` in window 1. To return the installed app to cloud
Firebase, rebuild and install without the property:

```powershell
.\gradlew.bat :app:installDebug
```

Do not run `npm.cmd run test:rules` while the full emulator is using port 8080.

## 17. Standard build and test commands

Run these from the repository root unless shown otherwise.

### Compile the debug APK

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

### Run Java unit tests

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest
```

This includes pure validation tests for authentication, marketplace listings,
chat, the scanner structured-result parser, scanner image-size bounds, and the
marketplace image path/size policy.

### Run Firestore and Storage Rules tests

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
Set-Location .\firebase-tests
npm.cmd ci
npm.cmd run test:rules
Set-Location ..
```

### Install the normal cloud-connected debug app

```powershell
.\gradlew.bat :app:installDebug
```

### Stop stale Gradle daemons when troubleshooting

```powershell
.\gradlew.bat --stop
```

Do not run `clean` after every change. Gradle incremental builds are expected.
Use `clean` only when troubleshooting clearly stale generated output:

```powershell
.\gradlew.bat clean
.\gradlew.bat :app:assembleDebug
```

## 18. Verify Phase 2A with two accounts

The app-side code and local test suites are complete. This section remains an
owner-controlled live smoke test after the reviewed production Rules/indexes
are deployed. Do not mark these owner checks complete from local emulator
evidence alone.

Use development-only email addresses and unique test passwords of at least six
characters. Do not reuse a personal, university, GitHub, or Google password.

You can switch accounts on one device. For a more obvious real-time chat test,
use two Android Emulators or an emulator and a physical device against cloud
Firebase.

### Account A: register and publish

1. Launch PropCycle and press **Go!**.
2. Open **Register**.
3. Register Account A with a display name, valid test email, and test password.
4. Confirm Home opens after registration.
5. Open **Market**.
6. Use the menu to open **Create listing**.
7. Enter all required values and publish a listing.
8. Return to the marketplace and open the new listing.
9. Confirm the listing shows the values Account A entered.

For cloud testing, Firebase Console should now show:

- Account A under **Authentication > Users**;
- `users/{accountAUid}` in Firestore; and
- a new `marketplaceListings/{listingId}` document.

The listing may remain text-only. If the project owner has completed Phase
2C.2 Storage setup, Account A may add one optional photo.

### Account B: discover the listing and start chat

1. Log out from Account A, or use a second device. To log out from Home, open
   **Market** from the hamburger fan. On Market, open its hamburger menu and
   choose **Log out**.
2. Register a different Account B.
3. Open **Market** from the Home hamburger fan.
4. Open Account A's listing.
5. Press **Chat**.
6. Send a short text message.
7. Return to Market, open its hamburger menu, choose **Messages**, and confirm
   the conversation is listed.

Firestore should now show:

```text
chatThreads/{threadId}
chatThreads/{threadId}/messages/{messageId}
```

The thread participant list should contain only Account A and Account B.

### Account A: receive and reply

1. Log out from Account B and sign in as Account A, or return to Account A's
   device. If needed, return to Market and use its hamburger menu to find
   **Log out**.
2. From Home, open Market from the hamburger fan. Open the Market hamburger
   menu and choose **Messages**.
3. Open the listing conversation.
4. Confirm Account B's message appears.
5. Send a reply.
6. Return to Account B and confirm the reply appears.

Expected behavior:

- an owner cannot start a chat with their own listing;
- opening Chat again for the same listing and two users reuses the same
  deterministic thread;
- messages appear in timestamp order;
- only the two participants can list or read the conversation; and
- signing out returns protected backend screens to an authentication-required
  state.

An optional third account should be unable to read the A/B thread. Do not edit
documents in the console to make this test pass; access is controlled by the
deployed Rules.

## 18A. Verify the Phase 2B AI scanner

Finish the complete [AI Smart Scanner setup guide](AI_SCANNER_SETUP.md) before
this test. The minimum live path is:

1. confirm AI Logic uses the Gemini Developer API and authenticated-users mode
   is enforced in AI Logic **Settings**;
2. register this device's own `DebugAppCheckProvider` token;
3. sign in to PropCycle;
4. capture one image or choose one through Android Photo Picker;
5. read the transmission disclosure, tick the consent box, and start one
   analysis;
6. confirm a validated result appears with **uncalibrated model estimate**
   wording; and
7. repeat the permission-denied, gallery fallback, offline, retry, rotation, and
   one-request-at-a-time cases in the scanner guide.

Firebase AI Logic is not emulated. The parser and image-bound calculations use
local unit tests. Check service failure paths manually and make only a small
number of deliberate live requests. The scanner must not create Cloud Storage
objects or permanent scan-history records.

## 18B. Verify Phase 2C.1 marketplace owner management

No new Firebase product or API key is needed. First deploy the reviewed
`firestore.rules` and `firestore.indexes.json` as described earlier in this
guide. Then use two cloud test accounts:

1. Account A creates a text-only listing and opens its detail page.
2. Confirm Account A sees **Edit details** and **Withdraw**.
3. Edit every supported field once and save. Confirm the detail page refreshes.
4. In Firestore, confirm `ownerId` and `createdAt` stayed unchanged. `imageUrl`
   stays unchanged unless Account A deliberately chooses a valid replacement
   in Phase 2C.2. `updatedAt` must change to a newer server timestamp.
5. Account B opens the available listing. Confirm Account B sees **Chat with
   seller** but no owner-management card.
6. Account A withdraws the listing after reading the confirmation.
7. Confirm the listing disappears from Account B's public Market list. Account
   B must not open it using an old direct link or start a new listing chat.
8. Confirm Account A remains on the withdrawn detail page, can edit it, and sees
   **Relist**.
9. Account A relists it. Confirm Account B can discover and open it again.
10. Keep any chat created before withdrawal. Withdrawal blocks only a new chat;
    it does not delete an existing conversation.

Use two Account A sessions for the conflict case. Open the same edit form on two
devices. Save on Device 1, then save the older form on Device 2. Device 2 must
show a conflict/reopen message and must not replace Device 1's newer values.

Also check airplane mode while editing. The app may show cached details, but it
must not report a successful edit, withdrawal, or relist without a network
connection.

## 18C. Verify Phase 2C.2 marketplace images

The Firebase owner must first enable the default Storage bucket, download a
fresh `google-services.json`, run both Rules test files, and deploy Firestore and
Storage Rules. Follow the complete
[Marketplace image setup guide](MARKETPLACE_IMAGE_SETUP.md).

The minimum live check is:

1. Account A creates a listing with one Photo Picker image.
2. Confirm the image appears on its detail page and marketplace card.
3. Confirm Firestore stores a private `gs://` reference, not a public token URL.
4. Account A replaces the image with a camera photo.
5. Confirm the new image appears and the old Storage object is removed.
6. Account B can view the authenticated image but cannot edit or delete it.
7. Deny camera permission and confirm Photo Picker still works.
8. Check offline, rotation, failed-upload cleanup, and two-device conflict cases
   from the detailed guide.

## 18D. Verify Phase 2D Recycling Centre map

The Google Cloud project owner must enable billing, Maps SDK for Android, and
Places API (New). Every developer then creates their own ignored
`secrets.properties` entry using a key restricted to package
`com.propcycle.app`, that developer's signing SHA-1, and only those two APIs.

Follow the complete [Recycling Centre map setup guide](RECYCLE_MAP_SETUP.md).
It includes the exact Cloud Console steps, `signingReport` command, local file,
permission tests, missing-key build, security checks, and troubleshooting.

## 19. Daily Git safety check

Before committing:

```powershell
git status --short
git diff --check
```

Confirm that no local configuration, generated output, credentials, test logs,
or `node_modules` appear. In particular:

```powershell
git check-ignore -v .\app\google-services.json
git check-ignore -v .\.firebaserc
git check-ignore -v .\local.properties
git check-ignore -v .\secrets.properties
```

If a protected local file was accidentally staged but not committed, unstage
it. Do not use a force-add workaround. If a credential or service-account key
was already pushed, notify the project owner immediately so access can be
revoked or rotated; deleting it in a later commit does not remove it from Git
history.

## 20. Common errors and exact first fixes

### `prepareKotlinBuildScriptModel` is not found

This usually means Android Studio opened or linked `app` as if it were the
Gradle root.

The task name does not mean the team added Kotlin application code. Android
Studio requests a Kotlin build-script model even for a Java application with
Groovy Gradle scripts, and that model task belongs to the real root project.

Fix it in this order:

1. Close PropCycle in Android Studio.
2. Reopen the repository folder containing `settings.gradle` and
   `gradlew.bat`, not the `app` folder.
3. In the Gradle tool window, unlink any separate top-level `app` project.
4. If Android Studio created `app\.idea`, `app\.gradle`, or
   `app\local.properties` during the wrong import, close Studio and remove only
   those generated nested items. Do not remove the repository-root project.
5. Sync again.
6. If the correct root still fails, use **File > Invalidate Caches > Invalidate
   and Restart**.

Do not add a fake `prepareKotlinBuildScriptModel` task to `app/build.gradle`, do
not add Kotlin, and do not disable Android Studio's Kotlin plugin. Those actions
hide the wrong project link instead of fixing it.

### The `app` module is missing from Run configurations

Finish Gradle Sync and confirm the correct root first. An empty module list is a
symptom of the import or sync failure, not a reason to create another project.

### Gradle reports an unsupported Java or JVM version

In Android Studio's Gradle settings, choose `GRADLE_LOCAL_JAVA_HOME` or the
bundled Android Studio JBR. In PowerShell, set `JAVA_HOME` to that same `jbr`
folder and run:

```powershell
.\gradlew.bat --stop
.\gradlew.bat --version
```

Do not change the project's AGP or Gradle versions as the first fix. Android
Studio Quail 2, AGP 9.3.0, Gradle 9.5.1, and JBR 21 are compatible.

### `SDK location not found`

Open **Tools > SDK Manager** and note the Android SDK path. Reopen the repository
root so Android Studio can create the root `local.properties`, or set the SDK
location through Android Studio. Never commit `local.properties`.

### API 36 or Build-Tools 36.0.0 is missing

Open **Tools > SDK Manager**, install Android SDK Platform 36 and Android SDK
Build-Tools 36.0.0, accept the licences, and sync again.

### Dependency download or Gradle Sync fails

Confirm the PC has internet access and that Gradle Offline mode is disabled.
Retry Sync. If the command-line build works but Android Studio Sync does not,
fix the IDE project link and invalidate IDE caches before changing build files.

### The app says Firebase setup is required

Check all of these:

- filename is exactly `google-services.json`;
- path is exactly `app\google-services.json`;
- project ID is `propcycle-e5f14`;
- package name is `com.propcycle.app`;
- Gradle was synced or the app was rebuilt after adding the file.

The app deliberately does not fake a successful backend operation when the file
is missing.

### `No matching client found for package name`

The downloaded JSON belongs to a different Android app. Download it from the
`com.propcycle.app` Android entry in Firebase project `propcycle-e5f14`. Do not
change the app package merely to match the wrong file.

### Registration says Email/Password is disabled

Ask the Firebase owner to enable the first Email/Password provider switch under
**Authentication > Sign-in method**. Passwordless email-link sign-in is not used.

### Registration or login reports a network error

Check internet access, phone/emulator date and time, and whether the app was
accidentally installed with `-PuseFirebaseEmulators=true` while local emulators
are stopped. Reinstall normally to use cloud Firebase.

### Firestore reports `PERMISSION_DENIED`

Check that the user is signed in and that the assigned maintainer deployed the
reviewed repository Rules to `propcycle-e5f14`. Do not replace the Rules with
`allow read, write: if true`.

### Firestore reports `FAILED_PRECONDITION` or requests an index

Open **Firestore Database > Indexes**. Wait until both repository composite
indexes show **Enabled**. If missing, the assigned maintainer should rerun the
reviewed rules/index deployment.

### `firebase.cmd` does not exist

From the repository root:

```powershell
Set-Location .\firebase-tests
npm.cmd ci
Set-Location ..
```

If npm rejects the Node version, install a supported Node 20-or-newer release,
open a new terminal, and try again.

### Firebase CLI cannot see `propcycle-e5f14`

You are likely signed in with the wrong Google account or the project owner has
not granted access. Run `logout`, then `login --reauth`. If it still does not
appear in `projects:list`, ask the owner to check your Google account access.
Do not use a teammate's login or service account.

### Rules tests say Java is missing

Set `JAVA_HOME` to Android Studio's bundled `jbr` and ensure its `bin` directory
is at the start of `Path` for that PowerShell session.

### Emulator ports are already in use

Stop another Firebase Emulator Suite or Rules-test process before starting a new
one. To inspect the ports without killing anything:

```powershell
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
    Where-Object LocalPort -In 4000, 8080, 9099 |
    Select-Object LocalAddress, LocalPort, OwningProcess
```

Do not terminate an unknown process until you identify it.

### A physical phone does not appear in Android Studio

Unlock it, enable USB debugging, accept the phone's authorisation prompt, try a
data-capable cable and another USB port, and install the manufacturer's Windows
driver if required.

### The AI scanner says setup or authentication is required

Confirm `app\google-services.json` matches `propcycle-e5f14` and
`com.propcycle.app`, then sign in with a development account. AI analysis is
intentionally unavailable to signed-out users and never returns a fake result.

A build installed with `-PuseFirebaseEmulators=true` deliberately blocks live
AI analysis so a local Firebase test cannot accidentally consume Gemini quota.
Install a normal debug build before the deliberate live scanner test.

### The AI request is rejected by App Check

Run the debug app, filter Logcat for `DebugAppCheckProvider`, and register that
device's current token under **Security > App Check > Apps >
com.propcycle.app > Manage debug tokens**. Clearing app data or using another
emulator can produce a new token. Never hard-code or commit it.

### The camera preview is black

Confirm the normal Android Camera app works. On an emulator, set the back camera
to VirtualScene or a working webcam and cold boot it. Gallery selection remains
the supported fallback and needs no broad storage permission.

### Firestore Emulator works but Gemini does not

Firebase AI Logic has no Local Emulator Suite emulator, and an emulator-mode
build deliberately blocks live AI. Install a normal debug build for the live
test. It then needs internet, the configured Firebase project, a signed-in cloud
user, AI Logic enabled, and valid App Check. Use
[AI_SCANNER_SETUP.md](AI_SCANNER_SETUP.md) for the full diagnosis steps.

## 21. Safe troubleshooting order

When setup fails, use this order so that one problem does not create several
new ones:

1. Confirm the PowerShell and Android Studio project root.
2. Confirm Android Studio Gradle Sync completes.
3. Confirm the Gradle JDK is Android Studio JBR.
4. Confirm API 36 and Build-Tools 36.0.0 are installed.
5. Run the baseline `:app:assembleDebug` command.
6. Confirm the Firebase JSON identity and ignored path.
7. Confirm Email/Password and deployed Firestore/Storage configuration.
8. Run Java unit tests and local Rules tests.
9. Run the app on one known-good emulator.
10. For scanner failures, confirm AI Logic, authenticated-users mode, and that
    device's registered App Check debug token.
11. For marketplace image failures, confirm the default bucket, fresh
    `google-services.json`, Storage Rules, sign-in, and billing/quota state.
12. Separate camera input from AI or Storage by testing the gallery fallback.
13. Only then investigate a feature-specific failure.

Do not solve an environment error by changing application architecture,
downgrading random versions, enabling deferred Firebase products, or weakening
Security Rules.

## Official references

- [Install Android Studio](https://developer.android.com/studio/install)
- [Android Studio projects and modules](https://developer.android.com/studio/projects/)
- [Java versions in Android builds](https://developer.android.com/build/jdks)
- [Android Gradle Plugin compatibility](https://developer.android.com/build/releases/about-agp)
- [Add Firebase to an Android project](https://firebase.google.com/docs/android/setup)
- [Firebase email/password Authentication](https://firebase.google.com/docs/auth/android/password-auth)
- [Firebase CLI](https://firebase.google.com/docs/cli)
- [Firestore Security Rules](https://firebase.google.com/docs/firestore/security/get-started)
- [Firestore indexes](https://firebase.google.com/docs/firestore/query-data/indexing)
- [Firebase Local Emulator Suite](https://firebase.google.com/docs/emulator-suite)
- [Cloud Storage Android setup](https://firebase.google.com/docs/storage/android/start)
- [Cloud Storage Android uploads](https://firebase.google.com/docs/storage/android/upload-files)
- [Cloud Storage Security Rules](https://firebase.google.com/docs/storage/security)
- [Firebase AI Logic Android setup](https://firebase.google.com/docs/ai-logic/get-started?platform=android)
- [Firebase App Check debug provider](https://firebase.google.com/docs/app-check/android/debug-provider)
- [CameraX image capture](https://developer.android.com/media/camera/camerax/take-photo)
- [Android Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker)

For the shorter one-PC Firebase owner checklist, see
[FIREBASE_SETUP.md](FIREBASE_SETUP.md). This teammate guide is the primary
starting point for a new clone. For the scanner service and device setup, use
[AI_SCANNER_SETUP.md](AI_SCANNER_SETUP.md). For Storage, marketplace photo, and
two-account image checks, use
[MARKETPLACE_IMAGE_SETUP.md](MARKETPLACE_IMAGE_SETUP.md).
