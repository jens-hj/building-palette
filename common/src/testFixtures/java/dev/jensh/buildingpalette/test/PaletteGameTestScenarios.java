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
        var player = (net.minecraft.server.level.ServerPlayer)test.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
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
                        test.assertTrue(level.getBlockEntity(pos) instanceof MultipartEntity, "Step placement uses Building Palette carrier from first part");
                        test.assertTrue(java.util.Arrays.equals(((MultipartEntity)level.getBlockEntity(pos)).cells().copy(), expected.copy()), "Step positions and materials match exactly");
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
        var player = (net.minecraft.server.level.ServerPlayer)test.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
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
        var player = (net.minecraft.server.level.ServerPlayer)test.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
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
        ItemStack second = PickBlock.pick(player, pos, test.getLevel().getBlockState(pos), false);
        test.assertTrue(Materials.get(second).shape() == Shape.STAIR, "Second pick matches clicked shape");
        test.assertValueEqual(MaterialStacks.units(second), 80, "Second pick preserves fractional balance");
        test.assertTrue(second.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME).equals(stored.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME)), "Pick preserves custom components");
        player.getInventory().clearContent();
        test.assertTrue(PickBlock.pick(player, pos, test.getLevel().getBlockState(pos), false).isEmpty(), "Survival pick cannot create missing material");
        test.succeed();
    }
    public void shiftClickMergesShapesBeforeEmptySlots(GameTestHelper test) {
        var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var chest = new SimpleContainer(27);
        var menu = new net.minecraft.world.inventory.ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x3, 1, player.getInventory(), chest, 3);
        chest.setItem(0, Materials.piece("minecraft:oak_planks", Shape.CORNER, 65));
        player.getInventory().setItem(8, Materials.piece("minecraft:oak_planks", Shape.STAIR, 448));
        player.getInventory().setItem(7, Materials.piece("minecraft:oak_planks", Shape.BLOCK, 500));
        menu.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, player);
        test.assertTrue(chest.getItem(0).isEmpty(), "Shift click moves the whole available source");
        test.assertValueEqual(MaterialStacks.units(player.getInventory().getItem(8)), 512, "Shift click fills existing stairs first");
        test.assertTrue(Materials.get(player.getInventory().getItem(8)).shape() == Shape.STAIR, "Shift target keeps stair shape");
        test.assertValueEqual(MaterialStacks.units(player.getInventory().getItem(7)), 501, "Shift remainder merges into another existing shape");
        test.assertTrue(player.getInventory().getItem(0).isEmpty(), "No empty slot used while compatible stacks have room");
        chest.setItem(0, Materials.piece("minecraft:oak_planks", Shape.SLAB, 500));
        chest.setItem(1, Materials.piece("minecraft:oak_planks", Shape.VERTICAL_SLAB, 500));
        player.getInventory().setItem(9, Materials.piece("minecraft:oak_planks", Shape.CORNER, 32));
        menu.clicked(27, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, player);
        test.assertValueEqual(MaterialStacks.units(chest.getItem(0)), 512, "Reverse shift click fills slab stack");
        test.assertValueEqual(MaterialStacks.units(chest.getItem(1)), 512, "Reverse shift click fills vertical slabs");
        test.assertValueEqual(MaterialStacks.units(chest.getItem(2)), 8, "Only overflow goes into an empty slot");
        test.assertTrue(player.getInventory().getItem(9).isEmpty(), "Reverse shift click empties source");
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
        player.getInventory().setItem(8, Materials.piece("minecraft:oak_planks", Shape.STAIR, 500));
        chest.setItem(0, Materials.piece("minecraft:oak_planks", Shape.CORNER, 32));
        menu.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, player);
        test.assertValueEqual(MaterialStacks.units(player.getInventory().getItem(8)), 512, "Full inventory still fills compatible material stack");
        test.assertValueEqual(MaterialStacks.units(chest.getItem(0)), 20, "Untransferable overflow remains in original slot");
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
            menu.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
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
        var player = (net.minecraft.server.level.ServerPlayer)test.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
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
        var player = (net.minecraft.server.level.ServerPlayer)test.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
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
        var player = (net.minecraft.server.level.ServerPlayer)test.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
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
            test.assertTrue(recipe.value().assemble(input).is(Items.CHEST), "Repeated plank ring produces chest");
            for (ItemStack stack : grid) if (!stack.isEmpty()) stack.shrink(1);
        }
        for (int i = 0; i < 9; i++) grid.set(i, i == 4 ? ItemStack.EMPTY : new ItemStack(i == 1 || i == 7 ? Items.OAK_SLAB : Items.OAK_PLANKS));
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, grid);
        var barrel = level.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level).orElseThrow();
        test.assertTrue(barrel.value().assemble(input).is(Items.BARREL), "Selected slabs produce vanilla barrel");
        test.assertTrue(!dev.tazer.clutternomore.recipe.RecipeFixes.isEnabled(), "Broad shape ingredient matching disabled");
        test.succeed();
    }

    public void inventoryGestures(GameTestHelper test) {
        var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var chest = new SimpleContainer(27);
        var menu = new net.minecraft.world.inventory.ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x3, 1, player.getInventory(), chest, 3);
        chest.setItem(0, MaterialStacks.withUnits(new ItemStack(Items.OAK_STAIRS), 512));
        menu.clicked(0, 1, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        test.assertValueEqual(MaterialStacks.units(chest.getItem(0)) + MaterialStacks.units(menu.getCarried()), 512, "Right click conserves remainder");
        menu.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        test.assertValueEqual(MaterialStacks.units(chest.getItem(0)), 512, "Cursor merges back by balance");
        menu.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        menu.clicked(-999, 0, net.minecraft.world.inventory.ContainerInput.QUICK_CRAFT, player);
        menu.clicked(1, 1, net.minecraft.world.inventory.ContainerInput.QUICK_CRAFT, player);
        menu.clicked(2, 1, net.minecraft.world.inventory.ContainerInput.QUICK_CRAFT, player);
        menu.clicked(-999, 2, net.minecraft.world.inventory.ContainerInput.QUICK_CRAFT, player);
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
            test.runAfterDelay(5, () -> {
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
            ItemStack copy = ItemStackTemplate.fromStack(stack).create();
            test.assertValueEqual(MaterialStacks.units(copy), 512, "Template conversion");
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
