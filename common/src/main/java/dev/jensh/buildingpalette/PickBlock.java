package dev.jensh.buildingpalette;

import dev.jensh.buildingpalette.core.*;
import dev.jensh.buildingpalette.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

public final class PickBlock {
    public record Target(String material, Shape shape) {}
    private PickBlock() {}
    public static Target target(ServerPlayer player, BlockPos pos, BlockState state) {
        if (!player.level().isLoaded(pos) || !player.isWithinBlockInteractionRange(pos, 1)) return null;
        if (player.level().getBlockEntity(pos) instanceof MultipartEntity entity) {
            HitResult result = Placement.pick(player);
            if (!(result instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(pos)) return null;
            String material = entity.cells().at(Placement.hitCell(hit, false));
            if (material == null) return null;
            int mask = 0;
            for (int i = 0; i < 8; i++) if (material.equals(entity.cells().at(i))) mask |= 1 << i;
            return new Target(material, ShapePicker.closest(mask));
        }
        var selection = Materials.get(new ItemStack(state.getBlock().asItem()));
        return selection == null ? null : new Target(selection.material(), selection.shape());
    }
    public static ItemStack pick(ServerPlayer player, BlockPos pos, BlockState state, boolean includeData) {
        if (player.isSpectator() || includeData) return null;
        Target target = target(player, pos, state);
        if (target == null) return null;
        ItemStack held = player.getMainHandItem();
        var current = Materials.get(held);
        if (current != null && current.material().equals(target.material)) {
            ItemStack shaped = reshape(held, target.shape);
            if (!shaped.isEmpty()) player.setItemInHand(InteractionHand.MAIN_HAND, shaped);
            return player.getMainHandItem().copy();
        }
        // First pick selects existing material without changing its shape or count.
        for (ItemStack stored : player.getInventory().getNonEquipmentItems()) {
            var selection = Materials.get(stored);
            if (selection != null && selection.material().equals(target.material)) return stored.copy();
        }
        var offhand = Materials.get(player.getOffhandItem());
        if (offhand != null && offhand.material().equals(target.material)) {
            ItemStack picked = player.getOffhandItem().copy();
            player.setItemInHand(InteractionHand.OFF_HAND, held);
            player.setItemInHand(InteractionHand.MAIN_HAND, picked);
            return picked.copy();
        }
        if (!player.hasInfiniteMaterials() && Platform.INSTANCE.isModLoaded("travelersbackpack")) {
            ItemStack stored = Platform.INSTANCE.pickBackpack(player, target.material);
            if (!stored.isEmpty()) return stored;
        }
        return player.hasInfiniteMaterials() ? Materials.piece(target.material, target.shape, target.shape.cost) : ItemStack.EMPTY;
    }
    public static ItemStack reshape(ItemStack source, Shape shape) {
        ItemStack result = source;
        for (int i = 0; i < Shape.values().length; i++) {
            var selection = Materials.get(result);
            if (selection == null) return ItemStack.EMPTY;
            if (selection.shape() == shape) return result.copy();
            result = ScrollPayload.convert(result, 1);
            if (result.isEmpty()) break;
        }
        return ItemStack.EMPTY;
    }
}
