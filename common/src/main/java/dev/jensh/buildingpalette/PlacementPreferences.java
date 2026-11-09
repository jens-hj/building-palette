package dev.jensh.buildingpalette;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Per-connection preferences; one player's placement controls never change another's. */
public final class PlacementPreferences {
    private static final Map<UUID, Boolean> PRECISE = new ConcurrentHashMap<>();
    public static BooleanSupplier clientPrecise = () -> false;
    private PlacementPreferences() {}
    public static boolean precise(Player player) {
        return player instanceof ServerPlayer ? PRECISE.getOrDefault(player.getUUID(), false) : clientPrecise.getAsBoolean();
    }
    public static void set(ServerPlayer player, boolean precise) {
        if (precise) PRECISE.put(player.getUUID(), true);
        else PRECISE.remove(player.getUUID());
    }
    public static void remove(ServerPlayer player) { PRECISE.remove(player.getUUID()); }
}
