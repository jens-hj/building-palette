package dev.jensh.buildingpalette.fabric;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.client.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;

public final class FabricPaletteClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        PaletteClient.initialize();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> PaletteConfig.sync());
        ModelLoadingPlugin.register(context -> context.modifyBlockModelAfterBake().register((model, bake) -> bake.state().is(BuildingPalette.MULTIPART.get()) ? new MultipartModel(model) : model));
        HudElementRegistry.addLast(BuildingPalette.id("palette"), (graphics, delta) -> { if (Minecraft.getInstance().gui.screen() == null) PaletteClient.draw(graphics); });
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> PaletteClient.tooltip(stack, lines));
    }
}
