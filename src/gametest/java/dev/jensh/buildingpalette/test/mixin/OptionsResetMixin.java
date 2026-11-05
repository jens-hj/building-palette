package dev.jensh.buildingpalette.test.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Some RPG mods register keys after Fabric's default-options snapshot. Keep fixture settings
 * rather than restoring absent snapshot entries as null key names. Only part of the test mod. */
@Mixin(targets = "net.fabricmc.fabric.impl.client.gametest.context.ClientGameTestContextImpl", remap = false)
public abstract class OptionsResetMixin {
    @Inject(method = "restoreDefaultGameOptions", at = @At("HEAD"), cancellable = true)
    private void palette$fixtureOptions(CallbackInfo ci) {
        if (Boolean.getBoolean("building_palette.rpg_test")) ci.cancel();
    }
}
