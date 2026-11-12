package dev.jensh.buildingpalette.mixin;

import dev.jensh.buildingpalette.*;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = ItemStack.class, priority = 900)
public abstract class ItemStackMixin implements RawStack {
    @Shadow private int count;
    @Override public void palette$rawCount(int count) { this.count = count; }
    @Unique private static final String PALETTE$SELF = ItemStack.class.getName();

    @Inject(method = "validateStrict", at = @At("RETURN"), cancellable = true)
    private static void palette$validBalance(ItemStack stack, CallbackInfoReturnable<com.mojang.serialization.DataResult<ItemStack>> cir) {
        var selection = Materials.get(stack);
        if (selection == null) return;
        int missing = MaterialStacks.missing(stack);
        long units = MaterialStacks.units(stack);
        if (missing < 0 || missing >= selection.shape().cost || units <= 0 || units > 512)
            cir.setReturnValue(com.mojang.serialization.DataResult.error(() -> "Invalid building material balance"));
    }
    /** Stacks saved before the seal existed are intact by definition: seal them as they are read. */
    @Inject(method = "<init>(Lnet/minecraft/core/Holder;ILnet/minecraft/core/component/DataComponentPatch;)V", at = @At("RETURN"))
    private void palette$sealOldStacks(net.minecraft.core.Holder<Item> item, int count, net.minecraft.core.component.DataComponentPatch patch, CallbackInfo ci) {
        if (!PaletteState.ready || patch.isEmpty() || count <= 0) return;
        ItemStack self = (ItemStack)(Object)this;
        if (MaterialStacks.missing(self) > 0 && !self.has(BuildingPalette.MISSING_FOR.get())) self.set(BuildingPalette.MISSING_FOR.get(), count);
    }
    /** A smaller copy of a stack with a remainder is either a split or a description.
     * Vanilla only splits this way (dropper, item frame, ...): the copy is whole pieces and the
     * remainder stays behind. Other mods also use it to describe a stack ("this item, N times"),
     * where dropping the remainder would create material, so their copies keep it, sealed. */
    @Inject(method = "copyWithCount", at = @At("HEAD"), cancellable = true)
    private void palette$splitRemainder(int count, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack self = (ItemStack)(Object)this;
        if (!PaletteState.ready || count >= this.count || count <= 0 || MaterialStacks.missing(self) == 0 || Materials.get(self) == null) return;
        ItemStack copy = self.copy();
        ((RawStack)(Object)copy).palette$rawCount(count);
        String caller = StackWalker.getInstance().walk(frames -> frames.map(StackWalker.StackFrame::getClassName)
            .filter(name -> !name.equals(PALETTE$SELF)).findFirst().orElse(""));
        if (caller.startsWith("net.minecraft.")) MaterialStacks.clearRemainder(copy);
        else copy.set(BuildingPalette.MISSING_FOR.get(), MaterialStacks.sealed(self));
        cir.setReturnValue(copy);
    }
    /** Taking part of a stack takes whole pieces; the remainder stays with what is left. */
    @Inject(method = "split", at = @At("HEAD"))
    private void palette$exactBeforeSplit(int amount, CallbackInfoReturnable<ItemStack> cir) {
        if (PaletteState.ready) MaterialStacks.normalize((ItemStack)(Object)this);
    }
    @Inject(method = "split", at = @At("RETURN"))
    private void palette$wholePieces(int amount, CallbackInfoReturnable<ItemStack> cir) {
        if (PaletteState.ready && this.count > 0 && MaterialStacks.missing(cir.getReturnValue()) != 0) MaterialStacks.clearRemainder(cir.getReturnValue());
    }
    /** Shrinking consumes whole pieces, so the remainder is resealed for the smaller count. */
    @Inject(method = "setCount", at = @At("HEAD"), cancellable = true)
    private void palette$keepRemainder(int count, CallbackInfo ci) {
        if (count >= this.count || count <= 0 || !PaletteState.ready) return;
        ItemStack self = (ItemStack)(Object)this;
        if (MaterialStacks.missing(self) == 0) return;
        var selection = Materials.get(self);
        if (selection == null) return;
        if (MaterialStacks.intact(self)) { self.set(BuildingPalette.MISSING_FOR.get(), count); return; }
        // Foreign code merged or split this stack: settle its remainders before shrinking.
        int units = MaterialStacks.units(self) - (this.count - count) * selection.shape().cost;
        MaterialStacks.clearRemainder(self);
        if (units <= 0) this.count = 0;
        else {
            ItemStack exact = MaterialStacks.withUnits(self, Math.min(units, 512));
            this.count = exact.getCount();
            if (MaterialStacks.missing(exact) != 0) {
                self.set(BuildingPalette.MISSING.get(), MaterialStacks.missing(exact));
                self.set(BuildingPalette.MISSING_FOR.get(), this.count);
            }
        }
        ci.cancel();
    }
    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    private void palette$pieceName(CallbackInfoReturnable<net.minecraft.network.chat.Component> cir) {
        ItemStack self = (ItemStack)(Object)this;
        var selection = Materials.get(self);
        if (selection != null && self.getItem() == BuildingPalette.PIECE.get()) {
            cir.setReturnValue(net.minecraft.network.chat.Component.translatable("item.building_palette.piece_name",
                new ItemStack(selection.parent()).getHoverName(), net.minecraft.network.chat.Component.translatable("shape.building_palette." + selection.shape().name().toLowerCase(java.util.Locale.ROOT))));
        }
    }
}
