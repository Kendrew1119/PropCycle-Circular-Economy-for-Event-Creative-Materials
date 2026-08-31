# PropCycle Native Android Master Plan

> **Planning baseline:** Group7-PropCycle proposal dated 15 July 2026<br>
> **Re-planning date:** 5 August 2026<br>
> **Course:** UCCD3223 Mobile Applications Development, June 2026 Trimester<br>
> **Team:** Group 7, four members<br>
> **Status:** Phase 2A through Phase 2G app/local slices complete; Phase 2F production/live/signing evidence remains pending<br>
> **Target platform:** Android only<br>
> **Primary implementation language:** Java

This document is the single source of truth for the native Android implementation. The proposal's modules and designer mock-ups remain the product baseline. The technology changes from React Native with Expo to a native Android application; it does not remove any product module.

## 1. Decision summary

### Non-negotiable scope rules

1. Retain every proposal module and supporting screen.
2. Treat the proposal mock-ups as the screen and flow authority.
3. Use native Android Views with XML layouts; do not use Jetpack Compose.
4. Write team-owned Android application source in Java.
5. Keep the twenty drawn proposal screens. Phase 2A replaces mock data for email/password authentication, user-profile creation, basic marketplace listings, conversation discovery, and participant-only real-time text chat. Phase 2B makes the Scanner and AI Result screens functional. Phase 2C.1 extends marketplace owner editing and `available`/`withdrawn` status management. Phase 2C.2 adds one optional marketplace image. Phase 2D makes the existing Recycling Centre map/list screen functional. Phase 2E completes the app-side lending lifecycle. Phase 2F adds release gates. Phase 2G corrects cross-module journeys and supporting-screen logic without changing the module hierarchy, including the later-authorised built-in demo-image fallback for Marketplace and Lending.
6. Keep the obsolete Expo/React Native stack deleted as explicitly directed on 5 August 2026; do not restore it or create a side-by-side legacy tree.
7. Do not embed unrestricted Gemini, Firebase Admin, or Google Maps credentials in the application.

### Technology decision record

| Decision | Selected planning direction | Reason |
|---|---|---|
| Platform | Native Android | Matches the revised requirement and removes the React Native/Expo runtime |
| UI | Android Views, XML, Material Components | Full Java interoperability and direct control over the designer's layouts |
| App language | Java 17 | Java-first development with modern language support |
| Kotlin policy | No team-authored Kotlin application source is planned | Keeps the codebase aligned with the team's Java preference |
| Compose policy | Not permitted in this implementation | Compose is Kotlin-first and would create two UI systems |
| Build scripts | Gradle with Groovy DSL | Avoids Kotlin build scripts and keeps configuration familiar |
| Android baseline | `minSdk 24`, `compileSdk 36`, `targetSdk 36` | Android 7+ device reach and the 2026 target API requirement |
| App architecture | Single Activity, Fragments, Navigation Component, MVVM-style state holders and repositories | Clear lifecycle handling, testability, and one navigation graph |
| Local database | Room 2.8.4 over SQLite using Java `annotationProcessor`, authorised in Phase 2G | Compile-time SQL checks for account-scoped scan/activity history while keeping the source Java-first; Room 3 is excluded because it requires Kotlin/KSP |
| Cloud data | Firebase Authentication and Cloud Firestore; Phase 2C.2 adds a narrow Firebase Cloud Storage marketplace path | Firestore snapshot listeners provide real-time chat; marketplace media uses authenticated `gs://` references while Phase 2B scanner images are never stored in the cloud |
| AI | Phase 2B: Firebase AI Logic Android SDK for Java, exact source-pinned Gemini model, and App Check; Remote Config later | Keeps a raw Gemini key out of the client and protects the currently authorised scanner without opening another integration |
| Camera/media | CameraX and Android Photo Picker for personal input; 12 packaged demo illustrations for Marketplace and Lending | Personal inputs remain app-private until explicit save; an allowlisted demo key displays an APK resource without a Cloud Storage upload |
| Maps/location | Phase 2D: Maps SDK for Android 20.0.0, Places SDK for Android 5.3.0 (New), and Fused Location Provider 21.4.0 | Native recycling-centre map and explicit Text Search; listing/lending maps remain later work |
| Background work | WorkManager Java `Worker` | Reliable draft/upload retry and deferred sync |
| Dependency injection | Hilt with Java annotation processing | Consistent construction, compile-time checks, and test replacement |
| Distribution | Signed APK for course submission; AAB/store release after compatibility review | Separates the assessed deliverable from optional store publication |

Version numbers other than the Android API baseline are resolved and locked at project bootstrap. Only stable releases compatible with JDK 17, Java source, and the chosen Android Gradle Plugin may be used.

### Authorised UI-first milestone - 9 August 2026

The user has approved implementation of all twenty distinct screens drawn in the proposal. This is a reviewable, offline UI prototype rather than a claim that production features are complete.

| Included now | Deferred until a later milestone |
|---|---|
| Java Activity/Fragment shell, XML layouts, shared styles/drawables, AndroidX Navigation, local click paths, and deterministic mock content | Firebase Auth/Firestore/Storage/FCM, Firebase AI Logic/Gemini, Google Maps/Places/location, CameraX/Photo Picker, Room, Hilt, WorkManager, networking, credentials, uploads, persistence, and trusted automation |
| Welcome, Login, Register, Home, Recent Activities | Real authentication, validation against a service, session state, and persisted activity history |
| Scanner, AI Result, Create Listing, Recycling Centre, Lend Resource | Camera/gallery input, AI inference, real maps/centres, publishing, and data validation |
| Marketplace Browse/Detail, Conversation, Lending Map/List/Detail | Search, real inventory, real-time chat, availability, booking, return, rating, and payments |
| Notifications, Messages, Settings, Profile | Push delivery, real conversations, persisted/system-backed settings, accounts, transaction trust, and aggregate statistics; reward points and achievement badges are excluded |

UI fidelity rules for this milestone:

- Keep the PDF's hierarchy, handwritten/cursive-style headings where shown, rounded cards, dashed/outlined containers, large image/map placeholders, and annotated three-action Home fan. Apply the approved soft, light eco-colour theme without changing that structure.
- Keep all twenty drawn screens reachable through local navigation. On Home, the existing hamburger toggles the bottom-right Market/Share/Map fan. Any undrawn menu used elsewhere remains a review aid and must not replace visible wireframe content.
- Use static sample labels and values where the mock-up uses `Text` or `XXXX`; this makes hierarchy legible without inventing backend behaviour.
- The repeated lending and marketplace chat mock-up maps to one reusable Conversation UI.
- Undrawn completion surfaces such as booking dates, returns, ratings, scan history, and account details remain in the product plan but are not part of this exact-wireframe UI pass.
- UI controls may navigate locally or change visual state, but must not suggest that a network/API/device operation succeeded.

### UI milestone implementation status - 9 August 2026

| Deliverable | Status | Evidence |
|---|---|---|
| Planning/guardrail update | Complete | `plan.md`, `README.md`, `proposal.md`, and `AGENTS.md` distinguish the authorised UI milestone from deferred functional work |
| Native UI shell | Complete | Java Activity/Fragment host, AndroidX Navigation graph, Android insets, shared accessible light-theme resources, Home navigation fan, review menu, and provisional launcher icon |
| Twenty drawn proposal screens | Complete | Five auth/home, five scan/handoff, six marketplace/lending/communication, and four user/support layouts |
| Local prototype navigation | Complete | Welcome → Login → Home → Scanner → AI Result is emulator-verified; every destination is also reachable through its planned controls or the review menu |
| Backend/device integrations at UI-milestone close | Deferred at that checkpoint | Phase 2A subsequently authorises only Firebase Auth/Firestore for accounts, marketplace, and text chat; all other integrations remain deferred |
| Verification | Complete for this UI milestone | Debug APK build, Android lint, resource/navigation audit, 20-screen emulator capture review, and no AndroidRuntime crash during launch |
| Phase 2A Firebase app slice | App-side implementation and local verification complete | Java/Auth/Firestore implementation, default-deny rules, composite indexes, emulator configuration, and developer guide are present. Both configured and missing-config builds, unit tests, lint, Rules tests, and twenty-screen emulator review pass |
| Phase 2A owner checks | Deliberately pending | The Firebase owner still deploys the reviewed production Rules/indexes and completes the documented two-account live smoke test; local success is not reported as production deployment evidence |
| Phase 2B scanner app slice | App-side implementation and local verification complete | Configured debug/release builds and the missing-config debug build pass; all 47 JVM tests pass; lint has 0 fatal issues and 0 errors (327 warnings remain across the full project); emulator checks pass for preview, capture, private processing, Photo Picker, camera denial fallback, consent, signed-out blocking, and invalid-result handling |
| Phase 2B owner/live checks | Deliberately pending | The owner still enables Firebase AI Logic, enforces authenticated-users mode, registers a private debug App Check token, reviews quota/budget settings, and completes one deliberate live Gemini request using `docs/AI_SCANNER_SETUP.md` |
| Phase 2C.1 marketplace owner slice | App-side implementation and local verification complete | Configured debug/release and missing-config debug builds pass; all 51 JVM tests and 12 Firestore Rules tests pass; 20 navigation destinations remain; resource XML parses; lint has 0 fatal issues and 0 errors (331 project-wide warnings); no emulator was launched at the user's request |
| Phase 2C.1 live checks | Deliberately pending until handoff | Deploy the reviewed Rules/indexes, then use two real accounts to verify owner/non-owner actions, public hiding after withdrawal, relisting, conflict handling, and chat availability |
| Phase 2C.2 marketplace image slice | App-side implementation and local verification complete | Configured debug/release and missing-config debug builds pass; 54 JVM tests and 17 combined Firestore/Storage Rules tests pass; all 68 resource XML files parse; 20 navigation destinations remain; lint has 0 fatal issues and 0 errors (351 project-wide warnings); no emulator was launched at the user's request |
| Phase 2C.2 owner/live checks | Deliberately pending until handoff | Confirm the default Storage bucket/Blaze billing, deploy reviewed Firestore and Storage Rules, then verify create/display/replace/cleanup with two accounts and two devices using `docs/MARKETPLACE_IMAGE_SETUP.md` |
| Phase 2D recycling-centre map | App-side implementation and local verification complete | Current fixed SDKs compile; configured debug/release and missing-key/missing-Firebase debug builds pass; all 60 JVM tests pass; all 69 resource XML files parse; 20 navigation destinations remain; lint has 0 fatal issues and 0 errors (327 project-wide warnings); no emulator was launched |
| Phase 2D owner/live checks | Deliberately pending until handoff | Enable billing/APIs, create and restrict an Android key, then verify real results, permission choices, map/list selection, and geo-intent handoff using `docs/RECYCLE_MAP_SETUP.md` |
| Phase 2E P2P lending | App-side implementation and local verification complete | Functional creation/edit/status, protected image, real-time list/map, request/date locks, chat, in-app request actions, return, and rating are covered by Java tests, Firebase Emulator Rules tests, build/lint/XML/navigation/secret checks; production deployment and real-device checks remain pending |
| Phase 2E owner/live checks | Deliberately pending until handoff | Deploy the reviewed Firestore/Storage Rules, then complete the two-account/two-device lifecycle and optional map checks in `docs/LENDING_SETUP.md` |
| Phase 2F release hardening | In progress | GitHub quality checks, a Windows release-readiness preflight, and one consolidated owner live/device/signing checklist are implemented locally; no production deployment or live-service success is claimed |
| Phase 2F completion gate | Deliberately pending | Project owner records green CI, reviewed production Rules/index deployment, two-account/two-device journeys, one protected live Gemini scan, Maps checks, accessibility/stability results, and exact signed-APK evidence in `docs/PHASE_2F_RELEASE_CHECKLIST.md` |
| Phase 2G functional journey correction | App/local complete | AI result/photo handoff to editable Marketplace/Lending forms, visible transaction intent/all categories, item-first lending list/map state, Room scan/activity history, and truthful Home/Profile/Recent/Settings logic are implemented locally. The combined debug/release build, 72 JVM tests, lint, 123-resource XML/20-destination navigation audit, Firebase Emulator Security Rules tests, secret checks, and missing-Firebase-configuration build pass without launching an emulator. |
| Phase 2G built-in demo-image fallback | App/local complete | Marketplace and Lending create/edit forms offer 12 allowlisted packaged illustrations. Firestore stores only `demoImageKey`; list/detail surfaces resolve the same local resource; a demo image and Storage URL cannot coexist. Build, lint, Java tests, Rules tests, XML/navigation, and secret checks pass without launching an emulator. |
| Phase 2G owner/live checks | Deliberately pending | Complete the cross-module camera/Photo Picker handoff, two-account marketplace/lending, account-isolated offline history, permission fallback, live Gemini, and real Maps checks in `docs/FUNCTIONAL_FLOW_AUDIT.md` and the Phase 2F release checklist |

