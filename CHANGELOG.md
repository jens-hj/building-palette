# Changelog

## 0.1.0-alpha.20

- Fix Offgrid fences, walls, panes, and bars not fitting into a block that holds a single step. A block with one piece of one material (a step, vertical step, slab, or vertical slab) is now stored as that material's normal block, so Offgrid can blocklog into it like any other step or slab. Mixed materials and other shapes still use a combined block; existing ones convert the next time you place or mine in them.
- Middle-click refills the held stack when it lacks material for the picked shape, such as a full block showing **0+**. It pulls the same material from your inventory first, then your equipped Traveler's Backpack, up to 64 blocks' worth. Stacks with enough material are left alone.
- Fix middle-click sometimes selecting a different hotbar slot with an identical stack after reshaping the held material.

## 0.1.0-alpha.19

- NeoForge: accept NeoForge 26.3.0.76 and newer instead of requiring 26.3.0.102, so the mod loads in packs that stay on 26.3.0.101 because 26.3.0.102 breaks Puzzles Lib. Fabric and gameplay are unchanged.

## 0.1.0-alpha.18

- Replace the icon with a flat isometric design. The Modrinth icon animates through the full block, slab, step, and corner, then mixed-material combinations; the jar uses a still frame. Gameplay is unchanged.

## 0.1.0-alpha.17

- Add native NeoForge support for Minecraft 26.3 alongside Fabric, with separate loader-specific jars and dependencies.
- Share material accounting, placement, inventory, mining, recipes, configuration, and saved data between loaders.
- Render NeoForge combined parts through chunk terrain models with immutable model data, parent textures, ambient occlusion, tint, and material layers.
- Add NeoForge networking, native Mods-screen settings, pick-block integration, and a transactional Traveler’s Backpack adapter.
- Preserve existing registry IDs, item components, and saved-cell schema.

## 0.1.0-alpha.16

- Support Clutter No More 3.1.3 and Offgrid 1.0.5. Dependencies now accept later patch releases (3.1.x, 1.0.x) instead of one exact version.
- Hook exact multipart drops on vanilla block drops, since Clutter No More 3.1.3 removed the class the previous hook targeted.

## 0.1.0-alpha.15

- Use vanilla selection-outline styling for combined parts by default, preserving the aimed-material shape, opacity, line width, transparency handling, and high-contrast accessibility passes.
- Add an optional combined-part highlight with a saved 0–360° hue slider and live colour preview in Mod Menu. The placement-preview toggle remains independent.
- Test actual vanilla outline submissions against multipart submissions in normal and high-contrast modes, custom hues, native-block isolation, and preference persistence.

## 0.1.0-alpha.14

- Always handle horizontal and vertical quarter steps through Building Palette's multipart placement, including the first piece with precise placement disabled. Native stair placement remains unchanged.
- Allow all four step positions to fill a block or combine different materials, preserving exact quarter-block costs.
- Add server coverage for both step directions, horizontal axes, placement modes, and single/mixed materials, plus real client/server placement-packet regressions.

## 0.1.0-alpha.13

- Reliably refresh lighting and occlusion for existing saved pieces after chunk loading. The previous scheduled-tick hook could run before the chunk's tick container existed.
- Skip lighting notifications and neighbor updates when a loaded piece already has the correct properties.
- Add real saved-world upgrade/reopen tests from the published alpha.10, alpha.11, and alpha.12 jars, covering 510 geometry/material fixtures, partial stacks, player inventories, ender chests, containers, drops, waterlogging, light emission, slab roofs, and editing old combined blocks.
- Add a terrain-rendering regression 80 blocks from the camera with entity distance reduced, beyond the old block-entity renderer's cutoff.

## 0.1.0-alpha.12

