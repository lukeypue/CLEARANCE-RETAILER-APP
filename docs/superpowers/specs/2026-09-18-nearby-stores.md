# ZIP store discovery and inventory access

The user authorized continuing toward a fully functional, live inventory app, with ZIP search, the closest 3–5 stores per retailer and more categories. This version delivers the independently achievable location and navigation features. Full inventory remains a blocked requirement, not a completed feature.

## Behavior

- Open in real-store mode. Enter a five-digit US ZIP, then show up to 3 or 5 nearest listed stores **per retailer** within 50 miles, with an explicit 100-mile option. Sort each chain by straight-line distance from the ZIP center. Fewer results mean fewer mapped stores in the radius; do not invent locations or imply exhaustive coverage.
- Geocode with Zippopotam.us and fetch mapped shops from OpenStreetMap Overpass. Validate retailer identities, exclude closed stores and in-store pharmacies, deduplicate nodes/buildings, and reject incomplete/error responses. Cache successful results for 24 hours by ZIP and radius, cap cache size and requests, and retain clearly dated cache when offline. No automatic polling.
- Add a searchable category chooser covering 36 departments. Search/category selections prepare searches on the retailer's own website. Website prices are not imported or represented as local inventory; the shopper must choose the store on that website.
- Offer addresses, map directions, store-specific saved locations, and links to official retailer sites. Never load arbitrary website URLs from OSM tags.
- Keep the original fictional catalog in an explicit optional sample mode. Saved samples remain separate from real saved stores.
- Every real store's inventory status is unavailable until an authorized feed is connected. Do not label unavailable stock as sold out, zero products, or checked live.
- Show sources, privacy, timestamps, OSM attribution and coverage limits. ZIP goes to Zippopotam.us; its approximate coordinates go to Overpass. No device location permission or analytics.

## Architecture

Pure Java models and nearest-store/classification logic; Android JSON parser; HTTPS transport with bounded responses/timeouts; app-lifetime repository with one background worker, main-thread observer updates and a persistent bounded cache. Activity recreation observes the same operation; switching screens cannot start duplicate requests. Favorites use independent local storage. Existing sample browsing remains isolated.

## Verified access constraints (2026-09-18)

- Zippopotam.us returned a valid ZIP centroid. Overpass returned actual mapped stores for a test ZIP. OSM coverage is community maintained and may be incomplete.
- Walmart Marketplace Inventory API is seller inventory, not an all-physical-store clearance feed: https://developer.walmart.com/us-marketplace/docs/inventory-api-overview
- Public Walmart/Home Depot pages requested browser verification, Lowe's returned access denied, Target/Walgreens returned store pages but no verified complete store inventory feed. No challenge is bypassed and no internal credentials are reused.
- Six retailer inventory connections therefore remain unconnected; an approved provider/feed with physical store identifiers, item/variant identifiers, price, stock, timestamps and redistribution rights is needed. Never put provider secrets in the APK.
- Public Overpass is suitable only for this small test. Before public distribution, provision shared caching and an appropriately hosted service; current limits count all app users together: https://wiki.openstreetmap.org/wiki/Overpass_API#Public_Overpass_API_instances

## Verification

Executable domain tests cover ZIP validation, nearest-per-chain selection, radius limits, distances, stable ties, duplicate shops, false brand matches and the expanded categories. Android instrumentation covers real payload parsing, incomplete replies, cache identity, stale/error behavior and favorites. GitHub builds/lints and runs emulator flows including ZIP input, cached real stores, inventory-unavailable labels, category search, saving, restoration and original sample isolation. A separate single live query validates production endpoints; fixtures never ship as real results.
