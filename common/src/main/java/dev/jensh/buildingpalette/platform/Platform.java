package dev.jensh.buildingpalette.platform;

import java.util.ServiceLoader;

public final class Platform {
    public static final PlatformServices INSTANCE = ServiceLoader.load(PlatformServices.class).findFirst()
        .orElseThrow(() -> new IllegalStateException("Missing Building Palette platform adapter"));
    private Platform() {}
}
