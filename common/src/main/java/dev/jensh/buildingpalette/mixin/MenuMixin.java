package dev.jensh.buildingpalette.mixin;

import dev.jensh.buildingpalette.*;
import java.util.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(AbstractContainerMenu.class)
public abstract class MenuMixin {
    @Unique private final Set<Slot> palette$dragSlots = new LinkedHashSet<>();
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void palette$click(int index, int button, ContainerInput input, Player player, CallbackInfo ci) {
        AbstractContainerMenu menu = (AbstractContainerMenu)(Object)this;
        if (input == ContainerInput.PICKUP && InventoryTransactions.pickup(menu, index, button, player)) ci.cancel();
        if (input == ContainerInput.PICKUP_ALL && Materials.get(menu.getCarried()) != null) {
            InventoryTransactions.collect(menu, player); ci.cancel();
        }
        if (input == ContainerInput.QUICK_CRAFT && Materials.get(menu.getCarried()) != null) {
            int header = AbstractContainerMenu.getQuickcraftHeader(button), type = AbstractContainerMenu.getQuickcraftType(button);
            if (header == 0) palette$dragSlots.clear();
            else if (header == 1 && index >= 0 && index < menu.slots.size()) {
                Slot slot = menu.getSlot(index);
                if (slot.mayPlace(menu.getCarried()) && (slot.getItem().isEmpty() || MaterialStacks.sameMaterial(slot.getItem(), menu.getCarried()))) palette$dragSlots.add(slot);
            } else if (header == 2 && !palette$dragSlots.isEmpty()) {
                ItemStack cursor = menu.getCarried().copy();
                int cost = Materials.get(cursor).shape().cost;
                int each = type == 1 ? 1 : MaterialStacks.available(cursor) / palette$dragSlots.size();
                if (type == 2 && player.hasInfiniteMaterials()) each = 512 / cost;
                for (Slot slot : palette$dragSlots) {
                    if (type == 2 && player.hasInfiniteMaterials()) {
                        ItemStack copy = MaterialStacks.withUnits(cursor, 512 / cost * cost);
                        InventoryTransactions.insert(slot, copy, each * cost);
                    } else InventoryTransactions.insert(slot, cursor, each * cost);
                }
                menu.setCarried(cursor.isEmpty() ? ItemStack.EMPTY : cursor);
                palette$dragSlots.clear();
            }
            ci.cancel();
        }
    }
    @Inject(method = "moveItemStackTo", at = @At("HEAD"), cancellable = true)
    private void palette$quickMove(ItemStack stack, int start, int end, boolean reverse, CallbackInfoReturnable<Boolean> cir) {
        if (Materials.get(stack) != null) cir.setReturnValue(InventoryTransactions.quickMove((AbstractContainerMenu)(Object)this, stack, start, end, reverse));
    }
}
