package dev.jensh.buildingpalette.mixin;

import dev.jensh.buildingpalette.*;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemStack.class, priority = 900)
public abstract class ItemStackMixin {
    @Inject(method = "validateStrict", at = @At("RETURN"), cancellable = true)
    private static void palette$validBalance(ItemStack stack, CallbackInfoReturnable<com.mojang.serialization.DataResult<ItemStack>> cir) {
        var selection = Materials.get(stack);
        if (selection == null) return;
        int missing = MaterialStacks.missing(stack);
        long units = (long)stack.getCount() * selection.shape().cost - missing;
        if (missing < 0 || missing >= selection.shape().cost || units <= 0 || units > 512)
            cir.setReturnValue(com.mojang.serialization.DataResult.error(() -> "Invalid building material balance"));
    }
    @Inject(method = "copyWithCount", at = @At("RETURN"))
    private void palette$splitRemainder(int count, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack self = (ItemStack)(Object)this;
        if (count < self.getCount()) cir.getReturnValue().remove(BuildingPalette.MISSING);
    }
    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    private void palette$pieceName(CallbackInfoReturnable<net.minecraft.network.chat.Component> cir) {
        ItemStack self = (ItemStack)(Object)this;
        var selection = Materials.get(self);
        if (self.getItem() == BuildingPalette.PIECE && selection != null) {
            cir.setReturnValue(net.minecraft.network.chat.Component.translatable("item.building_palette.piece_name",
                new ItemStack(selection.parent()).getHoverName(), net.minecraft.network.chat.Component.translatable("shape.building_palette." + selection.shape().name().toLowerCase(java.util.Locale.ROOT))));
        }
    }
}
