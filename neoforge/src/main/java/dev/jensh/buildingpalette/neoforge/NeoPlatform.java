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
    @Override public ItemStack pickBackpack(ServerPlayer player, String material) { return dev.jensh.buildingpalette.compat.BackpackPick.pick(player, material); }
    @Override public void renderDataChanged(MultipartEntity entity) { entity.requestModelDataUpdate(); }
}
