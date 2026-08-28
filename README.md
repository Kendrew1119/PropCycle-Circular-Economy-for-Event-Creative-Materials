# PropCycle - Circular Economy for Event and Creative Materials

> **Course:** UCCD3223 Mobile Applications Development, June 2026 Trimester<br>
> **University:** Universiti Tunku Abdul Rahman (UTAR)<br>
> **Theme:** UN SDG 12 - Responsible Consumption and Production<br>
> **Team:** Group 7, four members<br>
> **Current phase:** Phase 2G Functional Journey Correction - app/local logic and verification complete; owner live checks pending

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
| Local data | Room 2.8.4/SQLite stores account-scoped scan and recent-activity history; temporary scanner images remain app-private and are deleted after handoff/use |
| Cloud | Firebase Auth, Firestore, and participant-only chat from Phase 2A; AI Logic/App Check from Phase 2B; private marketplace images from Phase 2C.2; lending items, requests, date locks, ratings, chat, and protected images from Phase 2E; Remote Config and FCM remain later work |
| AI | Phase 2B uses the Firebase AI Logic Android SDK for Java with a source-pinned Gemini model and structured response validation |
| Camera/media | Phase 2B uses CameraX and Photo Picker for temporary scanner input; Phase 2C.2 and Phase 2E reuse safe preparation for one optional protected marketplace or lending image |
| Maps/location | Phase 2D uses Maps/Places and one-time Fused Location for Recycling Centre; Phase 2E reuses Maps and one-time foreground location only for privacy-rounded optional lending points |
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

- There is no React Native/Expo application. A native Android Gradle project exists at the repository root. The twenty proposal screens are implemented with Java/XML. Phase 2A makes accounts, marketplace listings, and text chat functional; Phase 2B makes the Scanner and AI Result journey functional; Phase 2C.1 adds owner listing management; Phase 2C.2 adds one optional marketplace image; Phase 2D makes the Recycling Centre map/list functional; Phase 2E completes the app-side lending lifecycle; Phase 2F adds repeatable release-readiness gates; Phase 2G connects the proposal-wide user journeys and replaces remaining mock logic with truthful local/account data.
- The native project currently uses namespace/application ID `com.propcycle.app`. Confirm this is the final team-owned ID before registering the long-lived production Firebase app or signing a release.
- Android Studio and Android SDK Platform 36 are configured locally through ignored `local.properties`. The wrapper is pinned to Gradle 9.5.1 and the project uses Android Gradle Plugin 9.3.0 with Java 17 source compatibility.
- The repository retains planning documents, course documents, project/agent guidance, and generic editor settings alongside the native app.
- Native development uses the repository root; no parallel `android-native/` tree is planned.
- The deleted `.env` is not a credential source. The shared development Firebase project is `propcycle-e5f14`, registered for `com.propcycle.app`; `app/google-services.json` remains ignored, is not stored in the repository, and must be downloaded separately by every authorised developer. Firebase AI Logic and per-developer App Check setup are documented for Phase 2B. Each developer keeps the Phase 2D Maps key in ignored `secrets.properties`; credentials and debug tokens must never be committed.
- The obsolete stack cleanup and native environment bootstrap were committed before this UI milestone.

## Planning documents

