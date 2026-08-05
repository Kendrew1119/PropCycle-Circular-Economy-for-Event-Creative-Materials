# PropCycle Native Android Master Plan

> **Planning baseline:** Group7-PropCycle proposal dated 15 July 2026<br>
> **Re-planning date:** 5 August 2026<br>
> **Course:** UCCD3223 Mobile Applications Development, June 2026 Trimester<br>
> **Team:** Group 7, four members<br>
> **Status:** Environment bootstrap authorised; no feature implementation has started<br>
> **Target platform:** Android only<br>
> **Primary implementation language:** Java

This document is the single source of truth for the native Android implementation. The proposal's modules and designer mock-ups remain the product baseline. The technology changes from React Native with Expo to a native Android application; it does not remove any product module.

## 1. Decision summary

### Non-negotiable scope rules

1. Retain every proposal module and supporting screen.
2. Treat the proposal mock-ups as the screen and flow authority.
3. Use native Android Views with XML layouts; do not use Jetpack Compose.
4. Write team-owned Android application source in Java.
5. Environment/bootstrap files may be created; do not begin native feature implementation until the team approves the remaining open decisions.
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
| Local database | Room 2.8.x over SQLite, using Java `annotationProcessor` | Compile-time SQL checks while meeting the device-storage requirement; Room 3 is excluded because it requires Kotlin/KSP |
| Cloud data | Firebase Authentication, Cloud Firestore, Cloud Storage | Preserves the proposal's Firebase backend; Firestore snapshot listeners provide real-time chat without cross-database rule gaps |
| AI | Firebase AI Logic Android SDK for Java with App Check and Remote Config | Keeps the Gemini key off the client and supports model/prompt updates |
| Camera/media | CameraX plus Android Photo Picker | Native capture and gallery selection with modern permission handling |
| Maps/location | Maps SDK for Android, Places SDK for Android, Fused Location Provider | Native map, nearby recycling centre, listing, and lending flows |
| Background work | WorkManager Java `Worker` | Reliable draft/upload retry and deferred sync |
| Dependency injection | Hilt with Java annotation processing | Consistent construction, compile-time checks, and test replacement |
| Distribution | Signed APK for course submission; AAB/store release after compatibility review | Separates the assessed deliverable from optional store publication |

Version numbers other than the Android API baseline are resolved and locked at project bootstrap. Only stable releases compatible with JDK 17, Java source, and the chosen Android Gradle Plugin may be used.

## 2. Course constraints and delivery targets

| Milestone | Date | Planning status |
|---|---|---|
| Part 1 proposal | 15 July 2026, before 5:00 PM | Past milestone; confirm the team's submission record; PDF is the scope baseline |
| Native plan approval | Target: 6 August 2026 | Pending team approval |
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
| Supporting - Profile and settings | Personal information, avatar, eco badges, owned activity, scan history, preferences, account details, logout | A user can inspect and manage their account and activity |
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
| HOME-01 | Home dashboard | PDF mock-up | Greeting, resource search, Smart Scan card, Recent Activities card, notification/profile icons, and designer radial quick menu; microphone only if voice search is approved | Every control has one destination, an accessible label, and a 48dp target; no decorative control appears tappable |
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
| USER-02 | Profile | PDF mock-up + platform completion | Avatar, name, eco/badge evidence, rating/trust evidence, published listings summary, owner-only edit action | Shows only permitted public/private fields and opens owned content/profile editing |
| USER-03 | Settings | PDF mock-up + platform completion | Theme, notifications, location preference, account details management, logout | Preferences persist and system permissions are never represented inaccurately |
| USER-04 | My activity | Existing-function consolidation | My listings, my lent items, requests, scan history, achievements/badges | Each subsection shows current data and supports its owner actions without becoming a new top-level module |

### Navigation model

