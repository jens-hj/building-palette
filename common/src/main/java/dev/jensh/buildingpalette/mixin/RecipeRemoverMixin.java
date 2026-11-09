package dev.jensh.buildingpalette.mixin;

import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.tazer.clutternomore.recipe.RecipeRemover", remap = false)
public abstract class RecipeRemoverMixin {
    @Inject(method = "removeShapeRecipes", at = @At("HEAD"), cancellable = true)
    private static void palette$keepNativeRecipes(RecipeManager manager, CallbackInfo ci) { ci.cancel(); }
}
