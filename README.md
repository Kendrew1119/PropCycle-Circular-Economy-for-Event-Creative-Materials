# PropCycle - Circular Economy for Event and Creative Materials

> **Course:** UCCD3223 Mobile Applications Development, June 2026 Trimester<br>
> **University:** Universiti Tunku Abdul Rahman (UTAR)<br>
> **Theme:** UN SDG 12 - Responsible Consumption and Production<br>
> **Team:** Group 7, four members<br>
> **Current phase:** Phase 2B AI Smart Scanner with CameraX, Photo Picker, and Firebase AI Logic

PropCycle helps campus event organisers, creative makers, cosplayers, toy miniaturists, and DIY communities keep useful materials and equipment in circulation. A user can identify a material with Gemini-assisted scanning, sell/donate/exchange leftover materials, find a recycling centre, or lend and borrow equipment through one connected Android application.

## Current decision

The implementation target has changed from React Native with Expo to a clean native Android application.

| Area | Selected planning direction |
|---|---|
| Platform | Android only |
| Application code | Java 17 |
| UI | Android Views with XML and Material Components |
| Navigation | One Activity, Fragments, and AndroidX Navigation |
| State/architecture | ViewModel, LiveData, SavedStateHandle, repositories, optional use cases |
| Local data | Phase 2B uses temporary app-private scanner cache only; Room and Preferences DataStore remain later work |
| Cloud | Firebase Auth and Firestore from Phase 2A; Phase 2B adds App Check only for Firebase AI Logic; Storage, Remote Config, and FCM remain later work |
| AI | Phase 2B uses the Firebase AI Logic Android SDK for Java with a source-pinned Gemini model and structured response validation |
| Camera/media | Phase 2B uses CameraX capture and Android Photo Picker; Cloud image upload remains deferred |
| Maps/location | Maps SDK for Android, Places SDK for Android, Fused Location Provider |
| Background sync | WorkManager Java Workers |
| Build | Gradle with Groovy scripts, JDK 17, API 36 compile/target, API 24 minimum |

Jetpack Compose, React Native, and Expo are not part of the target architecture. No team-authored Kotlin application source is planned. Kotlin may exist only as a transitive/generated tooling dependency or through a separately approved exception.

## Retained product scope

The technology change does not reduce product scope.

The three core modules remain:

1. **AI Smart Scanner** - camera/gallery input, structured material result, Malaysian recycling/upcycling guidance, editable review, local cache/history, and handoff to Recycle, Marketplace, or Lend.
2. **Material Marketplace** - browse, search/filter, listing details, create/edit, sale/donation/exchange intent, pickup/meeting arrangements, status handling, and seller chat.
3. **P2P Equipment Lending** - map and list discovery, availability, date request, owner approval, pickup/return, rating/trust, and direct owner chat.

Supporting capabilities also remain: Welcome, Login, Register, Home Dashboard, Recycling Centres, Recent Activities, Notifications, Messages, Conversation, Profile, Settings, and the prose-required scan-history/booking/return/rating flows.

The supplied proposal PDF is the authority for the twenty drawn screens and their visual flow. Functional sub-screens not fully drawn in the proposal use the same approved design system and do not create new top-level modules. Earlier-plan ideas such as a daily tip, community statistics, text-only scanner lookup, or a marketplace map are optional enhancements pending designer/team approval; they are not proposal-parity requirements.

## Important scope boundaries

- PropCycle displays prices and optional lending deposits but does not process payments.
- Marketplace and lending arrangements are completed between users, primarily through text chat.
- Location is foreground-only, approximate location is accepted, and manual location entry is available.
- AI results are estimates that the user reviews before saving or publishing.
- A recycling-centre result does not guarantee material acceptance; the user must confirm with the centre.
- The assessed signed APK is the first release target. Store distribution is reviewed separately, especially for Huawei devices without Google Mobile Services.

## Repository status

At the user's explicit direction, the obsolete Expo/React Native implementation was removed from the working tree on 5 August 2026. This includes the Node dependency tree, Expo/Node configuration, TS/TSX application source, generated output, placeholder database/service files, default Expo imagery/licence boilerplate, and the old `.env`.

