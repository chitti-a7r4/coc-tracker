# Clash Ledger 1.4.0

Changes since the 1.3.0 release.

## Town Hall 19 and new defenses

- **Town Hall 19 (“Clash of Titans”).** Added TH19 artwork to the overview and widgets, support for TH19 exports, and the 27M Gold cost to upgrade from TH18.
- **Squad Barracks (`1000106`).** Added dedicated artwork, name mapping, and Elixir prices for all 12 levels.
- **Firecracker Tower (`1000108`).** Added artwork, name mapping, and upgrade prices of 31M Gold for level 1 and 32M Gold for level 2.
- **Spiky Tower (`1000107`).** Added artwork, name mapping, and upgrade prices of 500k Dark Elixir for level 1 and 510k for level 2.
- **Guardian Shrine (`1000105`).** Added artwork and name mapping for the building that houses Longshot, Smasher, and Logger.
- **New research.** Added names, artwork, and research prices for Heroic Spell and all 12 squads.
- **New helpers.** Added artwork and name mappings for the Alchemist (`93000002`) and Prospector (`93000003`). Timer forecasts use the Builder’s Apprentice and Lab Assistant.
- **Expanded level caps.** Updated the catalogue for Gold and Elixir Storages (20), Dark Elixir Storage (14), Multi-Archer Tower and Ricochet Cannon (5), Spell Factory (10), and Walls (20). The catalogue also includes new levels and revised earlier-level prices for buildings, heroes, and research.

## Next-level loot planner and discount settings

- **Loot planner.** The upgrade queue and detailed widget show the price of the level after the one in progress when that price is available. The compact widget includes this information in its accessibility descriptions.
- **Event and Gold Pass discounts.** Set Builder Jam (50%), Gold Pass builder or research discounts (10%, 15%, or 20%), or a custom percentage in **Settings → Upgrade loot & discounts**.
- **Target level labels.** The upgrade editor labels the level you’re upgrading to as **Target level**. The planner shows **Max** when that target is a known final level.

## Widget improvements

- **Redesigned refresh button.** Both widgets use a symmetrical refresh icon with rounded ends in the app’s gold accent.
- **Direct import.** Both widget layouts have an **Import** button beside the village switcher. Copy an export in Clash of Clans and tap **Import** to open the review. The import form offers clipboard paste and file selection when there’s no export to preview. Tap **Import village** to save.

## Testing and compatibility

- Requires Android 8 or newer. The app targets Android 16, where the signed 1.4.0 APK was tested as an in-place update from 1.3.0.
- Automated tests cover Town Hall values 1–19, TH19 building and research fixtures, loot prices, and discount rules. A live export with active squad research is still needed to confirm where the game places those timers. [Validation details](VALIDATION.md)
