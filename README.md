# PropCycle - Circular Economy for Event and Creative Materials

PropCycle is a native Android application designed to help campus event organisers, creative makers, cosplayers, toy miniaturists, and DIY communities keep useful materials and equipment in circulation. 

Aligned with **UN SDG 12 (Responsible Consumption and Production)**, the app connects scanning, sharing, borrowing, and recycling into one cohesive mobile journey, encouraging users to reuse before they dispose.

## Core Features

1. **AI Smart Scanner:** Utilizes Google Gemini AI to analyze photos of materials, providing identification, safety notes, and creative upcycling/reuse suggestions.
2. **Material Marketplace:** A peer-to-peer platform to donate, sell, or exchange leftover materials. Features include filtering, in-app messaging, and seller ratings.
3. **Equipment Lending:** Discover and borrow tools and equipment. Includes a robust date-booking system, owner approval workflow, and return confirmation.
4. **Recycling Maps:** Integrated with Google Maps and Places API to help users discover nearby recycling centres based on their current location.

## Technology Stack

* **Platform:** Native Android (Java 17)
* **UI:** Android Views with XML and Material Design Components
* **Local Storage:** Room Database (SQLite) for offline activity history
* **Cloud & Backend:** Firebase Authentication, Cloud Firestore, Firebase Storage
* **AI & Location:** Firebase AI Logic (Gemini), Google Maps SDK, Google Places API

## Building and Running

1. Clone the repository and open the root folder in Android Studio.
2. Add your Firebase `google-services.json` file to the `app/` directory.
3. Add your Google Maps API key to the `secrets.properties` file.
4. Build and run on an Android emulator or physical device (API 24+).

## Licence

This project was developed for academic evaluation under UTAR course UCCD3223 Mobile Applications Development. All rights are reserved by the group members.
