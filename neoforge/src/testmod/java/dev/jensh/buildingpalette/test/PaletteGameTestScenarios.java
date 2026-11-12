package dev.jensh.buildingpalette.test;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.core.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;

public final class PaletteGameTestScenarios {
    public void stepsAlwaysUseMultipartPlacement(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        var player = TestPlayers.survival(test);
        player.setPos(pos.getX() + 2, pos.getY(), pos.getZ() + 2);
        try {
            for (Shape shape : new Shape[]{Shape.STEP, Shape.VERTICAL_STEP})
                for (boolean precise : new boolean[]{false, true}) for (boolean mixed : new boolean[]{false, true})
                    for (Direction facing : new Direction[]{Direction.SOUTH, Direction.EAST}) {
                PlacementPreferences.set(player, precise); player.setYRot(facing.toYRot());
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                ItemStack oak = Materials.piece("minecraft:oak_planks", shape, mixed ? 4 : 8);
                ItemStack stone = Materials.piece("minecraft:stone_bricks", shape, 4);
                if (shape == Shape.STEP) test.assertTrue(oak.getItem() instanceof BlockItem, "Regression exercises the actual CNM step item");
                Cells expected = new Cells();
                for (int part = 0; part < 4; part++) {
                    boolean useStone = mixed && (part == 1 || part == 2);
                    ItemStack stack = useStone ? stone : oak;
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
                    int half = part / 2, side = part % 2;
                    double x = .25, y = half * .5, z = .25 + side * .5;
                    int mask;
                    if (shape == Shape.VERTICAL_STEP) {
                        x = .25 + half * .5; y = 0;
                        mask = Cells.mask(shape, half | side << 2, 0);
                    } else {
                        if (facing == Direction.EAST) { x = z; z = .25; }
                        mask = Cells.mask(shape, (half << 1) | (facing == Direction.EAST ? side : side << 2), facing == Direction.EAST ? 0 : 2);
                    }
                    var hit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX() + x, pos.getY() + y, pos.getZ() + z),
                        Direction.UP, y == 0 ? pos.below() : pos, false);
                    test.assertTrue(!Placement.usesNativePlacement(level, player, stack, hit), "Steps never delegate to CNM in either preference mode");
                    test.assertTrue(Placement.use(player, level, net.minecraft.world.InteractionHand.MAIN_HAND, hit) == net.minecraft.world.InteractionResult.SUCCESS, "All four quarter positions can be filled");
                    expected = expected.place(mask, useStone ? "minecraft:stone_bricks" : "minecraft:oak_planks");
                    if (part < 3 || mixed) {
                        boolean single = java.util.Arrays.stream(expected.copy()).filter(java.util.Objects::nonNull).distinct().count() == 1;
                        test.assertTrue(single || level.getBlockEntity(pos) instanceof MultipartEntity, "Mixed steps use Building Palette carrier");
                        test.assertTrue(Placement.existing(level, pos) != null && java.util.Arrays.equals(Placement.existing(level, pos).copy(), expected.copy()), "Step positions and materials match exactly");
                    } else test.assertTrue(level.getBlockState(pos).is(Blocks.OAK_PLANKS), "Four same-material steps become a full native block");
                }
                test.assertTrue(oak.isEmpty() && (!mixed || stone.isEmpty()), "Four quarters spend exactly one block of material");
            }
            // A step already placed by CNM before upgrading must also convert
            // when a different material is added to its remaining space.
            PlacementPreferences.set(player, false); player.setYRot(0);
            for (Shape shape : new Shape[]{Shape.STEP, Shape.VERTICAL_STEP}) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                var oak = Materials.piece("minecraft:oak_planks", shape, 2);
                if (!(oak.getItem() instanceof BlockItem item)) continue;
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, oak);
                var originalHit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX() + .25, pos.getY(), pos.getZ() + .25), Direction.UP, pos.below(), false);
                var state = item.getBlock().getStateForPlacement(new net.minecraft.world.item.context.BlockPlaceContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, oak, originalHit));
                level.setBlock(pos, state, 3);
                Cells before = Placement.existing(level, pos);
                test.assertValueEqual(Integer.bitCount(before.occupied()), 2, "Legacy native fixture contains one quarter step");
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, Materials.piece("minecraft:stone_bricks", shape, 2));
                boolean placed = false;
                for (int x = 0; x < 2 && !placed; x++) for (int z = 0; z < 2 && !placed; z++) {
                    var hit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX() + .25 + x * .5, pos.getY(), pos.getZ() + .25 + z * .5), Direction.UP, pos.below(), false);
                    var plan = Placement.plan(level, player, player.getMainHandItem(), hit);
                    if (plan == null) continue;
                    test.assertTrue(Placement.use(player, level, net.minecraft.world.InteractionHand.MAIN_HAND, hit) == net.minecraft.world.InteractionResult.SUCCESS, "Can add different material to old native step");
                    var after = ((MultipartEntity)level.getBlockEntity(pos)).cells();
                    test.assertValueEqual(Integer.bitCount(after.occupied()), 4, "Converting old native step preserves volume");
                    for (int i = 0; i < 8; i++) if (before.at(i) != null) test.assertTrue(before.at(i).equals(after.at(i)), "Old native step material survives conversion");
                    test.assertTrue(player.getMainHandItem().isEmpty(), "Legacy conversion consumes exactly one new step");
                    placed = true;
                }
                test.assertTrue(placed, "Legacy native step has an accessible free quarter");
            }
        } finally { PlacementPreferences.remove(player); }
        test.succeed();
    }
    public void placementChecksExactPieceAgainstPlayer(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(2, 2, 2));
        var player = TestPlayers.survival(test);
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        var hit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX() + .25, pos.getY(), pos.getZ() + .25), Direction.UP, pos.below(), false);
        try {
            for (boolean precise : new boolean[]{false, true}) {
                PlacementPreferences.set(player, precise);
                // A corner fits beside a player whose hitbox occupies the same
                // grid cell, but not the same octant. Test actual player bounds.
                for (boolean overlaps : new boolean[]{true, false}) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    player.setPos(pos.getX() + (overlaps ? .1 : .9), pos.getY(), pos.getZ() + (overlaps ? .1 : .9));
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, Materials.piece("minecraft:oak_planks", Shape.CORNER, 8));
                    var result = Placement.use(player, level, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
                    test.assertTrue(result == (overlaps ? net.minecraft.world.InteractionResult.FAIL : net.minecraft.world.InteractionResult.SUCCESS), "Corner uses exact collision in mode " + precise);
                    test.assertValueEqual(MaterialStacks.units(player.getMainHandItem()), overlaps ? 8 : 7, "Rejected placement preserves material");
                    test.assertTrue((level.getBlockEntity(pos) instanceof MultipartEntity) != overlaps, "Rejected placement creates no carrier");
                }
            }
            PlacementPreferences.set(player, true);
            for (Shape shape : Shape.values()) if (shape.isMultipart()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                player.setPos(pos.getX() + .1, pos.getY(), pos.getZ() + .1);
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, Materials.piece("minecraft:oak_planks", shape, 16));
                test.assertTrue(Placement.plan(level, player, player.getMainHandItem(), hit) == null, "No overlapping preview for " + shape);
                test.assertTrue(Placement.use(player, level, net.minecraft.world.InteractionHand.MAIN_HAND, hit) == net.minecraft.world.InteractionResult.FAIL, "Reject intersecting " + shape);
                test.assertValueEqual(MaterialStacks.units(player.getMainHandItem()), 16, "Rejected " + shape + " preserves balance");
            }
            // A bottom slab may be placed directly under feet at its top face;
            // an upper slab in that same cell must be rejected.
            for (boolean upper : new boolean[]{false, true}) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                player.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, Materials.piece("minecraft:oak_planks", Shape.SLAB, 16));
                level.setBlock(pos.west(), Blocks.STONE.defaultBlockState(), 3);
                var slabHit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX(), pos.getY() + (upper ? .75 : .25), pos.getZ() + .25), Direction.EAST, pos.west(), false);
                test.assertTrue(Placement.use(player, level, net.minecraft.world.InteractionHand.MAIN_HAND, slabHit) == (upper ? net.minecraft.world.InteractionResult.FAIL : net.minecraft.world.InteractionResult.SUCCESS), "Slab checks actual half and allows touching boundary");
            }
        } finally { PlacementPreferences.remove(player); }
        test.succeed();
    }
    public void pickKeepsExistingShapeThenReshapesHeldMaterial(GameTestHelper test) {
        var player = TestPlayers.survival(test);
        var pos = test.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(pos.getX() + 1, pos.getY(), pos.getZ() + 1);
        test.getLevel().setBlock(pos, Blocks.OAK_STAIRS.defaultBlockState(), 3);
        var stored = Materials.piece("minecraft:oak_planks", Shape.SLAB, 80);
        stored.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Picked material"));
        player.getInventory().setItem(10, stored);
        ItemStack first = PickBlock.pick(player, pos, test.getLevel().getBlockState(pos), false);
        test.assertTrue(Materials.get(first).shape() == Shape.SLAB, "First pick preserves available shape");
        test.assertValueEqual(MaterialStacks.units(first), 80, "First pick preserves whole balance");
        player.getInventory().pickSlot(10);
        test.assertTrue(PickBlock.pick(player, pos, test.getLevel().getBlockState(pos), false).isEmpty(), "Second pick edits the held slot in place");
        ItemStack second = player.getMainHandItem();
        test.assertTrue(Materials.get(second).shape() == Shape.STAIR, "Second pick matches clicked shape");
        test.assertValueEqual(MaterialStacks.units(second), 80, "Second pick preserves fractional balance");
        test.assertTrue(second.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME).equals(stored.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME)), "Pick preserves custom components");
        player.getInventory().clearContent();
        test.assertTrue(PickBlock.pick(player, pos, test.getLevel().getBlockState(pos), false).isEmpty(), "Survival pick cannot create missing material");
        test.succeed();
    }
    public void pickRefillsInsufficientHeldMaterial(GameTestHelper test) {
        var player = TestPlayers.survival(test);
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(pos.getX() + 1, pos.getY(), pos.getZ() + 1);
        var inventory = player.getInventory();
        inventory.selected = 0;
        // Same shape: a full block stack with only a fraction left ("0+").
        level.setBlock(pos, Blocks.OAK_PLANKS.defaultBlockState(), 3);
        inventory.setItem(0, Materials.piece("minecraft:oak_planks", Shape.BLOCK, 3));
        inventory.setItem(5, Materials.piece("minecraft:oak_planks", Shape.SLAB, 200));
        inventory.setItem(20, Materials.piece("minecraft:oak_planks", Shape.CORNER, 100));
        inventory.setItem(21, Materials.piece("minecraft:stone_bricks", Shape.BLOCK, 64));
        PickBlock.pick(player, pos, level.getBlockState(pos), false);
        test.assertTrue(Materials.get(inventory.getItem(0)).shape() == Shape.BLOCK, "Refill keeps the held shape");
        test.assertValueEqual(MaterialStacks.units(inventory.getItem(0)), 303, "Refill pulls all matching material");
        test.assertTrue(inventory.getItem(5).isEmpty() && inventory.getItem(20).isEmpty(), "Refill drains other stacks of the material");
        test.assertValueEqual(MaterialStacks.units(inventory.getItem(21)), 64, "Refill ignores other materials");
        test.assertValueEqual(inventory.selected, 0, "Refill keeps the selected slot");
        // Storage is used before the hotbar, and the stack fills to capacity.
        inventory.setItem(0, Materials.piece("minecraft:oak_planks", Shape.BLOCK, 3));
        inventory.setItem(1, Materials.piece("minecraft:oak_planks", Shape.BLOCK, 3));
        inventory.setItem(30, Materials.piece("minecraft:oak_planks", Shape.BLOCK, 512));
        PickBlock.pick(player, pos, level.getBlockState(pos), false);
        test.assertValueEqual(MaterialStacks.units(inventory.getItem(0)), 512, "Refill fills to capacity");
        test.assertValueEqual(MaterialStacks.units(inventory.getItem(1)), 3, "Refill leaves hotbar stacks when storage suffices");
        test.assertValueEqual(MaterialStacks.units(inventory.getItem(30)), 3, "Refill takes only what fits");
        test.assertValueEqual(inventory.selected, 0, "Identical leftover stack is not selected");
        // A different, larger target shape than the held material can make.
        inventory.clearContent();
        level.setBlock(pos, Blocks.OAK_STAIRS.defaultBlockState(), 3);
        inventory.setItem(0, Materials.piece("minecraft:oak_planks", Shape.SLAB, 4));
        inventory.setItem(12, Materials.piece("minecraft:oak_planks", Shape.BLOCK, 64));
        PickBlock.pick(player, pos, level.getBlockState(pos), false);
        test.assertTrue(Materials.get(inventory.getItem(0)).shape() == Shape.STAIR, "Pick reshapes before refilling");
        test.assertValueEqual(MaterialStacks.units(inventory.getItem(0)), 68, "Reshaped stack is refilled");
        // Enough material already: nothing else moves.
        inventory.clearContent();
        inventory.setItem(0, Materials.piece("minecraft:oak_planks", Shape.STAIR, 20));
        inventory.setItem(12, Materials.piece("minecraft:oak_planks", Shape.BLOCK, 64));
        PickBlock.pick(player, pos, level.getBlockState(pos), false);
        test.assertValueEqual(MaterialStacks.units(inventory.getItem(0)), 20, "Sufficient held stack is not refilled");
        test.assertValueEqual(MaterialStacks.units(inventory.getItem(12)), 64, "Sufficient pick leaves inventory alone");
        test.succeed();
    }
    public void shiftClickMergesShapesBeforeEmptySlots(GameTestHelper test) {
        var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var chest = new SimpleContainer(27);
        var menu = new net.minecraft.world.inventory.ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x3, 1, player.getInventory(), chest, 3);
        chest.setItem(0, Materials.piece("minecraft:oak_planks", Shape.CORNER, 65));
        player.getInventory().setItem(8, Materials.piece("minecraft:oak_planks", Shape.STAIR, 448));
        player.getInventory().setItem(7, Materials.piece("minecraft:oak_planks", Shape.BLOCK, 500));
        menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, player);
        test.assertTrue(chest.getItem(0).isEmpty(), "Shift click moves the whole available source");
        test.assertValueEqual(MaterialStacks.units(player.getInventory().getItem(8)), 512, "Shift click fills existing stairs first");
        test.assertTrue(Materials.get(player.getInventory().getItem(8)).shape() == Shape.STAIR, "Shift target keeps stair shape");
        test.assertValueEqual(MaterialStacks.units(player.getInventory().getItem(7)), 501, "Shift remainder merges into another existing shape");
        test.assertTrue(player.getInventory().getItem(0).isEmpty(), "No empty slot used while compatible stacks have room");
        chest.setItem(0, Materials.piece("minecraft:oak_planks", Shape.SLAB, 500));
        chest.setItem(1, Materials.piece("minecraft:oak_planks", Shape.VERTICAL_SLAB, 500));
        player.getInventory().setItem(9, Materials.piece("minecraft:oak_planks", Shape.CORNER, 32));
        menu.clicked(27, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, player);
        test.assertValueEqual(MaterialStacks.units(chest.getItem(0)), 512, "Reverse shift click fills slab stack");
        test.assertValueEqual(MaterialStacks.units(chest.getItem(1)), 512, "Reverse shift click fills vertical slabs");
        test.assertValueEqual(MaterialStacks.units(chest.getItem(2)), 8, "Only overflow goes into an empty slot");
        test.assertTrue(player.getInventory().getItem(9).isEmpty(), "Reverse shift click empties source");
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
        player.getInventory().setItem(8, Materials.piece("minecraft:oak_planks", Shape.STAIR, 500));
        chest.setItem(0, Materials.piece("minecraft:oak_planks", Shape.CORNER, 32));
        menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, player);
        test.assertValueEqual(MaterialStacks.units(player.getInventory().getItem(8)), 512, "Full inventory still fills compatible material stack");
        test.assertValueEqual(MaterialStacks.units(chest.getItem(0)), 20, "Untransferable overflow remains in original slot");
        test.succeed();
    }
    public void singlePiecesUseNativeBlocks(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        var player = TestPlayers.survival(test);
        player.setPos(pos.getX() + 2, pos.getY(), pos.getZ() + 2);
        try {
            for (boolean precise : new boolean[]{false, true}) {
                PlacementPreferences.set(player, precise);
                // An upper step, placed against the top half of a neighbouring block.
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(pos.west(), Blocks.STONE.defaultBlockState(), 3);
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, Materials.piece("minecraft:oak_planks", Shape.STEP, 8));
                var hit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX(), pos.getY() + .75, pos.getZ() + .25), Direction.EAST, pos.west(), false);
                test.assertTrue(Placement.use(player, level, net.minecraft.world.InteractionHand.MAIN_HAND, hit) == net.minecraft.world.InteractionResult.SUCCESS, "Upper step places");
                var state = level.getBlockState(pos);
                test.assertTrue(Materials.classify(state.getBlock()) == Shape.STEP && !(level.getBlockEntity(pos) instanceof MultipartEntity), "Single upper step is stored as the native step block");
                test.assertValueEqual(Integer.bitCount(Placement.existing(level, pos).occupied()), 2, "Native step keeps the quarter volume");
                test.assertTrue((Placement.existing(level, pos).occupied() & 0b00110011) == 0, "Native step stays in the upper half");
                // Offgrid can blocklog a fence running under the step.
                var axis = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING).getAxis();
                var fence = Blocks.OAK_FENCE.defaultBlockState();
                for (Direction side : Direction.Plane.HORIZONTAL) if (side.getAxis() == axis)
                    fence = fence.setValue(net.minecraft.world.level.block.PipeBlock.PROPERTY_BY_DIRECTION.get(side), true);
                test.assertTrue(dev.tazer.offgrid.world.Blocklogging.of(fence, state) != null, "Offgrid can blocklog a fence into a placed upper step");
                // A lone post, or a fence crossing the step, also fits; Offgrid cuts the overlap.
                var crossing = Blocks.OAK_FENCE.defaultBlockState();
                for (Direction side : Direction.Plane.HORIZONTAL) if (side.getAxis() != axis)
                    crossing = crossing.setValue(net.minecraft.world.level.block.PipeBlock.PROPERTY_BY_DIRECTION.get(side), true);
                for (var piece : new net.minecraft.world.level.block.state.BlockState[]{Blocks.OAK_FENCE.defaultBlockState(), crossing}) {
                    var cut = dev.tazer.offgrid.world.Blocklogging.of(piece, state);
                    test.assertTrue(cut != null, "Offgrid can blocklog " + piece + " into an upper step");
                    test.assertTrue(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(cut.outline(),
                        state.getShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO), net.minecraft.world.phys.shapes.BooleanOp.AND), "Blocklogged post does not overlap the step");
                }
                // Real placement: a fence clicked against the step's underside goes into the step's block.
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_FENCE, 4));
                var under = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX() + .25, pos.getY() + .5, pos.getZ() + .5), Direction.DOWN, pos, false);
                player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, under));
                test.assertValueEqual(dev.tazer.offgrid.world.Cells.overlays(level, pos).size(), 1, "Fence post is blocklogged into the step's block");
                test.assertTrue(Materials.classify(level.getBlockState(pos).getBlock()) == Shape.STEP, "Step stays in place under the fence");
                test.assertValueEqual(player.getMainHandItem().getCount(), 3, "Blocklogging uses one fence");
                for (var overlay : dev.tazer.offgrid.world.Cells.overlays(level, pos)) dev.tazer.offgrid.world.Overlays.remove(level, pos, overlay, false);
                // Adding a different material still converts into a carrier.
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, Materials.piece("minecraft:stone_bricks", Shape.CORNER, 8));
                var corner = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX() + .25, pos.getY(), pos.getZ() + .25), Direction.UP, pos.below(), false);
                test.assertTrue(Placement.use(player, level, net.minecraft.world.InteractionHand.MAIN_HAND, corner) == net.minecraft.world.InteractionResult.SUCCESS, "Corner combines into native step");
                test.assertTrue(level.getBlockEntity(pos) instanceof MultipartEntity, "Mixed materials use a carrier");
                test.assertValueEqual(Integer.bitCount(Placement.existing(level, pos).occupied()), 3, "Combining keeps the step volume");
            }
            // Mining a mixed block down to one native piece stores that piece natively.
            level.setBlock(pos, BuildingPalette.MULTIPART.get().defaultBlockState(), 3);
            ((MultipartEntity)level.getBlockEntity(pos)).setCells(new Cells().place(Cells.mask(Shape.SLAB, 2, 0), "minecraft:oak_planks").place(1 << 1, "minecraft:stone_bricks"));
            player.setPos(pos.getX() + .75, pos.getY(), pos.getZ() - 2);
            var aim = new net.minecraft.world.phys.Vec3(pos.getX() + .75, pos.getY() + .25, pos.getZ() + .25).subtract(player.getEyePosition());
            player.setYRot((float)(Math.toDegrees(Math.atan2(aim.z, aim.x)) - 90));
            player.setXRot((float)-Math.toDegrees(Math.atan2(aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z))));
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
            test.assertTrue(Placement.mine(player, pos), "Mining handler accepts mixed block");
            test.assertTrue(level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.SlabBlock, "Remaining single slab is stored natively");
            test.assertValueEqual(Placement.existing(level, pos).occupied(), Cells.mask(Shape.SLAB, 2, 0), "Remaining slab keeps its cells");
        } finally { PlacementPreferences.remove(player); }
        test.succeed();
    }
    public void offgridFenceAndStepInEitherOrder(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(pos.above(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        var player = TestPlayers.survival(test);
        player.setPos(pos.getX() + .5, pos.getY() - 1, pos.getZ() - 1.5);
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        // Step under the ceiling, clicked on the ceiling's underside as in game.
        var ceiling = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX() + .25, pos.getY() + 1, pos.getZ() + .25), Direction.DOWN, pos.above(), false);
        try {
            for (boolean precise : new boolean[]{false, true}) {
                PlacementPreferences.set(player, precise);
                // Fence first, then the step.
                level.setBlock(pos, Blocks.OAK_FENCE.defaultBlockState(), 3);
                player.setItemInHand(hand, Materials.piece("minecraft:oak_planks", Shape.STEP, 8));
                test.assertTrue(Placement.use(player, level, hand, ceiling) == net.minecraft.world.InteractionResult.SUCCESS, "Step places into a block holding a fence");
                test.assertTrue(Materials.classify(level.getBlockState(pos).getBlock()) == Shape.STEP, "Step becomes the block's host");
                test.assertValueEqual(dev.tazer.offgrid.world.Cells.overlays(level, pos).size(), 1, "Fence is kept inside the step");
                test.assertTrue(dev.tazer.offgrid.world.Cells.overlays(level, pos).getFirst().getState().is(Blocks.OAK_FENCE), "Kept piece is the fence");
                test.assertValueEqual(MaterialStacks.units(player.getMainHandItem()), 6, "Step spends one piece");
                // A second step that still leaves room for the fence joins the host.
                var otherCorner = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX() + .25, pos.getY() + 1, pos.getZ() + .75), Direction.DOWN, pos.above(), false);
                var planned = Placement.plan(level, player, player.getMainHandItem(), otherCorner);
                if (planned != null) {
                    test.assertTrue(Placement.use(player, level, hand, otherCorner) == net.minecraft.world.InteractionResult.SUCCESS, "Second piece joins a step with a fence");
                    test.assertValueEqual(dev.tazer.offgrid.world.Cells.overlays(level, pos).size(), 1, "Fence survives the second piece");
                }
                // Any shape that leaves part of the fence visible shares the block: a lone corner,
                // then a second material, stored in a carrier Offgrid sees through its cells.
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(pos, Blocks.OAK_FENCE.defaultBlockState(), 3);
                player.setItemInHand(hand, Materials.piece("minecraft:oak_planks", Shape.CORNER, 8));
                test.assertTrue(Placement.use(player, level, hand, ceiling) == net.minecraft.world.InteractionResult.SUCCESS, "Corner shares a block with a fence");
                player.setItemInHand(hand, Materials.piece("minecraft:stone_bricks", Shape.CORNER, 8));
                test.assertTrue(Placement.use(player, level, hand, otherCorner) == net.minecraft.world.InteractionResult.SUCCESS, "Second material joins the fence block");
                test.assertTrue(level.getBlockEntity(pos) instanceof MultipartEntity, "Mixed pieces with a fence use a carrier");
                test.assertValueEqual(dev.tazer.offgrid.world.Cells.overlays(level, pos).size(), 1, "Fence is kept inside the carrier");
                var kept = dev.tazer.offgrid.world.Cells.overlays(level, pos).getFirst();
                test.assertTrue(dev.tazer.offgrid.world.Blocklogging.at(level, pos, kept) != null, "Offgrid sees the carrier as the fence's host");
                // A full block would hide the fence completely, so it is refused instead of destroying it.
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                for (var overlay : dev.tazer.offgrid.world.Cells.overlays(level, pos)) dev.tazer.offgrid.world.Overlays.remove(level, pos, overlay, false);
                level.setBlock(pos, Blocks.OAK_FENCE.defaultBlockState(), 3);
                player.setItemInHand(hand, Materials.piece("minecraft:oak_planks", Shape.BLOCK, 8));
                test.assertTrue(Placement.use(player, level, hand, ceiling) != net.minecraft.world.InteractionResult.SUCCESS, "Full block cannot share a fence's block");
                test.assertTrue(level.getBlockState(pos).is(Blocks.OAK_FENCE) && MaterialStacks.units(player.getMainHandItem()) == 8, "Refused block leaves fence and material");
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
            // Fence after the pieces: a carrier with any cells hosts a newly blocklogged fence.
            String[] mixed = new String[8];
            for (int i : new int[]{0, 1, 2, 4, 5, 6, 7}) mixed[i] = i == 0 ? "minecraft:stone_bricks" : "minecraft:oak_planks";
            Placement.store(level, pos, new dev.jensh.buildingpalette.core.Cells(mixed), false);
            var post = Blocks.OAK_FENCE.defaultBlockState();
            test.assertTrue(dev.tazer.offgrid.world.Blocklogging.at(level, pos, post, net.minecraft.world.phys.Vec3.ZERO) != null, "Fence fits a carrier missing one corner");
            test.assertTrue(dev.tazer.offgrid.world.Overlays.add(level, pos, post, net.minecraft.world.phys.Vec3.ZERO, null) != null, "Fence is blocklogged into the carrier");
            test.assertTrue(level.getBlockEntity(pos) instanceof MultipartEntity e && e.cells().occupied() == 0b11110111, "Carrier keeps its cells");
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            for (var overlay : dev.tazer.offgrid.world.Cells.overlays(level, pos)) dev.tazer.offgrid.world.Overlays.remove(level, pos, overlay, false);
            // The relaxed rules hold for native hosts and other blocklogged pieces too.
            var stair = Blocks.OAK_STAIRS.defaultBlockState();
            test.assertTrue(dev.tazer.offgrid.world.Blocklogging.of(post, stair) != null, "Lone fence post stands in a stair");
            test.assertTrue(dev.tazer.offgrid.world.Blocklogging.of(Blocks.COBBLESTONE_WALL.defaultBlockState(), Blocks.OAK_SLAB.defaultBlockState()) != null, "Wall stands in a bottom slab");
            test.assertTrue(dev.tazer.offgrid.world.Blocklogging.of(Blocks.GLASS_PANE.defaultBlockState(), stair) != null, "Pane stands in a stair");
            // Breaking the step returns its material, with the fence inside or after it is gone.
            for (boolean fenceRemoved : new boolean[]{false, true}) {
                level.setBlock(pos, Blocks.OAK_FENCE.defaultBlockState(), 3);
                player.setItemInHand(hand, Materials.piece("minecraft:stone_bricks", Shape.STEP, 2));
                test.assertTrue(Placement.use(player, level, hand, ceiling) == net.minecraft.world.InteractionResult.SUCCESS, "Stone step places around fence");
                if (fenceRemoved) for (var overlay : dev.tazer.offgrid.world.Cells.overlays(level, pos))
                    dev.tazer.offgrid.world.Overlays.remove(level, pos, overlay, false);
                level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2)).forEach(e -> e.discard());
                player.setItemInHand(hand, new ItemStack(Items.NETHERITE_PICKAXE));
                test.assertTrue(player.gameMode.destroyBlock(pos), "Step can be broken" + (fenceRemoved ? " after its fence" : " with its fence"));
                int units = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2)).stream()
                    .filter(e -> Materials.get(e.getItem()) != null && Materials.get(e.getItem()).material().equals("minecraft:stone_bricks"))
                    .mapToInt(e -> MaterialStacks.units(e.getItem())).sum();
                test.assertValueEqual(units, 2, "Broken step returns its material" + (fenceRemoved ? " after its fence" : " with its fence"));
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                for (var overlay : dev.tazer.offgrid.world.Cells.overlays(level, pos)) dev.tazer.offgrid.world.Overlays.remove(level, pos, overlay, false);
            }
        } finally { PlacementPreferences.remove(player); }
        test.succeed();
    }
    /** Storage mods describe a stack as "item x count" and rebuild stacks of any size from that.
     * However they split or merge those, a remainder must never turn into extra material. */
    public void storageDescriptionsNeverCreateMaterial(GameTestHelper test) {
        String oak = "minecraft:oak_planks";
        ItemStack stack = Materials.piece(oak, Shape.BLOCK, 20);
        test.assertValueEqual(stack.getCount(), 3, "20 units show as three blocks, the last incomplete");
        ItemStack key = stack.copyWithCount(1);
        test.assertValueEqual(MaterialStacks.units(key.copyWithCount(3)), 20, "The whole stack comes back exact");
        test.assertValueEqual(MaterialStacks.units(key.copyWithCount(6)), 40, "Two merged stacks come back exact");
        test.assertTrue(3 * MaterialStacks.units(key.copyWithCount(1)) <= 20, "Taking one at a time creates nothing");
        test.assertTrue(MaterialStacks.units(key.copyWithCount(2)) + MaterialStacks.units(key.copyWithCount(1)) <= 20, "An uneven split creates nothing");
        test.assertTrue(MaterialStacks.units(key.copyWithCount(4)) + MaterialStacks.units(key.copyWithCount(2)) <= 40, "Splitting a merged pool creates nothing");

        ItemStack rest = stack.copy(), part = rest.split(1);
        test.assertValueEqual(MaterialStacks.units(part), 8, "A split takes whole pieces");
        test.assertValueEqual(MaterialStacks.units(rest), 12, "and the remainder stays behind");
        test.assertTrue(MaterialStacks.intact(rest), "The rest is an exact stack again");
        rest.shrink(1);
        test.assertValueEqual(MaterialStacks.units(rest), 4, "Consuming a piece keeps the remainder");

        ItemStack pool = key.copyWithCount(6), most = pool.split(4);
        test.assertValueEqual(MaterialStacks.units(most) + MaterialStacks.units(pool), 40, "Splitting a merged stack keeps every unit");
        pool = key.copyWithCount(6);
        pool.shrink(2);
        test.assertValueEqual(MaterialStacks.units(pool), 24, "Shrinking a merged stack keeps both remainders");

        ItemStack planks = ScrollPayload.convert(Materials.piece(oak, Shape.CORNER, 8), 1);
        for (int i = 0; i < 12 && Materials.get(planks).shape() != Shape.BLOCK; i++) planks = ScrollPayload.convert(planks, 1);
        test.assertTrue(ItemStack.isSameItemSameComponents(planks, new ItemStack(Items.OAK_PLANKS)), "Corners reshaped to a block are ordinary planks");

        ItemStack one = Materials.piece(oak, Shape.BLOCK, 4), merged = one.copy();
        merged.grow(1);
        test.assertValueEqual(MaterialStacks.units(merged), 8, "Two half blocks merged by count are still two halves");
        test.succeed();
    }

    /** An end rod (any small Offgrid piece) and pieces share a block wherever they do not actually overlap. */
    public void offgridSmallPiecesShareCombinedBlocks(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        var player = TestPlayers.survival(test);
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() - 1.5);
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        String stone = "minecraft:stone_bricks";
        java.util.function.BiFunction<Double, Double, net.minecraft.world.phys.BlockHitResult> onCorner = (x, z) ->
            new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX() + x, pos.getY() + .5, pos.getZ() + z), Direction.UP, pos, false);
        try {
            PlacementPreferences.set(player, true);
            // Three bottom corners and one on top: a rod stands on a free bottom corner beside it.
            Placement.store(level, pos, new dev.jensh.buildingpalette.core.Cells().place(0b00010111, stone), false);
            player.setItemInHand(hand, new ItemStack(Items.END_ROD, 4));
            test.assertTrue(player.gameMode.useItemOn(player, level, player.getMainHandItem(), hand, onCorner.apply(.75, .25)).consumesAction(), "Rod stands on a bottom corner next to a higher one");
            var rods = dev.tazer.offgrid.world.Cells.overlays(level, pos);
            test.assertValueEqual(rods.size(), 1, "Rod is kept in the pieces' block");
            test.assertTrue(rods.getFirst().getState().is(Blocks.END_ROD), "Kept piece is the rod");
            test.assertValueEqual(rods.getFirst().getOffset(), new net.minecraft.world.phys.Vec3(.25, .5, -.25), "Rod is centred on the clicked half cell");
            test.assertTrue(!player.gameMode.useItemOn(player, level, player.getMainHandItem(), hand, onCorner.apply(.75, .25)).consumesAction()
                || dev.tazer.offgrid.world.Cells.overlays(level, pos).size() == 1, "A second rod cannot stand inside the first");
            dev.tazer.offgrid.world.Overlays.remove(level, pos, rods.getFirst(), false);

            // A lone corner with a rod on it: other pieces join wherever the rod is not.
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            Placement.store(level, pos, new dev.jensh.buildingpalette.core.Cells().place(0b00000001, stone), false);
            player.setItemInHand(hand, new ItemStack(Items.END_ROD, 4));
            test.assertTrue(player.gameMode.useItemOn(player, level, player.getMainHandItem(), hand, onCorner.apply(.25, .25)).consumesAction(), "Rod stands on a lone corner");
            test.assertValueEqual(dev.tazer.offgrid.world.Cells.overlays(level, pos).size(), 1, "Rod shares the corner's block");
            player.setItemInHand(hand, Materials.piece(stone, Shape.CORNER, 8));
            var floor = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX() + .75, pos.getY(), pos.getZ() + .75), Direction.UP, pos.below(), false);
            test.assertTrue(Placement.use(player, level, hand, floor) == net.minecraft.world.InteractionResult.SUCCESS, "A corner clear of the rod joins the block");
            test.assertValueEqual(Placement.existing(level, pos).occupied(), 0b00100001, "Both corners are stored");
            test.assertValueEqual(dev.tazer.offgrid.world.Cells.overlays(level, pos).size(), 1, "Rod survives the new corner");
            test.assertTrue(Placement.plan(level, player, player.getMainHandItem(), onCorner.apply(.25, .25)) == null, "A corner inside the rod is refused");
            // A rod reaching in from the block below blocks only the cells it reaches.
            player.setItemInHand(hand, Materials.piece(stone, Shape.SLAB, 8));
            var above = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX() + .75, pos.getY() + 2, pos.getZ() + .75), Direction.DOWN, pos.above(2), false);
            level.setBlock(pos.above(2), Blocks.STONE.defaultBlockState(), 3);
            test.assertTrue(Placement.use(player, level, hand, above) == net.minecraft.world.InteractionResult.SUCCESS, "A top slab above the rod's block is clear of it");
            var beside = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX() + .75, pos.getY() + 1.5, pos.getZ() + .75), Direction.DOWN, pos.above(), false);
            player.setItemInHand(hand, Materials.piece(stone, Shape.CORNER, 8));
            test.assertTrue(Placement.use(player, level, hand, beside) == net.minecraft.world.InteractionResult.SUCCESS, "A corner beside the rod's upper half is placed");
            player.setItemInHand(hand, Materials.piece(stone, Shape.SLAB, 8));
            test.assertTrue(Placement.plan(level, player, player.getMainHandItem(), beside) == null, "A slab across the rod's upper half is refused");
            test.succeed();
        } finally {
            PlacementPreferences.set(player, false);
            for (BlockPos cell : new BlockPos[]{pos, pos.above()}) for (var overlay : java.util.List.copyOf(dev.tazer.offgrid.world.Cells.overlays(level, cell))) dev.tazer.offgrid.world.Overlays.remove(level, cell, overlay, false);
        }
    }

    public void overlappingPiecesMerge(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        var player = TestPlayers.survival(test);
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() - 1.5);
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        var floor = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX() + .25, pos.getY(), pos.getZ() + .25), Direction.UP, pos.below(), false);
        String oak = "minecraft:oak_planks";
        int alongZ = dev.jensh.buildingpalette.core.Cells.mask(Shape.STEP, 0, 2), alongX = dev.jensh.buildingpalette.core.Cells.mask(Shape.STEP, 0, 0);
        try {
            PlacementPreferences.set(player, true);
            player.setYRot(0); player.setYHeadRot(0);
            player.setItemInHand(hand, Materials.piece(oak, Shape.STEP, 4));
            test.assertTrue(Placement.use(player, level, hand, floor) == net.minecraft.world.InteractionResult.SUCCESS, "First step places");
            test.assertValueEqual(Placement.existing(level, pos).occupied(), alongZ, "First step runs along the facing");
            // The same step again overlaps all of it: refused.
            test.assertTrue(Placement.use(player, level, hand, floor) != net.minecraft.world.InteractionResult.SUCCESS, "Identical step is refused");
            // A perpendicular step overlaps one cell: it merges into an L and costs only the added cell.
            player.setYRot(90); player.setYHeadRot(90);
            test.assertTrue(Placement.use(player, level, hand, floor) == net.minecraft.world.InteractionResult.SUCCESS, "Perpendicular step merges");
            test.assertValueEqual(Placement.existing(level, pos).occupied(), alongZ | alongX, "Steps merge into an L");
            test.assertValueEqual(MaterialStacks.units(player.getMainHandItem()), 4 - 2 - 1, "Merged step costs only its added cell");
            // A slab covering the whole L overlaps all existing material: refused.
            player.setItemInHand(hand, Materials.piece(oak, Shape.SLAB, 4));
            test.assertTrue(Placement.use(player, level, hand, floor) != net.minecraft.world.InteractionResult.SUCCESS, "Piece containing all existing material is refused");
            // Overlap must be the same material.
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            player.setYRot(0); player.setYHeadRot(0);
            player.setItemInHand(hand, Materials.piece(oak, Shape.STEP, 4));
            Placement.use(player, level, hand, floor);
            player.setYRot(90); player.setYHeadRot(90);
            player.setItemInHand(hand, Materials.piece("minecraft:stone_bricks", Shape.STEP, 4));
            test.assertTrue(Placement.use(player, level, hand, floor) != net.minecraft.world.InteractionResult.SUCCESS, "Overlap with another material is refused");
            // Unit rule: partial overlaps only.
            var step = new dev.jensh.buildingpalette.core.Cells().place(alongZ, oak);
            test.assertValueEqual(Placement.added(step, alongX, oak), alongX & ~alongZ, "Partial overlap adds the rest");
            test.assertValueEqual(Placement.added(step, alongZ, oak), 0, "Full overlap of new piece adds nothing");
            test.assertValueEqual(Placement.added(step, 0b00110011, oak), 0, "Overlap equal to existing material adds nothing");
            test.assertValueEqual(Placement.added(step, ~alongZ & 255 & 0b00001100, oak), 0b00001100 & ~alongZ, "Disjoint piece adds itself");
        } finally { PlacementPreferences.remove(player); level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); }
        test.succeed();
    }
    public void shiftClickMergesNativeBlocksIntoPartialShapes(GameTestHelper test) {
        var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var chest = new SimpleContainer(27);
        var menu = new net.minecraft.world.inventory.ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x3, 1, player.getInventory(), chest, 3);
        // Fuel (planks) and non-fuel (stone bricks) materials, each from a plain
        // native full-block stack as found in a chest.
        for (var material : new Item[]{Items.OAK_PLANKS, Items.STONE_BRICKS}) {
            String key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(material).toString();
            for (Shape shape : new Shape[]{Shape.CORNER, Shape.STEP, Shape.SLAB, Shape.STAIR}) for (int slot : new int[]{20, 4}) {
                player.getInventory().clearContent();
                chest.clearContent();
                chest.setItem(0, new ItemStack(material, 64));
                player.getInventory().setItem(slot, Materials.piece(key, shape, 100));
                menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, player);
                ItemStack merged = player.getInventory().getItem(slot);
                test.assertTrue(Materials.get(merged).shape() == shape, "Shift-clicked " + key + " keeps destination " + shape);
                test.assertValueEqual(MaterialStacks.units(merged), 512, "Shift-clicked " + key + " fills partial " + shape);
                int total = 0;
                for (int i = 0; i < 36; i++) total += MaterialStacks.units(player.getInventory().getItem(i));
                test.assertValueEqual(total, 612, "Shift-click " + key + " into " + shape + " conserves material");
                test.assertTrue(chest.getItem(0).isEmpty(), "Shift-clicked " + key + " leaves the chest");
            }
        }
        // The player's own inventory screen moves main inventory <-> hotbar the same way.
        var inventory = player.inventoryMenu;
        String key = "minecraft:oak_planks";
        for (Shape shape : new Shape[]{Shape.CORNER, Shape.STEP, Shape.SLAB, Shape.STAIR}) for (boolean toHotbar : new boolean[]{true, false}) {
            player.getInventory().clearContent();
            int from = toHotbar ? 20 : 3, to = toHotbar ? 4 : 25;
            player.getInventory().setItem(from, new ItemStack(Items.OAK_PLANKS, 64));
            player.getInventory().setItem(to, Materials.piece(key, shape, 100));
            inventory.clicked(from < 9 ? 36 + from : from, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, player);
            ItemStack merged = player.getInventory().getItem(to);
            test.assertTrue(Materials.get(merged) != null && Materials.get(merged).shape() == shape, "Inventory shift-click keeps destination " + shape);
            test.assertValueEqual(MaterialStacks.units(merged), 512, "Inventory shift-click " + (toHotbar ? "into hotbar" : "into main") + " fills partial " + shape);
            int total = 0;
            for (int i = 0; i < 36; i++) total += MaterialStacks.units(player.getInventory().getItem(i));
            test.assertValueEqual(total, 612, "Inventory shift-click conserves material");
        }
        test.succeed();
    }
    public void slabRoofMatchesNativeLightBlocking(GameTestHelper test) {
        var level = test.getLevel();
        for (int x = 0; x <= 4; x++) for (int y = 0; y <= 4; y++) for (int z = 0; z <= 4; z++) {
            var pos = test.absolutePos(new BlockPos(x, y, z));
            boolean wall = y == 0 || x == 0 || x == 4 || z == 0 || z == 4;
            level.setBlock(pos, wall ? Blocks.STONE.defaultBlockState() : y == 4 ? Blocks.OAK_SLAB.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
        }
        var sample = test.absolutePos(new BlockPos(2, 3, 2));
        level.setBlock(test.absolutePos(new BlockPos(2, 5, 2)), Blocks.GLOWSTONE.defaultBlockState(), 3);
        test.runAfterDelay(15, () -> {
            test.assertValueEqual(level.getBrightness(net.minecraft.world.level.LightLayer.SKY, test.absolutePos(new BlockPos(1, 5, 1))), 15, "Roof test is exposed to skylight");
            test.assertValueEqual(level.getBrightness(net.minecraft.world.level.LightLayer.SKY, sample), 0, "Native slab roof blocks skylight");
            test.assertValueEqual(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, sample), 0, "Native slab roof blocks overhead block light");
            for (int x = 1; x <= 3; x++) for (int z = 1; z <= 3; z++) {
                var pos = test.absolutePos(new BlockPos(x, 4, z));
                level.setBlock(pos, BuildingPalette.MULTIPART.get().defaultBlockState(), 3);
                ((MultipartEntity)level.getBlockEntity(pos)).setCells(new Cells().place(51, "minecraft:oak_planks"));
            }
            test.runAfterDelay(15, () -> {
                test.assertValueEqual(level.getBrightness(net.minecraft.world.level.LightLayer.SKY, sample), 0, "Multipart bottom slab roof blocks skylight");
                test.assertValueEqual(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, sample), 0, "Multipart bottom slab roof blocks overhead block light");
                for (int x = 1; x <= 3; x++) for (int z = 1; z <= 3; z++) {
                    var pos = test.absolutePos(new BlockPos(x, 4, z));
                    ((MultipartEntity)level.getBlockEntity(pos)).setCells(new Cells().place(68, "minecraft:oak_planks").place(136, "minecraft:stone_bricks"));
                }
                test.runAfterDelay(15, () -> {
                    test.assertValueEqual(level.getBrightness(net.minecraft.world.level.LightLayer.SKY, sample), 0, "Mixed top slab roof blocks skylight");
                    test.assertValueEqual(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, sample), 0, "Mixed top slab roof blocks overhead block light");
                    test.succeed();
                });
            });
        });
    }
    public void everyShapeMergesByMaterial(GameTestHelper test) {
        var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var chest = new SimpleContainer(27);
        var menu = new net.minecraft.world.inventory.ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x3, 1, player.getInventory(), chest, 3);
        var palette = Materials.palette(new ItemStack(Items.OAK_PLANKS));
        for (ItemStack from : palette) for (ItemStack to : palette) for (int[] amounts : new int[][]{{17, 65}, {448, 65}, {512, 17}, {3, 1}}) {
            chest.setItem(0, MaterialStacks.withUnits(to, amounts[0]));
            menu.setCarried(MaterialStacks.withUnits(from, amounts[1]));
            menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
            int total = amounts[0] + amounts[1], expected = Math.min(total, 512);
            test.assertValueEqual(MaterialStacks.units(chest.getItem(0)), expected, "Every shape pairing fills receiver by material");
            test.assertValueEqual(MaterialStacks.units(menu.getCarried()), total - expected, "Only overflow remains on cursor");
            test.assertTrue(Materials.get(chest.getItem(0)).shape() == Materials.get(to).shape(), "Static stack keeps its shape");
            if (total <= 512) test.assertTrue(menu.getCarried().isEmpty(), "Fully merged cursor is empty");
            else test.assertTrue(Materials.get(menu.getCarried()).shape() == Materials.get(from).shape(), "Overflow keeps moved shape");
        }
        test.succeed();
    }
    public void nativePlacementAndPreferences(GameTestHelper test) {
        var level = test.getLevel();
        var support = test.absolutePos(new BlockPos(2, 2, 2));
        var target = support.east();
        level.setBlock(support, Blocks.STONE.defaultBlockState(), 3);
        var player = TestPlayers.survival(test);
        player.setPos(support.getX() + 2, support.getY(), support.getZ() + 2);
        try {
            test.assertTrue(!PlacementPreferences.precise(player), "Placement defaults to native");
            for (Direction facing : Direction.Plane.HORIZONTAL) for (double height : new double[]{.25, .75}) for (double side : new double[]{.25, .75}) {
                level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
                player.setYRot(facing.toYRot());
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_STAIRS, 64));
                var hit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(support.getX() + 1, support.getY() + height, support.getZ() + side), Direction.EAST, support, false);
                test.assertTrue(Placement.use(player, level, net.minecraft.world.InteractionHand.MAIN_HAND, hit) == net.minecraft.world.InteractionResult.PASS, "Native stairs use original item placement");
                Items.OAK_STAIRS.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
                var state = level.getBlockState(target);
                test.assertTrue(state.is(Blocks.OAK_STAIRS), "Native stair block remains native");
                test.assertTrue(state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING) == facing, "Native stair faces player direction");
                var half = height > .5 ? net.minecraft.world.level.block.state.properties.Half.TOP : net.minecraft.world.level.block.state.properties.Half.BOTTOM;
                test.assertTrue(state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HALF) == half, "Native stair half follows vanilla hit height");
                test.assertValueEqual(player.getMainHandItem().getCount(), 63, "Native placement spends one piece");
            }
            // A second slab must still be consumed despite CNM's doubled-shape shortcut.
            level.setBlock(target, Blocks.OAK_SLAB.defaultBlockState(), 3);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_SLAB, 2));
            var slabHit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(target.getX() + .25, target.getY() + .5, target.getZ() + .25), Direction.UP, target, false);
            Items.OAK_SLAB.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, slabHit));
            test.assertValueEqual(player.getMainHandItem().getCount(), 1, "Double slab spends the second slab");
            level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, MaterialStacks.withUnits(new ItemStack(Items.OAK_STAIRS), 1));
            var reservedHit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(support.getX() + 1, support.getY() + .25, support.getZ() + .25), Direction.EAST, support, false);
            Items.OAK_STAIRS.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, reservedHit));
            test.assertTrue(level.getBlockState(target).isAir(), "Reserved fraction cannot place a native stair");
            test.assertValueEqual(MaterialStacks.units(player.getMainHandItem()), 1, "Reserved material is preserved");
            level.setBlock(target, Blocks.CHEST.defaultBlockState(), 3);
            var chestHit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(target.getX() + .5, target.getY() + .875, target.getZ() + .5), Direction.UP, target, false);
            test.assertTrue(Placement.use(player, level, net.minecraft.world.InteractionHand.MAIN_HAND, chestHit) == net.minecraft.world.InteractionResult.PASS, "Reserved material does not block chest interaction");
            level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
            PlacementPreferences.set(player, true);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_STAIRS, 2));
            var hit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(support.getX() + 1, support.getY() + .25, support.getZ() + .25), Direction.EAST, support, false);
            test.assertTrue(Placement.use(player, level, net.minecraft.world.InteractionHand.MAIN_HAND, hit) == net.minecraft.world.InteractionResult.SUCCESS, "Precise placement remains available");
            test.assertTrue(level.getBlockEntity(target) instanceof MultipartEntity, "Precise placement makes explicit multipart stairs");
        } finally { PlacementPreferences.remove(player); }
        test.succeed();
    }
    public void noAirPlacementInEitherMode(GameTestHelper test) {
        var level = test.getLevel();
        var air = test.absolutePos(new BlockPos(2, 3, 2));
        var player = TestPlayers.survival(test);
        player.setPos(air.getX() + 1, air.getY(), air.getZ() + 1);
        var point = net.minecraft.world.phys.Vec3.atCenterOf(air);
        try {
            for (boolean precise : new boolean[]{false, true}) for (ItemStack stack : new ItemStack[]{new ItemStack(Items.OAK_STAIRS, 2), Materials.piece("minecraft:oak_planks", Shape.CORNER, 2)}) {
                PlacementPreferences.set(player, precise);
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
                for (var hit : new net.minecraft.world.phys.BlockHitResult[]{new net.minecraft.world.phys.BlockHitResult(point, Direction.UP, air, false), net.minecraft.world.phys.BlockHitResult.miss(point, Direction.UP, air)}) {
                    test.assertTrue(Placement.plan(level, player, stack, hit) == null, "Air and miss have no preview plan");
                    test.assertTrue(Placement.use(player, level, net.minecraft.world.InteractionHand.MAIN_HAND, hit) == net.minecraft.world.InteractionResult.FAIL, "Air placement rejected");
                    test.assertValueEqual(stack.getCount(), 2, "Rejected placement spends nothing");
                    test.assertTrue(level.getBlockState(air).isAir(), "Rejected placement creates no block");
                }
            }
        } finally { PlacementPreferences.remove(player); }
        test.succeed();
    }
    public void randomizedNativeTransfers(GameTestHelper test) {
        var random = new java.util.Random(837264);
        var storage = new SimpleContainer(6);
        storage.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));
        for (int iteration = 0; iteration < 2000; iteration++) {
            int sourceIndex = random.nextInt(6), target = random.nextInt(6);
            if (sourceIndex == target) continue;
            ItemStack source = storage.getItem(sourceIndex);
            if (source.isEmpty()) continue;
            if (random.nextBoolean()) {
                var palette = Materials.palette(source);
                source = MaterialStacks.withUnits(palette.get(random.nextInt(palette.size())), MaterialStacks.units(source));
                storage.setItem(sourceIndex, source);
            }
            InventoryTransactions.insert(new Slot(storage, target, 0, 0), source, random.nextInt(513));
            int units = 0;
            for (int i = 0; i < 6; i++) units += MaterialStacks.units(storage.getItem(i));
            test.assertValueEqual(units, 512, "Random inventory operations conserve material, iteration " + iteration);
        }
        test.succeed();
    }

    public void eightCornersBecomeBlock(GameTestHelper test) {
        var level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(pos.getX() + 2, pos.getY(), pos.getZ() + 2);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, Materials.piece("minecraft:oak_planks", Shape.CORNER, 8));
        for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) for (int x = 0; x < 2; x++) {
            var point = new net.minecraft.world.phys.Vec3(pos.getX() + x * .5 + .25, pos.getY() + y * .5, pos.getZ() + z * .5 + .25);
            var hit = new net.minecraft.world.phys.BlockHitResult(point, Direction.UP, y == 0 ? pos.below() : pos, false);
            var result = Placement.use(player, level, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            test.assertTrue(result == net.minecraft.world.InteractionResult.SUCCESS, "Corner placement succeeds");
        }
        test.assertTrue(level.getBlockState(pos).is(Blocks.OAK_PLANKS), "Eight same-material corners become ordinary full block");
        test.assertTrue(player.getMainHandItem().isEmpty(), "Eight corners consume exactly one block");
        test.succeed();
    }

    public void connectedMining(GameTestHelper test) {
        var level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(pos, BuildingPalette.MULTIPART.get().defaultBlockState(), 3);
        var entity = (MultipartEntity)level.getBlockEntity(pos);
        entity.setCells(new Cells().place(7, "minecraft:oak_planks").place(128, "minecraft:stone_bricks"));
        var player = TestPlayers.survival(test);
        player.setPos(pos.getX() + 2, pos.getY(), pos.getZ() - 2);
        var aim = new net.minecraft.world.phys.Vec3(pos.getX() + .25, pos.getY() + .25, pos.getZ() + .25).subtract(player.getEyePosition());
        player.setYRot((float)(Math.toDegrees(Math.atan2(aim.z, aim.x)) - 90));
        player.setXRot((float)-Math.toDegrees(Math.atan2(aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z))));
        player.setYHeadRot(180); // Deliberately stale, as before the next server entity tick.
        test.assertTrue(player.gameMode.destroyBlock(pos), "Mining handler accepts target");
        test.assertValueEqual(((MultipartEntity)level.getBlockEntity(pos)).cells().occupied(), 128, "All connected oak removed, stone remains");
        var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(pos).inflate(1));
        test.assertValueEqual(drops.stream().mapToInt(e -> MaterialStacks.units(e.getItem())).sum(), 3, "Mining refunds exact L-shape volume");
        test.succeed();
    }

    public void nativeShapeDrops(GameTestHelper test) {
        BlockPos pos = test.absolutePos(new BlockPos(1, 1, 1));
        for (var block : new net.minecraft.world.level.block.Block[]{Blocks.OAK_SLAB, Blocks.OAK_STAIRS}) {
            var state = block.defaultBlockState();
            test.getLevel().setBlock(pos, state, 3);
            var drops = net.minecraft.world.level.block.Block.getDrops(state, test.getLevel(), pos, null);
            int expected = block == Blocks.OAK_SLAB ? 4 : 6;
            test.assertValueEqual(drops.stream().mapToInt(MaterialStacks::units).sum(), expected, "Existing native shape drops its physical volume");
        }
        // Clutter No More shapes have no loot table of their own.
        for (Shape shape : new Shape[]{Shape.STEP, Shape.VERTICAL_SLAB, Shape.VERTICAL_STEP})
            for (String material : new String[]{"minecraft:oak_planks", "minecraft:stone_bricks"}) {
                ItemStack item = Materials.piece(material, shape, shape.cost);
                if (!(item.getItem() instanceof BlockItem block)) continue;
                var state = block.getBlock().defaultBlockState();
                test.getLevel().setBlock(pos, state, 3);
                var pickaxe = new ItemStack(Items.NETHERITE_PICKAXE);
                var drops = net.minecraft.world.level.block.Block.getDrops(state, test.getLevel(), pos, null, null, pickaxe);
                test.assertValueEqual(drops.stream().mapToInt(MaterialStacks::units).sum(), shape.cost, "Clutter No More " + shape + " of " + material + " drops its volume");
            }
        for (var shape : net.minecraft.world.level.block.state.properties.StairsShape.values()) {
            var state = Blocks.OAK_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.STAIRS_SHAPE, shape);
            test.getLevel().setBlock(pos, state, 3);
            var drops = net.minecraft.world.level.block.Block.getDrops(state, test.getLevel(), pos, null);
            test.assertValueEqual(drops.stream().mapToInt(MaterialStacks::units).sum(), 6, "Auto-connected stair keeps one stair's material value");
        }
        test.succeed();
    }

    public void repeatedCrafting(GameTestHelper test) {
        var level = test.getLevel();
        java.util.List<ItemStack> grid = new java.util.ArrayList<>();
        for (int i = 0; i < 9; i++) grid.add(i == 4 ? ItemStack.EMPTY : new ItemStack(Items.OAK_PLANKS, 2));
        for (int attempt = 0; attempt < 2; attempt++) {
            var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, grid);
            var recipe = level.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level).orElseThrow();
            // Other mods may replace the vanilla chest with a wood-specific one.
            test.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(recipe.value().assemble(input, test.getLevel().registryAccess()).getItem()).getPath().endsWith("chest"), "Repeated plank ring produces chest");
            for (ItemStack stack : grid) if (!stack.isEmpty()) stack.shrink(1);
        }
        for (int i = 0; i < 9; i++) grid.set(i, i == 4 ? ItemStack.EMPTY : new ItemStack(i == 1 || i == 7 ? Items.OAK_SLAB : Items.OAK_PLANKS));
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, grid);
        var barrel = level.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level).orElseThrow();
        test.assertTrue(barrel.value().assemble(input, test.getLevel().registryAccess()).is(Items.BARREL), "Selected slabs produce vanilla barrel");
        test.assertTrue(!dev.tazer.clutternomore.recipe.RecipeFixes.isEnabled(), "Broad shape ingredient matching disabled");
        test.succeed();
    }

    public void inventoryGestures(GameTestHelper test) {
        var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var chest = new SimpleContainer(27);
        var menu = new net.minecraft.world.inventory.ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x3, 1, player.getInventory(), chest, 3);
        chest.setItem(0, MaterialStacks.withUnits(new ItemStack(Items.OAK_STAIRS), 512));
        menu.clicked(0, 1, net.minecraft.world.inventory.ClickType.PICKUP, player);
        test.assertValueEqual(MaterialStacks.units(chest.getItem(0)) + MaterialStacks.units(menu.getCarried()), 512, "Right click conserves remainder");
        menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        test.assertValueEqual(MaterialStacks.units(chest.getItem(0)), 512, "Cursor merges back by balance");
        menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        menu.clicked(-999, 0, net.minecraft.world.inventory.ClickType.QUICK_CRAFT, player);
        menu.clicked(1, 1, net.minecraft.world.inventory.ClickType.QUICK_CRAFT, player);
        menu.clicked(2, 1, net.minecraft.world.inventory.ClickType.QUICK_CRAFT, player);
        menu.clicked(-999, 2, net.minecraft.world.inventory.ClickType.QUICK_CRAFT, player);
        int total = MaterialStacks.units(menu.getCarried());
        for (int i = 0; i < 27; i++) total += MaterialStacks.units(chest.getItem(i));
        test.assertValueEqual(total, 512, "Drag conserves reserved fraction");
        ItemStack dropA = new ItemStack(Items.OAK_SLAB, 100), dropB = new ItemStack(Items.OAK_PLANKS, 10);
        test.assertTrue(net.minecraft.world.entity.item.ItemEntity.areMergable(dropA, dropB), "Different shapes can merge as dropped material");
        ItemStack merged = net.minecraft.world.entity.item.ItemEntity.merge(dropA, dropB, 64);
        test.assertValueEqual(MaterialStacks.units(merged) + MaterialStacks.units(dropB), 480, "Dropped merge conserves material above 64 pieces");
        test.succeed();
    }

    public void furnaceRemainder(GameTestHelper test) {
        BlockPos pos = test.absolutePos(new BlockPos(1, 1, 1));
        test.getLevel().setBlock(pos, Blocks.FURNACE.defaultBlockState(), 3);
        var furnace = (net.minecraft.world.level.block.entity.FurnaceBlockEntity)test.getLevel().getBlockEntity(pos);
        furnace.setItem(0, new ItemStack(Items.COBBLESTONE));
        furnace.setItem(1, MaterialStacks.withUnits(new ItemStack(Items.OAK_STAIRS), 2));
        test.runAfterDelay(5, () -> {
            test.assertValueEqual(MaterialStacks.units(furnace.getItem(1)), 2, "Incomplete fuel cannot burn");
            furnace.setItem(1, new ItemStack(Items.OAK_STAIRS));
            // Wake furnaces that optimisation mods put to sleep while they cannot burn.
            furnace.setChanged();
            test.runAfterDelay(10, () -> {
                test.assertTrue(furnace.getItem(1).isEmpty(), "Whole selected stair burns normally");
                test.succeed();
            });
        });
    }

    public void materialStacks(GameTestHelper test) {
        ItemStack seed = new ItemStack(Items.OAK_PLANKS, 64);
        test.assertTrue(Materials.get(seed) != null, "Oak family discovered after reload");
        test.assertValueEqual(MaterialStacks.units(seed), 512, "Initial units");
        for (ItemStack icon : Materials.palette(seed)) {
            ItemStack stack = MaterialStacks.withUnits(icon, 512);
            test.assertValueEqual(MaterialStacks.units(stack), 512, "Scrolling balance");
            test.assertTrue(stack.getCount() <= stack.getMaxStackSize(), "Capacity permits selected shape");
            var ops = RegistryOps.create(NbtOps.INSTANCE, test.getLevel().registryAccess());
            var encoded = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
            ItemStack decoded = ItemStack.CODEC.parse(ops, encoded).getOrThrow();
            test.assertValueEqual(MaterialStacks.units(decoded), 512, "Persistent count and remainder");
            int before = MaterialStacks.units(stack);
            ItemStack split = stack.split(1);
            test.assertValueEqual(MaterialStacks.units(stack) + MaterialStacks.units(split), before, "Split conservation");
        }
        ItemStack slab = new ItemStack(Items.OAK_SLAB, 128);
        test.assertValueEqual(slab.getMaxStackSize(), 128, "Slab capacity");
        SimpleContainer storage = new SimpleContainer(2);
        storage.setItem(0, slab);
        test.assertValueEqual(storage.getItem(0).getCount(), 128, "Container doesn't truncate count");
        test.succeed();
    }

    public void remainderTransfers(GameTestHelper test) {
        ItemStack stairs = MaterialStacks.withUnits(new ItemStack(Items.OAK_STAIRS), 512);
        MaterialStacks.spend(stairs, 85);
        test.assertValueEqual(MaterialStacks.units(stairs), 2, "Stair remainder");
        test.assertValueEqual(MaterialStacks.available(stairs), 0, "Remainder is not a stair");
        test.assertTrue(!Ingredient.of(Items.OAK_STAIRS).test(stairs), "Partial stair cannot satisfy recipe");
        SimpleContainer chest = new SimpleContainer(1);
        chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 63));
        int moved = InventoryTransactions.insert(new Slot(chest, 0, 0, 0), stairs, 512);
        test.assertValueEqual(moved, 2, "Fractional material moves by units");
        test.assertValueEqual(MaterialStacks.units(chest.getItem(0)), 506, "Destination keeps selected shape and remainder");
        test.assertTrue(stairs.isEmpty(), "Transferred source empty");
        var limited = new Slot(new SimpleContainer(1), 0, 0, 0) {
            @Override public int getMaxStackSize() { return 1; }
        };
        ItemStack planks = new ItemStack(Items.OAK_PLANKS, 64);
        InventoryTransactions.insert(limited, planks, 512);
        test.assertValueEqual(limited.getItem().getCount(), 1, "Special slot limit respected");
        test.assertValueEqual(MaterialStacks.units(limited.getItem()) + MaterialStacks.units(planks), 512, "Limited slot conserves overflow");
        test.assertTrue(!ItemStack.isSameItemSameComponents(new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.OAK_SLAB)), "Ingredients use selected shape");
        test.succeed();
    }

    public void multipartPersistence(GameTestHelper test) {
        BlockPos pos = test.absolutePos(new BlockPos(1, 1, 1));
        test.getLevel().setBlock(pos, BuildingPalette.MULTIPART.get().defaultBlockState(), 3);
        MultipartEntity entity = (MultipartEntity)test.getLevel().getBlockEntity(pos);
        Cells cells = new Cells().place(7, "minecraft:oak_planks").place(128, "minecraft:stone_bricks");
        entity.setCells(cells);
        var tag = entity.saveWithFullMetadata(test.getLevel().registryAccess());
        var restored = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, entity.getBlockState(), tag, test.getLevel().registryAccess());
        test.assertTrue(restored instanceof MultipartEntity, "Multipart block entity loads");
        test.assertValueEqual(((MultipartEntity)restored).cells().connected(0), 7, "Connected mining survives save");
        test.assertValueEqual(((MultipartEntity)restored).cells().at(7), "minecraft:stone_bricks", "Mixed material survives save");
        test.assertTrue(entity.getBlockState().getCollisionShape(test.getLevel(), pos).toAabbs().size() > 0, "Pieces have collision");
        test.succeed();
    }
}
