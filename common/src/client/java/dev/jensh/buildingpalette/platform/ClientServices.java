package dev.jensh.buildingpalette.platform;

import net.minecraft.world.inventory.Slot;
import dev.jensh.buildingpalette.*;

public interface ClientServices {
    Slot hoveredSlot();
    void closeOverlay();
    boolean shapeKeyDown();
    void sendScroll(ScrollPayload payload);
    void syncPreferences(PreferencesPayload payload);
    /** False when the server cannot handle material picks. */
    boolean sendPick(PickPayload payload);
}
