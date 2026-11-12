package dev.jensh.buildingpalette.neoforge;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class NeoPaletteClient {
    public static void initialize(IEventBus bus, ModContainer container) {
        PaletteClient.initialize();
        container.registerExtensionPoint(IConfigScreenFactory.class, (c, parent) -> new PaletteConfigScreen(parent));
        bus.addListener((ModelEvent.ModifyBakingResult event) -> {
            var models = event.getModels();
            NeoMultipartModel carrier = null;
            for (var state : BuildingPalette.MULTIPART.get().getStateDefinition().getPossibleStates()) {
                var location = BlockModelShaper.stateToModelLocation(state);
                var original = models.get(location);
                if (original == null) continue;
                if (carrier == null) carrier = new NeoMultipartModel(original);
                models.put(location, carrier);
            }
            var piece = ModelResourceLocation.inventory(BuildingPalette.id("piece"));
            if (models.containsKey(piece)) models.put(piece, new PieceItemModel(models.get(piece), resolver -> new net.minecraft.client.renderer.block.model.ItemOverrides() {
                @Override public net.minecraft.client.resources.model.BakedModel resolve(net.minecraft.client.resources.model.BakedModel model, ItemStack stack, net.minecraft.client.multiplayer.ClientLevel level, net.minecraft.world.entity.LivingEntity entity, int seed) {
                    return resolver.resolve(model, stack, level, entity, seed);
                }
            }));
        });
        bus.addListener((RegisterColorHandlersEvent.Block event) -> event.register(NeoMultipartModel::tint, BuildingPalette.MULTIPART.get()));
        bus.addListener((RegisterColorHandlersEvent.Item event) -> event.register((stack, index) -> {
            var selection = Materials.get(stack);
            return selection == null ? -1 : event.getItemColors().getColor(new ItemStack(selection.parent()), index);
        }, BuildingPalette.PIECE.get()));
        bus.addListener((RegisterGuiLayersEvent event) -> event.registerAboveAll(BuildingPalette.id("palette"), (graphics, delta) -> {
            if (Minecraft.getInstance().screen == null) PaletteClient.draw(graphics);
        }));
        NeoForge.EVENT_BUS.addListener((RenderHighlightEvent.Block event) -> {
            if (PaletteOutline.render(event.getPoseStack(), event.getMultiBufferSource(), event.getCamera().getPosition(), event.getTarget())) event.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> PaletteConfig.sync());
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent event) -> PaletteClient.tooltip(event.getItemStack(), event.getToolTip()));
    }
    private NeoPaletteClient() {}
}
