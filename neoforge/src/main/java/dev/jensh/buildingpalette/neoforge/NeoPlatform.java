package dev.jensh.buildingpalette.neoforge;

import dev.jensh.buildingpalette.MultipartEntity;
import dev.jensh.buildingpalette.platform.PlatformServices;
import dev.tazer.clutternomore.shape.ShapeMap;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.minecraft.world.item.*;
import net.minecraft.server.level.ServerPlayer;
import java.nio.file.Path;
import java.util.*;

public final class NeoPlatform implements PlatformServices {
    @Override public Item parent(Item item) { return ShapeMap.getParent(item); }
    @Override public List<Item> shapes(Item parent) { return new ArrayList<>(ShapeMap.getShapes(parent)); }
    @Override public boolean isModLoaded(String id) { return ModList.get().isLoaded(id); }
    @Override public Path configDirectory() { return FMLPaths.CONFIGDIR.get(); }
    // Traveler's Backpack compat is not part of the 1.21.1 build.
    @Override public ItemStack pickBackpack(ServerPlayer player, String material) { return ItemStack.EMPTY; }
    @Override public int takeBackpack(ServerPlayer player, ItemStack like, int units) { return 0; }
    @Override public void renderDataChanged(MultipartEntity entity) { entity.requestModelDataUpdate(); }
}
