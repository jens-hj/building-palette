# Compatibility and data

## Inventory format

A native shape retains its native item identity. The ordinary item count stores the number of whole pieces plus one reserved partial piece, if needed. `building_palette:missing_units` records how much of that final piece is missing. Total material is `count * selected_shape_cost - missing_units`.

New shape items use `building_palette:piece`, with `building_palette:material` holding the parent item identifier and `building_palette:shape` holding the palette shape. Counts displayed to the player exclude the partial piece and add `+` for a remainder.

Costs in eighth-block units: block 8; slab/vertical slab 4; straight stair 6; step/vertical step 2; corner 1. Fences/walls remain at 8 as native convenience conversions. Capacity is 512 units. Persistent stack count codecs accept up to 512; runtime capacity changes only for supported material families.

## World format

Multipart blocks store eight material identifiers in `cells` and schema version 1. Empty strings represent air. Face adjacency determines mining groups. Block entity synchronization and saves carry the same cells. Complete cubes of a single material become ordinary native blocks.

Multipart blocks are immovable by pistons. Mixed pieces do not tick, oxidize, or reproduce arbitrary functional-block behavior. Block entities and families with no full-block parent (such as standalone slabs) are excluded from material families. Unknown material identifiers render as stone rather than crashing; keep their source mods installed to recover the intended material.

## Integration ownership

- Building Palette handles solid shape placement and stacking for managed families; Offgrid retains its other placement features and native fences/walls. A block holding one material in a single slab, vertical slab, step, or vertical step is stored as that native block, so Offgrid can blocklog fences, walls, panes, and bars into it. Building Palette lifts Offgrid's rule that these must run along a step, so a post can stand anywhere in a step block. The order does not matter: a step placed into a block holding a fence takes it in as an Offgrid overlay; combined mixed-material blocks are not Offgrid blocklog hosts.
- Native items other than horizontal/vertical quarter steps use their original placement code by default. Precise half-grid placement is an opt-in per-player preference, synchronized from the client's configuration; the preview toggle is independent and defaults off. Generic pieces without a native item and combinations inside multipart blocks use the half-grid representation.
- Auto-connected native inner/outer stairs retain the value of one stair and stay native. They are not converted into multipart cells, because their 5/7-cell geometry differs from their 6-unit item cost. Use precise placement for explicit stair-and-corner combinations.
- Multipart geometry uses the loader’s terrain model API (Fabric render data or NeoForge immutable ModelData), receiving chunk lighting and ambient occlusion through Indigo or Sodium/Iris. Material tint, render layer, and emissive properties remain attached to each quad.
- Opaque octants contribute their exact shape to the light engine. Bottom/top slab roofs block skylight and overhead block light like native slabs. Existing multipart block entities queue an occlusion refresh for the end of the level tick after loading, preserving the existing saved-cell schema. Already-correct properties do not trigger redundant updates.
- Pick-block runs after vanilla server validation, through Fabric’s pick event or a NeoForge hook at the vanilla clone-stack lookup. Existing material is selected without changing shape on the first pick; a second pick reshapes the held balance. The optional Traveler's Backpack adapter is compiled against the corresponding loader build of 26.3-11.4.3 and uses its equipped storage access and normal hotbar swap, preserving the displaced stack. NeoForge storage changes commit atomically through its transaction API.
- Multipart mining leaves the client carrier intact until the server sends authoritative remaining cells. Removing the final material removes the block and collision normally. Part targeting uses the latest received player rotation, avoiding the server head-angle delay during rapid turns.
- Custom placement includes the placing player in obstruction checks and tests only the exact added-piece shape. Non-overlapping pieces in the same occupied grid cell and slabs touching the player’s feet remain valid.
- Clutter No More still supplies material families and its shape key. Its broad recipe matching, recipe replacement/removal, and one-for-one conversion are overridden by this companion.
- Native recipe and processing consumers see native item identifiers. Shared ingredient matching rejects incomplete pieces. Recipe book counts include only whole available pieces. Furnace fuel value is proportional to parent material and shape volume.
- Inventory movement, container capacities, drops, and hoppers use material units for managed stacks. Named/custom component differences prevent unintended merging.
- Optional mod machines using standard Minecraft ingredient/stack APIs inherit these rules. Bespoke handlers require explicit validation; arbitrary third-party processing is not guaranteed.

Material discovery reads the live Clutter No More map after reload. `building_palette:included` and `building_palette:excluded` item tags allow pack overrides; exclusion takes precedence. Inclusion cannot make functional block entities safe building materials.

Combined pieces have block-entity storage, but use chunk terrain geometry rather than a block-entity renderer. Their visibility follows terrain render distance. They have no continuously ticking block-entity ticker.

## Validation record

The executable checks cover pure geometry/accounting, native stack serialization, templates, container capacities, split/merge/drag operations, partial ingredients/fuel, repeated chest and barrel matching, multipart saves, and client synchronization/rendering. RPG-specific test results are recorded in `VALIDATION.md` after the full fixture run.

### Test harness

The copied RPG launch runs on Xvfb so failed launches cannot display error dialogs or capture desktop input. Crash Assistant is omitted. A separate Java test agent suppresses FrozenLib 3.0.3's early development-only log, reproducing its normal non-development startup behavior; original jars remain unchanged. The test mod also retains copied options because some RPG mods register keybinds after Fabric's default-options snapshot. Neither workaround is packaged in Building Palette.

Horizontal and vertical quarter steps always use Building Palette placement and merging, including the first piece. Native stairs still use native facing/half/corner placement by default. This prevents delegation to CNM’s restricted native step states.

Selection rendering keeps vanilla’s submission path, changing only the aimed material’s shape. The default colour, alpha, line width, render type, and high-contrast passes remain vanilla. `highlightParts` (default false) optionally recolours the primary multipart selection stroke using `outlineHue` (0–360°, default 48). These are client-only preferences, independent of placement previews. Old config files without the new fields retain vanilla styling.

Ordinary resource-pack block textures are read from Minecraft’s current parent block models and baked sprite atlas; the mod does not ship hard-coded copies of building-material textures. Resource reloads rebake those models through the normal model-loading hook. Special connected-texture effects and unusual custom 3D models require separate compatibility testing.

## Loader builds

Minecraft 26.3 builds are produced separately for Fabric and NeoForge. Both retain the `building_palette` mod ID, registry IDs, data components, and schema-1 `cells` format. The `common` module owns gameplay and geometry; each loader supplies registration, packets, lifecycle, input, terrain-model integration, and optional backpack access. NeoForge uses immutable `ModelData`, refreshes it after block-entity packets, and keeps combined geometry in chunk meshes. Its backpack adapter uses transactions, preserving displaced hotbar items and fractional balances.

Use matching loader jars for Building Palette, Clutter No More, Offgrid, and optional mods. Mixed Fabric/NeoForge multiplayer is not supported. Preserving Building Palette's format does not guarantee that an entire modpack can change loaders; other mods have independent registries and saved data.

NeoForge shader testing is currently blocked by the published renderer versions: Sodium 0.9.2 fails its `LevelExtractorMixin` on NeoForge 26.3.0.102, and Sodium 0.9.3-beta.4 rejects Iris 1.11.7 because it requires Iris 1.11.8+. No dependency overrides or renderer-jar patches are shipped. Fabric shader validation remains recorded below/in VALIDATION.md; NeoForge shader compatibility must be checked again with a compatible upstream pair.