### Authorised Phase 2A Firebase essentials - 9 August 2026

The user has now authorised a deliberately narrow backend vertical slice. It makes the account, marketplace, and text-chat journeys real while avoiding credentials or services needed by later modules.

| Implement in Phase 2A | Explicitly defer |
|---|---|
| Firebase email/password register, login, cached-session restore, logout, and an owner-only Firestore user-profile document | Phone/username/social authentication, account editing/deletion, avatars, reward points, achievement badges, and aggregate statistics |
| Firestore marketplace listing create, authenticated real-time browse/search, and listing detail; text-only fields with a nullable image URL | Cloud Storage, image capture/picking/upload, listing edit/status workflow, payment, pickup fulfilment, and trusted automation |
| Deterministic listing-linked conversation creation, participant conversation list, and participant-only real-time text messages | Presence/online status, typing/read receipts, attachments, moderation backend, FCM push, and automated notifications |
| Security Rules, required composite indexes, Firebase Emulator Suite configuration, client input validation, loading/empty/error/offline states, and setup documentation | Firebase AI Logic/Gemini, Maps/Places/location, recycling APIs, CameraX, Room, Hilt, WorkManager, lending booking/return/rating, and all other APIs |

Phase 2A implementation rules:

- Use only Java source, Android Views/XML, the Firebase Android BoM, `firebase-auth`, and `firebase-firestore`; do not add KTX artifacts.
- Keep `google-services.json` outside Git. A missing file must not break compilation: affected screens show a Firebase setup message and never report a false success.
- Use Firebase server timestamps and Firestore snapshot listeners. Remove listeners with the owning ViewModel/Fragment lifecycle.
- Require authentication for all Phase 2A Firestore reads and writes. A user may update only their own profile; authenticated users may directly read a display-name profile for seller/chat identity, but profile collection enumeration is denied. Marketplace records carry an immutable owner UID, and only conversation participants may list/read/write chat data.
- Listing media remains the proposal placeholder. `imageUrl` is nullable and no upload path is opened in this phase.
- The existing scanner, AI result, recycle, map, lending, notifications, settings, recent-activity, and profile-support surfaces remain reviewable static UI unless a small account/session label is needed.
- Development project `propcycle-e5f14`, Android registration, matching `app/google-services.json`, and Singapore Firestore are present. Email/Password remains a required console setting. The recorded pending Phase 2A owner checks are production Rules/index deployment and the two-account live smoke test.

Phase 2A app-side implementation and local verification are complete. Production
Rules/index deployment and the two-account cloud smoke test remain deliberate
owner checks in `docs/FIREBASE_SETUP.md`; this plan does not infer that a local
test changed the production project.

### Authorised Phase 2B AI Smart Scanner - 12 August 2026

The user has authorised only the existing `SCAN-01` to `SCAN-02` journey. This
slice adds device image input and one authenticated Gemini analysis without
starting another proposal module.

| Implement in Phase 2B | Explicitly defer |
|---|---|
| Stable CameraX preview and one-photo capture; Android Photo Picker for one gallery image; camera permission only | Microphone, video, continuous image analysis, broad storage/media permission, background capture, and multi-image scanning |
| Bounded decode, EXIF-correct orientation, metadata-stripped temporary JPEG, app-private cache, and cleanup after use | Cloud Storage, permanent scan image, Room, scan history, offline result cache, draft/outbox, and WorkManager |
| Firebase AI Logic Android SDK through the Gemini Developer API, exact source-pinned `gemini-3.6-flash` model, structured JSON response, and strict local validation | Raw Gemini/AI Studio key, direct REST integration, Vertex AI switch, Remote Config, custom backend, and abuse-resistant per-user quotas |
| Signed-in-user gate, transmission disclosure, one request at a time, loading/setup/auth/offline/error/retry states, and result review | Automatic publish, AI image upload, Marketplace prefill, recycling-centre API, maps/location, and lending backend behaviour |
| Debug App Check provider only in debug builds and Play Integrity provider only in release builds | Committed debug token, debug provider in release, or a claim that adding the SDK completes Play Console/SHA-256/release attestation setup |

Phase 2B implementation rules:

- Preserve the Scanner and AI Result wireframe hierarchy, proportions, labels,
  and existing Recycle/Marketplace/Lend relationships while adding only the
  states needed for a real scan.
- Camera hardware is optional. Request `CAMERA` only when capture is selected;
  gallery selection remains available after denial or when no camera exists.
  Android Photo Picker does not require broad storage permission.
- Copy one selected input into app-private cache, decode it off the main thread,
  validate type/dimensions, rotate from EXIF, bound dimensions and bytes,
  recompress it as a metadata-stripped JPEG, and remove temporary scanner files
  after completion, cancellation, or failure.
- Require a configured Firebase app and a current Firebase Authentication user.
  Show the disclosure before transmission. Never fake a successful AI result.
- Use the Firebase AI Logic SDK rather than a raw provider key. Build one
  structured multimodal request, validate every response field and enum, reject
  malformed or overlong output safely, and label confidence as an uncalibrated
  model estimate.
- Allow only one live request at a time and cancel or ignore obsolete work with
  the owning lifecycle. Do not log images, tokens, raw responses, or private
  account data.
- Firebase AI Logic is not emulated. Use parser and image-bound unit tests for
  repeatable local checks, test service failure paths manually, and reserve a
  real shared-project request for the manual test in
  `docs/AI_SCANNER_SETUP.md`.
- When `useFirebaseEmulators` is enabled for local Auth/Firestore testing, block
  live AI analysis. A deliberate live scanner test uses a normal configured
  debug build, a cloud-authenticated test user, and valid App Check.
- Each developer registers their own private App Check debug token. The release
  Play Integrity provider, release SHA-256, Play/Firebase project link, exact
  signed-APK test, quotas, and budget alerts remain deliberate owner checks.
- Maps/Places/location, recycling APIs, Cloud Storage, Room/history, Remote
  Config, Hilt, WorkManager, FCM/notifications, lending workflows, and every
  other API/module stay unchanged and deferred.

### Authorised Phase 2C.1 Marketplace Owner Management - 23 August 2026

The user has authorised a narrow continuation of the existing text-only
marketplace. This slice uses the current Firebase Auth/Firestore configuration
and starts no new external service.

| Implement in Phase 2C.1 | Explicitly defer |
|---|---|
| Reuse Create Listing as an owner-only edit form for title, description, category, condition, transaction intent, fulfilment, sale price, and exchange terms | Cloud Storage/images, scanner prefill, draft/outbox, location/distance, reservation, completion, payment, recent activity, and notifications |
| Owner-only withdraw and relist with confirmation; public browse remains `status == available`; owner can directly read a withdrawn listing | Permanent deletion, admin moderation, automatic expiry, scheduled reminders, and trusted automation |
| Immutable `ownerId`, `createdAt`, and `imageUrl`; server `updatedAt`; conflict-safe transaction and strict client/Rules validation | New marketplace fields, map entry, precise address, delivery, or an in-app transaction system |
| Loading, validation, configuration, authentication, permission, offline, conflict, success, and recovery states | Any other module or API |

Phase 2C.1 implementation rules:

- Preserve the proposal's browse -> detail -> chat structure and existing
  create form, but prioritise a polished, accessible, user-friendly light theme
  rather than pixel-perfect reproduction.
- Use stable IDs: transaction intent is `sale`, `donation`, or `exchange`;
  fulfilment is `pickup` or `meetup`; current status is `available` or
  `withdrawn`.
- Only the authenticated owner sees Edit and Withdraw/Relist. Non-owners see
  Chat only for an available listing. Confirm status changes before writing.
- Perform edits/status changes with a Firestore transaction against the loaded
  server version. Do not silently overwrite a newer edit from another device.
- Security Rules independently enforce ownership, exact field shape, immutable
  owner/creation/image fields, allowed mutable fields, valid status, and server
  timestamps. The client never treats UI hiding as authorisation.
- Keep production deployment and live two-account verification pending. At the
  user's request, do not launch an emulator; run builds, JVM tests, Firestore
  Rules tests, lint, and static checks, then hand off the exact live checklist.
- Maps/Places/location, recycling APIs, Cloud Storage/images, Room/history,
  Remote Config, Hilt, WorkManager, FCM/notifications, lending workflows, and
  all other integrations remain deferred.

### Authorised Phase 2C.2 Marketplace Images - 23 August 2026

| Implement in Phase 2C.2 | Explicitly defer |
|---|---|
| One optional CameraX or Android Photo Picker image, normalised into a metadata-stripped JPEG at no more than 1600 px longest edge and 4 MiB | Multiple images, video, broad storage permission, image editing/cropping, scanner prefill, and permanent local drafts |
| Versioned Firebase Storage object under the authenticated owner's listing path; private `gs://` Firestore reference; authenticated card/detail display | Public download-token URLs, unauthenticated media, arbitrary paths, service-account code, and embedded bucket/API credentials |
| New-listing upload with failed-create cleanup; conflict-safe owner replacement followed by old-version cleanup | Permanent listing deletion, background orphan cleanup, WorkManager, trusted automation, moderation, and scheduled expiry |
| Setup/auth/offline/processing/upload/progress/retry states; Firestore and Storage Rules tests | Maps/location, payments, reservation/completion, lending, notifications, and every unrelated API/module |

Cloud Storage currently requires Blaze billing and an owner-provisioned default
bucket. This remains a deliberate console/live gate. App code and automated
rules tests must still work locally and compile when Firebase configuration is
absent.

### Authorised Phase 2D Recycling Centre Map - 23 August 2026

| Implement in Phase 2D | Explicitly defer |
|---|---|
| One functional Recycling Centre screen using Maps SDK for Android and Places Text Search (New), with at most ten results | Separate lending/marketplace maps, raw REST/geocoding, route drawing, directions, travel time, favourites, centre photos, and material-acceptance claims |
| One foreground current-location request using Fused Location Provider; accept coarse location; no continuous updates | Background location, tracking, geofencing, persisted/uploaded coordinates, and precise home/address collection |
| Manual city/area search when location is denied, disabled, unavailable, or unwanted | Requiring permission before any search or silently using a fixed fake location |
| Proposal-faithful map plus nearby list, synchronised selection, approximate straight-line distance, rating, address, retry, and standard geo-intent handoff | Booking/drop-off workflow, navigation SDK, API-derived opening hours, calling/messaging, saved activity, and notifications |
| Ignored local Maps key with checked setup-required fallback; Android package/SHA-1 and API restrictions documented | Committed key, unrestricted key, service-account credentials, or a claim that client-side hiding alone secures an APK key |

