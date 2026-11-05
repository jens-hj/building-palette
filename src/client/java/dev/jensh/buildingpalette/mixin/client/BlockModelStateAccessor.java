package dev.jensh.buildingpalette.mixin.client;

import java.util.List;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockModelRenderState.class)
public interface BlockModelStateAccessor {
    @Accessor("modelParts") List<BlockStateModelPart> palette$getParts();
    @Accessor("modelParts") void palette$setParts(List<BlockStateModelPart> parts);
}
