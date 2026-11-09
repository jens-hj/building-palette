package dev.jensh.buildingpalette.fabric;

import dev.jensh.buildingpalette.MultipartEntity;
import dev.jensh.buildingpalette.platform.PlatformServices;
import dev.tazer.clutternomore.shape.ShapeMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.*;
import net.minecraft.server.level.ServerPlayer;
import java.nio.file.Path;
import java.util.*;

public final class FabricPlatform implements PlatformServices {
    @Override public Item parent(Item item) { return ShapeMap.getParent(item); }
    @Override public List<Item> shapes(Item parent) { return new ArrayList<>(ShapeMap.getShapes(parent)); }
    @Override public boolean isModLoaded(String id) { return FabricLoader.getInstance().isModLoaded(id); }
    @Override public Path configDirectory() { return FabricLoader.getInstance().getConfigDir(); }
    @Override public ItemStack pickBackpack(ServerPlayer player, String material) { return dev.jensh.buildingpalette.compat.BackpackPick.pick(player, material); }
    @Override public void renderDataChanged(MultipartEntity entity) {}
}
