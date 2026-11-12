package dev.jensh.buildingpalette;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Minecraft 1.21.1 resolves pick-block on the client; material picks need the server's inventory rules. */
public record PickPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<PickPayload> TYPE = new Type<>(BuildingPalette.id("pick"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PickPayload> CODEC = StreamCodec.of(
        (buf, p) -> buf.writeBlockPos(p.pos), buf -> new PickPayload(buf.readBlockPos()));
    @Override public Type<PickPayload> type() { return TYPE; }
    public void apply(ServerPlayer player) {
        if (!player.level().isLoaded(pos)) return;
        ItemStack picked = PickBlock.pick(player, pos, player.level().getBlockState(pos), false);
        if (picked == null || picked.isEmpty()) return;
        Inventory inventory = player.getInventory();
        int slot = inventory.findSlotMatchingItem(picked);
        if (slot != -1) {
            if (Inventory.isHotbarSlot(slot)) inventory.selected = slot;
            else inventory.pickSlot(slot);
        } else if (player.hasInfiniteMaterials()) inventory.setPickedItem(picked);
        player.connection.send(new ClientboundSetCarriedItemPacket(inventory.selected));
        player.inventoryMenu.broadcastChanges();
    }
}
