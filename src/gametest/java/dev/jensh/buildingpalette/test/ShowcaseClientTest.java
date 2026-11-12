package dev.jensh.buildingpalette.test;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.core.*;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Renders the Modrinth page images. Runs only with -PpaletteShowcase; not part of the test suite. */
public final class ShowcaseClientTest implements FabricClientGameTest {
    // Cell masks: index = x | y << 1 | z << 2. The camera looks north, so "front" is +z.
    static final int BOTTOM = 0b00110011, TOP = 0b11001100, BACK = 0b00001111, FRONT = 0b11110000, WEST = 0b01010101, EAST = 0b10101010;
    static final String OAK = "minecraft:oak_planks", SPRUCE = "minecraft:spruce_planks", BRICK = "minecraft:stone_bricks",
        MOSSY = "minecraft:mossy_stone_bricks", COBBLE = "minecraft:cobblestone", ANDESITE = "minecraft:polished_andesite";
    // The scene is designed facing +z and rotated to face +x (east), where the morning sun lights it.
    static Vec3 r(Vec3 v) { return new Vec3(v.z, v.y, -v.x); }
    static BlockPos r(BlockPos p) { return new BlockPos(p.getZ(), p.getY(), -p.getX() - 1); }
    private final Path out = Path.of(System.getProperty("building_palette.showcase_dir", "showcase"));