- Pick-block finds the pointed material in any inventory shape, preserving the first selected stack. Picking again while holding that material reshapes it to match the target without changing its material balance.
- Support the equipped Traveler's Backpack's normal hotbar/storage swap for material-aware pick-block.
- Choose reusable pieces for compound geometry: steps for diagonal steps, and vertical steps for a rotated stair shape.
- Keep multipart block entities during client mining prediction so remaining materials stay visible, targetable, and mineable.
- Select mined/picked parts using the latest received player aim instead of head rotation from the previous server tick.
- Reject placement intersecting the placing player using the exact added-piece collision shape, while allowing pieces in unoccupied parts of the same grid block.
- Add Efficiency V/Haste II rapid-mining, real shifted-mouse, backpack, two-stage pick, player-collision, and native-versus-combined quartz/blackstone lighting regressions.

## 0.1.0-alpha.11

- Add Mod Menu settings for placement outlines and precise half-grid placement, both off by default. Placement controls synchronize per player to the server.
- Use native placement for native shapes by default, preserving stair facing, halves, and corner connections. Keep exact material costs for slab doubling and auto-connected stairs.
- Reject missed, unsupported, and out-of-range placement hits; hide the preview when aiming into empty space.
- Render combined pieces through the terrain pipeline for per-vertex lighting, ambient occlusion, and Sodium/Iris shader compatibility.
- Match native shape-aware skylight and block-light occlusion, including slab roofs. Refresh light-blocking properties for existing combined blocks when chunks load.
- Stabilize fractional inventory cursor updates and preserve vanilla stack identity comparisons.
- Replace the legacy inventory selector and scroll handler with the same palette order as the hotbar, showing choices above and below the hovered slot.
- Center fractional item models in inventories, the palette, and held-item views.
- Cover every source/destination shape pairing with material-based merging and overflow tests.

## 0.1.0-alpha.10

- Stop invisible multipart carriers from emitting missing-texture break particles alongside the correct material particles.
- Keep the removed material's normal break effect, with a client regression for carrier suppression and oak/stone particle emission.

## 0.1.0-alpha.9

- Support Fabric Loader 0.19.5 instead of unnecessarily requiring 0.20.1.
- Build and validate against Loader 0.19.5 so existing compatible instances can load the mod.

## 0.1.0-alpha.8

- Refine the mixed-material icon with straighter isometric edges and more consistent pixel corners; gameplay is unchanged.

## 0.1.0-alpha.7

- Restore the mixed oak, stone, and turquoise icon with coarser pixels and simpler silhouettes; gameplay is unchanged.

## 0.1.0-alpha.6

- Replace the icon with a clearer pixel-art arrangement of matching oak slab, step, and corner pieces.
- Update the AI disclosure for the image-generated icon; gameplay is unchanged.

## 0.1.0-alpha.5

- Draw the vertical selector as one continuous vanilla hotbar, retaining its thin internal dividers instead of repeating black slot borders.

## 0.1.0-alpha.4

- Replace the floating palette panel with a vertical, looping shape strip rising from the active hotbar slot.
- Use Minecraft's hotbar sprites and selection frame, including resource-pack overrides, with no title.
- Anchor the strip to the hovered inventory slot or offhand slot when those are being changed.

## 0.1.0-alpha.3

- Fix shared GUI atlas entries making the full block and vertical step look like extra corners in the shape palette.
- Give generated shape icons a distinct cache identity for their material and shape.
- Add a client regression checking that every palette shape has a distinct GUI model identity.

## 0.1.0-alpha.2

- Replace the vector-style icon with an original 16×16 pixel-art sprite inspired by the other projects.
- Include the new icon in the mod jar and on the Modrinth page; gameplay is unchanged.

## 0.1.0-alpha.1

- Add Alt-scroll building palette with conserved material balances and up to 512 corners per slot.
- Add half-block-grid multipart placement, mixed materials, explicit stairs, and connected-material mining.
- Add selected-shape ingredient matching, reserved fractional material, and proportional furnace fuel.
- Add native storage, cursor, drag, shift-transfer, hopper, and dropped-stack integration.
- Add model, Minecraft server, and graphical client tests.