Phase 2D implementation rules:

- Search only after the user taps a button. Allow one request at a time and
  ignore obsolete responses. Request only place ID, display name, formatted
  address, coordinates, and rating.
- Use the text query `recycling centre` because Places does not define a
  supported recycling-centre place type. Bias within a bounded radius around a
  one-time current location when available; otherwise include the manual
  Malaysia area in the query.
- Request coarse and fine foreground permissions together and work with either
  result. The app must remain useful through manual search when permission or
  Google Play services is unavailable.
- Never store, upload, log, or continuously observe the user's position.
  Distances are local straight-line estimates and must be labelled approximate.
- Keep markers and list selection in sync and offer only a standard geo-intent
  handoff. Do not imply route, travel time, or accepted-material verification.
- Provide setup, loading, permission, location-unavailable, offline, empty,
  quota/key/API failure, retry, and manual-search states without sample results
  masquerading as live data.
- Keep the real key in ignored `secrets.properties`, with a harmless committed
  fallback for team builds. Restrict the production key by Android package and
  signing SHA-1 and to Maps SDK for Android plus Places API (New).
- No emulator launch is required in this phase. Automated verification covers
  configured/missing-key builds, JVM policy tests, lint, resource/navigation
  integrity, and secret scanning. The owner completes the real-service/device
  checks in `docs/RECYCLE_MAP_SETUP.md`.

### Authorised Phase 2E P2P Equipment Lending - 28 August 2026

| Implement in Phase 2E | Explicitly defer |
|---|---|
| Authenticated lending item create/edit/withdraw/relist with one optional protected JPEG, category, condition, maximum duration, pickup method, area, optional informational deposit, and optional approximate location | In-app payments, required rental fees, precise private addresses, multiple images, delivery, moderation backend, and permanent deletion |
| Real-time searchable list and a map/list view of available items; manual title/area filtering always works and approximate distance is shown only when both coordinates are available | Route drawing, travel time, directions SDK, background tracking, geofencing, marketplace location, and mandatory location permission |
| Inclusive Malaysia-date request of at most 31 days; owner approve/reject, booked-day collision locks, pickup activation, borrower return report, owner confirmation, and borrower-to-owner rating | Automated approval, penalties, identity verification, insurance, payment collection, stored trust aggregates, and scheduled server actions |
| Lending-linked participant-only chat plus request actions in the existing Notifications screen | FCM/OS push, presence, read receipts, attachments, and a custom trusted sender |
| Firestore/Storage Rules, local policy tests, emulator Rules tests, failure states, setup guide, and live two-account checklist | Claiming that local tests deploy production Rules or prove live Firebase/Maps behavior |

Phase 2E resolves OD-12 for the current assessed build: lending is free to use,
an owner may state an optional refundable deposit, and all money handling occurs
outside PropCycle after users agree in chat. The app displays no payment button
and makes no payment-success claim.

Item locations are optional and public only at coarse, rounded precision after
the owner explicitly chooses to attach the current area. An area label remains
required. Discovery and request management remain usable without location
permission or a real Maps key.

### Authorised Phase 2F Release Hardening - 28 August 2026

| Implement in Phase 2F | Explicitly defer |
|---|---|
| GitHub checks for Java tests, setup-fallback debug/release builds, lint, Firebase Emulator Rules tests, secret-path scanning, and Java-first/legacy-stack policy | Production credentials in CI, automatic production deployment, automatic signing, or any claim that CI proves live services |
| Windows preflight for local secrets-ignore status, XML/navigation integrity, builds, lint, JVM tests, and optional installed Firebase Rules tests | Opening an Android emulator, changing a developer's cloud project, generating a keystore, or silently installing system tools |
| Consolidated owner steps for Rules/index deployment, real Firebase/Gemini/Maps/lending checks, accessibility/stability, exact signed APK, hash, and evidence | FCM, payments, routes, background location, Room/WorkManager history/outbox, trusted automation, multiple images, and store publication |

Phase 2F freezes the current assessed feature set. Local automation can confirm
source, build, test, and policy conditions, but it cannot confirm production
deployment, cloud-console settings, a real Gemini response, Android key
restrictions, physical-device permissions, or release App Check. Those rows
remain `PENDING` until the project owner performs and records them using
`docs/PHASE_2F_RELEASE_CHECKLIST.md`.

### Authorised Phase 2G Functional Journey Correction - 28 August 2026

After reviewing every proposal requirement, the user explicitly authorised a
release-correctness pass before final visual polishing. This exception to the
Phase 2F feature freeze connects existing modules; it does not create or remove
a top-level module.

| Implement in Phase 2G | Explicitly defer |
|---|---|
| Photo/AI-first creation launcher with manual fallback; AI result and app-private processed JPEG handoff into the shared editable Marketplace or Lending form | Automatic publishing, AI-selected price/deposit/location/dates, permanent scan images, multiple images, or a raw provider key |
| Visible Sale/Donation/Exchange choice, all marketplace categories, truthful seller identity, and direct create entry from Marketplace | Marketplace map/location, payments, delivery, reservation/completion automation, and reputation claims without evidence |
| Item-first lending List/Map with query/category preservation and optional approximate-distance sorting | Places search for lending, routes, travel time, precise/private addresses, tracking, geofencing, or background location |
| Room 2.8.4 account-scoped scan/activity history, offline Recent Activities, real local Home/Profile activity summaries, and clear-local-history control | Reward points/badges, WorkManager outbox, cloud activity aggregation, cross-device scan history, OS push, or automated background sync |
| Honest Settings state for light theme, in-app lending updates, and Android-managed location permission | Unfinished dark theme, FCM push sender, notification permission prompts, or fake preference switches |
| Pure Java tests, exported Room schema, proposal-wide flow audit, builds/lint/XML/navigation/Rules/secret checks | Emulator launch at the user's request and any claim of live cloud/device success without owner evidence |

The authoritative flow matrix and manual cross-module checks are in
`docs/FUNCTIONAL_FLOW_AUDIT.md`. The Phase 2F production, live-service, signing,
and accessibility gates still apply to this corrected build.

## 2. Course constraints and delivery targets

| Milestone | Date | Planning status |
|---|---|---|
| Part 1 proposal | 15 July 2026, before 5:00 PM | Past milestone; confirm the team's submission record; PDF is the scope baseline |
| Native plan approval | 28 August 2026 | UI milestone plus Phase 2A through Phase 2E app/local work complete; Phase 2F hardening is active and live owner gates remain open |
| Feature freeze | Target: 29 August 2026 | No new functionality after this point |
| Release candidate | Target: 2 September 2026 | All release gates must pass |
| Working app and final report | 5 September 2026, before 5:00 PM | Fixed deadline |
| Presentation and Q&A | Week 13 practical session | Date to confirm |

The assessed minimums remain covered:

| Requirement | Native Android evidence |
|---|---|
| Custom launcher icon | Adaptive icon resources in `mipmap-*`, including foreground/background and monochrome where supported |
| Store, update, and retrieve device data | Room tables for scan cache, scan history, drafts, and the sync outbox |
| Connect to an external endpoint | Firebase services, Firebase AI Logic/Gemini, and Google Maps/Places |
| Program completeness | Every retained module passes its end-to-end acceptance journey |
| Source-code quality | Java package boundaries, ViewModels, repositories, validation, tests, lint, and attributed commits |

## 3. Retained product scope

No module is removed. The proposal defines three core modules; authentication, home, recycling, messaging, notifications, profile, settings, and local/offline behaviour support those modules rather than becoming extra product modules.

| Scope area | Retained capabilities | Release outcome |
|---|---|---|
| Supporting - Authentication | Welcome, registration, login, session restore, logout | A user can securely enter and leave the authenticated app |
| Supporting - Home dashboard | Greeting, resource search, Smart Scan shortcut, recent activities, quick navigation, notification/profile access | The home screen routes to every primary journey |
| **Core 1 - AI Smart Scanner** | Camera/gallery input, structured identification, editable result, recycling/upcycling advice, save/history | A scan produces a reviewed result and a valid next action |
| Supporting - Recycling centre finder | Current/manual location, nearby centre map and list, centre details, external navigation | A user can find a practical drop-off option without requiring background location |
| **Core 2 - Material Marketplace** | Browse, search, category filters, create/edit listing, sale/donation/exchange intent, pickup/meeting fulfilment, details, status, seller contact | A listing can move from draft to a completed community exchange |
| **Core 3 - P2P Equipment Lending** | Map/list discovery, create/edit lendable item, availability, request dates, owner decision, pickup/return, rating/trust, direct chat | A borrow request can complete its full lifecycle |
| Supporting - Messaging | Conversation list and real-time text conversation linked to a listing or lending item | Only participants can read or send messages |
| Supporting - Notifications | In-app updates, unread state, deep links, preference control; push delivery only if a trusted sender is approved | Relevant events route the user to their context |
| Supporting - Profile and settings | Personal information, avatar, owned activity, scan history, preferences, account details, logout; no reward points or achievement badges | A user can inspect and manage their account and activity |
| Supporting - Offline/local storage | Account-scoped scan cache/history, drafts, retry queue, clear error states | Core saved data remains available without a connection and cannot cross accounts |

Candidate enhancements from the earlier prototype plan—text-only scanner lookup, home eco/impact summary, community statistics, a daily tip, and a marketplace map/list switch—remain documented options only. They are not proposal-parity requirements and require designer/team sign-off before scheduling.

### Explicit scope boundaries

- The app does not process payments. A marketplace price or optional lending deposit is informational and arranged between users.
- Chat is text-first. Images, voice, calls, and live location are not required for the assessed release.
- The app does not guarantee that a recycling centre accepts an AI-identified material. The user must confirm with the centre.
- Background location is not required.
- iOS, web, React Native, Expo, and Jetpack Compose are not implementation targets.
- AppGallery support on Huawei devices without Google Mobile Services is not assumed. This requires a separate HMS compatibility decision and is not allowed to destabilise the assessed APK.

### Stable category taxonomy

The same IDs are used by AI output, forms, filters, storage, and tests. Labels may be localised; IDs do not change after data creation.

| Material ID | Label | Examples |
|---|---|---|
| `banner` | Banners and Signage | PVC banners, vinyl backdrops, foam boards, standees |
| `decoration` | Decorations and Props | Stage props, paper flowers, booth frames, lights |
| `fabric` | Fabric and Textile | Tablecloths, curtains, cosplay offcuts, tulle |
| `stationery` | Stationery and Print | Lanyards, name tags, flyers, certificates |
| `craft` | Craft Supplies | EVA foam, paint, glue, wire, clay |
| `cosplay` | Cosplay and Costumes | Wigs, armour pieces, props, specialist fabric |
| `toys` | Toys and Miniatures | Doll parts, dioramas, figures, miniature supplies |
| `wood` | Wood and Structural | Plywood, frames, booth structures, pallets |
| `electronic` | Event Electronics | LED strips, cables, batteries, speakers |
| `packaging` | Packaging and Containers | Bubble wrap, boxes, containers, cardboard |
| `other` | Other | Material that cannot be classified safely |

