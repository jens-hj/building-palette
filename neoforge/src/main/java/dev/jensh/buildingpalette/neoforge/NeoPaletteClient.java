package dev.jensh.buildingpalette.neoforge;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.client.*;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.*;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class NeoPaletteClient {
    public static void initialize(IEventBus bus, ModContainer container) {
        PaletteClient.initialize();
        container.registerExtensionPoint(IConfigScreenFactory.class, (c, parent) -> new PaletteConfigScreen(parent));
        bus.addListener((ModelEvent.ModifyBakingResult event) -> event.getBakingResult().blockStateModels().replaceAll((state, model) ->
            state.is(BuildingPalette.MULTIPART.get()) ? new NeoMultipartModel(model) : model));
        bus.addListener((RegisterGuiLayersEvent event) -> event.registerAboveAll(BuildingPalette.id("palette"), (graphics, delta) -> {
            if (Minecraft.getInstance().gui.screen() == null) PaletteClient.draw(graphics);
        }));
        bus.addListener((RegisterClientExtensionsEvent event) -> event.registerBlock(new IClientBlockExtensions() {
            @Override public void collectDynamicTintValues(net.minecraft.world.level.block.state.BlockState state, net.minecraft.client.renderer.block.BlockAndTintGetter level,
                    net.minecraft.core.BlockPos pos, it.unimi.dsi.fastutil.ints.IntList colors) {
                colors.addAll(NeoMultipartModel.tints(NeoMultipartModel.cells(level, pos), level, pos).colors());
            }
        }, BuildingPalette.MULTIPART.get()));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> PaletteConfig.sync());
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent event) -> PaletteClient.tooltip(event.getItemStack(), event.getToolTip()));
    }
    private NeoPaletteClient() {}
}
