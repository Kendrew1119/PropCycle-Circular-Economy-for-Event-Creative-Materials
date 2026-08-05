# PropCycle Native Android Direction

## Current phase: planning hold

Do not create or modify application implementation files until the user/team explicitly approves the native plan in `plan.md` and authorises coding.

During the planning hold:

- Documentation-only changes are allowed.
- The obsolete Expo/React Native source, configuration, generated output, dependencies, default assets, and `.env` were removed on 5 August 2026 at the user's explicit direction. Do not restore that stack.
- Preserve the proposal/course documents and planning files that remain in the repository.
- Do not generate an Android project, install dependencies, or add Java/XML application source during this hold.
- Reserve the repository root for the future native Gradle project after approval; do not create an `android-native/` side project.
- Treat the external `C:\Users\B2B\Downloads\Group7-PropCycle.pdf` as the UI/module source of truth and `plan.md` as the implementation-planning source of truth.

## Direction after explicit implementation approval

- Build a clean native Android application; do not use React Native or Expo in the target app.
- Use Java 17 for team-authored application and test source.
- Use Android Views with XML layouts and Material Components; do not use Jetpack Compose.
- Do not add handwritten Kotlin unless the user approves a documented exception after a stable Java option has been exhausted.
- Use Gradle Groovy scripts, one `:app` module initially, and package-by-feature boundaries.
- Use `minSdk 24`, `compileSdk 36`, and `targetSdk 36`; re-check official Android requirements when scaffolding starts.
- Use one Activity, Fragments, AndroidX Navigation, View Binding, Java ViewModels/LiveData, repositories, and Hilt Java annotation processing.
- Use Room 2.8.x with Java `annotationProcessor`; do not use Room 3 because this plan is Java-first.
- Use CameraX, Android Photo Picker, Maps/Places SDKs, Fused Location Provider, and Java WorkManager APIs.
- Use Firebase Android SDKs through the Firebase BoM for Auth, Firestore (including real-time chat), Storage, AI Logic, App Check, and Remote Config. Add FCM only if the trusted-sender scope in `plan.md` is approved.
- Never embed an unrestricted Gemini/provider key, service-account credential, or unrestricted Maps key in source or an APK.
- Preserve all proposal modules and supporting screens. Any scope or navigation change requires an update to `plan.md` and team approval.
- Add tests and loading/empty/error/offline/permission-denied states with each vertical slice; do not postpone them to the end.

Use current official Android, Firebase, and Google Maps documentation when implementation begins. Exact dependency versions must be verified and locked at that time rather than copied from an old tutorial.