- There is no React Native/Expo application. A native Android Gradle project exists at the repository root. The twenty proposal screens are implemented with Java/XML. Phase 2A makes accounts, marketplace listings, and text chat functional; Phase 2B makes only the Scanner and AI Result journey functional.
- The native project currently uses namespace/application ID `com.propcycle.app`. Confirm this is the final team-owned ID before registering the long-lived production Firebase app or signing a release.
- Android Studio and Android SDK Platform 36 are configured locally through ignored `local.properties`. The wrapper is pinned to Gradle 9.5.1 and the project uses Android Gradle Plugin 9.3.0 with Java 17 source compatibility.
- The repository retains planning documents, course documents, project/agent guidance, and generic editor settings alongside the native app.
- Native development uses the repository root; no parallel `android-native/` tree is planned.
- The deleted `.env` is not a credential source. The shared development Firebase project is `propcycle-e5f14`, registered for `com.propcycle.app`; `app/google-services.json` remains ignored, is not stored in the repository, and must be downloaded separately by every authorised developer. Firebase AI Logic and per-developer App Check setup are documented for Phase 2B. Maps and every other new integration remain deferred, and credentials or debug tokens must never be committed.
- The obsolete stack cleanup and native environment bootstrap were committed before this UI milestone.

## Planning documents

- External design source: `C:\Users\B2B\Downloads\Group7-PropCycle.pdf` (inspected but not copied or edited).
- [Master native Android plan](plan.md) - complete scope, screen contract, architecture, data models, security, testing, ownership, migration, and schedule.
- [Proposal planning copy and technology addendum](proposal.md) - the product idea with the revised implementation direction noted separately.
- [Agent/development guardrails](AGENTS.md) - current Phase 2B scanner boundary and rules for later functional implementation.
- [Teammate setup and run guide](docs/TEAM_SETUP_GUIDE.md) - private-repository access, Android/Firebase setup, build and test commands, device setup, verification, and troubleshooting.
- [AI Smart Scanner setup guide](docs/AI_SCANNER_SETUP.md) - simple Firebase AI Logic, App Check, camera, live-test, privacy, and troubleshooting steps.

## Completed UI milestone

The user approved proposal-parity UI implementation on 9 August 2026. This milestone covers the twenty distinct screens drawn in the proposal:

1. Welcome, Login, Register, Home, and Recent Activities.
2. AI Smart Scanner, AI Result, Create Marketplace Listing, Recycling Centre, and Lend Resource.
3. Marketplace Browse, Marketplace Item Detail, Conversation, Lending Map Search, Lending List, and Lending Resource Detail.
4. Notifications, Messages, Settings, and User Profile.

The original screen pass used deterministic mock content and local navigation so the full flow could be reviewed before integrations. Phase 2A connects Firebase email/password accounts, Firestore marketplace listings, listing-linked conversation discovery, and participant-only real-time text chat. Phase 2B adds CameraX capture, Android Photo Picker, and authenticated Firebase AI Logic/Gemini analysis to the two scanner screens. Google Maps/Places, Cloud Storage/image upload, Room/scan history, lending transactions, presence, and notification delivery remain deferred.

## Phase 2A Firebase scope

- Register, sign in, cached-session restore, and logout use Firebase Authentication.
- Registration creates a minimal Firestore public profile containing a display name and server timestamps; email remains in Firebase Authentication.
- Marketplace create, browse/search, and detail use `marketplaceListings` snapshot data. Listing photos remain placeholders and Cloud Storage is not enabled.
- Starting a seller chat creates one deterministic listing-linked `chatThreads` document. Messages are immutable, participant-only Firestore documents and the thread preview is updated in the same atomic batch.
- Default-deny Firestore rules, composite indexes, emulator configuration, validation tests, and setup instructions are included.
- Without `app/google-services.json`, the project still builds and Firebase screens show setup-required state instead of fake success.

Phase 2A app-side code and local verification are complete. The project owner must still deliberately deploy the reviewed production Firestore Rules/indexes and complete the documented two-account live smoke test.

## Phase 2B AI scanner scope

