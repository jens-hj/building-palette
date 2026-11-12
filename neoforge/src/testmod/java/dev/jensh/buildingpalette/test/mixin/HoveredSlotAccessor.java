package dev.jensh.buildingpalette.test.mixin;

import dev.jensh.buildingpalette.test.NeoInput;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class HoveredSlotAccessor {
    @Shadow protected Slot hoveredSlot;
    @Inject(method = "render", at = @At("TAIL"))
    private void palette$testHover(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (NeoInput.hover != null) hoveredSlot = NeoInput.hover;
    }
}
