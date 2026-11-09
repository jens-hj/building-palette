package dev.jensh.buildingpalette;

import dev.jensh.buildingpalette.core.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class Placement {
    public record Plan(BlockPos pos, Cells before, int mask, String material) {}
    private Placement() {}
    public static BlockHitResult pick(Player player) {
        Vec3 eye = player.getEyePosition();
        // Server movement packets update body rotation before the next entity
        // tick copies it into head rotation. Entity.pick uses that stale head
        // angle, which can select a previous target during rapid mining/turning.
        Vec3 end = eye.add(Vec3.directionFromRotation(player.getXRot(), player.getYRot()).scale(player.blockInteractionRange()));
        return player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }
    public static int hitCell(BlockHitResult hit, boolean outside) {
        Vec3 local = hit.getLocation().subtract(Vec3.atLowerCornerOf(hit.getBlockPos()))
            .add(Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i()).scale(outside ? .0001 : -.0001));
        return cell(local);
    }
    private static int cell(Vec3 local) { return (local.x >= .5 ? 1 : 0) | (local.y >= .5 ? 2 : 0) | (local.z >= .5 ? 4 : 0); }
    public static boolean supportedHit(Level level, Player player, BlockHitResult hit) {
        if (hit.getType() != HitResult.Type.BLOCK || !Double.isFinite(hit.getLocation().lengthSqr())
                || player.getEyePosition().distanceToSqr(hit.getLocation()) > Math.pow(player.blockInteractionRange() + 1, 2)) return false;
        BlockState state = level.getBlockState(hit.getBlockPos());
        if (state.isAir()) return false;
        Vec3 inside = hit.getLocation().subtract(Vec3.atLowerCornerOf(hit.getBlockPos()))
            .add(Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i()).scale(-.0001));
        return state.getShape(level, hit.getBlockPos()).toAabbs().stream().anyMatch(box -> box.inflate(.00001).contains(inside));
    }
    private static BlockPos target(Level level, BlockHitResult hit) {
        return level.getBlockState(hit.getBlockPos()).canBeReplaced() ? hit.getBlockPos()
            : BlockPos.containing(hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i()).scale(.0001)));
    }
    public static boolean usesNativePlacement(Level level, Player player, ItemStack stack, BlockHitResult hit) {
        var selection = Materials.get(stack);
        // Quarter pieces always use our cell placement and material merging.
        // CNM's native step states only support a restricted diagonal doubling.
        return selection != null && selection.shape() != Shape.STEP && selection.shape() != Shape.VERTICAL_STEP
            && !PlacementPreferences.precise(player) && stack.getItem() instanceof BlockItem
            && !(level.getBlockEntity(target(level, hit)) instanceof MultipartEntity);
    }
    public static Cells existing(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MultipartEntity e) return e.cells();
        BlockState state = level.getBlockState(pos);
        if (state.canBeReplaced()) return new Cells();
        ItemStack stack = new ItemStack(state.getBlock().asItem());
        var selection = Materials.get(stack);
        if (selection == null || !selection.shape().isMultipart() || state.hasBlockEntity()) return null;
        // Auto-connected native stairs cost one stair despite having 5/7 octants.
        // They cannot be converted into octant accounting without changing value.
        if (state.getBlock() instanceof StairBlock && state.getValue(BlockStateProperties.STAIRS_SHAPE)
                != net.minecraft.world.level.block.state.properties.StairsShape.STRAIGHT) return null;
        String[] values = new String[8];
        var boxes = state.getShape(level, pos).toAabbs();
        for (int i = 0; i < 8; i++) {
            Vec3 center = new Vec3((i & 1) * .5 + .25, ((i >> 1) & 1) * .5 + .25, ((i >> 2) & 1) * .5 + .25);
            if (boxes.stream().anyMatch(box -> box.contains(center))) values[i] = selection.material();
        }
        return new Cells(values);
    }
    public static Plan plan(Level level, Player player, ItemStack stack, BlockHitResult hit) {
        var selection = Materials.get(stack);
        if (selection == null || !selection.shape().isMultipart() || !supportedHit(level, player, hit)) return null;
        if (usesNativePlacement(level, player, stack, hit)) {
            var item = (BlockItem)stack.getItem();
            var context = item.updatePlacementContext(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit));
            if (context == null || !context.canPlace()) return null;
            BlockState state = item.getBlock().getStateForPlacement(context);
            if (state == null || !state.canSurvive(level, context.getClickedPos())) return null;
            if (!canOccupy(level, player, state.getCollisionShape(level, context.getClickedPos(), CollisionContext.placementContext(player))
                    .move(context.getClickedPos()))) return null;
            Cells before = existing(level, context.getClickedPos());
            if (before == null) return null;
            int mask = shapeMask(state, level, context.getClickedPos()) & ~before.occupied();
            return mask == 0 ? null : new Plan(context.getClickedPos(), before, mask, selection.material());
        }
        Vec3 outside = hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i()).scale(.0001));
        BlockPos target = BlockPos.containing(outside);
        // Replaceable blocks (grass, snow) use their own space.
        if (level.getBlockState(hit.getBlockPos()).canBeReplaced()) target = hit.getBlockPos();
        Cells before = existing(level, target);
        if (before == null) return null;
        Vec3 local = outside.subtract(Vec3.atLowerCornerOf(target));
        int anchor = cell(local);
        int axis = hit.getDirection().getAxis() == Direction.Axis.Y
            ? (player.getDirection().getAxis() == Direction.Axis.X ? 0 : 2)
            : (hit.getDirection().getAxis() == Direction.Axis.X ? 0 : 2);
        int mask = Cells.mask(selection.shape(), anchor, axis);
        if (!PlacementPreferences.precise(player) && selection.shape() == Shape.STAIR && stack.getItem() instanceof BlockItem item) {
            // When combining into an existing multipart, retain native stair facing
            // and half instead of deriving its horizontal orientation from the click.
            var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit);
            var state = item.getBlock().getStateForPlacement(context);
            if (state == null) return null;
            state = state.setValue(BlockStateProperties.STAIRS_SHAPE, net.minecraft.world.level.block.state.properties.StairsShape.STRAIGHT);
            mask = shapeMask(state, level, target);
        }
        if ((before.occupied() & mask) != 0) return null;
        if (!canOccupy(level, player, MultipartBlock.shape(new Cells().place(mask, selection.material()), mask).move(target))) return null;
        return new Plan(target, before, mask, selection.material());
    }
    private static boolean canOccupy(Level level, Player player, VoxelShape added) {
        // Check the added piece, not its whole grid cell. Passing the placing
        // player to isUnobstructed would exclude that player from the check.
        return !Shapes.joinIsNotEmpty(added, Shapes.create(player.getBoundingBox()), BooleanOp.AND)
            && level.isUnobstructed(null, added);
    }
    private static int shapeMask(BlockState state, Level level, BlockPos pos) {
        int mask = 0;
        var boxes = state.getShape(level, pos).toAabbs();
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) * .5 + .25, y = ((i >> 1) & 1) * .5 + .25, z = ((i >> 2) & 1) * .5 + .25;
            if (boxes.stream().anyMatch(box -> box.contains(x, y, z))) mask |= 1 << i;
        }
        return mask;
    }
    public static InteractionResult use(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        var selection = Materials.get(stack);
        if (selection == null || player.isSpectator()) return InteractionResult.PASS;
        if (!supportedHit(level, player, hit)) return InteractionResult.FAIL;
        // Sneaking allows native interaction to remain reachable without the managed placement handler.
        if (!player.isShiftKeyDown() && level.getBlockState(hit.getBlockPos()).hasBlockEntity()
                && !(level.getBlockEntity(hit.getBlockPos()) instanceof MultipartEntity)) return InteractionResult.PASS;
        // Native placement checks fractional availability in BlockItem.place, after
        // normal block interaction, so a reserved stack can still open a chest.
        if (!selection.shape().isMultipart() || usesNativePlacement(level, player, stack, hit)) return InteractionResult.PASS;
        if (!player.hasInfiniteMaterials() && MaterialStacks.available(stack) < 1) return InteractionResult.FAIL;
        Plan plan = plan(level, player, stack, hit);
        if (plan == null || (!player.hasInfiniteMaterials() && MaterialStacks.available(stack) < 1)) return InteractionResult.FAIL;
        if (!level.mayInteract(player, plan.pos()) || !player.mayUseItemAt(plan.pos(), hit.getDirection(), stack)
                || level.isOutsideBuildHeight(plan.pos())) return InteractionResult.FAIL;
        Cells next = plan.before().place(plan.mask(), plan.material());
        VoxelShape added = MultipartBlock.shape(next, plan.mask()).move(plan.pos().getX(), plan.pos().getY(), plan.pos().getZ());
        if (!canOccupy(level, player, added)) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            boolean wet = level.getFluidState(plan.pos()).getType() == Fluids.WATER;
            String full = next.fullMaterial();
            if (full != null) level.setBlock(plan.pos(), MultipartBlock.material(full), 3);
            else {
                BlockState state = BuildingPalette.MULTIPART.get().defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, wet);
                if (!level.getBlockState(plan.pos()).is(BuildingPalette.MULTIPART.get())) level.setBlock(plan.pos(), state, 3);
                if (level.getBlockEntity(plan.pos()) instanceof MultipartEntity e) e.setCells(next);
            }
            if (!player.hasInfiniteMaterials()) MaterialStacks.spend(stack, 1);
            level.playSound(null, plan.pos(), MultipartBlock.material(plan.material()).getSoundType().getPlaceSound(),
                net.minecraft.sounds.SoundSource.BLOCKS, 1, 1);
        }
        return InteractionResult.SUCCESS;
    }
    public static boolean mine(net.minecraft.server.level.ServerPlayer player, BlockPos pos) {
        var level = player.level();
        if (!(level.getBlockEntity(pos) instanceof MultipartEntity entity)) return false;
        HitResult hit = pick(player);
        if (!(hit instanceof BlockHitResult block) || !block.getBlockPos().equals(pos)) return true;
        if (!level.mayInteract(player, pos) || player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())) return true;
        int cell = hitCell(block, false), mask = entity.cells().connected(cell);
        if (mask == 0) return true;
        String material = entity.cells().at(cell);
        BlockState source = MultipartBlock.material(material);
        ItemStack refund = Materials.piece(material, Shape.CORNER, Integer.bitCount(mask));
        if (refund.isEmpty()) return true;
        Cells remaining = entity.cells().remove(mask);
        if (remaining.occupied() == 0) level.setBlock(pos,
            entity.getBlockState().getValue(BlockStateProperties.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
        else entity.setCells(remaining);
        if (!player.hasInfiniteMaterials()) {
            if (player.hasCorrectToolForDrops(source)) Block.popResource(level, pos, refund);
            player.getMainHandItem().mineBlock(level, source, pos, player);
        }
        level.levelEvent(2001, pos, Block.getId(source));
        return true;
    }
}
