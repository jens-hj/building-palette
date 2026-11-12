package dev.jensh.buildingpalette;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public record ScrollPayload(int container, int slot, int direction) implements CustomPacketPayload {
    public static final Type<ScrollPayload> TYPE = new Type<>(BuildingPalette.id("scroll"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScrollPayload> CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeVarInt(p.container); buf.writeVarInt(p.slot); buf.writeVarInt(p.direction); },
        buf -> new ScrollPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
    @Override public Type<ScrollPayload> type() { return TYPE; }
    public void apply(ServerPlayer player) {
        if (player.isSpectator() || Math.abs(direction) != 1) return;
        var menu = player.containerMenu;
        if (menu.containerId != container || !menu.stillValid(player) || slot < 0 || slot >= menu.slots.size()) return;
        Slot target = menu.getSlot(slot);
        ItemStack source = target.getItem();
        if (!target.mayPickup(player)) return;
        ItemStack converted = convert(source, direction);
        // Output slots cannot be transmuted, including pending recipe results.
        if (converted.isEmpty() || !target.mayPlace(converted)) return;
        target.setByPlayer(converted);
        menu.broadcastChanges();
    }
    public static ItemStack convert(ItemStack source, int direction) {
        var current = Materials.get(source);
        if (current == null || MaterialStacks.units(source) > 512) return ItemStack.EMPTY;
        var palette = Materials.palette(source);
        int index = 0;
        for (int i = 0; i < palette.size(); i++) if (Materials.get(palette.get(i)).shape() == current.shape()) index = i;
        return reshape(source, palette.get(Math.floorMod(index + direction, palette.size())), MaterialStacks.units(source));
    }
    /** The given units of the source's material as the target palette entry, keeping the source's other components. */
    public static ItemStack reshape(ItemStack source, ItemStack target, int units) {
        ItemStack converted = source.transmuteCopy(target.getItem(), 1);
        converted.remove(BuildingPalette.MATERIAL.get()); converted.remove(BuildingPalette.SHAPE.get()); MaterialStacks.clearRemainder(converted);
        if (target.has(BuildingPalette.MATERIAL.get())) converted.set(BuildingPalette.MATERIAL.get(), target.get(BuildingPalette.MATERIAL.get()));
        if (target.has(BuildingPalette.SHAPE.get())) converted.set(BuildingPalette.SHAPE.get(), target.get(BuildingPalette.SHAPE.get()));
        return MaterialStacks.withUnits(converted, units);
    }
}