- The Scanner screen supports stable CameraX preview/capture and one-image Android Photo Picker selection. Gallery use stays available when camera permission is denied or camera hardware is unavailable.
- A selected image is rotated correctly, bounded, recompressed without original metadata, kept only in app-private temporary cache, and deleted after use. Phase 2B does not add Cloud Storage or permanent scan history.
- A signed-in user sees a disclosure before one processed image is sent to Firebase AI Logic using the Gemini Developer API. No raw Gemini API key is created, requested, or embedded.
- Structured output is validated before the AI Result screen displays it. Confidence is labelled as an uncalibrated model estimate, and the user reviews the result before using an existing next action.
- Missing Firebase setup, signed-out, permission-denied, offline, malformed-response, retry, and one-request-at-a-time states are part of this slice.
- Firebase AI Logic has no Local Emulator Suite emulator. The structured parser and image-bound calculations use local unit tests; service failure paths and a real request are deliberate manual tests with project quota.
- Builds installed with `-PuseFirebaseEmulators=true` deliberately block live AI analysis. Install a normal debug build only when performing the documented live scanner test.
- Debug builds use only the App Check debug provider, and each developer registers their own private debug token. Release builds use the Play Integrity provider, but the project owner must still register the release SHA-256/provider and verify the exact signed APK.

The Phase 2B app-side implementation and local checks are complete. A live
Gemini result is not claimed yet: the Firebase owner must finish the AI Logic,
authenticated-users, App Check debug-token, quota/budget, and one-request steps
in the [AI Smart Scanner setup guide](docs/AI_SCANNER_SETUP.md).

The UI shell currently locks stable AppCompat 1.7.1, Material Components 1.14.0, ConstraintLayout 2.2.2, and AndroidX Navigation 2.9.8. The newly approved visual direction uses the light-colour interface theme, and the Home hamburger controls the three-destination fan for Market, Share, and Map. Final export-quality brand assets can replace the current launcher asset when supplied.

## Build and run

For a new computer or clone, follow the [teammate setup and run guide](docs/TEAM_SETUP_GUIDE.md) first. Open the repository root (the folder containing `settings.gradle` and `gradlew.bat`) in Android Studio, never the `app` subfolder. Run the `app` configuration on an Android device or emulator using Android Studio's bundled JBR and Android SDK Platform 36.

From PowerShell, the verified debug build command is:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

The generated APK is `app\build\outputs\apk\debug\app-debug.apk`. The normal launch begins at Welcome. All drawn screens remain reachable; on Home, the hamburger control opens the approved fan destinations Market, Share, and Map.

No service file is needed to compile or review deferred static screens. Real account, marketplace, chat, and AI scanner testing requires each authorised teammate's ignored `app/google-services.json`. The normal process is in the [teammate guide](docs/TEAM_SETUP_GUIDE.md), the Firebase owner checklist is in [Firebase setup](docs/FIREBASE_SETUP.md), and the complete scanner procedure is in [AI Smart Scanner setup](docs/AI_SCANNER_SETUP.md).

## Later implementation gate

Before the remaining production behaviour is added, the team still confirms:

- all proposal modules and screens are represented correctly;
- Java/XML/no-Compose direction;
- API 24 minimum and API 36 target;
- final Android namespace/application ID;
- final copy and recovery/account-management additions around the implemented email/password flow;
- remaining designer tokens and export-quality brand assets;
- Firebase development/production setup and push-notification sender;
- release Play Integrity/project-Owner/SHA-256 setup for App Check, or a documented decision that the AI scanner is not release-ready; the debug provider/token is never a release fallback;
- team ownership, remaining dates, testing, and release gates;
- remaining marketplace, lending-fee, and optional-enhancement decisions.

Phase 2A Firebase essentials and the narrow Phase 2B CameraX/Photo Picker/Firebase AI Logic scanner are authorised. Map/location, recycling-centre APIs, Cloud Storage, permanent scan history/local persistence, lending workflows, push notifications, trusted automation, Remote Config, and other integrations remain on hold until their relevant decisions are approved. No automated source conversion is planned.

## Team ownership

| Workstream | Primary responsibility |
|---|---|
| Member A - Lead/Core | Native project, navigation, auth, permissions, maps/location, integration, signing/release |
| Member B - UI/UX | XML design system, screen styling, reusable Views, Home fan navigation, accessibility, icon/screenshots |
| Member C - AI/Local | CameraX, Photo Picker, Firebase AI Logic, scan workflow, Room/cache/outbox |
| Member D - Cloud/Exchange | Firebase data/rules, marketplace, lending, chat, notifications, ratings, emulator tests |

Every feature still requires peer review, test evidence, designer comparison, and contribution notes for the final report and Q&A.

## Licence

This project is developed for academic evaluation under UTAR course UCCD3223 Mobile Applications Development. All rights are reserved by the group members.