- External design source: `C:\Users\B2B\Downloads\Group7-PropCycle.pdf` (inspected but not copied or edited).
- [Master native Android plan](plan.md) - complete scope, screen contract, architecture, data models, security, testing, ownership, migration, and schedule.
- [Proposal planning copy and technology addendum](proposal.md) - the product idea with the revised implementation direction noted separately.
- [Agent/development guardrails](AGENTS.md) - completed Phase 2E boundary and rules for later functional implementation.
- [Teammate setup and run guide](docs/TEAM_SETUP_GUIDE.md) - private-repository access, Android/Firebase setup, build and test commands, device setup, verification, and troubleshooting.
- [AI Smart Scanner setup guide](docs/AI_SCANNER_SETUP.md) - simple Firebase AI Logic, App Check, camera, live-test, privacy, and troubleshooting steps.
- [Marketplace image setup guide](docs/MARKETPLACE_IMAGE_SETUP.md) - detailed Storage enablement, Rules deployment, live checks, and troubleshooting.
- [Recycling Centre map setup guide](docs/RECYCLE_MAP_SETUP.md) - detailed API, billing, restricted-key, SHA-1, live-test, and troubleshooting steps.
- [P2P lending setup guide](docs/LENDING_SETUP.md) - detailed Firebase deployment, two-account lifecycle, privacy, map, image, security, and troubleshooting steps.
- [Phase 2F release checklist](docs/PHASE_2F_RELEASE_CHECKLIST.md) - local/GitHub gates, reviewed Firebase deployment, two-device journeys, AI/Maps checks, signing, hashing, and safe evidence capture.
- [Functional flow and UX logic audit](docs/FUNCTIONAL_FLOW_AUDIT.md) - every proposal screen, the AI-to-form decision, lending item-map logic, cross-module rules, and live checks.

## Completed UI milestone

The user approved proposal-parity UI implementation on 9 August 2026. This milestone covers the twenty distinct screens drawn in the proposal:

1. Welcome, Login, Register, Home, and Recent Activities.
2. AI Smart Scanner, AI Result, Create Marketplace Listing, Recycling Centre, and Lend Resource.
3. Marketplace Browse, Marketplace Item Detail, Conversation, Lending Map Search, Lending List, and Lending Resource Detail.
4. Notifications, Messages, Settings, and User Profile.

The original screen pass used deterministic mock content and local navigation so the full flow could be reviewed before integrations. Phase 2A connects Firebase email/password accounts, Firestore marketplace listings, listing-linked conversation discovery, and participant-only real-time text chat. Phase 2B adds CameraX capture, Android Photo Picker, and authenticated Firebase AI Logic/Gemini analysis to the two scanner screens. Phase 2C.1 adds owner listing management, Phase 2C.2 adds one optional authenticated marketplace image, Phase 2D adds the real Recycling Centre map/list search, and Phase 2E adds functional lending discovery and transactions. Phase 2G adds Room-backed scan/activity history, AI draft/photo handoff, item-first lending map flow, real Home/Profile support data, and honest Settings behavior. Marketplace location, presence, OS push delivery, and payment processing remain deferred.

## Phase 2A Firebase scope

- Register, sign in, cached-session restore, and logout use Firebase Authentication.
- Registration creates a minimal Firestore public profile containing a display name and server timestamps; email remains in Firebase Authentication.
- Marketplace create, browse/search, and detail use `marketplaceListings` snapshot data. This Phase 2A baseline was text-only; Phase 2C.2 now adds the optional image field and secure Storage object.
- Starting a seller chat creates one deterministic listing-linked `chatThreads` document. Messages are immutable, participant-only Firestore documents and the thread preview is updated in the same atomic batch.
- Default-deny Firestore rules, composite indexes, emulator configuration, validation tests, and setup instructions are included.
- Without `app/google-services.json`, the project still builds and Firebase screens show setup-required state instead of fake success.

Phase 2A app-side code and local verification are complete. The project owner must still deliberately deploy the reviewed production Firestore Rules/indexes and complete the documented two-account live smoke test.

## Phase 2B AI scanner scope

- The Scanner screen supports stable CameraX preview/capture and one-image Android Photo Picker selection. Gallery use stays available when camera permission is denied or camera hardware is unavailable.
- A selected image is rotated correctly, bounded, recompressed without original metadata, kept only in app-private temporary cache, transferred only to a user-selected review form, and deleted after use or abandonment. Phase 2G stores the validated text result/activity in Room but never stores the scan image permanently.
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

## Phase 2C.1 marketplace owner scope

- App-side implementation and local verification are complete; production/live
  owner checks remain pending.
