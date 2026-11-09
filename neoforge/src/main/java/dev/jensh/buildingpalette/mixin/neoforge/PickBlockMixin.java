package dev.jensh.buildingpalette.mixin.neoforge;

import dev.jensh.buildingpalette.PickBlock;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PickBlockMixin {
    @Shadow public ServerPlayer player;
    @WrapOperation(method = "handlePickItemFromBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getCloneItemStack(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/LevelReader;ZLnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack palette$materialPick(BlockState state, BlockPos pos, LevelReader level, boolean data, net.minecraft.world.entity.player.Player picker, Operation<ItemStack> original) {
        var picked = PickBlock.pick(player, pos, state, data);
        return picked == null ? original.call(state, pos, level, data, picker) : picked;
    }
}
