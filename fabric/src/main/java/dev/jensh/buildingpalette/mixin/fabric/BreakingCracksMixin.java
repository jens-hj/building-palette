package dev.jensh.buildingpalette.mixin.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.client.FabricMultipartModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cracks cover only the material being mined: the aimed, connected cells of a combined block. */
@Mixin(BlockRenderDispatcher.class)
public abstract class BreakingCracksMixin {
    @Inject(method = "renderBreakingTexture", at = @At("HEAD"))
    private void palette$aimedCells(BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack pose, VertexConsumer consumer, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!state.is(BuildingPalette.MULTIPART.get()) || mc.level == null || !(mc.level.getBlockEntity(pos) instanceof MultipartEntity entity)
                || !(mc.hitResult instanceof BlockHitResult hit) || !hit.getBlockPos().equals(pos)) return;
        int aimed = entity.cells().connected(Placement.hitCell(hit, false));
        if (aimed != 0) FabricMultipartModel.BREAKING.set(entity.cells().remove(~aimed & 255));
    }
    @Inject(method = "renderBreakingTexture", at = @At("RETURN"))
    private void palette$allCells(CallbackInfo ci) { FabricMultipartModel.BREAKING.remove(); }
}
