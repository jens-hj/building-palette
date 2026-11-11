package dev.jensh.buildingpalette.test;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.client.*;
import dev.jensh.buildingpalette.core.*;
import dev.jensh.buildingpalette.neoforge.*;
import dev.jensh.buildingpalette.test.mixin.HoveredSlotAccessor;
import dev.tazer.clutternomore.client.ClutterNoMoreClient;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Test-only tick driver: real NeoForge integrated server, packets and chunk meshes. */
public final class NeoClientTest {
    private record Step(int delay, Runnable action) {}
    private final Queue<Step> steps = new ArrayDeque<>();
    private boolean started, seeded, done;
    private int delay, ticks;
    private Minecraft mc;
    public static void install() { var test = new NeoClientTest(); NeoForge.EVENT_BUS.addListener(test::tick); }
    private void tick(ClientTickEvent.Post event) {
        if (done) return;
        mc = Minecraft.getInstance();
        if (mc == null) return;
        try {
            if (++ticks > 2400) throw new AssertionError("NeoForge graphical test timed out");
            if (!started) {
                if (mc.gui.screen() instanceof net.neoforged.neoforge.client.gui.LoadingErrorScreen screen) {
                    for (var child : screen.children()) if (child instanceof net.minecraft.client.gui.components.Button button && button.getMessage().getString().equals(net.neoforged.fml.i18n.FMLTranslations.parseMessage("fml.button.continue.launch"))) {
                        button.onPress(null); return;
                    }
                    throw new AssertionError("NeoForge loading errors; see client log");
                }
                if (!(mc.gui.screen() instanceof TitleScreen)) return;
                started = true;
                mc.createWorldOpenFlows().createFreshLevel("palette-test-" + System.currentTimeMillis(),
                    new LevelSettings("Building Palette test", GameType.CREATIVE, LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT),
                    new WorldOptions(0, false, false), WorldPresets::createTestWorldDimensions, new TitleScreen());
                return;
            }
            if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
            if (!seeded) { seeded = true; seed(); scenario(); delay = 30; }
            if (delay-- > 0) return;
            var step = steps.poll();
            if (step != null) { step.action.run(); delay = step.delay; }
        } catch (Throwable error) { error.printStackTrace(); finish(false, error.toString()); }
    }
    private void server(Consumer<MinecraftServer> action) { CompletableFuture.runAsync(() -> action.accept(mc.getSingleplayerServer()), mc.getSingleplayerServer()).join(); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private void add(int delay, Runnable action) { steps.add(new Step(delay, action)); }
    private void command(String command) { server(s -> s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), command)); }
    private static void set(ServerLevel level, BlockPos pos, Cells cells) {
        level.setBlock(pos, BuildingPalette.MULTIPART.get().defaultBlockState(), 3);
        ((MultipartEntity)level.getBlockEntity(pos)).setCells(cells);
    }
    private void seed() {
        mc.gui.setScreen(null);
        mc.options.renderDistance().set(6);
        command("fill -8 99 -8 8 99 8 minecraft:stone");
        command("time set noon"); command("weather clear"); command("tp @a 0.5 100 4.5 180 12");
        server(s -> {
            var level = s.overworld();
            set(level, new BlockPos(0,100,0), new Cells().place(17,"minecraft:oak_planks").place(136,"minecraft:stone_bricks"));
            set(level, new BlockPos(-2,100,0), new Cells().place(51,"minecraft:oak_planks"));
            set(level, new BlockPos(2,100,0), new Cells().place(85,"minecraft:quartz_block").place(170,"minecraft:blackstone"));
            var player = s.getPlayerList().getPlayers().getFirst();
            player.getInventory().setSelectedSlot(0);
            player.getInventory().setItem(0, Materials.piece("minecraft:oak_planks",Shape.BLOCK,512));
            player.getInventory().setItem(9, Materials.piece("minecraft:stone_bricks",Shape.BLOCK,65));
            player.inventoryMenu.broadcastChanges();
        });
    }
    private void geometry(BlockPos pos) {
        require(mc.level.getBlockEntity(pos) instanceof NeoMultipartEntity, "Wrong NeoForge entity factory");
        var data = mc.level.getModelData(pos).get(NeoMultipartEntity.CELLS);
        require(data != null && data.occupied() != 0, "Missing immutable NeoForge model data");
        var state = mc.level.getBlockState(pos);
        var parts = new ArrayList<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart>();
        var model = mc.getModelManager().getBlockStateModelSet().get(state);
        model.collectParts(mc.level,pos,state,RandomSource.create(1),parts);
        require(parts.stream().anyMatch(part -> !part.getQuads(null).isEmpty()), "Empty NeoForge terrain mesh");
        require(parts.stream().allMatch(net.minecraft.client.renderer.block.dispatch.BlockStateModelPart::useAmbientOcclusion), "Missing parent ambient occlusion");
        require(state.getRenderShape() == RenderShape.MODEL && mc.getBlockEntityRenderDispatcher().getRenderer(mc.level.getBlockEntity(pos)) == null, "Multipart must use chunk rendering");
    }
    private void screenshot(String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try {
                var directory = Path.of(System.getProperty("building_palette.previews")); Files.createDirectories(directory);
                image.writeToFile(directory.resolve(name + ".png"));
            } catch (Exception error) { throw new RuntimeException(error); }
            finally { image.close(); }
        });
    }
    private void scenario() {
        add(5, () -> { geometry(new BlockPos(0,100,0)); geometry(new BlockPos(2,100,0)); OutlineStyleTest.verify(mc); screenshot("building_palette_neoforge_shapes"); });
        add(8, () -> { NeoInput.alt = true; require(ClutterNoMoreClient.onMouseScrolled(-1), "Shape scroll was not consumed"); NeoInput.alt = false; });
        add(5, () -> {
            require(Materials.get(mc.player.getMainHandItem()).shape() == Shape.SLAB && MaterialStacks.units(mc.player.getMainHandItem()) == 512, "NeoForge held scroll lost shape or balance");
            server(s -> require(MaterialStacks.units(s.getPlayerList().getPlayers().getFirst().getMainHandItem()) == 512, "Server disagrees with scroll balance"));
            mc.gui.setScreen(new InventoryScreen(mc.player));
            var slot = mc.player.inventoryMenu.getSlot(9);
            ((HoveredSlotAccessor)mc.gui.screen()).palette$hover(slot);
            NeoInput.alt = true;
            require(!ClutterNoMoreClient.allowScreenScroll(mc.gui.screen(),0,0,0,-1), "Inventory scroll was not intercepted");
            NeoInput.alt = false;
        });
        add(12, () -> {
            require(Materials.get(mc.player.getInventory().getItem(9)).shape() == Shape.SLAB && MaterialStacks.units(mc.player.getInventory().getItem(9)) == 65, "NeoForge inventory scroll lost remainder");
            mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId,9,0,net.minecraft.world.inventory.ContainerInput.PICKUP,mc.player);
            mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId,10,0,net.minecraft.world.inventory.ContainerInput.PICKUP,mc.player);
        });
        add(12, () -> { require(mc.player.containerMenu.getCarried().isEmpty() && MaterialStacks.units(mc.player.getInventory().getItem(10)) == 65, "Fractional cursor failed to clear: cursor="+mc.player.containerMenu.getCarried()+", slot9="+mc.player.getInventory().getItem(9)+", slot10="+mc.player.getInventory().getItem(10)); mc.gui.setScreen(null); });
        add(5, () -> {
            var screen = new PaletteConfigScreen(null); mc.gui.setScreen(screen);
            require(!PaletteConfig.highlightParts,"Outline should default to vanilla");
            ((net.minecraft.client.gui.components.Button)screen.children().get(2)).onPress(null);
            var slider = (net.minecraft.client.gui.components.AbstractSliderButton)screen.children().get(3);
            require(slider.active,"Hue should enable");
            slider.onClick(new net.minecraft.client.input.MouseButtonEvent(slider.getX()+4+.75*(slider.getWidth()-8),slider.getY()+10,new net.minecraft.client.input.MouseButtonInfo(1,0)),false);
            require(PaletteConfig.outlineHue == 270,"Hue slider failed"); PaletteConfig.load(); require(PaletteConfig.highlightParts,"Preferences did not persist");
            ((net.minecraft.client.gui.components.Button)screen.children().get(2)).onPress(null); mc.gui.setScreen(null);
        });
        for (Shape shape : new Shape[]{Shape.STEP,Shape.VERTICAL_STEP}) {
            add(8, () -> {
                command("tp @a 2.5 100 -1.5 0 30");
                server(s -> {
                    var p = s.getPlayerList().getPlayers().getFirst(); p.setGameMode(GameType.SURVIVAL); p.getInventory().setSelectedSlot(0);
                    p.getInventory().setItem(0,Materials.piece("minecraft:oak_planks",shape,4)); p.getInventory().setItem(1,Materials.piece("minecraft:stone_bricks",shape,4));
                    s.overworld().setBlock(new BlockPos(0,100,0),Blocks.AIR.defaultBlockState(),3); p.inventoryMenu.broadcastChanges();
                });
            });
            for (int i=0;i<4;i++) {
                int part=i;
                add(6, () -> {
                    int half=part/2, side=part%2;
                    mc.player.getInventory().setSelectedSlot(part==1||part==2?1:0);
                    double x=shape==Shape.STEP?.25:.25+half*.5, y=shape==Shape.STEP?half*.5:0, z=.25+side*.5;
                    mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(x,100+y,z),Direction.UP,new BlockPos(0,y==0?99:100,0),false));
                });
            }
            add(3, () -> {
                require(Placement.existing(mc.level,new BlockPos(0,100,0)).occupied()==255,"NeoForge quarter assembly incomplete");
                require(mc.player.getInventory().getItem(0).isEmpty()&&mc.player.getInventory().getItem(1).isEmpty(),"Quarter assembly balance changed");
            });
        }
        add(8, () -> {
            command("gamemode creative @a"); command("tp @a 0.25 100 -1.5 0 38");
            server(s -> { var p=s.getPlayerList().getPlayers().getFirst(); p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY); p.inventoryMenu.broadcastChanges(); set(s.overworld(),new BlockPos(0,100,0),new Cells().place(3,"minecraft:oak_planks").place(192,"minecraft:stone_bricks")); });
        });
        add(10, () -> { mc.gameMode.startDestroyBlock(new BlockPos(0,100,0),Direction.NORTH); require(mc.level.getBlockEntity(new BlockPos(0,100,0))!=null,"Mining predicted full carrier removal"); });
        add(8, () -> {
            require(Placement.existing(mc.level,new BlockPos(0,100,0)).occupied()==192,"Mining hid or removed other material");
            server(s -> { var p=s.getPlayerList().getPlayers().getFirst(); p.setGameMode(GameType.SURVIVAL); p.setItemInHand(InteractionHand.MAIN_HAND,Materials.piece("minecraft:stone",Shape.SLAB,65)); p.inventoryMenu.broadcastChanges(); s.overworld().setBlock(new BlockPos(0,100,0),Blocks.STONE_STAIRS.defaultBlockState(),3); });
        });
        add(8, () -> mc.gameMode.handlePickItemFromBlock(new BlockPos(0,100,0),false));
        add(3, () -> require(Materials.get(mc.player.getMainHandItem()).shape()==Shape.STAIR && MaterialStacks.units(mc.player.getMainHandItem())==65,"Native NeoForge pick-block did not reshape held material"));
        if (net.neoforged.fml.ModList.get().isLoaded("travelersbackpack")) {
            add(8, () -> server(s -> NeoBackpackTest.prepare(s.getPlayerList().getPlayers().getFirst())));
            add(8, () -> mc.gameMode.handlePickItemFromBlock(new BlockPos(0,100,0),false));
            add(8, () -> { server(s -> NeoBackpackTest.check(s.getPlayerList().getPlayers().getFirst(),Shape.SLAB)); mc.gameMode.handlePickItemFromBlock(new BlockPos(0,100,0),false); });
            add(3, () -> server(s -> NeoBackpackTest.check(s.getPlayerList().getPlayers().getFirst(),Shape.STAIR)));
        }
        add(20, () -> {
            command("fill -6 100 -2 6 106 84 minecraft:air"); command("fill -6 99 -2 6 99 84 minecraft:stone");
            server(s -> { for(int x=-4;x<=4;x++)if(Math.abs(x)>=2)for(int y=100;y<=104;y++){var pos=new BlockPos(x,y,0);if(x<0)s.overworld().setBlock(pos,Blocks.BLACKSTONE_SLAB.defaultBlockState(),3);else set(s.overworld(),pos,new Cells().place(51,"minecraft:blackstone").place(128,"minecraft:quartz_block"));} });
            mc.options.entityDistanceScaling().set(.5);mc.options.renderDistance().set(12);mc.options.fov().set(30);command("tp @a 0.5 100 80.5 180 0");
        });
        add(20, () -> { geometry(new BlockPos(3,102,0)); require(mc.player.distanceToSqr(Vec3.atCenterOf(new BlockPos(3,102,0)))>4096,"Inside old entity renderer cutoff"); screenshot("building_palette_neoforge_distance"); });
        add(10, () -> finish(true,"NeoForge graphical, packet, inventory, partial mining and terrain tests passed"));
    }
    private void finish(boolean passed,String message) {
        done=true; NeoInput.alt=false;
        try { Files.writeString(Path.of(System.getProperty("building_palette.test_result")),"{\"passed\":"+passed+",\"message\":\""+message.replace("\\","/").replace("\"","'")+"\"}\n"); }
        catch(Exception e){e.printStackTrace();}
        System.out.println("[Building Palette NeoForge Test] "+message);
        mc.stop();
    }
}
