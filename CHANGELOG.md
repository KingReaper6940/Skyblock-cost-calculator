# Changelog

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
