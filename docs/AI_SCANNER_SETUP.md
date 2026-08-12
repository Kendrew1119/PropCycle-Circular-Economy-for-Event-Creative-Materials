# PropCycle AI Smart Scanner setup guide

This guide explains how to make the Phase 2B scanner work on one development
computer. Follow the steps in order. The scanner needs all three items below:

1. a signed-in PropCycle test account;
2. a working camera photo or gallery photo; and
3. Firebase AI Logic enabled for the correct Firebase project.

The project is `propcycle-e5f14`. The Android package is
`com.propcycle.app`.

## Important: do not create a Gemini API key

PropCycle does **not** use a raw Gemini API key in Java, XML, Gradle,
`local.properties`, or an `.env` file.

The Android app uses the Firebase AI Logic Android SDK. Firebase connects the
app to the Gemini Developer API. The Firebase setup keeps the provider key out
of the installed app.

Do not paste a key from Google AI Studio into the repository. Do not send a key
to a teammate. Do not commit a debug App Check token either.

If a tutorial asks you to add a line such as `GEMINI_API_KEY=...`, stop using
that tutorial. It describes a different integration.

The Firebase Android `google-services.json` can contain normal Firebase client
configuration, including a Firebase client API-key field. Do not copy that
field into Java and do not confuse it with a raw Gemini/Google AI Studio key.
This repository still keeps the whole JSON local and ignored by policy.

## What Phase 2B includes

The scanner slice includes:

- a CameraX preview and one-photo capture;
- Android Photo Picker for choosing one image from the gallery;
- camera permission requested only when the camera is used;
- a bounded, rotated, metadata-stripped temporary JPEG in app-private cache;
- a clear disclosure before the image is sent for analysis;
- one Firebase AI Logic request at a time;
- a structured Gemini result checked by the app;
- a result and review screen; and
- a confidence value labelled as an **uncalibrated model estimate**.

This phase does not save scan history. It does not upload an image to Cloud
Storage. Scanner temporary files are deleted after use. Maps, recycling-centre
APIs, lending logic, push notifications, Room, Remote Config, and every other
new integration remain deferred.

## What is already in the Android project

The project currently locks these scanner dependencies:

| Item | Project version |
|---|---|
| Firebase Android BoM | `34.17.0` |
| Firebase AI Logic | managed by the BoM (`firebase-ai` `17.15.0` in that BoM) |
| CameraX | `1.6.1` |
| AndroidX Activity/Photo Picker contract | `1.13.0` |
| AndroidX ExifInterface | `1.4.2` |
| Debug App Check provider | managed by the BoM (`firebase-appcheck-debug` `19.4.0` in that BoM); debug builds only |
| Play Integrity App Check provider | managed by the Firebase BoM; release builds only |
| Gemini model selected in the current code | `gemini-3.6-flash` |

Do not add these dependencies again. Do not copy old version numbers from a
video. If a later update is needed, the team must check the current official
release pages and update the locked versions together.

## Before you start

Complete the normal teammate setup first:

1. Clone the private repository.
2. Open the repository root in Android Studio. Do not open only the `app`
   folder.
3. Use Android Studio's bundled JBR for Gradle.
4. Install Android SDK Platform 36 and Build-Tools 36.0.0.
5. Put your own downloaded `google-services.json` at exactly:

   ```text
   app\google-services.json
   ```

6. Confirm the JSON is for project `propcycle-e5f14` and package
   `com.propcycle.app`.
7. Confirm the app builds.
8. Confirm Email/Password Authentication works and you can sign in.

Use [TEAM_SETUP_GUIDE.md](TEAM_SETUP_GUIDE.md) for the full new-computer setup.
Use [FIREBASE_SETUP.md](FIREBASE_SETUP.md) for the Phase 2A owner checks.

`google-services.json` stays private and ignored by Git. A private GitHub
repository is not a reason to commit it. Every authorised teammate downloads
their own copy from the same Firebase Android app entry.

## Part A - Firebase project owner setup

Only a person with permission to change the shared Firebase project should do
Part A. This changes a shared cloud service for the whole team.

### A1. Open the correct Firebase project