| Equipment ID | Label | Examples |
|---|---|---|
| `av` | Audio/Visual | PA systems, speakers, microphones, projectors |
| `tools` | Tools and Hardware | Drills, heat guns, rotary tools, soldering irons |
| `sewing` | Sewing and Craft Tools | Sewing machines, cutting mats, rotary cutters |
| `lighting` | Lighting and Decor | Stage lights, ring lights, spotlights, stands |
| `furniture` | Event Furniture | Folding tables, chairs, tents, display stands |
| `transport` | Transport and Logistics | Trolleys, hand trucks, straps, roof racks |

## 4. Screen contract from the proposal

Each screen ID is stable for planning, testing, issue tracking, and report screenshots.

The PDF contains twenty distinct drawn screens. The table also separates functional surfaces that the proposal promises but does not fully mock up, such as booking/return/rating and consolidated My Activity. These complete an existing module; they are not new top-level modules.

| ID | Screen | Source | Required content and actions | Acceptance condition |
|---|---|---|---|---|
| AUTH-01 | Welcome | PDF mock-up | Brand artwork, short purpose statement, continue action | Continue reaches login; first launch and signed-out launch are deterministic |
| AUTH-02 | Login | PDF mock-up + platform completion | Approved identifier/authentication method, validation, sign-in, register link, progress/error state; password-reset placement requires sign-off | Valid user reaches HOME-01; invalid or offline states are explained without losing input |
| AUTH-03 | Register | PDF mock-up + platform completion | Full name plus fields required by the approved auth method, validation, sign-in link; confirmation/terms placement requires sign-off | Creates one Firebase user/profile and prevents duplicate submission |
| HOME-01 | Home dashboard | PDF mock-up | Greeting, resource search, Smart Scan card, Recent Activities card, notification/profile icons, and designer Home fan; microphone only if voice search is approved | Every control has one destination, an accessible label, and a 48dp target; no decorative control appears tappable |
| HOME-02 | Recent activities | PDF mock-up | Chronological scan, listed, recycled, sold/donated/exchanged, borrow/lend, and return events | Empty, loading, error, and populated states are implemented |
| SCAN-01 | Smart scanner | PDF mock-up + permission fallback | Camera preview, permission rationale, capture, gallery picker, retry | A valid image reaches SCAN-02; denial offers the gallery fallback |
| SCAN-02 | AI result and review | PDF mock-up | Item/material/category, uncalibrated model estimate, recycling guidance, upcycling ideas, impact estimate, edit/review, Save, Recycle, Marketplace, Lend | No AI output can be published before user review; every displayed next action works |
| MAP-01 | Recycling centres | PDF mock-up | Map/list results, current/manual location, nearby search, name, distance, open status when available, rating/attribution, external directions | Works with precise, approximate, denied, and unavailable location states |
| MARKET-01 | Marketplace browse | PDF mock-up | Search, material category chips, transaction/fulfilment labels, result cards, create action | Filters are deterministic and unavailable/reserved items are visibly distinct |
| MARKET-02 | Marketplace detail | PDF mock-up | Images, title, transaction intent, price/terms, condition, material, description, seller, distance/location, status, direct chat action | Only valid actions appear for owner and non-owner contexts |
| MARKET-03 | Create/edit listing | PDF mock-up + platform completion | Photo, title, category, material, condition, transaction intent, fulfilment method, price rules, description, location, preview, save draft, publish | AI prefill remains editable; invalid combinations cannot publish |
| LEND-01 | Lending map search | PDF mock-up | Map, search, equipment markers/cards, distance/area, filters, route to results/detail | Map results follow the active search/filter and open the correct equipment context |
| LEND-02 | Lending listings | PDF mock-up | Search/filter summary, equipment cards, availability summary, distance, create action | List state is deterministic; relationship to map results follows the approved navigation decision |
| LEND-03 | Lending detail | PDF mock-up | Photos, description, impact text, owner/trust, availability, deposit/rental wording, pickup method, location, direct chat and separate borrow-request actions | Chat opens immediately; date request is blocked for unavailable or owner-owned items |
| LEND-04 | Create/edit lending item | PDF mock-up + platform completion | Photo, title, category, description, availability, approved fee/deposit wording, pickup method, location, preview, draft/publish | Required availability and pickup data are validated |
| LEND-05 | Borrow request, return, and rating | PDF prose-derived | Start/end dates, message, owner approve/decline, borrower cancel, active loan, returned confirmation, rating | Concurrent approvals through the app use deterministic booked-day records; adversarial owner-proof enforcement depends on OD-13 |
| CHAT-01 | Messages | PDF mock-up | Conversation rows, related item, participant, last message/time, unread count | Opens the correct thread and handles no-conversation state |
| CHAT-02 | Conversation | PDF mock-up | Header/context, chronological text messages, composer, send/retry state, duplicate-send protection | Real-time updates are participant-only and preserve unsent text on rotation |
| USER-01 | Notifications | PDF mock-up | Activity/update cards, unread/read state, contextual deep link | Selecting a notification opens its valid destination and marks it read |
| USER-02 | Profile | PDF mock-up + approved scope correction | Avatar, name, account/local activity summary, rating/trust evidence, published listings summary, owner-only edit action; no reward points or achievement badges | Shows only permitted public/private fields and opens owned content/profile editing |
| USER-03 | Settings | PDF mock-up + platform completion | Theme, notifications, location preference, account details management, logout | Preferences persist and system permissions are never represented inaccurately |
| USER-04 | My activity | Existing-function consolidation | My listings, my lent items, requests, and scan history; no reward points or achievement badges | Each subsection shows current data and supports its owner actions without becoming a new top-level module |

### Navigation model

- `LauncherActivity` is not required; `MainActivity` hosts one `NavHostFragment`.
- An authentication graph contains AUTH-01 through AUTH-03.
- An authenticated graph contains HOME-01 and every feature graph.
- HOME-01 preserves the designer's bottom-right fan. The existing top-left hamburger is the single collapsed control; tapping it reveals labelled Market, Share, and Map actions plus a close control. Market opens marketplace browse, Share opens Lend Resource, and Map opens the static Lending Map while its real API remains deferred.
- The PDF does not define a separate full drawer. The popup available from hamburger controls on other review screens remains a prototype review aid, not an approved production information architecture.
- Notifications and profile remain available from the home toolbar as drawn.
- Back always returns to the previous context; top-level destinations do not create duplicate back stacks.
- A deep link or notification validates authentication and object existence before opening a detail screen.

```text
Launch
  -> signed out -> Welcome -> Login <-> Register -> Home
  -> signed in -------------------------------> Home

Home
  -> Smart Scan -> AI Result -> Save | Recycle Centres | Market Post | Lending Post
  -> Search -> Market/Lending filtered results
  -> Recent Activities -> related detail
  -> Market -> Listing Detail -> Conversation
  -> Lend -> Map Search | Listings -> Lending Detail -> Conversation
                                                \-> Borrow Request -> Return/Rating
  -> Notifications -> contextual detail
  -> Profile -> My Activity | Settings
  -> Messages -> Conversation
```

## 5. Native Android architecture

### Layer responsibilities

| Layer | Java components | Rules |
|---|---|---|
| UI | `MainActivity`, Fragments, RecyclerView adapters, custom Views, XML, View Binding | Renders immutable UI state and forwards user events; no direct Firebase/Room calls |
| Presentation | Screen ViewModels, `LiveData`, `SavedStateHandle`, UI-state and one-time-event wrappers | Owns screen state, invokes use cases/repositories, survives configuration changes |
| Domain | Validators, state transition policies, score calculations, date conflict checks, selected use cases | Plain Java; added only for logic reused across screens or requiring isolated tests |
| Data | Repositories and local/remote data sources | Each data type has one source-of-truth policy and one public repository boundary |
| Platform/integration | Currently authorised Firebase/Auth/Firestore, CameraX, Photo Picker, Firebase AI Logic, and App Check adapters; later integrations remain planned | Wrapped so screens do not depend directly on vendor APIs; adding one integration does not authorise the rest |

### State and concurrency policy

- UI state flows from repository/use case to ViewModel to Fragment.
- User events flow from Fragment to ViewModel and then to the data/domain layer.
- Firebase callbacks, Guava `ListenableFuture`, Room background executors, and WorkManager are adapted behind repositories.
- No database, image processing, JSON parsing, or network work runs on the main thread.
- Each asynchronous screen exposes loading, content, empty, permission-required, recoverable-error, and terminal-error states where applicable.
- View Binding references are cleared in every Fragment's `onDestroyView()`.
- `SavedStateHandle` preserves IDs, filters, form drafts, and unsent text needed after process or configuration recreation.

### Planned project structure

The native project exists at the repository root as one Gradle `:app` module for delivery speed, with package-by-feature boundaries.

```text
repository root/
  settings.gradle
  build.gradle
  gradle.properties
  app/
    build.gradle
    src/main/
      AndroidManifest.xml
      java/com/<approved-namespace>/propcycle/
        PropCycleApplication.java
        MainActivity.java
        core/
          common/          result, error, executor, constants
          navigation/      destination and deep-link helpers
          ui/              reusable Views, adapters, theme helpers
          validation/      shared input validation
        data/
          local/           Room database, entities, DAOs, migrations
          remote/          Firebase, AI, maps, storage data sources
          repository/      repository implementations
        domain/
          model/           Java domain models and enums
          usecase/         only shared or complex business operations
        feature/
          auth/
          home/
          scanner/
          recycling/
          marketplace/
          lending/
          chat/
          notifications/
          profile/
          settings/
        worker/            upload and sync workers
        di/                Hilt modules
      res/
        layout/            screen, row, dialog, and reusable layouts
        navigation/        auth and authenticated graphs
        drawable/          shapes, gradients, selectors, vectors
        mipmap-*/          adaptive launcher icon
        values/            strings, colours, dimensions, styles, themes
        values-night/      dark-theme overrides
    src/test/              JVM tests
    src/androidTest/       Room, navigation, Firebase-emulator, and Espresso tests
```

Package names are finalised before project generation. Do not create empty placeholder packages or one class per unnecessary abstraction.

## 6. Native technology mapping

| Removed Expo/React Native concept | Native Android replacement |
|---|---|
| React Native / Expo SDK 57 | Android SDK, AndroidX, Material Components |
| TypeScript / TSX | Java 17 and XML resources |
| Expo Router | Navigation Component with `NavHostFragment`, feature graphs, and Java-generated Safe Args |
| Zustand | ViewModel, LiveData, SavedStateHandle, repositories |
| `expo-sqlite` | Room 2.8.x over SQLite |
| `expo-camera` | CameraX Preview and ImageCapture |
| `expo-image-picker` | Android Photo Picker with a compatible fallback |
| `react-native-maps` | Maps SDK for Android |
| `expo-location` | Fused Location Provider and runtime permission APIs |
| Expo blur/gradient components | Material cards, XML drawables, elevation, and an API-aware blur/fallback policy |
| Ionicons | Material Symbols or approved vector drawables |
| Firebase JavaScript SDK | Firebase Android SDK managed through the Firebase BoM |
| Direct Gemini REST key | Phase 2B Firebase AI Logic Java SDK plus build-specific App Check; no raw key; Remote Config remains later scope |
| EAS Build | Gradle `assemble`/`bundle`, Android signing, and release variants |
| Removed Expo `.env` configuration | Gradle/local properties and restricted console configuration recreated from the service consoles; no committed secrets |

### Dependency rules

- Use the Firebase Android BoM so Firebase libraries remain mutually compatible.
- Use Room 2.8.x with Java annotation processing; do not upgrade to Room 3 in this Java-only plan.
- Use Java variants of WorkManager and lifecycle libraries rather than `-ktx` artifacts unless a vendor requires a transitive Kotlin runtime.
- Kotlin runtime dependencies pulled transitively by AndroidX/Firebase do not authorise Kotlin source files.
- Add a third-party library only when the Android platform/Jetpack/Firebase SDK does not reasonably cover the need, and record its licence and maintenance status.
- Do not add a chart library for simple impact numbers; use native Views unless a real chart is approved.

