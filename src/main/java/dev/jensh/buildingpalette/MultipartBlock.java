package dev.jensh.buildingpalette;

import dev.jensh.buildingpalette.core.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class MultipartBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty OPAQUE = BooleanProperty.create("opaque");
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);
    public static final IntegerProperty OCCLUSION = IntegerProperty.create("occlusion", 0, 255);
    private static final VoxelShape[] OCCLUSION_SHAPES = occlusionShapes();
    private static VoxelShape[] occlusionShapes() {
        var result = new VoxelShape[256];
        result[0] = Shapes.empty();
        for (int mask = 1; mask < 255; mask++) result[mask] = shape(new Cells().place(mask, "opaque"), mask);
        result[255] = Shapes.block();
        return result;
    }
    public MultipartBlock(BlockBehaviour.Properties properties) {
        // Mining sends the removed material's break effect separately. The invisible
        // carrier has no particle texture and must not add a second placeholder burst.
        super(properties.noTerrainParticles().pushReaction(PushReaction.IMMOVEABLE).lightLevel(state -> state.getValue(LIGHT)));
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.WATERLOGGED, false).setValue(OPAQUE, false).setValue(LIGHT, 0).setValue(OCCLUSION, 0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(BlockStateProperties.WATERLOGGED, OPAQUE, LIGHT, OCCLUSION); }
    @Override protected int getLightDampening(BlockState state) { return state.getValue(OPAQUE) ? 15 : state.getValue(BlockStateProperties.WATERLOGGED) ? 1 : 0; }
    @Override protected boolean propagatesSkylightDown(BlockState state) { return !state.getValue(OPAQUE) && !state.getValue(BlockStateProperties.WATERLOGGED); }
    @Override protected boolean useShapeForLightOcclusion(BlockState state) { return true; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MultipartEntity(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getOcclusionShape(BlockState state) { return OCCLUSION_SHAPES[state.getValue(OCCLUSION)]; }
    @Override protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) { return state.getValue(OPAQUE) ? .2f : 1; }
    @Override protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (level.getBlockEntity(pos) instanceof MultipartEntity entity) entity.refreshPropertiesOnLoad();
    }
    public static VoxelShape shape(Cells cells, int mask) {
        VoxelShape shape = Shapes.empty();
        for (int i = 0; i < 8; i++) if (cells.at(i) != null && (mask & 1 << i) != 0) {
            double x = (i & 1) * .5, y = ((i >> 1) & 1) * .5, z = ((i >> 2) & 1) * .5;
            shape = Shapes.or(shape, Shapes.box(x, y, z, x + .5, y + .5, z + .5));
        }
        return shape;
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return world.getBlockEntity(pos) instanceof MultipartEntity e ? shape(e.cells(), 255) : Shapes.empty();
    }
    @Override protected FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbor, BlockState neighborState, net.minecraft.util.RandomSource random) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return state;
    }
    public static BlockState material(String name) {
        Identifier id = name == null ? null : Identifier.tryParse(name);
        Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(id);
        return item instanceof BlockItem b ? b.getBlock().defaultBlockState() : Blocks.STONE.defaultBlockState();
    }
    @Override protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MultipartEntity e) {
            HitResult hit = Placement.pick(player);
            if (hit instanceof BlockHitResult block && block.getBlockPos().equals(pos)) {
                int cell = Placement.hitCell(block, false);
                if (e.cells().at(cell) != null) return material(e.cells().at(cell)).getDestroyProgress(player, level, pos);
            }
        }
        return 0;
    }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (!(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof MultipartEntity e)) return List.of();
        Map<String, Integer> amounts = new LinkedHashMap<>();
        for (String value : e.cells().copy()) if (value != null) amounts.merge(value, 1, Integer::sum);
        return amounts.entrySet().stream().map(entry -> Materials.piece(entry.getKey(), Shape.CORNER, entry.getValue())).toList();
    }
}
