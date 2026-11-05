package dev.jensh.buildingpalette.mixin.client;

import dev.jensh.buildingpalette.client.PaletteClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void palette$draw(GuiGraphicsExtractor graphics, int x, int y, float delta, CallbackInfo ci) { PaletteClient.draw(graphics); }
}
