package dev.jensh.buildingpalette.mixin;

import dev.jensh.buildingpalette.Materials;
import dev.jensh.buildingpalette.MaterialStacks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/** Native slab doubling still spends its second slab under material accounting. */
@Mixin(value = BlockItem.class, priority = 900)
public abstract class NativeConsumptionMixin {
    @WrapMethod(method = "place")
    private InteractionResult palette$consumeNativePiece(BlockPlaceContext context, Operation<InteractionResult> original) {
        ItemStack stack = context.getItemInHand();
        boolean managed = Materials.get(stack) != null;
        if (managed && MaterialStacks.available(stack) == 0 && (context.getPlayer() == null || !context.getPlayer().hasInfiniteMaterials())) return InteractionResult.FAIL;
        int before = stack.getCount();
        InteractionResult result = original.call(context);
        if (managed && result.consumesAction() && stack.getCount() == before) stack.consume(1, context.getPlayer());
        return result;
    }
}
