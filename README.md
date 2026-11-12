# Building Palette

Fabric and NeoForge 26.3 companion for Clutter No More and Offgrid. Hold **Alt** and scroll a held block or a stack in an inventory to select its building shape.

A vertical strip of shapes rises from the active hotbar slot while Alt is held. Scrolling pulls shapes into that slot and loops through the choices. The strip uses Minecraft's hotbar textures, has no title, and follows the hovered inventory slot or offhand when editing those stacks.

In inventories, nearby choices appear above **and** below the hovered stack. Both selectors use the same shape order, including corners. Drop any shape onto another stack of the same material to fill the destination in its existing shape; only material beyond the 64-block capacity remains on the cursor.

Shift-click fills compatible material stacks before empty slots, preserving each destination's shape. Middle-click a block to select its material from any shape in your inventory or equipped Traveler's Backpack. The first pick keeps that existing stack's shape; pick again while holding the material to match the target shape without changing its balance. When the held stack lacks material for the picked shape (for example a full block showing **0+**), picking refills it from the inventory, then the equipped backpack, up to 512 units. For compound pieces, the picker chooses a reusable shape that tiles the pointed material's geometry: diagonal steps pick steps, and a vertical slab plus a vertical step picks vertical steps. Mixed blocks pick the material you point at.

Open **Mods → Building Palette → Configure** on NeoForge, or with Mod Menu installed on Fabric. **Placement outline**, **Precise half-grid placement**, and **Highlight combined parts** default to **Off**. Selection outlines match vanilla by default; enable **Highlight combined parts** and adjust **Outline hue** to distinguish them with your chosen colour. With precise placement off, slabs and stairs use their normal placement code: stairs follow your facing, support upside-down placement, and connect into native corners. Turn precise placement on to use clicked half-grid positions for solid pieces. Settings apply per player and synchronize to the server; outlines remain client-side. Settings are saved in `config/building_palette.json` and can also be edited there while the game is closed. Horizontal and vertical quarter steps, corners, and pieces without a native item always use Building Palette’s half-grid placement and mixed-material combining.

[Modrinth](https://modrinth.com/mod/building-palette) · [Source and issues](https://github.com/jens-hj/building-palette)

One material stack holds up to **64 full blocks**. Scrolling changes the available-piece count in the same slot: **64 blocks → 128 slabs → 256 steps → 512 corners**. Stairs show **85+** when two eighth-block units remain reserved. Hover the stack to see the exact balance.

Shapes combine on a **2×2×2 grid** inside each world block. Add four quarter-volume steps or eight corners, with different materials if desired. Mining removes all face-connected material of the aimed type within that block space. Precise placement keeps stairs’ explicit geometry; add corners to construct inner and outer turns.

Fences and walls remain native and retain their normal connection/placement behavior. They remain selectable in the palette and cost one full-block unit each.

## Installation

Choose the jar for your loader and install it on **client and server**. Both builds require Minecraft 26.3, Java 25, and matching loader builds of Clutter No More and Offgrid.

Fabric requirements:

- Minecraft 26.3, Java 25, Fabric Loader 0.19.5 or later
- Fabric API 0.165.0+26.3
- Clutter No More **3.1.3+26.3** (3.1.x)
- Offgrid **1.0.5+26.3** (1.0.x)

NeoForge requirements:

- NeoForge **26.3.0.76** or later for Minecraft 26.3
- Clutter No More **3.1.3+26.3** (3.1.x), NeoForge build
- Offgrid **1.0.5+26.3** (1.0.x), NeoForge build

Fabric API and Mod Menu are only used on Fabric. Forge is not supported. Use the same loader on client and server. A Fabric-to-NeoForge modpack migration is separate from updating Building Palette.

This alpha is for a copied instance and disposable world first. Existing ordinary items are valued by their current shape's physical volume. Historical losses from prior Clutter No More conversions cannot be reconstructed.

Selected native shapes remain real ingredients: select slabs for a barrel and full planks for a chest. The companion disables Clutter No More's broad shape-ingredient substitution and recipe replacements. Crafting output slots cannot be scrolled. Fractional leftover material never satisfies an ingredient or burns as a whole fuel item.

**World removal:** retain this mod while a world contains multipart blocks. Restore a pre-install backup before removing it; no automatic world export is provided.

## Development

```sh
nix develop
./gradlew build               # both jars and shared model assertions
./gradlew :fabric:runGameTest :neoforge:runGameTestServer # server integration suites
python scripts/headless-test.py ./gradlew --no-daemon runClientGameTest   # Fabric
BUILDING_PALETTE_TEST_LOADER=neoforge python scripts/headless-test.py ./gradlew --no-daemon :neoforge:runClientTest
```

Dependencies are pinned to public Modrinth version IDs. No proprietary dependency code or jars are included in this repository or its published jar. Development and testing do not read the Modrinth publishing token.

For the RPG fixture:

```sh
python scripts/prepare-rpg-test.py "/path/to/RPG"
nix develop --command python scripts/headless-test.py ./gradlew --no-daemon runClientGameTest -PrpgMods="$PWD/build/rpg-instance/mods"
```

The fixture copies mods and configuration, never live worlds or account files. Previews are saved in `build/previews/`. See [compatibility notes](docs/COMPATIBILITY.md) for tested integrations and limits.

MIT license. Independently implemented; requires the original mods at runtime.

Prepare reviewable release files without uploading anything:

```sh
python scripts/package-release.py
```

The full-RPG development test suppresses FrozenLib's development-only early log (which otherwise loads translations before Controlify can prepare its mixin), retains fixture options instead of restoring Fabric's incomplete keybind snapshot, and omits Crash Assistant. These harness changes are absent from the released mod.

## Publishing

The Modrinth project is configured with an icon, gallery, MIT license, both-side requirements, and pinned companion dependencies. Alpha.24 has separate uploaded Fabric and NeoForge release files with pinned loader-specific dependencies. The Modrinth project is still awaiting public review.

For a new release, update `version` in `gradle.properties` and add its `CHANGELOG.md` section, then run `./gradlew build :fabric:modrinth :neoforge:modrinth`. Use `./gradlew modrinthSyncBody` to update the description. Minotaur loads credentials internally from `MODRINTH_TOKEN` or the external user configuration file; credentials must never be committed or printed.

Combined pieces use chunk terrain rendering and block render distance. End-to-end saved-world upgrade checks are available through `nix develop --command python scripts/world-upgrade-test.py`; they use released old jars and disposable copies under `build/migration/`.

The loader adapters live in `fabric/` and `neoforge/`; shared gameplay, model geometry, and resources live in `common/`. Build outputs are `build/libs/*-fabric.jar` and `neoforge/build/libs/*-neoforge.jar`. The common jar is a development artifact, not a player download.

Saved-world regression commands (disposable fixtures only):

```sh
python scripts/world-upgrade-test.py       # published older Fabric saves → current Fabric
python scripts/neoforge-world-test.py     # native NeoForge save, edit, and reopen
```

NeoForge graphics integration can be tested with `-PneoGraphics` (Sodium 0.9.3-beta.4) and `-PneoBackpack`. The currently published Iris 1.11.7 is incompatible with that Sodium version; NeoForge shader testing awaits a compatible upstream release. See [compatibility notes](docs/COMPATIBILITY.md).
