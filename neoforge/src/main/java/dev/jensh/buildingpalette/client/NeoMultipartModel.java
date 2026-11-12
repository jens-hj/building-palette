package dev.jensh.buildingpalette.client;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.core.Cells;
import dev.jensh.buildingpalette.neoforge.NeoMultipartEntity;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Chunk-meshed geometry from model data; never reads live block entities on meshing threads. */
public final class NeoMultipartModel implements IDynamicBakedModel {
    private final BakedModel fallback;
    public NeoMultipartModel(BakedModel fallback) { this.fallback = fallback; }
    /** The cells at pos as the renderer sees them, also off the main thread. */
    public static Cells cells(BlockAndTintGetter level, BlockPos pos) {
        Cells data = level.getModelData(pos).get(NeoMultipartEntity.CELLS);
        if (data != null) return data;
        return level.getBlockEntity(pos) instanceof MultipartEntity entity ? entity.cells() : null;
    }
    public static int tint(BlockState state, BlockAndTintGetter level, BlockPos pos, int index) {
        if (level == null || pos == null || index < 0) return -1;
        Cells cells = cells(level, pos);
        int octant = index / MultipartGeometry.TINT_STRIDE;
        if (cells == null || octant > 7 || cells.at(octant) == null) return -1;
        return Minecraft.getInstance().getBlockColors().getColor(MultipartBlock.material(cells.at(octant)), level, pos, index % MultipartGeometry.TINT_STRIDE);
    }
    @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType renderType) {
        Cells cells = data.get(NeoMultipartEntity.CELLS);
        if (side != null || cells == null) return List.of();
        BlockPos pos = data.get(NeoMultipartEntity.POS);
        var models = Minecraft.getInstance().getBlockRenderer();
        List<BakedQuad> result = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String material = cells.at(i);
            if (material == null) continue;
            BlockState source = MultipartBlock.material(material);
            BakedModel model = models.getBlockModel(source);
            long seed = pos == null ? 42 : source.getSeed(pos);
            random.setSeed(seed);
            if (renderType != null && !model.getRenderTypes(source, random, ModelData.EMPTY).contains(renderType)) continue;
            List<BakedQuad> all = new ArrayList<>();
            random.setSeed(seed);
            all.addAll(model.getQuads(source, null, random, ModelData.EMPTY, renderType));
            for (Direction direction : Direction.values()) {
                random.setSeed(seed);
                all.addAll(model.getQuads(source, direction, random, ModelData.EMPTY, renderType));
            }
            MultipartGeometry.crop(all, i, MultipartGeometry.visible(cell -> cells.at(cell) != null, i), i * MultipartGeometry.TINT_STRIDE, result);
        }
        return result;
    }
    @Override public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
        Cells cells = data.get(NeoMultipartEntity.CELLS);
        if (cells == null) return ChunkRenderTypeSet.none();
        var models = Minecraft.getInstance().getBlockRenderer();
        List<ChunkRenderTypeSet> sets = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String material : cells.copy()) if (material != null && seen.add(material)) {
            BlockState source = MultipartBlock.material(material);
            sets.add(models.getBlockModel(source).getRenderTypes(source, random, ModelData.EMPTY));
        }
        return ChunkRenderTypeSet.union(sets);
    }
    @Override public TextureAtlasSprite getParticleIcon(ModelData data) {
        Cells cells = data.get(NeoMultipartEntity.CELLS);
        if (cells != null) for (String material : cells.copy()) if (material != null)
            return Minecraft.getInstance().getBlockRenderer().getBlockModel(MultipartBlock.material(material)).getParticleIcon(ModelData.EMPTY);
        return fallback.getParticleIcon();
    }
    @Override public boolean useAmbientOcclusion() { return true; }
    @Override public boolean isGui3d() { return false; }
    @Override public boolean usesBlockLight() { return true; }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleIcon() { return fallback.getParticleIcon(); }
    @Override public ItemTransforms getTransforms() { return fallback.getTransforms(); }
    @Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
}
