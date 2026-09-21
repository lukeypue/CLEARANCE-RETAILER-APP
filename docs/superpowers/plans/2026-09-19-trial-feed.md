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
- [ ] Write unittest fixtures for wrong store, seller/stock exclusion, deduplication, invalid price and insufficient credits. Run `python -m unittest discover -s tests -p 'test_feed.py'`; expect failure before implementation.
- [ ] Implement normalization and a quota-checked three-request collector, secret environment only, no retries or schedule. Validate prices with Decimal and requested store identity with exact string comparison.
- [ ] Generate seed feed from already-observed responses with original timestamps; label the collection source/status precisely.
- [ ] Run the Python suite; expect all tests passing. Commit collector, tests and data.

### Task 2: Android browsing
Files: data/TrialFeed.java, data/TrialRepository.java, ui/TrialScreen.java, MainActivity.java, bundled trial.json, androidTest/TrialFeedTests.java.
Interface: TrialFeed.parse(String) returns immutable source/store/offer lists; TrialRepository preserves previous valid feed on network failure; TrialScreen renders Discover and source status.
- [ ] Write parser instrumentation for wrong schema, unsafe URL, bad timestamp, unknown store, other ZIP coverage, bundled feed validation. Run through Gradle/CI; initial missing production classes establish red compile.
- [ ] Implement parse/validation, assets + preferences cache and background refresh; preserve observation dates and offline status.
- [ ] Implement immediate listings with retailer/store/category selectors, listed price, observation dates and safe retailer links; source statuses for all six; preserve Stores/Saved/sample flows.
- [ ] Add repository injected-transport failure test and emulator browse/offline assertions. Run full suites.

### Task 3: Build and deliver
Files: app/build.gradle, .github/workflows/android.yml, scripts/android-smoke.py, README.md, docs/TESTING.md.
- [ ] Bump version and artifact names to 0.3.0; include Python tests in CI; test the new flow and existing map/sample paths.
- [ ] Request independent review with this spec and plan, fix material findings with regression tests.
- [ ] Push verified changes to the existing task branch. Await successful Android lint/build and emulator jobs.
- [ ] Download APK, verify signing/hash and absence of the actual API key, save deliverable and report tested capabilities and refresh setup limitations.
