package dev.jensh.buildingpalette.compat;

import com.tiviacz.travelersbackpack.attachment.AttachmentUtils;
import dev.jensh.buildingpalette.Materials;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Extend the equipped backpack's usual pick-block storage/hotbar swap to materials. */
public final class BackpackPick {
    private BackpackPick() {}
    public static ItemStack pick(ServerPlayer player, String material) {
        if (!AttachmentUtils.isWearingBackpack(player)) return ItemStack.EMPTY;
        var storage = AttachmentUtils.getBackpackWrapper(player, AttachmentUtils.STORAGE_ONLY.get()).getStorageForInputOutput();
        var inventory = player.getInventory();
        for (int i = 0; i < storage.getSlots(); i++) {
            ItemStack source = storage.getStackInSlot(i);
            var selection = Materials.get(source);
            if (selection == null || !selection.material().equals(material)) continue;
            int hotbar = inventory.getSuitableHotbarSlot();
            ItemStack displaced = inventory.getItem(hotbar);
            if (!displaced.isEmpty() && !storage.isItemValid(i, displaced)) continue;
            ItemStack picked = source.copy();
            storage.setStackInSlot(i, displaced);
            inventory.setItem(hotbar, picked);
            inventory.setSelectedSlot(hotbar);
            return picked.copy();
        }
        return ItemStack.EMPTY;
    }
}
