package dev.jensh.buildingpalette.mixin.client;

import dev.jensh.buildingpalette.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(GuiGraphics.class)
public abstract class GuiMixin {
    /** Show usable pieces instead of the raw stack count; "+" marks a reserved remainder. */
    @ModifyVariable(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
        at = @At("HEAD"), argsOnly = true)
    private String palette$count(String text, Font font, ItemStack stack) {
        var s = Materials.get(stack);
        if (s == null || text != null) return text;
        int units = MaterialStacks.units(stack), count = MaterialStacks.available(stack);
        if (count == 1 && MaterialStacks.missing(stack) == 0) return null;
        return count + (units % s.shape().cost != 0 ? "+" : "");
    }
}
