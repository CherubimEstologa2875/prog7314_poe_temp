# Runway — Final POE Release Notes

**Release:** 1.0.0 Final POE  
**Date:** 5 November 2026  
**Project:** PROG7314 Group 11

## Release summary

This release moves Runway from the Part 1 interaction prototype and Part 2 working foundation to the final POE submission. The final product combines the wardrobe, outfit and planning workflows with weather-aware recommendations, localisation, biometric access, settings, push notifications and supporting documentation.

## Evolution from prototype to final product

### Part 1 — interaction prototype

- Defined the Runway visual identity, layout system and reusable components.
- Established the core journeys: authentication, adding an item, wardrobe browsing, outfit creation, planning and profile/settings.
- Validated the intended user experience with a clickable prototype.

### Part 2 — working foundation

- Delivered an Android client using Kotlin, XML layouts, Material 3 and MVVM.
- Added Google/Firebase authentication, a Ktor REST API and Firestore persistence.
- Implemented wardrobe capture, on-device subject cut-out, tagging and wear tracking.
- Added colour matching, outfit canvas editing, outfit persistence, monthly planning and confidence ratings.
- Added Room caching and queued wardrobe synchronisation for offline use.
- Added repository, domain and ViewModel test coverage together with API tests and OpenAPI documentation.

### Final POE — completed release

- Added Open-Meteo weather retrieval and wardrobe-based weather outfit suggestions.
- Added English, Afrikaans and isiZulu language selection with persisted settings.
- Added biometric capability detection and optional biometric unlock after Google authentication.
- Added Firebase Cloud Messaging token registration, notification channels, inbox handling, permission states, test notifications and invalid-token cleanup.
- Added profile/settings expansion, language selection, item editing and final UI evidence.
- Improved validation, safe error states, API documentation and developer setup guidance.

## Technical changes

- Android minimum SDK remains 26; compile SDK is 37.
- API remains a separate Ktor/Netty Gradle build using Firebase Admin and Firestore.
- Protected API routes verify Firebase ID tokens and enforce per-user ownership.
- The app continues to use a single-activity navigation graph and manual dependency injection.
- The API host can be switched between the hosted Render deployment and the local emulator address from `app/build.gradle.kts`.

## Verification

- Android JVM tests cover domain models, authentication, repositories, notification rules, mappers and ViewModels.
- API tests cover routing, validation, authentication requirements and notification token behavior.
- Manual push-notification scenarios are documented in `docs/notifications-testing.md`.
- UI evidence includes Google sign-in, language selection, home, wardrobe, cut-out, outfit builder, planner and profile screens.

## Known operational requirements

- The included Firebase configuration and shared debug keystore are used by the supplied project builds.
- The API requires a local Firebase Admin service-account key when run outside the hosted environment.
- Push notifications require a Google Play-enabled device/emulator and Android notification permission where applicable.
- The first request to the free Render deployment may be delayed while the service wakes.

## Documentation

- [`README.md`](README.md) — final technical report and setup guide
- [`AI-REPORT.md`](AI-REPORT.md) — AI-use declaration
- [`docs/notifications-testing.md`](docs/notifications-testing.md) — notification manual test plan
