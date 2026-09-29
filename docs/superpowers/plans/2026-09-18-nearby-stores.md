# Nearby stores implementation

Use superpowers:executing-plans. User has authorized routine development and continued work.

1. Write failing pure Java assertions for ZIPs, retailer matching, nearest 3/5 per chain, radius, deduplication and category coverage; implement validated models and queries.
2. Add Android payload parsing, HTTPS transport, observable repository with bounded persistent caching, error handling, and saved stores. Add instrumentation assertions over real-shaped payloads and controlled transport failures.
3. Integrate real store mode into the existing UI: ZIP/radius/count, retailer filtering, searchable categories, store cards/directions/official-site search, favorites, sources and opt-in sample mode. Preserve original saved deals.
4. Bump version, update documentation and GitHub build, add emulator flows for the new behavior, then run domain tests, lint/build, parser/repository instrumentation and UI smoke tests. Review the implementation and fix concrete issues.
5. Download and verify the GitHub APK, save the deliverable and report implemented behavior plus the blocked inventory-feed requirement. Do not call this full-live or app-store ready.
