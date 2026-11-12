package dev.jensh.buildingpalette.client;

import java.util.*;
import java.util.function.IntPredicate;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;

/** Crop each parent face to its octant, preserving vanilla texel density. */
public final class MultipartGeometry {
    /** Tint indices of a cell's quads are offset by its octant times this, so one colour lookup can find the material. */
    public static final int TINT_STRIDE = 1024;
    private static final int POSITION = 0, UV = 4;
    private MultipartGeometry() {}
    /** Faces of an octant that are not covered by another occupied octant of the same block. */
    public static int visible(IntPredicate occupied, int octant) {
        int visible = 0;
        for (Direction direction : Direction.values()) {
            int x = (octant & 1) + direction.getStepX(), y = ((octant >> 1) & 1) + direction.getStepY(), z = ((octant >> 2) & 1) + direction.getStepZ();
            if (x < 0 || x > 1 || y < 0 || y > 1 || z < 0 || z > 1 || !occupied.test(x | y << 1 | z << 2)) visible |= 1 << direction.ordinal();
        }
        return visible;
    }
    public static void crop(List<BakedQuad> all, int octant, int visible, int tintBase, List<BakedQuad> out) {
        float ox = (octant & 1) * .5f, oy = ((octant >> 1) & 1) * .5f, oz = ((octant >> 2) & 1) * .5f;
        for (BakedQuad q : all) {
            if ((visible & 1 << q.getDirection().ordinal()) == 0) continue;
            int[] source = q.getVertices(), data = source.clone();
            int stride = source.length / 4;
            float[] p0 = position(source, 0), e1 = minus(position(source, stride), p0), e3 = minus(position(source, 3 * stride), p0);
            float l1 = dot(e1, e1), l3 = dot(e3, e3);
            if (l1 < 1.0E-12f || l3 < 1.0E-12f) continue;
            float u0 = uv(source, 0, 0), v0 = uv(source, 0, 1);
            float u1 = uv(source, stride, 0) - u0, v1 = uv(source, stride, 1) - v0, u3 = uv(source, 3 * stride, 0) - u0, v3 = uv(source, 3 * stride, 1) - v0;
            for (int vertex = 0; vertex < 4; vertex++) {
                int at = vertex * stride;
                float[] p = position(source, at);
                p[0] = p[0] * .5f + ox; p[1] = p[1] * .5f + oy; p[2] = p[2] * .5f + oz;
                float[] delta = minus(p, p0);
                float a = dot(delta, e1) / l1, b = dot(delta, e3) / l3;
                for (int axis = 0; axis < 3; axis++) data[at + POSITION + axis] = Float.floatToRawIntBits(p[axis]);
                data[at + UV] = Float.floatToRawIntBits(u0 + a * u1 + b * u3);
                data[at + UV + 1] = Float.floatToRawIntBits(v0 + a * v1 + b * v3);
            }
            out.add(new BakedQuad(data, q.isTinted() ? tintBase + q.getTintIndex() : -1, q.getDirection(), q.getSprite(), q.isShade()));
        }
    }
    /** Move quads so their bounds are centred in the block, for item icons of small pieces. */
    public static List<BakedQuad> centered(List<BakedQuad> quads) {
        float[] min = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY}, max = {Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
        for (BakedQuad q : quads) for (int vertex = 0; vertex < 4; vertex++) {
            float[] p = position(q.getVertices(), vertex * (q.getVertices().length / 4));
            for (int axis = 0; axis < 3; axis++) { min[axis] = Math.min(min[axis], p[axis]); max[axis] = Math.max(max[axis], p[axis]); }
        }
        if (!Float.isFinite(min[0])) return quads;
        float[] offset = new float[3];
        for (int axis = 0; axis < 3; axis++) offset[axis] = .5f - (min[axis] + max[axis]) / 2;
        if (dot(offset, offset) < 1.0E-7f) return quads;
        List<BakedQuad> result = new ArrayList<>(quads.size());
        for (BakedQuad q : quads) {
            int[] data = q.getVertices().clone();
            int stride = data.length / 4;
            for (int vertex = 0; vertex < 4; vertex++) for (int axis = 0; axis < 3; axis++) {
                int at = vertex * stride + POSITION + axis;
                data[at] = Float.floatToRawIntBits(Float.intBitsToFloat(data[at]) + offset[axis]);
            }
            result.add(new BakedQuad(data, q.getTintIndex(), q.getDirection(), q.getSprite(), q.isShade()));
        }
        return result;
    }
    private static float[] position(int[] data, int at) {
        return new float[]{Float.intBitsToFloat(data[at + POSITION]), Float.intBitsToFloat(data[at + POSITION + 1]), Float.intBitsToFloat(data[at + POSITION + 2])};
    }
    private static float uv(int[] data, int at, int component) { return Float.intBitsToFloat(data[at + UV + component]); }
    private static float[] minus(float[] a, float[] b) { return new float[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]}; }
    private static float dot(float[] a, float[] b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }
}
