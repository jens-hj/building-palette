package dev.jensh.buildingpalette.test;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.core.Shape;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

/** Traveler's Backpack scenarios; the loader supplies access to the equipped backpack's storage. */
public final class BackpackScenarios {
    public interface Pack {
        void equip(ServerPlayer player);
        ItemStack get(ServerPlayer player, int slot);
        void set(ServerPlayer player, int slot, ItemStack stack);
    }
    private BackpackScenarios() {}
    public static void pickUsesEquippedBackpack(GameTestHelper test, Pack pack) {
        var player = TestPlayers.survival(test);
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(pos.getX() + 1, pos.getY(), pos.getZ() + 1);
        level.setBlock(pos, Blocks.STONE_STAIRS.defaultBlockState(), 3);
        var inventory = player.getInventory();
        inventory.clearContent();
        for (int i = 0; i < 9; i++) inventory.setItem(i, new ItemStack(Items.DIRT, 64));
        inventory.selected = 0;
        pack.equip(player);
        pack.set(player, 0, Materials.piece("minecraft:stone", Shape.SLAB, 65));
        pack.set(player, 1, Materials.piece("minecraft:oak_planks", Shape.CORNER, 512));
        // The stored stack swaps with the selected hotbar stack, keeping its shape and remainder.
        ItemStack picked = PickBlock.pick(player, pos, level.getBlockState(pos), false);
        test.assertTrue(picked != null && !picked.isEmpty(), "Pick found the material in the backpack");
        var held = Materials.get(player.getMainHandItem());
        test.assertTrue(held != null && held.material().equals("minecraft:stone") && held.shape() == Shape.SLAB, "Backpack pick keeps the stored shape");
        test.assertValueEqual(MaterialStacks.units(player.getMainHandItem()), 65, "Backpack pick keeps the remainder");
        test.assertTrue(pack.get(player, 0).is(Items.DIRT) && pack.get(player, 0).getCount() == 64, "Displaced hotbar stack moves into the backpack");
        test.assertValueEqual(MaterialStacks.units(pack.get(player, 1)), 512, "Backpack pick leaves other materials alone");
        // A held stack without a whole piece refills from the backpack once the inventory has none.
        inventory.setItem(0, Materials.piece("minecraft:stone", Shape.STAIR, 2));
        pack.set(player, 2, Materials.piece("minecraft:stone", Shape.BLOCK, 100));
        PickBlock.pick(player, pos, level.getBlockState(pos), false);
        test.assertTrue(Materials.get(inventory.getItem(0)).shape() == Shape.STAIR, "Refill keeps the held shape");
        test.assertValueEqual(MaterialStacks.units(inventory.getItem(0)), 102, "Refill draws material from the backpack");
        test.assertTrue(pack.get(player, 2).isEmpty(), "Refill empties the drained backpack slot");
        test.assertValueEqual(MaterialStacks.units(pack.get(player, 1)), 512, "Refill leaves other materials alone");
        test.succeed();
    }
}