1. Open <https://console.firebase.google.com/>.
2. Sign in with the Google account that has access to PropCycle.
3. Open project **propcycle-e5f14**.
4. Check the project name and project ID before enabling anything.
5. Do not create a second Firebase project for this app.

### A2. Confirm Email/Password Authentication

The scanner requires a signed-in user.

1. In Firebase Console, open **Authentication**.
2. Open **Sign-in method**.
3. Open **Email/Password**.
4. Turn on the first **Email/Password** switch.
5. Leave passwordless email-link sign-in off.
6. Save the setting.

Do not use a real personal password for testing. Create a development-only
account in the PropCycle Register screen.

### A3. Enable Firebase AI Logic

1. In the Firebase Console left menu, find **AI Services**.
2. Open **AI Logic**.
3. Press **Get started**.
4. Choose the **Gemini Developer API** workflow.
5. Confirm the Android app is `com.propcycle.app`.
6. Complete the guided setup.
7. Return to **AI Services > AI Logic** and open **Settings**.
8. Find **Authenticated-users mode**.
9. Choose **Enforce authenticated-users mode**, then press **Confirm**.

Use the Gemini Developer API option for this phase. Do not switch the code to a
direct REST key. Do not enable Vertex AI unless the team separately approves
that provider and its billing setup.

The AI Logic guided setup automatically enables App Check enforcement for AI
Logic. This is expected. A first debug request can be rejected until that
developer registers the debug token described below.

### A4. Check that the correct Firebase Android app exists

1. In Firebase Console, press the gear icon.
2. Open **Project settings**.
3. Scroll to **Your apps**.
4. Find the Android app with package name `com.propcycle.app`.
5. If it is missing, stop and ask the project owner. Do not register a different
   package just to bypass the problem.

If a teammate's JSON is old or belongs to another app, download a fresh copy
from this exact Android app entry and place it at `app\google-services.json`.
Do not commit it.

You normally do not need to download the JSON again only because AI Logic was
enabled. The existing matching file contains the Android app's Firebase client
configuration. Download it again only if the Firebase Android registration was
changed, the file is missing/wrong, or the console specifically tells the owner
that the app configuration changed.

### A5. Verify App Check for Firebase AI Logic

1. In Firebase Console, open **Security**.
2. Open **App Check**.
3. Open the **APIs** or **Products** view. The label can vary slightly as the
   console changes.
4. Find **Firebase AI Logic**.
5. Confirm the `com.propcycle.app` Android app is listed.
6. Confirm enforcement is enabled for Firebase AI Logic after at least one
   developer has registered a valid debug token and verified a request.

Do not turn on enforcement for unrelated services merely because the switch is
visible. Firestore protection still comes from the reviewed Security Rules,
and its rollout is a separate owner action.

## Part B - register your own debug App Check token

Every developer needs their own token. A token identifies one local debug app
installation. It is not a team password.

The debug provider exists only in the `debug` build. The release build uses the
Play Integrity provider instead.

### B1. Build and install the debug app