- `LauncherActivity` is not required; `MainActivity` hosts one `NavHostFragment`.
- An authentication graph contains AUTH-01 through AUTH-03.
- An authenticated graph contains HOME-01 and every feature graph.
- HOME-01 preserves the designer's radial quick menu. The PDF currently indicates Market, Share/Lend, and Map; final labelled destinations require designer sign-off. Smart Scan and Profile already have direct home controls and must not be duplicated without a reason.
- The PDF draws a hamburger control but does not define its contents. A candidate accessible menu may duplicate Home, Messages, Notifications, Profile, Settings, and Logout; its inclusion, order, labels, and destinations require designer/team sign-off under OD-05.
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
| Platform/integration | Firebase, Room, CameraX, Maps/Places, Fused Location, WorkManager, Remote Config | Wrapped so screens do not depend on vendor APIs |

### State and concurrency policy

- UI state flows from repository/use case to ViewModel to Fragment.
- User events flow from Fragment to ViewModel and then to the data/domain layer.
- Firebase callbacks, Guava `ListenableFuture`, Room background executors, and WorkManager are adapted behind repositories.
- No database, image processing, JSON parsing, or network work runs on the main thread.
- Each asynchronous screen exposes loading, content, empty, permission-required, recoverable-error, and terminal-error states where applicable.
- View Binding references are cleared in every Fragment's `onDestroyView()`.
- `SavedStateHandle` preserves IDs, filters, form drafts, and unsent text needed after process or configuration recreation.

### Planned project structure

The native project is created only after approval. It starts as one Gradle `:app` module for delivery speed, with package-by-feature boundaries.

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
| Direct Gemini REST key | Firebase AI Logic Java SDK, App Check, and Remote Config |
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
| Images | Cloud Storage after publish intent; UID-scoped app-private draft copy or durable picker grant before upload | WorkManager retries only for the same authenticated UID; final metadata refers to storage paths |
| Scan cache/history/drafts/outbox | Room, scoped by initiating account UID | Fully readable for that account; outbox retries only idempotent operations and never crosses accounts |
| Theme and non-sensitive settings | Preferences DataStore through its RxJava Java API | Immediate local persistence behind a `SettingsDataSource` |
| AI model/prompt/cache policy | Firebase Remote Config with a safe, exact stable model version and prompt version | Last activated configuration remains available; preview/moving model aliases are not release defaults |
| Optional home enhancements | No source until OD-10 is approved | Daily tip/statistics/impact panels remain absent rather than displaying invented data |
| Map/Places results | Google SDK responses plus short-lived memory cache | Saved item coordinates still render; live nearby search requires a connection |

### Cloud Firestore collections

| Path | Required fields and ownership |
|---|---|
| `users/{uid}` | `displayName`, `username` only if approved, avatar storage path, coarse public location label, badge evidence, timestamps; one general account acts contextually and the owner writes only profile-safe fields |
| `marketplaceListings/{listingId}` | owner ID, title, description, image storage paths, category, material, transaction intent (`sale`, `donation`, `exchange`), fulfilment method (`pickup`, `meetup`, or approved equivalent), price/exchange terms when applicable, condition, coordinates/geohash, location label, status, AI-origin flag, timestamps |
| `lendingItems/{itemId}` | owner ID, title, description, image storage paths, category, availability rules, approved fee/deposit wording, pickup method, coordinates/geohash, status, timestamps |
| `borrowRequests/{requestId}` | Participant-only item, borrower, owner, Malaysia calendar start/end dates, opaque lock token after approval, message, status, decision/activation/return timestamps |
| `ratings/{requestId}_{raterUid}` | completed request, rater, recipient, score, optional short review, timestamp; deterministic ID enforces one immutable rating per eligible participant/request |
| `activities/{activityId}` | actor, type, related object, user-safe summary, timestamp; used by recent activity and eco evidence |
| `notifications/{uid}/items/{notificationId}` | Non-chat domain event type, title/body, related object/destination, read flag, timestamp; written in the same Firestore batch/transaction as a rule-valid transition where feasible |
| `scanHistory/{uid}/items/{scanId}` | Optional signed-in backup of reviewed scan summary/action; raw scan images may be transmitted for AI analysis but are not persisted here |
| `lendingItems/{itemId}/bookedDays/{yyyy-MM-dd}` | Public privacy-minimal availability lock containing only `booked`, opaque random lock token, and update timestamp; borrower/request identity stays in participant-only request data |

