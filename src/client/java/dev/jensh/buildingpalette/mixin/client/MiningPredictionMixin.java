package dev.jensh.buildingpalette.mixin.client;

import dev.jensh.buildingpalette.MultipartEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MiningPredictionMixin {
    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void palette$keepRemainingMaterials(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        var level = Minecraft.getInstance().level;
        // Keep the carrier and its render data until the authoritative cell update.
        // Vanilla prediction would delete every material and discard its block entity.
        if (level != null && level.getBlockEntity(pos) instanceof MultipartEntity) cir.setReturnValue(true);
    }
}
