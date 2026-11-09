package dev.jensh.buildingpalette.client;

import java.util.*;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

/** Crop each parent face to its octant, preserving vanilla texel density. */
public final class MultipartGeometry {
    private MultipartGeometry() {}
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