Firestore is not treated as a full-text search engine. The release search plan is normalised title-prefix search plus server-side category/status/geohash filters and local filtering of the loaded result window. A hosted search service is future scope only if the dataset outgrows this approach.

### Cloud Firestore chat shape

| Path | Content |
|---|---|
| `chatThreads/{contextType}_{contextId}_{ownerUid}_{contactUid}` | Deterministic context-bound ID; immutable owner/contact participant UIDs, related listing/item ID/type, last-message preview/time, per-user unread/last-read metadata |
| `chatThreads/{chatId}/messages/{messageId}` | Sender UID, text, client operation ID, server timestamp, delivery state; immutable after acknowledged send |

Firestore Rules allow access only when `auth.uid` is an immutable thread participant. On creation, rules validate the deterministic ID/fields against the related Firestore listing or lending item, require its real owner as `ownerUid`, require the authenticated user to be the owner or contact, and prevent participant/context changes. A user cannot forge another sender UID, mutate an acknowledged message, or join a thread by guessing an ID. Message unread state remains on the thread and is merged with non-chat domain notifications in USER-01, avoiding an unverifiable cross-database notification write. Online presence is excluded because it is not required by the proposal.

### Cloud Storage paths

- `users/{uid}/avatar/...`
- `marketplace/{uid}/{listingId}/{imageId}.jpg`
- `lending/{uid}/{itemId}/{imageId}.jpg`

Local drafts keep images app-private; cloud image upload starts only after an explicit publish action. An owner-only Firestore `DRAFT` metadata document supports lifecycle/recovery, but does not authorise Cloud Storage because Storage Rules cannot read Firestore. Storage writes/deletes require `request.auth.uid` to match the `{uid}` path segment, valid MIME type/size, and agreeing owner metadata. List access is denied. Authenticated reads are allowed for marketplace/lending image paths because other users must view published content; authenticated avatar reads are allowed only for non-sensitive profile avatars, while avatar writes/deletes remain owner-only. The app reveals unguessable content-image paths only from a published Firestore document. This creates a short staging interval that is not strict atomic visibility—if strict owner-only staging followed by rule-verified publication is required, OD-13 must provide a trusted promotion/signed-access design. Firestore Rules separately validate document ownership and image-manifest length. Neither rule system is claimed to count sibling files. Automatic orphan cleanup is also not promised unless OD-13 selects a trusted backend.

### Room 2.8.x tables

| Table | Purpose and key fields |
|---|---|
| `scan_cache` | Owner UID/account scope, cache key, input type/hash, prompt version, model configuration, structured JSON, created/expiry timestamps |
| `scan_history` | Owner UID, local ID/cloud ID, reviewed item/material/category, app-private image reference when retained, structured result, action, sync state, scan timestamp |
| `draft_posts` | Owner UID, draft type, editable fields, app-private image references/durable grants, location, updated timestamp, publish state |
| `sync_outbox` | Owner UID, unique operation ID, entity type/ID, operation, payload reference, attempt count, next retry, last error |

Room migrations are mandatory from the first released schema. Configure `room.schemaLocation`, commit exported schema JSON, and test every migration path. Destructive fallback is prohibited for user-created history and drafts. On logout, cancel that UID's scheduled Workers and prevent its rows from rendering; every Worker verifies that its recorded owner UID matches the current Firebase user before reading files or performing a remote write.

Firestore persistent disk caching is disabled to avoid account A data remaining available to account B. Repositories use in-memory snapshots for public lists and UID-scoped Room for intentional private offline data. Remote writes require connectivity and explicit completion; the app does not present an unacknowledged Firestore SDK write as saved. Logout blocks new actions, detaches all listeners, invalidates callbacks/session generation, cancels UID Workers, and waits for tracked writes to finish or fail before changing account. Immutable owner/sender fields plus Security Rules reject any late operation under a different UID. The account-switch test is: sign in as A, read/write private data, go offline, log out, sign in as B, and verify that A's profile, drafts, messages, notifications, files, and pending actions cannot render or sync.

## 8. Core business flows and state machines

### AI scan and handoff

