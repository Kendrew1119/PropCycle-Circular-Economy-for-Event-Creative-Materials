# PropCycle - Circular Economy for Event and Creative Materials

> **Course:** UCCD3223 Mobile Applications Development, June 2026 Trimester<br>
> **University:** Universiti Tunku Abdul Rahman (UTAR)<br>
> **Theme:** UN SDG 12 - Responsible Consumption and Production<br>
> **Team:** Group 7, four members<br>
> **Current phase:** Native Android planning only

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
| Local data | Room 2.8.x over SQLite; Preferences DataStore for non-sensitive settings |
| Cloud | Firebase Auth, Firestore (including real-time chat), Storage, App Check, Remote Config; FCM only with an approved trusted sender |
| AI | Firebase AI Logic Android SDK for Java with a remotely configured Gemini model |
| Camera/media | CameraX and Android Photo Picker |
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

- There is no React Native/Expo application and no native Gradle project yet.
- The repository currently retains planning documents, course documents, project/agent guidance, and generic editor settings.
- Native development will use the repository root after this plan is approved; no parallel `android-native/` tree is planned.
- The deleted `.env` is not a credential source. Firebase, Maps, and AI configuration must be retrieved or recreated through the relevant consoles during the approved setup and must never be committed.
- The deletions are currently unstaged and uncommitted, so tracked legacy files still exist in `main`/`origin` until the team commits the cleanup. Untracked files that were directly deleted have no repository recovery guarantee.

## Planning documents

- External design source: `C:\Users\B2B\Downloads\Group7-PropCycle.pdf` (inspected but not copied or edited).
- [Master native Android plan](plan.md) - complete scope, screen contract, architecture, data models, security, testing, ownership, migration, and schedule.
- [Proposal planning copy and technology addendum](proposal.md) - the product idea with the revised implementation direction noted separately.
- [Agent/development guardrails](AGENTS.md) - planning hold and rules for the later native implementation.

## Implementation approval gate

Native coding remains on hold until the team confirms:

- all proposal modules and screens are represented correctly;
- Java/XML/no-Compose direction;
- API 24 minimum and API 36 target;
- final Android namespace/application ID;
- authentication methods and copy;
- radial-menu destinations and designer tokens/assets;
- Firebase development/production setup and push-notification sender;
- Play Console/project-Owner access for App Check, or explicit acceptance of the documented non-enforcement fallback;
- team ownership, remaining dates, testing, and release gates;
- exact authentication, marketplace, lending-fee, and optional-enhancement decisions.

After approval, the team follows the native bootstrap sequence and vertical-slice schedule in `plan.md`. No automated source conversion is planned.

## Team ownership

| Workstream | Primary responsibility |
|---|---|
| Member A - Lead/Core | Native project, navigation, auth, permissions, maps/location, integration, signing/release |
| Member B - UI/UX | XML design system, screen styling, reusable Views, radial menu, accessibility, icon/screenshots |
| Member C - AI/Local | CameraX, Photo Picker, Firebase AI Logic, scan workflow, Room/cache/outbox |
| Member D - Cloud/Exchange | Firebase data/rules, marketplace, lending, chat, notifications, ratings, emulator tests |

Every feature still requires peer review, test evidence, designer comparison, and contribution notes for the final report and Q&A.

## Licence

This project is developed for academic evaluation under UTAR course UCCD3223 Mobile Applications Development. All rights are reserved by the group members.
