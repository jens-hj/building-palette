package dev.jensh.buildingpalette.mixin.client;

import dev.jensh.buildingpalette.client.PaletteClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenMixin {
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    private void palette$hideLegacyTooltip(GuiGraphics graphics, int x, int y, CallbackInfo ci) {
        if (!PaletteClient.active()) return;
        var slot = PaletteClient.target();
        if (slot != null) PaletteClient.draw(graphics, leftPos + slot.x - 3, topPos + slot.y - 3);
        ci.cancel();
    }
}
