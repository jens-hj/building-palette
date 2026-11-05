package dev.jensh.buildingpalette.mixin;

import dev.jensh.buildingpalette.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceMixin extends net.minecraft.world.level.block.entity.BaseContainerBlockEntity {
    protected FurnaceMixin(net.minecraft.world.level.block.entity.BlockEntityType<?> type, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { super(type, pos, state); }
    @Inject(method = "getBurnDuration", at = @At("HEAD"), cancellable = true)
    private void palette$fuel(ServerLevel level, ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        var selection = Materials.get(stack);
        if (selection == null) return;
        if (MaterialStacks.available(stack) == 0) { cir.setReturnValue(0); return; }
        int base = ResolvableInt.getFromItem(new ItemStack(selection.parent()), DataComponents.COOKING_FUEL,
            CookingFuel::burnTime, getLootContext(level), 0);
        cir.setReturnValue(base * selection.shape().cost / 8);
    }
}
