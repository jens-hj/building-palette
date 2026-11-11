package dev.jensh.buildingpalette.mixin;

import dev.jensh.buildingpalette.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Offgrid resolves every host through Covers.raw; show it a carrier's cells as a state. */
@Mixin(targets = "dev.tazer.offgrid.world.Covers", remap = false)
public abstract class OffgridHostShapeMixin {
    @Inject(method = "raw", at = @At("RETURN"), cancellable = true, require = 0)
    private static void palette$carrierHost(BlockGetter getter, BlockPos pos, BlockState state, CallbackInfoReturnable<BlockState> cir) {
        if (cir.getReturnValue().is(BuildingPalette.MULTIPART.get())) cir.setReturnValue(OffgridHosts.proxy(getter, pos));
    }
}
