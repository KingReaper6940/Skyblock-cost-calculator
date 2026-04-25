# Skyblock Cost Calculator

This repo started as a Hypixel SkyBlock craft-cost website, and now also contains the Fabric client mod that we are actively building and shipping.

The mod is the main thing here.

`CraftCost` adds a lightweight tooltip overlay in Hypixel SkyBlock that compares market price against the direct craft cost of an item. Instead of only telling you the lowest BIN, it can also show the raw craft cost and explain which option is better.

## What the mod does

When you hover a SkyBlock item for long enough, the mod:

1. identifies the SkyBlock item ID from its NBT,
2. reads recipe data from REI's loaded recipe displays,
3. fetches current price data from SkyCofl / Coflnet,
4. prices the immediate recipe ingredients using their market prices,
5. shows the result directly in the tooltip.

The tooltip is intentionally conservative so it does not hammer your game or the pricing API during normal browsing.

## Current behavior

As of `craftcost 1.0.4`, the tooltip flow works like this:

- No price checks happen immediately when you glance over an item.
- You must hold the same SkyBlock item for 10 seconds before calculation starts.
- After that, CraftCost queues the item and its direct recipe ingredients for pricing, then processes requests slowly in the background.
- For craftable items, the tooltip stays in a loading state until the immediate ingredient prices are ready, so it does not flash partial or incorrect craft costs before settling.
- Once the needed data is available, the tooltip can show:
  - `Lowest BIN` or `Bazaar Buy`
  - `Raw Craft Cost`
  - a recommendation line such as `Not worth crafting, buy on AH instead`
  - an ingredient cost breakdown

This means the mod trades speed for stability on purpose. It is built to be quiet during ordinary inventory movement and REI browsing.

Direct craft cost means CraftCost prices only one layer of ingredients. If an ingredient is itself a crafted item, the mod uses that ingredient's Bazaar or AH price instead of recursively expanding it again. This makes items such as `ATOMSPLIT_KATANA` behave more intuitively in-game.

## How recipe loading works

CraftCost is back to REI-first recipe loading.

That is intentional. REI already receives the recipe displays from the SkyBlock mods in your profile, so using REI means CraftCost reads the same item stacks and SkyBlock IDs that you are actually seeing in-game. That is more reliable than shipping our own frozen recipe copy.

Startup recipe source order:

1. REI recipe displays
2. local `skyblock-repo-cache/recipes.min.json`, only if REI exposes no recipes after repeated startup attempts

The local repo fallback is a safety net, not the normal path. CraftCost no longer ships a bundled recipe snapshot inside the jar.

## How price loading works

Price data comes from SkyCofl / Coflnet.

To keep the mod light:

- most items use Coflnet's compact current-price endpoint first,
- if that misses, CraftCost tries Bazaar snapshot,
- if Bazaar misses too, CraftCost tries active AH BIN,
- requests are queued instead of fired immediately,
- only hovered items are tracked,
- the queue is capped,
- refreshes are spaced out,
- retry attempts are cooled down,
- cached results are reused where possible.

This is why an item may briefly show `Calculating...` before the full craft result appears.

Coflnet's public API is rate-limited, so CraftCost is deliberately paced. The documented free limits are 30 requests per 10 seconds and 100 requests per minute. A normal item price is usually one request now; fallback pricing can take more than one request only when the compact endpoint misses.

## Requirements

For the current mod build:

- Minecraft `1.21.11`
- Fabric Loader `0.18.5+`
- Java `21`
- Fabric API `0.141.3+1.21.11`
- Roughly Enough Items `21.11.x`
- internet access to `sky.coflnet.com`

Recommended:

- a SkyBlock mod setup that registers SkyBlock recipes into REI
- another SkyBlock mod that populates `skyblock-repo-cache` as a fallback

Without REI recipe displays, price lines can still work, but craft-cost calculations will be limited or unavailable unless the local repo fallback is present.

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
craftcost-1.0.4
```

## Installing the mod

1. Build the jar or grab the latest built jar from `mod/build/libs/`.
2. Put it in your Fabric profile's `mods` folder.
3. Make sure the profile also has Fabric API.
4. Install REI and use a SkyBlock mod setup that registers SkyBlock recipes into REI.

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
Reason: Not worth crafting, buy on AH instead
```

or:

```text
Lowest BIN: 10.9M
Raw Craft Cost: 9.8M
Reason: Crafting saves 1.1M
```

## Known limitations

- CraftCost depends on the recipes exposed by the user's current REI/SkyBlock mod setup.
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

## Recipe sources

CraftCost now uses an in-game-source-first order for recipes:

1. REI recipe displays
2. the user's local `skyblock-repo-cache/recipes.min.json`, only if REI never produces recipes

This keeps recipe IDs tied to the actual item stacks in the running client. It also removes the stale bundled snapshot that caused wrong or confusing item IDs.

## Release 1.0.4

`1.0.4` is a tooltip wording polish release.

Main release points:

- explicitly says `Not worth crafting` when buying is cheaper
- recommendation text now names `AH` or `Bazaar` based on the current market source
- shows how much more crafting would cost in the buy-instead case

## Release 1.0.3

`1.0.3` is a recipe and pricing reliability hotfix.

Main release points:

- removed the bundled recipe snapshot
- made REI the primary recipe source again
- kept local repo recipes only as a late fallback
- switched normal price lookups to Coflnet's compact current-price endpoint
- fall back to Bazaar snapshot, then AH active BIN, only when needed
- reduced noisy API warnings for expected misses
- URL-encodes item tags before calling Coflnet

## Release 1.0.2

`1.0.2` is a recipe-source reliability release.

Main release points:

- bundled a normalized SkyBlock recipe snapshot inside the mod
- local repo cache now augments the bundled snapshot instead of being the only trustworthy source
- REI is kept only as a final fallback path for missing items

## Release 1.0.1

`1.0.1` is a hotfix release for tooltip resolution logic.

Main release points:

- the tooltip now considers an item ready when any complete direct recipe is priceable
- hovered items can request ingredient pricing again if recipe data appears slightly later via fallback

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
