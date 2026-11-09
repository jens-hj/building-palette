package dev.jensh.buildingpalette.mixin;

import dev.jensh.buildingpalette.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class MiningMixin {
    @Shadow @Final protected ServerPlayer player;
    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void palette$mineConnected(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (Placement.mine(player, pos)) cir.setReturnValue(true);
    }
}
