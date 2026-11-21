package dev.jensh.buildingpalette.neoforge;

import dev.jensh.buildingpalette.MultipartBlock;
import dev.jensh.buildingpalette.MultipartEntity;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public final class SableCollisions {
    private SableCollisions() {}

    /** Sable bakes at the origin in a BlockGetter without block entities and caches by state.
     * Use the carrier's existing eight-bit shape property as the key for the physics query. */
    public static BlockState physicsState(BlockState state, BlockGetter world, BlockPos pos) {
        if (state.getBlock() instanceof MultipartBlock && world.getBlockEntity(pos) instanceof MultipartEntity entity)
            return state.setValue(MultipartBlock.OCCLUSION, entity.cells().occupied());
        return state;
    }

    public static void refresh(ServerLevel level, BlockPos pos, BlockState state) {
        var container = SubLevelContainer.getContainer(level);
        if (container == null) return;
        var system = container.physicsSystem();
        var chunk = level.getChunkAt(pos);
        var section = chunk.getSection(chunk.getSectionIndex(pos.getY()));
        system.getPipeline().handleBlockChange(SectionPos.of(pos), section, pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, state, state);
        system.wakeUpObjectsAt(pos.getX(), pos.getY(), pos.getZ());
    }
}
