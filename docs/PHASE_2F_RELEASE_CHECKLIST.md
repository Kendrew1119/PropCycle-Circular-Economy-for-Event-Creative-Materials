# Phase 2F Release Hardening and Live-Test Guide

This guide helps the PropCycle team turn the locally verified app into an
evidence-backed course release candidate. Use simple test accounts and test
data only.

Phase 2F does **not** add another feature or API. It checks the existing Java/XML
app, deploys reviewed Firebase configuration only when the project owner is
ready, tests real services, and prepares a signed APK. A local build cannot
prove that Firebase, Gemini, Maps, permissions, or a physical device works.

## 1. What is automatic now

Every push or pull request to `main` runs GitHub checks for:

- all Java unit tests;
- debug and unsigned release builds;
- Android lint;
- Firestore and Storage Security Rules tests in local Firebase emulators;
- forbidden secret/configuration files;
- accidental Kotlin, React Native, Expo, or TypeScript source.

GitHub uses the app's setup-required fallback. Do not upload
`google-services.json`, a Maps key, App Check token, or signing key to GitHub
Actions.

On Windows, run the same local gate from the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1
```

If Rules-test dependencies are missing, install them once:

```powershell
cd .\firebase-tests
npm ci
cd ..
powershell -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1
```

`PASS` means that one local check succeeded. `PENDING` means that a setup or
live check is still required. The script never deploys Firebase, creates a
keystore, signs a candidate, or opens an emulator.

## 2. Before any production deployment

Only the Firebase project owner should do this section.

1. Pull the newest `main` branch and confirm GitHub's Android quality checks
   are green.
2. Open Firebase Console and confirm the selected project is
   `propcycle-e5f14` and the Android package is `com.propcycle.app`.
3. Compare any rule edits made in Firebase Console with `firestore.rules` and
   `storage.rules`. A CLI deployment replaces the console rules.
4. Run the local preflight again. Confirm all 80 Java tests and all 25 Rules
   tests pass before continuing.
5. Sign in to Firebase CLI with the authorised project-owner account. Never use
   a service-account JSON file on a shared student computer.
6. Confirm the selected project before deployment:

```powershell
cd C:\Users\YOUR_NAME\path\to\mobileapp
.\firebase-tests\node_modules\.bin\firebase.cmd projects:list
.\firebase-tests\node_modules\.bin\firebase.cmd use propcycle-e5f14
```

7. Deploy only the reviewed Firestore Rules/indexes and Storage Rules:

```powershell
.\firebase-tests\node_modules\.bin\firebase.cmd deploy --only firestore,storage --project propcycle-e5f14
```

8. Save the command output and deployment time in the evidence table below.
9. If Firebase asks to allow Cloud Storage Rules to read Firestore documents,
   review the selected project again before accepting. The lending and
   marketplace Storage Rules use Firestore ownership data.

Official Firebase documentation confirms that CLI Rules deployment overwrites
the active console Rules, so always test and review the repository copies first:
[Manage and deploy Security Rules](https://firebase.google.com/docs/rules/manage-deploy)
and [Firebase CLI reference](https://firebase.google.com/docs/cli).

## 3. Live service setup that remains required

Complete the detailed guides in this order:

1. [Firebase accounts, marketplace, and chat](FIREBASE_SETUP.md)
2. [Marketplace images](MARKETPLACE_IMAGE_SETUP.md)
3. [AI scanner and App Check](AI_SCANNER_SETUP.md)
4. [Recycling Centre Maps and Places](RECYCLE_MAP_SETUP.md)
5. [P2P lending lifecycle](LENDING_SETUP.md)

Each authorised developer downloads their own copy of
`app/google-services.json` from the same Firebase Android app. They keep their
own restricted Maps key in `secrets.properties` and register their own private
App Check debug token. Do not send these files or values in Git, screenshots,
the report, or a group chat.

## 4. Two-account and two-device test

Use two test accounts, called Owner and Borrower. Prefer two physical Android
devices. Record the Android version and device model.

### Account and session

- Register both accounts and confirm each can sign in after restarting the app.
- Sign out Owner, sign in Borrower on the same device, and confirm Owner data is
  not shown as Borrower data.
- Try an invalid password and confirm the app shows an error without crashing.
- Turn off the network, reopen a Firebase screen, and confirm an honest offline
  or retry state appears.

### Marketplace and chat

- Owner creates a listing. Borrower sees it in browse and opens its detail.
- Borrower cannot edit, withdraw, relist, or replace Owner's image.
- Owner edits, withdraws, and relists the listing. Borrower sees each expected
  public-state change.
- Owner creates/replaces one image. Borrower sees it while signed in; an
  unauthorised request is rejected.
- Borrower starts a chat. Both users exchange text and no third account can read
  the thread.
- Borrower taps the seller avatar in Marketplace Detail and the other-member
  avatar in Conversation. Confirm both open the correct public profile without
  exposing email, edit/logout controls, or device-local activity.
- Borrower saves a 1–5-star Marketplace seller rating, updates it once, and
  confirms the aggregate appears on Marketplace Detail and the seller's public
  profile. Confirm Owner cannot rate their own listing and a third account
  cannot overwrite Borrower's rating.

### AI scanner

- Confirm AI Logic uses authenticated-users protection and the test device has
  its own registered debug App Check token.
- Test camera permission allowed and denied, plus Photo Picker fallback.
- Submit one non-sensitive image with the disclosure accepted.
- Confirm one real structured result or record the exact service error. Do not
  call a setup/error screen a successful Gemini scan.
- Review request metrics and quota/budget controls after the request.

### Recycling centre

- Use a restricted Android Maps key with the correct package name and SHA-1.
- Test precise, approximate, and denied foreground-location choices.
- Confirm manual area search still works without location permission.
- Confirm marker/list selection stays in sync and map-app handoff opens.
- Confirm the screen reminds the user to check material acceptance before
  travelling.

### Lending

- Owner publishes one lending item with an optional image and approximate map
  point. Borrower finds it in the list and map.
- Borrower requests valid inclusive dates. Owner approves the request.
- A second overlapping approval must fail because the booked-day locks exist.
- Test lending chat, pickup, borrower return report, owner return confirmation,
  and one rating.
- Confirm non-participants cannot read or change the request, chat, locks,
  image, or rating.
- Complete every extra check in [the lending guide](LENDING_SETUP.md).

## 5. Accessibility and stability test

On at least one lower-memory phone and one recent phone:

- test Android 7/API 24 if the team has a device, plus one Android 13+ device;
- use large font and display size, landscape, dark system mode, and split screen;
- use TalkBack on login, Home, scanner, marketplace detail, lending request,
  Notifications, and chat;
- deny camera and location permissions, then recover through Settings;
- rotate during a form, upload, and search;
- run the presentation path for ten minutes without a crash;
- keep a non-sensitive local demonstration image and screenshots as a fallback,
  clearly labelled as prepared demo material rather than a live result.

## 6. Create the signed course APK

The project owner should own the signing key. Never commit or send the keystore,
passwords, or `keystore.properties`.

1. In Android Studio, select **Build > Generate Signed App Bundle or APK**.
2. Select **APK**, then choose the `app` module.
3. Create or select the team-controlled release keystore in a private backup
   location outside this repository.
4. Record who controls the key and where the protected backup is stored. Do not
   record passwords in this repository.
5. Select the `release` build type and generate the APK.
6. Register the release certificate SHA-1/SHA-256 where required by Firebase,
   App Check/Play Integrity, and the Android-restricted Maps key.
7. Install this exact signed APK on both test devices. A debug build is not
   acceptable evidence for release App Check or release-key restrictions.
8. Calculate and record its SHA-256 hash:

```powershell
Get-FileHash -Algorithm SHA256 C:\private\path\PropCycle-release.apk
```

9. Keep the final APK, hash, build commit, test evidence, report, and
   contribution log together in the private submission folder.

Android's official release guide explains that a release APK must be signed and
that Android Studio provides the signed APK wizard:
[Build your app for release](https://developer.android.com/build/build-for-release).

## 7. Evidence record

Copy this table into the private team report or issue. Do not add credentials.

| Evidence | Result | Date/time | Tester/device | Safe link or note |
|---|---|---|---|---|
| GitHub quality checks | Pending | | | |
| Local Phase 2F preflight | Pending | | | |
| Firestore Rules/index deployment | Pending | | | |
| Storage Rules deployment | Pending | | | |
| Two-account auth/market/chat | Pending | | | |
| One live protected Gemini scan | Pending | | | |
| Maps permission/manual search | Pending | | | |
| Full lending lifecycle | Pending | | | |
| Accessibility/device matrix | Pending | | | |
| Exact signed APK install | Pending | | | |
| Release App Check evidence | Pending | | | |
| APK SHA-256 and private archive | Pending | | | |

## 8. Release decision

Do not mark Phase 2F complete while any release-blocking row above is pending or
failed. A failure does not mean the whole app is unusable; record the exact
scope, fix it, rerun the affected test, and keep the earlier evidence.

Features still outside this assessed baseline include OS push notifications,
payment processing, routes/background tracking, permanent scanner images or
cross-device scan history, multiple marketplace/lending images, WorkManager offline outbox, trusted
server automation, and store publication. Do not add them during feature
freeze without a separately approved scope change.
