package dev.jensh.buildingpalette;

import com.mojang.serialization.Codec;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerPickItemEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class BuildingPalette implements ModInitializer {
    public static final String ID = "building_palette";
    public static final DataComponentType<Integer> MISSING = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("missing_units"),
        DataComponentType.<Integer>builder().persistent(Codec.intRange(0, 7)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    public static final DataComponentType<String> MATERIAL = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("material"),
        DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
    public static final DataComponentType<Integer> SHAPE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("shape"),
        DataComponentType.<Integer>builder().persistent(Codec.intRange(0, 8)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    public static final Item PIECE = Registry.register(BuiltInRegistries.ITEM, id("piece"), new Item(new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM, id("piece")))));
    public static final MultipartBlock MULTIPART = Registry.register(BuiltInRegistries.BLOCK, id("multipart"),
        new MultipartBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id("multipart")))
            .strength(2.0f).dynamicShape()));
    public static final BlockEntityType<MultipartEntity> ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("multipart"),
        FabricBlockEntityTypeBuilder.create(MultipartEntity::new, MULTIPART).build());

    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(ID, path); }
    @Override public void onInitialize() {
        PaletteState.ready = true;
        PayloadTypeRegistry.serverboundPlay().register(ScrollPayload.TYPE, ScrollPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PreferencesPayload.TYPE, PreferencesPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PreferencesPayload.TYPE, (payload, context) -> context.server().execute(
            () -> PlacementPreferences.set(context.player(), payload.precisePlacement())));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PlacementPreferences.remove(handler.player));
        ServerPlayNetworking.registerGlobalReceiver(ScrollPayload.TYPE, (payload, context) -> context.server().execute(() -> payload.apply(context.player())));
        UseBlockCallback.EVENT.register(Placement::use);
        var pickPhase = id("material_pick");
        PlayerPickItemEvents.BLOCK.addPhaseOrdering(pickPhase, Event.DEFAULT_PHASE);
        PlayerPickItemEvents.BLOCK.register(pickPhase, PickBlock::pick);
        // Scheduling a block tick during BLOCK_ENTITY_LOAD is too early: the
        // chunk's tick container may not yet be registered. Drain after world
        // ticking so legacy states reliably receive their new light shape.
        var pending = new java.util.IdentityHashMap<net.minecraft.server.level.ServerLevel, java.util.Set<MultipartEntity>>();
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof MultipartEntity multipart) pending.computeIfAbsent(level,
                ignored -> java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>())).add(multipart);
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((entity, level) -> {
            var loaded = pending.get(level);
            if (loaded != null) { loaded.remove(entity); if (loaded.isEmpty()) pending.remove(level); }
        });
        ServerTickEvents.END_LEVEL_TICK.register(level -> {
            var loaded = pending.remove(level);
            if (loaded != null) for (var entity : loaded) if (!entity.isRemoved()) entity.refreshPropertiesOnLoad();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> pending.keySet().removeIf(level -> level.getServer() == server));
    }
}
