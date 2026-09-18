# Test version 0.2.0 on Android

1. Download and open `Clearance-Retailer-v0.2.0-test.apk`. Allow installation from your browser/file app if Android asks. **Uninstall v0.1 first**: its CI signing key was not retained, so Android cannot update it in place. Uninstalling resets saved sample items. The v0.2 workflow fixes the signing-key path for future updates.
2. Open **Clearance Test**, enter a five-digit US ZIP, select 3 or 5 stores per retailer and a 50- or 100-mile radius, then tap **Find stores**. The first lookup needs internet and can take up to about a minute. US ZIPs only; some special-purpose ZIPs may be absent from the ZIP provider.
3. Check the result's city/ZIP and radius. Each retailer has its own nearest locations. Distances are straight-line estimates from the ZIP center. Fewer than 3/5 results means fewer mapped stores were found within the radius. Use the retailer locator if coverage is sparse.
4. Tap **Save store**, then **Saved**. Close/reopen the app and confirm it remains. Remove the final saved store and check that the empty state appears.
5. In **Discover**, open **All 36 categories** and type a department such as Baby, Automotive, Farm, Grocery or Seasonal. Set an optional product search. **Search retailer website** explains which store to select on that website before opening it. Online prices can differ from store prices.
6. Load a ZIP, disconnect internet, and search the same ZIP/radius. Cached results should remain visible with their retrieval timestamp. Older saved data is labeled; a failed new ZIP lookup must not relabel old results as that ZIP.
7. Check **Sources**. All six inventory feeds must say **not connected**. This app cannot yet show real stock, clearance prices, a complete product list or clearance dates for those locations.
8. Optionally select **Sources → Try sample catalog** to use the original fictional finds. Save a sample item, inspect its price history, and switch back with **Return to real stores**. Fictional offers must never appear at real stores. The two sample Walmart stores intentionally have different prices for the same air fryer.

## Network behavior

Successful store lookups are cached for 24 hours. Switching the 3/5 count or retailer does not issue a network request. A new ZIP/radius is a new lookup; uncached requests are limited to one per 30 seconds and 20 per day in this small test. There is no polling or background location access.

The app contains no credentials or paid service enrollment. Complete live inventory remains blocked on an authorized data feed and backend integration. Never use this version's absence of product data as evidence that a physical store has sold out.

## Automated evidence

`scripts/test-domain.sh` runs the production query and store-selection assertions. GitHub builds and lints the Android code, runs six instrumentation tests including one real network lookup near test ZIP 84043, then performs `scripts/android-smoke.py` UI flows. That ZIP is a test fixture location, not the user's location. Test data and caches are created only on the CI emulator and are not packaged in the APK. Screenshots, the instrumentation report and failure diagnostics are retained in the `android-phone-smoke` artifact.
