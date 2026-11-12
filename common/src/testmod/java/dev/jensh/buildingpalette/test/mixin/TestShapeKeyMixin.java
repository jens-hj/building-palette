package dev.jensh.buildingpalette.test.mixin;

import dev.jensh.buildingpalette.test.TestInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.tazer.clutternomore.client.ClutterNoMoreClient", remap = false)
public abstract class TestShapeKeyMixin {
    @Inject(method = "isShapeKeyPhysicallyDown", at = @At("HEAD"), cancellable = true)
    private static void palette$testModifier(CallbackInfoReturnable<Boolean> cir) { if (TestInput.alt) cir.setReturnValue(true); }
}
