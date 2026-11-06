package dev.jensh.buildingpalette.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.client.PaletteConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(LevelRenderer.class)
public abstract class OutlineMixin {
    @Inject(method = "submitBlockOutline", at = @At("HEAD"))
    private void palette$preview(PoseStack pose, SubmitNodeCollector collector, LevelRenderState state, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !(mc.hitResult instanceof BlockHitResult hit)
                || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return;
        var outline = state.blockOutlineRenderState;
        if (outline != null && outline.pos().equals(hit.getBlockPos())
                && mc.level.getBlockEntity(hit.getBlockPos()) instanceof MultipartEntity entity) {
            int mask = entity.cells().connected(Placement.hitCell(hit, false));
            // Keep vanilla colour, opacity, line width, transparency handling,
            // and accessibility passes. Only select the aimed material's shape.
            state.blockOutlineRenderState = new BlockOutlineRenderState(outline.pos(), outline.isTranslucent(), outline.highContrast(),
                MultipartBlock.shape(entity.cells(), mask), outline.collisionShape(), outline.occlusionShape(), outline.interactionShape());
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
    @ModifyArgs(method = "submitBlockOutline", at = @At(value = "INVOKE", ordinal = 1,
        target = "Lnet/minecraft/client/renderer/LevelRenderer;submitHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/state/level/BlockOutlineRenderState;IFZ)V"))
    private void palette$optionalHighlight(Args args) {
        var mc = Minecraft.getInstance();
        BlockOutlineRenderState outline = args.get(3);
        if (PaletteConfig.highlightParts && mc.level != null && mc.level.getBlockEntity(outline.pos()) instanceof MultipartEntity)
            args.set(4, PaletteConfig.outlineColor());
    }
}
