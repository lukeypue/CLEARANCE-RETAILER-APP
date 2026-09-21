# Clearance Retailer App

Android test app for Walmart, Target, Home Depot, Lowe's, Tractor Supply and Walgreens.

## Version 0.3.0

**Phone trial with real dated retailer listings. This is not complete live clearance inventory or an app-store release.**

- Discover opens on ZIP **84414** with a browsable list, no product search required.
- Store pickup candidates from the three nearest mapped Walmarts: Harrisville 2921, Ogden 3789, Riverdale 1708. These appeared in a clearance search and offered pickup when checked; their shelf prices, clearance markdowns and quantities remain unverified.
- Home Depot website deals appear separately as online listings, without local stock claims.
- Target, Lowe's, Tractor Supply and Walgreens show their actual incomplete/failed trial statuses.
- Filter by retailer, trial store or title-derived category; sort by listed price or most recently checked. Original clearance dates are not available.
- A dated bundled feed works offline. **Check for feed updates** downloads shared JSON from GitHub; it does not initiate retailer scans or spend ScrapingBee credits. Older observations remain clearly labeled.
- The Stores tab still searches nationwide ZIPs for the closest 3 or 5 mapped locations per chain, offers 36 retailer website search categories, directions and saved stores.
- Fictional sample offers remain opt-in under Sources and separate from the trial listings.

## Refreshing the trial

The feed is collected outside Android. Set the **SCRAPINGBEE_API_KEY** repository secret for the prepared **Refresh trial feed** workflow; it is manually dispatched on `codex/android-foundation` once available in GitHub Actions. The workflow must be registered on the repository's default branch before dispatch is available. No collection schedule is enabled. The script can also run on a server with that environment variable:

```sh
python scripts/collect-feed.py
```

A run makes three 10-credit Walmart requests, keeps a 100-credit reserve, has no automatic retry loop, and replaces the feed only after all three responses pass validation. Existing Home Depot observations keep their earlier timestamps; this collector currently refreshes Walmart only. No key is included in the APK, feed, repository, or workflow inputs. GitHub secret configuration and automated refresh have not been completed for this trial build.

## Data and limits

ZIP centers come from [Zippopotam.us](https://www.zippopotam.us/). Store map records are © [OpenStreetMap contributors](https://www.openstreetmap.org/copyright), licensed under ODbL, retrieved through Overpass. Distances are from the ZIP center, not the phone or a driving route. These are the nearest **mapped** locations in the selected radius, not a guarantee of complete retailer coverage. Duplicated map points within about 190 meters for the same retailer are treated as a single store. Missing addresses remain unknown.

The app sends the requested ZIP to Zippopotam.us and approximate ZIP coordinates to Overpass. No location permission, analytics, accounts or production credentials are used in the APK. Checking the shared feed contacts GitHub, which receives normal request metadata such as IP address. External map and retailer links have their own privacy policies. Requests use HTTPS, run off the UI thread and have timeouts/size limits. Store searches have a 30-second cooldown and a test limit of 20 uncached searches per day per installation. Public Overpass limits apply across all app users: **shared caching and an appropriately hosted data service are required before public distribution**. See [Overpass usage policy](https://wiki.openstreetmap.org/wiki/Overpass_API#Public_Overpass_API_instances).

No verified complete physical-store clearance feed is available to this project for any of the six retailers. A location list, an online product listing, and a seller's own inventory are different datasets. For example, [Walmart's Marketplace Inventory API](https://developer.walmart.com/us-marketplace/docs/inventory-api-overview) manages seller SKUs; it does not establish access to all local-store clearance stock. A future authorized feed must supply store/item/variant identifiers, local price and currency, stock semantics, observation time, coverage and permission to redistribute. Secrets must remain on a backend, not in the APK. The UI reports unavailable inventory explicitly.

## Install and test

In **Actions**, select a successful **Android test build** for `codex/android-foundation` and download **Clearance-Retailer-v0.3.0-test**. Unzip and install the APK on Android 8 or newer. Version 0.1's signing key was not retained by its CI job. **Uninstall v0.1 before installing v0.3; saved sample items will reset.** Version 0.2 explicitly generates and caches its test key at the same path used by Gradle so later test updates can preserve data while that cache survives. Version 0.3 has the same verified signing certificate as the delivered v0.2 APK. See [phone test steps](docs/TESTING.md).

## Build and verification

JDK 17, Gradle 8.11.1, AGP 8.9.2, Android SDK 35/Build Tools 35.0.0; min API 26. No third-party runtime libraries in the installation app. AndroidX/JUnit are test-only dependencies.

```sh
bash scripts/test-domain.sh
./gradlew lintDebug assembleDebug assembleDebugAndroidTest
```

GitHub runs collector tests, domain assertions, lint, APK/test-APK builds and an API 35 emulator. Instrumentation covers parsing, cache identity and failure handling, favorites and one actual Android HTTPS ZIP/store query. UI automation checks ZIP validation, 3/5 selection, categories, saved locations, offline cache, sample isolation, offer saving, rotation and large text. A build is only phone-tested when that workflow's **phone-smoke** job passes.

The Android APK is for testing. Production inventory, scalable data access, release signing, store listings/privacy disclosures and platform-specific release validation remain open. It is not an iPhone app or approved for any app store.
