# Version 0.3 phone trial

Install the v0.3.0-test APK over v0.2 if Android accepts the retained signing identity. The CI signer fingerprint is checked before delivery. Existing saves should remain. This is an Android test app, not an app-store release.

1. Open Discover: ZIP 84414 and real dated listings should appear immediately.
2. Select Walmart and Harrisville, then Clothing. Confirm each card shows the store and observation time. Listed pickup availability is not a shelf count or confirmed markdown.
3. Select Home Depot. Its cards must say WEBSITE DEAL / NOT LOCAL STOCK.
4. Select Target, Lowe's, Tractor Supply or Walgreens: each shows its actual incomplete/failed feed status. An empty feed does not mean a store has no clearance.
5. Change the trial ZIP to 84043: local 84414 offers disappear and the coverage message appears. Restore 84414.
6. Turn off connectivity and check for feed updates: previous results remain, with a clear refresh error and their original dates.
7. In Stores, search any US ZIP and choose closest 3 or 5 per retailer. Save a store, reopen the app and confirm it persists.
8. Sources → Try sample catalog: fictional offers must remain visibly labeled samples.

The button checks published data only. The three-store collector runs separately; no scheduled collection is currently active. Home Depot data is an earlier observed snapshot. Full local clearance, exact inventory counts, and clearance-start dates are not verified.

## Automated verification

`python -m unittest discover -s tests -p test_feed.py` checks normalization, budget, isolation and rejection. `bash scripts/test-domain.sh` checks catalog/store behavior. Android instrumentation validates the bundled feed, wrong ZIP/store/link/time/schema/price handling, and preservation after malformed refreshes. GitHub's emulator test exercises the trial filters, other-ZIP isolation, offline fallback, nationwide map lookup, saved stores, sample separation, rotation and large text.