1. User captures an image or picks a photo. Text-only lookup is added only if OD-10 is approved.
2. App normalises the input, rotates correctly, removes unnecessary EXIF location metadata, and compresses a working copy.
3. Repository checks `scan_cache` using input hash plus prompt/model version.
4. On a miss, the app discloses that the working image will be transmitted to Firebase AI Logic/Gemini for analysis, then requests a structured response after the user proceeds.
5. The response is schema-validated. Unknown categories, missing fields, invalid numbers, or unsafe content produce a review/error state rather than a crash.
6. SCAN-02 displays item name, category, material, recyclable status, local-context guidance, upcycling ideas, impact estimate, and an explicitly labelled uncalibrated model estimate rather than a guaranteed confidence score.
7. User reviews and can edit publishable fields.
8. Save writes local history. Recycle opens MAP-01. Marketplace opens MARKET-03. Lend opens LEND-04.
9. Source image and AI fields transfer through a graph-scoped ViewModel or saved draft ID, not a global mutable singleton.

Gallery/camera media that must survive beyond the current screen is copied into UID-scoped app-private draft storage before Room or WorkManager references it. A durable picker permission may be retained where the returned URI/provider supports it, but a short-lived Photo Picker URI is never assumed to survive process death or deferred work.

The category enum includes every retained material category: banner, decoration, fabric, stationery, craft, cosplay, toys/miniatures, wood, electronics, packaging, and other.

Default cache expiry is seven days, but Remote Config can change it. Prompt/model changes invalidate old cache entries. Release configuration uses an exact stable model version, not a preview or moving alias. AI access requires an authenticated user, project quotas, and budget alerts; App Check is enforced only when OD-14 prerequisites pass, and abuse-resistant per-user limits require OD-13 trusted logic. AI impact values are estimates and are labelled accordingly.

### Marketplace listing lifecycle

```text
local draft -> available -> reserved -> completed
                    |            |
                    +-> withdrawn+-> available (reservation cancelled)
```

- Transaction intent is `sale`, `donation`, or `exchange`; fulfilment is a separate `pickup`, `meetup`, or other approved handover value. The designer must confirm how the mock-up's "pickup" label maps to these fields.
- Donation requires price `0`; sale requires a non-negative RM price; exchange requires a short description of what the owner will consider. Fulfilment states who arranges collection and never implies in-app delivery or payment.
- Only the owner can edit, withdraw, reserve, or complete a listing.
- Non-owners can view and open a related chat/request action.
- Completion records recent activity. Eco/impact credit from an owner-only completion is labelled self-reported; stronger badge/impact evidence requires counterparty confirmation or OD-13 trusted computation, so the baseline does not claim a tamper-proof eco score.

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
- Store item coordinates and a geohash; query geohash ranges, then calculate and filter exact distance client-side.
- Recycling search uses Places Nearby/Text Search with Malaysia-oriented terms such as `recycling centre` and `kitar semula`; results show required Google attribution.
- Request only the place fields needed for the screen to control latency and billing.
- External directions open an installed map application through an intent; PropCycle does not implement turn-by-turn navigation.

### Notifications

- USER-01 merges unread Firestore chat-thread state with non-chat notification documents for marketplace status, borrow requests/decisions, return events, and ratings. Chat does not create a second cross-product notification record. Non-chat writes must be included with and rule-validated against the related Firestore transition where feasible.
- Device-local WorkManager reminders may cover due/return dates for the signed-in device. Cross-device scheduled notifications and automated FCM push are enabled only if OD-13 selects a trusted event sender; the Android client never holds service-account credentials.
- Android 13+ notification permission is requested in context after explaining its value.
- Denying push does not disable the in-app notification page.

### Trusted automation boundary

The assessed baseline does not assume a custom trusted backend. Consequently, automated FCM sending, cross-device schedules, global community statistics, stored eco/trust aggregates, and automatic orphan cleanup are not release guarantees. If the team requires them, OD-13 must select a Java-capable trusted service, name an owner, add emulator/integration and abuse tests, define deployment/secrets/billing controls, and place that work on the schedule before implementation. Client code and Security Rules alone must not be described as trusted server computation.

## 9. UI and design-system plan

### Designer fidelity rules