From the repository root in PowerShell:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:installDebug
```

You can also select the `app` run configuration in Android Studio and press
Run.

### B2. Open Logcat before requesting AI

1. In Android Studio, open **View > Tool Windows > Logcat**.
2. Select the phone or emulator that is running PropCycle.
3. Select the `com.propcycle.app` process.
4. In the Logcat search box, enter:

   ```text
   DebugAppCheckProvider
   ```

5. If no line appears yet, leave Logcat open.
6. In PropCycle, sign in and open **AI Smart Scanner**.
7. Choose or capture an image and continue to the analysis step. This causes
   Firebase to request an App Check token.

Logcat should print a message from `DebugAppCheckProvider`. It contains a debug
secret and tells you to register it in the Firebase Console.

### B3. Register the token in Firebase Console

1. Copy only the debug token from Logcat.
2. Open Firebase Console project **propcycle-e5f14**.
3. Open **Security > App Check**.
4. Open **Apps**.
5. Find `com.propcycle.app`.
6. Open its menu and choose **Manage debug tokens**.
7. Press **Add debug token**.
8. Give it a clear name, for example:

   ```text
   Aiman - laptop - Pixel 8 API 36
   ```

9. Paste the token.
10. Save it.
11. Return to PropCycle and press Retry, or restart the app and scan again.

Never put this token in a Markdown file, source file, Gradle property, issue,
chat message, screenshot, or Git commit.

If you clear the app's data, uninstall it, or use another emulator, Android can
create a different debug token. Register the new token with a new clear label.
Ask the Firebase owner to delete old tokens that are no longer used.

### B4. Confirm that App Check is working

1. Complete one real scanner request.
2. Return to **Security > App Check** in Firebase Console.
3. Open the request metrics for `com.propcycle.app`.
4. Confirm the request is shown as valid after metrics update.
5. Recheck the **Firebase AI Logic** row in the APIs/Products view.

Metrics may take a short time to appear. Do not repeatedly send images only to
refresh the graph because every live request uses project quota.

## Part C - camera and gallery setup

### C1. Physical Android phone

A physical phone gives the most reliable camera test.

1. Enable Developer options and USB debugging.
2. Connect the phone with a data-capable USB cable.
3. Accept the phone's computer-authorisation prompt.
4. Select the phone in Android Studio.
5. Run the debug app.
6. When PropCycle asks for camera access, choose **While using the app**.

PropCycle does not request microphone access. It does not request broad photo or
storage permission. Gallery selection uses the system Photo Picker.

### C2. Android Emulator camera

The emulator can use a virtual scene or a computer webcam.

1. In Android Studio, open **Tools > Device Manager**.
2. Find the test emulator.
3. Press its edit pencil.
4. Open **Show Advanced Settings**.
5. Find **Camera**.
6. Set the back camera to **VirtualScene** for a predictable test, or choose a
   working computer webcam.
7. Save the emulator settings.
8. Cold boot the emulator if the camera stays black.
9. Open the Android Camera app once to confirm that the emulator camera works.
10. Run PropCycle and test capture.

If the emulator has no working camera, use **Choose from gallery**. Camera
hardware is optional and gallery use must still work.

### C3. Put a test image into an emulator

You can drag a normal JPEG or PNG from Windows onto the running emulator. Wait
until Android imports it. Then open PropCycle and use the gallery button.

Use a clear photo of one material for the first test. Avoid personal documents,
faces, addresses, number plates, or private location details.

## Part D - run one complete scanner test

1. Make sure the device has internet access.
2. Launch PropCycle.
3. Register or sign in with a development-only account.
4. Open Home.
5. Open **AI Smart Scanner**.
6. Test one input method:
   - allow camera access and capture one photo; or
   - use the gallery button and select one image.
7. Check that a preview appears.
8. Read the disclosure. It explains that a processed copy of the image will be
   sent to Firebase AI Logic/Gemini.
9. Tick **I understand and want to send this image for AI analysis**. The
   analysis button stays disabled until this box is ticked.
10. Press the analysis button once.
11. Wait for the request. Do not press repeatedly.
12. Confirm that the result screen shows the identified item, material,
    category, recycling guidance, upcycling ideas, and impact text.
13. Confirm the confidence wording says **uncalibrated model estimate**.
14. Review the result. AI output can be inaccurate and is not a guarantee that
    a Malaysian recycling centre accepts the material.
15. Test the existing next-action buttons. Maps and lending backend behaviour
    are still static/deferred; a scan must not silently publish anything.

The app sends only its bounded working JPEG for this request. It does not add
the scan to Cloud Storage or permanent scan history in Phase 2B.

## Manual Phase 2B test checklist

The Firebase AI Logic service has no local emulator. The structured parser and
image-bound calculations use local unit tests. Service failure paths and one
successful live AI request must be checked manually.

Use this checklist before calling the phase ready on a computer:

- [ ] The app builds when `app/google-services.json` is present.
- [ ] The app also builds when the JSON is absent and shows setup-required
  state instead of fake AI success.
- [ ] A signed-out user is asked to sign in before analysis.
- [ ] Camera permission is requested only after choosing the camera path.
- [ ] Denying camera permission still leaves gallery selection usable.
- [ ] Permanent camera denial shows a clear Settings/fallback path.
- [ ] A physical device or configured emulator can capture one image.
- [ ] Android Photo Picker can choose one image without storage permission.
- [ ] A portrait image is shown with the correct orientation.
- [ ] A landscape image is shown with the correct orientation.
- [ ] A bad or unsupported image shows a readable error and does not crash.
- [ ] The transmission disclosure appears before the live request.
- [ ] Pressing Analyze several times does not create concurrent requests.
- [ ] Turning off the network shows a readable retry state.
- [ ] An unregistered App Check token is rejected.
- [ ] Registering that developer's token allows a valid debug request.
- [ ] The result parser rejects missing, unknown, oversized, or malformed
  structured fields without crashing.
- [ ] The confidence is labelled as an uncalibrated model estimate.
- [ ] The result can be reviewed before any next action.
- [ ] Back, retry, rotation, and leaving the scanner do not crash the app.
- [ ] PropCycle app code does not log scanner images, raw AI responses, or
  private user data. The debug provider's expected token may appear in debug
  Logcat; never capture it in screenshots, share it, or commit it.

Because a live request consumes quota, use local automated tests for repeated
failure cases. Do not repeatedly call Gemini only to test button styling.

## Quota, billing, and privacy checks

The Firebase project owner should do these checks before a team demo:

1. Open the usage and quota pages linked from Firebase AI Logic.
2. Check that the project is using the intended Gemini Developer API plan.
3. Review the current limits in the console. Do not copy an old free-tier limit
   into the report because quotas can change.
4. If billing is linked, create a Google Cloud budget and email alert for the
   project.
5. Understand that a budget alert sends a warning. It does not automatically
   stop spending.
6. Keep App Check enforcement enabled for AI Logic after valid traffic is
   confirmed.
7. Use project quotas and budget alerts. The current Android-only slice does
   not claim abuse-resistant per-user rate limits because that needs trusted
   backend logic.

Only test with non-sensitive images. The working image is sent to Google's
Firebase AI Logic/Gemini service for analysis. Check the current Firebase and
Gemini data-use terms before testing with real users.

## Release setup is a later owner gate

Debug App Check is only for local development. Never ship it in a release APK.

Before a real release, the project owner must:

1. choose and protect the final release signing key;
2. obtain the release certificate SHA-256 fingerprint;
3. add that SHA-256 fingerprint to the `com.propcycle.app` Android entry in
   Firebase Project settings;
4. register the Android app with the **Play Integrity** App Check provider;
5. confirm the correct Google Play/Firebase/Google Cloud project links and
   permissions;
6. review the policy for a course APK installed outside Google Play;
7. test the exact signed APK and watch App Check metrics; and
8. enable or keep enforcement only after valid release requests are confirmed.

The code keeps the debug provider in debug builds and the Play Integrity
provider in release builds. That separation does not complete the console,
certificate, Play Console, or signed-APK steps automatically.

Do not solve a release failure by adding the debug token to the APK or by
disabling security without documenting and approving the reduced protection.

## Troubleshooting

### The scanner says Firebase setup is required

Check all of these:

- the file is named exactly `google-services.json`;
- it is inside the `app` folder;
- its project ID is `propcycle-e5f14`;
- its Android package is `com.propcycle.app`; and
- the app was rebuilt after the file was added.

Do not change the Java package to match the wrong JSON.

### The scanner asks you to sign in

This is expected. Phase 2B allows AI analysis only for authenticated users.
Register a test account or sign in. If sign-in fails, finish the Phase 2A
Authentication setup first.

### The request is rejected by App Check

1. Open Logcat and filter `DebugAppCheckProvider`.
2. Copy the current token.
3. Register that exact token under **Security > App Check > Apps >
   com.propcycle.app > Manage debug tokens**.
4. Wait briefly, restart the app, and retry.

If you cleared app data or changed emulator, the old token may no longer be the
current token.

### No debug token appears in Logcat

- Confirm you installed the `debug` variant, not a release APK.
- Select the correct device and `com.propcycle.app` process in Logcat.
- Remove other Logcat filters.
- Search for `DebugAppCheckProvider` again.
- Trigger a scanner AI request after Firebase is configured.
- Restart the app if Logcat attached after the first token message.

Do not hard-code a made-up token.

### Firebase AI Logic is missing in the console

Confirm the correct project is open. Look under **AI Services > AI Logic**. If
the service still is not visible, the signed-in Google account may not have
permission to enable it. Ask the Firebase project owner.

### The request says unauthenticated or permission denied

Check two separate items:

1. PropCycle has a signed-in Firebase Authentication user.
2. The device's debug App Check token is registered.

Also confirm AI Logic was configured for authenticated users in project
`propcycle-e5f14`.

### The request reports quota or rate limiting

Stop retrying. Open the Firebase AI Logic usage page and Google Cloud quota page
for the correct project. Wait for the limit window or ask the project owner to
review the approved plan. Do not switch projects or add an unrestricted raw key.

### The request reports that the model is unavailable

Confirm the project is using the current committed model setting and the Gemini
Developer API. Do not guess another preview model name in Java. Check the
official Firebase AI Logic supported-model documentation, then make a reviewed
code and plan update if the selected model must change.

### Camera preview is black

- Test the Android Camera app on the same device.
- Close another app that is using the camera.
- On an emulator, set the back camera to VirtualScene or a real webcam and cold
  boot it.
- Check the system camera permission for PropCycle.
- Use the gallery fallback to separate a camera problem from an AI problem.

### Camera permission was permanently denied

Open Android **Settings > Apps > PropCycle > Permissions > Camera** and allow
it, or keep using the gallery. PropCycle should not request storage permission.

### The gallery opens but the image is rejected

Choose a normal JPEG, PNG, WebP, or HEIC image that Android can decode. Avoid a
corrupt file or an unusual document provider. The app rejects invalid or
oversized decoded input instead of loading an unbounded image into memory.

### The result is wrong

AI identification is an estimate. Use a clear image with one main item and good
light. Review every field. Do not present recycling guidance as a guarantee and
do not publish an unreviewed result.

### Firestore Emulator is running but AI still fails

This is expected. Firebase AI Logic is not part of the Local Emulator Suite.
For safety, a build installed with `-PuseFirebaseEmulators=true` deliberately
blocks live AI analysis. Install a normal debug build for the deliberate live
test. The scanner then needs internet access, a signed-in cloud user, and valid
App Check. Firestore Rules tests do not test Gemini.

## Quick owner checklist

- [ ] Project is `propcycle-e5f14`.
- [ ] Android package is `com.propcycle.app`.
- [ ] Email/Password Authentication is enabled.
- [ ] Firebase AI Logic guided setup is complete.
- [ ] Gemini Developer API was selected.
- [ ] Authenticated-users mode is enforced from AI Logic **Settings**.
- [ ] At least one developer registered their own debug App Check token.
- [ ] A valid debug request appears in App Check metrics.
- [ ] Firebase AI Logic enforcement is verified.
- [ ] Current quota and usage are reviewed.
- [ ] A billing budget alert exists if billing is linked.
- [ ] The team understands that AI Logic has no local emulator.
- [ ] Release Play Integrity and release SHA-256 remain a separate owner gate.

## Official references

- [Get started with Firebase AI Logic on Android](https://firebase.google.com/docs/ai-logic/get-started?platform=android)
- [Generate text from text and image input](https://firebase.google.com/docs/ai-logic/generate-text?platform=android)
- [Generate structured output](https://firebase.google.com/docs/ai-logic/generate-structured-output?platform=android)
- [Require Firebase Authentication for AI Logic](https://firebase.google.com/docs/ai-logic/auth-mode)
- [Firebase AI Logic production checklist](https://firebase.google.com/docs/ai-logic/production-checklist)
- [Firebase App Check debug provider](https://firebase.google.com/docs/app-check/android/debug-provider)
- [Firebase App Check with Play Integrity](https://firebase.google.com/docs/app-check/android/play-integrity-provider)
- [CameraX image capture](https://developer.android.com/media/camera/camerax/take-photo)
- [Android Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker)
- [Android runtime permissions](https://developer.android.com/training/permissions/requesting)
- [Android Emulator camera setup](https://developer.android.com/studio/run/emulator-use-camera)
- [Google Cloud budgets and alerts](https://cloud.google.com/billing/docs/how-to/budgets)
