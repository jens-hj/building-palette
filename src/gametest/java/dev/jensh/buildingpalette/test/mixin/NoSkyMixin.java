package dev.jensh.buildingpalette.test.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.renderer.SkyRenderer.class)
public abstract class NoSkyMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void palette$noSky(CallbackInfo ci) {
        if (dev.jensh.buildingpalette.test.SkyMask.active) ci.cancel();
    }
}