- Preserve the proposal's content hierarchy, cards, arrows/flows, screen purpose, and home radial quick menu.
- Convert the monochrome wireframes into Android XML without inventing a new information architecture.
- Use Android-native back behaviour, permissions, text scaling, insets, keyboard handling, and feedback.
- Resolve a designer sign-off screenshot for each screen ID before marking UI complete.
- Do not copy iPhone notches, home indicators, or unsafe fixed pixel measurements into the Android app.

### Provisional visual tokens

The proposal is monochrome, so colour and typography remain subject to designer sign-off. The following palette is provisional planning data only; no reusable Expo design asset or token file remains in the repository.

| Token | Working value | Use |
|---|---|---|
| Primary | `#2D6A4F` | Main actions, active state, headers |
| Secondary | `#95D5B2` | Highlights and supporting surfaces |
| Accent | `#D4A373` | Eco badges and attention accents |
| Error | `#E63946` | Destructive/error states only |
| Light background | `#F8F9FA` | Default light surface |
| Dark background | `#0D1B14` | Default dark surface |
| Text light theme | `#1A1A2E` | Primary text |
| Text dark theme | `#F8F9FA` | Primary text on dark surfaces |

- Use resource tokens, not hard-coded colours/dimensions in Java or individual layouts.
- Use an 8dp spacing grid with documented 4dp exceptions, scalable `sp` text, and reusable shape styles.
- Glass-like cards use translucent surfaces, borders, elevation, and gradients. Blur is an enhancement only; a high-contrast fallback is mandatory on unsupported/slow devices.
- Animations must not block navigation, and reduced-motion behaviour must remain understandable.
- Target API 36 screens are edge-to-edge and apply status bar, navigation bar, display-cutout, and IME insets correctly.
- Navigation uses the current predictive-back APIs and Navigation Component integration rather than legacy custom back interception.

### Accessibility and adaptive checks

- Minimum 48dp touch target for every interactive element, including the radial menu.
- Meaningful `contentDescription` for non-text controls; decorative images are excluded from accessibility focus.
- Logical TalkBack focus order, headings, state announcements, and labelled form errors.
- Text remains usable at 200% font scale without clipping critical actions.
- Colour is never the only status signal; contrast is verified in light and dark themes.
- Phone layouts are portrait-first. API 36 testing also covers at least one `sw600dp` tablet/foldable in portrait, landscape, freely resized, and split-screen modes because large-screen orientation/resizability restrictions cannot be relied on.
- The radial menu has equivalent labelled actions in the navigation menu for switch-access and TalkBack users.

## 10. Security, privacy, and validation

### Security controls

- Preferred release protection is Firebase App Check with Play Integrity. It requires a Google Play Console app entry, linking the same Cloud/Firebase project, direct project Owner permission, and registration of the release SHA-256. For the sideloaded course APK, configure outside-Google-Play settings: `PLAY_RECOGNIZED` not required, `LICENSED` not required, and device integrity required. Test the exact signed APK, monitor metrics, and enforce only after legitimate requests are verified.
- If the team cannot satisfy those Play Console/Owner prerequisites by the foundation gate, do not ship a release build with the debug provider and do not enable enforcement that blocks the demo. Record the lower-assurance fallback: Authentication plus tested Security Rules, restricted keys, App Check metrics where available, strict AI/project quotas and budget alerts; OD-13 is required for a custom attestation/proxy alternative.
- Firebase Authentication is required for all user data, listings, lending, chat, ratings, and uploads.
- Deploy and test Firestore and Storage Rules before connecting the release build.
- Validate ownership and state transitions in both client code and Security Rules. Use trusted backend logic only for the capabilities explicitly approved in OD-13.
- Restrict Maps/Places keys to the Android application ID and signing certificate; restrict enabled APIs.
- Firebase AI Logic keeps the Gemini provider key behind its proxy. Exact stable model version, prompt version, safety settings, and cache policy come from Remote Config with safe defaults; preview/moving aliases are not release defaults.
- Never log passwords, tokens, message bodies, exact private locations, raw AI images, or full user documents.
- Release builds disable debug logging, shrink/obfuscate where compatible, and use a team-controlled signing key backup.
- Use Network Security Configuration to disallow cleartext traffic and to document any exceptional trust configuration.
- Define backup/data-extraction rules so credentials, tokens, private caches, and sensitive local files are not included in device backup.
- Mark Android components non-exported unless a documented external entry point requires exposure.

