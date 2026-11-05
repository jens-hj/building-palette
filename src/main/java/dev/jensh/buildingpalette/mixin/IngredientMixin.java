package dev.jensh.buildingpalette.mixin;

import dev.jensh.buildingpalette.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Ingredient.class, priority = 900)
public abstract class IngredientMixin {
    @Inject(method = "test", at = @At("RETURN"), cancellable = true)
    private void palette$wholePieces(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (Materials.get(stack) != null && MaterialStacks.available(stack) == 0) cir.setReturnValue(false);
    }
}
