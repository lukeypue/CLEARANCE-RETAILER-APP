# Android Foundation Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task in the current session. The user has authorized continuation and routine development choices.

**Goal:** Produce a buildable Android test app for the approved clearance-browsing flows.

**Architecture:** Native Java Android UI over immutable data models and a pure Java query layer. Explicit sample data is bundled offline. Favorites and filters are persisted locally.

**Tech Stack:** JDK 17, Gradle 8.11.1, AGP 8.9.2, Android API 26–35.

**Spec:** `docs/superpowers/specs/2026-09-18-android-foundation-design.md`

## Global Constraints

- Every inventory screen and item detail must clearly say sample/demo.
- No live inventory or complete-store coverage claims.
- Support all six agreed retailers and individual store IDs.
- Hide sold out by default; available/limited before unknown.
- Separate first-spotted date from retailer-confirmed clearance date.
- Money uses integer cents; saved identity is store-specific.
- Do not put credentials or production signing material in source.

### Task 1: Domain behavior and fictional catalog

Files: `app/src/main/java/com/clearance/retailer/model/{Retailer,Store,Deal}.java`, `domain/DealQuery.java`, `data/DemoCatalog.java`, `tests/DomainTests.java`, `scripts/test-domain.sh`.

Interfaces: `DealQuery.apply(List<Deal>, DealQuery.Filter, Set<String>)` returns a new sorted list. `DemoCatalog.stores()` and `DemoCatalog.deals()` return fixture lists. `Deal` exposes integer money, stock, dates, and immutable history.

- [ ] Write executable assertions: a query selecting `walmart-north` never returns `walmart-south`; default excludes sold out; enabling includes sold out last; query plus category plus retailer compose; unknown stock follows known; newest uses known clearance date otherwise firstSeen; saved IDs stay specific; invalid price and history rejected.
- [ ] Run `bash scripts/test-domain.sh`, observing the missing implementation failure.
- [ ] Implement the immutable models, queries, and sample catalog, then rerun the assertions.

```java
DealQuery.Filter filter = new DealQuery.Filter();
filter.storeId = "walmart-north";
List<Deal> result = DealQuery.apply(DemoCatalog.deals(), filter, Set.of());
if (result.stream().anyMatch(d -> !d.storeId.equals("walmart-north"))) {
    throw new AssertionError("Store inventory leaked");
}
```

### Task 2: Native Android browsing and persistence

Files: `app/src/main/AndroidManifest.xml`, `MainActivity.java`, `ui/{Ui,DealDetails}.java`, `data/SavedDeals.java`, `app/src/main/res/values/{strings,styles}.xml`, `app/src/main/res/drawable/ic_launcher.xml`.

Interfaces: Activity consumes `DealQuery`, `DemoCatalog`, and a `SavedDeals` preference store. Detail dialog consumes one `Deal`, its `Store`, and a save callback.

- [ ] Build Discover / Stores / Saved / Sources navigation, composing the domain filters.
- [ ] Add details with original/current prices, meaningful dates, availability, sample history, and save/remove actions.
- [ ] Persist saved IDs and filter state across rotation and process restart; maintain accessible 48dp controls and inset handling.
- [ ] Verify Android compile and lint; document device testing status accurately.

### Task 3: Reproducible build and delivery

Files: `settings.gradle`, `build.gradle`, `app/build.gradle`, `gradle.properties`, `gradlew`, `gradlew.bat`, wrapper files, `.github/workflows/android.yml`, `.gitignore`, `README.md`, `docs/TESTING.md`.

- [ ] Configure the exact compatible Android toolchain and official Gradle wrapper with checksum validation.
- [ ] Make CI run `bash scripts/test-domain.sh` and `./gradlew lintDebug assembleDebug --no-daemon`; upload `app/build/outputs/apk/debug/app-debug.apk`.
- [ ] Commit/push the verified foundation to its feature branch and create a draft PR, leaving the empty baseline main reviewable.
- [ ] Inspect workflow jobs and report the actual build result; provide an APK if one was successfully built and made accessible, otherwise identify the specific build/access blocker.
