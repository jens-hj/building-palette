package dev.jensh.buildingpalette.test;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.core.Cells;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedbackpacks.api.IBlockClickResponseUpgrade;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;

public final class SophisticatedToolSwapperTest {
    public static void run(GameTestHelper test) {
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        var backpack = BackpackWrapper.fromStack(new ItemStack(ModItems.DIAMOND_BACKPACK.get()));
        backpack.setContentsUuid(java.util.UUID.randomUUID());
        backpack.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.ADVANCED_TOOL_SWAPPER_UPGRADE.get()));
        backpack.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND_AXE));
        backpack.getInventoryHandler().setStackInSlot(1, new ItemStack(Items.DIAMOND_SHOVEL));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        var upgrades = backpack.getUpgradeHandler().getWrappersThatImplement(IBlockClickResponseUpgrade.class);
        test.assertTrue(upgrades.size() == 1, "One enabled advanced swapper");
        var swapper = upgrades.iterator().next();
        var pos = test.absolutePos(new BlockPos(2, 2, 2));
        test.getLevel().setBlock(pos, BuildingPalette.MULTIPART.get().defaultBlockState(), 3);
        var entity = (MultipartEntity)test.getLevel().getBlockEntity(pos);
        entity.setCells(new Cells().place(1, "minecraft:oak_planks")
            .place(2, "minecraft:stone").place(4, "minecraft:dirt"));
        // Change aim within the SAME carrier: the cached block must follow the material.
        select(test, player, swapper, pos, .25, .25, Items.DIAMOND_AXE);
        select(test, player, swapper, pos, .75, .25, Items.DIAMOND_PICKAXE);
        select(test, player, swapper, pos, .25, .75, Items.DIAMOND_SHOVEL);
        select(test, player, swapper, pos, .25, .25, Items.DIAMOND_AXE);
        test.assertValueEqual(entity.cells().occupied(), 7, "Tool selection must not change the combined block");
        int tools = player.getMainHandItem().getCount();
        for (int i=0; i<backpack.getInventoryHandler().size(); i++) tools += backpack.getInventoryHandler().getStackInSlot(i).getCount();
        test.assertValueEqual(tools, 3, "All tools survive repeated swaps");
        // Ordinary blocks continue through the swapper's original logic.
        test.getLevel().setBlock(pos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        select(test, player, swapper, pos, .25, .25, Items.DIAMOND_PICKAXE);
        test.succeed();
    }

    private static void select(GameTestHelper test, Player player, IBlockClickResponseUpgrade swapper,
            BlockPos pos, double x, double y, Item expected) {
        player.setPos(pos.getX()+x, pos.getY()+y-player.getEyeHeight(), pos.getZ()-2);
        var aim = new Vec3(pos.getX()+x, pos.getY()+y, pos.getZ()+.25).subtract(player.getEyePosition());
        player.setYRot((float)(Math.toDegrees(Math.atan2(aim.z,aim.x))-90));
        player.setXRot((float)-Math.toDegrees(Math.atan2(aim.y,Math.sqrt(aim.x*aim.x+aim.z*aim.z))));
        player.setYHeadRot(180); // Aim must use the latest packet rotation, not the stale head.
        swapper.onBlockClick(player, pos);
        test.assertTrue(player.getMainHandItem().is(expected), "Expected " + expected + " at " + x + "," + y + "; got " + player.getMainHandItem());
    }
}
