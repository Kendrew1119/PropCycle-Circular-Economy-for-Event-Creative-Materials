# PropCycle Native Android Direction

## Current phase: Phase 2A Firebase essentials

On 9 August 2026, the user explicitly authorised the first functional backend slice after approving the twenty-screen proposal UI. Implement only the Firebase-backed essentials needed for email/password accounts, marketplace listings, conversation discovery, and participant-only real-time text chat. Keep every proposal module and screen, but leave unrelated screens as the existing static UI until their own phase is authorised.

During Phase 2A:

- The obsolete Expo/React Native source, configuration, generated output, dependencies, default assets, and `.env` were removed on 5 August 2026 at the user's explicit direction. Do not restore that stack.
- Preserve the proposal/course documents and planning files that remain in the repository.
- Create the native Gradle project at the repository root; do not create an `android-native/` side project.
- Use the proposal's monochrome wireframes as the visual source of truth. Preserve their hierarchy, proportions, card shapes, labels, and navigation relationships while applying Android insets and accessibility requirements.
- Preserve the proposal-faithful Java/XML UI shell. Functional loading, empty, validation, authentication-required, offline/cache, and error states may be added to the affected screens without redesigning the mock-up.
- Firebase Authentication email/password and Cloud Firestore are authorised. Use the current Firebase Android BoM and the Google services Gradle plugin verified from official documentation.
- Implement user profile creation, marketplace listing create/read/detail, conversation create/list, and participant-only real-time text messages. Use Security Rules, indexes, server timestamps, bounded input validation, and lifecycle-aware listener cleanup.
- Keep `google-services.json` ignored and require each developer to obtain it from the correct Firebase project. The app must still compile and present a setup message when the file is absent; it must never fake a successful backend operation.
- Add Firebase Emulator Suite configuration and a human-readable setup guide. Production deployment remains a deliberate manual step.
- Do not add Cloud Storage or image uploads in this slice; listing images remain placeholders and `imageUrl` stays nullable. Do not add FCM, automated notifications, presence, or a trusted backend.
- Do not add Maps/Places/location, Firebase AI Logic/Gemini, CameraX/Photo Picker, recycling-centre APIs, Room, Hilt, WorkManager, lending booking/rating logic, or any other API/integration yet. Their existing proposal screens remain static and visibly deferred.
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
