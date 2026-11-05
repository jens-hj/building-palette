package dev.jensh.buildingpalette.client;

import dev.jensh.buildingpalette.*;
import dev.tazer.clutternomore.client.ClutterNoMoreClient;
import java.util.Locale;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class PaletteClient implements ClientModInitializer {
    private static double scroll;
    @Override public void onInitializeClient() {
        BlockEntityRendererRegistry.register(BuildingPalette.ENTITY, MultipartRenderer::new);
        HudElementRegistry.addLast(BuildingPalette.id("palette"), (graphics, delta) -> draw(graphics));
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            var selection = Materials.get(stack);
            if (selection == null) return;
            int units = MaterialStacks.units(stack), count = MaterialStacks.available(stack), remainder = units % selection.shape().cost;
            lines.add(Component.translatable("tooltip.building_palette.balance", count, units / 8, units % 8).withColor(0xa9b7c5));
            if (remainder > 0) lines.add(Component.translatable("tooltip.building_palette.remainder", remainder).withColor(0xf5c977));
            lines.add(Component.translatable("tooltip.building_palette.scroll").withColor(0x859987));
        });
    }
    public static Slot target() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        if (mc.gui.screen() instanceof AbstractContainerScreen<?>) return ClutterNoMoreClient.getHoveredSlot();
        if (mc.gui.screen() != null) return null;
        if (Materials.get(mc.player.getMainHandItem()) == null && Materials.get(mc.player.getOffhandItem()) != null)
            return mc.player.inventoryMenu.getSlot(45);
        return mc.player.inventoryMenu.getSlot(36 + mc.player.getInventory().getSelectedSlot());
    }
    public static boolean managed() { Slot slot = target(); return slot != null && Materials.get(slot.getItem()) != null; }
    public static boolean active() { return managed() && ClutterNoMoreClient.isShapeKeyPhysicallyDown(); }
    public static boolean onScroll(double amount) {
        if (!active()) { scroll = 0; return false; }
        Slot slot = target();
        if (slot == null || !slot.mayPlace(slot.getItem()) || !slot.mayPickup(Minecraft.getInstance().player)) return true;
        scroll += amount;
        int steps = (int)scroll;
        scroll -= steps;
        // Cap high-resolution mouse input per event; no arbitrary target item is sent to the server.
        steps = Math.clamp(steps, -8, 8);
        Minecraft mc = Minecraft.getInstance();
        int direction = steps > 0 ? -1 : 1;
        for (int i = 0; i < Math.abs(steps); i++) {
            ItemStack converted = ScrollPayload.convert(slot.getItem(), direction);
            if (converted.isEmpty() || !slot.mayPlace(converted)) break;
            ClientPlayNetworking.send(new ScrollPayload(mc.player.containerMenu.containerId, slot.index, direction));
            slot.set(converted);
        }
        return true;
    }
    public static void draw(GuiGraphicsExtractor graphics) {
        if (!active()) return;
        Minecraft mc = Minecraft.getInstance();
        ItemStack source = target().getItem();
        var palette = Materials.palette(source);
        int left = (mc.getWindow().getGuiScaledWidth() - palette.size() * 22) / 2;
        int top = mc.getWindow().getGuiScaledHeight() - 84;
        graphics.fill(left - 5, top - 22, left + palette.size() * 22 + 3, top + 25, 0xe0202428);
        var selected = Materials.get(source);
        for (int i = 0; i < palette.size(); i++) {
            ItemStack icon = palette.get(i);
            int x = left + i * 22;
            if (Materials.get(icon).shape() == selected.shape()) graphics.fill(x - 2, top - 2, x + 19, top + 20, 0xff718e79);
            graphics.item(icon, x, top);
        }
        Component name = Component.translatable("shape.building_palette." + selected.shape().name().toLowerCase(Locale.ROOT));
        graphics.text(mc.font, name, left, top - 16, -1, false);
    }
}
