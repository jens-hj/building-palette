package dev.jensh.buildingpalette;

import dev.jensh.buildingpalette.core.Balance;
import net.minecraft.world.item.ItemStack;

/** Native count = ceil(units / cost). Missing units mark the incomplete final piece.
 * Incomplete pieces are storage, never ingredients. All conversions use this service.
 *
 * <p>The remainder is sealed with the count it was written for. Storage mods describe a stack as
 * "this item, N times" and later rebuild stacks of any size from that description; a remainder
 * that silently applied once to whatever count came back would create material. So every
 * {@code sealed} items carry one remainder: stacks this mod writes always have count == sealed
 * and are exact, and anything foreign code splits or merges can only round down. */
public final class MaterialStacks {
    private MaterialStacks() {}
    public static int units(ItemStack stack) {
        var s = Materials.get(stack);
        return s == null ? 0 : Math.max(0, stack.getCount() * s.shape().cost - deficit(stack));
    }
    public static int missing(ItemStack stack) { return stack.getOrDefault(BuildingPalette.MISSING.get(), 0); }
    /** Count the remainder belongs to. Stacks from before the seal existed count as intact. */
    public static int sealed(ItemStack stack) { return Math.max(1, stack.getOrDefault(BuildingPalette.MISSING_FOR.get(), stack.getCount())); }
    public static boolean intact(ItemStack stack) { return missing(stack) == 0 || sealed(stack) == stack.getCount(); }
    private static int deficit(ItemStack stack) {
        int missing = missing(stack), sealed = sealed(stack);
        return missing == 0 ? 0 : (stack.getCount() + sealed - 1) / sealed * missing;
    }
    public static void clearRemainder(ItemStack stack) {
        stack.remove(BuildingPalette.MISSING.get()); stack.remove(BuildingPalette.MISSING_FOR.get());
    }
    /** Rewrites a stack foreign code split or merged into the exact form, keeping its units. */
    public static void normalize(ItemStack stack) {
        if (intact(stack) || Materials.get(stack) == null) return;
        int units = units(stack);
        if (units > Balance.CAPACITY) return; // an oversized merge stays conservative
        InventoryTransactions.assign(stack, withUnits(stack, units));
    }
    public static int available(ItemStack stack) {
        var s = Materials.get(stack);
        return s == null ? stack.getCount() : units(stack) / s.shape().cost;
    }
    public static ItemStack withUnits(ItemStack target, int units) {
        if (units == 0) return ItemStack.EMPTY;
        var s = Materials.get(target);
        if (s == null) throw new IllegalArgumentException("Not a building material");
        Balance balance = new Balance(units);
        ItemStack result = target.copy();
        if (result.getItem() == BuildingPalette.PIECE.get()) {
            var fuel = new ItemStack(s.parent()).get(net.minecraft.core.component.DataComponents.COOKING_FUEL);
            if (fuel != null) result.set(net.minecraft.core.component.DataComponents.COOKING_FUEL, fuel);
        }
        clearRemainder(result);
        result.setCount(balance.slots(s.shape()));
        if (balance.missing(s.shape()) != 0) {
            result.set(BuildingPalette.MISSING.get(), balance.missing(s.shape()));
            result.set(BuildingPalette.MISSING_FOR.get(), result.getCount());
        }
        return result;
    }
    public static boolean sameMaterial(ItemStack a, ItemStack b) {
        var sa = Materials.get(a); var sb = Materials.get(b);
        if (sa == null || sb == null || sa.parent() != sb.parent()) return false;
        // Do not combine named/enchanted/custom stacks and lose their extra components.
        ItemStack aa = a.transmuteCopy(sa.parent(), 1), bb = b.transmuteCopy(sb.parent(), 1);
        for (var type : new net.minecraft.core.component.DataComponentType<?>[]{BuildingPalette.MISSING.get(), BuildingPalette.MISSING_FOR.get(), BuildingPalette.MATERIAL.get(), BuildingPalette.SHAPE.get()}) {
            aa.remove(type); bb.remove(type);
        }
        return aa.getComponents().equals(bb.getComponents());
    }
    public static int capacity(ItemStack stack) {
        var selection = Materials.get(stack);
        return selection == null ? stack.getMaxStackSize() : (Balance.CAPACITY + missing(stack)) / selection.shape().cost;
    }
    public static void spend(ItemStack stack, int pieces) {
        var selection = Materials.get(stack);
        if (selection == null || pieces < 0 || pieces > available(stack)) throw new IllegalArgumentException("Insufficient material");
        stack.shrink(pieces);
    }
}
