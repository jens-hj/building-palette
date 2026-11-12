package dev.jensh.buildingpalette.test.mixin;

import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Showcase mask pass: no sky, magenta clear color, so sky pixels can be cut out exactly. */
@Mixin(net.minecraft.client.renderer.fog.FogRenderer.class)
public abstract class SkyMaskMixin {

    @Inject(method = "computeFogColor", at = @At("TAIL"))
    private void palette$magenta(net.minecraft.client.Camera camera, float partialTick, net.minecraft.client.multiplayer.ClientLevel level, int renderDistance, float darkenWorldAmount, Vector4f color, CallbackInfo ci) {
        if (dev.jensh.buildingpalette.test.SkyMask.active) color.set(1, 0, 1, 1);
    }
}
