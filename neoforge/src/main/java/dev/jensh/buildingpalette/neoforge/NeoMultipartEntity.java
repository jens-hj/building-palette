package dev.jensh.buildingpalette.neoforge;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.core.Cells;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.*;

public final class NeoMultipartEntity extends MultipartEntity {
    public static final ModelProperty<Cells> CELLS = new ModelProperty<>();
    public static final ModelProperty<BlockPos> POS = new ModelProperty<>();
    public NeoMultipartEntity(BlockPos pos, BlockState state) { super(pos, state); }
    @Override public void setCells(Cells value) {
        var before = getBlockState();
        int occupied = cells().occupied();
        super.setCells(value);
        // Edits to glass can leave all vanilla light properties unchanged.
        if (before == getBlockState() && occupied != value.occupied()) refreshSableCollision();
    }
    @Override public void refreshPropertiesOnLoad() {
        var before = getBlockState();
        super.refreshPropertiesOnLoad();
        if (before == getBlockState()) refreshSableCollision();
    }
    private void refreshSableCollision() {
        if (level instanceof ServerLevel server && ModList.get().isLoaded("sable"))
            SableCollisions.refresh(server, worldPosition, getBlockState());
    }
    @Override public ModelData getModelData() { return ModelData.builder().with(CELLS, cells()).with(POS, getBlockPos()).build(); }
    @Override public void onLoad() { super.onLoad(); LoadedParts.loaded(this); }
    @Override public void onChunkUnloaded() { super.onChunkUnloaded(); LoadedParts.unloaded(this); }
}
