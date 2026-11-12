package dev.jensh.buildingpalette.fabric;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.client.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

public final class FabricPaletteClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        PaletteClient.initialize();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> PaletteConfig.sync());
        var carrier = BuildingPalette.id("multipart");
        var piece = ModelResourceLocation.inventory(BuildingPalette.id("piece"));
        ModelLoadingPlugin.register(plugin -> plugin.modifyModelAfterBake().register((model, context) -> {
            var id = context.topLevelId();
            if (id == null || model == null) return model;
            if (id.equals(piece)) return new PieceItemModel(model, resolver -> new ItemOverrides(null, null, List.of()) {
                @Override public BakedModel resolve(BakedModel item, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
                    return resolver.resolve(item, stack, level, entity, seed);
                }
            });
            return id.id().equals(carrier) && !id.getVariant().equals(ModelResourceLocation.INVENTORY_VARIANT) ? new FabricMultipartModel(model) : model;
        }));
        ColorProviderRegistry.BLOCK.register(FabricMultipartModel::tint, BuildingPalette.MULTIPART.get());
        ColorProviderRegistry.ITEM.register((stack, index) -> {
            var selection = Materials.get(stack);
            var colors = selection == null ? null : ColorProviderRegistry.ITEM.get(selection.parent());
            return colors == null ? -1 : colors.getColor(new ItemStack(selection.parent()), index);
        }, BuildingPalette.PIECE.get());
        HudRenderCallback.EVENT.register((graphics, delta) -> { if (Minecraft.getInstance().screen == null) PaletteClient.draw(graphics); });
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> PaletteClient.tooltip(stack, lines));
        WorldRenderEvents.BLOCK_OUTLINE.register((context, outline) -> {
            if (!(Minecraft.getInstance().hitResult instanceof BlockHitResult hit) || !hit.getBlockPos().equals(outline.blockPos())) return true;
            return !PaletteOutline.render(context.matrixStack(), context.consumers(), context.camera().getPosition(), hit);
        });
    }
}