- The existing create-listing form is reused for owner-only editing of text fields.
- Owners can withdraw an available listing and relist a withdrawn listing after confirmation.
- Withdrawn listings are hidden from public browse but remain directly visible to their owner.
- Owner ID and creation time remain immutable. Phase 2C.1 originally kept the image field unchanged; Phase 2C.2 now permits only a validated owner listing-image replacement. Permanent listing deletion is still excluded.
- Conflict, offline, permission, authentication, validation, loading, and success states are required, with Security Rules and two-account tests protecting ownership.
- Phase 2A production deployment/live checks and Phase 2B AI/App Check live checks remain pending until the owner records evidence.
- Local evidence: configured debug/release and missing-config debug builds pass;
  51 JVM tests and 12 Firestore Rules tests pass; all 20 navigation destinations
  remain present; lint reports 0 fatal issues and 0 errors.

## Phase 2C.2 marketplace image scope

Phase 2C.2 app-side implementation and local verification are complete. It adds
one optional, bounded marketplace image from
camera or Android Photo Picker, Firebase Cloud Storage upload/replacement,
authenticated image display, owner-only Storage writes, and cleanup of replaced
or failed uploads. Images use private `gs://` references rather than public token
download URLs. Multiple images, scanner prefill, listing deletion, and all
unrelated integrations were deferred in that slice.

Storage enablement, deployment, and the real-device checks are owner tasks. See
the [Marketplace image setup guide](docs/MARKETPLACE_IMAGE_SETUP.md). The app
still builds without local Firebase configuration and shows a clear setup state
instead of pretending an upload worked.

Local evidence: configured debug/release and missing-config debug builds pass;
54 JVM tests and 17 combined Firestore/Storage Rules tests pass; all 68 resource
XML files parse; all 20 navigation destinations remain; lint reports 0 fatal
issues and 0 errors. No emulator was launched for this phase at the user's
request.

## Phase 2D recycling-centre map scope

Phase 2D replaces only the static Recycling Centre page. The screen uses Google
Maps, Places Text Search (New), and one-time foreground Fused Location. Users
can search by city/area without granting location. If the local key or Google
Play services is unavailable, the app shows a setup state and can hand the
manual query to an installed map app instead of displaying fake results.

The page returns at most ten places, requests only the five fields it displays,
sorts by local straight-line distance when a current location is available,
labels that distance approximate, synchronises marker/list selection, and opens
the selected place through a standard geo intent. Location is never stored or
uploaded. Routes, travel time, background location, saved history, verified
material acceptance, lending/marketplace maps, and every other integration stay
deferred.

Every developer must follow the [Recycling Centre map setup guide](docs/RECYCLE_MAP_SETUP.md).
The real restricted key belongs only in ignored `secrets.properties`; the
checked fallback keeps a clean clone buildable and shows a setup message.

Phase 2D local evidence: configured debug/release APKs, the missing-key debug
APK, and the missing-Firebase-plus-missing-key debug APK build successfully; all
60 JVM tests pass; all 69 resource XML files parse; all 20 navigation
destinations remain; lint reports 0 fatal issues and 0 errors (327 project-wide
warnings). No emulator was launched at the user's request. Real Maps/Places
results still require the owner setup and device checklist in the guide.

## Phase 2E P2P equipment lending scope

Phase 2E replaces the static lending pages with an authenticated Firebase
lifecycle. An owner can create, edit, withdraw, and relist an item with one
optional protected JPEG, a required public area, an optional privacy-rounded
map point, maximum borrowing days, pickup method, and an optional informational
deposit. PropCycle does not collect payments.

Available items appear in real time in a searchable list and optional map. A
borrower can request inclusive Malaysia dates; the owner can approve or reject;
deterministic per-day locks prevent overlapping approvals. The existing
Notifications page handles cancellation, pickup, return report, return
confirmation, and one borrower-to-owner rating. The existing participant-only
chat also accepts lending-linked threads.

