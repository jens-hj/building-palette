package dev.jensh.buildingpalette;

import java.util.*;
import net.minecraft.server.level.ServerLevel;

/** Drain after level ticking: chunk tick containers are not ready during load. */
public final class LoadedParts {
    private static final Map<ServerLevel, Set<MultipartEntity>> PENDING = new IdentityHashMap<>();
    public static void loaded(MultipartEntity entity) {
        if (entity.getLevel() instanceof ServerLevel level) PENDING.computeIfAbsent(level,
            ignored -> Collections.newSetFromMap(new IdentityHashMap<>())).add(entity);
    }
    public static void unloaded(MultipartEntity entity) {
        if (entity.getLevel() instanceof ServerLevel level) {
            var entries = PENDING.get(level);
            if (entries != null) { entries.remove(entity); if (entries.isEmpty()) PENDING.remove(level); }
        }
    }
    public static void endTick(ServerLevel level) {
        var loaded = PENDING.remove(level);
        if (loaded != null) for (var entity : loaded) if (!entity.isRemoved()) entity.refreshPropertiesOnLoad();
    }
    public static void clear() { PENDING.clear(); }
    private LoadedParts() {}
}
