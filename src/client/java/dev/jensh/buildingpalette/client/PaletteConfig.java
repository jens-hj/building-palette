package dev.jensh.buildingpalette.client;

import com.google.gson.GsonBuilder;
import dev.jensh.buildingpalette.PreferencesPayload;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.slf4j.LoggerFactory;

public final class PaletteConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("building_palette.json");
    public static boolean placementPreview;
    public static boolean precisePlacement;
    public static boolean highlightParts;
    public static int outlineHue = 48;
    private record Settings(boolean placementPreview, boolean precisePlacement, boolean highlightParts, Integer outlineHue) {}
    private PaletteConfig() {}
    public static void load() {
        if (!Files.exists(FILE)) { save(); return; }
        try (var reader = Files.newBufferedReader(FILE)) {
            var config = new GsonBuilder().create().fromJson(reader, Settings.class);
            if (config != null) {
                placementPreview = config.placementPreview;
                precisePlacement = config.precisePlacement;
                highlightParts = config.highlightParts;
                outlineHue = config.outlineHue == null ? 48 : net.minecraft.util.Mth.clamp(config.outlineHue, 0, 360);
            }
        } catch (Exception error) { LoggerFactory.getLogger("Building Palette").warn("Could not load placement settings; using defaults", error); }
    }
    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, new GsonBuilder().setPrettyPrinting().create().toJson(new Settings(placementPreview, precisePlacement, highlightParts, outlineHue)) + "\n");
        } catch (Exception error) { LoggerFactory.getLogger("Building Palette").warn("Could not save placement settings", error); }
    }
    public static void sync() {
        if (ClientPlayNetworking.canSend(PreferencesPayload.TYPE)) ClientPlayNetworking.send(new PreferencesPayload(precisePlacement));
    }
    public static int outlineColor() {
        return 0xff000000 | net.minecraft.util.Mth.hsvToRgb((outlineHue % 360) / 360f, .55f, 1f);
    }
}