    @Override public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("building_palette.showcase")) return;
        try (var world = context.worldBuilder().create()) {
            var server = world.getServer();
            server.runCommand("gamerule advance_time false");
            server.runCommand("gamerule advance_weather false");
            server.runCommand("time set 1500");
            server.runCommand("weather clear");
            server.runCommand("fill -20 99 -20 20 99 20 minecraft:grass_block");
            server.runCommand("fill -20 98 -20 20 90 20 minecraft:dirt");
            server.runCommand("place feature minecraft:fancy_oak -9 100 9");
            server.runCommand("place feature minecraft:oak -6 100 -12");
            server.runCommand("place feature minecraft:birch -12 100 -7");
            server.computeOnServer(s -> { build(s.overworld()); return true; });
            world.getConnection().waitForChunksRender();
            context.waitTicks(40);
            context.runOnClient(mc -> { mc.gui.toastManager().clear(); mc.options.fov().set(55); });
            context.getInput().pressKey(o -> o.keyToggleGui);

            shot(context, server, "hero", new Vec3(7.2, 102.9, 8.6), new Vec3(0.4, 102.4, -1.4), 1920, 1080);
            shot(context, server, "detail_window", new Vec3(-0.2, 102.4, 4.4), new Vec3(-1.9, 101.4, 0.9), 1280, 720);
            shot(context, server, "detail_bench", new Vec3(5.9, 101.6, 6.4), new Vec3(3.2, 100.4, 3.3), 1280, 720);
            shot(context, server, "detail_roof", new Vec3(7.8, 105.2, 3.6), new Vec3(3.5, 104.6, -1.5), 1280, 720);

            // Two steps laid across each other merge into an L.
            BlockPos merge = new BlockPos(12, 100, 8);
            frame(context, server, "merge_0", merge, new Cells());
            frame(context, server, "merge_1", merge, new Cells().place(0b00010001, OAK));
            frame(context, server, "merge_2", merge, new Cells().place(0b00010001, OAK).place(0b00000010, OAK));
            frame(context, server, "merge_3", merge, new Cells().place(0b00010001, OAK).place(0b00000010, OAK).place(0b00100000, BRICK));
            frame(context, server, "merge_4", merge, new Cells().place(0b00010001, OAK).place(0b00000010, OAK).place(0b00100000, BRICK).place(0b11000000, BRICK));

            // Hotbar: Alt + scroll through the shapes of one stack.
            context.getInput().pressKey(o -> o.keyToggleGui);
            server.computeOnServer(s -> {
                var player = s.getPlayerList().getPlayers().getFirst();
                player.getInventory().clearContent();
                player.getInventory().setItem(0, new ItemStack(Items.STONE_BRICKS, 64));
                player.getInventory().setItem(1, new ItemStack(Items.OAK_PLANKS, 40));
                player.getInventory().setItem(2, Materials.piece(SPRUCE, Shape.STEP, 200));
                player.getInventory().setItem(3, new ItemStack(Items.LANTERN, 4));
                player.getInventory().setSelectedSlot(0);
                player.inventoryMenu.broadcastChanges();
                return true;
            });
            look(server, r(new Vec3(1.5, 101.62, 7.5)), r(new Vec3(0.5, 101.2, 0.5)));
            context.waitTicks(10);
            context.getInput().holdAlt();
            context.waitTicks(3);
            for (int i = 0; i < 8; i++) {
                context.runOnClient(mc -> { mc.gui.toastManager().clear(); mc.gui.hud.getChat().clearMessages(false); });
                context.takeScreenshot(TestScreenshotOptions.of("scroll_" + i).disableCounterPrefix().withSize(1280, 720).withDestinationDir(out));
                context.getInput().scroll(-1);
                context.waitTicks(3);
            }
            context.getInput().releaseAlt();
        }
    }

    private static void look(net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server, Vec3 eye, Vec3 target) {
        Vec3 d = target.subtract(eye);
        float yaw = (float)Math.toDegrees(Math.atan2(-d.x, d.z)), pitch = (float)Math.toDegrees(-Math.atan2(d.y, Math.hypot(d.x, d.z)));
        server.runCommand(String.format(java.util.Locale.ROOT, "tp @a %.3f %.3f %.3f %.2f %.2f", eye.x, eye.y - 1.62, eye.z, yaw, pitch));
    }
    private void shot(ClientGameTestContext context, net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server, String name, Vec3 eye, Vec3 target, int w, int h) {
        look(server, r(eye), r(target));
        context.waitTicks(20);
        context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withSize(w, h).withDestinationDir(out));
    }

    private void frame(ClientGameTestContext context, net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server, String name, BlockPos pos, Cells cells) {
        server.computeOnServer(s -> {
            var level = s.overworld();
            if (cells.occupied() == 0) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            else Placement.store(level, pos, cells, false);
            return true;
        });
        if (name.endsWith("_0")) {
            look(server, new Vec3(pos.getX() + 2.1, pos.getY() + 1.9, pos.getZ() + 2.4), Vec3.atCenterOf(pos).add(0, -.15, 0));
            context.waitTicks(20);
        }
        context.waitTicks(8);
        context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withSize(900, 900).withDestinationDir(out));
    }

    private static void put(ServerLevel level, int x, int y, int z, Object... maskMaterial) {
        String[] values = new String[8];
        for (int k = 0; k < maskMaterial.length; k += 2) for (int i = 0; i < 8; i++) if (((Integer)maskMaterial[k] >> i & 1) != 0) {
            // Rotate the cell with its block: x' = z, z' = -x - 1 in half cells.
            int xh = (i >> 2 & 1), zh = 1 - (i & 1);
            values[xh | (i & 2) | zh << 2] = (String)maskMaterial[k + 1];
        }
        Placement.store(level, r(new BlockPos(x, y, z)), new Cells(values), false);
    }

    private static void block(ServerLevel level, int x, int y, int z, net.minecraft.world.level.block.Block block) {
        level.setBlock(r(new BlockPos(x, y, z)), block.defaultBlockState(), 3);
    }

    /** A small cottage built from half-grid pieces. */
    private static void build(ServerLevel level) {
        var cells = new java.util.HashMap<BlockPos, String[]>();
        java.util.function.BiConsumer<int[], String> cell = (h, material) -> {
            int xh = h[2], zh = -h[0] - 1;
            var pos = new BlockPos(Math.floorDiv(xh, 2), Math.floorDiv(h[1], 2), Math.floorDiv(zh, 2));
            cells.computeIfAbsent(pos, p -> new String[8])[(xh & 1) | (h[1] & 1) << 1 | (zh & 1) << 2] = material;
        };
        // Walls x -3..3, z -4..0, y 100..102 (half-cell coordinates below).
        for (int xh = -6; xh <= 7; xh++) for (int zh = -8; zh <= 1; zh++) for (int yh = 200; yh <= 205; yh++) {
            boolean edgeX = xh <= -5 || xh >= 6, edgeZ = zh <= -7 || zh >= 0;
            if (!edgeX && !edgeZ) continue;
            boolean corner = edgeX && edgeZ;
            String material = corner ? SPRUCE : yh <= 201 ? ((xh + zh) % 3 == 0 ? MOSSY : COBBLE) : BRICK;
            // Door (x 0) and windows (x -2 and 2) in the front wall.
            if (zh >= 0 && xh >= 0 && xh <= 1 && yh <= 203) continue;
            if (zh >= 0 && (xh == -4 || xh == -3 || xh == 4 || xh == 5) && yh >= 202 && yh <= 203) continue;
            cell.accept(new int[]{xh, yh, zh}, material);
        }
        // Protruding half-block timber: a beam along the front, and sills under the windows.
        for (int xh = -7; xh <= 8; xh++) cell.accept(new int[]{xh, 205, 2}, OAK);
        for (int xh : new int[]{-5, -4, -3, -2, 3, 4, 5, 6}) cell.accept(new int[]{xh, 201, 2}, OAK);
        // Door threshold: a half-height step.
        for (int xh = 0; xh <= 1; xh++) for (int zh = 0; zh <= 2; zh++) cell.accept(new int[]{xh, 199, zh}, ANDESITE);
        // Stepped gable roof: one half-cell thick, rising half a block per half block, ridge along x.
        for (int xh = -8; xh <= 9; xh++) for (int zh = -10; zh <= 3; zh++) {
            int rise = Math.min(zh + 10, 3 - zh);
            String material = rise >= 6 ? OAK : (xh == -8 || xh == 9) ? OAK : SPRUCE;
            cell.accept(new int[]{xh, 206 + rise, zh}, material);
            // Gable ends: brick under the roof.
            if (xh == -6 || xh == 7) for (int yh = 206; yh < 206 + rise; yh++) if (zh >= -8 && zh <= 1) cell.accept(new int[]{xh, yh, zh}, BRICK);
        }
        cells.forEach((pos, values) -> Placement.store(level, pos, new Cells(values), false));
        // Bench: oak steps on spruce fence legs that pass through them (Offgrid blocklogging).
        for (int x = 2; x <= 4; x++) put(level, x, 100, 3, TOP & FRONT, OAK);
        for (int x : new int[]{2, 4}) dev.tazer.offgrid.world.Overlays.add(level, r(new BlockPos(x, 100, 3)), Blocks.SPRUCE_FENCE.defaultBlockState(), Vec3.ZERO, null);
        // Path: stepping stones of slabs, steps and corners.
        put(level, 0, 100, 2, BOTTOM & BACK, BRICK, 0b00010000, COBBLE);
        put(level, 1, 100, 3, BOTTOM & WEST, ANDESITE);
        put(level, 0, 100, 4, 0b00000001, MOSSY, 0b00100000, BRICK);
        put(level, 1, 100, 5, BOTTOM & EAST, COBBLE, 0b00010000, MOSSY);
        put(level, 0, 100, 6, BOTTOM & BACK, ANDESITE);
        // Planter box: a hollow frame of steps around a flower.
        put(level, -3, 100, 2, BOTTOM & (WEST | BACK), SPRUCE, 0b00100000, SPRUCE);
        block(level, -2, 100, 2, Blocks.POPPY);
        put(level, -2, 100, 3, 0b00000001, SPRUCE);
        for (var flower : new Object[][]{{-6, 3, Blocks.OXEYE_DAISY}, {8, 1, Blocks.CORNFLOWER}, {-4, 5, Blocks.DANDELION}, {6, 1, Blocks.SHORT_GRASS}, {-5, 1, Blocks.SHORT_GRASS}, {3, 6, Blocks.SHORT_GRASS}})
            block(level, (Integer)flower[0], 100, (Integer)flower[1], (net.minecraft.world.level.block.Block)flower[2]);
    }
}
