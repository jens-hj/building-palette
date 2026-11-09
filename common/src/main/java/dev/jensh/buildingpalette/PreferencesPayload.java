package dev.jensh.buildingpalette;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PreferencesPayload(boolean precisePlacement) implements CustomPacketPayload {
    public static final Type<PreferencesPayload> TYPE = new Type<>(BuildingPalette.id("preferences"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PreferencesPayload> CODEC = StreamCodec.of(
        (buf, p) -> buf.writeBoolean(p.precisePlacement), buf -> new PreferencesPayload(buf.readBoolean()));
    @Override public Type<PreferencesPayload> type() { return TYPE; }
}
