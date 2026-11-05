package dev.jensh.buildingpalette.mixin.client;

import dev.jensh.buildingpalette.client.PaletteClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets = "dev.tazer.clutternomore.client.ClutterNoMoreClient", remap = false)
public abstract class ClutterClientMixin {
    @Inject(method = "allowScreenScroll", at = @At("HEAD"), cancellable = true)
    private static void palette$inventoryScroll(net.minecraft.client.gui.screens.Screen screen, double mouseX, double mouseY,
            double horizontal, double vertical, CallbackInfoReturnable<Boolean> cir) {
        if (PaletteClient.onScroll(vertical)) cir.setReturnValue(false);
    }
    @Inject(method = "onKeyPress", at = @At("HEAD"), cancellable = true)
    private static void palette$inventoryKey(net.minecraft.client.gui.screens.Screen screen, int key, CallbackInfo ci) {
        if (PaletteClient.managed()) { dev.tazer.clutternomore.client.ClutterNoMoreClient.closeOverlay(); ci.cancel(); }
    }
    @Inject(method = "selectHoveredShape", at = @At("HEAD"), cancellable = true)
    private static void palette$inventoryNumber(int index, CallbackInfoReturnable<Boolean> cir) {
        if (PaletteClient.managed()) cir.setReturnValue(false);
    }
    @Inject(method = "onMouseScrolled", at = @At("HEAD"), cancellable = true)
    private static void palette$scroll(double amount, CallbackInfoReturnable<Boolean> cir) {
        if (PaletteClient.onScroll(amount)) cir.setReturnValue(true);
    }
    @Inject(method = "onKeyInput", at = @At("HEAD"), cancellable = true)
    private static void palette$key(int key, int action, CallbackInfo ci) {
        if (PaletteClient.managed()) { dev.tazer.clutternomore.client.ClutterNoMoreClient.closeOverlay(); ci.cancel(); }
    }
    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private static void palette$overlay(net.minecraft.client.gui.GuiGraphicsExtractor graphics, float delta, CallbackInfo ci) {
        if (PaletteClient.managed()) ci.cancel();
    }
}
