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
        ItemStack target = palette.get(Math.floorMod(index + direction, palette.size()));
        ItemStack converted = source.transmuteCopy(target.getItem(), 1);
        converted.remove(BuildingPalette.MATERIAL); converted.remove(BuildingPalette.SHAPE); converted.remove(BuildingPalette.MISSING);
        if (source.getItem() == BuildingPalette.PIECE && target.getItem() != BuildingPalette.PIECE
                && java.util.Objects.equals(source.get(net.minecraft.core.component.DataComponents.COOKING_FUEL),
                    new ItemStack(current.parent()).get(net.minecraft.core.component.DataComponents.COOKING_FUEL))) {
            converted.remove(net.minecraft.core.component.DataComponents.COOKING_FUEL);
        }
        if (target.has(BuildingPalette.MATERIAL)) converted.set(BuildingPalette.MATERIAL, target.get(BuildingPalette.MATERIAL));
        if (target.has(BuildingPalette.SHAPE)) converted.set(BuildingPalette.SHAPE, target.get(BuildingPalette.SHAPE));
        return MaterialStacks.withUnits(converted, MaterialStacks.units(source));
    }
}
