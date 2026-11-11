package dev.jensh.buildingpalette.platform;

import dev.jensh.buildingpalette.MultipartEntity;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.world.item.*;
import net.minecraft.server.level.ServerPlayer;

public interface PlatformServices {
    Item parent(Item item);
    List<Item> shapes(Item parent);
    boolean isModLoaded(String id);
    Path configDirectory();
    ItemStack pickBackpack(ServerPlayer player, String material);
    /** Remove up to {@code units} of {@code like}'s material from the equipped backpack; returns units removed. */
    int takeBackpack(ServerPlayer player, ItemStack like, int units);
    void renderDataChanged(MultipartEntity entity);
}
