package dev.jensh.buildingpalette;

import java.util.*;
import dev.jensh.buildingpalette.core.Balance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class InventoryTransactions {
    private InventoryTransactions() {}
    public static void assign(ItemStack destination, ItemStack value) {
        // Clear first: changing the count of a stack that still carries a remainder rebalances it.
        MaterialStacks.clearRemainder(destination);
        destination.setCount(value.getCount());
        if (value.has(BuildingPalette.MISSING.get()) && !destination.isEmpty()) {
            destination.set(BuildingPalette.MISSING.get(), value.get(BuildingPalette.MISSING.get()));
            destination.set(BuildingPalette.MISSING_FOR.get(), MaterialStacks.sealed(value));
        }
    }
    /** Transfer material to a slot, preserving the receiver's selected shape. */
    public static int insert(Slot slot, ItemStack source, int limitUnits) {
        if (source.isEmpty() || Materials.get(source) == null) return 0;
        ItemStack existing = slot.getItem();
        if (!slot.mayPlace(source) || (!existing.isEmpty() && !MaterialStacks.sameMaterial(existing, source))) return 0;
        ItemStack target = existing.isEmpty() ? source : existing;
        int stored = existing.isEmpty() ? 0 : MaterialStacks.units(existing);
        if (slot.getMaxStackSize() < 64)
            limitUnits = Math.min(limitUnits, Math.max(0, slot.getMaxStackSize() * Materials.get(target).shape().cost - stored));
        int amount = Math.min(Math.min(Balance.CAPACITY - stored, MaterialStacks.units(source)), limitUnits);
        if (amount <= 0) return 0;
        int left = MaterialStacks.units(source) - amount;
        slot.setByPlayer(MaterialStacks.withUnits(target, stored + amount));
        assign(source, MaterialStacks.withUnits(source, left));
        slot.setChanged();
        return amount;
    }
    public static boolean pickup(AbstractContainerMenu menu, int index, int button, Player player) {
        // Screens normalize physical SDL buttons into inventory buttons 0/1.
        if (index < 0 || index >= menu.slots.size() || (button != 0 && button != 1)) return false;
        Slot slot = menu.getSlot(index);
        ItemStack cursor = menu.getCarried(), existing = slot.getItem();
        boolean managedCursor = Materials.get(cursor) != null, managedSlot = Materials.get(existing) != null;
        if (!managedCursor && !managedSlot) return false;
        // Recipe output slots must execute their own onTake and resource consumption.
        if (!existing.isEmpty() && !slot.mayPlace(existing)) return false;
        if (!slot.mayPickup(player)) return true;
        if (cursor.isEmpty() && managedSlot) {
            int units = MaterialStacks.units(existing);
            int take = button == 0 ? units : Math.max(1, (MaterialStacks.available(existing) + 1) / 2) * Materials.get(existing).shape().cost;
            take = Math.min(units, take);
            menu.setCarried(MaterialStacks.withUnits(existing, take));
            slot.setByPlayer(MaterialStacks.withUnits(existing, units - take));
        } else if (managedCursor && (existing.isEmpty() || MaterialStacks.sameMaterial(cursor, existing))) {
            // Do not mutate a carried stack that a prediction/synchronization snapshot
            // may still reference. Publish the completed cursor value atomically.
            cursor = cursor.copy();
            int limit = button == 0 ? MaterialStacks.units(cursor)
                : MaterialStacks.available(cursor) > 0 ? Materials.get(cursor).shape().cost : 0;
            insert(slot, cursor, limit);
            menu.setCarried(cursor.isEmpty() ? ItemStack.EMPTY : cursor);
        } else if (managedCursor && slot.mayPlace(cursor)) {
            slot.setByPlayer(cursor);
            menu.setCarried(existing);
        } else return false;
        slot.setChanged();
        return true;
    }
    public static boolean quickMove(AbstractContainerMenu menu, ItemStack source, int start, int end, boolean reverse) {
        boolean changed = false;
        for (int pass = 0; pass < 2; pass++) for (int i = 0; i < end - start && !source.isEmpty(); i++) {
            Slot slot = menu.getSlot(reverse ? end - 1 - i : start + i);
            if ((pass == 0) == slot.hasItem()) changed |= insert(slot, source, 512) > 0;
        }
        return changed;
    }
    public static void collect(AbstractContainerMenu menu, Player player) {
        ItemStack cursor = menu.getCarried();
        for (Slot slot : menu.slots) {
            ItemStack source = slot.getItem();
            if (source.isEmpty() || !slot.mayPickup(player) || !slot.mayPlace(source) || !MaterialStacks.sameMaterial(cursor, source)) continue;
            int take = Math.min(512 - MaterialStacks.units(cursor), MaterialStacks.units(source));
            if (take <= 0) break;
            cursor = MaterialStacks.withUnits(cursor, MaterialStacks.units(cursor) + take);
            slot.setByPlayer(MaterialStacks.withUnits(source, MaterialStacks.units(source) - take));
        }
        menu.setCarried(cursor);
    }
}
