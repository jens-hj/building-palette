package dev.jensh.buildingpalette.fabric;

import dev.jensh.buildingpalette.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class FabricPalette implements ModInitializer {
    @Override public void onInitialize() {
        BuildingPalette.components((id, value) -> Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id, value));
        BuildingPalette.items((id, value) -> Registry.register(BuiltInRegistries.ITEM, id, value));
        BuildingPalette.blocks((id, value) -> Registry.register(BuiltInRegistries.BLOCK, id, value));
        BuildingPalette.ENTITY.bind(Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, BuildingPalette.id("multipart"),
            FabricBlockEntityTypeBuilder.<MultipartEntity>create(FabricMultipartEntity::new, BuildingPalette.MULTIPART.get()).build()));
        PaletteState.ready = true;
        PayloadTypeRegistry.serverboundPlay().register(ScrollPayload.TYPE, ScrollPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PreferencesPayload.TYPE, PreferencesPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PreferencesPayload.TYPE, (payload, context) -> context.server().execute(() -> PlacementPreferences.set(context.player(), payload.precisePlacement())));
        ServerPlayNetworking.registerGlobalReceiver(ScrollPayload.TYPE, (payload, context) -> context.server().execute(() -> payload.apply(context.player())));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PlacementPreferences.remove(handler.player));
        UseBlockCallback.EVENT.register(Placement::use);
        var phase = BuildingPalette.id("material_pick");
        PlayerPickItemEvents.BLOCK.addPhaseOrdering(phase, Event.DEFAULT_PHASE);
        PlayerPickItemEvents.BLOCK.register(phase, PickBlock::pick);
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((entity, level) -> { if (entity instanceof MultipartEntity part) LoadedParts.loaded(part); });
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((entity, level) -> { if (entity instanceof MultipartEntity part) LoadedParts.unloaded(part); });
        ServerTickEvents.END_LEVEL_TICK.register(LoadedParts::endTick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> LoadedParts.clear());
    }
}
