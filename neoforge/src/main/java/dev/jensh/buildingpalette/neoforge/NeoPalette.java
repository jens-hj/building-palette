package dev.jensh.buildingpalette.neoforge;

import dev.jensh.buildingpalette.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;

@Mod(BuildingPalette.ID)
public final class NeoPalette {
    public NeoPalette(IEventBus bus, ModContainer container) {
        bus.addListener(this::register);
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> PaletteState.ready = true));
        bus.addListener((RegisterPayloadHandlersEvent event) -> {
            var registrar = event.registrar("1");
            registrar.playToServer(ScrollPayload.TYPE, ScrollPayload.CODEC, (payload, context) -> context.enqueueWork(() -> payload.apply((ServerPlayer)context.player())));
            registrar.playToServer(PreferencesPayload.TYPE, PreferencesPayload.CODEC, (payload, context) -> context.enqueueWork(() -> PlacementPreferences.set((ServerPlayer)context.player(), payload.precisePlacement())));
            registrar.playToServer(PickPayload.TYPE, PickPayload.CODEC, (payload, context) -> context.enqueueWork(() -> payload.apply((ServerPlayer)context.player())));
        });
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock event) -> {
            var result = Placement.use(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
            if (result != InteractionResult.PASS) { event.setCancellationResult(result); event.setCanceled(true); }
        });
        NeoForge.EVENT_BUS.addListener(NeoPalette::fuel);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> PlacementPreferences.remove((ServerPlayer)event.getEntity()));
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post event) -> { if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) LoadedParts.endTick(level); });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> LoadedParts.clear());
        if (FMLEnvironment.dist == Dist.CLIENT) NeoPaletteClient.initialize(bus, container);
    }
    /** A shape burns for its share of the full block; a stack with no whole piece left is not fuel. */
    private static void fuel(FurnaceFuelBurnTimeEvent event) {
        ItemStack stack = event.getItemStack();
        var selection = Materials.get(stack);
        if (selection == null) return;
        if (MaterialStacks.available(stack) == 0) { event.setBurnTime(0); return; }
        int base = stack.getItem() == selection.parent() ? event.getBurnTime() : new ItemStack(selection.parent()).getBurnTime(event.getRecipeType());
        event.setBurnTime(base * selection.shape().cost / 8);
    }
    private void register(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.DATA_COMPONENT_TYPE)) BuildingPalette.components((id, value) -> event.register(Registries.DATA_COMPONENT_TYPE, id, () -> value));
        if (event.getRegistryKey().equals(Registries.ITEM)) BuildingPalette.items((id, value) -> event.register(Registries.ITEM, id, () -> value));
        if (event.getRegistryKey().equals(Registries.BLOCK)) BuildingPalette.blocks((id, value) -> event.register(Registries.BLOCK, id, () -> value));
        if (event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) {
            BuildingPalette.ENTITY.bind(BlockEntityType.Builder.<MultipartEntity>of(NeoMultipartEntity::new, BuildingPalette.MULTIPART.get()).build(null));
            event.register(Registries.BLOCK_ENTITY_TYPE, BuildingPalette.id("multipart"), () -> BuildingPalette.ENTITY.get());
        }
    }
}