## 7. Data design and ownership

### Source-of-truth matrix

| Data | Source of truth | Offline behaviour |
|---|---|---|
| Authentication session | Firebase Authentication | Firebase restores a valid cached session; protected writes require connectivity |
| User/listing/lending/request metadata | Cloud Firestore with persistent disk cache disabled | Public data can use in-memory cache; account-private offline data is stored explicitly in UID-scoped Room tables |
| Real-time messages | Cloud Firestore thread/message documents with snapshot listeners | Sending requires connectivity; unsent composer text stays in saved state and no cross-account SDK disk queue is used |
| Scanner working image | One orientation-corrected, bounded, metadata-stripped JPEG in app-private temporary cache | Phase 2G may transfer it once to a user-selected review form; delete after consumption, abandonment, cancellation, or failure; no permanent image or Cloud Storage scan upload |
| Phase 2C.2 marketplace image | One bounded, metadata-stripped JPEG in Cloud Storage at `marketplace/{ownerUid}/{listingId}/primary_{version}.jpg`; private `gs://` reference in Firestore | Upload needs connectivity; failed-create and replaced-version cleanup are immediate best effort; no Room/WorkManager retry or permanent local draft |
| Built-in demo image | One allowlisted `demoImageKey` in a Marketplace or Lending Firestore document; illustration bytes are packaged in the APK | Needs no Cloud Storage or Storage billing. The Firestore listing save still requires authentication, current Rules, and connectivity; personal image URL and demo key are mutually exclusive |
| Other later published images | Phase 2C.2 marketplace and Phase 2E lending each allow one protected JPEG; any other image needs a separately authorised publish intent and storage contract | Avatars, multiple marketplace/lending images, and durable outbox remain deferred with Room/WorkManager |
| Phase 2G scan/activity history | Room 2.8.4, scoped by current Firebase account UID | Validated text result and truthful actions are readable offline on this device; scan images are never stored in Room |
| Later drafts/outbox | Room plus WorkManager, scoped by initiating account UID | Deferred; Phase 2G does not claim durable publish drafts or background retries |
| Theme, push, and location settings | Light theme and in-app updates are fixed honest states; Android Settings owns location permission | No fake persistence: dark theme and OS push remain unavailable until their actual implementations are approved |
| Phase 2B AI model/prompt policy | Exact `gemini-3.6-flash` model and bounded structured prompt pinned in reviewed Java source | A fresh scan requires connectivity; Remote Config and persistent result caching remain deferred |
| Home activity summary | Account-scoped Room activity records | Counts only completed local scans and publish actions; it does not invent global/community impact |
| Map/Places results | Google SDK responses plus short-lived memory cache | Saved item coordinates still render; live nearby search requires a connection |

### Cloud Firestore collections

Phase 2A intentionally implements a strict subset of the release schema. Its deployed
contract is exactly:

| Phase 2A path | Implemented fields |
|---|---|
| `users/{uid}` | `displayName`, `createdAt`, `updatedAt`; email remains in Firebase Authentication; authenticated direct document reads are allowed for seller identity, collection enumeration is denied, and only the owner writes |
| `marketplaceListings/{listingId}` | immutable `ownerId`; `title`, `titleNormalized`, `description`, stable category, condition, `transactionIntent`, separate `fulfilmentMethod`, integer `priceMinor`, `exchangeTerms`, nullable `imageUrl`, optional allowlisted `demoImageKey`, `status`, `createdAt`, `updatedAt` |
| `chatThreads/{threadId}` | marketplace or lending context type/ID/title, immutable owner/contact UIDs and ordered participant list, last-message ID/text/sender/time, created/updated server timestamps |
| `chatThreads/{threadId}/messages/{messageId}` | immutable sender UID, text, matching client operation/message ID, server timestamp |

The Android Firestore SDK uses memory-only cache in this slice because its persistent
cache is process-wide rather than partitioned by Firebase user. This prevents one
account's private thread documents persisting on disk after logout. Durable offline
drafts/outbox remain a later Room/WorkManager slice.

| Path | Required fields and ownership |
|---|---|
| `users/{uid}` | `displayName`, `username` only if approved, avatar storage path, coarse public location label, and timestamps; one general account acts contextually and the owner writes only profile-safe fields. No reward-point or achievement-badge fields are stored. |
| `marketplaceListings/{listingId}` | owner ID, title, description, image storage paths, category, material, transaction intent (`sale`, `donation`, `exchange`), fulfilment method (`pickup`, `meetup`, or approved equivalent), price/exchange terms when applicable, condition, coordinates/geohash, location label, status, AI-origin flag, timestamps |
| `lendingItems/{itemId}` | immutable owner ID; title/normalised title, description, one nullable protected image URL or one allowlisted `demoImageKey`, stable category/condition, maximum days, optional informational deposit, pickup method, required area label, nullable rounded latitude/longitude, status, server timestamps |
| `lendingRequests/{requestId}` | Participant-only item/title, borrower, owner, immutable participant list, inclusive Malaysia start/end/day keys, opaque lock token after approval, return-report flag, lifecycle status, server timestamps |
| `lendingRatings/{requestId}_{raterUid}` | completed request, item, borrower rater, owner recipient, score, optional short review, timestamp; deterministic ID enforces one immutable rating per eligible request |
| `activities/{activityId}` | actor, type, related object, user-safe summary, timestamp; used by recent activity and eco evidence |
| `notifications/{uid}/items/{notificationId}` | Non-chat domain event type, title/body, related object/destination, read flag, timestamp; written in the same Firestore batch/transaction as a rule-valid transition where feasible |
| `scanHistory/{uid}/items/{scanId}` | Optional signed-in backup of reviewed scan summary/action; raw scan images may be transmitted for AI analysis but are not persisted here |
| `lendingItems/{itemId}/bookedDays/{yyyy-MM-dd}` | Participant-readable collision lock containing request ID, opaque random lock token, exact date key, and update timestamp; borrower identity stays only in the participant-private request |

Firestore is not treated as a full-text search engine. The release search plan is normalised title-prefix search plus server-side category/status/geohash filters and local filtering of the loaded result window. A hosted search service is future scope only if the dataset outgrows this approach.

### Cloud Firestore chat shape

| Path | Content |
|---|---|
| `chatThreads/{contextType}_{contextId}_{ownerUid}_{contactUid}` | Deterministic marketplace or lending context ID, immutable owner/contact participant UIDs, related item ID/title, and last-message preview/time; unread/last-read metadata is deferred |
| `chatThreads/{chatId}/messages/{messageId}` | Phase 2A stores sender UID, text, client operation ID, and server timestamp; each message is immutable after creation |

Firestore Rules allow access only when `auth.uid` is an immutable thread participant. On marketplace or lending thread creation, rules validate the deterministic ID and exact fields against the related available item, require its real owner as `ownerUid`, and require the authenticated non-owner contact to create the thread. Either participant may then send a message through an atomic message-plus-thread-preview batch. A user cannot forge another sender UID, mutate an acknowledged message, or join a thread by guessing an ID. Unread/last-read metadata, OS notification merging, and online presence remain later scope.

### Cloud Storage paths

Current authorised paths:

- `marketplace/{ownerUid}/{listingId}/primary_{version}.jpg`
- `lending/{ownerUid}/{itemId}/primary_{version}.jpg`

Future paths such as avatars and multiple marketplace/lending images are still
closed. Storage writes/deletes require `request.auth.uid` to match the
`{ownerUid}` segment, a JPEG no larger than 4 MiB, a versioned primary filename,
and matching owner/context/kind metadata. Authenticated reads support item
display; all other paths are denied. Firestore Rules separately
require `imageUrl` to be null or a matching owner/listing `gs://` path. The app
uploads only after the owner presses Publish or Save, deletes a newly uploaded
object if the Firestore write fails, and deletes the previous version after a
successful conflict-safe replacement. A process kill or network loss can still
interrupt best-effort cleanup; scheduled orphan cleanup requires a separately
approved trusted backend or WorkManager design and is not claimed here.

### Room 2.8.x tables

| Table | Purpose and key fields |
|---|---|
| `scan_cache` | Owner UID/account scope, cache key, input type/hash, prompt version, model configuration, structured JSON, created/expiry timestamps |
| `scan_history` | Owner UID, local ID/cloud ID, reviewed item/material/category, app-private image reference when retained, structured result, action, sync state, scan timestamp |
| `draft_posts` | Owner UID, draft type, editable fields, app-private image references/durable grants, location, updated timestamp, publish state |
| `sync_outbox` | Owner UID, unique operation ID, entity type/ID, operation, payload reference, attempt count, next retry, last error |

Room migrations are mandatory from the first released schema. Configure `room.schemaLocation`, commit exported schema JSON, and test every migration path. Destructive fallback is prohibited for user-created history and drafts. On logout, cancel that UID's scheduled Workers and prevent its rows from rendering; every Worker verifies that its recorded owner UID matches the current Firebase user before reading files or performing a remote write.

Firestore persistent disk caching is disabled to avoid account A data surviving an app restart for account B. Phase 2A repositories use in-memory snapshots. Chat listeners stop with the owning screen; marketplace and detail listeners close when their owning ViewModels are cleared. Logout clears the protected navigation stack so those ViewModels can be released and new protected actions require authentication. Remote writes require explicit task completion, chat marks local pending messages as sending, and immutable owner/sender fields plus Security Rules reject a late operation authenticated as a different UID. Phase 2A does not yet claim a central pending-write drain/session-generation manager or UID Workers; those are required when Room/WorkManager arrives. A two-account profile/listing/thread/message switch is a required manual Firebase-project check until an automated account-switch journey is added. The later full-release test additionally covers drafts, notifications, files, Workers, and pending outbox actions.

## 8. Core business flows and state machines

### AI scan and handoff

Current Phase 2B flow:

1. A signed-in user captures one image or picks one image. Text-only lookup is
   added only if OD-10 is separately approved.
2. The app copies the selected input into app-private temporary cache, validates
   it, decodes it off the main thread, rotates it correctly, bounds dimensions
   and bytes, removes EXIF metadata, and writes a working JPEG.
3. The app shows a disclosure that the working image will be transmitted to
   Firebase AI Logic/Gemini. No request starts until the user proceeds.
4. The repository allows one request at a time and sends the image plus a
   bounded prompt through Firebase AI Logic using the Gemini Developer API.
5. The structured response is schema-validated. Unknown categories, missing or
   overlong fields, invalid numbers, malformed JSON, or unsafe/unusable output
   produce a recoverable error/retry state rather than a crash or fake result.
6. SCAN-02 displays item name, category, material, recyclable status,
   Malaysian-context guidance, upcycling ideas, impact text, and an explicitly
   labelled uncalibrated model estimate rather than guaranteed confidence.
7. The user reviews the result before using Recycle, Marketplace, or Lend. Phase
   2G can prefill the existing editable Marketplace/Lending review form and
   transfer its app-private processed JPEG, but it never auto-publishes.
8. Room stores the validated text result as account-scoped local history. The
   image remains temporary and is deleted after form consumption, abandonment,
   cancellation, or failure; no Cloud Storage scan copy is created.

The handoff uses the already processed app-private file and never persists a
short-lived Photo Picker URI. Durable publish drafts and WorkManager remain a
separate future slice.

The category enum includes every retained material category: banner, decoration, fabric, stationery, craft, cosplay, toys/miniatures, wood, electronics, packaging, and other.

