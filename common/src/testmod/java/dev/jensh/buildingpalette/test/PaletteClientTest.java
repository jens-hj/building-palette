package dev.jensh.buildingpalette.test;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.client.*;
import dev.jensh.buildingpalette.core.*;
import dev.jensh.buildingpalette.platform.ClientPlatform;
import dev.tazer.clutternomore.client.ClutterNoMoreClient;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;

/** Test-only tick driver: real integrated server, packets and chunk meshes. Loaders call {@link #tick()} after every client tick. */
public abstract class PaletteClientTest {
    private record Step(int delay, Runnable action) {}
    private static final int Y = -60;
    private static final BlockPos CENTER = new BlockPos(0, Y, 0);
    private final Queue<Step> steps = new ArrayDeque<>();
    private boolean started, seeded, done;
    private int delay, ticks;
    protected Minecraft mc;
    /** The loader's render data and replaced carrier model are present at pos. */
    protected abstract void geometry(BlockPos pos);
    /** Fail early when the loader shows its own error screen. */
    protected void checkLoading() {}
    public final void tick() {
        if (done) return;
        mc = Minecraft.getInstance();
        if (mc == null) return;
        try {
            if (++ticks > 6000) throw new AssertionError("Graphical test timed out");
            if (!started) {
                checkLoading();
                if (ticks > 3000) throw new AssertionError("Never reached the title screen; stuck on " + mc.screen);
                if (!(mc.screen instanceof TitleScreen)) return;
                started = true;
                mc.createWorldOpenFlows().createFreshLevel("palette-test-" + System.currentTimeMillis(),
                    new LevelSettings("Building Palette test", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                    new WorldOptions(0, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
                return;
            }
            if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
            if (!seeded) { seeded = true; seed(); scenario(); delay = Integer.getInteger("building_palette.settle_ticks", 60); }
            if (delay-- > 0) return;
            var step = steps.poll();
            if (step != null) { step.action.run(); delay = step.delay; }
        } catch (Throwable error) { error.printStackTrace(); finish(false, error.toString()); }
    }
    private void server(Consumer<MinecraftServer> action) { CompletableFuture.runAsync(() -> action.accept(mc.getSingleplayerServer()), mc.getSingleplayerServer()).join(); }
    protected static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private void add(int delay, Runnable action) { steps.add(new Step(delay, action)); }
    private void command(String command) { server(s -> s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), command)); }
    private static void set(ServerLevel level, BlockPos pos, Cells cells) {
        level.setBlock(pos, BuildingPalette.MULTIPART.get().defaultBlockState(), 3);
        ((MultipartEntity)level.getBlockEntity(pos)).setCells(cells);
    }
    private void seed() {
        mc.setScreen(null);
        mc.options.renderDistance().set(6);
        command("fill -8 " + (Y - 1) + " -8 8 " + (Y - 1) + " 8 minecraft:stone");
        command("fill -8 " + Y + " -8 8 " + (Y + 6) + " 8 minecraft:air");
        command("time set noon"); command("weather clear"); command("tp @a 0.5 " + Y + " 4.5 180 12");
        server(s -> {
            var level = s.overworld();
            set(level, CENTER, new Cells().place(17, "minecraft:oak_planks").place(136, "minecraft:stone_bricks"));
            set(level, CENTER.west(2), new Cells().place(51, "minecraft:oak_planks"));
            set(level, CENTER.east(2), new Cells().place(85, "minecraft:quartz_block").place(170, "minecraft:blackstone"));
            // Tinted and translucent materials exercise colour lookup and render layers.
            set(level, CENTER.above(2), new Cells().place(15, "minecraft:grass_block").place(240, "minecraft:glass"));
            var player = s.getPlayerList().getPlayers().getFirst();
            player.getInventory().selected = 0;
            player.getInventory().setItem(0, Materials.piece("minecraft:oak_planks", Shape.BLOCK, 512));
            player.getInventory().setItem(1, Materials.piece("minecraft:oak_planks", Shape.CORNER, 100));
            player.getInventory().setItem(2, Materials.piece("minecraft:stone_bricks", Shape.VERTICAL_STEP, 101));
            player.getInventory().setItem(3, Materials.piece("minecraft:stone_bricks", Shape.STAIR, 100));
            player.getInventory().setItem(9, Materials.piece("minecraft:stone_bricks", Shape.BLOCK, 65));
            player.inventoryMenu.broadcastChanges();
        });
    }
    private void screenshot(String name) {
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            var directory = Path.of(System.getProperty("building_palette.previews")); Files.createDirectories(directory);
            image.writeToFile(directory.resolve(name + ".png"));
        } catch (Exception error) { throw new RuntimeException(error); }
    }
    private void scenario() {
        add(5, () -> {
            geometry(CENTER); geometry(CENTER.east(2)); geometry(CENTER.above(2));
            require(mc.getBlockColors().getColor(mc.level.getBlockState(CENTER.above(2)), mc.level, CENTER.above(2), 0) != -1, "Grass cell lost its tint");
            var piece = mc.getItemRenderer().getModel(mc.player.getInventory().getItem(1), mc.level, mc.player, 0);
            require(!piece.getQuads(null, null, RandomSource.create(1)).isEmpty(), "Piece item has no cropped model");
            screenshot("building_palette_1_21_1_shapes");
        });
        add(8, () -> { TestInput.alt = true; require(ClutterNoMoreClient.onMouseScrolled(-1), "Shape scroll was not consumed"); });
        add(5, () -> { screenshot("building_palette_1_21_1_hotbar"); TestInput.alt = false; });
        add(5, () -> {
            require(Materials.get(mc.player.getMainHandItem()).shape() == Shape.SLAB && MaterialStacks.units(mc.player.getMainHandItem()) == 512, "Held scroll lost shape or balance");
            server(s -> require(MaterialStacks.units(s.getPlayerList().getPlayers().getFirst().getMainHandItem()) == 512, "Server disagrees with scroll balance"));
            server(s -> s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.SURVIVAL));
        });
        add(5, () -> {
            mc.setScreen(new InventoryScreen(mc.player));
            TestInput.hover = mc.player.inventoryMenu.getSlot(9);
        });
        add(5, () -> {
            TestInput.alt = true;
            require(!ClutterNoMoreClient.allowScreenScroll(mc.screen, 0, 0, 0, -1), "Inventory scroll was not intercepted");
        });
        add(3, () -> { screenshot("building_palette_1_21_1_inventory"); TestInput.alt = false; TestInput.hover = null; });
        add(12, () -> {
            require(Materials.get(mc.player.getInventory().getItem(9)).shape() == Shape.SLAB && MaterialStacks.units(mc.player.getInventory().getItem(9)) == 65, "Inventory scroll lost remainder");
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 9, 0, ClickType.PICKUP, mc.player);
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 10, 0, ClickType.PICKUP, mc.player);
        });
        add(12, () -> { require(mc.player.containerMenu.getCarried().isEmpty() && MaterialStacks.units(mc.player.getInventory().getItem(10)) == 65, "Fractional cursor failed to clear"); mc.setScreen(null); command("gamemode creative @a"); });
        for (Shape shape : new Shape[]{Shape.STEP, Shape.VERTICAL_STEP}) {
            add(8, () -> {
                command("tp @a 2.5 " + Y + " -1.5 0 30");
                server(s -> {
                    var p = s.getPlayerList().getPlayers().getFirst(); p.setGameMode(GameType.SURVIVAL); p.getInventory().selected = 0;
                    p.getInventory().setItem(0, Materials.piece("minecraft:oak_planks", shape, 4)); p.getInventory().setItem(1, Materials.piece("minecraft:stone_bricks", shape, 4));
                    s.overworld().setBlock(CENTER, Blocks.AIR.defaultBlockState(), 3); p.inventoryMenu.broadcastChanges();
                });
            });
            for (int i = 0; i < 4; i++) {
                int part = i;
                add(6, () -> {
                    int half = part / 2, side = part % 2;
                    mc.player.getInventory().selected = part == 1 || part == 2 ? 1 : 0;
                    double x = shape == Shape.STEP ? .25 : .25 + half * .5, y = shape == Shape.STEP ? half * .5 : 0, z = .25 + side * .5;
                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(x, Y + y, z), Direction.UP, new BlockPos(0, y == 0 ? Y - 1 : Y, 0), false));
                });
            }
            add(3, () -> {
                require(Placement.existing(mc.level, CENTER).occupied() == 255, "Quarter assembly incomplete for " + shape);
                require(mc.player.getInventory().getItem(0).isEmpty() && mc.player.getInventory().getItem(1).isEmpty(), "Quarter assembly balance changed");
            });
        }
        add(8, () -> {
            command("gamemode creative @a"); command("tp @a 0.25 " + Y + " -1.5 0 38");
            server(s -> { var p = s.getPlayerList().getPlayers().getFirst(); p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); p.inventoryMenu.broadcastChanges(); set(s.overworld(), CENTER, new Cells().place(3, "minecraft:oak_planks").place(192, "minecraft:stone_bricks")); });
        });
        add(10, () -> {
            require(mc.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(CENTER), "Not aiming at the combined block");
            screenshot("building_palette_1_21_1_outline");
            mc.gameMode.startDestroyBlock(CENTER, Direction.NORTH); require(mc.level.getBlockEntity(CENTER) != null, "Mining predicted full carrier removal");
        });
        add(8, () -> {
            require(Placement.existing(mc.level, CENTER).occupied() == 192, "Mining hid or removed other material");
            server(s -> { var p = s.getPlayerList().getPlayers().getFirst(); p.setGameMode(GameType.SURVIVAL); p.getInventory().selected = 0; p.setItemInHand(InteractionHand.MAIN_HAND, Materials.piece("minecraft:stone", Shape.SLAB, 65)); p.inventoryMenu.broadcastChanges(); s.overworld().setBlock(CENTER, Blocks.STONE_STAIRS.defaultBlockState(), 3); });
        });
        add(8, () -> require(ClientPlatform.INSTANCE.sendPick(new PickPayload(CENTER)), "Server does not accept material picks"));
        add(3, () -> require(Materials.get(mc.player.getMainHandItem()).shape() == Shape.STAIR && MaterialStacks.units(mc.player.getMainHandItem()) == 65, "Pick-block did not reshape held material"));
        add(10, () -> finish(true, "Graphical, packet, inventory, partial mining and terrain tests passed"));
    }
    private void finish(boolean passed, String message) {
        done = true; TestInput.alt = false; TestInput.hover = null;
        try { Files.writeString(Path.of(System.getProperty("building_palette.test_result")), "{\"passed\":" + passed + ",\"message\":\"" + message.replace("\\", "/").replace("\"", "'") + "\"}\n"); }
        catch (Exception e) { e.printStackTrace(); }
        System.out.println("[Building Palette Client Test] " + message);
        mc.stop();
    }
}
