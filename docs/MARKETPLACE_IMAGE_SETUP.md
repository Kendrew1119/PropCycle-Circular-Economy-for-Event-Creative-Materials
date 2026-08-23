# PropCycle marketplace image setup - detailed guide

This guide enables the Phase 2C.2 marketplace photo feature. It uses Firebase
Cloud Storage. A listing can have zero or one photo.

The app does not contain a private Storage key. It uses the signed-in Firebase
user and the Firebase configuration in `app/google-services.json`.

## 1. Understand what is already in the project

The repository already contains:

- Firebase Storage Android dependency through the Firebase BoM;
- camera capture with CameraX;
- gallery selection with Android Photo Picker;
- safe photo processing into a rotated, metadata-stripped JPEG;
- a maximum long edge of 1600 pixels and maximum uploaded size of 4 MiB;
- one versioned object path per uploaded photo;
- private `gs://` references in Firestore instead of public download-token URLs;
- authenticated marketplace image display;
- owner-only upload, replacement, and cleanup code;
- `storage.rules`, Firestore rule updates, and local Rules tests; and
- clear setup, sign-in, offline, progress, retry, and error messages.

The repository deliberately does not contain `app/google-services.json`, an
App Check debug token, a service-account file, or another secret.

## 2. Check permission and billing with the Firebase owner

Only a Firebase project owner or an approved maintainer should do this section.

Cloud Storage for Firebase currently requires the Firebase project to use the
Blaze pay-as-you-go plan. Blaze does not mean that every test has a charge, but
it does connect billing and usage beyond free quotas can cost money.

Before continuing:

1. Confirm the project is `propcycle-e5f14`.
2. Confirm the Android package is `com.propcycle.app`.
3. Ask the team owner for permission to connect billing.
4. In Firebase Console, open **Project settings > Usage and billing**.
5. If the project is not on Blaze, follow the console steps to upgrade it.
6. In Google Cloud Billing, create a small budget and email alert for the team.

Do not create a service-account key. It is not required for the Android app or
normal Firebase CLI deployment.

## 3. Create the default Storage bucket

1. Open <https://console.firebase.google.com/>.
2. Open project **propcycle-e5f14**.
3. In the left menu, open **Build > Storage**.
4. Press **Get started**.
5. Read the billing message and continue only with the project owner's approval.
6. Choose the bucket location carefully. A bucket location cannot be changed
   after creation. Keep it near the app's Malaysian users and the existing
   Singapore Firestore deployment when the console offers a suitable choice.
7. Finish bucket creation.
8. Open the **Files** tab. An empty bucket is normal.

New default buckets normally use this format:

```text
propcycle-e5f14.firebasestorage.app
```

Use the exact bucket name shown by Firebase Console. Do not type a guessed
bucket name into Java source or Gradle files.

## 4. Download a fresh Firebase Android configuration

Download the file again after the bucket exists. An older file may not contain
the `storage_bucket` value.

1. In Firebase Console, open **Project settings**.
2. In **Your apps**, choose Android app `com.propcycle.app`.
3. Press **Download google-services.json**.
4. Put the downloaded file here:

```text
C:\Users\B2B\Desktop\mobileapp\app\google-services.json
```

5. Do not rename it.
6. Do not commit it to Git.

Check that the bucket value exists without printing the whole file:

```powershell
$firebaseConfig = Get-Content .\app\google-services.json -Raw | ConvertFrom-Json
$firebaseConfig.project_info.project_id
$firebaseConfig.project_info.storage_bucket
```

The first output must be `propcycle-e5f14`. The second output must be the exact
bucket shown in Firebase Console.

Confirm Git is ignoring the file:

```powershell
git check-ignore -v app/google-services.json
```

The command must print an ignore rule. Never use `git add -f` for this file.

## 5. Install the local Rules test tools

Open PowerShell in the repository root:

```text
C:\Users\B2B\Desktop\mobileapp
```

Use Android Studio's Java 17 runtime:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
```

If `firebase-tests\node_modules` is missing, install the locked test tools:

```powershell
Set-Location firebase-tests
npm ci
Set-Location ..
```

Run both Firestore and Storage Rules tests:

```powershell
Set-Location firebase-tests
npm test
Set-Location ..
```

Do not deploy if any test fails.

## 6. Deploy Firestore and Storage rules

The Firestore rules were also changed because `imageUrl` can now change only to
the current owner's matching marketplace Storage path.

Sign in to Firebase CLI:

```powershell
.\firebase-tests\node_modules\.bin\firebase.cmd login --reauth
```

Use the Google account that is allowed to edit `propcycle-e5f14`.

Deploy the reviewed Firestore indexes/rules and Storage rules together:

```powershell
.\firebase-tests\node_modules\.bin\firebase.cmd deploy --only firestore,storage --project propcycle-e5f14
```

Wait for **Deploy complete**. Then check both places in Firebase Console:

1. **Firestore Database > Rules** shows the new marketplace image validation.
2. **Storage > Rules** shows the owner-path, JPEG, size, and metadata checks.
3. **Firestore Database > Indexes** shows both required indexes as **Enabled**.

Never leave Storage in a temporary public test mode.

## 7. Build the app

From the repository root:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

A successful build ends with `BUILD SUCCESSFUL`. The APK is:

```text
app\build\outputs\apk\debug\app-debug.apk
```

You can also open the repository root in Android Studio, select the `app` run
configuration, and run it on a device.

## 8. Live check with two accounts

These checks need the real Firebase project. Local builds and Rules tests cannot
prove that the bucket and production Rules are configured correctly.

### Account A - create with a gallery photo

1. Sign in as Account A.
2. Open **Market > Create listing**.
3. Press **Choose photo**.
4. Choose a normal JPEG or PNG from the Android Photo Picker.
5. Confirm the preview appears.
6. Complete the required listing fields and press **Publish**.
7. Keep the screen open while preparation, upload, and save are in progress.
8. Confirm the detail page shows the photo.
9. Return to Market and confirm the browse card shows the photo.
10. In Firestore, inspect the new listing. `imageUrl` must begin with `gs://`.
11. In Storage, confirm one file exists below:

