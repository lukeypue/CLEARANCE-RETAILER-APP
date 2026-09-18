# Clearance Retailer App

Android test app for Walmart, Target, Home Depot, Lowe's, Tractor Supply and Walgreens.

## Version 0.2.0

**Real nearby-store discovery works independently of inventory. Live in-store clearance prices and stock are not connected. This is not a full-inventory or store-release build.**

- Search a five-digit US ZIP, including ZIPs with leading zeros.
- Show up to the closest **3 or 5 locations per retailer** within 50 or 100 miles of the ZIP center.
- Filter by retailer, view mapped addresses and straight-line distances, get directions and save stores locally.
- Choose from **36 searchable categories** and prepare product searches on the retailer's website. Select the physical store again on that website; a search link does not verify its stock.
- Restore cached location results offline, with their retrieval time and a warning for older records. Successful ZIP/radius searches are cached for 24 hours, with up to eight searches retained.
- Keep the original fictional catalog and saved sample offers under **Sources → Try sample catalog**. Samples never appear as products at real locations.

## Data and limits

ZIP centers come from [Zippopotam.us](https://www.zippopotam.us/). Store map records are © [OpenStreetMap contributors](https://www.openstreetmap.org/copyright), licensed under ODbL, retrieved through Overpass. Distances are from the ZIP center, not the phone or a driving route. These are the nearest **mapped** locations in the selected radius, not a guarantee of complete retailer coverage. Duplicated map points within about 190 meters for the same retailer are treated as a single store. Missing addresses remain unknown.

The app sends the requested ZIP to Zippopotam.us and approximate ZIP coordinates to Overpass. No location permission, analytics, accounts or production credentials are used. External map and retailer links have their own privacy policies. Requests use HTTPS, run off the UI thread and have timeouts/size limits. Store searches have a 30-second cooldown and a test limit of 20 uncached searches per day per installation. Public Overpass limits apply across all app users: **shared caching and an appropriately hosted data service are required before public distribution**. See [Overpass usage policy](https://wiki.openstreetmap.org/wiki/Overpass_API#Public_Overpass_API_instances).

No verified complete physical-store clearance feed is available to this project for any of the six retailers. A location list, an online product listing, and a seller's own inventory are different datasets. For example, [Walmart's Marketplace Inventory API](https://developer.walmart.com/us-marketplace/docs/inventory-api-overview) manages seller SKUs; it does not establish access to all local-store clearance stock. A future authorized feed must supply store/item/variant identifiers, local price and currency, stock semantics, observation time, coverage and permission to redistribute. Secrets must remain on a backend, not in the APK. The UI reports unavailable inventory explicitly.

## Install and test

In **Actions**, select a successful **Android test build** for `codex/android-foundation` and download **Clearance-Retailer-v0.2.0-test**. Unzip and install the APK on Android 8 or newer. Version 0.1's signing key was not retained by its CI job. **Uninstall v0.1 before installing v0.2; saved sample items will reset.** Version 0.2 explicitly generates and caches its test key at the same path used by Gradle so later test updates can preserve data while that cache survives. See [phone test steps](docs/TESTING.md).

## Build and verification

JDK 17, Gradle 8.11.1, AGP 8.9.2, Android SDK 35/Build Tools 35.0.0; min API 26. No third-party runtime libraries in the installation app. AndroidX/JUnit are test-only dependencies.

```sh
bash scripts/test-domain.sh
./gradlew lintDebug assembleDebug assembleDebugAndroidTest
```

GitHub runs domain assertions, lint, APK/test-APK builds and an API 35 emulator. Instrumentation covers parsing, cache identity and failure handling, favorites and one actual Android HTTPS ZIP/store query. UI automation checks ZIP validation, 3/5 selection, categories, saved locations, offline cache, sample isolation, offer saving, rotation and large text. A build is only phone-tested when that workflow's **phone-smoke** job passes.

The Android APK is for testing. Production inventory, scalable data access, release signing, store listings/privacy disclosures and platform-specific release validation remain open. It is not an iPhone app or approved for any app store.
