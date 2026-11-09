package dev.jensh.buildingpalette.mixin.neoforge;

import dev.jensh.buildingpalette.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** NeoForge overrides ItemInstance's default capacity on stacks and resources. */
@Mixin(ItemStack.class)
public abstract class StackCapacityMixin {
    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void palette$capacity(CallbackInfoReturnable<Integer> cir) {
        var stack = (ItemStack)(Object)this;
        if (Materials.get(stack) != null) cir.setReturnValue(MaterialStacks.capacity(stack));
    }
}
