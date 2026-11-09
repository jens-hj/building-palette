package dev.jensh.buildingpalette.fabric;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.platform.ClientServices;
import dev.tazer.clutternomore.client.ClutterNoMoreClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.inventory.Slot;

public final class FabricClientPlatform implements ClientServices {
    @Override public void closeOverlay() { ClutterNoMoreClient.closeOverlay(); }
    @Override public Slot hoveredSlot() { return ClutterNoMoreClient.getHoveredSlot(); }
    @Override public boolean shapeKeyDown() { return ClutterNoMoreClient.isShapeKeyPhysicallyDown(); }
    @Override public void sendScroll(ScrollPayload payload) { ClientPlayNetworking.send(payload); }
    @Override public void syncPreferences(PreferencesPayload payload) { if (ClientPlayNetworking.canSend(PreferencesPayload.TYPE)) ClientPlayNetworking.send(payload); }
}
