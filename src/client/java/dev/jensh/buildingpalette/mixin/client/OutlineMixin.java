package dev.jensh.buildingpalette.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.client.PaletteConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class OutlineMixin {
    @Inject(method = "submitBlockOutline", at = @At("HEAD"), cancellable = true)
    private void palette$preview(PoseStack pose, SubmitNodeCollector collector, LevelRenderState state, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !(mc.hitResult instanceof BlockHitResult hit)
                || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return;
        if (mc.level.getBlockEntity(hit.getBlockPos()) instanceof MultipartEntity entity) {
            int mask = entity.cells().connected(Placement.hitCell(hit, false));
            var camera = state.cameraRenderState.pos;
            pose.pushPose(); pose.translate(hit.getBlockPos().getX() - camera.x, hit.getBlockPos().getY() - camera.y, hit.getBlockPos().getZ() - camera.z);
            collector.submitShapeOutline(pose, MultipartBlock.shape(entity.cells(), mask), RenderTypes.linesTranslucent(), 0xffe9dca6, 2, false);
            pose.popPose(); ci.cancel();
        }
        if (!PaletteConfig.placementPreview) return;
        var plan = Placement.plan(mc.level, mc.player, mc.player.getMainHandItem(), hit);
        if (plan == null) return;
        var camera = state.cameraRenderState.pos;
        pose.pushPose(); pose.translate(plan.pos().getX() - camera.x, plan.pos().getY() - camera.y, plan.pos().getZ() - camera.z);
        var cells = plan.before().place(plan.mask(), plan.material());
        int color = MaterialStacks.available(mc.player.getMainHandItem()) > 0 || mc.player.hasInfiniteMaterials() ? 0xff8acba1 : 0xffe37474;
        collector.submitShapeOutline(pose, MultipartBlock.shape(cells, plan.mask()), RenderTypes.linesTranslucent(), color, 2, false);
        pose.popPose();
    }
}
