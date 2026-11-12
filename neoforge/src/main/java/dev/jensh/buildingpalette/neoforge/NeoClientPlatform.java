package dev.jensh.buildingpalette.neoforge;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.platform.ClientServices;
import dev.tazer.clutternomore.client.ClutterNoMoreClient;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.Slot;

public final class NeoClientPlatform implements ClientServices {
    @Override public void closeOverlay() { ClutterNoMoreClient.closeOverlay(); }
    @Override public Slot hoveredSlot() { return ClutterNoMoreClient.getHoveredSlot(); }
    @Override public boolean shapeKeyDown() { return ClutterNoMoreClient.isShapeKeyPhysicallyDown(); }
    @Override public void sendScroll(ScrollPayload payload) { PacketDistributor.sendToServer(payload); }
    @Override public void syncPreferences(PreferencesPayload payload) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null && connection.hasChannel(PreferencesPayload.TYPE)) PacketDistributor.sendToServer(payload);
    }
    @Override public boolean sendPick(PickPayload payload) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null || !connection.hasChannel(PickPayload.TYPE)) return false;
        PacketDistributor.sendToServer(payload);
        return true;
    }
}
