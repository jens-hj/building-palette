package dev.jensh.buildingpalette.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** The persistent codec's 99-item ceiling is separate from each item's runtime capacity. */
@Mixin(ItemStack.class)
public abstract class StackCodecMixin {
    // The codec lambda: Mojang's name on NeoForge, the intermediary name on Fabric.
    @ModifyConstant(method = {"lambda$static$3", "method_57371"}, remap = false, constant = @Constant(intValue = 99))
    private static int palette$serializedCapacity(int limit) { return 512; }
}
