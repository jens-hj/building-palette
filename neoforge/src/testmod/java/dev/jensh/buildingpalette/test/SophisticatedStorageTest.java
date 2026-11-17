package dev.jensh.buildingpalette.test;

import dev.jensh.buildingpalette.*;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageInventorySlot;
import net.p3pp3rf1y.sophisticatedcore.util.InventorySorter;

public final class SophisticatedStorageTest {
    public static void run(GameTestHelper test) {
        if (!net.neoforged.fml.ModList.get().isLoaded("sophisticatedstorage")) { test.succeed(); return; }
        var block = BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("sophisticatedstorage", "barrel"));
        var pos = test.absolutePos(new BlockPos(1, 1, 1));
        test.getLevel().setBlock(pos, block.defaultBlockState(), 3);
        var entity = test.getLevel().getBlockEntity(pos);
        IStorageWrapper wrapper;
        try { wrapper = (IStorageWrapper)entity.getClass().getMethod("getStorageWrapper").invoke(entity); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        var inventory = wrapper.getInventoryHandler();
        inventory.setBaseSlotLimit(256); // same capacity used by a fourfold stack upgrade
        var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var slot = new StorageInventorySlot(wrapper, 0, player);
        inventory.setStackInSlotInternal(0, new ItemStack(Items.COBBLESTONE, 169));
        ItemStack remainder = MaterialStacks.withUnits(new ItemStack(Items.COBBLESTONE), 3);
        test.assertValueEqual(InventoryTransactions.insert(slot, remainder, Integer.MAX_VALUE), 3, "169 cobblestone accepts partial material");
        test.assertTrue(remainder.isEmpty(), "Cursor remainder is fully merged");
        test.assertValueEqual(MaterialStacks.units(inventory.getInternalStack(0)), 169 * 8 + 3, "Exact upgraded-slot balance");
        inventory.setStackInSlotInternal(0, new ItemStack(Items.COBBLESTONE, 256));
        remainder = MaterialStacks.withUnits(new ItemStack(Items.COBBLESTONE), 3);
        test.assertValueEqual(InventoryTransactions.insert(slot, remainder, Integer.MAX_VALUE), 0, "Full upgraded slot rejects excess");
        test.assertValueEqual(MaterialStacks.units(remainder), 3, "Rejected remainder preserved");
        inventory.setStackInSlotInternal(1, new ItemStack(Items.COBBLESTONE, 169));
        inventory.setStackInSlotInternal(2, remainder);
        inventory.setStackInSlotInternal(3, new ItemStack(Items.DIAMOND, 17));
        inventory.setStackInSlotInternal(4, MaterialStacks.withUnits(new ItemStack(Items.COBBLESTONE), 5));
        int expected = (256 + 169) * 8 + 8;
        for (int repeat = 0; repeat < 5; repeat++) {
            InventorySorter.sortHandler(inventory, InventorySorter.BY_NAME, Set.of());
            int total = 0, materialSlots = 0, diamonds = 0;
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.getInternalStack(i);
                total += MaterialStacks.units(stack);
                if (Materials.get(stack) != null) materialSlots++;
                if (stack.is(Items.DIAMOND)) diamonds += stack.getCount();
            }
            test.assertValueEqual(total, expected, "Repeated sorting conserves exact material");
            test.assertValueEqual(materialSlots, 2, "Partial tails merge into nonfull upgraded stacks");
            test.assertValueEqual(diamonds, 17, "Unmanaged items keep normal counts");
        }
        for (int i = 0; i < inventory.size(); i++) inventory.setStackInSlotInternal(i, ItemStack.EMPTY);
        inventory.setStackInSlotInternal(0, MaterialStacks.withUnits(new ItemStack(Items.COBBLESTONE), 3));
        inventory.setStackInSlotInternal(1, new ItemStack(Items.COBBLESTONE, 169));
        inventory.setStackInSlotInternal(2, MaterialStacks.withUnits(new ItemStack(Items.COBBLESTONE), 5));
        InventorySorter.sortHandler(inventory, InventorySorter.BY_NAME, Set.of(0), Set.of(5), (i, stack) -> stack.is(Items.COBBLESTONE));
        test.assertValueEqual(MaterialStacks.units(inventory.getInternalStack(0)), 3, "No-sort slot stays untouched");
        test.assertValueEqual(MaterialStacks.units(inventory.getInternalStack(5)), 169 * 8 + 5, "Memory slot receives merged fractional balance");
        // Every slot is full of fractional stairs; sorting must not round each
        // slot's capacity down to a whole number of stairs and lose the tail.
        for (int i = 0; i < inventory.size(); i++)
            inventory.setStackInSlotInternal(i, MaterialStacks.withStoredUnits(new ItemStack(Items.OAK_STAIRS), 2048));
        InventorySorter.sortHandler(inventory, InventorySorter.BY_COUNT, Set.of());
        int fullTotal = 0;
        for (int i = 0; i < inventory.size(); i++) fullTotal += MaterialStacks.units(inventory.getInternalStack(i));
        test.assertValueEqual(fullTotal, inventory.size() * 2048, "Sorting full upgraded storage conserves fractional stairs");
        test.succeed();
    }
}
