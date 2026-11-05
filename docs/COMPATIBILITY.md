# Compatibility and data

## Inventory format

A native shape retains its native item identity. The ordinary item count stores the number of whole pieces plus one reserved partial piece, if needed. `building_palette:missing_units` records how much of that final piece is missing. Total material is `count * selected_shape_cost - missing_units`.

New shape items use `building_palette:piece`, with `building_palette:material` holding the parent item identifier and `building_palette:shape` holding the palette shape. Counts displayed to the player exclude the partial piece and add `+` for a remainder.

Costs in eighth-block units: block 8; slab/vertical slab 4; straight stair 6; step/vertical step 2; corner 1. Fences/walls remain at 8 as native convenience conversions. Capacity is 512 units. Persistent stack count codecs accept up to 512; runtime capacity changes only for supported material families.

## World format

Multipart blocks store eight material identifiers in `cells` and schema version 1. Empty strings represent air. Face adjacency determines mining groups. Block entity synchronization and saves carry the same cells. Complete cubes of a single material become ordinary native blocks.

Multipart blocks are immovable by pistons. Mixed pieces do not tick, oxidize, or reproduce arbitrary functional-block behavior. Block entities and families with no full-block parent (such as standalone slabs) are excluded from material families. Unknown material identifiers render as stone rather than crashing; keep their source mods installed to recover the intended material.

## Integration ownership

- Building Palette handles solid shape placement and stacking for managed families; Offgrid retains its other placement features and native fences/walls.
- Native items use their original placement code by default. Precise half-grid placement is an opt-in per-player preference, synchronized from the client's configuration; the preview toggle is independent and defaults off. Generic pieces without a native item and combinations inside multipart blocks use the half-grid representation.
- Auto-connected native inner/outer stairs retain the value of one stair and stay native. They are not converted into multipart cells, because their 5/7-cell geometry differs from their 6-unit item cost. Use precise placement for explicit stair-and-corner combinations.
- Multipart geometry uses Fabric's terrain model API and immutable block-entity render data, receiving chunk lighting and ambient occlusion through Indigo or Sodium/Iris. Material tint, render layer, and emissive properties remain attached to each quad.
- Opaque octants contribute their exact shape to the light engine. Bottom/top slab roofs block skylight and overhead block light like native slabs. Existing multipart block entities queue an occlusion refresh for the end of the level tick after loading, preserving the existing saved-cell schema. Already-correct properties do not trigger redundant updates.
- Pick-block uses Fabric's server-side pick event before ordinary callbacks. Existing material is selected without changing shape on the first pick; a second pick reshapes the held balance. The optional Traveler's Backpack adapter is compiled against Fabric 26.3-11.4.3 and uses its equipped storage access and normal hotbar swap, preserving the displaced stack.
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
