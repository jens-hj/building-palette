package dev.jensh.buildingpalette.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.client.MultipartGeometry;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.dispatch.*;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The crack overlay draws a block's model parts. Carriers build their geometry from the block
 * entity instead, so supply the cells being mined (the aimed connected material, else all). */
@Mixin(LevelRenderer.class)
public abstract class BreakingCracksMixin {
    @WrapOperation(method = "submitBlockDestroyAnimation", require = 0, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;collectParts(Lnet/minecraft/util/RandomSource;Ljava/util/List;)V"))
    private void palette$carrierCracks(BlockStateModel model, RandomSource random, List<BlockStateModelPart> parts, Operation<Void> original,
            @Local BlockBreakingRenderState state) {
        original.call(model, random, parts);
        palette$cells(state, parts);
    }
    /** NeoForge calls its level-aware overload, but with an empty level and position zero. */
    @WrapOperation(method = "submitBlockDestroyAnimation", require = 0, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;collectParts(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;Ljava/util/List;)V"))
    private void palette$carrierCracksNeo(BlockStateModel model, net.minecraft.client.renderer.block.BlockAndTintGetter level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState blockState, RandomSource random, List<BlockStateModelPart> parts, Operation<Void> original,
            @Local BlockBreakingRenderState state) {
        original.call(model, level, pos, blockState, random, parts);
        palette$cells(state, parts);
    }
    private static void palette$cells(BlockBreakingRenderState state, List<BlockStateModelPart> parts) {
        Minecraft mc = Minecraft.getInstance();
        if (!parts.isEmpty() || mc.level == null || !(mc.level.getBlockEntity(state.blockPos()) instanceof MultipartEntity entity)) return;
        int mask = 255;
        if (mc.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(state.blockPos())) {
            int aimed = entity.cells().connected(Placement.hitCell(hit, false));
            if (aimed != 0) mask = aimed;
        }
        MultipartGeometry.cellParts(entity.cells(), mask, state.blockPos(), parts);
    }
}
