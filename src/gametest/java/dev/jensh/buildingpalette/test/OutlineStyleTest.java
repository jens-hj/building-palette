package dev.jensh.buildingpalette.test;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.client.PaletteConfig;
import dev.jensh.buildingpalette.test.mixin.LevelRendererInvoker;
import java.lang.reflect.Proxy;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.*;
import net.minecraft.core.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Capture actual vanilla render submissions rather than duplicating its colours. */
public final class OutlineStyleTest {
    private record Stroke(VoxelShape shape, net.minecraft.client.renderer.rendertype.RenderType renderType, int color, float width, boolean translucent) {}
    public static void verify(Minecraft mc) {
        var hit = mc.hitResult;
        boolean preview = PaletteConfig.placementPreview, highlight = PaletteConfig.highlightParts;
        int hue = PaletteConfig.outlineHue;
        try {
            PaletteConfig.placementPreview = false;
            var part = new BlockPos(0, 100, 0);
            var nativePos = new BlockPos(0, 99, 0);
            for (boolean highContrast : new boolean[]{false, true}) {
                PaletteConfig.highlightParts = false;
                var nativeStrokes = draw(mc, nativePos, highContrast);
                var normal = draw(mc, part, highContrast);
                require(normal.size() == (highContrast ? 2 : 1), "Vanilla accessibility outline pass count changed");
                sameStyle(nativeStrokes, normal);
                for (var stroke : normal) require(Math.abs(volume(stroke.shape) - .25) < .000001, "Outline must still select the aimed quarter material");
                PaletteConfig.highlightParts = true;
                PaletteConfig.outlineHue = 120;
                sameStyle(nativeStrokes, draw(mc, nativePos, highContrast));
                var green = draw(mc, part, highContrast);
                require(green.getLast().color == PaletteConfig.outlineColor() && green.getLast().color != normal.getLast().color, "Opt-in hue was not applied to multipart selection");
                require(sameRenderType(green.getLast().renderType, normal.getLast().renderType) && green.getLast().width == normal.getLast().width, "Highlight changed vanilla line rendering");
                if (highContrast) require(green.getFirst().color == nativeStrokes.getFirst().color, "High-contrast outer stroke changed");
                PaletteConfig.outlineHue = 240;
                var blue = draw(mc, part, highContrast);
                require(blue.getLast().color == PaletteConfig.outlineColor() && blue.getLast().color != green.getLast().color, "Changing hue did not change selection colour");
            }
            PaletteConfig.outlineHue = 0; int red = PaletteConfig.outlineColor();
            PaletteConfig.outlineHue = 360;
            require(PaletteConfig.outlineColor() == red, "Hue endpoint must wrap to red");
        } finally {
            mc.hitResult = hit; PaletteConfig.placementPreview = preview;
            PaletteConfig.highlightParts = highlight; PaletteConfig.outlineHue = hue;
        }
    }
    private static List<Stroke> draw(Minecraft mc, BlockPos pos, boolean highContrast) {
        var strokes = new ArrayList<Stroke>();
        SubmitNodeCollector collector = (SubmitNodeCollector)Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(), new Class<?>[]{SubmitNodeCollector.class}, (proxy, method, args) -> {
            if (method.getName().equals("submitShapeOutline")) strokes.add(new Stroke((VoxelShape)args[1], (net.minecraft.client.renderer.rendertype.RenderType)args[2], (Integer)args[3], (Float)args[4], (Boolean)args[5]));
            return null;
        });
        var state = new LevelRenderState();
        state.cameraRenderState = new CameraRenderState(); state.cameraRenderState.pos = mc.player.getEyePosition();
        VoxelShape shape = mc.level.getBlockEntity(pos) instanceof MultipartEntity entity ? MultipartBlock.shape(entity.cells(), 255) : Shapes.block();
        state.blockOutlineRenderState = new BlockOutlineRenderState(pos, false, highContrast, shape);
        mc.hitResult = new BlockHitResult(new Vec3(pos.getX() + .25, pos.getY() + .25, pos.getZ()), Direction.NORTH, pos, false);
        ((LevelRendererInvoker)mc.levelRenderer).palette$submitBlockOutline(new PoseStack(), collector, state);
        return strokes;
    }
    private static void sameStyle(List<Stroke> expected, List<Stroke> actual) {
        require(expected.size() == actual.size(), "Outline pass count differs from vanilla");
        for (int i = 0; i < expected.size(); i++) {
            var a = expected.get(i); var b = actual.get(i);
            require(a.color == b.color && sameRenderType(a.renderType, b.renderType) && a.width == b.width && a.translucent == b.translucent, "Default outline style differs from vanilla: native=" + a + "; multipart=" + b);
        }
    }
    private static boolean sameRenderType(net.minecraft.client.renderer.rendertype.RenderType a, net.minecraft.client.renderer.rendertype.RenderType b) {
        // Iris and other mods can return fresh wrappers for the same pipeline.
        var x = a.pipeline(); var y = b.pipeline();
        return x.getLocation().equals(y.getLocation()) && x.getColorTargetStates().equals(y.getColorTargetStates())
            && java.util.Objects.equals(x.getDepthStencilState(), y.getDepthStencilState())
            && x.getPolygonMode() == y.getPolygonMode() && x.isCull() == y.isCull()
            && a.hasBlending() == b.hasBlending() && a.primitiveTopology() == b.primitiveTopology();
    }
    private static double volume(VoxelShape shape) { return shape.toAabbs().stream().mapToDouble(b -> (b.maxX-b.minX)*(b.maxY-b.minY)*(b.maxZ-b.minZ)).sum(); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
