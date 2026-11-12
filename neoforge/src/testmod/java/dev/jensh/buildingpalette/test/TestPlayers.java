package dev.jensh.buildingpalette.test;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

public final class TestPlayers {
    private TestPlayers() {}
    /** A survival server player that is not added to the level. */
    public static ServerPlayer survival(GameTestHelper test) {
        return FakePlayerFactory.get(test.getLevel(), new GameProfile(UUID.randomUUID(), "palette-test"));
    }
}
