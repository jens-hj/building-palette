# Building Palette

Fabric 26.3 companion for Clutter No More and Offgrid. Hold **Alt** and scroll a held block or a stack in an inventory to select its building shape.

One material stack holds up to **64 full blocks**. Scrolling changes the available-piece count in the same slot: **64 blocks → 128 slabs → 256 steps → 512 corners**. Stairs show **85+** when two eighth-block units remain reserved. Hover the stack to see the exact balance.

Shapes combine on a **2×2×2 grid** inside each world block. Add four quarter-volume steps or eight corners, with different materials if desired. Mining removes all face-connected material of the aimed type within that block space. Stairs keep their explicit geometry; add corners to construct inner and outer turns.

Fences and walls remain native and retain their normal connection/placement behavior. They remain selectable in the palette and cost one full-block unit each.

## Installation

Install the same Building Palette jar on **client and server**, with:

- Minecraft 26.3, Java 25, Fabric Loader 0.20.1 or later
- Fabric API 0.165.0+26.3
- Clutter No More **3.1.1+26.3**
- Offgrid **1.0.4+26.3**

This alpha is for a copied instance and disposable world first. Existing ordinary items are valued by their current shape's physical volume. Historical losses from prior Clutter No More conversions cannot be reconstructed.

Selected native shapes remain real ingredients: select slabs for a barrel and full planks for a chest. The companion disables Clutter No More's broad shape-ingredient substitution and recipe replacements. Crafting output slots cannot be scrolled. Fractional leftover material never satisfies an ingredient or burns as a whole fuel item.

**World removal:** retain this mod while a world contains multipart blocks. Restore a pre-install backup before removing it; no automatic world export is provided.

## Development

```sh
nix develop
./gradlew build               # model assertions and Minecraft server GameTests
python scripts/headless-test.py ./gradlew --no-daemon runClientGameTest   # private display and screenshots
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
