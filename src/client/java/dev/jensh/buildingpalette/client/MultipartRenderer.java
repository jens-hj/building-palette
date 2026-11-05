package dev.jensh.buildingpalette.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.mixin.client.BlockModelStateAccessor;
import java.util.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Crop each parent face to its octant, preserving vanilla texel density. */
public final class MultipartRenderer implements BlockEntityRenderer<MultipartEntity, MultipartRenderer.State> {
    public MultipartRenderer(BlockEntityRendererProvider.Context context) {}
    public static final class State extends BlockEntityRenderState { final List<BlockModelRenderState> cells = new ArrayList<>(); public int materialCount() { return cells.size(); } }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(MultipartEntity entity, State state, float ticks, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay progress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, ticks, camera, progress);
        state.cells.clear();
        for (int i = 0; i < 8; i++) {
            String material = entity.cells().at(i);
            if (material == null) continue;
            BlockModelRenderState model = new BlockModelRenderState();
            var worldModel = net.minecraft.client.Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(MultipartBlock.material(material));
            List<BlockStateModelPart> source = new ArrayList<>();
            worldModel.collectParts(net.minecraft.util.RandomSource.create(entity.getBlockPos().asLong()), source);
            model.setupModel(new org.joml.Matrix4f(), worldModel.hasMaterialFlag(1));
            var access = (BlockModelStateAccessor)model;
            int visible = 0;
            for (Direction direction : Direction.values()) {
                int x = (i & 1) + direction.getStepX(), y = ((i >> 1) & 1) + direction.getStepY(), z = ((i >> 2) & 1) + direction.getStepZ();
                boolean outside = x < 0 || x > 1 || y < 0 || y > 1 || z < 0 || z > 1;
                if (outside || entity.cells().at(x | y << 1 | z << 2) == null) visible |= 1 << direction.ordinal();
            }
            List<BlockStateModelPart> parts = new ArrayList<>();
            for (BlockStateModelPart part : source) parts.add(new CroppedPart(part, i, visible));
            access.palette$setParts(parts);
            state.cells.add(model);
        }
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        for (BlockModelRenderState model : state.cells) model.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
    }
    public record CroppedPart(BlockStateModelPart source, List<BakedQuad> quads) implements BlockStateModelPart {
        CroppedPart(BlockStateModelPart source, int octant, int visible) { this(source, crop(source, octant, visible)); }
        @Override public List<BakedQuad> getQuads(Direction direction) { return direction == null ? quads : List.of(); }
        @Override public boolean useAmbientOcclusion() { return source.useAmbientOcclusion(); }
        @Override public Material.Baked particleMaterial() { return source.particleMaterial(); }
        @Override public int materialFlags() { return source.materialFlags(); }
        private static List<BakedQuad> crop(BlockStateModelPart part, int octant, int visible) {
            List<BakedQuad> all = new ArrayList<>(part.getQuads(null));
            for (Direction dir : Direction.values()) all.addAll(part.getQuads(dir));
            return cropQuads(all, octant, visible);
        }
        public static List<BakedQuad> cropQuads(List<BakedQuad> all, int octant, int visible) {
            List<BakedQuad> result = new ArrayList<>();
            for (BakedQuad q : all) {
                if ((visible & 1 << q.direction().ordinal()) == 0) continue;
                Vector3f[] p = new Vector3f[4]; long[] uv = new long[4];
                Vector3f e1 = new Vector3f(q.position1()).sub(q.position0()), e3 = new Vector3f(q.position3()).sub(q.position0());
                for (int v = 0; v < 4; v++) {
                    p[v] = new Vector3f(q.position(v)).mul(.5f).add((octant & 1) * .5f, ((octant >> 1) & 1) * .5f, ((octant >> 2) & 1) * .5f);
                    Vector3f delta = new Vector3f(p[v]).sub(q.position0());
                    float a = delta.dot(e1) / e1.lengthSquared(), b = delta.dot(e3) / e3.lengthSquared();
                    float u = low(q.packedUV0()) + a * (low(q.packedUV1()) - low(q.packedUV0())) + b * (low(q.packedUV3()) - low(q.packedUV0()));
                    float t = high(q.packedUV0()) + a * (high(q.packedUV1()) - high(q.packedUV0())) + b * (high(q.packedUV3()) - high(q.packedUV0()));
                    uv[v] = Integer.toUnsignedLong(Float.floatToRawIntBits(u)) | (long)Float.floatToRawIntBits(t) << 32;
                }
                result.add(new BakedQuad(p[0], p[1], p[2], p[3], uv[0], uv[1], uv[2], uv[3], q.direction(), q.materialInfo()));
            }
            return result;
        }
        private static float low(long packed) { return Float.intBitsToFloat((int)packed); }
        private static float high(long packed) { return Float.intBitsToFloat((int)(packed >> 32)); }
    }
}
