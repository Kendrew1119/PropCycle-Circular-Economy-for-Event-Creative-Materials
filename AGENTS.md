# PropCycle Native Android Direction

## Current phase: Phase 2B AI smart scanner

Phase 2A's app-side implementation and local verification are complete. Email/password accounts, marketplace listings, conversation discovery, and participant-only real-time text chat remain in place. Firebase production Rules/index deployment and the two-account live smoke test remain deliberate owner checks.

On 12 August 2026, the user explicitly authorised the next narrow slice: make the existing AI Smart Scanner and AI Result wireframes functional with CameraX capture, Android Photo Picker, and Firebase AI Logic using Gemini. Implement only this scanner slice. Keep every other proposal module and screen unchanged until its own phase is authorised.

During Phase 2B:

- The obsolete Expo/React Native source, configuration, generated output, dependencies, default assets, and `.env` were removed on 5 August 2026 at the user's explicit direction. Do not restore that stack.
- Preserve the proposal/course documents and planning files that remain in the repository.
- Create the native Gradle project at the repository root; do not create an `android-native/` side project.
- Use the proposal's monochrome wireframes as the visual source of truth. Preserve their hierarchy, proportions, card shapes, labels, and navigation relationships while applying Android insets and accessibility requirements.
- Preserve the proposal-faithful Java/XML UI shell. Functional loading, empty, validation, authentication-required, offline/cache, and error states may be added to the affected screens without redesigning the mock-up.
- Preserve the completed Phase 2A Firebase Authentication, Cloud Firestore marketplace, and participant-only chat implementation.
- Keep `google-services.json` ignored and require each developer to obtain it from the correct Firebase project. The app must still compile and present a setup message when the file is absent; it must never fake a successful backend operation.
- Use stable CameraX preview and image capture plus Android Photo Picker. Request only the camera permission and keep gallery selection usable when camera access is denied or unavailable.
- Normalize one selected image into a bounded, orientation-corrected, metadata-stripped temporary JPEG in app-private cache. Delete temporary scanner files after use. Do not add Cloud Storage or permanent scan history.
- Use Firebase AI Logic with the Gemini Developer API through the Firebase Android SDK. Do not embed or request a raw Gemini API key. Require a configured Firebase app and signed-in user, show a disclosure before transmission, allow one request at a time, validate structured output, and label confidence as an uncalibrated model estimate.
- Use the App Check debug provider only in debug builds and Play Integrity only in release builds. Each developer registers their own debug token; no token is committed.
- Preserve the scanner and result wireframe hierarchy while adding permission, loading, setup-required, authentication-required, offline, malformed-response, retry, and review states.
- Add a simple, detailed AI scanner setup guide. Firebase AI Logic is not emulated, so automated tests must use local fakes/parsers and live requests remain a deliberate manual test.
- Do not add Maps/Places/location, recycling-centre APIs, Room/scan history, Hilt, WorkManager, lending booking/rating logic, Cloud Storage/image upload, FCM, automated notifications, presence, Remote Config, or any other API/integration in this slice. Their existing proposal screens remain static and visibly deferred.
- Keep all twenty drawn screens reachable for review. Do not add the undrawn booking, return, rating, scan-history, or account-detail surfaces until their layouts are planned and separately scheduled.
- Treat the external `C:\Users\B2B\Downloads\Group7-PropCycle.pdf` as the UI/module source of truth and `plan.md` as the implementation-planning source of truth.

## Direction for later functional implementation

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
