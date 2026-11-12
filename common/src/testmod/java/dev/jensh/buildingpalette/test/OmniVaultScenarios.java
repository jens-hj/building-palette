package dev.jensh.buildingpalette.test;

import com.nightbeam.omnivault.network.NetworkStorage;
import com.nightbeam.omnivault.storage.*;
import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.core.Shape;
import java.util.*;
import java.util.function.ObjLongConsumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;

/** Only loaded when OmniVault is present. */
public final class OmniVaultScenarios {
    private static final class Cell implements ItemStorage {
        final Map<ItemKey, Long> items = new LinkedHashMap<>();
        @Override public long insert(ItemKey key, long amount, boolean simulate) { if (!simulate) items.merge(key, amount, Long::sum); return amount; }
        @Override public long extract(ItemKey key, long amount, boolean simulate) {
            long taken = Math.min(amount, items.getOrDefault(key, 0L));
            if (!simulate && taken > 0) { if (items.merge(key, -taken, Long::sum) == 0) items.remove(key); }
            return taken;
        }
        @Override public void forEach(ObjLongConsumer<ItemKey> consumer) { items.forEach(consumer::accept); }
    }

    /** Every shape entering a network becomes whole blocks plus fewer than eight corners. */
    public void networkStoresWholeBlocks(GameTestHelper test) {
        try {
            var network = new NetworkStorage();
            var cell = new Cell();
            var set = NetworkStorage.class.getDeclaredMethod("setStorages", List.class);
            set.setAccessible(true);
            set.invoke(network, List.of(cell));
            String oak = "minecraft:oak_planks";
            ItemKey blocks = ItemKey.of(new ItemStack(Items.OAK_PLANKS)), corners = ItemKey.of(Materials.piece(oak, Shape.CORNER, 1));

            ItemStack steps = Materials.piece(oak, Shape.STEP, 10);
            test.assertValueEqual(network.insert(ItemKey.of(steps), steps.getCount(), true), 5L, "Simulation accepts the steps");
            test.assertTrue(cell.items.isEmpty(), "Simulation stores nothing");
            test.assertValueEqual(network.insert(ItemKey.of(steps), steps.getCount(), false), 5L, "Five steps are accepted");
            test.assertValueEqual(network.getCount(blocks), 1L, "Five steps hold one block");
            test.assertValueEqual(network.getCount(corners), 2L, "and two corners");
            test.assertValueEqual(cell.items.size(), 2, "Steps are not stored as their own entry");

            // "0+": a block stack holding only half a block must not round up.
            ItemStack half = Materials.piece(oak, Shape.BLOCK, 4);
            test.assertValueEqual(half.getCount(), 1, "Half a block shows as one incomplete block");
            network.insert(ItemKey.of(half), half.getCount(), false);
            test.assertValueEqual(network.getCount(blocks), 1L, "Half a block adds no block");
            test.assertValueEqual(network.getCount(corners), 6L, "Half a block adds four corners");

            ItemStack partial = Materials.piece(oak, Shape.BLOCK, 20);
            network.insert(ItemKey.of(partial), partial.getCount(), false);
            test.assertValueEqual(network.getCount(blocks), 4L, "Corners roll over into blocks");
            test.assertValueEqual(network.getCount(corners), 2L, "leaving the rest");
            test.assertValueEqual(network.getCount(blocks) * 8 + network.getCount(corners), 34L, "No unit is created or lost");

            network.insert(corners, 6, false);
            test.assertValueEqual(network.getCount(blocks), 5L, "Loose corners complete a block");
            test.assertValueEqual(network.getCount(corners), 0L, "and leave no corner entry");

            network.insert(blocks, 3, false);
            test.assertValueEqual(network.getCount(blocks), 8L, "Whole blocks pass straight through");
            test.succeed();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
