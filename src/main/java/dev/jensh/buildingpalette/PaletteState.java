package dev.jensh.buildingpalette;

/** Bootstrap guard: inventory mixins also run while vanilla registries are being constructed. */
public final class PaletteState {
    public static boolean ready;
    private PaletteState() {}
}
