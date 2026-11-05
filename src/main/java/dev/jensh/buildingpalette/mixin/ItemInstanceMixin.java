package dev.jensh.buildingpalette.mixin;

import dev.jensh.buildingpalette.*;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemInstance.class)
public interface ItemInstanceMixin {
    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    default void palette$capacity(CallbackInfoReturnable<Integer> cir) {
        if ((Object)this instanceof ItemStack stack && Materials.get(stack) != null) cir.setReturnValue(MaterialStacks.capacity(stack));
    }
}
