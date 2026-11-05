package dev.jensh.buildingpalette.client;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.core.Cells;
import java.util.*;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.blockgetter.v2.FabricBlockGetter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.*;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/** World-aware terrain geometry: vanilla/Indigo/Sodium apply per-vertex light and AO. */
public final class MultipartModel implements BlockStateModel, FabricBlockStateModel {
    private final BlockStateModel fallback;
    public MultipartModel(BlockStateModel fallback) { this.fallback = fallback; }
    private static Cells cells(BlockAndTintGetter world, BlockPos pos) {
        Object data = ((FabricBlockGetter)world).getBlockEntityRenderData(pos);
        return data instanceof Cells cells ? cells : new Cells();
    }
    private record GeometryKey(List<String> cells, long seed) {}
    @Override public Object createGeometryKey(BlockAndTintGetter world, BlockPos pos, BlockState state, RandomSource random) {
        return new GeometryKey(Arrays.asList(cells(world, pos).copy()), pos.asLong());
    }
    @Override public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {}
    @Override public Material.Baked particleMaterial() { return fallback.particleMaterial(); }
    @Override public int materialFlags() { return 0; }
    @Override public int materialFlags(BlockAndTintGetter world, BlockPos pos, BlockState state, RandomSource random) {
        int flags = 0;
        var models = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        for (String material : cells(world, pos).copy()) if (material != null) flags |= models.get(MultipartBlock.material(material)).materialFlags();
        return flags;
    }
    @Override public void emitQuads(QuadEmitter emitter, BlockAndTintGetter world, BlockPos pos, BlockState state,
            RandomSource random, Predicate<Direction> cullTest) {
        Cells cells = cells(world, pos);
        var mc = Minecraft.getInstance();
        var models = mc.getModelManager().getBlockStateModelSet();
        for (int i = 0; i < 8; i++) {
            String material = cells.at(i);
            if (material == null) continue;
            BlockState sourceState = MultipartBlock.material(material);
            var model = models.get(sourceState);
            List<BlockStateModelPart> parts = new ArrayList<>();
            // All octants of a material must share its vanilla model variant, so
            // cropping does not introduce seams between independently randomized faces.
            model.collectParts(RandomSource.create(sourceState.getSeed(pos)), parts);
            int visible = 0;
            for (Direction direction : Direction.values()) {
                int x = (i & 1) + direction.getStepX(), y = ((i >> 1) & 1) + direction.getStepY(), z = ((i >> 2) & 1) + direction.getStepZ();
                if (x < 0 || x > 1 || y < 0 || y > 1 || z < 0 || z > 1 || cells.at(x | y << 1 | z << 2) == null)
                    visible |= 1 << direction.ordinal();
            }
            for (var part : parts) {
                var cropped = new MultipartGeometry.CroppedPart(part, i, visible);
                for (var quad : cropped.quads()) {
                    emitter.fromBakedQuad(quad).cullFace(null).ambientOcclusion(part.useAmbientOcclusion() ? TriState.TRUE : TriState.FALSE);
                    if (emitter.tintIndex() >= 0) {
                        var tint = mc.getBlockColors().getTintSource(sourceState, emitter.tintIndex());
                        if (tint != null) emitter.multiplyColor(tint.colorInWorld(sourceState, world, pos) | 0xff000000);
                        emitter.tintIndex(-1);
                    }
                    emitter.emit();
                }
            }
        }
    }
}
