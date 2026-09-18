# First phone test

This is an offline demo, version 0.1.0. All products, prices, stores, dates, and stock are fictional sample data fixed at September 18, 2026. This is not a live bargain finder yet.

1. Open **Clearance Test**. Check that the sample-inventory banner is visible.
2. Tap **Stores**, then browse **Walmart · North**. All results should belong to the North sample store.
3. Open the compact air fryer. Its sample price is **$24**. Save it.
4. Browse **Walmart · South**. The same sample product is **$32** at this different location. Saving one must not save the other.
5. Tap **Saved**. Close the app and reopen it. Your saved item should remain.
6. Try product search, category filters, and all three sorting choices.
7. On Walmart North, enable **Include sold-out items** to reveal the wooden building set. Unknown stock remains visible and clearly labeled.
8. Open a detail page. Check that **First spotted** and **Clearance started** are separate, with **Not provided** when the latter is unknown.
9. Try portrait and landscape orientation and larger text. Controls should remain usable with scrolling.
10. Visit **Sources**. All six retailers must say live data is not connected.

## Automated checks

```sh
bash scripts/test-domain.sh
./gradlew lintDebug assembleDebug --no-daemon
```

The first command runs production domain code on Java 17, checking store isolation, combined filters, stock ordering, saved identity, dates, money, and fixture provenance. The second produces the Android test APK and lint report.

Android build and device-test results are reported in the GitHub Actions run and PR. A passing build does not by itself mean a phone/emulator test has passed.

## Test build signing

The debug package is `com.clearance.retailer.test`. CI caches a debug-only signing key for repeat installations. Cache eviction can change the debug certificate and require uninstall/reinstall (which removes local saved items). Production signing is not configured. No production keystore or API keys are checked in.