Firestore and Storage Rules enforce bounded queries, exact field shapes,
ownership, immutable participants, lifecycle transitions, date-lock ownership,
rating eligibility, and owner-only photo writes. See the detailed
[P2P lending setup guide](docs/LENDING_SETUP.md). Production Rules deployment
and the documented two-account/two-device live checks remain deliberate owner
tasks; local verification does not prove live Firebase or Maps behaviour.

Phase 2E local evidence: Firebase-configured debug/release APKs and the
missing-Firebase-plus-missing-Maps-key debug APK build successfully; all 67 JVM
tests and 24 combined Firestore/Storage Rules tests pass; all 122 resource XML
files parse; all 20 navigation destinations remain; lint reports 0 fatal
issues and 0 errors (321 project-wide warnings). No Android emulator was
launched at the user's request.

## Phase 2F release-hardening scope

Phase 2F adds no new user-facing feature. The current assessed feature set is
frozen while the team verifies it. GitHub now checks every push and pull request
to `main` with Java tests, setup-fallback debug/release builds, lint, Firebase
Emulator Security Rules tests, and secret/technology-policy checks. Teammates
can run the matching Windows preflight from the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1
```

The [Phase 2F release checklist](docs/PHASE_2F_RELEASE_CHECKLIST.md) separates
local evidence from owner-only production deployment, two-account/two-device,
live Gemini, real Maps, accessibility, signing, and exact-candidate checks.
Phase 2F is not complete until those release-blocking live rows are recorded.
No Firebase/Maps/App Check/signing secret belongs in GitHub or the repository.

The UI shell currently locks stable AppCompat 1.7.1, Material Components 1.14.0, ConstraintLayout 2.2.2, and AndroidX Navigation 2.9.8. The newly approved visual direction uses the light-colour interface theme, and the Home hamburger controls the three-destination fan for Market, Share, and Map. Final export-quality brand assets can replace the current launcher asset when supplied.

## Build and run

For a new computer or clone, follow the [teammate setup and run guide](docs/TEAM_SETUP_GUIDE.md) first. Open the repository root (the folder containing `settings.gradle` and `gradlew.bat`) in Android Studio, never the `app` subfolder. Run the `app` configuration on an Android device or emulator using Android Studio's bundled JBR and Android SDK Platform 36.

From PowerShell, the verified debug build command is:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

The generated APK is `app\build\outputs\apk\debug\app-debug.apk`. The normal launch begins at Welcome. All drawn screens remain reachable; on Home, the hamburger control opens the approved fan destinations Market, Share, and Map.

No service file or real Maps key is needed to compile or review setup states. Real account, marketplace, chat, marketplace-image, lending, and AI scanner testing requires each authorised teammate's ignored `app/google-services.json`. A real in-app map requires ignored `secrets.properties`. The normal process is in the [teammate guide](docs/TEAM_SETUP_GUIDE.md), the Firebase owner checklist is in [Firebase setup](docs/FIREBASE_SETUP.md), the Storage procedure is in [Marketplace image setup](docs/MARKETPLACE_IMAGE_SETUP.md), the scanner procedure is in [AI Smart Scanner setup](docs/AI_SCANNER_SETUP.md), Maps setup is in [Recycling Centre map setup](docs/RECYCLE_MAP_SETUP.md), lending setup is in [P2P lending setup](docs/LENDING_SETUP.md), and final evidence belongs in the [Phase 2F release checklist](docs/PHASE_2F_RELEASE_CHECKLIST.md).

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

Phase 2A Firebase essentials, the narrow Phase 2B scanner, Phase 2C.1 marketplace owner management, Phase 2C.2 one-image marketplace support, the narrow Phase 2D Recycling Centre map, Phase 2E P2P lending, Phase 2F release hardening, and Phase 2G functional journey correction are authorised. Phase 2G adds account-scoped Room scan/activity history and corrected cross-module handoffs; Phase 2F owner live/signing evidence remains pending for the corrected build. Marketplace location, routes, background location, multiple images, push notifications, trusted automation, payment processing, Remote Config, WorkManager outbox, and other integrations remain on hold until their relevant decisions are approved. No automated source conversion is planned.

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
