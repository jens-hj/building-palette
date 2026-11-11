package dev.jensh.buildingpalette.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.jensh.buildingpalette.Materials;
import dev.jensh.buildingpalette.core.Shape;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Offgrid only lets fences, walls, panes and bars into a step when they run along it,
 * and otherwise rejects pieces the host would notch. Building Palette places steps
 * anywhere in the half-grid, so let a post stand in any step block; Offgrid still cuts
 * the overlapping part from its outline, collision and support shapes.
 * Optional: a changed Offgrid only loses this allowance.
 */
@Mixin(targets = "dev.tazer.offgrid.world.Blocklogging", remap = false)
public abstract class OffgridStepBlocklogMixin {
    private static boolean palette$step(BlockState host) {
        return Materials.classify(host.getBlock()) == Shape.STEP;
    }
    @WrapOperation(method = "compute", require = 0, at = @At(value = "INVOKE",
        target = "Ldev/tazer/offgrid/world/Blocklogging;runAxis(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/core/Direction$Axis;"))
    private static Direction.Axis palette$anyRunInStep(BlockState state, BlockState host, Operation<Direction.Axis> original) {
        return palette$step(host) ? null : original.call(state, host);
    }
    @WrapOperation(method = "compute", require = 0, at = @At(value = "INVOKE",
        target = "Ldev/tazer/offgrid/world/Blocklogging;thins(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/Vec3;)Z"))
    private static boolean palette$notchInStep(BlockState state, VoxelShape outline, VoxelShape hostShape, Vec3 offset, Operation<Boolean> original,
            @Local(argsOnly = true, ordinal = 1) BlockState host) {
        return !palette$step(host) && original.call(state, outline, hostShape, offset);
    }
}
