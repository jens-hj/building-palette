package dev.jensh.buildingpalette.platform;

import java.util.ServiceLoader;

public final class ClientPlatform {
    public static final ClientServices INSTANCE = ServiceLoader.load(ClientServices.class).findFirst()
        .orElseThrow(() -> new IllegalStateException("Missing Building Palette client adapter"));
    private ClientPlatform() {}
}
