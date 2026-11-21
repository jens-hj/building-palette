package dev.jensh.buildingpalette.mixin.neoforge;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.jensh.buildingpalette.MultipartBlock;
import dev.jensh.buildingpalette.neoforge.CreateCollisions;
import dev.jensh.buildingpalette.neoforge.SableCollisions;
import dev.ryanhcode.sable.api.sublevel.KinematicContraption;
import net.neoforged.fml.ModList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.physics.impl.rapier.RapierPhysicsPipeline", remap = false)
abstract class SableRapierPipelineMixin {
    @Shadow @Final private ServerLevel level;
    private static final String BAKE = "Ldev/ryanhcode/sable/physics/impl/rapier/collider/RapierVoxelColliderBakery;getPhysicsDataForBlock(Lnet/minecraft/world/level/block/state/BlockState;)Ldev/ryanhcode/sable/physics/impl/rapier/collider/RapierVoxelColliderData;";

    @ModifyArg(method = "handleChunkSectionAddition", at = @At(value = "INVOKE", target = BAKE), index = 0)
    private BlockState palette$chunkCollider(BlockState state, @Local(ordinal = 0) BlockPos pos) {
        return SableCollisions.physicsState(state, level, pos);
    }

    // The first bake is a neighbour. The second is the block that changed.
    @ModifyArg(method = "handleBlockChange", at = @At(value = "INVOKE", target = BAKE, ordinal = 0), index = 0)
    private BlockState palette$neighborCollider(BlockState state, @Local(ordinal = 1) BlockPos neighbor) {
        return SableCollisions.physicsState(state, level, neighbor);
    }
    @ModifyArg(method = "handleBlockChange", at = @At(value = "INVOKE", target = BAKE, ordinal = 1), index = 0)
    private BlockState palette$changedCollider(BlockState state, @Local(ordinal = 0) BlockPos pos) {
        return SableCollisions.physicsState(state, level, pos);
    }

    @WrapOperation(method = "add(Ldev/ryanhcode/sable/api/sublevel/KinematicContraption;)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/BlockGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState palette$contraptionCollider(BlockGetter world, BlockPos pos, Operation<BlockState> original,
            @Local(argsOnly = true) KinematicContraption contraption) {
        var state = original.call(world, pos);
        if (state.getBlock() instanceof MultipartBlock && ModList.get().isLoaded("create"))
            return CreateCollisions.physicsState(contraption, world, pos, state);
        return SableCollisions.physicsState(state, world, pos);
    }
}
