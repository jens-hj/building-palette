package dev.jensh.buildingpalette.test;

import com.tiviacz.travelersbackpack.attachment.AttachmentUtils;
import com.tiviacz.travelersbackpack.init.ModItems;
import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.core.Shape;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;

/** Loaded only in the copied RPG fixture where Traveler's Backpack is present. */
public final class BackpackPickTest {
    public static void prepare(ServerPlayer player) {
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        for (int i = 0; i < 9; i++) player.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
        player.getInventory().setSelectedSlot(0);
        var backpack = ModItems.STANDARD_TRAVELERS_BACKPACK.getDefaultInstance();
        if (com.tiviacz.travelersbackpack.TravelersBackpack.enableIntegration()) {
            try {
                var api = Class.forName("eu.pb4.trinkets.api.TrinketsApi");
                Object attachment = api.getMethod("getAttachment", net.minecraft.world.entity.LivingEntity.class).invoke(null, player);
                var type = Class.forName("eu.pb4.trinkets.api.TrinketAttachment");
                var inventories = (java.util.Map<?, ?>)type.getMethod("getInventories").invoke(attachment);
                boolean equipped = false;
                for (var entry : inventories.entrySet()) if (entry.getKey().toString().endsWith("back") && entry.getValue() instanceof net.minecraft.world.Container slot && slot.getContainerSize() > 0) {
                    slot.setItem(0, backpack); slot.setChanged(); equipped = true; break;
                }
                if (!equipped) throw new AssertionError("No backpack trinket slot in fixture");
            } catch (ReflectiveOperationException error) { throw new AssertionError("Could not equip backpack in Trinkets fixture", error); }
        } else AttachmentUtils.equipBackpack(player, backpack);
        if (!AttachmentUtils.isWearingBackpack(player)) throw new AssertionError("Backpack test failed to equip backpack");
        var storage = AttachmentUtils.getBackpackWrapper(player, AttachmentUtils.STORAGE_ONLY.get()).getStorageForInputOutput();
        storage.setStackInSlot(0, Materials.piece("minecraft:stone", Shape.SLAB, 65));
        player.inventoryMenu.broadcastChanges();
    }
    public static void check(ServerPlayer player, Shape expected) {
        var selected = Materials.get(player.getMainHandItem());
        if (selected == null || !selected.material().equals("minecraft:stone") || selected.shape() != expected
                || MaterialStacks.units(player.getMainHandItem()) != 65) throw new AssertionError("Backpack material pick lost shape or balance");
        var displaced = AttachmentUtils.getBackpackWrapper(player, AttachmentUtils.STORAGE_ONLY.get()).getStorageForInputOutput().getStackInSlot(0);
        if (!displaced.is(Items.DIRT) || displaced.getCount() != 64) throw new AssertionError("Backpack pick did not preserve displaced hotbar stack");
    }
}
