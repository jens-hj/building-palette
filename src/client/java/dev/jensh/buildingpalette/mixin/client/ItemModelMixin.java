package dev.jensh.buildingpalette.mixin.client;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.core.Cells;
import dev.jensh.buildingpalette.client.MultipartRenderer;
import java.util.*;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.resources.model.geometry.*;
import net.minecraft.core.Direction;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelResolver.class)
public abstract class ItemModelMixin {
    @Inject(method = "appendItemLayers", at = @At("HEAD"), cancellable = true)
    private void palette$materialIcon(ItemStackRenderState output, ItemStack stack, ItemDisplayContext context,
            Level level, ItemOwner owner, int seed, CallbackInfo ci) {
        if (stack.getItem() != BuildingPalette.PIECE) return;
        var selection = Materials.get(stack);
        if (selection == null) return;
        ItemStateAccessor accessor = (ItemStateAccessor)output;
        int start = accessor.palette$getCount();
        ((ItemModelResolver)(Object)this).appendItemLayers(output, new ItemStack(selection.parent()), context, level, owner, seed);
        // Cropped geometry must not reuse the parent's GUI atlas entry or another shape's entry.
        output.appendModelIdentityElement(selection);
        int mask = Cells.mask(selection.shape(), 0, 0);
        for (int layer = start; layer < accessor.palette$getCount(); layer++) {
            var state = accessor.palette$getLayers()[layer];
            List<BakedQuad> original = ((ItemLayerAccessor)state).palette$getQuads().all(), result = new ArrayList<>();
            for (int octant = 0; octant < 8; octant++) if ((mask & 1 << octant) != 0) {
                int visible = 0;
                for (Direction d : Direction.values()) {
                    int x = (octant & 1) + d.getStepX(), y = ((octant >> 1) & 1) + d.getStepY(), z = ((octant >> 2) & 1) + d.getStepZ();
                    if (x < 0 || x > 1 || y < 0 || y > 1 || z < 0 || z > 1 || (mask & 1 << (x | y << 1 | z << 2)) == 0) visible |= 1 << d.ordinal();
                }
                result.addAll(MultipartRenderer.CroppedPart.cropQuads(original, octant, visible));
            }
            state.setQuads(ItemQuads.split(result));
            state.setExtents(() -> result.stream().flatMap(q -> java.util.stream.IntStream.range(0, 4).mapToObj(q::position)).toArray(org.joml.Vector3fc[]::new));
        }
        ci.cancel();
    }
}
