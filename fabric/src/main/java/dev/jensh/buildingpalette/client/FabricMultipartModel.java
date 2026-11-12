package dev.jensh.buildingpalette.client;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.core.Cells;
import java.util.*;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.*;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Chunk-meshed geometry from block entity render data; never reads live block entities on meshing threads. */
public final class FabricMultipartModel implements BakedModel {
    /** While the breaking overlay is drawn, the cells being mined; set on the render thread only. */
    public static final ThreadLocal<Cells> BREAKING = new ThreadLocal<>();
    private final BakedModel fallback;
    public FabricMultipartModel(BakedModel fallback) { this.fallback = fallback; }
    /** The cells at pos as the renderer sees them, also off the main thread. */
    public static Cells cells(BlockAndTintGetter level, BlockPos pos) {
        if (level.getBlockEntityRenderData(pos) instanceof Cells data) return data;
        return level.getBlockEntity(pos) instanceof MultipartEntity entity ? entity.cells() : null;
    }
    public static int tint(BlockState state, BlockAndTintGetter level, BlockPos pos, int index) {
        if (level == null || pos == null || index < 0) return -1;
        Cells cells = cells(level, pos);
        int octant = index / MultipartGeometry.TINT_STRIDE;
        if (cells == null || octant > 7 || cells.at(octant) == null) return -1;
        return Minecraft.getInstance().getBlockColors().getColor(MultipartBlock.material(cells.at(octant)), level, pos, index % MultipartGeometry.TINT_STRIDE);
    }
    @Override public boolean isVanillaAdapter() { return false; }
    @Override public void emitBlockQuads(BlockAndTintGetter level, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
        Cells breaking = BREAKING.get();
        Cells cells = breaking != null ? breaking : cells(level, pos);
        var renderer = RendererAccess.INSTANCE.getRenderer();
        if (cells == null || renderer == null) return;
        var models = Minecraft.getInstance().getBlockRenderer();
        var emitter = context.getEmitter();
        RandomSource random = randomSupplier.get();
        List<BakedQuad> all = new ArrayList<>(), cropped = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String material = cells.at(i);
            if (material == null) continue;
            BlockState source = MultipartBlock.material(material);
            BakedModel model = models.getBlockModel(source);
            // All octants of a material share its model variant, so cropping leaves no seams.
            long seed = source.getSeed(pos);
            all.clear(); cropped.clear();
            random.setSeed(seed);
            all.addAll(model.getQuads(source, null, random));
            for (Direction direction : Direction.values()) {
                random.setSeed(seed);
                all.addAll(model.getQuads(source, direction, random));
            }
            // A neighbouring cell hides a face only if it is the same material or opaque: glass must not hide stone.
            MultipartGeometry.crop(all, i, MultipartGeometry.visible(cell -> cells.at(cell) != null
                && (cells.at(cell).equals(material) || MultipartBlock.material(cells.at(cell)).isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)), i),
                i * MultipartGeometry.TINT_STRIDE, cropped);
            RenderMaterial layer = renderer.materialFinder()
                .blendMode(BlendMode.fromRenderLayer(ItemBlockRenderTypes.getChunkRenderType(source)))
                .ambientOcclusion(model.useAmbientOcclusion() ? TriState.DEFAULT : TriState.FALSE).find();
            for (BakedQuad quad : cropped) { emitter.fromVanilla(quad, layer, null); emitter.emit(); }
        }
    }
    @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) { return List.of(); }
    @Override public boolean useAmbientOcclusion() { return true; }
    @Override public boolean isGui3d() { return false; }
    @Override public boolean usesBlockLight() { return true; }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleIcon() { return fallback.getParticleIcon(); }
    @Override public ItemTransforms getTransforms() { return fallback.getTransforms(); }
    @Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
}
