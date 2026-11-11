package dev.jensh.buildingpalette;

import dev.jensh.buildingpalette.core.Cells;
import dev.tazer.offgrid.OffgridSettings;
import dev.tazer.offgrid.world.Blocklogging;
import dev.tazer.offgrid.world.Overlay;
import dev.tazer.offgrid.world.Overlays;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Lets pieces share a block with an Offgrid blocklog piece (fence, wall, pane, bars, chain),
 * whichever is placed first, as long as part of that piece stays visible. */
public final class OffgridHosts {
    private OffgridHosts() {}
    /** A real fence/wall/pane/bars standing alone in the block, which would become an overlay. */
    public static boolean isGuest(Level level, BlockPos pos, BlockState state) {
        return Blocklogging.canBlocklog(state) && !dev.tazer.offgrid.world.Cells.hasOverlays(level, pos) && OffgridSettings.of(level).allowsBlocklogging();
    }
    /** Whether the block holds blocklog pieces that a new host would have to keep. */
    public static boolean occupied(Level level, BlockPos pos) {
        return dev.tazer.offgrid.world.Cells.hasOverlays(level, pos) || isGuest(level, pos, level.getBlockState(pos));
    }
    /** The carrier at pos as Offgrid should see it: a state whose shape is its occupied cells.
     * Reads the chunk directly: Offgrid may be redirecting the level's block entity lookups to an overlay. */
    public static BlockState proxy(BlockGetter getter, BlockPos pos) {
        Object entity = getter instanceof Level level
            ? (dev.tazer.offgrid.world.Cells.chunkAt(level, pos) instanceof net.minecraft.world.level.chunk.LevelChunk chunk ? chunk.getBlockEntity(pos) : null)
            : getter.getBlockEntity(pos);
        return HostShapeBlock.of(entity instanceof MultipartEntity e ? e.cells().occupied() : 0);
    }
    /** The host Offgrid will see once {@code cells} are stored, or null for a full block. */
    public static BlockState host(Cells cells, boolean wet) {
        if (cells.occupied() == 255) return null;
        BlockState nativeState = Placement.nativeState(cells, wet);
        return nativeState != null ? nativeState : HostShapeBlock.of(cells.occupied());
    }
    /** The guest's state fitted to {@code host}, or null if Offgrid would not blocklog it there. */
    public static BlockState fit(BlockState guest, BlockState host) {
        return Blocklogging.of(guest, host) != null ? guest : Blocklogging.fit(guest, host, guest);
    }
    /** Whether {@code host} can replace the block at pos without dropping what it holds. */
    public static boolean accepts(Level level, BlockPos pos, BlockState host) {
        if (host == null) return false;
        BlockState current = level.getBlockState(pos);
        if (isGuest(level, pos, current)) return fit(current, host) != null;
        for (Overlay overlay : dev.tazer.offgrid.world.Cells.overlays(level, pos))
            if (Blocklogging.of(overlay.getState(), host, overlay.getOffset()) == null) return false;
        return true;
    }
    /** Turn a former guest block into an overlay of the host now at pos. */
    public static boolean adopt(ServerLevel level, BlockPos pos, BlockState guest) {
        BlockState fitted = fit(guest, Blocklogging.hostState(level, pos));
        return fitted != null && Overlays.add(level, pos, fitted, Vec3.ZERO, null) != null;
    }
}
