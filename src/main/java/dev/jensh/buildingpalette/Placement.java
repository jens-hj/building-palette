package dev.jensh.buildingpalette;

import dev.jensh.buildingpalette.core.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
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
    public static int hitCell(BlockHitResult hit, boolean outside) {
        Vec3 local = hit.getLocation().subtract(Vec3.atLowerCornerOf(hit.getBlockPos()))
            .add(Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i()).scale(outside ? .0001 : -.0001));
        return cell(local);
    }
    private static int cell(Vec3 local) { return (local.x >= .5 ? 1 : 0) | (local.y >= .5 ? 2 : 0) | (local.z >= .5 ? 4 : 0); }
    public static Cells existing(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MultipartEntity e) return e.cells();
        BlockState state = level.getBlockState(pos);
        if (state.canBeReplaced()) return new Cells();
        ItemStack stack = new ItemStack(state.getBlock().asItem());
        var selection = Materials.get(stack);
        if (selection == null || !selection.shape().isMultipart() || state.hasBlockEntity()) return null;
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
        if (selection == null || !selection.shape().isMultipart()) return null;
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
        if ((before.occupied() & mask) != 0) return null;
        return new Plan(target, before, mask, selection.material());
    }
    public static InteractionResult use(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        var selection = Materials.get(stack);
        if (selection == null || !selection.shape().isMultipart() || player.isSpectator()) return InteractionResult.PASS;
        // Sneaking allows native interaction to remain reachable without the managed placement handler.
        if (!player.isShiftKeyDown() && level.getBlockState(hit.getBlockPos()).hasBlockEntity()
                && !(level.getBlockEntity(hit.getBlockPos()) instanceof MultipartEntity)) return InteractionResult.PASS;
        Plan plan = plan(level, player, stack, hit);
        if (plan == null || (!player.hasInfiniteMaterials() && MaterialStacks.available(stack) < 1)) return InteractionResult.FAIL;
        if (!level.mayInteract(player, plan.pos()) || !player.mayUseItemAt(plan.pos(), hit.getDirection(), stack)
                || level.isOutsideBuildHeight(plan.pos())) return InteractionResult.FAIL;
        Cells next = plan.before().place(plan.mask(), plan.material());
        VoxelShape added = MultipartBlock.shape(next, plan.mask()).move(plan.pos().getX(), plan.pos().getY(), plan.pos().getZ());
        if (!level.isUnobstructed(player, added)) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            boolean wet = level.getFluidState(plan.pos()).getType() == Fluids.WATER;
            String full = next.fullMaterial();
            if (full != null) level.setBlock(plan.pos(), MultipartBlock.material(full), 3);
            else {
                BlockState state = BuildingPalette.MULTIPART.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, wet);
                if (!level.getBlockState(plan.pos()).is(BuildingPalette.MULTIPART)) level.setBlock(plan.pos(), state, 3);
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
        HitResult hit = player.pick(player.blockInteractionRange(), 1, false);
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
