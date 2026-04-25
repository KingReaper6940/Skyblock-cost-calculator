# Skyblock Cost Calculator

This repo started as a Hypixel SkyBlock craft-cost website, and now also contains the Fabric client mod that we are actively building and shipping.

The mod is the main thing here.

`CraftCost` adds a lightweight tooltip overlay in Hypixel SkyBlock that compares market price against the direct craft cost of an item. Instead of only telling you the lowest BIN, it can also show the raw craft cost and explain which option is better.

## What the mod does

When you hover a SkyBlock item for long enough, the mod:

1. identifies the SkyBlock item ID from its NBT,
2. loads recipe data from the local `skyblock-repo-cache`,
3. fetches current price data from Coflnet,
4. prices the immediate recipe ingredients using their market prices,
5. shows the result directly in the tooltip.

The tooltip is intentionally conservative so it does not hammer your game or the pricing API during normal browsing.

## Current behavior

As of `craftcost 1.0.0`, the tooltip flow works like this:

- No price checks happen immediately when you glance over an item.
- You must hold the same SkyBlock item for 10 seconds before calculation starts.
- After that, CraftCost queues the item and its direct recipe ingredients for pricing, then processes requests slowly in the background.
- For craftable items, the tooltip stays in a loading state until the immediate ingredient prices are ready, so it does not flash partial or incorrect craft costs before settling.
- Once the needed data is available, the tooltip can show:
  - `Lowest BIN` or `Bazaar Buy`
  - `Raw Craft Cost`
  - a recommendation line such as `Craft cost is higher, buy Lowest BIN`
  - an ingredient cost breakdown

This means the mod trades speed for stability on purpose. It is built to be quiet during ordinary inventory movement and REI browsing.

Direct craft cost means CraftCost prices only one layer of ingredients. If an ingredient is itself a crafted item, the mod uses that ingredient's Bazaar or AH price instead of recursively expanding it again. This makes items such as `ATOMSPLIT_KATANA` behave more intuitively in-game.

## How recipe loading works

The mod does not depend entirely on REI anymore.

Its primary recipe source is the local SkyBlock repo cache at:

```text
<minecraft game dir>/skyblock-repo-cache/recipes.min.json
```

That cache is usually created by other SkyBlock mods that sync repo data. In practice, the setup that has been tested most is a profile with mods such as Skyblocker, Firmament, Catharsis, or similar tooling that keeps the SkyBlock repo cache up to date.

CraftCost currently loads these recipe types from the local cache:

- `crafting`
- `forge`
- `shop`

REI support is still present as a fallback and compatibility layer, but it is treated as best-effort and should never be the thing that takes the client down.

## How price loading works

Price data comes from SkyCofl / Coflnet.

To keep the mod light:

- requests are queued instead of fired immediately,
- only hovered items are tracked,
- the queue is capped,
- refreshes are spaced out,
- retry attempts are cooled down,
- cached results are reused where possible.

This is why an item may briefly show `Calculating...` before the full craft result appears.

## Requirements

For the current mod build:

- Minecraft `1.21.11`
- Fabric Loader `0.18.5+`
- Java `21`
- Fabric API `0.141.3+1.21.11`

Recommended:

- Roughly Enough Items (optional, but useful)
- another SkyBlock mod that populates `skyblock-repo-cache`

Without a local SkyBlock recipe cache, price lines can still work, but craft-cost calculations will be limited or unavailable.

## Building the mod

The Fabric mod lives in [`mod/`](mod/).

From the repo root:

```powershell
cd mod
.\gradlew.bat build
```

Built jars are written to:

```text
mod/build/libs/
```

The current version in source is:

```text
craftcost-1.0.0
```

## Installing the mod

1. Build the jar or grab the latest built jar from `mod/build/libs/`.
2. Put it in your Fabric profile's `mods` folder.
3. Make sure the profile also has Fabric API.
4. For full craft-cost support, use a SkyBlock mod setup that maintains `skyblock-repo-cache`.

## Config

The mod writes a JSON config file to:

```text
config/craftcost.json
```

Current settings:

- `enabled`
- `refreshIntervalSeconds`
- `showBreakdown`
- `showSavings`

## Tooltip examples

Depending on what data is available, the tooltip may show results like:

```text
Lowest BIN: 10.9M
Raw Craft Cost: 11.4M
Reason: Craft cost is higher, buy Lowest BIN
```

or:

```text
Lowest BIN: 10.9M
Raw Craft Cost: 9.8M
Reason: Crafting saves 1.1M
```

## Known limitations

- The local repo cache loader currently supports `crafting` and `forge` recipes, not every SkyBlock recipe type yet.
- Craft results depend on the direct ingredient prices that are available from the current cache/API state.
- The 10-second hover delay is intentional and not a bug.

## Web app

There is also a Next.js app in the repo root under [`src/`](src/). It is the original website-side calculator project and still exists as a companion surface for browsing recipes and pricing ideas.

To run it locally:

```powershell
npm install
npm run dev
```

## Repo layout

```text
mod/                     Fabric mod source
src/                     Next.js app source
scripts/                 helper scripts for the web side
README.md                project documentation
AGENTS.md                repo-specific instructions for coding agents
```

## Documentation policy

This README is meant to track the real behavior of the project, not the ideal version in our heads.

If the mod's tooltip flow, recipe sources, compatibility target, config format, or installation story changes, the README should be updated in the same change.

## Release 1.0.0

`1.0.0` is the first release meant to be publishable on GitHub and Modrinth.

Main release points:

- one-layer craft-cost pricing for practical in-game comparisons
- long-hover activation to avoid normal inventory lag
- queued and deduplicated price fetching
- direct ingredient loading from local SkyBlock repo cache
- soft-fail REI fallback instead of crash-prone hard dependency
- cleaner caching and lower repeated tooltip work

## Credits

- Hypixel SkyBlock for the game data context
- SkyCofl / Coflnet for price data
- Fabric, Fabric API, and the surrounding modding ecosystem
