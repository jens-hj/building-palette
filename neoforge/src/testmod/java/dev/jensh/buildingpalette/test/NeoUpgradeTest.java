package dev.jensh.buildingpalette.test;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class NeoUpgradeTest extends WorldUpgradeScenario {
    @Override protected net.minecraft.server.level.ServerPlayer fakePlayer(net.minecraft.server.level.ServerLevel level,com.mojang.authlib.GameProfile profile) { return net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,profile); }
    public static void install() {
        var test=new NeoUpgradeTest();
        test.initialize(net.neoforged.fml.ModList.get().getModContainerById("building_palette").orElseThrow().getModInfo().getVersion().toString());
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> test.started(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> test.tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> test.stopped(event.getServer()));
    }
}
