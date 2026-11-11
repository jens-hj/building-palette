package dev.jensh.buildingpalette.compat;

import com.tiviacz.travelersbackpack.attachment.AttachmentUtils;
import dev.jensh.buildingpalette.MaterialStacks;
import dev.jensh.buildingpalette.Materials;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Use the equipped backpack's filter-aware, transactional storage API. */
public final class BackpackPick {
    private BackpackPick() {}
    public static ItemStack pick(ServerPlayer player, String material) {
        if (!AttachmentUtils.isWearingBackpack(player)) return ItemStack.EMPTY;
        var storage = AttachmentUtils.getBackpackWrapper(player, AttachmentUtils.STORAGE_ONLY.get()).getStorageForInputOutput();
        var inventory = player.getInventory();
        for (int i = 0; i < storage.size(); i++) {
            int count = Math.toIntExact(storage.getAmountAsLong(i));
            if (count <= 0) continue;
            var resource = storage.getResource(i);
            ItemStack source = resource.toStack(count);
            var selection = Materials.get(source);
            if (selection == null || !selection.material().equals(material)) continue;
            int hotbar = inventory.getSuitableHotbarSlot();
            ItemStack displaced = inventory.getItem(hotbar).copy();
            var replacement = ItemResource.of(displaced);
            if (!displaced.isEmpty() && !storage.isValid(i, replacement)) continue;
            try (var transaction = Transaction.openRoot()) {
                if (storage.extract(i, resource, count, transaction) != count) continue;
                if (!displaced.isEmpty() && storage.insert(i, replacement, displaced.getCount(), transaction) != displaced.getCount()) continue;
                transaction.commit();
            }
            inventory.setItem(hotbar, source.copy());
            inventory.setSelectedSlot(hotbar);
            return source.copy();
        }
        return ItemStack.EMPTY;
    }
    public static int take(ServerPlayer player, ItemStack like, int units) {
        if (!AttachmentUtils.isWearingBackpack(player)) return 0;
        var storage = AttachmentUtils.getBackpackWrapper(player, AttachmentUtils.STORAGE_ONLY.get()).getStorageForInputOutput();
        int taken = 0;
        for (int i = 0; i < storage.size() && taken < units; i++) {
            int count = Math.toIntExact(storage.getAmountAsLong(i));
            if (count <= 0) continue;
            var resource = storage.getResource(i);
            ItemStack source = resource.toStack(count);
            if (!MaterialStacks.sameMaterial(like, source)) continue;
            int amount = Math.min(units - taken, MaterialStacks.units(source));
            // A partial take changes the remaining stack's missing-unit component, so
            // replace the slot's contents with the remainder in one transaction.
            ItemStack remainder = MaterialStacks.withUnits(source, MaterialStacks.units(source) - amount);
            try (var transaction = Transaction.openRoot()) {
                if (storage.extract(i, resource, count, transaction) != count) continue;
                if (!remainder.isEmpty() && storage.insert(i, ItemResource.of(remainder), remainder.getCount(), transaction) != remainder.getCount()) continue;
                transaction.commit();
            }
            taken += amount;
        }
        return taken;
    }
}
