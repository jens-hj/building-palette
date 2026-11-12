package dev.jensh.buildingpalette.mixin;

import dev.jensh.buildingpalette.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** One material stack holds 64 blocks' worth, whatever shape it currently shows. */
@Mixin(ItemStack.class)
public abstract class StackCapacityMixin {
    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void palette$capacity(CallbackInfoReturnable<Integer> cir) {
        var stack = (ItemStack)(Object)this;
        if (Materials.get(stack) != null) cir.setReturnValue(MaterialStacks.capacity(stack));
    }
}
