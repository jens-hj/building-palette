package dev.jensh.buildingpalette.neoforge;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import dev.jensh.buildingpalette.MultipartBlock;
import dev.jensh.buildingpalette.MultipartEntity;
import dev.ryanhcode.sable.api.sublevel.KinematicContraption;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public final class CreateCollisions {
    private CreateCollisions() {}

    public static BlockState physicsState(KinematicContraption contraption, BlockGetter world, BlockPos pos, BlockState state) {
        // Create stores the captured block entity as NBT. Its virtual level can delegate
        // block entity lookups to an unrelated position in the parent world.
        if (contraption instanceof AbstractContraptionEntity entity) {
            var block = entity.getContraption().getBlocks().get(pos);
            if (block != null && block.nbt() != null) {
                var cells = block.nbt().getList("cells", Tag.TAG_STRING);
                if (cells.size() == 8) return state.setValue(MultipartBlock.OCCLUSION, MultipartEntity.readCells(cells).occupied());
            }
            return state;
        }
        return SableCollisions.physicsState(state, world, pos);
    }
}
