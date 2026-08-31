# PropCycle Functional Flow and UX Logic Audit

**Audit date:** 28 August 2026  
**Source of truth:** `Group7-PropCycle.pdf`, `plan.md`, and the current Java/XML app  
**Scope:** Functional logic, navigation, honest states, and data flow. Final visual polish is intentionally later.

## Main journey decisions

1. **Create Listing and Lend Resource remain pages, but not isolated starting journeys.** They are shared create/edit review forms. A user still needs to confirm fields that AI cannot safely decide, such as sale/donation/exchange, price, condition, pickup, public area, maximum borrowing days, and deposit.
2. **The recommended creation journey is photo first.** From Home, Marketplace, or Lending, the user chooses **Use photo and AI**. Camera and Android Photo Picker remain inside the scanner. The AI result then creates an editable form draft and transfers the prepared photo. Nothing is published automatically.
3. **Manual entry remains available.** AI, App Check, the network, or a camera may be unavailable. **Enter manually** opens the same review form without pretending that AI succeeded.
4. **The lending map discovers items, not places.** Search filters real available lending items by title or public area. “Near me” only sorts those items by approximate straight-line distance. Items without a map point remain visible in the list. No Places search, route, travel-time claim, or background tracking is used.
5. **Recycling Centre remains the place-search map.** It searches recycling-centre places by manual area or one-time foreground location and reminds the user to confirm accepted materials.
6. **Recent Activity is truthful and account scoped.** Room/SQLite stores completed scans and app actions locally under the current Firebase UID. It never converts “searched for a centre” into “recycled an item,” and clearing local history does not delete Firebase records.

## All proposal screens and functions

| Screen or function | Intended user job | Functional decision and current logic |
|---|---|---|
| Welcome | Understand the product and continue | One clear path to Login; no false service claims. |
| Login | Enter an existing account | Firebase email/password only. “Username login” is not claimed because no username field or mapping exists. |
| Register | Create an account | Full name, email, and password create Firebase Auth plus the minimal Firestore profile. |
| Home | Start any main journey | Search chooses Marketplace or Lending; Smart Scan opens scanner; creation actions offer AI-first or manual; Recycle opens centre search; Recent opens real local history. |
| Recent Activities | Review account activity | Room-backed, offline-readable, newest first, limited to 100 records, and deep-links to the valid destination. |
| AI Smart Scanner | Take or choose one item photo | CameraX or Photo Picker, consent before transmission, bounded metadata-stripped JPEG, one AI request, setup/offline/error states. |
| AI Result | Review identification and next action | Shows uncertainty and safety guidance. Recycle/List/Lend availability follows the reviewed action policy. Result and temporary image can prefill the next form. |
| Create Listing | Review, create, or edit a marketplace offer | Shared form. All categories plus Sale/Donation/Exchange are reachable. Sale price and exchange terms appear only when needed. Photo remains optional. |
| Marketplace | Discover material listings | Real-time available listings, local search/category filter, all categories reachable, and a visible List an item action. |
| Marketplace Detail | Decide whether to contact or manage | Loads the listing and seller profile name, shows transaction terms and real aggregate rating, opens the public seller profile or participant chat, accepts one editable 1–5-star rating per non-owner reviewer, and exposes edit/withdraw/relist plus a confirmed owner-only final Mark as sold action. Sold leaves browse while existing chats remain. |
| Recycle Center | Find a recycling drop-off point | Google Maps plus Places Text Search, maximum ten results, manual-area fallback, approximate distance, marker/list sync, and external geo handoff. |
| Lend Resource | Review, create, or edit an equipment offer | Shared form with optional photo and rounded approximate point; requires public area, max days, pickup method, and optional deposit. |
| Lending List | Find an available item first | Real item search and category filter. Switching to Map preserves the same query/category. The create action offers AI-first or manual entry. |
| Lending Map | See where matching items are approximately located | Uses only Firestore lending items. “Near me” requests foreground location and sorts matching items; it does not search for places. Switching back to List preserves state. |
| Lending Detail | Review availability and request dates | Owner can edit/withdraw/relist. Non-owner chooses bounded dates, sends a request, and can open lending chat. Ratings shown are real. |
| Notifications | Handle important lifecycle updates | Functions as the in-app lending request centre for approve/reject/cancel/start/return/rate and shows recent sold Marketplace listings only when the member already has that listing's conversation. Individual chat messages remain in Messages; OS push is not claimed. |
| Messages | Find conversations | Real-time participant-only Marketplace and lending threads. Empty threads show no fake message timestamp; real message timestamps use the phone's local timezone. |
| Conversation | Chat with the other participant | Participant-only real-time text, Firestore UTC server timestamps converted once to the phone's local timezone/clock format, validation, loading/offline/error handling, and avatar navigation to the other member's public profile; no unsupported attachment/read-receipt claims. |
| Profile | Inspect an account | The same destination has a private self view and safe public-member view. Email, edit/logout, and device-local activity remain self-only; public view shows display name, member date, Marketplace rating summary, and first available listing. Reward points and achievement badges are intentionally absent. |
| Settings | Manage real app/device behavior | Account opens Profile; location opens Android permission settings; local history can be cleared. Dark theme and phone push stay visibly unavailable instead of using fake switches. |

## Cross-module correctness rules

- AI creates a **draft**, never an automatic public listing.
- Hazardous, unknown, and unsupported scan results cannot open unsafe marketplace/lending actions.
- Marketplace location is not invented; lending uses only a rounded optional point and a required public area label.
- Lending discovery shows available items before location. Location improves sorting only.
- Transaction intent, fulfilment, lending dates, deposits, ratings, ownership, and status are validated again before storage.
- Chat is opened only from a valid available marketplace or lending item and only participants can read it.
- Marketplace `sold` is an owner-only terminal state from `available`; it removes the listing from browse, blocks edit/relist/new chat, preserves existing participant chat, and exposes sold status only to the owner or an existing listing-chat contact.
- Marketplace ratings are 1–5, cannot be self-authored, remain one document per reviewer/seller, and must reference an available listing owned by the rated seller.
- Every authenticated top-level/supporting screen redirects to Login if the session is missing.
- Temporary scan images remain in app-private cache, transfer only to an explicitly selected review form, and are deleted after consumption or abandonment.
- Firebase, AI, Maps, Storage, and App Check failures show setup/retry information and never return invented success data.
- Reward points, eco points, and achievement badges are not part of PropCycle. Activity counts remain factual history counts, not a spendable or ranked score.

## Deliberately separate from this logic pass

- Final colours, spacing, illustrations, motion, and pixel-level mock-up matching.
- Operating-system push notifications until a trusted event sender and FCM scope are approved.
- Payments, routes, travel time, background location, geofencing, marketplace map/location, and multiple images.
- Live Firebase Rules/index deployment, two-account/two-device checks, one protected Gemini request, and real Maps key/device checks. These remain owner-run checks in the setup and Phase 2F release guides.

## Required live checks after local verification

1. Scan with Camera and Photo Picker, then send the same result/photo into both Marketplace and Lending review forms.
2. Confirm a manual form still works when AI setup is unavailable.
3. Publish Sale, Donation, and Exchange listings with two accounts; verify owner-only controls, seller name, avatar-to-profile navigation, rating create/update, and the same aggregate on detail/profile.
4. Search one lending item in List, switch to Map, use approximate location, and confirm the same filter remains.
5. Complete request, chat, approval, active, return, and rating with two accounts/devices.
6. Reopen the app offline and confirm the signed-in account sees its own local recent history only.
7. Deny camera/location permissions and confirm Photo Picker/manual-area fallbacks remain usable.
