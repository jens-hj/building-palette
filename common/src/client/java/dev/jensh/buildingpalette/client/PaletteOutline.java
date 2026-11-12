package dev.jensh.buildingpalette.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jensh.buildingpalette.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PaletteOutline {
    private PaletteOutline() {}
    /** Draws the placement preview and, for combined blocks, the aimed material's outline.
     * Returns true when the vanilla outline is replaced. */
    public static boolean render(PoseStack pose, MultiBufferSource buffers, Vec3 camera, BlockHitResult hit) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        boolean replaced = false;
        if (mc.level.getBlockEntity(hit.getBlockPos()) instanceof MultipartEntity entity) {
            int mask = entity.cells().connected(Placement.hitCell(hit, false));
            if (mask != 0) {
                // Vanilla colour and opacity unless highlighting is enabled; only the shape changes.
                int color = PaletteConfig.highlightParts ? PaletteConfig.outlineColor() : 0x66000000;
                shape(pose, lines, MultipartBlock.shape(entity.cells(), mask), hit.getBlockPos(), camera, color);
                replaced = true;
            }
        }
        if (PaletteConfig.placementPreview) {
            var plan = Placement.plan(mc.level, mc.player, mc.player.getMainHandItem(), hit);
            if (plan != null) {
                var cells = plan.before().place(plan.mask(), plan.material());
                int color = MaterialStacks.available(mc.player.getMainHandItem()) > 0 || mc.player.hasInfiniteMaterials() ? 0xff8acba1 : 0xffe37474;
                shape(pose, lines, MultipartBlock.shape(cells, plan.mask()), plan.pos(), camera, color);
            }
        }
        return replaced;
    }
    private static void shape(PoseStack pose, VertexConsumer lines, VoxelShape shape, BlockPos pos, Vec3 camera, int argb) {
        var last = pose.last();
        float x = (float)(pos.getX() - camera.x), y = (float)(pos.getY() - camera.y), z = (float)(pos.getZ() - camera.z);
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            float dx = (float)(x2 - x1), dy = (float)(y2 - y1), dz = (float)(z2 - z1);
            float length = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= length; dy /= length; dz /= length;
            lines.addVertex(last, (float)x1 + x, (float)y1 + y, (float)z1 + z).setColor(argb).setNormal(last, dx, dy, dz);
            lines.addVertex(last, (float)x2 + x, (float)y2 + y, (float)z2 + z).setColor(argb).setNormal(last, dx, dy, dz);
        });
    }
}
