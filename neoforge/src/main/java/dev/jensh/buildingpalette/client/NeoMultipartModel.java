package dev.jensh.buildingpalette.client;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.core.Cells;
import dev.jensh.buildingpalette.neoforge.NeoMultipartEntity;
import java.util.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.*;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;

/** Chunk-meshed geometry; never reads live block entities on meshing threads. */
public final class NeoMultipartModel implements DynamicBlockStateModel {
    private final BlockStateModel fallback;
    public NeoMultipartModel(BlockStateModel fallback) { this.fallback = fallback; }
    public static Cells cells(BlockAndTintGetter level, BlockPos pos) {
        var data = level.getModelData(pos).get(NeoMultipartEntity.CELLS);
        return data == null ? new Cells() : data;
    }
    private record Key(List<String> cells, long seed) {}
    @Override public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return new Key(Arrays.asList(cells(level, pos).copy()), pos.asLong());
    }
    public record Tints(Map<String, Integer> offsets, IntArrayList colors) {}
    public static Tints tints(Cells cells, BlockAndTintGetter level, BlockPos pos) {
        var offsets = new LinkedHashMap<String, Integer>(); var colors = new IntArrayList();
        var registry = Minecraft.getInstance().getBlockColors();
        for (String material : cells.copy()) if (material != null && !offsets.containsKey(material)) {
            offsets.put(material, colors.size());
            var source = MultipartBlock.material(material);
            var sources = registry.getTintSources(source);
            if (!sources.isEmpty()) for (var tint : sources) colors.add(tint.colorInWorld(source, level, pos) | 0xff000000);
            else {
                int before = colors.size();
                IClientBlockExtensions.of(source).collectDynamicTintValues(source, level, pos, colors);
                if (colors.size() == before) colors.add(-1);
            }
        }
        return new Tints(offsets, colors);
    }
    @Override public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> output) {
        Cells cells = cells(level, pos);
        var tints = tints(cells, level, pos);
        var models = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        for (int i = 0; i < 8; i++) {
            String material = cells.at(i); if (material == null) continue;
            var source = MultipartBlock.material(material);
            var parts = new ArrayList<BlockStateModelPart>();
            models.get(source).collectParts(level, pos, source, RandomSource.create(source.getSeed(pos)), parts);
            int visible = 0;
            for (Direction direction : Direction.values()) {
                int x = (i & 1) + direction.getStepX(), y = ((i >> 1) & 1) + direction.getStepY(), z = ((i >> 2) & 1) + direction.getStepZ();
                if (x < 0 || x > 1 || y < 0 || y > 1 || z < 0 || z > 1 || cells.at(x | y << 1 | z << 2) == null) visible |= 1 << direction.ordinal();
            }
            for (var part : parts) {
                var cropped = new MultipartGeometry.CroppedPart(part, i, visible);
                var quads = new ArrayList<BakedQuad>();
                for (var quad : cropped.quads()) {
                    var info = quad.materialInfo();
                    if (info.isTinted()) info = new BakedQuad.MaterialInfo(info.sprite(), info.layer(), info.itemRenderType(), info.itemGlintRenderType(), info.itemGlintSpecialRenderType(),
                        tints.offsets.get(material) + info.tintIndex(), info.shadeDirectionOverride(), info.lightEmission());
                    quads.add(new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(), quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(), info));
                }
                output.add(new MultipartGeometry.CroppedPart(part, quads));
            }
        }
    }
    @Override public Material.Baked particleMaterial() { return fallback.particleMaterial(); }
    @Override public int materialFlags() { return 0; }
    @Override public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        int flags = 0;
        for (String material : cells(level, pos).copy()) if (material != null) flags |= Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(MultipartBlock.material(material)).materialFlags();
        return flags;
    }
}
