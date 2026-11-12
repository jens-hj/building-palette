package dev.jensh.buildingpalette.mixin;

import dev.jensh.buildingpalette.OffgridHosts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets small Offgrid pieces stand on any free half cell of a combined block. */
@Mixin(targets = "dev.tazer.offgrid.world.Surfaces", remap = false)
public abstract class OffgridSurfaceMixin {
    @Inject(method = "offsetFor", at = @At("RETURN"), cancellable = true, require = 0)
    private static void palette$halfCell(Level level, BlockPos clicked, Direction face, Vec3 location, BlockState placed, CallbackInfoReturnable<Vec3> cir) {
        if (cir.getReturnValue() == null) cir.setReturnValue(OffgridHosts.halfCellOffset(level, clicked, face, location, placed));
    }
}
