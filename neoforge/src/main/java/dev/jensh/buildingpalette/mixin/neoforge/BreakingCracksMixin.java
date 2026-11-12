package dev.jensh.buildingpalette.mixin.neoforge;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.core.Cells;
import dev.jensh.buildingpalette.neoforge.NeoMultipartEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Cracks cover only the material being mined: the aimed, connected cells of a combined block. */
@Mixin(BlockRenderDispatcher.class)
public abstract class BreakingCracksMixin {
    @ModifyVariable(method = "renderBreakingTexture(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/neoforged/neoforge/client/model/data/ModelData;)V",
        at = @At("HEAD"), argsOnly = true, require = 0)
    private ModelData palette$aimedCells(ModelData data, BlockState state, BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (!state.is(BuildingPalette.MULTIPART.get()) || mc.level == null || !(mc.level.getBlockEntity(pos) instanceof MultipartEntity entity)
                || !(mc.hitResult instanceof BlockHitResult hit) || !hit.getBlockPos().equals(pos)) return data;
        int aimed = entity.cells().connected(Placement.hitCell(hit, false));
        if (aimed == 0) return data;
        Cells cells = entity.cells().remove(~aimed & 255);
        return ModelData.builder().with(NeoMultipartEntity.CELLS, cells).with(NeoMultipartEntity.POS, pos).build();
    }
}
