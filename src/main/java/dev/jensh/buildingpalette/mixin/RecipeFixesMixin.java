package dev.jensh.buildingpalette.mixin;

import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets = "dev.tazer.clutternomore.recipe.RecipeFixes", remap = false)
public abstract class RecipeFixesMixin {
    @Inject(method = "isEnabled", at = @At("HEAD"), cancellable = true)
    private static void palette$nativeMatching(CallbackInfoReturnable<Boolean> cir) { cir.setReturnValue(false); }
    @Inject(method = "generate", at = @At("HEAD"), cancellable = true)
    private static void palette$nativeRecipes(ResourceManager resources, CallbackInfo ci) { ci.cancel(); }
}
