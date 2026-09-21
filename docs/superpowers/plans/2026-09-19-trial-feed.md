# Trial Feed Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Deliver a phone-test APK with real dated listings for 84414 and truthful six-retailer coverage.
**Architecture:** Python collector → shared JSON → native Android cached feed. No provider credentials in the phone.
**Tech Stack:** Python standard library, Java 17, Android API 26–35, existing Gradle and GitHub Actions.
**Spec:** docs/superpowers/specs/2026-09-19-trial-feed.md

## Global Constraints
- Version 0.3.0-test; retain existing debug signing cache.
- Collection at most 30 credits/run, reserve 100, manual dispatch only.
- No fabricated stock, markdowns, original prices, or clearance dates.
- ZIP 84414 trial only; nationwide map search remains available.

## Review Focus
- Wrong-store provider response: reject without replacing valid feed (task 1).
- Duplicate items/third-party shipping: exclude and deduplicate (task 1).
- Bad response after valid cache: preserve previous dated feed (task 2).
- Other ZIP or store: never show 84414 listings as local elsewhere (task 2).
- APK credential leakage and sample/live mixing: scan artifacts and exercise UI (task 3).

### Task 1: Collector and shared data
Files: scripts/collect-feed.py, tests/test_feed.py, feeds/trial.json, .github/workflows/feed.yml.
Interface: normalize_walmart(payload, store, observed_at) returns offer dictionaries matching schema 1; collector updates all stores atomically only if every response is usable.
- [x] Write unittest fixtures for wrong store, seller/stock exclusion, deduplication, invalid price and insufficient credits. Run `python -m unittest discover -s tests -p 'test_feed.py'`; expect failure before implementation.
- [x] Implement normalization and a quota-checked three-request collector, secret environment only, no retries or schedule. Validate prices with Decimal and requested store identity with exact string comparison.
- [x] Generate seed feed from already-observed responses with original timestamps; label the collection source/status precisely.
- [x] Run the Python suite; expect all tests passing. Commit collector, tests and data.

### Task 2: Android browsing
Files: data/TrialFeed.java, data/TrialRepository.java, ui/TrialScreen.java, MainActivity.java, bundled trial.json, androidTest/TrialFeedTests.java.
Interface: TrialFeed.parse(String) returns immutable source/store/offer lists; TrialRepository preserves previous valid feed on network failure; TrialScreen renders Discover and source status.
- [x] Write parser instrumentation for wrong schema, unsafe URL, bad timestamp, unknown store, other ZIP coverage, bundled feed validation. All five parser/cache tests passed on Android CI. Local Gradle compilation was unavailable; no local Android red-build claim.
- [x] Implement parse/validation, assets + preferences cache and background refresh; preserve observation dates and offline status.
- [x] Implement immediate listings with retailer/store/category selectors, listed price, observation dates and safe retailer links; source statuses for all six; preserve Stores/Saved/sample flows.
- [x] Add repository injected-transport failure test and emulator browse/offline assertions. Run full suites.

### Task 3: Build and deliver
Files: app/build.gradle, .github/workflows/android.yml, scripts/android-smoke.py, README.md, docs/TESTING.md.
- [x] Bump version and artifact names to 0.3.0; include Python tests in CI; test the new flow and existing map/sample paths.
- [x] Request independent review with this spec and plan, fix material findings with regression tests.
- [x] Push verified changes to the existing task branch. Await successful Android lint/build and emulator jobs.
- [x] Download APK, verify signing/hash and absence of the actual API key, save deliverable and report tested capabilities and refresh setup limitations.

## Delivery evidence — 2026-09-21

- Implementation commit: `7a923d77b964d211d8158cce1b84210dd9776169`.
- GitHub run [35559245540](https://github.com/lukeypue/CLEARANCE-RETAILER-APP/actions/runs/35559245540): build/lint succeeded; phone-smoke succeeded on attempt 2. Attempt 1 received an HTTP 504 from the public map service; that external availability risk remains.
- 12 Python collector tests, 23 Java domain assertions, 11 Android instrumentation tests, and the full emulator UI walkthrough passed.
- Independent review found one collector issue: a final quota lookup could fail after a successful feed write. Fixed by making quota reporting nonfatal, with regression coverage for that failure and partial collection preservation.
- APK SHA-256: `ffed0ffc9894e5ed5e87ddb6d5b78331c89f2322c17290028b7530c9016b8d5f`.
- APK signer SHA-256: `c2c782c277a55c48537514a9cd93c7fc3d4d3eedb801a92734629394f3f2ab53`, matching delivered v0.2.
- Actual provider key absent from APK archive, decompressed entries, and source files. Bundled 42-offer feed matches the publicly published HTTPS feed.
- Delivered APK saved separately. Provider usage endpoint confirms 777 credits remain.
- Refresh setup remains incomplete: GitHub connection supports code/build operations but not Secrets; browser sign-in does not offer this account's Google method. No repository secret was saved and no collection schedule was enabled. Manual workflow also requires default-branch registration. This does not block the bundled phone trial.
