package dev.jensh.buildingpalette.mixin.neoforge;

import dev.jensh.buildingpalette.*;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemResource.class)
public abstract class ResourceCapacityMixin {
    // Creating an immutable resource is a description, not an inventory split.
    // Keep the reserved remainder when NeoForge normalizes its backing count to one.
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(
        method = "of(Lnet/minecraft/world/item/ItemStack;)Lnet/neoforged/neoforge/transfer/item/ItemResource;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copyWithCount(I)Lnet/minecraft/world/item/ItemStack;"))
    private static net.minecraft.world.item.ItemStack palette$resourceDescription(net.minecraft.world.item.ItemStack source, int count,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<net.minecraft.world.item.ItemStack> original) {
        if (Materials.get(source) == null) return original.call(source, count);
        var copy = source.copy(); copy.setCount(count); return copy;
    }

    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void palette$capacity(CallbackInfoReturnable<Integer> cir) {
        var stack = ((ItemResource)(Object)this).toStack(1);
        if (Materials.get(stack) != null) cir.setReturnValue(MaterialStacks.capacity(stack));
    }
}
