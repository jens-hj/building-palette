package dev.jensh.buildingpalette.test;

import dev.jensh.buildingpalette.client.NeoMultipartModel;
import dev.jensh.buildingpalette.neoforge.NeoMultipartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.NeoForge;

public final class NeoClientTest extends PaletteClientTest {
    public static void install() { var test = new NeoClientTest(); NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> test.tick()); }
    @Override protected void checkLoading() {
        if (mc.screen instanceof net.neoforged.neoforge.client.gui.LoadingErrorScreen) throw new AssertionError("NeoForge loading errors; see client log");
    }
    @Override protected void geometry(BlockPos pos) {
        require(mc.level.getBlockEntity(pos) instanceof NeoMultipartEntity, "Wrong entity factory");
        ModelData data = mc.level.getModelData(pos);
        var cells = data.get(NeoMultipartEntity.CELLS);
        require(cells != null && cells.occupied() != 0, "Missing immutable model data");
        var state = mc.level.getBlockState(pos);
        var model = mc.getBlockRenderer().getBlockModel(state);
        require(model instanceof NeoMultipartModel, "Carrier model was not replaced");
        int quads = 0;
        for (var type : model.getRenderTypes(state, RandomSource.create(1), data)) quads += model.getQuads(state, null, RandomSource.create(1), data, type).size();
        require(quads > 0, "Empty terrain mesh");
        require(state.getRenderShape() == RenderShape.MODEL && mc.getBlockEntityRenderDispatcher().getRenderer(mc.level.getBlockEntity(pos)) == null, "Multipart must use chunk rendering");
    }
}
