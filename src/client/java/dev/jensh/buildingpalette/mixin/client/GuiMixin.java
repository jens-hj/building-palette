package dev.jensh.buildingpalette.mixin.client;

import dev.jensh.buildingpalette.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiMixin {
    @Inject(method = "itemCount", at = @At("HEAD"), cancellable = true)
    private void palette$count(Font font, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        var s = Materials.get(stack);
        if (s == null || text != null) return;
        int units = MaterialStacks.units(stack), count = MaterialStacks.available(stack);
        String label = Integer.toString(count) + (units % s.shape().cost != 0 ? "+" : "");
        if (count != 1 || MaterialStacks.missing(stack) > 0) ((GuiGraphicsExtractor)(Object)this).text(font, label, x + 17 - font.width(label), y + 9, -1, true);
        ci.cancel();
    }
}
