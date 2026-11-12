package dev.jensh.buildingpalette.mixin.neoforge;

import dev.jensh.buildingpalette.*;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemResource.class)
public abstract class ResourceCapacityMixin {
    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void palette$capacity(CallbackInfoReturnable<Integer> cir) {
        var stack = ((ItemResource)(Object)this).toStack(1);
        if (Materials.get(stack) != null) cir.setReturnValue(MaterialStacks.capacity(stack));
    }
}
