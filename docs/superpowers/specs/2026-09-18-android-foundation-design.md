# Clearance Retailer App: first Android test version

## Approved product goal

An Android app to browse clearance at a particular physical store or across Walmart, Target, Home Depot, Lowe's, Tractor Supply, and Walgreens. Categories and newest-clearance ordering help people discover items. In-stock and limited-stock items come first; sold-out items are retained but hidden by default. The user approved the product scope and asked to continue implementation without repeated routine approvals.

## This build

Version 0.1.0 is an offline interaction test, with unmistakably labeled fictional sample inventory. It delivers store-level browsing, text search, retailer/category filters, newest/discount/price sorting, a sold-out switch, item details, sample price history, and persistent saved items. Two distinct Walmart sample stores demonstrate that store inventory and prices never bleed together.

All dates are labeled by meaning. `firstSeen` means when our catalog first observed the offer. `clearanceSince` is optional and means a supplied retailer-confirmed start date. Unknown dates and quantities stay unknown. A repeated price observation never changes the first-seen date. Sample data is fixed as of 2026-09-18 and is never represented as a fresh live check.

Live inventory, actual nearby-store discovery, ZIP/radius lookup, background price tracking, alerts, and complete-store coverage are future stages requiring a verified source and separate integration tests. The app must not display fabricated distances, report all inventory as covered, or present sample stock as live. The Sources screen explains the current status for each of the six retailers.

## Architecture and alternatives

Use a small native Android Java app with no runtime third-party dependencies and a pure Java domain layer. This is easy to build in GitHub Actions and works offline on Android 8+. A WebView app would reduce UI code but add another runtime boundary; a hosted web app would not satisfy the agreed Android installation flow. The native approach is selected for this milestone.

`model` owns immutable offer/store data and validation. `data/DemoCatalog` owns explicitly fictional fixtures. `domain/DealQuery` owns filtering and deterministic sorting. `data/SavedDeals` owns local preference persistence. `ui` owns reusable view construction and item details, while `MainActivity` owns navigation and filter state. No keys, accounts, location permissions, analytics, or internet access are needed for this test build.

Queries are scoped by stable retailer and store IDs, never just product title. Money uses integer cents. In-stock and limited-stock form the first availability tier; unknown follows; sold-out is last when explicitly included. Sorting operates within these tiers. All filters compose, and saved identity includes the specific store offer.

## Build and verification

JDK 17; Gradle 8.11.1; Android Gradle Plugin 8.9.2; compile/target SDK 35; min SDK 26. GitHub Actions runs the real domain assertions, Android lint, and debug APK build and uploads an APK artifact. The app is a debug test build, not a Play Store release. Test signing must be kept stable between CI builds to allow upgrades; no production signing key belongs in this repository.

Verify specific-store isolation, combined filters, sold-out default and opt-in, known stock ordering, meaningful date sorting, discounts, saved-offer isolation, invalid money/history, and demo provenance. A device/emulator smoke test is required before claiming the UI has been tested on Android. Build success alone does not establish device testing.

## Source evidence

- Android tool compatibility: https://developer.android.com/build/releases/agp-8-9-0-release-notes
- Walmart's Marketplace inventory API manages a seller's SKUs and is not evidence of access to a physical store's entire clearance inventory: https://developer.walmart.com/us-marketplace/docs/inventory-api-overview
