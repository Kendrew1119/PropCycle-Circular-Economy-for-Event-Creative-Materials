# PropCycle Native Android Direction

## Current phase: Phase 2G functional journey correction

On 28 August 2026, the user explicitly authorised a complete proposal-wide
functional/UX logic audit and correction before final UI polishing. This is a
release-correctness exception to the earlier Phase 2F feature freeze. It does
not add or remove a top-level module.

The Phase 2G contract is:

- Keep all twenty proposal destinations and the existing Java/XML structure.
- Treat Create Listing and Lend Resource as shared create/edit review forms.
  Recommended entry is camera/Photo Picker through AI Scanner, followed by an
  editable AI-assisted draft; retain manual entry when AI is unavailable.
- Transfer only the app-private processed scanner JPEG to the selected form,
  never auto-publish an AI result, and delete abandoned/consumed handoff files.
- Lending discovery is item-first. List and Map filter Firestore lending items
  by title/category/area and preserve filter state. Current location only sorts
  those items by approximate distance; it never performs a place search.
- Keep Recycling Centre as the only Places search. Do not mix recycling-centre
  locations with lending item locations.
- Use Room 2.8.4 with Java `annotationProcessor` for account-scoped local scan
  and activity history. History must not claim that a centre search means an
  item was recycled, and clearing history must not delete Firebase records.
- Replace static Home/Profile/Recent/Settings claims with real account/local
  data or honest unavailable states. Do not imply dark theme, OS push, payment,
  routes, or other deferred services are active.
- Add pure Java tests and a complete flow audit. Do not launch an emulator at
  the user's request; use builds, JVM tests, lint, XML/navigation, Rules, and
  secret checks, then list real-device checks separately.
- Continue to keep `google-services.json`, `secrets.properties`, API keys, App
  Check tokens, service-account credentials, and keystores outside Git.

Phase 2F local release automation remains in the working milestone, and its
production/live/signing evidence remains pending. Phase 2G changes must be
included in the same final release checks.

## Previous phase: Phase 2F release hardening and live readiness

On 28 August 2026, the user authorised the next phase after Phase 2E. Phase 2F
is a feature freeze and release-readiness slice. Do not add another user-facing
module or external integration in this phase.

The Phase 2F local contract is:

- Add repeatable teammate and GitHub checks for Java unit tests, debug/release
  builds, lint, Firebase Emulator Security Rules tests, XML/navigation
  integrity, Java-first/legacy-stack policy, and tracked-secret protection.
- Keep CI buildable without `google-services.json` and without a Maps key. CI
  must never receive Firebase configuration, API keys, App Check tokens,
  service-account files, signing keystores, or passwords.
- Write one detailed owner checklist for reviewed production Rules/index
  deployment, two-account/two-device journeys, Firebase AI Logic/App Check,
  Maps/Places, accessibility, stability, signed-APK testing, hashes, and safe
  evidence collection.
- Do not deploy Firebase, create or commit a signing key, enable billing/APIs,
  alter cloud settings, or claim live-service success without the project owner
  deliberately completing and recording those checks.
- Do not open an Android emulator at the user's request. Local verification may
  run Gradle, JVM tests, lint, XML/navigation audits, secret checks, and local
  Firebase emulators.
- Treat `PASS` from local automation as local evidence only. Phase 2F remains
  incomplete until the owner records every release-blocking live/device row in
  `docs/PHASE_2F_RELEASE_CHECKLIST.md`.

Phase 2E app-side implementation and local verification are complete. Firebase
production Rules deployment plus the documented two-account/two-device item,
image, request, collision, chat, return, rating, and optional map checks remain
deliberate owner tasks in `docs/LENDING_SETUP.md` and the Phase 2F checklist.

On 28 August 2026, after explicitly requesting a pull of the team's latest UI
work, the user authorised the next planned phase and asked to continue toward a
complete system. Phase 2E makes the existing Lend Resource, Lending List,
Lending Map, Lending Detail, Notifications, Messages, and Conversation surfaces
functional as one bounded Firebase lending lifecycle. Preserve the pulled UI
direction and every completed earlier module.

The completed Phase 2E contract is:

- Use the existing Firebase Auth, Firestore, Storage, Maps, and foreground
  location integrations. Do not introduce a custom backend, payment provider,
  FCM sender, routes, background location, Room, Hilt, or WorkManager.
- Resolve the proposal's "rent or borrow" wording as borrowing with an optional
  informational RM deposit arranged between users. PropCycle never collects a
  fee, deposit, or payment.
- Allow an authenticated owner to publish, edit, withdraw, and relist one
  lending item. One optional protected JPEG may use an owner/item-scoped Cloud
  Storage path. Keep precise private addresses out of public documents.
- Discovery uses authenticated real-time list data. The map shows only items
  that an owner deliberately published with an approximate location. Manual
  title/area search remains usable without location permission or a Maps key.
- Borrow requests use Malaysia calendar dates, inclusive ranges of at most 31
  days, owner approval/rejection, collision-safe booked-day locks, pickup,
  borrower return reporting, owner return confirmation, and one immutable
  borrower-to-owner rating. Integrate these actions into existing screens and
  dialogs; do not add a new top-level module.
- Extend the existing participant-only chat contract to lending items. In-app
  request updates belong on the existing Notifications screen; automated OS
  push remains explicitly unavailable without a trusted sender.
