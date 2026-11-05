package dev.jensh.buildingpalette.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PaletteConfigScreen extends Screen {
    private final Screen parent;
    public PaletteConfigScreen(Screen parent) { super(Component.translatable("config.building_palette.title")); this.parent = parent; }
    private static Component label(String key, boolean value) {
        return Component.translatable(key, Component.translatable(value ? "options.on" : "options.off"));
    }
    @Override protected void init() {
        int x = width / 2 - 130, y = height / 2 - 30;
        addRenderableWidget(Button.builder(label("config.building_palette.preview", PaletteConfig.placementPreview), button -> {
            PaletteConfig.placementPreview = !PaletteConfig.placementPreview;
            PaletteConfig.save();
            button.setMessage(label("config.building_palette.preview", PaletteConfig.placementPreview));
        }).bounds(x, y, 260, 20).tooltip(Tooltip.create(Component.translatable("config.building_palette.preview.help"))).build());
        addRenderableWidget(Button.builder(label("config.building_palette.precise", PaletteConfig.precisePlacement), button -> {
            PaletteConfig.precisePlacement = !PaletteConfig.precisePlacement;
            PaletteConfig.save(); PaletteConfig.sync();
            button.setMessage(label("config.building_palette.precise", PaletteConfig.precisePlacement));
        }).bounds(x, y + 26, 260, 20).tooltip(Tooltip.create(Component.translatable("config.building_palette.precise.help"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose()).bounds(width / 2 - 100, y + 64, 200, 20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(font, title, (width - font.width(title)) / 2, height / 2 - 65, -1, true);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
