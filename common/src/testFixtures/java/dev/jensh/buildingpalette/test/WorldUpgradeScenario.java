package dev.jensh.buildingpalette.test;

import com.mojang.authlib.GameProfile;
import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.core.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Test-only mod: boot released jars in separate JVMs against actual saved worlds. */
public abstract class WorldUpgradeScenario {
    private static final GameProfile PROFILE = new GameProfile(UUID.fromString("61e55f9b-42d5-4db6-bdb3-b59a40e04abb"), "UpgradeFixture");
    private static final BlockPos WET = new BlockPos(0, 100, 40), LAMP = new BlockPos(4, 100, 40);
    private int ticks, checks;
    private boolean passed, finished;
    private String phase, version;
    protected abstract net.minecraft.server.level.ServerPlayer fakePlayer(ServerLevel level, GameProfile profile);
    protected void initialize(String actualVersion) {
        phase = System.getProperty("palette.upgrade.phase");
        version = actualVersion;
        require(version.equals(System.getProperty("palette.upgrade.version")), "Wrong released jar loaded: " + version);
    }
    public void started(MinecraftServer server) {
            try {
                var level = server.overworld();
                for (int x = -1; x <= 6; x++) for (int z = 0; z <= 2; z++) {
                    level.setChunkForced(x, z, true);
                    level.getChunk(x, z);
                }
                if (phase.equals("seed")) seed(server);
            } catch (Throwable error) { fail(server, error); }
    }
    public void tick(MinecraftServer server) {
            if (finished || ++ticks < 100) return;
            try {
                if (!phase.equals("seed")) {
                    verify(server, phase.equals("reopen"));
                    if (phase.equals("upgrade")) exerciseOldBlock(server);
                }
                server.saveEverything(false, true, true);
                passed = true; finished = true;
                System.out.println("[UPGRADE TEST] PASS " + phase + " on " + version + ": " + checks + " checks");
                server.halt(false);
            } catch (Throwable error) { fail(server, error); }
    }
    public void stopped(MinecraftServer server) {
            try { Files.writeString(Path.of("upgrade-result.json"), "{\"passed\":" + passed + ",\"phase\":\"" + phase + "\",\"version\":\"" + version + "\",\"checks\":" + checks + "}\n"); }
            catch (Exception error) { throw new RuntimeException(error); }
    }
    private void fail(MinecraftServer server, Throwable error) {
        finished = true; error.printStackTrace(); server.halt(false);
    }
    private void require(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    private static BlockPos position(int mask, boolean mixed) {
        int index = (mixed ? 255 : 0) + mask - 1;
        return new BlockPos(index % 32 * 2, 100, index / 32 * 2);
    }
    private static Cells cells(int mask, boolean mixed) {
        var cells = new Cells();
        for (int i = 0; i < 8; i++) if ((mask & 1 << i) != 0)
            cells = cells.place(1 << i, mixed && (i & 1) != 0 ? "minecraft:stone_bricks" : "minecraft:oak_planks");
        return cells;
    }
    private static void set(ServerLevel level, BlockPos pos, Cells cells, boolean wet) {
        level.setBlock(pos, net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("building_palette", "multipart")).defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, wet), 3);
        ((MultipartEntity)level.getBlockEntity(pos)).setCells(cells);
    }
    private static List<ItemStack> items() {
        var items = new ArrayList<ItemStack>();
        for (Item parent : new Item[]{Items.OAK_PLANKS, Items.STONE_BRICKS, Items.QUARTZ_BLOCK, Items.BLACKSTONE})
            for (ItemStack shape : Materials.palette(new ItemStack(parent))) for (int units : new int[]{1, 65, 512}) {
                var item = MaterialStacks.withUnits(shape, units);
                item.set(DataComponents.CUSTOM_NAME, Component.literal("Upgrade fixture " + items.size()));
                items.add(item);
            }
        return items;
    }
    private static PlayerDataStorage storage(MinecraftServer server) throws Exception {
        var field = MinecraftServer.class.getDeclaredField("playerDataStorage"); field.setAccessible(true);
        return (PlayerDataStorage)field.get(server);
    }
    private void seed(MinecraftServer server) throws Exception {
        var level = server.overworld();
        for (boolean mixed : new boolean[]{false, true}) for (int mask = 1; mask <= 255; mask++) {
            var pos = position(mask, mixed);
            level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
            set(level, pos, cells(mask, mixed), false);
        }
        for (Direction direction : Direction.values()) level.setBlock(WET.relative(direction), Blocks.STONE.defaultBlockState(), 3);
        set(level, WET, cells(17, true), true);
        set(level, LAMP, new Cells().place(1, "minecraft:glowstone").place(128, "minecraft:oak_planks"), false);
        // An old partial roof has no saved per-octant occlusion property.
        for (int x = 96; x <= 100; x++) for (int y = 100; y <= 104; y++) for (int z = 0; z <= 4; z++) {
            var pos = new BlockPos(x, y, z);
            if (x == 96 || x == 100 || z == 0 || z == 4 || y == 100) level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
            else if (y == 104) set(level, pos, cells(51, false), false);
            else level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
        level.setBlock(new BlockPos(98, 105, 2), Blocks.GLOWSTONE.defaultBlockState(), 3);
        var items = items(); require(items.size() >= 84, "Fixture must cover all supported shapes and partial balances");
        for (int i = 0; i < items.size(); i++) {
            var pos = new BlockPos(80, 100, (i / 27) * 2);
            if (i % 27 == 0) level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
            ((ChestBlockEntity)level.getBlockEntity(pos)).setItem(i % 27, items.get(i).copy());
        }
        var player = fakePlayer(level, PROFILE);
        player.setPos(80, 101, 12);
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, items.get(i).copy());
        player.getInventory().setSelectedSlot(5);
        player.setItemInHand(InteractionHand.OFF_HAND, items.get(36).copy());
        for (int i = 0; i < 27; i++) player.getEnderChestInventory().setItem(i, items.get(37 + i).copy());
        storage(server).save(player);
        for (int i = 0; i < 3; i++) {
            level.setBlock(new BlockPos(80 + i * 4, 99, 16), Blocks.STONE.defaultBlockState(), 3);
            var drop = new ItemEntity(level, 80.5 + i * 4, 100, 16.5, items.get(i).copy());
            drop.setNeverPickUp(); drop.setUnlimitedLifetime(); drop.setNoGravity(true); drop.setDeltaMovement(Vec3.ZERO);
            level.addFreshEntity(drop);
        }
        System.out.println("[UPGRADE TEST] Created 510 multipart masks, wet/light fixtures, old slab roof, " + items.size() + " chest stacks, player inventory/ender chest and drops");
    }
    private void sameStack(ItemStack expected, ItemStack actual, String where) {
        require(expected.getCount() == actual.getCount(), where + " item count changed");
        require(MaterialStacks.units(expected) == MaterialStacks.units(actual), where + " material balance changed");
        require(ItemStack.isSameItemSameComponents(expected, actual), where + " item/components changed");
    }
    private void verify(MinecraftServer server, boolean reopened) throws Exception {
        var level = server.overworld();
        for (boolean mixed : new boolean[]{false, true}) for (int mask = 1; mask <= 255; mask++) {
            var pos = position(mask, mixed);
            Cells expected = cells(mask, mixed);
            if (reopened && mixed && mask == 7) expected = expected.remove(4);
            require(level.getBlockEntity(pos) instanceof MultipartEntity, "Missing old multipart at " + pos);
            var entity = (MultipartEntity)level.getBlockEntity(pos);
            require(Arrays.equals(expected.copy(), entity.cells().copy()), "Changed saved materials at " + pos);
            require(entity.getBlockState().getValue(MultipartBlock.OCCLUSION) == expected.occupied(), "Occlusion migration missed " + pos);
            require(Math.abs(entity.getBlockState().getCollisionShape(level, pos).toAabbs().stream().mapToDouble(box -> (box.maxX-box.minX)*(box.maxY-box.minY)*(box.maxZ-box.minZ)).sum() - Integer.bitCount(expected.occupied()) / 8.0) < .000001, "Collision changed at " + pos);
        }
        require(level.getBlockState(WET).getValue(BlockStateProperties.WATERLOGGED), "Waterlogging lost");
        require(Arrays.equals(((MultipartEntity)level.getBlockEntity(WET)).cells().copy(), cells(17, true).copy()), "Wet materials changed");
        require(level.getBlockState(LAMP).getLightEmission() == 15, "Saved emitting material lost light");
        require(level.getBrightness(LightLayer.SKY, new BlockPos(98, 103, 2)) == 0, "Old slab roof leaks skylight after upgrade");
        require(level.getBrightness(LightLayer.BLOCK, new BlockPos(98, 103, 2)) == 0, "Old slab roof leaks block light after upgrade");
        var items = items();
        for (int i = 0; i < items.size(); i++) sameStack(items.get(i), ((ChestBlockEntity)level.getBlockEntity(new BlockPos(80, 100, (i / 27) * 2))).getItem(i % 27), "chest " + i);
        var player = fakePlayer(level, PROFILE);
        var saved = storage(server).load(new NameAndId(PROFILE)).orElseThrow();
        player.load(TagValueInput.create(ProblemReporter.DISCARDING, server.registryAccess(), saved));
        require(player.getInventory().getSelectedSlot() == 5, "Selected hotbar slot changed");
        for (int i = 0; i < 36; i++) sameStack(items.get(i), player.getInventory().getItem(i), "player " + i);
        sameStack(items.get(36), player.getOffhandItem(), "offhand");
        for (int i = 0; i < 27; i++) sameStack(items.get(37 + i), player.getEnderChestInventory().getItem(i), "ender chest " + i);
        for (int i = 0; i < 3; i++) {
            var pos = new Vec3(80.5 + i * 4, 100, 16.5);
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos.add(-1,-1,-1), pos.add(1,1,1)));
            require(drops.size() == 1, "Saved dropped stack missing/merged at " + pos);
            sameStack(items.get(i), drops.getFirst().getItem(), "dropped " + i);
        }
    }
    private void exerciseOldBlock(MinecraftServer server) {
        var level = server.overworld(); var player = fakePlayer(level, PROFILE);
        var pos = position(7, true);
        var held = player.getMainHandItem().copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_AXE));
        player.setPos(pos.getX() + .25, pos.getY(), pos.getZ() - 2);
        var aim = new Vec3(pos.getX() + .25, pos.getY() + .25, pos.getZ() + .25).subtract(player.getEyePosition());
        player.setYRot((float)(Math.toDegrees(Math.atan2(aim.z, aim.x)) - 90));
        player.setXRot((float)-Math.toDegrees(Math.atan2(aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z))));
        require(Placement.mine(player, pos), "Cannot mine old block");
        require(Integer.bitCount(Placement.existing(level, pos).occupied()) == 2, "Mining old material removed other material");
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
        require(drops.stream().mapToInt(e -> MaterialStacks.units(e.getItem())).sum() == 2, "Old block refund balance changed");
        player.setItemInHand(InteractionHand.MAIN_HAND, Materials.piece("minecraft:oak_planks", Shape.CORNER, 1));
        var hit = new BlockHitResult(new Vec3(pos.getX() + .25, pos.getY(), pos.getZ() + .25), Direction.UP, pos.below(), false);
        require(Placement.use(player, level, InteractionHand.MAIN_HAND, hit) == InteractionResult.SUCCESS, "Cannot add new part to old saved block");
        require(Integer.bitCount(Placement.existing(level, pos).occupied()) == 3, "Combining into old block changed leftovers");
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
    }
}
