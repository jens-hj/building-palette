package dev.jensh.buildingpalette.mixin.neoforge;

import dev.jensh.buildingpalette.MultipartBlock;
import dev.jensh.buildingpalette.neoforge.SableCollisions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sable's neighbourhood cache also samples at the origin, independently of the collider bakery. */
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.physics.chunk.VoxelNeighborhoodState", remap = false)
abstract class SableNeighborhoodMixin {
    @Inject(method = "isSolid", at = @At("HEAD"), cancellable = true)
    private static void palette$solid(BlockGetter level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof MultipartBlock)
            cir.setReturnValue(SableCollisions.physicsState(state, level, pos).getValue(MultipartBlock.OCCLUSION) != 0);
    }
    @Inject(method = "isFullBlock", at = @At("HEAD"), cancellable = true)
    private static void palette$full(BlockGetter level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof MultipartBlock)
            cir.setReturnValue(SableCollisions.physicsState(state, level, pos).getValue(MultipartBlock.OCCLUSION) == 255);
    }
}
