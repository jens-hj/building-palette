package dev.jensh.buildingpalette.mixin.client;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.core.Cells;
import dev.jensh.buildingpalette.client.MultipartGeometry;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
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
    @WrapMethod(method = "appendItemLayers")
    private void palette$materialIcon(ItemStackRenderState output, ItemStack stack, ItemDisplayContext context,
            Level level, ItemOwner owner, int seed, Operation<Void> originalCall) {
        var selection = Materials.get(stack);
        ItemStateAccessor accessor = (ItemStateAccessor)output;
        int start = accessor.palette$getCount();
        if (stack.getItem() != BuildingPalette.PIECE.get() || selection == null) {
            originalCall.call(output, stack, context, level, owner, seed);
            if (selection != null && selection.shape().isMultipart() && selection.shape().cost < 8) {
                output.appendModelIdentityElement(selection);
                palette$center(accessor, start);
            }
            return;
        }
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
                result.addAll(MultipartGeometry.CroppedPart.cropQuads(original, octant, visible));
            }
            state.setQuads(ItemQuads.split(result));
            state.setExtents(() -> result.stream().flatMap(q -> java.util.stream.IntStream.range(0, 4).mapToObj(q::position)).toArray(org.joml.Vector3fc[]::new));
        }
        palette$center(accessor, start);
    }
    private static void palette$center(ItemStateAccessor accessor, int start) {
        org.joml.Vector3f min = new org.joml.Vector3f(Float.POSITIVE_INFINITY), max = new org.joml.Vector3f(Float.NEGATIVE_INFINITY);
        for (int layer = start; layer < accessor.palette$getCount(); layer++)
            for (var quad : ((ItemLayerAccessor)accessor.palette$getLayers()[layer]).palette$getQuads().all())
                for (int vertex = 0; vertex < 4; vertex++) { min.min(quad.position(vertex)); max.max(quad.position(vertex)); }
        if (!Float.isFinite(min.x)) return;
        var offset = new org.joml.Vector3f(.5f).sub(new org.joml.Vector3f(min).add(max).mul(.5f));
        if (offset.lengthSquared() < .0000001f) return;
        for (int layer = start; layer < accessor.palette$getCount(); layer++) {
            var state = accessor.palette$getLayers()[layer];
            List<BakedQuad> centered = new ArrayList<>();
            for (var quad : ((ItemLayerAccessor)state).palette$getQuads().all()) centered.add(new BakedQuad(
                new org.joml.Vector3f(quad.position0()).add(offset), new org.joml.Vector3f(quad.position1()).add(offset),
                new org.joml.Vector3f(quad.position2()).add(offset), new org.joml.Vector3f(quad.position3()).add(offset),
                quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(), quad.materialInfo()));
            state.setQuads(ItemQuads.split(centered));
            state.setExtents(() -> centered.stream().flatMap(q -> java.util.stream.IntStream.range(0, 4).mapToObj(q::position)).toArray(org.joml.Vector3fc[]::new));
        }
    }
}
