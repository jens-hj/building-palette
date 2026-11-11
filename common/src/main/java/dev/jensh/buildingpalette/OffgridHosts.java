package dev.jensh.buildingpalette;

import dev.tazer.offgrid.OffgridSettings;
import dev.tazer.offgrid.world.Blocklogging;
import dev.tazer.offgrid.world.Cells;
import dev.tazer.offgrid.world.Overlay;
import dev.tazer.offgrid.world.Overlays;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Lets pieces go into a block that already holds an Offgrid blocklog piece (fence, wall, pane, bars),
 * so a step and a fence combine in either order. */
public final class OffgridHosts {
    private OffgridHosts() {}
    /** A real fence/wall/pane/bars standing alone in the block, which would become an overlay. */
    public static boolean isGuest(Level level, BlockPos pos, BlockState state) {
        return Blocklogging.canBlocklog(state) && !Cells.hasOverlays(level, pos) && OffgridSettings.of(level).allowsBlocklogging();
    }
    /** Whether the block holds blocklog pieces that a new host would have to keep. */
    public static boolean occupied(Level level, BlockPos pos) {
        return Cells.hasOverlays(level, pos) || isGuest(level, pos, level.getBlockState(pos));
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
        for (Overlay overlay : Cells.overlays(level, pos))
            if (Blocklogging.of(overlay.getState(), host, overlay.getOffset()) == null) return false;
        return true;
    }
    /** Turn a former guest block into an overlay of the host now at pos. */
    public static boolean adopt(ServerLevel level, BlockPos pos, BlockState guest) {
        BlockState fitted = fit(guest, level.getBlockState(pos));
        return fitted != null && Overlays.add(level, pos, fitted, Vec3.ZERO, null) != null;
    }
}
