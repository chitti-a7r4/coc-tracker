# Clash Ledger 1.4.0

All improvements since the 1.3.0 release are included here.

## Town Hall 19 and new defenses

- **Town Hall 19 ("Clash of Titans"):** Full support including base artwork, 27M Gold upgrade cost, and export parser compatibility.
- **Squad Barracks (`1000106`):** Dedicated artwork, name mapping, and complete 12-level Elixir upgrade cost progression.
- **Firecracker Tower (`1000108`):** Merged defense artwork, name mapping, and Level 1 (31M) & Level 2 (32M) Gold upgrade costs.
- **Spiky Tower (`1000107`):** Dark Elixir defense artwork, name mapping, and Level 1 (500k DE) & Level 2 (510k DE) upgrade costs.
- **Guardian Shrine (`1000105`):** Housing for base Guardians (Longshot, Smasher, Logger) with artwork and name mapping.
- **New Helpers:** Artwork and name mappings for The Alchemist (`93000002`) and The Prospector (`93000003`).
- **Expanded level capacities:** Updated catalog maximum levels for Gold/Elixir storages (Lv 20), Dark Elixir storages (Lv 14), Multi-Archer Tower (Lv 5), Ricochet Cannon (Lv 5), Spell Factory (Lv 10), and Walls (Lv 20).

## Next-level loot planner & discount settings

- **Loot planner:** Displays the exact resource cost (Gold, Elixir, Dark Elixir) required for the next upgrade level directly on upgrade queue items, editor sheets, and widget views.
- **Event and Gold Pass discounts:** Easily configure discounts including Builder Jam (50%), Gold Pass Builder reductions (10%, 15%, 20%), Gold Pass Research reductions (10%, 15%, 20%), or custom percentage discounts.
- **Level target labels:** Clarified labeling to display current vs. target upgrade levels cleanly.

## Widget visual & UX enhancements

- **Redesigned refresh button:** Replaced the legacy distorted vector with a mathematically symmetrical, rounded-cap vector in the app's gold accent (`#F2C46C`).
- **Direct import button:** Added an import button alongside the village switcher and refresh button on both Compact and Detailed widgets.
- **One-tap clipboard import:** Tapping the widget import button automatically detects valid village JSON on the device clipboard and immediately opens the Review Village Import confirmation dialog.

## Testing and quality assurance

- Expanded automated unit test suite to cover Town Hall levels 1–19, real TH19 export shapes with new defense items, loot cost calculations, and discount rules.
- Fully compatible with Android 8.0+ (`minSdk 26`) through Android 16 (`targetSdk 36`).