Phase 2B has no persistent AI cache and no Remote Config. Its exact model and
schema/prompt are source-pinned for a reviewable build. AI access requires an
authenticated user, project quotas, budget alerts, and App Check. Debug builds
use only each developer's registered debug token; release builds use only Play
Integrity and still require the owner steps in OD-14. Abuse-resistant per-user
limits require OD-13 trusted logic. AI confidence, impact, and recycling advice
remain estimates and are labelled/reviewed accordingly.

### Marketplace listing lifecycle

Current authorised Phase 2C.1 lifecycle:

```text
available <-> withdrawn
```

- Transaction intent is `sale`, `donation`, or `exchange`; fulfilment is a separate `pickup` or `meetup` value.
- Donation requires price `0`; sale requires a positive RM price; exchange requires a short description of what the owner will consider. Fulfilment never implies in-app delivery or payment.
- Only the owner can edit, withdraw, or relist. Withdrawn listings disappear from public browse and new contact-chat creation, but the owner retains direct access.
- Reservation, completion, recent activity, impact credit, counterparty confirmation, and stronger trust evidence remain later work.

### Lending lifecycle

```text
pending -> approved -> active -> returned -> rated
   |          |          |
   +-> rejected          +-> return confirmation required
   +-> cancelled
```

- Bookings use `Asia/Kuala_Lumpur` calendar dates; start and end are inclusive, start may equal end, neither may be in the past, and the assessed release caps a request at 31 days.
- The owner cannot borrow their own item.
- Approval uses a bounded transaction over exact document references: the request, lending item, and one deterministic `bookedDays/{yyyy-MM-dd}` document for each inclusive requested day. The transaction fails if any day already exists, then atomically approves the request and creates privacy-minimal locks sharing a random token also stored in the participant-only request. Cancellation/release reads the bounded day set and removes only locks whose token matches that request.
- Security Rules restrict request/lock transitions to the item owner, validate immutable participants/field shapes/date bounds, and prevent a non-owner from changing availability. Rules cannot prove that every date in an arbitrary client-supplied range was written, so the baseline guarantee is collision-safe concurrent approvals through the reviewed app. Adversarial owner-proof booking enforcement requires trusted OD-13 logic and must not be claimed otherwise.
- Deposit is an informational RM amount; no payment is collected in the app.
- A rating uses deterministic ID `{requestId}_{raterUid}`. Rules require a returned request, an eligible participant/rater-recipient pairing, score range, and immutable content after creation. Trust is calculated for display from those documents; users cannot write a personal aggregate trust value. A stored trusted aggregate is excluded unless OD-13 adds a backend.
- Return and rating actions remain linked to the original request for auditability.

### Location and map flow

- Request foreground location only when the user opens a location-dependent action.
- Accept approximate location and provide manual place/address selection when permission is denied.
- Phase 2E stores only optional latitude/longitude rounded to two decimal places and sorts the bounded loaded window by straight-line distance locally. Geohash range queries remain a future scale optimisation.
- Recycling search uses Places Nearby/Text Search with Malaysia-oriented terms such as `recycling centre` and `kitar semula`; results show required Google attribution.
- Request only the place fields needed for the screen to control latency and billing.
- External directions open an installed map application through an intent; PropCycle does not implement turn-by-turn navigation.

### Notifications

- Phase 2E uses the existing Notifications surface as an in-app lending request centre backed directly by the signed-in participant's bounded `lendingRequests` query. It does not create duplicate notification documents or claim OS delivery.
- The later USER-01 notification design may merge unread chat state with other trusted domain events only after its sender/data contract is separately approved.
- Device-local WorkManager reminders may cover due/return dates for the signed-in device. Cross-device scheduled notifications and automated FCM push are enabled only if OD-13 selects a trusted event sender; the Android client never holds service-account credentials.
- Android 13+ notification permission is requested in context after explaining its value.
- Denying push does not disable the in-app notification page.

### Trusted automation boundary

The assessed baseline does not assume a custom trusted backend. Consequently, automated FCM sending, cross-device schedules, global community statistics, stored eco/trust aggregates, and automatic orphan cleanup are not release guarantees. If the team requires them, OD-13 must select a Java-capable trusted service, name an owner, add emulator/integration and abuse tests, define deployment/secrets/billing controls, and place that work on the schedule before implementation. Client code and Security Rules alone must not be described as trusted server computation.

## 9. UI and design-system plan

### Designer fidelity rules

- Preserve the proposal's content hierarchy, cards, arrows/flows, screen purpose, and Home navigation fan.
- Convert the monochrome wireframes into Android XML without inventing a new information architecture.
- Use Android-native back behaviour, permissions, text scaling, insets, keyboard handling, and feedback.
- Resolve a designer sign-off screenshot for each screen ID before marking UI complete.
- Do not copy iPhone notches, home indicators, or unsafe fixed pixel measurements into the Android app.

### Current light-theme visual tokens

The proposal is monochrome, and the user has now approved a soft light eco-colour pass. The palette changes colour only: the submitted hierarchy, proportions, labels, cards, and Home fan remain intact. Supplied production fonts, logo artwork, and final launcher artwork still require team assets and sign-off.

| Token | Working value | Use |
|---|---|---|
| Primary | `#1F5C42` | Main actions, active state, icons, and high-emphasis cards |
| Primary container | `#C3DDCB` | Tonal buttons and selected supporting surfaces |
| Surface container | `#E2F0E6` | Cards, chips, and diagram markers |
| Accent | `#D28A36` | Reserved warm highlight |
| Error | `#B3261E` | Destructive/error states only |
| Light background | `#F7F8F3` | Default app background |
| Surface | `#FFFFFF` | Fields, cards, and foreground panels |
| Text | `#18231E` | Primary text in the light theme |
| Outline | `#5F7066` | Borders and lower-emphasis marks |

- Use resource tokens, not hard-coded colours/dimensions in Java or individual layouts.
- Use an 8dp spacing grid with documented 4dp exceptions, scalable `sp` text, and reusable shape styles.
- Glass-like cards use translucent surfaces, borders, elevation, and gradients. Blur is an enhancement only; a high-contrast fallback is mandatory on unsupported/slow devices.
- Animations must not block navigation, and reduced-motion behaviour must remain understandable.
- Target API 36 screens are edge-to-edge and apply status bar, navigation bar, display-cutout, and IME insets correctly.
- Navigation uses the current predictive-back APIs and Navigation Component integration rather than legacy custom back interception.

### Accessibility and adaptive checks

- Minimum 48dp touch target for every interactive element, including the Home fan.
- Meaningful `contentDescription` for non-text controls; decorative images are excluded from accessibility focus.
- Logical TalkBack focus order, headings, state announcements, and labelled form errors.
- Text remains usable at 200% font scale without clipping critical actions.
- Colour is never the only status signal; contrast is verified for every enabled theme. This pass enables the light theme only.
- Phone layouts are portrait-first. API 36 testing also covers at least one `sw600dp` tablet/foldable in portrait, landscape, freely resized, and split-screen modes because large-screen orientation/resizability restrictions cannot be relied on.
- The Home fan exposes visible text labels, accessibility descriptions, and 60dp-or-larger focus targets for switch access and TalkBack.

## 10. Security, privacy, and validation

### Security controls

- Phase 2B debug builds initialise only the Firebase App Check debug provider. Every developer registers their own token in the Firebase Console; tokens are private, never shared, and never committed.
- Phase 2B release builds initialise only the Play Integrity provider. Release use still requires a protected signing key, release SHA-256 registration, correct Play/Firebase/Cloud project links and permissions, review of outside-Google-Play policy for the course APK, exact signed-APK testing, request-metric review, and enforcement only after legitimate requests are verified.
- If those release prerequisites are unavailable, never ship the debug provider or token in the release APK and never report the protected AI scanner as release-ready. Record the limitation and either keep the scanner out of that release or approve a reviewed lower-assurance/non-enforced policy; OD-13 is required for a custom attestation/proxy alternative.
- Firebase Authentication is required for all user data, listings, lending, chat, ratings, uploads, and every Phase 2B AI request.
- Deploy and test Firestore and Storage Rules before connecting the release build.
- Validate ownership and state transitions in both client code and Security Rules. Use trusted backend logic only for the capabilities explicitly approved in OD-13.
- Restrict Maps/Places keys to the Android application ID and signing certificate; restrict enabled APIs.
- Firebase AI Logic keeps the Gemini provider key behind its proxy. Phase 2B source-pins the exact model, bounded prompt/schema, and safety configuration; Remote Config and persistent cache policy remain deferred.
- Never log passwords, tokens, message bodies, exact private locations, raw AI images, or full user documents.
- Release builds disable debug logging, shrink/obfuscate where compatible, and use a team-controlled signing key backup.
- Use Network Security Configuration to disallow cleartext traffic and to document any exceptional trust configuration.
- Phase 2A disables Android application backup with `android:allowBackup="false"`. If backup is enabled later, add and test legacy backup plus API 31+ data-extraction rules that exclude credentials, tokens, private caches, and sensitive local files.
- Mark Android components non-exported unless a documented external entry point requires exposure.

### Privacy and permission policy

| Capability | Permission/data policy |
|---|---|
| Camera | Ask only from SCAN-01; explain why; gallery remains available after denial |
| Gallery | Use the system Photo Picker to avoid broad media access on supported versions |
| Location | Foreground only, approximate accepted, manual fallback, no background tracking |
| Notifications | Ask in context; in-app notifications remain available after denial |
| Scan images | Explain before analysis that a bounded working image is transmitted to Firebase AI Logic/Gemini for transient processing; keep it app-private, transfer it only to the selected editable form, and delete it after use. Room stores validated text/history only, never the image |
| Listing location | Show a useful area/meeting point rather than exposing a user's private home by default |
| Account data | Passwords remain solely with Firebase Auth; settings store no credentials |

### Validation baseline

- Trim and length-limit every text input.
- Validate dates, price/deposit, category enums, image count/type/size, coordinates, and object ownership.
- Sanitize UI display but do not rely on client sanitisation for access control.
- Use idempotency/operation IDs for publish, send-message, approve, return, and rating actions to prevent duplicate taps.
- All errors are mapped to user-readable, recoverable messages without exposing backend internals.

## 11. Offline, reliability, and performance

### Offline expectations

- Phase 2G stores validated scan text and truthful activity in UID-scoped Room history. A new analysis still requires connectivity and shows an offline/retry state without inventing a result.
- Saved local history is readable offline for the same signed-in account. Durable form drafts, an upload outbox, and cross-device scan history remain deferred.
- Marketplace/lending shows cached Firestore data with a visible stale/offline state.
- Creating or editing a draft is always local first.
- Publishing, messaging, map/Places search, authentication changes, and a fresh AI request require connectivity unless the SDK safely queues the exact operation.
- The sync outbox retries idempotent work with network constraints and exponential backoff only when its owner UID still matches the active Firebase user; logout cancels account Workers. Permanent validation/auth/account-mismatch errors stop automatic retries and require user action.
- Conflict rule: server state wins for remote ownership/status; the user is prompted before a local edit overwrites a newer remote edit.

### Performance budgets

- No StrictMode disk/network violations in the debug critical journeys.
- Paginate marketplace, lending, messages, activity, and notifications; do not attach unbounded root listeners.
- Load thumbnail-sized images in lists and full images only on details.
- Compress uploads while preserving enough detail for AI/material recognition.
- Remove listeners in the matching lifecycle callback and cancel obsolete AI/search requests.
- Test startup, scrolling, camera, map, and upload on at least one lower-memory physical Android device.

