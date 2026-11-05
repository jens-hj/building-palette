package dev.jensh.buildingpalette.mixin;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.core.Shape;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.tazer.clutternomore.shape.ShapeHooks", remap = false)
public abstract class ShapeDropsMixin {
    @Inject(method = "getDrops", at = @At("HEAD"), cancellable = true)
    private static void palette$exactDrops(List<ItemStack> original, BlockState state, ServerLevel level, BlockPos pos,
            BlockEntity entity, Entity breaker, ItemInstance tool, CallbackInfoReturnable<List<ItemStack>> cir) {
        var selection = Materials.get(new ItemStack(state.getBlock().asItem()));
        if (selection == null || selection.shape() == Shape.BLOCK) return;
        if (!selection.shape().isMultipart()) { cir.setReturnValue(original); return; }
        if (original.isEmpty()) { cir.setReturnValue(original); return; }
        // During vanilla destruction the block may already be air. Sample the supplied state directly.
        int units = 0;
        var boxes = state.getShape(level, pos).toAabbs();
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) * .5 + .25, y = ((i >> 1) & 1) * .5 + .25, z = ((i >> 2) & 1) * .5 + .25;
            if (boxes.stream().anyMatch(box -> box.contains(x, y, z))) units++;
        }
        // Vanilla auto-connected inner/outer stairs still cost and drop one stair.
        if (state.getBlock() instanceof net.minecraft.world.level.block.StairBlock) units = Shape.STAIR.cost;
        cir.setReturnValue(units == 0 ? List.of() : List.of(Materials.piece(selection.material(), Shape.CORNER, units)));
    }
}
