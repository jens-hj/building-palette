<p align="center"><img src="https://raw.githubusercontent.com/jens-hj/building-palette/main/docs/page/banner.jpg" alt="Building Palette: slabs, steps and corners in one slot" width="100%"></p>

<p align="center"><b>Every shape of a block in one stack. Build on a half-block grid, mix materials inside a block, and pay only for what you place.</b></p>

<p align="center"><img src="https://raw.githubusercontent.com/jens-hj/building-palette/main/docs/page/divider.png" alt="Section divider" width="100%"></p>

<img src="https://raw.githubusercontent.com/jens-hj/building-palette/main/docs/page/header_shapes.png" alt="Shapes" height="54">

Hold **Alt** and scroll to change a stack's shape, in your hand or in any inventory. A stack of 64 blocks is 128 slabs, 256 steps or 512 corners, all in the same slot.

<p align="center"><img src="https://raw.githubusercontent.com/jens-hj/building-palette/main/docs/page/scroll.gif" alt="Alt + scroll through the shapes of one stack" width="49%"> <img src="https://raw.githubusercontent.com/jens-hj/building-palette/main/docs/page/shapes.png" alt="Material cost: block 8, stair 6, slab 4, step 2, corner 1" width="49%"></p>

Leftovers are kept: a **+** beside the count means material waiting for a smaller shape.

<p align="center"><img src="https://raw.githubusercontent.com/jens-hj/building-palette/main/docs/page/divider.png" alt="Section divider" width="100%"></p>

<img src="https://raw.githubusercontent.com/jens-hj/building-palette/main/docs/page/header_combine.png" alt="Combine" height="54">

Pieces snap to a half-block grid and stack inside one block, in any mix of materials. Lay a step across another and they merge into an L, costing only the added corner. Mining takes the piece you aim at and returns exactly its material.

<p align="center"><img src="https://raw.githubusercontent.com/jens-hj/building-palette/main/docs/page/merge.gif" alt="Steps and corners merging inside one block" width="40%"> <img src="https://raw.githubusercontent.com/jens-hj/building-palette/main/docs/page/detail_roof.jpg" alt="Brick gable under a stepped roof edge" width="58%"></p>

<p align="center"><img src="https://raw.githubusercontent.com/jens-hj/building-palette/main/docs/page/detail_window.jpg" alt="Window with a protruding sill and lintel" width="100%"></p>

<p align="center"><img src="https://raw.githubusercontent.com/jens-hj/building-palette/main/docs/page/divider.png" alt="Section divider" width="100%"></p>

<img src="https://raw.githubusercontent.com/jens-hj/building-palette/main/docs/page/header_offgrid.png" alt="Offgrid" height="54">

Fences, walls, panes, bars and chains pass through any piece, placed in either order.

<p align="center"><img src="https://raw.githubusercontent.com/jens-hj/building-palette/main/docs/page/detail_bench.jpg" alt="Bench of oak steps on fence legs" width="100%"></p>

<p align="center"><img src="https://raw.githubusercontent.com/jens-hj/building-palette/main/docs/page/divider.png" alt="Section divider" width="100%"></p>

<img src="https://raw.githubusercontent.com/jens-hj/building-palette/main/docs/page/header_controls.png" alt="Controls" height="54">

| Input | Does |
|---|---|
| **Alt** + scroll | Change shape |
| Middle-click | Pick the pointed material; tops up from inventory or [Traveler's Backpack](https://modrinth.com/mod/travelersbackpack) |
| Drop onto a stack | Merge into that stack's shape |
| Shift-click | Fill partial stacks first |

Precise half-grid placement, placement preview and part highlighting are in **Mods → Building Palette → Configure** (Mod Menu on Fabric).

<p align="center"><img src="https://raw.githubusercontent.com/jens-hj/building-palette/main/docs/page/divider.png" alt="Section divider" width="100%"></p>

<img src="https://raw.githubusercontent.com/jens-hj/building-palette/main/docs/page/header_requirements.png" alt="Requirements" height="54">

Install on **client and server**, Minecraft **26.3**, Java **25**.

| | Fabric | NeoForge |
|---|---|---|
| Loader | 0.19.5+ with [Fabric API](https://modrinth.com/mod/fabric-api) | 26.3.0.76+ |
| Required | [Clutter No More](https://modrinth.com/mod/clutternomore) 3.1.x, [Offgrid](https://modrinth.com/mod/offgrid) 1.0.x | same |

**Alpha:** back up your world first, and keep the mod installed while a world contains combined blocks. Combined blocks can't be pushed by pistons. Functional blocks are excluded; pack authors can adjust materials with the `building_palette:included` and `building_palette:excluded` item tags.
