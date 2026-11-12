package dev.jensh.buildingpalette.test;

import dev.jensh.buildingpalette.client.FabricMultipartModel;
import dev.jensh.buildingpalette.core.Cells;
import dev.jensh.buildingpalette.fabric.FabricMultipartEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;

public final class FabricClientTest extends PaletteClientTest implements ClientModInitializer {
    @Override public void onInitializeClient() {
        if (Boolean.getBoolean("building_palette.client_test")) ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }
    @Override protected void geometry(BlockPos pos) {
        require(mc.level.getBlockEntity(pos) instanceof FabricMultipartEntity, "Wrong entity factory");
        require(mc.level.getBlockEntityRenderData(pos) instanceof Cells cells && cells.occupied() != 0, "Missing immutable render data");
        var state = mc.level.getBlockState(pos);
        require(mc.getBlockRenderer().getBlockModel(state) instanceof FabricMultipartModel, "Carrier model was not replaced");
        require(state.getRenderShape() == RenderShape.MODEL && mc.getBlockEntityRenderDispatcher().getRenderer(mc.level.getBlockEntity(pos)) == null, "Multipart must use chunk rendering");
    }
}