- Enforce ownership, immutable participants, exact field allowlists, state
  transitions, bounded queries, and rating eligibility in Security Rules and
  automated emulator tests. Never rely on hidden buttons for authorisation.
- Keep `google-services.json`, `secrets.properties`, API keys, App Check tokens,
  service-account credentials, and keystores outside Git.
- Do not launch an emulator unless the user later asks. Run JVM tests, Firebase
  Rules tests, debug/release and missing-configuration builds, lint, XML and
  navigation checks, and secret scanning. Document the remaining two-account,
  two-device real-service checks.

Phase 2D app-side implementation and local verification are complete. Google
Cloud billing/API enablement, restricted real-key setup, and the documented
real-device search/permission checks remain deliberate owner tasks.

Phase 2C.2 app-side implementation and local verification are complete.
Production Firestore/Storage Rules deployment and the documented two-account,
two-device live image checks remain deliberate owner tasks.

Phase 2A's app-side implementation and local verification are complete. Email/password accounts, marketplace listings, conversation discovery, and participant-only real-time text chat remain in place. Firebase production Rules/index deployment and the two-account live smoke test remain deliberate owner checks.

Phase 2B's app-side AI scanner implementation and local verification are complete. Firebase AI Logic enablement, authenticated-users enforcement, private debug App Check registration, quota/budget review, and one live Gemini scan remain deliberate owner checks.

On 23 August 2026, the user explicitly authorised Phase 2C.1: extend the existing text-only Firestore marketplace so an owner can edit, withdraw, and relist their own listing. Implement only this marketplace-owner slice. Keep images, Maps/location, lending, persistence, notifications, and every other integration deferred.

Phase 2C.1 app-side implementation and local verification are complete. Production Rules/index deployment plus the documented two-account and two-device live checks remain owner tasks.

On 23 August 2026, the user explicitly authorised Phase 2C.2 Marketplace Images. Implement one optional marketplace image with camera or Android Photo Picker, secure Firebase Cloud Storage upload/replacement, authenticated display, and owner-scoped cleanup. Keep Maps, scanner prefill, listing deletion, lending, notifications, and every other integration deferred.

On 23 August 2026, the user explicitly authorised Phase 2D: make the existing
Recycle Center screen functional with Google Maps, Places Text Search, current
foreground location, and a manual area fallback. Implement only this one map
slice and keep every other proposal screen and module unchanged.

During Phase 2D:

- The obsolete Expo/React Native source, configuration, generated output, dependencies, default assets, and `.env` were removed on 5 August 2026 at the user's explicit direction. Do not restore that stack.
- Preserve the proposal/course documents and planning files that remain in the repository.
- Create the native Gradle project at the repository root; do not create an `android-native/` side project.
- Use the proposal's Recycle Center hierarchy as the structural source of truth: heading and subtitle, prominent map, then a nearby-point list with name, approximate distance, and rating. The user has approved a polished, user-friendly light-theme treatment rather than pixel-perfect reproduction.
- Preserve the completed Phase 2A Firebase Authentication and marketplace/chat behaviour, Phase 2B scanner, Phase 2C.1 owner edit/withdraw/relist behaviour, and Phase 2C.2 marketplace images.
- Keep `google-services.json` ignored and require each developer to obtain it from the correct Firebase project. The app must still compile and present a setup message when the file is absent; it must never fake a successful backend operation.
- Use Maps SDK for Android 20.0.0, Places SDK for Android 5.3.0 (New), and Fused Location Provider 21.4.0. Do not call a raw Maps/Places web service and do not add a custom backend.
- Keep the Maps key outside Git in ignored `secrets.properties`. Commit only a harmless setup-required fallback. The Android manifest may receive the build-injected value, but the real key must have Android package/SHA-1 restrictions and API restrictions for only the enabled Android APIs.
- Request only foreground coarse and fine location together. Accept approximate location, never request background location, never track continuously, and never store or upload precise coordinates.
- Manual area search must remain usable when permission is denied, location is disabled or unavailable, Google Play services are unavailable, or the user prefers not to share location.
- Search only after an explicit user action, allow one Places request at a time, cap results at ten, bias around the current location when available, and request only place ID, display name, formatted address, coordinates, and rating.
- Places has no supported recycling-centre place type. Use a bounded Text Search query for recycling centres instead of inventing type filtering. When location is known, sort locally by straight-line distance and label the value approximate.
- Keep map markers and the nearby list selection in sync. Allow the selected place to open in an installed map app through a standard geo intent. Do not add directions, route drawing, turn-by-turn navigation, or travel-time claims.
- Do not claim that a result accepts the user's material. Show a clear reminder to check with the centre before travelling.
- Show setup-required, Google Play services unavailable, permission-denied, location-unavailable, offline, loading, empty, API/quota/key error, retry, and manual-search states without inventing results.
- Add plain JVM tests for query normalisation, result limits, rating/distance formatting, and distance sorting. At the user's request, do not launch an emulator; verify configured and missing-key builds, tests, lint, XML/navigation integrity, and secret scanning, then provide the manual live checklist.
- Do not add the separate lending map, marketplace location, scanner prefill, saved favourites/history, Room, Hilt, WorkManager, background tracking, geofencing, route APIs, recycling-material acceptance data, photos, FCM/notifications, or any other integration in this slice.
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
