# Clearance Retailer App

An Android app for browsing clearance across **Walmart, Target, Home Depot, Lowe's, Tractor Supply, and Walgreens**, or inside one selected physical store.

## Version 0.1.0: first interaction test

**This build contains fictional sample inventory. It does not retrieve live store stock or prices.**

Working flows:

- Browse 23 sample offers at seven fictional stores across six retailers.
- Filter by store, retailer, category, or product search.
- Sort newest finds, biggest percentage discounts, or lowest prices.
- Show in-stock and limited-stock first; hide sold-out by default.
- View separate first-spotted and known clearance-start dates, plus sample price history.
- Save store-specific offers on the phone and reopen them later.
- Check the honest connection status of every retailer.

Real nearby stores, ZIP/radius search, live inventory, verified full-store coverage, and automatic refresh are not implemented in this milestone. The next phase is to validate a data source for one retailer before expanding independent retailer connectors. Walmart's seller Marketplace inventory API alone does not provide evidence of access to full physical-store clearance inventory ([official API documentation](https://developer.walmart.com/us-marketplace/docs/inventory-api-overview)).

## Get the test app

Open this repository's **Actions** tab, select a successful **Android test build** run on `codex/android-foundation`, and download the **Clearance-Retailer-v0.1.0-test** artifact. Unzip it and install the APK on Android 8 or newer. A GitHub sign-in may be required to download an Actions artifact.

See [phone test steps](docs/TESTING.md). No account, API keys, location permission, internet connection, or paid services are needed to try this demo.

## Build

JDK 17, Android SDK 35 and Build Tools 35.0.0 are required. The Gradle 8.11.1 wrapper is included and checksum-pinned. Android Gradle Plugin is pinned to 8.9.2; compatibility is documented by [Android Developers](https://developer.android.com/build/releases/agp-8-9-0-release-notes).

```sh
bash scripts/test-domain.sh
./gradlew lintDebug assembleDebug
```

On Windows, open the project in Android Studio or use `gradlew.bat lintDebug assembleDebug`. Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Structure

- `app/src/main/java/com/clearance/retailer/model`: validated offers, stores, and retailer identities.
- `domain`: deterministic filtering and sorting without Android dependencies.
- `data`: fictional fixtures and local saved items.
- `ui` and `MainActivity`: native screens and navigation.
- `tests`: executable domain assertions.
- `.github/workflows/android.yml`: lint, tests, and APK build.

There are no retailer credentials, scraping implementations, network permissions, or production signing keys in this project. Retailer names identify planned coverage; this independent app is not affiliated with those companies.
