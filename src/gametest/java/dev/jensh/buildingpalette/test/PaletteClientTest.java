package dev.jensh.buildingpalette.test;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.core.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;

public final class PaletteClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("fill -8 99 -8 8 99 8 minecraft:smooth_stone");
            world.getServer().runCommand("tp @a 0.5 100 4.5 180 12");
            world.getServer().computeOnServer(server -> {
                var level = server.overworld();
                for (int x = -2; x <= 2; x++) {
                    var pos = new BlockPos(x, 100, 0);
                    level.setBlock(pos, BuildingPalette.MULTIPART.defaultBlockState(), 3);
                    Cells cells = switch (x) {
                        case -2 -> new Cells().place(Cells.mask(Shape.SLAB, 0, 0), "minecraft:oak_planks");
                        case -1 -> new Cells().place(Cells.mask(Shape.STAIR, 0, 0), "minecraft:stone_bricks");
                        case 0 -> new Cells().place(Cells.mask(Shape.STEP, 0, 0), "minecraft:oak_planks").place(Cells.mask(Shape.STEP, 3, 0), "minecraft:stone_bricks");
                        case 1 -> new Cells().place(1, "minecraft:oak_planks").place(128, "minecraft:stone_bricks");
                        default -> new Cells().place(Cells.mask(Shape.VERTICAL_STEP, 0, 0), "minecraft:stone_bricks");
                    };
                    ((MultipartEntity)level.getBlockEntity(pos)).setCells(cells);
                }
                var player = server.getPlayerList().getPlayers().getFirst();
                player.getInventory().setItem(0, Materials.piece("minecraft:oak_planks", Shape.CORNER, 512));
                player.getInventory().setItem(1, new ItemStack(Items.OAK_SLAB, 128));
                player.getInventory().setItem(2, MaterialStacks.withUnits(new ItemStack(Items.STONE_BRICK_STAIRS), 512));
                player.inventoryMenu.broadcastChanges();
                return true;
            });
            world.getConnection().waitForChunksRender();
            context.waitTicks(25);
            context.runOnClient(mc -> {
                var entity = (MultipartEntity)mc.level.getBlockEntity(new BlockPos(0, 100, 0));
                if (entity == null || entity.cells().occupied() == 0) throw new AssertionError("Multipart cells did not synchronize");
                var renderer = mc.getBlockEntityRenderDispatcher().<MultipartEntity, dev.jensh.buildingpalette.client.MultipartRenderer.State>getRenderer(entity);
                var state = renderer.createRenderState();
                renderer.extractRenderState(entity, state, 0, mc.player.position(), null);
                if (state.materialCount() == 0) throw new AssertionError("Multipart world models are empty");
                mc.gui.toastManager().clear();
                if (MaterialStacks.units(mc.player.getInventory().getItem(0)) != 512) throw new AssertionError("Corner balance did not synchronize");
                if (mc.player.getInventory().getItem(1).getCount() != 128) throw new AssertionError("Slab count did not synchronize");
            });
            context.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("building_palette_shapes").disableCounterPrefix().withDestinationDir(java.nio.file.Path.of("../../previews")));
            context.getInput().holdAlt();
            context.waitTicks(2);
            context.runOnClient(mc -> {
                if (!dev.jensh.buildingpalette.client.PaletteClient.active()) throw new AssertionError("Alt does not activate the held palette");
            });
            context.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("building_palette_palette").disableCounterPrefix().withDestinationDir(java.nio.file.Path.of("../../previews")));
            context.getInput().scroll(-1);
            context.waitTicks(5);
            boolean switched = world.getServer().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                var selection = Materials.get(player.getMainHandItem());
                return selection != null && selection.shape() == Shape.FENCE && MaterialStacks.units(player.getMainHandItem()) == 512;
            });
            if (!switched) throw new AssertionError("Held Alt-scroll did not preserve material on the server");
            context.getInput().releaseAlt();
            world.getServer().computeOnServer(server -> {
                int checked = 0, modded = 0;
                for (var parent : dev.tazer.clutternomore.shape.ShapeMap.getShapesByParent().keySet()) {
                    ItemStack stack = new ItemStack(parent, 64);
                    if (Materials.get(stack) == null) continue;
                    var seen = java.util.EnumSet.noneOf(Shape.class);
                    for (var icon : Materials.palette(stack)) {
                        if (!seen.add(Materials.get(icon).shape())) throw new AssertionError("Duplicate shape in family " + parent);
                        ItemStack converted = MaterialStacks.withUnits(icon, 512);
                        if (MaterialStacks.units(converted) != 512) throw new AssertionError("Family balance: " + parent);
                    }
                    checked++;
                    if (!net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(parent).getNamespace().equals("minecraft")) modded++;
                }
                System.out.println("[Building Palette] Validated " + checked + " material families, including " + modded + " modded families");
                return checked;
            });
            context.runOnClient(mc -> mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
            context.waitTicks(5);
            context.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("building_palette_inventory").disableCounterPrefix().withDestinationDir(java.nio.file.Path.of("../../previews")));
            double[] cursor = context.computeOnClient(mc -> {
                var screen = (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)mc.gui.screen();
                try {
                var type = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class;
                var left = type.getDeclaredField("leftPos"); left.setAccessible(true);
                var top = type.getDeclaredField("topPos"); top.setAccessible(true);
                var slot = screen.getMenu().getSlot(37);
                double scale = mc.getWindow().getGuiScale();
                return new double[]{(left.getInt(screen) + slot.x + 8) * scale, (top.getInt(screen) + slot.y + 8) * scale};
                } catch (ReflectiveOperationException error) { throw new AssertionError("Could not locate inventory slot", error); }
            });
            context.getInput().setCursorPos(cursor[0], cursor[1]);
            context.getInput().holdAlt();
            context.waitTicks(2);
            context.getInput().scroll(-1);
            context.waitTicks(5);
            boolean hovered = world.getServer().computeOnServer(server -> {
                var stack = server.getPlayerList().getPlayers().getFirst().getInventory().getItem(1);
                return Materials.get(stack).shape() == Shape.VERTICAL_SLAB && MaterialStacks.units(stack) == 512;
            });
            if (!hovered) throw new AssertionError("Inventory hover scroll did not reach the server");
            context.getInput().releaseAlt();
            context.runOnClient(mc -> mc.gui.setScreen(null));
            world.getServer().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.OAK_PLANKS, 64));
                player.inventoryMenu.broadcastChanges();
                return true;
            });
            context.waitTicks(5);
            context.getInput().holdAlt();
            context.getInput().scroll(-1);
            context.waitTicks(5);
            boolean offhand = world.getServer().computeOnServer(server -> {
                var stack = server.getPlayerList().getPlayers().getFirst().getOffhandItem();
                return stack.is(Items.OAK_SLAB) && stack.getCount() == 128 && MaterialStacks.units(stack) == 512;
            });
            if (!offhand) throw new AssertionError("Offhand Alt-scroll did not preserve material");
            context.getInput().releaseAlt();
        }
    }
}