## 12. Test and release strategy

### Test layers

| Layer | Required coverage |
|---|---|
| Plain JVM tests | Validators, category mapping, Phase 2B structured AI schema/parser and bounds, later cache keys, eco/trust calculations, search normalisation, state machines, date overlap, error mapping |
| Repository tests | Success/error/offline/malformed/one-request mapping with local fakes; Firebase AI Logic has no emulator and live AI calls are not unit tests |
| Room instrumentation | DAO CRUD, owner-scope isolation, query results, committed/exported schema JSON, migration preservation, outbox retry selection and logout cancellation |
| Firebase Emulator Suite | Auth-dependent Firestore/Storage rule allow/deny cases, deterministic chat/rating identities, lending-lock transitions, and representative data flows |
| Fragment/navigation tests | Destination arguments, authentication guards, back stack, process/state restoration |
| Espresso journeys | Register/login, scan-to-save, scan-to-listing, marketplace-to-chat, lending request-to-return/rating, settings/logout |
| Manual device matrix | API 24, API 33, API 36; scanner camera/gallery/denial/no-camera/rotation/offline/App Check/live request, approximate/denied location later, notification denial later, dark mode, large text, rotation/process recreation; API 36 `sw600dp` portrait/landscape/resizing/split-screen |

### Definition of done for every screen/module

1. Matches the approved designer comparison for its screen ID.
2. Loading, empty, error, offline, permission-denied, and success states are handled where relevant.
3. ViewModel/repository boundaries are respected; no vendor calls in a Fragment.
4. Input validation and role/ownership rules pass.
5. Accessibility checks pass with TalkBack and large text.
6. Unit/instrumentation tests appropriate to the logic pass.
7. No new Android Lint error or warning is accepted without a documented reason.
8. Another member reviews the pull request and runs the critical path.
9. Report-ready screenshot and short contribution note are captured.

### Release gates

- Clean Gradle build from a fresh clone using the wrapper and JDK 17.
- Unit tests, Android tests, Lint, and Firebase rules tests pass.
- All screen IDs and retained modules have a completed acceptance check.
- No secrets in Git history or built artefacts; release Maps key restrictions verified.
- The release variant contains only the Play Integrity App Check provider. Before the AI scanner is called release-ready, the exact signed course APK must produce valid App Check requests under the reviewed outside-Play policy; OD-14 records any non-enforced limitation, and the debug provider/token is never a release fallback.
- Signed APK installs and launches on two physical Android devices.
- Fresh install, upgrade install, logout/login, offline restart, and denied-permission smoke tests pass.
- Crash-free 10-minute presentation rehearsal with a prepared fallback dataset and cached scan.
- APK hash, signing key custody, final report, screenshots, and contribution log are archived.

## 13. Team ownership and integration

Member labels remain placeholders until the team records names beside them. Ownership means primary implementation and explanation; it does not remove peer review.

| Owner | Primary native responsibility | Secondary/review responsibility |
|---|---|---|
| Member A - Lead/Core | Project bootstrap, Gradle, navigation, authentication, permissions/location, Maps/Places, release/signing | Firebase setup, integration management, final architecture/report |
| Member B - UI/UX | XML design system, reusable Views, home, all screen styling, Home fan, launcher icon, accessibility | Designer comparison set, screenshots, UI review across modules |
| Member C - AI/Local | CameraX, Photo Picker, Firebase AI Logic, schema parser, scan result/history, Room/outbox | Data models, cache/performance tests, scanner report/demo |
| Member D - Cloud/Exchange | Firestore/Storage repositories and rules, marketplace, lending booked-day locks, Firestore chat, in-app notifications, ratings | Emulator tests, edge cases, optional backend only after OD-13, store/release support |

### Integration rules

- Agree model interfaces, enums, collection names, navigation arguments, and design tokens before parallel feature work.
- Each feature branch delivers a vertical slice: XML + Fragment + ViewModel + repository contract + states + tests.
- Avoid long-lived shared-file edits. Core graph/theme/schema changes require advance notice in the team channel.
- Pull requests must identify screen IDs, acceptance cases, screenshots, test evidence, and schema/rule changes.
- Integrate at least once daily during the remaining delivery window; do not wait until feature freeze.
- Every member must be able to explain their Java, XML, data flow, tests, and security decisions for Q&A.

## 14. Remaining delivery schedule

This schedule assumes plan approval by 6 August 2026 and four parallel contributors. If approval slips, the lead must re-baseline dates rather than silently deleting modules or testing.

| Dates | Gate and outcome | Member A | Member B | Member C | Member D |
|---|---|---|---|---|---|
| 5-6 Aug | Plan and contract freeze | Confirm namespace, API baseline, Git strategy | Screen inventory/design tokens | AI schema/cache contract | Firebase schema/rules contract |
| 7-9 Aug | Native foundation | Create approved project, CI, auth/navigation shell | XML component library, welcome/login/register/home shells | Room 2.8 database and scanner spike | Firebase emulator/config, repository interfaces/rule skeleton |
| 10-14 Aug | Narrow Phase 2B scanner vertical slice | Authentication/App Check integration review; no map work in this slice | Preserve Scanner/AI Result wireframes and all scanner states | Camera/gallery -> bounded temporary image -> AI -> review flow; no save/history | Peer-review only; no new user/activity/notification backend |
| 23 Aug re-baseline | Narrow Phase 2C.1 marketplace owner slice | Integration/release review; no new API | User-friendly edit/detail owner actions | Conflict/state tests; no images or local drafts | Owner edit/status repository and Security Rules tests |
| 23-24 Aug re-baseline | Narrow Phase 2C.2 marketplace image slice | Storage/config integration review | Camera/gallery/create/edit image states | Reused bounded metadata-stripping pipeline and policy tests | Storage repository, Firestore/Storage Rules, and setup guide |
| 23-24 Aug re-baseline | Narrow Phase 2D recycling-centre map slice | Maps/Places restricted-key integration review | Proposal-faithful map/list and permission/manual states | Query/distance/result policy tests | Places Text Search repository and detailed setup guide |
| 20-24 Aug | Lending vertical slice | Lending map and date/navigation integration | Lending screens/calendar/request states | Shared image/local helpers and performance | Lending, deterministic booked-day transaction, return, eligible ratings |
| 25-27 Aug | Supporting modules | Auth guards, deep links, settings permissions | Home/recent/profile/messages/notifications visual completion | Scan history, offline and failure states | Notifications, my activity, status/ownership edge cases |
| 28-29 Aug | End-to-end integration and feature freeze | Full navigation/release build | Designer/accessibility pass | AI/offline/performance pass | Rules/emulator/data integrity pass |
| 30 Aug-1 Sep | Hardening | Device matrix, signing rehearsal | Screenshot/report assets | JVM/Room/camera tests | Firebase/rules/journey tests |
| 2 Sep | Release candidate | Produce signed RC and issue list | Final UI sign-off | Demo cache/fallback data | Firebase production rules/config sign-off |
| 3-4 Sep | Submission buffer | Fix release blockers only, assemble archive | Final screenshots/presentation | AI demo rehearsal/Q&A | Exchange demo/release verification |
| 5 Sep | Submission | Submit verified deliverables | Support verification | Support verification | Support verification |

No new feature begins after 29 August. Only release-blocking correctness, security, accessibility, and crash fixes are accepted after the release candidate.

## 15. Native implementation sequence

Repository cleanup is complete: on 5 August 2026, the user explicitly authorised direct deletion of the obsolete Expo/React Native source, Node/Expo configuration and dependencies, generated output, placeholder services/database files, default Expo assets/licence boilerplate, and old `.env`. The user subsequently authorised the native Gradle skeleton and, on 9 August 2026, the proposal-parity UI milestone.

The environment, proposal-parity UI, Phase 2A Firebase essentials, narrow Phase 2B AI scanner, Phase 2C.1 marketplace owner management, Phase 2C.2 marketplace images, Phase 2D recycling-centre map, Phase 2E P2P lending, and Phase 2F release hardening portions of this sequence are authorised. Later integrations still require their relevant decision gates:

1. Review and approve this plan, open decisions, namespace, design assets/tokens, and ownership.
2. Retrieve/recreate Firebase, Maps, and AI configuration from the owning consoles; do not treat the deleted `.env` or Git history as a secret store.
3. Create the native Gradle project at the repository root with one `:app` module. **Completed as an environment-only skeleton:** Gradle wrapper, root/app Groovy build scripts, API 24/36 configuration, provisional application ID, minimal manifest, and empty non-code placeholders.
4. **Completed:** configure feature-level Views/XML and Navigation, then implement and verify the twenty PDF screens using local mock content.
5. **Phase 2A app-side implementation and local verification completed:** add only Firebase Authentication and Cloud Firestore for email/password accounts, text-only marketplace listings, and participant-only listing chat; include deny-by-default rules, indexes, emulator tests, and the developer setup guide. Production Rules/index deployment and the two-account live smoke test still follow `docs/FIREBASE_SETUP.md` as owner checks.
6. **Phase 2B authorised:** implement only CameraX capture, Android Photo Picker, bounded temporary image handling, authenticated Firebase AI Logic/Gemini structured analysis, build-specific App Check, result review states, local parser/image-bound tests, and `docs/AI_SCANNER_SETUP.md`.
7. The project owner enables AI Logic with the Gemini Developer API, enforces authenticated-users mode in AI Logic Settings, registers each developer's debug token, verifies AI Logic enforcement and one live request, reviews quotas/budget alerts, and later completes release Play Integrity/SHA-256 setup.
8. **Phase 2C.1 authorised:** add owner-only text editing plus `available`/`withdrawn` management using the existing Auth/Firestore stack; keep images, Maps, Room, lending, activity, notifications, and deletion deferred in that slice.
9. **Phase 2C.2 authorised:** add one safely prepared marketplace image, authenticated Storage upload/display/replacement, matching Firestore/Storage Rules, local tests, and `docs/MARKETPLACE_IMAGE_SETUP.md`; keep other images and integrations deferred.
10. **Phase 2D authorised:** make only the Recycling Centre screen functional with Maps SDK, Places Text Search (New), one-time foreground location, manual area fallback, restricted-key setup, local policy tests, and `docs/RECYCLE_MAP_SETUP.md`.
11. **Phase 2E authorised and app-side complete:** add one optional protected lending image, real-time list/optional map, bounded date requests and booked-day locks, owner decisions, pickup/return/rating, lending chat, in-app request actions, Rules tests, and `docs/LENDING_SETUP.md`; production deployment/live evidence remains an owner task.
12. **Phase 2F authorised and in progress:** freeze features; add GitHub and Windows preflight checks; consolidate production Rules, two-account/two-device, AI, Maps, accessibility, signed-APK, hash, and evidence steps in `docs/PHASE_2F_RELEASE_CHECKLIST.md`. Automation must not receive secrets, deploy production, create signing keys, or claim live success.
13. After separate approval for each remaining service, implement later scheduled vertical slices without restoring or converting TypeScript/Expo source.
14. Run structural review against all twenty PDF mock-ups and the two explicitly derived completion surfaces, prioritising accessibility and usability over pixel-perfect copying.
15. Pass security, adaptive-layout, offline/account-switch, test, and release gates before submission.

Brownfield React Native embedding and automated TSX-to-Java conversion are out of scope because the selected target is one clean native architecture.

## 16. Risk register

