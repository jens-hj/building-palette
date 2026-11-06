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
                if (Boolean.getBoolean("building_palette.shader_test")) {
                    try {
                        var type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                        var api = type.getMethod("getInstance").invoke(null);
                        if (!(Boolean)type.getMethod("isShaderPackInUse").invoke(api)) throw new AssertionError("Shader test did not activate a shader pack");
                    } catch (ReflectiveOperationException error) { throw new AssertionError("Cannot verify Iris shader pipeline", error); }
                }
                var identities = new java.util.HashMap<Object, Shape>();
                for (ItemStack icon : Materials.palette(new ItemStack(Items.OAK_PLANKS))) {
                    var shape = Materials.get(icon).shape();
                    var itemState = new net.minecraft.client.renderer.item.TrackingItemStackRenderState();
                    mc.getItemModelResolver().updateForTopItem(itemState, icon, ItemDisplayContext.GUI, mc.level, mc.player, 0);
                    Shape previous = identities.put(itemState.getModelIdentity(), shape);
                    if (previous != null) throw new AssertionError("GUI model cache collision between " + previous + " and " + shape);
                    if (shape.isMultipart() && shape.cost < 8) {
                        var accessor = (dev.jensh.buildingpalette.mixin.client.ItemStateAccessor)itemState;
                        var min = new org.joml.Vector3f(Float.POSITIVE_INFINITY);
                        var max = new org.joml.Vector3f(Float.NEGATIVE_INFINITY);
                        for (int layer = 0; layer < accessor.palette$getCount(); layer++)
                            for (var quad : ((dev.jensh.buildingpalette.mixin.client.ItemLayerAccessor)accessor.palette$getLayers()[layer]).palette$getQuads().all())
                                for (int vertex = 0; vertex < 4; vertex++) { min.min(quad.position(vertex)); max.max(quad.position(vertex)); }
                        if (new org.joml.Vector3f(min).add(max).mul(.5f).distance(new org.joml.Vector3f(.5f)) > .0001)
                            throw new AssertionError("Item geometry is not centered: " + shape);
                    }
                }
                var entity = (MultipartEntity)mc.level.getBlockEntity(new BlockPos(0, 100, 0));
                if (entity == null || entity.cells().occupied() == 0) throw new AssertionError("Multipart cells did not synchronize");
                var mesh = net.fabricmc.fabric.api.client.renderer.v1.Renderer.get().mutableMesh();
                var model = mc.getModelManager().getBlockStateModelSet().get(entity.getBlockState());
                if (entity.getBlockState().getRenderShape() != net.minecraft.world.level.block.RenderShape.MODEL) throw new AssertionError("Multipart does not use terrain rendering");
                ((net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel)model).emitQuads(mesh.emitter(), mc.level,
                    entity.getBlockPos(), entity.getBlockState(), net.minecraft.util.RandomSource.create(1), direction -> false);
                if (mesh.size() == 0) throw new AssertionError("Multipart terrain model is empty");
                mesh.forEach(quad -> {
                    if (quad.ambientOcclusion() != net.fabricmc.fabric.api.util.TriState.TRUE) throw new AssertionError("Missing terrain ambient occlusion");
                });
                OutlineStyleTest.verify(mc);
                mc.gui.toastManager().clear();
                if (MaterialStacks.units(mc.player.getInventory().getItem(0)) != 512) throw new AssertionError("Corner balance did not synchronize");
                if (mc.player.getInventory().getItem(1).getCount() != 128) throw new AssertionError("Slab count did not synchronize");
                // Client prediction must not emit particles for the invisible carrier.
                // The server sends a separate break event with the removed material.
                mc.particleEngine.clearParticles();
                mc.level.addDestroyBlockEffect(new BlockPos(0, 100, 0), BuildingPalette.MULTIPART.defaultBlockState());
                mc.particleEngine.tick();
                if (!mc.particleEngine.countParticles().matches(".*T\\s*0")) throw new AssertionError("Invisible multipart carrier emitted break particles: " + mc.particleEngine.countParticles());
                for (var material : new String[]{"minecraft:oak_planks", "minecraft:stone_bricks"}) {
                    mc.level.addDestroyBlockEffect(new BlockPos(0, 100, 0), MultipartBlock.material(material));
                    mc.particleEngine.tick();
                    if (mc.particleEngine.countParticles().matches(".*T\\s*0")) throw new AssertionError("Missing material break particles: " + material);
                    mc.particleEngine.clearParticles();
                }
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
            hoverSlot(context, 37);
            context.getInput().holdAlt();
            context.waitTicks(2);
            context.runOnClient(mc -> {
                if (!dev.jensh.buildingpalette.client.PaletteClient.active()) throw new AssertionError("Inventory hover palette is inactive");
            });
            context.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("building_palette_inventory_scroll").disableCounterPrefix().withDestinationDir(java.nio.file.Path.of("../../previews")));
            context.runOnClient(mc -> {
                var expected = ScrollPayload.convert(mc.player.inventoryMenu.getSlot(37).getItem(), 1);
                var screen = mc.gui.screen();
                boolean allowed = dev.tazer.clutternomore.client.ClutterNoMoreClient.allowScreenScroll(screen, 0, 0, 0, -1);
                if (allowed || Materials.get(mc.player.inventoryMenu.getSlot(37).getItem()).shape() != Materials.get(expected).shape())
                    throw new AssertionError("Inventory scroll did not use Building Palette's order");
                var seen = java.util.EnumSet.noneOf(Shape.class);
                int shapes = Materials.palette(expected).size();
                for (int i = 0; i < shapes; i++) {
                    if (!dev.jensh.buildingpalette.client.PaletteClient.onScroll(-1)) throw new AssertionError("Palette scroll was not handled");
                    var current = mc.player.inventoryMenu.getSlot(37).getItem();
                    seen.add(Materials.get(current).shape());
                    if (MaterialStacks.units(current) != 512) throw new AssertionError("Palette cycle changed material");
                }
                if (seen.size() != shapes || !seen.contains(Shape.CORNER)) throw new AssertionError("Inventory palette skipped shapes or corners");
            });
            context.waitTicks(5);
            context.getInput().scroll(1);
            context.waitTicks(5);
            context.getInput().scroll(-1);
            context.waitTicks(5);
            boolean hovered = world.getServer().computeOnServer(server -> {
                var stack = server.getPlayerList().getPlayers().getFirst().getInventory().getItem(1);
                return Materials.get(stack).shape() == Shape.VERTICAL_SLAB && MaterialStacks.units(stack) == 512;
            });
            if (!hovered) throw new AssertionError("Inventory hover scroll did not reach the server");
            context.getInput().releaseAlt();
            context.runOnClient(mc -> mc.gui.setScreen(null));
            context.runOnClient(mc -> {
                if (dev.jensh.buildingpalette.client.PaletteConfig.placementPreview || dev.jensh.buildingpalette.client.PaletteConfig.precisePlacement
                        || dev.jensh.buildingpalette.client.PaletteConfig.highlightParts)
                    throw new AssertionError("Placement settings must default off");
                var screen = new dev.jensh.buildingpalette.client.PaletteConfigScreen(null);
                mc.gui.setScreen(screen);
                ((net.minecraft.client.gui.components.Button)screen.children().get(0)).onPress(null);
                if (!dev.jensh.buildingpalette.client.PaletteConfig.placementPreview) throw new AssertionError("Preview toggle did not enable");
                ((net.minecraft.client.gui.components.Button)screen.children().get(0)).onPress(null);
                var highlight = (net.minecraft.client.gui.components.Button)screen.children().get(2);
                var hue = (net.minecraft.client.gui.components.AbstractSliderButton)screen.children().get(3);
                if (hue.active) throw new AssertionError("Hue slider should be disabled for vanilla styling");
                highlight.onPress(null);
                if (!hue.active || !dev.jensh.buildingpalette.client.PaletteConfig.highlightParts) throw new AssertionError("Highlight toggle did not enable hue");
                hue.onClick(new net.minecraft.client.input.MouseButtonEvent(hue.getX() + 4 + .75 * (hue.getWidth() - 8), hue.getY() + 10,
                    new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
                if (dev.jensh.buildingpalette.client.PaletteConfig.outlineHue != 270) throw new AssertionError("Hue slider did not select the requested colour");
                dev.jensh.buildingpalette.client.PaletteConfig.highlightParts = false;
                dev.jensh.buildingpalette.client.PaletteConfig.outlineHue = 48;
                dev.jensh.buildingpalette.client.PaletteConfig.load();
                if (!dev.jensh.buildingpalette.client.PaletteConfig.highlightParts || dev.jensh.buildingpalette.client.PaletteConfig.outlineHue != 270)
                    throw new AssertionError("Outline preference did not persist");
                highlight.onPress(null);
                if (hue.active) throw new AssertionError("Hue slider should disable after restoring vanilla styling");
                ((net.minecraft.client.gui.components.Button)screen.children().get(1)).onPress(null);
            });
            context.waitTicks(5);
            if (!world.getServer().computeOnServer(server -> PlacementPreferences.precise(server.getPlayerList().getPlayers().getFirst())))
                throw new AssertionError("Placement preference was not synchronized to server");
            context.runOnClient(mc -> {
                ((net.minecraft.client.gui.components.Button)mc.gui.screen().children().get(1)).onPress(null);
            });
            context.waitTicks(5);
            context.getInput().setCursorPos(0, 0);
            context.waitTicks(2);
            context.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("building_palette_settings").disableCounterPrefix().withDestinationDir(java.nio.file.Path.of("../../previews")));
            context.runOnClient(mc -> mc.gui.setScreen(null));
            world.getServer().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.getInventory().setItem(9, MaterialStacks.withUnits(new ItemStack(Items.STONE), 9));
                player.getInventory().setItem(10, ItemStack.EMPTY);
                player.inventoryMenu.broadcastChanges();
                return true;
            });
            context.waitTicks(5);
            context.runOnClient(mc -> {
                var fractional = mc.player.inventoryMenu.getSlot(9).getItem();
                if (!ItemStack.isSameItemSameComponents(fractional, fractional.copy())) throw new AssertionError("Fractional identity is not reflexive");
                int from = 9, to = 10;
                for (int i = 0; i < 20; i++) {
                    mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, from, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, mc.player);
                    if (MaterialStacks.units(mc.player.containerMenu.getCarried()) != 9) throw new AssertionError("Fractional cursor pickup prediction");
                    mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, to, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, mc.player);
                    if (!mc.player.containerMenu.getCarried().isEmpty()) throw new AssertionError("Ghost cursor after fractional placement");
                    int swap = from; from = to; to = swap;
                }
            });
            context.waitTicks(10);
            if (!world.getServer().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                return player.containerMenu.getCarried().isEmpty() && MaterialStacks.units(player.inventoryMenu.getSlot(9).getItem()) == 9
                    && player.inventoryMenu.getSlot(10).getItem().isEmpty();
            })) throw new AssertionError("Server disagrees with fractional cursor moves");
            context.runOnClient(mc -> {
                if (!mc.player.containerMenu.getCarried().isEmpty() || MaterialStacks.units(mc.player.inventoryMenu.getSlot(9).getItem()) != 9
                        || !mc.player.inventoryMenu.getSlot(10).getItem().isEmpty()) throw new AssertionError("Fractional cursor correction mismatch");
            });
            context.runOnClient(mc -> mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
            context.waitTicks(3);
            hoverSlot(context, 9);
            context.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
            context.waitTicks(2);
            context.runOnClient(mc -> {
                if (MaterialStacks.units(mc.player.containerMenu.getCarried()) != 9) throw new AssertionError("Mouse pickup lost fractional cursor");
            });
            hoverSlot(context, 10);
            context.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
            context.waitTicks(3);
            context.runOnClient(mc -> {
                if (!mc.player.containerMenu.getCarried().isEmpty() || MaterialStacks.units(mc.player.inventoryMenu.getSlot(10).getItem()) != 9)
                    throw new AssertionError("Mouse placement left a ghost cursor");
            });
            context.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT);
            context.waitTicks(2);
            context.runOnClient(mc -> {
                if (MaterialStacks.units(mc.player.containerMenu.getCarried()) != 8 || MaterialStacks.units(mc.player.inventoryMenu.getSlot(10).getItem()) != 1)
                    throw new AssertionError("Right mouse button did not split whole pieces from reserved material");
            });
            context.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
            context.waitTicks(2);
            context.runOnClient(mc -> {
                if (!mc.player.containerMenu.getCarried().isEmpty() || MaterialStacks.units(mc.player.inventoryMenu.getSlot(10).getItem()) != 9)
                    throw new AssertionError("Left mouse button did not merge reserved material back");
                mc.gui.setScreen(null);
            });
            world.getServer().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.getInventory().setItem(11, Materials.piece("minecraft:oak_planks", Shape.SLAB, 448));
                player.getInventory().setItem(12, Materials.piece("minecraft:oak_planks", Shape.CORNER, 65));
                player.inventoryMenu.broadcastChanges();
                return true;
            });
            context.waitTicks(5);
            context.runOnClient(mc -> {
                var menu = mc.player.containerMenu;
                mc.gameMode.handleContainerInput(menu.containerId, 12, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, mc.player);
                mc.gameMode.handleContainerInput(menu.containerId, 11, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, mc.player);
                if (MaterialStacks.units(menu.getSlot(11).getItem()) != 512 || Materials.get(menu.getSlot(11).getItem()).shape() != Shape.SLAB
                        || MaterialStacks.units(menu.getCarried()) != 1) throw new AssertionError("Cross-shape merge prediction or overflow");
                mc.gameMode.handleContainerInput(menu.containerId, 12, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, mc.player);
                if (!menu.getCarried().isEmpty()) throw new AssertionError("Overflow placement left a ghost cursor");
            });
            context.waitTicks(10);
            if (!world.getServer().computeOnServer(server -> {
                var menu = server.getPlayerList().getPlayers().getFirst().containerMenu;
                return menu.getCarried().isEmpty() && MaterialStacks.units(menu.getSlot(11).getItem()) == 512
                    && MaterialStacks.units(menu.getSlot(12).getItem()) == 1;
            })) throw new AssertionError("Server disagrees with cross-shape merge");
            world.getServer().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.getInventory().setItem(3, ItemStack.EMPTY);
                player.getInventory().setItem(4, Materials.piece("minecraft:oak_planks", Shape.STAIR, 448));
                player.getInventory().setItem(13, Materials.piece("minecraft:oak_planks", Shape.CORNER, 65));
                player.inventoryMenu.broadcastChanges();
                return true;
            });
            context.waitTicks(5);
            context.runOnClient(mc -> mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
            context.waitTicks(3);
            hoverSlot(context, 13);
            context.runOnClient(mc -> {
                // TestInput.pressMouse hardcodes modifiers=0. Send the actual
                // shifted mouse event so Screen exercises its QUICK_MOVE path.
                var input = (net.fabricmc.fabric.mixin.client.gametest.input.MouseHandlerAccessor)mc.mouseHandler;
                var button = new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,
                    com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT);
                input.invokeOnButton(mc.getWindow().handle(), button, 1);
                input.invokeOnButton(mc.getWindow().handle(), button, 0);
            });
            context.waitTicks(5);
            context.runOnClient(mc -> {
                if (MaterialStacks.units(mc.player.getInventory().getItem(4)) != 512 || Materials.get(mc.player.getInventory().getItem(4)).shape() != Shape.STAIR
                        || MaterialStacks.units(mc.player.getInventory().getItem(3)) != 1 || !mc.player.getInventory().getItem(13).isEmpty()
                        || !mc.player.containerMenu.getCarried().isEmpty()) throw new AssertionError("Shift mouse click did not merge shapes before using empty slots");
                mc.gui.setScreen(null);
            });
            if (!world.getServer().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                return MaterialStacks.units(player.getInventory().getItem(4)) == 512 && MaterialStacks.units(player.getInventory().getItem(3)) == 1
                    && player.getInventory().getItem(13).isEmpty() && player.containerMenu.getCarried().isEmpty();
            })) throw new AssertionError("Server disagrees with shift-click material merge");
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
            // Compare an identical two-material cube straddling a native grid
            // boundary with a multipart cube contained in one grid cell.
            world.getServer().runCommand("fill -6 99 -3 6 104 6 minecraft:stone");
            world.getServer().runCommand("fill -5 100 -2 5 103 5 minecraft:air");
            world.getServer().runCommand("fill -5 104 -2 5 104 5 minecraft:glowstone");
            world.getServer().runCommand("tp @a -0.25 100 4.5 180 12");
            world.getServer().computeOnServer(server -> {
                var level = server.overworld();
                var quartz = new BlockPos(-3, 100, 0);
                var blackstone = new BlockPos(-2, 100, 0);
                level.setBlock(quartz, nativeSlabState(Materials.piece("minecraft:quartz_block", Shape.VERTICAL_SLAB, 4), 170, level, quartz), 3);
                level.setBlock(blackstone, nativeSlabState(Materials.piece("minecraft:blackstone", Shape.VERTICAL_SLAB, 4), 85, level, blackstone), 3);
                var combined = new BlockPos(1, 100, 0);
                level.setBlock(combined, BuildingPalette.MULTIPART.defaultBlockState(), 3);
                ((MultipartEntity)level.getBlockEntity(combined)).setCells(new Cells().place(85, "minecraft:quartz_block").place(170, "minecraft:blackstone"));
                return true;
            });
            context.waitTicks(25);
            world.getConnection().waitForChunksRender();
            context.runOnClient(mc -> {
                int nativeFace = mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, new BlockPos(-3, 101, 0));
                int combinedFace = mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, new BlockPos(1, 101, 0));
                if (nativeFace <= 0 || nativeFace != combinedFace) throw new AssertionError("Comparison faces do not have equal lighting");
                mc.gui.toastManager().clear();
            });
            context.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("building_palette_material_lighting").disableCounterPrefix().withDestinationDir(java.nio.file.Path.of("../../previews")));
            // Exercise the normal serverbound pick packet, including first/second pick.
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("tp @a 0.25 100 -1.5 0 38");
            world.getServer().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.getInventory().clearContent();
                player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
                player.getInventory().setItem(10, Materials.piece("minecraft:oak_planks", Shape.SLAB, 80));
                server.overworld().setBlock(new BlockPos(0, 100, 0), net.minecraft.world.level.block.Blocks.OAK_STAIRS.defaultBlockState(), 3);
                player.inventoryMenu.broadcastChanges();
                return true;
            });
            context.waitTicks(5);
            context.runOnClient(mc -> mc.gameMode.handlePickItemFromBlock(new BlockPos(0, 100, 0), false));
            context.waitTicks(5);
            assertPicked(context, Shape.SLAB, 80);
            context.runOnClient(mc -> mc.gameMode.handlePickItemFromBlock(new BlockPos(0, 100, 0), false));
            context.waitTicks(5);
            assertPicked(context, Shape.STAIR, 80);
            world.getServer().computeOnServer(server -> {
                var level = server.overworld();
                level.setBlock(new BlockPos(0, 100, 0), BuildingPalette.MULTIPART.defaultBlockState(), 3);
                ((MultipartEntity)level.getBlockEntity(new BlockPos(0, 100, 0))).setCells(new Cells().place(95, "minecraft:oak_planks"));
                return true;
            });
            world.getServer().runCommand("tp @a 0.25 100 -1.5 0 25");
            context.waitTicks(5);
            context.runOnClient(mc -> mc.gameMode.handlePickItemFromBlock(new BlockPos(0, 100, 0), false));
            context.waitTicks(5);
            assertPicked(context, Shape.VERTICAL_STEP, 80);
            world.getServer().computeOnServer(server -> {
                ((MultipartEntity)server.overworld().getBlockEntity(new BlockPos(0, 100, 0))).setCells(new Cells().place(153, "minecraft:oak_planks"));
                return true;
            });
            world.getServer().runCommand("tp @a 0.25 100 -1.5 0 38");
            context.waitTicks(5);
            context.runOnClient(mc -> mc.gameMode.handlePickItemFromBlock(new BlockPos(0, 100, 0), false));
            context.waitTicks(5);
            assertPicked(context, Shape.STEP, 80);
            if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("travelersbackpack")) {
                world.getServer().computeOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    BackpackPickTest.prepare(player);
                    server.overworld().setBlock(new BlockPos(0, 100, 0), net.minecraft.world.level.block.Blocks.STONE_STAIRS.defaultBlockState(), 3);
                    return true;
                });
                context.waitTicks(5);
                context.runOnClient(mc -> mc.gameMode.handlePickItemFromBlock(new BlockPos(0, 100, 0), false));
                context.waitTicks(5);
                world.getServer().computeOnServer(server -> { BackpackPickTest.check(server.getPlayerList().getPlayers().getFirst(), Shape.SLAB); return true; });
                context.runOnClient(mc -> mc.gameMode.handlePickItemFromBlock(new BlockPos(0, 100, 0), false));
                context.waitTicks(5);
                world.getServer().computeOnServer(server -> { BackpackPickTest.check(server.getPlayerList().getPlayers().getFirst(), Shape.STAIR); return true; });
                assertPicked(context, Shape.STAIR, 65);
            }
            // The client must keep the carrier until the server sends remaining cells.
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("tp @a 0.25 100 -1.5 0 38");
            world.getServer().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                server.overworld().setBlock(new BlockPos(0, 100, 0), BuildingPalette.MULTIPART.defaultBlockState(), 3);
                ((MultipartEntity)server.overworld().getBlockEntity(new BlockPos(0, 100, 0))).setCells(new Cells().place(3, "minecraft:oak_planks").place(192, "minecraft:stone_bricks"));
                player.inventoryMenu.broadcastChanges();
                return true;
            });
            context.waitTicks(5);
            context.runOnClient(mc -> {
                mc.gameMode.startDestroyBlock(new BlockPos(0, 100, 0), net.minecraft.core.Direction.NORTH);
                if (!(mc.level.getBlockEntity(new BlockPos(0, 100, 0)) instanceof MultipartEntity)) throw new AssertionError("Client predicted removal of remaining materials");
            });
            context.waitTicks(10);
            context.runOnClient(mc -> {
                var entity = mc.level.getBlockEntity(new BlockPos(0, 100, 0));
                if (!(entity instanceof MultipartEntity multipart) || multipart.cells().occupied() != 192) throw new AssertionError("Remaining stone is not visible/targetable on client");
            });
            if (!world.getServer().computeOnServer(server -> server.overworld().getBlockEntity(new BlockPos(0, 100, 0)) instanceof MultipartEntity entity
                    && entity.cells().occupied() == 192)) throw new AssertionError("Server removed other material");
            world.getServer().runCommand("tp @a 0.75 100 -1.5 0 21");
            context.waitTicks(5);
            context.runOnClient(mc -> mc.gameMode.startDestroyBlock(new BlockPos(0, 100, 0), net.minecraft.core.Direction.NORTH));
            context.waitTicks(10);
            context.runOnClient(mc -> {
                if (!mc.level.getBlockState(new BlockPos(0, 100, 0)).isAir()) throw new AssertionError("Leftover material could not be mined separately");
            });
            if (!world.getServer().computeOnServer(server -> server.overworld().getBlockState(new BlockPos(0, 100, 0)).isAir()))
                throw new AssertionError("Server retained invisible collision after final material removal");
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("fill -5 100 -2 5 103 5 minecraft:air");
            world.getServer().runCommand("tp @a 0.5 100 0.5 0 38");
            world.getServer().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                var pickaxe = new ItemStack(Items.NETHERITE_PICKAXE);
                pickaxe.enchant(server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.EFFICIENCY), 5);
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pickaxe);
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.HASTE, 12000, 1));
                for (int x = -2; x <= 2; x++) {
                    var pos = new BlockPos(x, 100, 2);
                    server.overworld().setBlock(pos, BuildingPalette.MULTIPART.defaultBlockState(), 3);
                    ((MultipartEntity)server.overworld().getBlockEntity(pos)).setCells(new Cells().place(51, "minecraft:stone").place(68, "minecraft:blackstone"));
                }
                player.inventoryMenu.broadcastChanges();
                return true;
            });
            context.waitTicks(10);
            context.runOnClient(mc -> {
                if (mc.player.getEffect(net.minecraft.world.effect.MobEffects.HASTE) == null
                        || mc.player.getEffect(net.minecraft.world.effect.MobEffects.HASTE).getAmplifier() != 1) throw new AssertionError("Fast-mining test needs Haste II");
            });
            for (int x = -2; x <= 2; x++) {
                int column = x;
                context.runOnClient(mc -> {
                    var pos = new BlockPos(column, 100, 2);
                    aimAndSend(mc, new net.minecraft.world.phys.Vec3(column + .5, 100.25, 2.25));
                    if (mc.level.getBlockState(pos).getDestroyProgress(mc.player, mc.level, pos) < 1) throw new AssertionError("Efficiency V/Haste II did not reach instant stone mining");
                    mc.gameMode.startDestroyBlock(pos, net.minecraft.core.Direction.NORTH);
                    if (!(mc.level.getBlockEntity(pos) instanceof MultipartEntity)) throw new AssertionError("Fast mining predicted full removal");
                });
                context.waitTicks(1);
            }
            context.waitTicks(10);
            String fastServerState = world.getServer().computeOnServer(server -> {
                var out = new StringBuilder();
                for (int x = -2; x <= 2; x++) {
                    var entity = server.overworld().getBlockEntity(new BlockPos(x, 100, 2));
                    out.append(x).append(':').append(entity instanceof MultipartEntity e ? e.cells().occupied() : server.overworld().getBlockState(new BlockPos(x, 100, 2))).append(' ');
                }
                var player = server.getPlayerList().getPlayers().getFirst();
                out.append(" player=").append(player.position()).append(" ground=").append(player.onGround()).append(" yaw=").append(player.getYRot()).append(" pitch=").append(player.getXRot())
                    .append(" hit=").append(Placement.pick(player)).append(" tool=").append(player.getMainHandItem())
                    .append(" progress=").append(server.overworld().getBlockState(new BlockPos(2,100,2)).getDestroyProgress(player, server.overworld(), new BlockPos(2,100,2)));
                return out.toString();
            });
            context.runOnClient(mc -> {
                for (int x = -2; x <= 2; x++) if (!(mc.level.getBlockEntity(new BlockPos(x, 100, 2)) instanceof MultipartEntity entity)
                        || entity.cells().occupied() != 68) throw new AssertionError("Fast mining remaining cells at x=" + x + "; server=" + fastServerState
                            + "; client=" + (mc.level.getBlockEntity(new BlockPos(x, 100, 2)) instanceof MultipartEntity e ? e.cells().occupied() : mc.level.getBlockState(new BlockPos(x, 100, 2))));
            });
            context.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("building_palette_fast_mining_leftovers").disableCounterPrefix().withDestinationDir(java.nio.file.Path.of("../../previews")));
            for (int x = -2; x <= 2; x++) {
                int column = x;
                context.runOnClient(mc -> {
                    var pos = new BlockPos(column, 100, 2);
                    aimAndSend(mc, new net.minecraft.world.phys.Vec3(column + .25, 100.75, 2.25));
                    if (mc.level.getBlockState(pos).getDestroyProgress(mc.player, mc.level, pos) < 1) throw new AssertionError("Leftover material is not targetable for instant mining");
                    mc.gameMode.startDestroyBlock(pos, net.minecraft.core.Direction.NORTH);
                });
                context.waitTicks(1);
            }
            context.waitTicks(10);
            context.runOnClient(mc -> {
                for (int x = -2; x <= 2; x++) if (!mc.level.getBlockState(new BlockPos(x, 100, 2)).isAir()) throw new AssertionError("Fast mining left client collision at x=" + x);
            });
            if (!world.getServer().computeOnServer(server -> {
                for (int x = -2; x <= 2; x++) if (!server.overworld().getBlockState(new BlockPos(x, 100, 2)).isAir()) return false;
                return true;
            })) throw new AssertionError("Fast mining left invisible server collision");
            // Use real player entities and placement packets for native/custom
            // paths, including legal placements within an occupied grid cell.
            for (boolean precise : new boolean[]{false, true}) for (int scenario = 0; scenario < 3; scenario++) {
                boolean allowed = scenario >= 2;
                Shape shape = scenario == 0 ? Shape.BLOCK : Shape.CORNER;
                double offset = scenario == 2 ? .9 : .1;
                double height = 100;
                world.getServer().runCommand("tp @a " + offset + " " + height + " " + offset + " 0 45");
                world.getServer().computeOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    PlacementPreferences.set(player, precise);
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, Materials.piece("minecraft:oak_planks", shape, 16));
                    server.overworld().setBlock(new BlockPos(0, 100, 0), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    player.inventoryMenu.broadcastChanges();
                    return true;
                });
                context.runOnClient(mc -> dev.jensh.buildingpalette.client.PaletteConfig.precisePlacement = precise);
                context.waitTicks(5);
                context.runOnClient(mc -> mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(.25, 100, .25), net.minecraft.core.Direction.UP, new BlockPos(0, 99, 0), false)));
                context.waitTicks(5);
                if (!world.getServer().computeOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    return server.overworld().getBlockState(new BlockPos(0, 100, 0)).isAir() != allowed
                        && MaterialStacks.units(player.getMainHandItem()) == 16 - (allowed ? shape.cost : 0);
                })) throw new AssertionError("Server player collision mismatch for " + shape + ", precise=" + precise + ", allowed=" + allowed);
                context.runOnClient(mc -> {
                    if (mc.level.getBlockState(new BlockPos(0, 100, 0)).isAir() == allowed
                            || MaterialStacks.units(mc.player.getMainHandItem()) != 16 - (allowed ? shape.cost : 0)) throw new AssertionError("Client player collision prediction mismatch");
                });
            }
            context.runOnClient(mc -> dev.jensh.buildingpalette.client.PaletteConfig.precisePlacement = false);
            // Actual step-item packets must never fall through to CNM's
            // restricted native step states, including the very first piece.
            for (Shape shape : new Shape[]{Shape.STEP, Shape.VERTICAL_STEP}) for (boolean mixed : new boolean[]{false, true}) {
                world.getServer().runCommand("tp @a 2.5 100 -1.5 0 30");
                world.getServer().computeOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    PlacementPreferences.set(player, false);
                    player.getInventory().setSelectedSlot(0);
                    player.getInventory().setItem(0, Materials.piece("minecraft:oak_planks", shape, mixed ? 4 : 8));
                    player.getInventory().setItem(1, mixed ? Materials.piece("minecraft:stone_bricks", shape, 4) : ItemStack.EMPTY);
                    server.overworld().setBlock(new BlockPos(0, 100, 0), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    player.inventoryMenu.broadcastChanges();
                    return true;
                });
                context.waitTicks(5);
                Cells expected = new Cells();
                for (int part = 0; part < 4; part++) {
                    boolean stone = mixed && (part == 1 || part == 2);
                    int half = part / 2, side = part % 2;
                    double x = shape == Shape.STEP ? .25 : .25 + half * .5;
                    double y = shape == Shape.STEP ? half * .5 : 0;
                    double z = .25 + side * .5;
                    int mask = Cells.mask(shape, shape == Shape.STEP ? (half << 1) | (side << 2) : half | (side << 2), 2);
                    expected = expected.place(mask, stone ? "minecraft:stone_bricks" : "minecraft:oak_planks");
                    Cells wanted = expected;
                    boolean fullNative = part == 3 && !mixed;
                    context.runOnClient(mc -> {
                        mc.player.getInventory().setSelectedSlot(stone ? 1 : 0);
                        mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND,
                            new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(x, 100 + y, z), net.minecraft.core.Direction.UP,
                                new BlockPos(0, y == 0 ? 99 : 100, 0), false));
                    });
                    context.waitTicks(5);
                    context.runOnClient(mc -> {
                        var pos = new BlockPos(0, 100, 0);
                        if (fullNative ? !mc.level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.OAK_PLANKS)
                                : !(mc.level.getBlockEntity(pos) instanceof MultipartEntity entity) || !java.util.Arrays.equals(entity.cells().copy(), wanted.copy()))
                            throw new AssertionError("Client step packet used wrong carrier or material arrangement");
                    });
                    if (!world.getServer().computeOnServer(server -> {
                        var pos = new BlockPos(0, 100, 0);
                        return fullNative ? server.overworld().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.OAK_PLANKS)
                            : server.overworld().getBlockEntity(pos) instanceof MultipartEntity entity && java.util.Arrays.equals(entity.cells().copy(), wanted.copy());
                    })) throw new AssertionError("Server step packet did not preserve all four quarter positions/materials");
                }
                context.runOnClient(mc -> {
                    if (!mc.player.getInventory().getItem(0).isEmpty() || !mc.player.getInventory().getItem(1).isEmpty())
                        throw new AssertionError("Step assembly consumed the wrong material balance");
                });
            }
            // Chunk geometry must remain visible beyond the old block-entity
            // renderer's 64-block cutoff, even at low entity render distance.
            world.getServer().runCommand("fill -6 100 -2 6 106 84 minecraft:air");
            world.getServer().runCommand("fill -6 99 -2 6 99 84 minecraft:stone");
            world.getServer().computeOnServer(server -> {
                var level = server.overworld();
                for (int x = -4; x <= 4; x++) if (Math.abs(x) >= 2) for (int y = 100; y <= 104; y++) {
                    var pos = new BlockPos(x, y, 0);
                    if (x < 0) level.setBlock(pos, net.minecraft.world.level.block.Blocks.BLACKSTONE_SLAB.defaultBlockState(), 3);
                    else {
                        level.setBlock(pos, BuildingPalette.MULTIPART.defaultBlockState(), 3);
                        ((MultipartEntity)level.getBlockEntity(pos)).setCells(new Cells().place(51, "minecraft:blackstone").place(128, "minecraft:quartz_block"));
                    }
                }
                return true;
            });
            context.runOnClient(mc -> {
                mc.options.entityDistanceScaling().set(.5);
                mc.options.renderDistance().set(12);
                mc.options.fov().set(30);
            });
            world.getServer().runCommand("tp @a 0.5 100 80.5 180 0");
            context.waitTicks(30);
            context.waitFor(mc -> mc.level.getBlockEntity(new BlockPos(3, 102, 0)) instanceof MultipartEntity
                && mc.levelRenderer.isSectionCompiledAndVisible(new BlockPos(3, 102, 0), 0));
            context.runOnClient(mc -> {
                var pos = new BlockPos(3, 102, 0);
                var entity = mc.level.getBlockEntity(pos);
                if (!(entity instanceof MultipartEntity)) throw new AssertionError("Far multipart chunk was not loaded");
                if (mc.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) <= 64 * 64) throw new AssertionError("Distance test is inside old renderer range");
                if (mc.level.getBlockState(pos).getRenderShape() != net.minecraft.world.level.block.RenderShape.MODEL
                        || mc.getBlockEntityRenderDispatcher().getRenderer(entity) != null)
                    throw new AssertionError("Multipart still depends on an entity renderer at long distance");
                // Pack mods may wrap the model. Require its terrain output,
                // rather than a particular outer Java implementation class.
                var model = mc.getModelManager().getBlockStateModelSet().get(mc.level.getBlockState(pos));
                var mesh = net.fabricmc.fabric.api.client.renderer.v1.Renderer.get().mutableMesh();
                ((net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel)model).emitQuads(mesh.emitter(), mc.level,
                    pos, mc.level.getBlockState(pos), net.minecraft.util.RandomSource.create(1), direction -> false);
                if (mesh.size() == 0) throw new AssertionError("Far multipart terrain geometry is empty");
                System.out.println("[Building Palette] Far terrain geometry verified through " + model.getClass().getName());
            });
            context.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("building_palette_terrain_distance").disableCounterPrefix().withDestinationDir(java.nio.file.Path.of("../../previews")));
        }
    }
    private static void assertPicked(ClientGameTestContext context, Shape shape, int units) {
        context.runOnClient(mc -> {
            var selected = Materials.get(mc.player.getMainHandItem());
            if (selected == null || selected.shape() != shape || MaterialStacks.units(mc.player.getMainHandItem()) != units)
                throw new AssertionError("Material pick mismatch: wanted " + shape + " with " + units + " units, got " + mc.player.getMainHandItem());
        });
    }
    private static void aimAndSend(net.minecraft.client.Minecraft mc, net.minecraft.world.phys.Vec3 point) {
        var delta = point.subtract(mc.player.getEyePosition());
        float yaw = (float)(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90);
        float pitch = (float)-Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
        mc.player.setYRot(yaw); mc.player.setYHeadRot(yaw); mc.player.setXRot(pitch);
        mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(yaw, pitch, mc.player.onGround(), mc.player.horizontalCollision));
    }
    private static net.minecraft.world.level.block.state.BlockState nativeSlabState(ItemStack item, int wanted,
            net.minecraft.world.level.Level level, BlockPos pos) {
        if (!(item.getItem() instanceof BlockItem block)) throw new AssertionError("Expected a native vertical slab: " + item);
        for (var state : block.getBlock().getStateDefinition().getPossibleStates()) {
            if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)
                    && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) continue;
            int mask = 0;
            var boxes = state.getShape(level, pos).toAabbs();
            for (int i = 0; i < 8; i++) {
                double x = (i & 1) * .5 + .25, y = ((i >> 1) & 1) * .5 + .25, z = ((i >> 2) & 1) * .5 + .25;
                if (boxes.stream().anyMatch(box -> box.contains(x, y, z))) mask |= 1 << i;
            }
            if (mask == wanted) return state;
        }
        throw new AssertionError("No native vertical slab orientation for " + item);
    }
    private static void hoverSlot(ClientGameTestContext context, int index) {
        double[] point = context.computeOnClient(mc -> {
            var screen = (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)mc.gui.screen();
            try {
                var type = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class;
                var left = type.getDeclaredField("leftPos"); left.setAccessible(true);
                var top = type.getDeclaredField("topPos"); top.setAccessible(true);
                var slot = screen.getMenu().getSlot(index);
                double scale = mc.getWindow().getGuiScale();
                return new double[]{(left.getInt(screen) + slot.x + 8) * scale, (top.getInt(screen) + slot.y + 8) * scale};
            } catch (ReflectiveOperationException error) { throw new AssertionError("Could not locate inventory slot", error); }
        });
        context.getInput().setCursorPos(point[0], point[1]);
        context.waitTicks(2);
    }
}
