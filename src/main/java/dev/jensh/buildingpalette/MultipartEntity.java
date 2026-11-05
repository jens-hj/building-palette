package dev.jensh.buildingpalette;

import com.mojang.serialization.Codec;
import dev.jensh.buildingpalette.core.Cells;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class MultipartEntity extends BlockEntity {
    private Cells cells = new Cells();
    public MultipartEntity(BlockPos pos, BlockState state) { super(BuildingPalette.ENTITY, pos, state); }
    public Cells cells() { return cells; }
    public void setCells(Cells value) {
        cells = value;
        setChanged();
        if (level != null) {
            boolean opaque = cells.occupied() == 255;
            int light = 0;
            for (String material : cells.copy()) if (material != null) {
                var state = MultipartBlock.material(material);
                opaque &= state.isSolidRender();
                light = Math.max(light, state.getLightEmission());
            }
            var state = getBlockState().setValue(MultipartBlock.OPAQUE, opaque).setValue(MultipartBlock.LIGHT, light);
            if (cells.occupied() == 255) state = state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, false);
            if (state != getBlockState()) level.setBlock(worldPosition, state, 3);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("schema", 1);
        output.store("cells", Codec.STRING.listOf(), Arrays.stream(cells.copy()).map(s -> s == null ? "" : s).toList());
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        List<String> saved = input.read("cells", Codec.STRING.listOf()).orElse(List.of());
        if (saved.size() != 8) { cells = new Cells(); return; }
        String[] values = saved.stream().map(s -> s.isEmpty() ? null : s).toArray(String[]::new);
        cells = new Cells(values);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
