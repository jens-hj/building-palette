package dev.jensh.buildingpalette.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Offgrid only lets fences, walls, panes, bars and chains into a stair or step when they
 * run along it, and rejects any host that would notch them. Building Palette builds
 * arbitrary half-grid shapes, so a blocklogged piece may share any host as long as part
 * of it stays visible: Offgrid still cuts the overlapping part from its outline,
 * collision and support shapes, and rejects a piece the host hides completely.
 * Optional: a changed Offgrid only loses this allowance.
 */
@Mixin(targets = "dev.tazer.offgrid.world.Blocklogging", remap = false)
public abstract class OffgridBlocklogMixin {
    @WrapOperation(method = "compute", require = 0, at = @At(value = "INVOKE",
        target = "Ldev/tazer/offgrid/world/Blocklogging;runAxis(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/core/Direction$Axis;"))
    private static Direction.Axis palette$anyRun(BlockState state, BlockState host, Operation<Direction.Axis> original) {
        return null;
    }
    @WrapOperation(method = "fit", require = 0, at = @At(value = "INVOKE",
        target = "Ldev/tazer/offgrid/world/Blocklogging;runAxis(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/core/Direction$Axis;"))
    private static Direction.Axis palette$fitAnyRun(BlockState state, BlockState host, Operation<Direction.Axis> original) {
        return null;
    }
    @WrapOperation(method = "compute", require = 0, at = @At(value = "INVOKE",
        target = "Ldev/tazer/offgrid/world/Blocklogging;thins(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/Vec3;)Z"))
    private static boolean palette$notchAllowed(BlockState state, VoxelShape outline, VoxelShape hostShape, Vec3 offset, Operation<Boolean> original) {
        return false;
    }
}
