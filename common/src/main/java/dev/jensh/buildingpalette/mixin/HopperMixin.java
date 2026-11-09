package dev.jensh.buildingpalette.mixin;

import dev.jensh.buildingpalette.*;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(HopperBlockEntity.class)
public abstract class HopperMixin {
    @Shadow private static boolean canPlaceItemInContainer(Container c, ItemStack s, int slot, Direction side) { throw new AssertionError(); }
    @Inject(method = "tryMoveInItem", at = @At("HEAD"), cancellable = true)
    private static void palette$merge(Container from, Container into, ItemStack source, int index, Direction direction, CallbackInfoReturnable<ItemStack> cir) {
        if (Materials.get(source) == null) return;
        ItemStack target = into.getItem(index);
        if (!target.isEmpty() && MaterialStacks.sameMaterial(source, target) && canPlaceItemInContainer(into, source, index, direction)) {
            int move = Math.min(512 - MaterialStacks.units(target), MaterialStacks.units(source));
            if (move > 0) {
                into.setItem(index, MaterialStacks.withUnits(target, MaterialStacks.units(target) + move));
                ItemStack remaining = MaterialStacks.withUnits(source, MaterialStacks.units(source) - move);
                into.setChanged();
                cir.setReturnValue(remaining);
            }
        }
    }
}