### Privacy and permission policy

| Capability | Permission/data policy |
|---|---|
| Camera | Ask only from SCAN-01; explain why; gallery remains available after denial |
| Gallery | Use the system Photo Picker to avoid broad media access on supported versions |
| Location | Foreground only, approximate accepted, manual fallback, no background tracking |
| Notifications | Ask in context; in-app notifications remain available after denial |
| Scan images | Explain that analysis transmits the working image to Firebase AI Logic/Gemini for transient processing; keep local working files app-private and persist to Cloud Storage only for an explicit publish/backup action |
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

- Saved scan history, valid cached scan results, and drafts are readable offline.
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
| Plain JVM tests | Validators, category mapping, cache keys, AI schema parser, eco/trust calculations, search normalisation, state machines, date overlap, error mapping |
| Repository tests | Success/error/offline mapping with fake local and remote data sources |
| Room instrumentation | DAO CRUD, owner-scope isolation, query results, committed/exported schema JSON, migration preservation, outbox retry selection and logout cancellation |
| Firebase Emulator Suite | Auth-dependent Firestore/Storage rule allow/deny cases, deterministic chat/rating identities, lending-lock transitions, and representative data flows |
| Fragment/navigation tests | Destination arguments, authentication guards, back stack, process/state restoration |
| Espresso journeys | Register/login, scan-to-save, scan-to-listing, marketplace-to-chat, lending request-to-return/rating, settings/logout |
| Manual device matrix | API 24, API 33, API 36; camera/no camera, approximate/denied location, notification denial, slow/offline network, dark mode, large text, rotation/process recreation; API 36 `sw600dp` portrait/landscape/resizing/split-screen |

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
- App Check either accepts the exact sideloaded signed course APK under the reviewed outside-Play policy before enforcement, or OD-14 records the non-enforced lower-assurance fallback and its restrictions.
- Signed APK installs and launches on two physical Android devices.
- Fresh install, upgrade install, logout/login, offline restart, and denied-permission smoke tests pass.
- Crash-free 10-minute presentation rehearsal with a prepared fallback dataset and cached scan.
- APK hash, signing key custody, final report, screenshots, and contribution log are archived.

## 13. Team ownership and integration

Member labels remain placeholders until the team records names beside them. Ownership means primary implementation and explanation; it does not remove peer review.

| Owner | Primary native responsibility | Secondary/review responsibility |
|---|---|---|
| Member A - Lead/Core | Project bootstrap, Gradle, navigation, authentication, permissions/location, Maps/Places, release/signing | Firebase setup, integration management, final architecture/report |
| Member B - UI/UX | XML design system, reusable Views, home, all screen styling, radial menu, launcher icon, accessibility | Designer comparison set, screenshots, UI review across modules |
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
| 10-14 Aug | Scanner vertical slice | Location permission and recycling map shell | Scanner/result/recycling UI states | Camera/gallery -> AI -> review/save flow | User/activity/notification data foundations |
| 15-19 Aug | Marketplace vertical slice | Marketplace distance/geohash integration | Browse/detail/post/chat layouts | Draft/image handoff and local retry support | Listings, Storage, search/filter, chat data/rules |
| 20-24 Aug | Lending vertical slice | Lending map and date/navigation integration | Lending screens/calendar/request states | Shared image/local helpers and performance | Lending, deterministic booked-day transaction, return, eligible ratings |
| 25-27 Aug | Supporting modules | Auth guards, deep links, settings permissions | Home/recent/profile/messages/notifications visual completion | Scan history, offline and failure states | Notifications, my activity, status/ownership edge cases |
| 28-29 Aug | End-to-end integration and feature freeze | Full navigation/release build | Designer/accessibility pass | AI/offline/performance pass | Rules/emulator/data integrity pass |
| 30 Aug-1 Sep | Hardening | Device matrix, signing rehearsal | Screenshot/report assets | JVM/Room/camera tests | Firebase/rules/journey tests |
| 2 Sep | Release candidate | Produce signed RC and issue list | Final UI sign-off | Demo cache/fallback data | Firebase production rules/config sign-off |
| 3-4 Sep | Submission buffer | Fix release blockers only, assemble archive | Final screenshots/presentation | AI demo rehearsal/Q&A | Exchange demo/release verification |
| 5 Sep | Submission | Submit verified deliverables | Support verification | Support verification | Support verification |

