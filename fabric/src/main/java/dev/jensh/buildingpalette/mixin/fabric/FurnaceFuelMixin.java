package dev.jensh.buildingpalette.mixin.fabric;

import dev.jensh.buildingpalette.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A shape burns for its share of the full block; a stack with no whole piece left is not fuel. */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceFuelMixin {
    private static int palette$burnTime(ItemStack stack) {
        var selection = Materials.get(stack);
        if (selection == null) return -1;
        if (MaterialStacks.available(stack) == 0) return 0;
        return AbstractFurnaceBlockEntity.getFuel().getOrDefault(selection.parent(), 0) * selection.shape().cost / 8;
    }
    @Inject(method = "getBurnDuration", at = @At("HEAD"), cancellable = true)
    private void palette$fuel(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        int time = palette$burnTime(stack);
        if (time >= 0) cir.setReturnValue(time);
    }
    @Inject(method = "isFuel", at = @At("HEAD"), cancellable = true)
    private static void palette$isFuel(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        int time = palette$burnTime(stack);
        if (time >= 0) cir.setReturnValue(time > 0);
    }
}
