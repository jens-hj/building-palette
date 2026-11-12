package dev.jensh.buildingpalette;

import dev.jensh.buildingpalette.core.Cells;
import dev.tazer.offgrid.OffgridSettings;
import dev.tazer.offgrid.world.Blocklogging;
import dev.tazer.offgrid.world.Overlay;
import dev.tazer.offgrid.world.OverlayShapes;
import dev.tazer.offgrid.world.Overlays;
import dev.tazer.offgrid.world.Surfaces;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.*;
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
    /** Whether the block at pos can become {@code cells} without dropping what it holds. Blocklog
     * pieces need a host that keeps part of them visible; any other Offgrid piece in the block
     * (an end rod, a lantern) only has to stay clear of the cells themselves. */
    public static boolean accepts(Level level, BlockPos pos, Cells cells, boolean wet) {
        BlockState host = host(cells, wet), current = level.getBlockState(pos);
        if (isGuest(level, pos, current)) return host != null && fit(current, host) != null;
        for (Overlay overlay : dev.tazer.offgrid.world.Cells.overlays(level, pos))
            if (Blocklogging.canBlocklog(overlay.getState()) && (host == null || Blocklogging.of(overlay.getState(), host, overlay.getOffset()) == null)) return false;
        return true;
    }
    /** Whether an Offgrid piece other than a blocklog piece, in this block or reaching in from a
     * neighbour, occupies part of {@code added} (a shape relative to pos). */
    public static boolean blocked(Level level, BlockPos pos, VoxelShape added) {
        if (added.isEmpty()) return false;
        for (BlockPos cell : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (!level.hasChunkAt(cell)) continue;
            for (Overlay overlay : dev.tazer.offgrid.world.Cells.overlays(level, cell)) {
                if (Blocklogging.canBlocklog(overlay.getState())) continue;
                VoxelShape shape = OverlayShapes.of(level, cell, overlay);
                if (!shape.isEmpty() && Shapes.joinIsNotEmpty(added, shape.move(cell.getX() - pos.getX(), cell.getY() - pos.getY(), cell.getZ() - pos.getZ()), BooleanOp.AND)) return true;
            }
        }
        return false;
    }
    /** Offgrid centres a placed piece on whichever box of the clicked shape lies under the cursor.
     * A combined block's boxes span several cells, so that centre can land inside a neighbouring
     * piece and the placement is refused. When Offgrid finds no spot, try the centre of the
     * clicked half cell: anything at most half a block wide fits there if the space is really free. */
    public static Vec3 halfCellOffset(Level level, BlockPos clicked, Direction face, Vec3 location, BlockState placed) {
        if (!dev.tazer.offgrid.world.Cells.realState(level, clicked).is(BuildingPalette.MULTIPART.get())) return null;
        VoxelShape shape = placed.getShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
        if (shape.isEmpty()) return null;
        var bounds = shape.bounds();
        Direction.Axis axis = face.getAxis();
        double[] hit = {location.x - clicked.getX(), location.y - clicked.getY(), location.z - clicked.getZ()}, offset = new double[3];
        for (Direction.Axis lateral : Direction.Axis.values()) {
            int i = lateral.ordinal();
            if (lateral == axis) {
                // Offsets are relative to the block in front of the clicked face.
                offset[i] = Math.round((hit[i] - (face.getAxisDirection().getStep() > 0 ? 1 : 0)) * 16) / 16.0;
                continue;
            }
            if (bounds.max(lateral) - bounds.min(lateral) > .5 + 1.0E-4) return null;
            double centre = hit[i] < .5 ? .25 : .75;
            offset[i] = centre - (bounds.min(lateral) + bounds.max(lateral)) / 2;
        }
        Vec3 candidate = new Vec3(offset[0], offset[1], offset[2]);
        return Surfaces.fits(level, clicked.relative(face), placed, candidate) ? candidate : null;
    }
    /** Turn a former guest block into an overlay of the host now at pos. */
    public static boolean adopt(ServerLevel level, BlockPos pos, BlockState guest) {
        BlockState fitted = fit(guest, Blocklogging.hostState(level, pos));
        return fitted != null && Overlays.add(level, pos, fitted, Vec3.ZERO, null) != null;
    }
}
