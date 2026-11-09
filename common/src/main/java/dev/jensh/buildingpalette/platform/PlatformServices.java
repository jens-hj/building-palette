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
    void renderDataChanged(MultipartEntity entity);
}
