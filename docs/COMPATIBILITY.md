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
- Clutter No More still supplies material families and its shape key. Its broad recipe matching, recipe replacement/removal, and one-for-one conversion are overridden by this companion.
- Native recipe and processing consumers see native item identifiers. Shared ingredient matching rejects incomplete pieces. Recipe book counts include only whole available pieces. Furnace fuel value is proportional to parent material and shape volume.
- Inventory movement, container capacities, drops, and hoppers use material units for managed stacks. Named/custom component differences prevent unintended merging.
- Optional mod machines using standard Minecraft ingredient/stack APIs inherit these rules. Bespoke handlers require explicit validation; arbitrary third-party processing is not guaranteed.

Material discovery reads the live Clutter No More map after reload. `building_palette:included` and `building_palette:excluded` item tags allow pack overrides; exclusion takes precedence. Inclusion cannot make functional block entities safe building materials.

## Validation record

The executable checks cover pure geometry/accounting, native stack serialization, templates, container capacities, split/merge/drag operations, partial ingredients/fuel, repeated chest and barrel matching, multipart saves, and client synchronization/rendering. RPG-specific test results are recorded in `VALIDATION.md` after the full fixture run.

### Test harness

The copied RPG launch runs on Xvfb so failed launches cannot display error dialogs or capture desktop input. Crash Assistant is omitted. A separate Java test agent suppresses FrozenLib 3.0.3's early development-only log, reproducing its normal non-development startup behavior; original jars remain unchanged. The test mod also retains copied options because some RPG mods register keybinds after Fabric's default-options snapshot. Neither workaround is packaged in Building Palette.
