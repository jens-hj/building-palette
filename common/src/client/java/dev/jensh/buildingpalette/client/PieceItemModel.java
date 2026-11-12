package dev.jensh.buildingpalette.client;

import dev.jensh.buildingpalette.Materials;
import dev.jensh.buildingpalette.core.Cells;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** A piece without a native item shows its material's own model, cropped to the piece's shape. */
public final class PieceItemModel implements BakedModel {
    private final BakedModel original;
    private final Map<Materials.Selection, BakedModel> cache = new ConcurrentHashMap<>();
    private final ItemOverrides overrides;
    public interface Resolver { BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel level, LivingEntity entity, int seed); }
    public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
        var selection = Materials.get(stack);
        if (selection == null) return model;
        return cache.computeIfAbsent(selection, key -> {
            BakedModel parent = Minecraft.getInstance().getItemRenderer().getModel(new ItemStack(key.parent()), level, entity, seed);
            return parent.isCustomRenderer() ? model : new Cropped(parent, Cells.mask(key.shape(), 0, 0));
        });
    }
    /** The loader builds the overrides: their empty constructor is not accessible everywhere. */
    public PieceItemModel(BakedModel original, Function<Resolver, ItemOverrides> overrides) { this.original = original; this.overrides = overrides.apply(this::resolve); }
    @Override public ItemOverrides getOverrides() { return overrides; }
    @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) { return original.getQuads(state, side, random); }
    @Override public boolean useAmbientOcclusion() { return original.useAmbientOcclusion(); }
    @Override public boolean isGui3d() { return original.isGui3d(); }
    @Override public boolean usesBlockLight() { return original.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return original.isCustomRenderer(); }
    @Override public TextureAtlasSprite getParticleIcon() { return original.getParticleIcon(); }
    @Override public ItemTransforms getTransforms() { return original.getTransforms(); }

    private static final class Cropped implements BakedModel {
        private final BakedModel parent;
        private final List<BakedQuad> quads;
        Cropped(BakedModel parent, int mask) {
            this.parent = parent;
            RandomSource random = RandomSource.create(42);
            List<BakedQuad> all = new ArrayList<>(parent.getQuads(null, null, random)), cropped = new ArrayList<>();
            for (Direction direction : Direction.values()) all.addAll(parent.getQuads(null, direction, random));
            for (int octant = 0; octant < 8; octant++) if ((mask & 1 << octant) != 0)
                MultipartGeometry.crop(all, octant, MultipartGeometry.visible(cell -> (mask & 1 << cell) != 0, octant), 0, cropped);
            quads = MultipartGeometry.centered(cropped);
        }
        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) { return side == null ? quads : List.of(); }
        @Override public boolean useAmbientOcclusion() { return parent.useAmbientOcclusion(); }
        @Override public boolean isGui3d() { return parent.isGui3d(); }
        @Override public boolean usesBlockLight() { return parent.usesBlockLight(); }
        @Override public boolean isCustomRenderer() { return false; }
        @Override public TextureAtlasSprite getParticleIcon() { return parent.getParticleIcon(); }
        @Override public ItemTransforms getTransforms() { return parent.getTransforms(); }
        @Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
    }
}
