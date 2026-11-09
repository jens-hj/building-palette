package dev.jensh.buildingpalette.test;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.loader.api.FabricLoader;

public final class WorldUpgradeTest extends WorldUpgradeScenario implements ModInitializer {
    @Override protected net.minecraft.server.level.ServerPlayer fakePlayer(net.minecraft.server.level.ServerLevel level, com.mojang.authlib.GameProfile profile) { return FakePlayer.get(level,profile); }
    @Override public void onInitialize() {
        initialize(FabricLoader.getInstance().getModContainer("building_palette").orElseThrow().getMetadata().getVersion().getFriendlyString());
        ServerLifecycleEvents.SERVER_STARTED.register(this::started);
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(this::stopped);
    }
}