| Risk | Likelihood / impact | Mitigation and trigger |
|---|---|---|
| Native rewrite close to 5 September | High / Critical | Four vertical workstreams, daily integration, 29 Aug freeze; re-baseline immediately if approval slips |
| Removed prototype contained an undocumented visual detail | Medium / Medium | Use the proposal PDF as authority, obtain designer sign-off per screen, and do not assume deleted untracked files are recoverable |
| Java examples lag Kotlin-first documentation | Medium / Medium | Use APIs with official Java support; Room 2.8, Java Workers, Java ViewModels, Java Firebase AI Logic; spike dependencies at foundation gate |
| AI response is invalid/inaccurate | High / High | Structured schema validation, uncalibrated-estimate labels, user review, cache versioning, prepared fallback demo |
| AI/API abuse or leaked key | Medium / High | Firebase AI Logic proxy, authenticated access, App Check, project quotas/budget alerts, stable model pin, restricted configuration, no direct key; approve a backend before claiming per-user enforcement |
| Team lacks Play Console/project-Owner access for Play Integrity | Medium / High | Verify access at foundation gate; never use debug provider in release; leave enforcement off with documented lower-assurance restrictions or approve an OD-13 alternative |
| Firestore search or radius assumptions fail | Medium / High | Explicit prefix/filter strategy, geohash bounding queries, exact client distance filtering, tests at range edges |
| Recycling centre data is incomplete | Medium / Medium | Nearby plus text search in English/Malay, manual search, attribution, user confirmation message |
| Lending double booking/trust manipulation | Medium / High | Bounded transaction over deterministic booked-day documents, concurrency tests, immutable eligible rating links, client-calculated rating display unless trusted backend approved |
| Chat listener cost/volume | Medium / Medium | Thread-scoped listeners, pagination, message limits, and cleanup from the owning screen/ViewModel lifecycle |
| Maps/Places billing changes | Medium / Medium | Field masks, bounded requests, debounce, budget alerts, no stale hard-coded free-tier claim |
| AppGallery device lacks Google services | High / High on affected devices | Course APK baseline uses GMS; decide separately between GMS-only disclosure and a later HMS provider abstraction |
| Permission denial blocks a demo | Medium / High | Gallery and manual-location fallbacks plus preflight permission rehearsal |
| Low-end device jank from maps/images/glass effects | Medium / Medium | Thumbnailing, pagination, API-aware visual fallback, physical low-memory test |
| Phone-only UI fails on API 36 large screens | Medium / High | `sw600dp` portrait/landscape/resizing/split-screen test matrix and adaptive XML constraints before UI sign-off |
| Plan implies server guarantees without a backend | Medium / High | Baseline excludes automated push/schedules/global aggregates/orphan cleanup; OD-13 must name a service, owner, tests, deployment, and schedule before those claims are enabled |
| Team merge conflicts | Medium / High | Stable contracts, package ownership, small PRs, daily integration, shared-file coordination |
| Report/presentation squeezed by rewrite | High / High | Capture screenshots/contribution notes per DoD and reserve 30 Aug onward for hardening/report |

## 17. Open decisions requiring team confirmation

These choices do not block documentation, but they must be closed before the related implementation or release gate. OD-02 is resolved for Phase 2A. Phase 2B fixes the debug-versus-release App Check provider policy. Phase 2C.1 fixes the current marketplace transaction, fulfilment, and status vocabulary. Phase 2C.2 fixes the one-image Storage path and authenticated-read policy. Phase 2D fixes the recycling-centre location privacy and search policy. Phase 2E fixes lending as free borrowing with an optional informational deposit and optional privacy-rounded map point.

| ID | Decision | Recommended default |
|---|---|---|
| OD-01 | Android application ID/namespace | `com.propcycle.app` is already registered in the shared development Firebase project; confirm it before release signing because changing it requires a new Android/Firebase app registration |
| OD-02 | Authentication promise in the mock-up | **Resolved for Phase 2A:** email/password is implemented. Phone, username, and social authentication are deferred and must not be implied by current UI copy |
| OD-03 | Firebase environments | Separate development/emulator data from the final production project |
| OD-04 | Final colours, font, logo, and icon | **Resolved for the current UI pass:** use a soft light eco-colour theme while preserving the proposal structure; supplied brand fonts, logo, and final launcher artwork remain deferred until the team provides them |
| OD-05 | Exact radial and hamburger-menu destinations | **Resolved for Home:** the existing hamburger toggles a labelled Market/Share/Map fan; Market opens marketplace browse, Share opens Lend Resource, and Map opens Lending Map. No separate production drawer has been approved |
| OD-06 | Push-notification sender | In-app notifications are mandatory; use a trusted Java-capable backend/event sender before enabling automated FCM push |
| OD-07 | Public item location precision | Default to area/meeting point rather than a private exact address |
| OD-08 | Store publication | Signed course APK first; AppGallery/Play submission only after GMS/store-policy review |
| OD-09 | Microphone icon in home search | Remove it unless voice search is a real accepted requirement; do not ship an inert control or request microphone permission unnecessarily |
| OD-10 | Earlier-plan enhancements not required by the PDF | Keep text-only scanner lookup, home impact/statistics/tip panels, and marketplace map/list switch out of the baseline unless the designer/team explicitly schedules them |
| OD-11 | Marketplace semantics | **Resolved for Phase 2C.1:** transaction intent is `sale`, `donation`, or `exchange`; fulfilment is `pickup` or `meetup`; status is `available` or `withdrawn`. Marketplace map/location and later reservation/completion semantics remain separately deferred |
| OD-12 | Proposal wording "rent or borrow" | **Resolved for Phase 2E:** borrowing is free; an owner may state an optional refundable deposit arranged outside PropCycle; no rental fee or in-app payment is shown |
| OD-13 | Trusted automation backend | Baseline has no custom backend. If automated FCM, cross-device schedules, stored global/eco/trust aggregates, orphan cleanup, or abuse-resistant per-user AI limits are required, select a Java-capable trusted service, owner, tests, secrets/billing plan, and schedule first |
| OD-14 | App Check release prerequisites | **Provider policy resolved for Phase 2B:** debug provider only in debug builds and Play Integrity only in release builds. The owner still confirms the Play entry/project links/permissions/release SHA-256, tests the exact signed APK, and records any non-enforced restriction; never use a debug token/provider in release |

Current registration/login text must claim only the implemented email/password behaviour. Phone/username/social methods remain deferred; password-reset, confirmation, and terms UI also require designer placement sign-off. Until OD-06 and OD-13 are closed with an implemented sender, the notification screen remains functional in-app but automated operating-system push is not represented as complete.

## 18. Official implementation references

- [Android app architecture](https://developer.android.com/topic/architecture)
- [XML layouts with Android Views](https://developer.android.com/develop/ui/views/layout/declaring-layout)
- [View Binding](https://developer.android.com/topic/libraries/view-binding)
- [Java versions in Android builds](https://developer.android.com/build/jdks)
- [Google Play target API requirement](https://developer.android.com/google/play/requirements/target-sdk)
- [Room](https://developer.android.com/training/data-storage/room/)
- [Room 2.8 release notes and schema tooling](https://developer.android.com/jetpack/androidx/releases/room)
- [CameraX image capture](https://developer.android.com/media/camera/camerax/take-photo)
- [Android Photo Picker and durable media access](https://developer.android.com/training/data-storage/shared/photo-picker)
- [WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent)
- [Android 16 large-screen behaviour changes](https://developer.android.com/about/versions/16/behavior-changes-16)
- [Maps SDK for Android](https://developers.google.com/maps/documentation/android-sdk/start)
- [Maps API key security](https://developers.google.com/maps/api-security-best-practices)
- [Secrets Gradle plugin](https://developers.google.com/maps/documentation/android-sdk/secrets-gradle-plugin)
- [Places Text Search](https://developers.google.com/maps/documentation/places/android-sdk/text-search)
- [Places SDK policies](https://developers.google.com/maps/documentation/places/android-sdk/policies)
- [Firebase AI Logic](https://firebase.google.com/docs/ai-logic)
- [Firebase AI Logic Android setup](https://firebase.google.com/docs/ai-logic/get-started?platform=android)
- [Firebase AI Logic multimodal generation](https://firebase.google.com/docs/ai-logic/generate-text?platform=android)
- [Firebase AI Logic structured output](https://firebase.google.com/docs/ai-logic/generate-structured-output?platform=android)
- [Firebase AI production checklist](https://firebase.google.com/docs/ai-logic/production-checklist)
- [Firebase App Check debug provider](https://firebase.google.com/docs/app-check/android/debug-provider)
- [Cloud Firestore real-time listeners](https://firebase.google.com/docs/firestore/query-data/listen)
- [Cloud Storage Security Rules conditions](https://firebase.google.com/docs/storage/security/rules-conditions)
- [Cloud Firestore Android transactions](https://firebase.google.com/docs/reference/android/com/google/firebase/firestore/Transaction)
- [Firebase App Check with Play Integrity outside Google Play](https://firebase.google.com/docs/app-check/android/play-integrity-provider)

## 19. Plan approval checklist

The UI milestone, Phase 2A Firebase essentials, narrow Phase 2B AI scanner, Phase 2C.1 marketplace owner management, Phase 2C.2 marketplace images, Phase 2D recycling-centre map, Phase 2E P2P lending, Phase 2F release hardening, and Phase 2G functional journey correction are authorised. Later production functionality begins only after the team confirms the relevant items below:

- [ ] Every proposal screen/module is represented correctly.
- [ ] Java/XML/no-Compose policy is accepted.
- [ ] API 24 minimum and API 36 target are accepted.
- [x] Screen IDs, navigation model, and designer Home fan are accepted for the current UI pass.
- [x] Phase 2B scanner uses temporary app-private files and Firebase AI Logic only; no raw key, Cloud Storage, Room/history, map, or automatic publish is opened.
- [x] Phase 2C.1 uses only existing Auth/Firestore for owner text editing and `available`/`withdrawn`; images, deletion, map, reservation/completion, activity, and notifications remain deferred.
- [x] Phase 2C.2 uses one bounded JPEG, an owner/listing-scoped Storage path, authenticated display, and private `gs://` references; multiple images, deletion, map, lending, and background cleanup remain deferred.
- [x] Phase 2D uses only foreground one-time location plus manual area search, Maps/Places Android SDKs, approximate local distance, and restricted local key setup; routes, tracking, history, lending/marketplace maps, and acceptance claims remain deferred.
- [x] Phase 2E uses authenticated item/list/map discovery, one protected JPEG, optional rounded location, bounded request/date locks, in-app lifecycle actions, participant chat, return/rating, and no payments or OS push; live owner checks remain in `docs/LENDING_SETUP.md`.
- [x] Phase 2F freezes features and adds secret-free GitHub/local gates plus one consolidated live/device/signing evidence checklist; automated production deployment and signing remain prohibited.
- [x] Phase 2G explicitly authorises the release-correctness exception: editable AI/photo handoff, item-first lending Map/List state, Room scan/activity history, and truthful Home/Profile/Recent/Settings logic.
- [x] Current Firebase/Room/Maps data ownership is accepted for the implemented slices; future drafts/outbox/background work remains gated.
- [ ] Authentication alternatives and optional enhancements are closed; current marketplace semantics and lending fee/deposit wording are resolved for the assessed build.
- [ ] Push/trusted-backend scope has a named owner and deadline, or the excluded automation is explicitly accepted.
- [ ] Play Console/project-Owner access and App Check enforcement/fallback decision are recorded.
- [ ] The direct Expo/RN deletion and root-native bootstrap path are acknowledged; required service configuration will be recreated securely.
- [ ] Team ownership and remaining schedule are realistic.
- [ ] Security, testing, feature-freeze, and release gates are accepted.

Once checked, record the approver names/date at the top of this document and authorise the related functional milestone as a separate task.
