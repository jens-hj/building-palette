package dev.jensh.buildingpalette.test;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

public final class TestPlayers {
    private TestPlayers() {}
    /** A survival server player that is not added to the level. */
    public static ServerPlayer survival(GameTestHelper test) {
        return FakePlayer.get(test.getLevel(), new GameProfile(UUID.randomUUID(), "palette-test"));
    }
}
