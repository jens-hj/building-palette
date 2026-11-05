package dev.jensh.buildingpalette.mixin;

import dev.jensh.buildingpalette.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void palette$pickup(int selected, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (Materials.get(stack) == null || MaterialStacks.units(stack) > 512) return;
        Inventory inventory = (Inventory)(Object)this;
        int initial = MaterialStacks.units(stack);
        for (int pass = 0; pass < 2; pass++) for (int i = 0; i < 36 && !stack.isEmpty(); i++) {
            int index = selected >= 0 ? selected : i;
            ItemStack current = inventory.getItem(index);
            if ((pass == 0) == !current.isEmpty() && (current.isEmpty() || MaterialStacks.sameMaterial(stack, current))) {
                int present = current.isEmpty() ? 0 : MaterialStacks.units(current);
                int move = Math.min(512 - present, MaterialStacks.units(stack));
                if (move > 0) {
                    inventory.setItem(index, MaterialStacks.withUnits(current.isEmpty() ? stack : current, present + move));
                    InventoryTransactions.assign(stack, MaterialStacks.withUnits(stack, MaterialStacks.units(stack) - move));
                }
            }
            if (selected >= 0) break;
        }
        cir.setReturnValue(stack.isEmpty() || MaterialStacks.units(stack) < initial);
    }
}
