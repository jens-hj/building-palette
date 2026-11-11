package dev.jensh.buildingpalette;

import dev.jensh.buildingpalette.core.Cells;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.*;

/**
 * Never placed in a world. Offgrid reads a host's shape from its block state alone,
 * but a carrier's cells live in its block entity, so Offgrid is shown this stand-in
 * whose state encodes the occupied cells.
 */
public final class HostShapeBlock extends Block {
    public static final IntegerProperty CELLS = IntegerProperty.create("cells", 0, 255);
    private static final VoxelShape[] SHAPES = new VoxelShape[256];
    static {
        for (int mask = 0; mask < 256; mask++) SHAPES[mask] = mask == 0 ? Shapes.empty() : MultipartBlock.shape(new Cells().place(mask, "x"), mask);
    }
    public HostShapeBlock(BlockBehaviour.Properties properties) {
        super(properties.noLootTable());
        registerDefaultState(stateDefinition.any().setValue(CELLS, 0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(CELLS); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPES[state.getValue(CELLS)]; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    public static BlockState of(int mask) { return BuildingPalette.HOST_SHAPE.get().defaultBlockState().setValue(CELLS, mask & 255); }
}
