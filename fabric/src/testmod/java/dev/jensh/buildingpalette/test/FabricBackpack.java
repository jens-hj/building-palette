package dev.jensh.buildingpalette.test;

import com.tiviacz.travelersbackpack.component.ComponentUtils;
import com.tiviacz.travelersbackpack.init.ModItems;
import com.tiviacz.travelersbackpack.inventory.handler.StorageAccessWrapper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Only loaded when Traveler's Backpack is present. */
final class FabricBackpack implements BackpackScenarios.Pack {
    private static StorageAccessWrapper storage(ServerPlayer player) {
        return ComponentUtils.getBackpackWrapper(player, ComponentUtils.STORAGE_ONLY.get()).getStorageForInputOutput();
    }
    @Override public void equip(ServerPlayer player) {
        ComponentUtils.equipBackpack(player, ModItems.STANDARD_TRAVELERS_BACKPACK.getDefaultInstance());
        if (!ComponentUtils.isWearingBackpack(player)) throw new AssertionError("Backpack not equipped");
    }
    @Override public ItemStack get(ServerPlayer player, int slot) { return storage(player).getStackInSlot(slot); }
    @Override public void set(ServerPlayer player, int slot, ItemStack stack) { storage(player).setStackInSlot(slot, stack); }
}