No new feature begins after 29 August. Only release-blocking correctness, security, accessibility, and crash fixes are accepted after the release candidate.

## 15. Native bootstrap procedure - environment setup authorised

Repository cleanup is complete: on 5 August 2026, the user explicitly authorised direct deletion of the obsolete Expo/React Native source, Node/Expo configuration and dependencies, generated output, placeholder services/database files, default Expo assets/licence boilerplate, and old `.env`. The user subsequently authorised an environment-only native Gradle skeleton at the repository root; it contains no feature code, activities, layouts, or service integrations.

The environment portion of this sequence is authorised. Feature implementation begins only when the user/team explicitly authorises coding:

1. Review and approve this plan, open decisions, namespace, design assets/tokens, and ownership.
2. Retrieve/recreate Firebase, Maps, and AI configuration from the owning consoles; do not treat the deleted `.env` or Git history as a secret store.
3. Create the native Gradle project at the repository root with one `:app` module. **Completed as an environment-only skeleton:** Gradle wrapper, root/app Groovy build scripts, API 24/36 configuration, provisional application ID, minimal manifest, and empty non-code placeholders.
4. Confirm namespace, then configure feature-level Views/XML, Navigation, Hilt, Room schema export, and test runners only when coding is authorised.
5. Connect a separate development Firebase project/emulators and deploy deny-by-default rule skeletons before real data.
6. Validate Play Console/project-Owner access and the signed-APK App Check plan (or formally accept OD-14 fallback), restricted Maps key, AI quota/budget controls, and UID-scoped media/outbox design.
7. Implement the scheduled vertical slices without restoring or converting TypeScript/Expo source.
8. Run parity review against all twenty PDF mock-ups and the two explicitly derived completion surfaces.
9. Pass security, adaptive-layout, offline/account-switch, test, and release gates before submission.

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
| Chat listener cost/volume | Medium / Medium | Thread-scoped listeners, pagination, message limits, detach on lifecycle stop |
| Maps/Places billing changes | Medium / Medium | Field masks, bounded requests, debounce, budget alerts, no stale hard-coded free-tier claim |
| AppGallery device lacks Google services | High / High on affected devices | Course APK baseline uses GMS; decide separately between GMS-only disclosure and a later HMS provider abstraction |
| Permission denial blocks a demo | Medium / High | Gallery and manual-location fallbacks plus preflight permission rehearsal |
| Low-end device jank from maps/images/glass effects | Medium / Medium | Thumbnailing, pagination, API-aware visual fallback, physical low-memory test |
| Phone-only UI fails on API 36 large screens | Medium / High | `sw600dp` portrait/landscape/resizing/split-screen test matrix and adaptive XML constraints before UI sign-off |
| Plan implies server guarantees without a backend | Medium / High | Baseline excludes automated push/schedules/global aggregates/orphan cleanup; OD-13 must name a service, owner, tests, deployment, and schedule before those claims are enabled |
| Team merge conflicts | Medium / High | Stable contracts, package ownership, small PRs, daily integration, shared-file coordination |
| Report/presentation squeezed by rewrite | High / High | Capture screenshots/contribution notes per DoD and reserve 30 Aug onward for hardening/report |

## 17. Open decisions requiring team confirmation

These choices do not block documentation, but they must be closed before the related implementation begins.

