package dev.jensh.buildingpalette.mixin;

import dev.jensh.buildingpalette.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "areMergable", at = @At("HEAD"), cancellable = true)
    private static void palette$canMerge(ItemStack a, ItemStack b, CallbackInfoReturnable<Boolean> cir) {
        if (MaterialStacks.sameMaterial(a, b)) cir.setReturnValue(MaterialStacks.units(a) + MaterialStacks.units(b) <= 512);
    }
    @Inject(method = "merge(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private static void palette$merge(ItemStack target, ItemStack source, int maxCount, CallbackInfoReturnable<ItemStack> cir) {
        if (!MaterialStacks.sameMaterial(target, source)) return;
        int limit = maxCount == 64 ? 512 : Math.min(512, maxCount * Materials.get(target).shape().cost);
        int move = Math.max(0, Math.min(limit - MaterialStacks.units(target), MaterialStacks.units(source)));
        ItemStack result = MaterialStacks.withUnits(target, MaterialStacks.units(target) + move);
        InventoryTransactions.assign(source, MaterialStacks.withUnits(source, MaterialStacks.units(source) - move));
        cir.setReturnValue(result);
    }
}
