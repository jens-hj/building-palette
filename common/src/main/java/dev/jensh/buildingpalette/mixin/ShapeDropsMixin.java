package dev.jensh.buildingpalette.mixin;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.core.Shape;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Clutter No More rewrites shape drops at RETURN of Block.getDrops. Cancelling at HEAD bypasses it for
// palette-managed shapes and hands back exact multipart material.
@Mixin(Block.class)
public abstract class ShapeDropsMixin {
    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)Ljava/util/List;",
            at = @At("HEAD"), cancellable = true)
    private static void palette$exactDrops(BlockState state, ServerLevel level, BlockPos pos, BlockEntity entity,
            CallbackInfoReturnable<List<ItemStack>> cir) {
        palette$exactDrops(state, level, pos, entity, null, ItemStack.EMPTY, cir);
    }

    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;",
            at = @At("HEAD"), cancellable = true)
    private static void palette$exactDrops(BlockState state, ServerLevel level, BlockPos pos, BlockEntity entity,
            Entity breaker, ItemInstance tool, CallbackInfoReturnable<List<ItemStack>> cir) {
        var selection = Materials.get(new ItemStack(state.getBlock().asItem()));
        if (selection == null || selection.shape() == Shape.BLOCK) return;
        var original = palette$vanillaDrops(state, level, pos, entity, breaker, tool);
        // Clutter No More shapes (steps, vertical slabs) have no loot table of their own; CNM
        // derives their drops from the parent at RETURN, which this HEAD cancel skips. Ask the
        // parent's loot whether the piece drops (tool requirements, etc.).
        if (original.isEmpty() && selection.shape().isMultipart())
            original = palette$vanillaDrops(Block.byItem(selection.parent()).defaultBlockState(), level, pos, entity, breaker, tool);
        if (!selection.shape().isMultipart() || original.isEmpty()) { cir.setReturnValue(original); return; }
        // During vanilla destruction the block may already be air, and Offgrid moves shapes at
        // positions with an offset. Native shape geometry depends only on the state, so sample that.
        int units = 0;
        var boxes = state.getShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs();
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) * .5 + .25, y = ((i >> 1) & 1) * .5 + .25, z = ((i >> 2) & 1) * .5 + .25;
            if (boxes.stream().anyMatch(box -> box.contains(x, y, z))) units++;
        }
        // Vanilla auto-connected inner/outer stairs still cost and drop one stair.
        if (state.getBlock() instanceof net.minecraft.world.level.block.StairBlock) units = Shape.STAIR.cost;
        cir.setReturnValue(units == 0 ? List.of() : List.of(Materials.piece(selection.material(), Shape.CORNER, units)));
    }

    private static List<ItemStack> palette$vanillaDrops(BlockState state, ServerLevel level, BlockPos pos,
            BlockEntity entity, Entity breaker, ItemInstance tool) {
        var params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, tool)
                .withOptionalParameter(LootContextParams.THIS_ENTITY, breaker)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, entity);
        return state.getDrops(params);
    }
}
