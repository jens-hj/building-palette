package dev.jensh.buildingpalette.client;

import dev.jensh.buildingpalette.*;
import dev.tazer.clutternomore.client.ClutterNoMoreClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class PaletteClient implements ClientModInitializer {
    private static final Identifier HOTBAR = Identifier.withDefaultNamespace("hud/hotbar");
    private static final Identifier SELECTION = Identifier.withDefaultNamespace("hud/hotbar_selection");
    private static double scroll;
    @Override public void onInitializeClient() {
        BlockEntityRendererRegistry.register(BuildingPalette.ENTITY, MultipartRenderer::new);
        HudElementRegistry.addLast(BuildingPalette.id("palette"), (graphics, delta) -> {
            if (Minecraft.getInstance().gui.screen() == null) draw(graphics);
        });
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
        int left = graphics.guiWidth() / 2 - 91 + mc.player.getInventory().getSelectedSlot() * 20;
        if (target().index == 45) left = graphics.guiWidth() / 2 + (mc.player.getMainArm() == HumanoidArm.RIGHT ? -120 : 98);
        draw(graphics, left, graphics.guiHeight() - 22);
    }
    public static void draw(GuiGraphicsExtractor graphics, int left, int top) {
        if (!active()) return;
        ItemStack source = target().getItem();
        var palette = Materials.palette(source);
        var selected = Materials.get(source);
        int selectedIndex = 0;
        for (int i = 0; i < palette.size(); i++) {
            if (Materials.get(palette.get(i)).shape() == selected.shape()) selectedIndex = i;
        }
        // Scroll up pulls the nearest preview into the actual slot. Wrap the previews
        // with the same ordering as ScrollPayload, so there is no end to the strip.
        int visible = Math.min(5, Math.min(palette.size() - 1, top / 20));
        // Turn one continuous vanilla hotbar upright so its internal dividers
        // remain thin. Repeating isolated slots also repeats their black end caps.
        if (visible > 0) {
            var pose = graphics.pose();
            pose.pushMatrix();
            pose.translate(left + 22, top - visible * 20);
            pose.rotate((float)(Math.PI / 2));
            int length = visible * 20 + 1;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR, 182, 22, 0, 0, 0, 0, length, 22);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR, 182, 22, 181, 0, length, 0, 1, 22);
            pose.popMatrix();
        }
        for (int distance = visible; distance >= 1; distance--) {
            int y = top - distance * 20;
            ItemStack icon = palette.get(Math.floorMod(selectedIndex - distance, palette.size()));
            graphics.item(icon, left + 3, y + 3);
        }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SELECTION, left - 1, top - 1, 24, 23);
    }
}
