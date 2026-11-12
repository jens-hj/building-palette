package dev.jensh.buildingpalette;

import dev.jensh.buildingpalette.core.Cells;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class MultipartEntity extends BlockEntity {
    private volatile Cells cells = new Cells();
    public MultipartEntity(BlockPos pos, BlockState state) { super(BuildingPalette.ENTITY.get(), pos, state); }
    public Cells cells() { return cells; }
    public void setCells(Cells value) {
        cells = value;
        setChanged();
        refreshProperties();
    }
    public void refreshProperties() {
        refreshProperties(true);
    }
    public void refreshPropertiesOnLoad() {
        refreshProperties(false);
    }
    private void refreshProperties(boolean notifyUnchanged) {
        if (level != null) {
            int light = 0, occlusion = 0;
            for (int i = 0; i < 8; i++) {
                String material = cells.at(i);
                if (material == null) continue;
                var state = MultipartBlock.material(material);
                if (state.isSolidRender(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) occlusion |= 1 << i;
                light = Math.max(light, state.getLightEmission());
            }
            var state = getBlockState().setValue(MultipartBlock.OPAQUE, occlusion == 255).setValue(MultipartBlock.LIGHT, light).setValue(MultipartBlock.OCCLUSION, occlusion);
            if (cells.occupied() == 255) state = state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, false);
            if (state == getBlockState() && !notifyUnchanged) return;
            if (state != getBlockState()) level.setBlock(worldPosition, state, 3);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("schema", 1);
        ListTag saved = new ListTag();
        for (String material : cells.copy()) saved.add(StringTag.valueOf(material == null ? "" : material));
        tag.put("cells", saved);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ListTag saved = tag.getList("cells", Tag.TAG_STRING);
        if (saved.size() != 8) { cells = new Cells(); return; }
        String[] values = new String[8];
        for (int i = 0; i < 8; i++) values[i] = saved.getString(i).isEmpty() ? null : saved.getString(i);
        cells = new Cells(values);
        // A block-entity-only update can leave its block state unchanged. Terrain
        // geometry must still be rebuilt from the new immutable render data.
        if (level != null && level.isClientSide()) {
            dev.jensh.buildingpalette.platform.Platform.INSTANCE.renderDataChanged(this);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
