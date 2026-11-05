package dev.jensh.buildingpalette.test;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.core.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;

public final class PaletteGameTests {
    @GameTest public void randomizedNativeTransfers(GameTestHelper test) {
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

    @GameTest public void eightCornersBecomeBlock(GameTestHelper test) {
        var level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(pos.getX() + 4, pos.getY(), pos.getZ() + 4);
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

    @GameTest public void connectedMining(GameTestHelper test) {
        var level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(pos, BuildingPalette.MULTIPART.defaultBlockState(), 3);
        var entity = (MultipartEntity)level.getBlockEntity(pos);
        entity.setCells(new Cells().place(7, "minecraft:oak_planks").place(128, "minecraft:stone_bricks"));
        var player = (net.minecraft.server.level.ServerPlayer)test.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(pos.getX() + .25, pos.getY(), pos.getZ() - 2);
        var aim = new net.minecraft.world.phys.Vec3(pos.getX() + .25, pos.getY() + .25, pos.getZ() + .25).subtract(player.getEyePosition());
        player.setYRot((float)(Math.toDegrees(Math.atan2(aim.z, aim.x)) - 90));
        player.setXRot((float)-Math.toDegrees(Math.atan2(aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z))));
        test.assertTrue(player.gameMode.destroyBlock(pos), "Mining handler accepts target");
        test.assertValueEqual(((MultipartEntity)level.getBlockEntity(pos)).cells().occupied(), 128, "All connected oak removed, stone remains");
        var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(pos).inflate(1));
        test.assertValueEqual(drops.stream().mapToInt(e -> MaterialStacks.units(e.getItem())).sum(), 3, "Mining refunds exact L-shape volume");
        test.succeed();
    }

    @GameTest public void nativeShapeDrops(GameTestHelper test) {
        BlockPos pos = test.absolutePos(new BlockPos(1, 1, 1));
        for (var block : new net.minecraft.world.level.block.Block[]{Blocks.OAK_SLAB, Blocks.OAK_STAIRS}) {
            var state = block.defaultBlockState();
            test.getLevel().setBlock(pos, state, 3);
            var drops = net.minecraft.world.level.block.Block.getDrops(state, test.getLevel(), pos, null);
            int expected = block == Blocks.OAK_SLAB ? 4 : 6;
            test.assertValueEqual(drops.stream().mapToInt(MaterialStacks::units).sum(), expected, "Existing native shape drops its physical volume");
        }
        test.succeed();
    }

    @GameTest public void repeatedCrafting(GameTestHelper test) {
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

    @GameTest public void inventoryGestures(GameTestHelper test) {
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

    @GameTest public void furnaceRemainder(GameTestHelper test) {
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

    @GameTest public void materialStacks(GameTestHelper test) {
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

    @GameTest public void remainderTransfers(GameTestHelper test) {
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

    @GameTest public void multipartPersistence(GameTestHelper test) {
        BlockPos pos = test.absolutePos(new BlockPos(1, 1, 1));
        test.getLevel().setBlock(pos, BuildingPalette.MULTIPART.defaultBlockState(), 3);
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
