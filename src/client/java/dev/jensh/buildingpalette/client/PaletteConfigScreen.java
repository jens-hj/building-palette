package dev.jensh.buildingpalette.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
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
        int x = width / 2 - 130, y = height / 2 - 57;
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
        var hue = new HueSlider(x, y + 78);
        hue.active = PaletteConfig.highlightParts;
        addRenderableWidget(Button.builder(label("config.building_palette.highlight", PaletteConfig.highlightParts), button -> {
            PaletteConfig.highlightParts = !PaletteConfig.highlightParts;
            PaletteConfig.save(); hue.active = PaletteConfig.highlightParts;
            button.setMessage(label("config.building_palette.highlight", PaletteConfig.highlightParts));
        }).bounds(x, y + 52, 260, 20).tooltip(Tooltip.create(Component.translatable("config.building_palette.highlight.help"))).build());
        addRenderableWidget(hue);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose()).bounds(width / 2 - 100, y + 110, 200, 20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(font, title, (width - font.width(title)) / 2, height / 2 - 83, -1, true);
        int x = width / 2 + 110, y = height / 2 + 23;
        graphics.fill(x, y, x + 20, y + 16, 0xffb0b0b0);
        graphics.outline(x + 2, y + 2, 16, 12, PaletteConfig.highlightParts ? PaletteConfig.outlineColor() : 0xff000000);
    }
    private static final class HueSlider extends AbstractSliderButton {
        HueSlider(int x, int y) { super(x, y, 232, 20, Component.empty(), PaletteConfig.outlineHue / 360.0); updateMessage(); }
        @Override protected void updateMessage() { setMessage(Component.translatable("config.building_palette.hue", Math.round(value * 360))); }
        @Override protected void applyValue() { PaletteConfig.outlineHue = (int)Math.round(value * 360); PaletteConfig.save(); }
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
