# Building Palette

Build with slabs, stairs, quarter-volume steps, and eighth-volume corners while keeping your material in one inventory slot.

Hold **Alt** and scroll a held stack or an inventory stack to change its shape. A full stack of 64 blocks becomes 128 slabs, 256 steps, or 512 corners **in the same slot**. Stair leftovers remain stored and become usable when you select another shape.

The selector rises vertically from the active hotbar slot using Minecraft's hotbar styling. In inventories it shows choices above and below the hovered stack, with the same shape order and corners. Drop any shape onto another stack of the same material to merge into the destination's shape, leaving only overflow beyond 64 blocks of material.

Shift-click also fills existing material stacks first. Middle-click selects the pointed material from any shape in your inventory or equipped Traveler's Backpack; pick again while holding it to match the target shape while keeping the same material balance. If the held stack is too small for the picked shape (a full block showing **0+**), picking tops it up with the same material from your inventory or backpack. Compound shapes choose a reusable building piece, such as steps for diagonal steps or vertical steps for a rotated stair. Mixed blocks pick the material you point at.

On NeoForge, or on Fabric with Mod Menu installed, open **Mods → Building Palette → Configure**. Placement previews, precise half-grid placement, and coloured part highlighting default off. Selection outlines match vanilla by default; enable **Highlight combined parts** and choose **Outline hue** for an optional custom-colour highlight. Slabs and stairs use their normal placement rules by default, including stair facing, upside-down placement, and corner connections. Enable precise placement to use the clicked half-grid position instead. Placement controls apply per player; horizontal and vertical quarter steps, corners, and pieces without a native item always use Building Palette’s half-grid placement and mixed-material combining. Settings are also available in `config/building_palette.json`.

Combine pieces on a half-block grid, including different materials in the same block space. Mine the aimed material to remove all face-connected pieces of that material within the block. Explicit stairs and corner pieces allow small details with ordinary Minecraft block textures.

Selected slabs remain slab ingredients, and planks remain plank ingredients. Fences and walls stay available in the palette with their normal behavior.

## Requirements

With Offgrid, fences, walls, panes, and bars fit into any block holding a single step or slab, including a lone post beside a step.

Client **and** server: Minecraft **26.3**, Java **25**, [Clutter No More 3.1.3+26.3 (3.1.x)](https://modrinth.com/mod/clutternomore), and [Offgrid 1.0.5+26.3 (1.0.x)](https://modrinth.com/mod/offgrid). Choose matching builds for your loader:

- **Fabric:** Loader 0.19.5 or newer and [Fabric API](https://modrinth.com/mod/fabric-api). Mod Menu is optional for settings.
- **NeoForge:** 26.3.0.76 or newer for Minecraft 26.3. Settings are available in NeoForge’s Mods screen.

Install the Building Palette jar for the same loader on both sides. Forge is not supported.

Building Palette is a companion: keep both original mods installed. It handles material accounting and solid-piece combinations while fences and walls keep their native placement behavior.

## Survival building

- Full block: 8 material units
- Slab or vertical slab: 4 units
- Straight stair: 6 units
- Step or vertical step: 2 units
- Corner cube: 1 unit

Each material stack holds up to 512 units. A **+** beside a count means some material is reserved until you choose a smaller shape. Mining returns the exact removed material amount.

Families from the RPG test instance were checked across vanilla and modded building blocks. Functional blocks and families without a full-block parent are excluded. Pack authors can adjust supported materials with the `building_palette:included` and `building_palette:excluded` item tags.

## Alpha limitations

Test in a copied instance and disposable world first. Multipart blocks cannot be pushed by pistons and do not reproduce ticking/functional-block behavior. Functional block entities are excluded. Arbitrary third-party machine compatibility is not guaranteed.

Keep the mod installed while a world contains multipart blocks; restore a pre-install backup before removing it. Existing items are valued by their current shape, without attempting to reconstruct historical conversion losses.

Independently implemented and MIT licensed. The original dependency mods are required and are not redistributed.

Combined pieces render with chunk terrain at block render distance. Existing saved pieces keep their materials and automatically refresh lighting after their chunks load.
