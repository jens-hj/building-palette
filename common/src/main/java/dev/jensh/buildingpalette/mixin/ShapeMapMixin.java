package dev.jensh.buildingpalette.mixin;

import dev.jensh.buildingpalette.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.tazer.clutternomore.shape.ShapeMap", remap = false)
public abstract class ShapeMapMixin {
    @Inject(method = "transferStack", at = @At("HEAD"), cancellable = true)
    private static void palette$conserve(ItemStack stack, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (Materials.get(stack) != null) {
            var list = dev.jensh.buildingpalette.platform.Platform.INSTANCE.shapes(stack.getItem());
            if (index < 0 || index >= list.size()) { cir.setReturnValue(stack.copy()); return; }
            ItemStack target = stack.transmuteCopy(list.get(index), 1);
            target.remove(BuildingPalette.MISSING.get());
            if (Materials.get(target) != null && MaterialStacks.units(stack) <= 512)
                cir.setReturnValue(MaterialStacks.withUnits(target, MaterialStacks.units(stack)));
            else cir.setReturnValue(stack.copy());
        }
    }
}