```text
marketplace/{accountAUid}/{listingId}/primary_{version}.jpg
```

The Firestore value must not be an `https://firebasestorage.googleapis.com`
download-token URL.

### Account A - use the camera and replace a photo

1. Open Account A's listing and press **Edit details**.
2. Press **Camera**.
3. Allow camera permission and wait for the preview.
4. Press **Capture photo**.
5. Confirm the new preview appears, then press **Save changes**.
6. Confirm the new photo appears on detail and browse screens.
7. In Storage, confirm the previous version was removed after the successful
   Firestore update.

If old-file cleanup fails because the network stops at the last moment, the new
listing is still valid. Record the old object for owner cleanup. Automatic
scheduled orphan cleanup is deliberately not included in this phase.

### Account B - check authenticated read and owner protection

1. Sign in as a different Account B.
2. Open Market and Account A's available listing.
3. Confirm Account B can see the photo while signed in.
4. Confirm Account B does not see **Edit details**, **Withdraw**, or **Relist**.
5. Do not manually edit cloud data to imitate Account A.

The local Rules tests already verify that Account B cannot upload, replace, or
delete files below Account A's Storage path.

### Permission and failure checks

Also check these cases on a real device:

- deny camera permission and confirm **Choose photo** still works;
- choose a very large or rotated photo and confirm a correctly rotated bounded
  preview is produced;
- turn off the network before publishing and confirm the app shows an error and
  does not claim success;
- rotate the device while a prepared preview is visible and confirm it remains;
- press Back during upload and confirm navigation is blocked until the operation
  finishes;
- open an existing listing on two devices, replace it on Device 1, then save the
  older form on Device 2 and confirm Device 2 reports a conflict;
- sign out and confirm protected marketplace images are not shown to an
  unauthenticated user; and
- create a text-only listing and confirm its normal placeholder remains usable.

## 9. App Check note

The project already uses the App Check debug provider only in debug builds and
Play Integrity only in release builds. If the Firebase owner enables App Check
enforcement for Cloud Storage, every developer must register their own debug
token first. Follow `docs/AI_SCANNER_SETUP.md` for debug-token registration.

Do not commit or share an App Check debug token. Do not use the debug provider
in a release build. Test the exact signed release APK before enforcing App Check
for production users.

## 10. Troubleshooting

### The app says Storage setup is required

- Confirm the default bucket exists in **Firebase Console > Storage**.
- Download a new `google-services.json` after bucket creation.
- Confirm `project_info.storage_bucket` is present.
- Confirm the file is in `app`, not the repository root.
- Rebuild the app.

### Upload says permission denied

- Confirm the user is signed in.
- Deploy `storage.rules` and the updated `firestore.rules`.
- Confirm the path starts with the signed-in owner's UID.
- Confirm the Firebase CLI used project `propcycle-e5f14`.

### Upload says retry limit, quota, or project problem

- Check the internet connection.
- Open **Firebase Console > Storage > Usage**.
- Check Google Cloud billing status, quotas, and budget alerts.
- Retry once after the service recovers. Do not weaken Rules to fix a quota
  problem.

### The listing saves but the image does not load

- Confirm the Firestore `imageUrl` begins with `gs://` and points to the same
  owner UID and listing ID.
- Confirm the object exists in Storage.
- Confirm the viewer is signed in.
- Confirm Storage Rules were deployed.
- Do not replace the value with a public token URL.

### A failed create left a file in Storage

The app attempts immediate cleanup when Firestore create fails. A process kill or
network loss can interrupt that best-effort cleanup. The Firebase owner may
delete that one verified orphan from the Storage console. Do not delete an
entire owner or marketplace folder.

## Official Firebase references

- [Get started with Cloud Storage on Android](https://firebase.google.com/docs/storage/android/start)
- [Upload files on Android](https://firebase.google.com/docs/storage/android/upload-files)
- [Cloud Storage Security Rules](https://firebase.google.com/docs/storage/security)
- [Storage Rules conditions](https://firebase.google.com/docs/storage/security/rules-conditions)
