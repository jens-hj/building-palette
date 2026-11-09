package dev.jensh.buildingpalette.fabric;

import dev.jensh.buildingpalette.MultipartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.blockgetter.v2.RenderDataBlockEntity;

public final class FabricMultipartEntity extends MultipartEntity implements RenderDataBlockEntity {
    public FabricMultipartEntity(BlockPos pos, BlockState state) { super(pos, state); }
    @Override public Object getRenderData() { return cells(); }
}