| ID | Decision | Recommended default |
|---|---|---|
| OD-01 | Android application ID/namespace | Provisional `com.propcycle.app` is used only for the environment skeleton; confirm a university/team-owned reverse-domain ID before Firebase registration or signing |
| OD-02 | Authentication promise in the mock-up | Email/password for release; phone requires a separate SMS OTP flow, and username login needs a secure backend design rather than a misleading text field |
| OD-03 | Firebase environments | Separate development/emulator data from the final production project |
| OD-04 | Final colours, font, logo, and icon | Designer signs off the provisional green palette and supplied assets before UI freeze |
| OD-05 | Exact radial and hamburger-menu destinations | Radial candidates are Market, Share/Lend, and Map as annotated; the hamburger's contents are undrawn. Designer/team must approve inclusion, labels, order, and unique destinations; Smart Scan/Profile already have direct controls |
| OD-06 | Push-notification sender | In-app notifications are mandatory; use a trusted Java-capable backend/event sender before enabling automated FCM push |
| OD-07 | Public item location precision | Default to area/meeting point rather than a private exact address |
| OD-08 | Store publication | Signed course APK first; AppGallery/Play submission only after GMS/store-policy review |
| OD-09 | Microphone icon in home search | Remove it unless voice search is a real accepted requirement; do not ship an inert control or request microphone permission unnecessarily |
| OD-10 | Earlier-plan enhancements not required by the PDF | Keep text-only scanner lookup, home impact/statistics/tip panels, and marketplace map/list switch out of the baseline unless the designer/team explicitly schedules them |
| OD-11 | Marketplace semantics | Use transaction intent (`sale`, `donation`, `exchange`) separately from fulfilment (`pickup`, `meetup`); designer confirms the mock-up labels and whether Market has any map entry |
| OD-12 | Proposal wording "rent or borrow" | Default to borrowing with optional informational deposit/fee arranged in chat; no in-app payment. Team must decide whether any rental fee is permitted and update copy consistently |
| OD-13 | Trusted automation backend | Baseline has no custom backend. If automated FCM, cross-device schedules, stored global/eco/trust aggregates, orphan cleanup, or abuse-resistant per-user AI limits are required, select a Java-capable trusted service, owner, tests, secrets/billing plan, and schedule first |
| OD-14 | App Check release prerequisites | Confirm Play Console app entry, Firebase/Cloud project link, direct project Owner permission, and release SHA-256. If unavailable, accept non-enforcement plus restricted-key/rules/quota fallback; never use a debug token/provider in the release APK |

Until OD-02 is closed, registration/login text must not claim unsupported phone/username-plus-password behaviour. Password-reset, confirmation, and terms UI also require designer placement sign-off. Until OD-06 and OD-13 are closed with an implemented sender, the notification screen remains functional in-app but automated operating-system push is not represented as complete.

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
- [Places Nearby Search](https://developers.google.com/maps/documentation/places/android-sdk/nearby-search)
- [Firebase AI Logic](https://firebase.google.com/docs/ai-logic)
- [Firebase AI production checklist](https://firebase.google.com/docs/ai-logic/production-checklist)
- [Cloud Firestore real-time listeners](https://firebase.google.com/docs/firestore/query-data/listen)
- [Cloud Storage Security Rules conditions](https://firebase.google.com/docs/storage/security/rules-conditions)
- [Cloud Firestore Android transactions](https://firebase.google.com/docs/reference/android/com/google/firebase/firestore/Transaction)
- [Firebase App Check with Play Integrity outside Google Play](https://firebase.google.com/docs/app-check/android/play-integrity-provider)

## 19. Plan approval checklist

Implementation begins only after the team confirms all of the following:

- [ ] Every proposal screen/module is represented correctly.
- [ ] Java/XML/no-Compose policy is accepted.
- [ ] API 24 minimum and API 36 target are accepted.
- [ ] Screen IDs, navigation model, and designer radial menu are accepted.
- [ ] Firebase/Room/AI/Maps data ownership is accepted.
- [ ] Authentication, marketplace semantics, lending fee wording, optional enhancements, and radial-menu destinations are closed.
- [ ] Push/trusted-backend scope has a named owner and deadline, or the excluded automation is explicitly accepted.
- [ ] Play Console/project-Owner access and App Check enforcement/fallback decision are recorded.
- [ ] The direct Expo/RN deletion and root-native bootstrap path are acknowledged; required service configuration will be recreated securely.
- [ ] Team ownership and remaining schedule are realistic.
- [ ] Security, testing, feature-freeze, and release gates are accepted.

Once checked, record the approver names/date at the top of this document and authorise the native project bootstrap as a separate task.
