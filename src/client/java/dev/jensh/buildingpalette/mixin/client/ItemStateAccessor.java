package dev.jensh.buildingpalette.mixin.client;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.class)
public interface ItemStateAccessor {
    @Accessor("activeLayerCount") int palette$getCount();
    @Accessor("layers") ItemStackRenderState.LayerRenderState[] palette$getLayers();
}
