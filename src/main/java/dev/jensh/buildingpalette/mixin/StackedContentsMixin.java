package dev.jensh.buildingpalette.mixin;

import dev.jensh.buildingpalette.*;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StackedItemContents.class)
public abstract class StackedContentsMixin {
    @Inject(method = "accountStack(Lnet/minecraft/world/item/ItemStack;I)V", at = @At("HEAD"), cancellable = true)
    private void palette$recipeBook(ItemStack stack, int max, CallbackInfo ci) {
        if (Materials.get(stack) != null && MaterialStacks.missing(stack) > 0) {
            int count = Math.min(max, MaterialStacks.available(stack));
            if (count > 0) {
                ItemStack whole = stack.copy();
                whole.remove(BuildingPalette.MISSING);
                whole.setCount(count);
                ((StackedItemContents)(Object)this).accountStack(whole, count);
            }
            ci.cancel();
        }
    }
}
