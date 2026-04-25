# Changelog

## 1.0.3 - 2026-04-25

REI-first recipe and pricing reliability hotfix.

- removed the bundled recipe snapshot from the mod jar
- made REI the primary recipe source again
- kept the local SkyBlock repo cache only as a late fallback if REI exposes no recipes
- switched normal price lookups to Coflnet's compact current-price endpoint
- fall back from current price to Bazaar snapshot, then to active BIN when needed
- URL-encode item tags and send a clear user agent on API calls
- stop logging expected Bazaar/AH misses as scary warnings

## 1.0.2 - 2026-04-25

Recipe-source reliability release.

- bundled a normalized fallback recipe snapshot inside the mod jar
- local SkyBlock repo cache now augments the bundled snapshot instead of being required
- REI is now only a last-resort fallback for missing recipe data

## 1.0.1 - 2026-04-25

Hotfix for tooltip resolution and stuck loading behavior.

- treat an item as ready when any complete direct recipe is fully priced
- re-request direct ingredient prices if recipe data appears after the first hover request

## 1.0.0 - 2026-04-25

First release intended for GitHub and Modrinth publishing.

- switched craft-cost math to direct one-layer ingredient pricing
- uses market prices for crafted ingredients inside larger recipes
- waits for a 10 second hover before starting any price work
- queues and deduplicates price requests to stay lightweight
- loads recipes primarily from the local SkyBlock repo cache
- keeps REI as a soft fallback instead of a hard dependency
- avoids repeated tooltip-frame requests for the same hovered item
- only invalidates cached craft results when price data actually changes
- stops the background price fetcher cleanly on client shutdown
- improved release metadata and documentation
