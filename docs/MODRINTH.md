# Building Palette

Build with slabs, stairs, quarter-volume steps, and eighth-volume corners while keeping your material in one inventory slot.

Hold **Alt** and scroll a held stack or an inventory stack to change its shape. A full stack of 64 blocks becomes 128 slabs, 256 steps, or 512 corners **in the same slot**. Stair leftovers remain stored and become usable when you select another shape.

Combine pieces on a half-block grid, including different materials in the same block space. Mine the aimed material to remove all face-connected pieces of that material within the block. Explicit stairs and corner pieces allow small details with ordinary Minecraft block textures.

Selected slabs remain slab ingredients, and planks remain plank ingredients. Fences and walls stay available in the palette with their normal behavior.

## Requirements

Client **and** server: Fabric 26.3, Java 25, Fabric API, Clutter No More 3.1.1+26.3, and Offgrid 1.0.4+26.3.

## Alpha limitations

Test in a copied instance and disposable world first. Multipart blocks cannot be pushed by pistons and do not reproduce ticking/functional-block behavior. Functional block entities are excluded. Arbitrary third-party machine compatibility is not guaranteed.

Keep the mod installed while a world contains multipart blocks; restore a pre-install backup before removing it. Existing items are valued by their current shape, without attempting to reconstruct historical conversion losses.

Independently implemented and MIT licensed. The original dependency mods are required and are not redistributed.
