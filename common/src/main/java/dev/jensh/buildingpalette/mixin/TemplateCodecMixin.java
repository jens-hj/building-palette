package dev.jensh.buildingpalette.mixin;

import net.minecraft.world.item.ItemStackTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ItemStackTemplate.class)
public abstract class TemplateCodecMixin {
    @ModifyConstant(method = "lambda$static$0", constant = @Constant(intValue = 99))
    private static int palette$serializedCapacity(int limit) { return 512; }
}
