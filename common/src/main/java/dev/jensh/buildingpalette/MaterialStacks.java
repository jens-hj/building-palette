package dev.jensh.buildingpalette;

import dev.jensh.buildingpalette.core.Balance;
import net.minecraft.world.item.ItemStack;

/** Native count = ceil(units / cost). Missing units mark the incomplete final piece.
 * Incomplete pieces are storage, never ingredients. All conversions use this service. */
public final class MaterialStacks {
    private MaterialStacks() {}
    public static int units(ItemStack stack) {
        var s = Materials.get(stack);
        return s == null ? 0 : Math.max(0, stack.getCount() * s.shape().cost - missing(stack));
    }
    public static int missing(ItemStack stack) { return stack.getOrDefault(BuildingPalette.MISSING.get(), 0); }
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
        result.setCount(balance.slots(s.shape()));
        if (balance.missing(s.shape()) == 0) result.remove(BuildingPalette.MISSING.get());
        else result.set(BuildingPalette.MISSING.get(), balance.missing(s.shape()));
        return result;
    }
    public static boolean sameMaterial(ItemStack a, ItemStack b) {
        var sa = Materials.get(a); var sb = Materials.get(b);
        if (sa == null || sb == null || sa.parent() != sb.parent()) return false;
        // Do not combine named/enchanted/custom stacks and lose their extra components.
        ItemStack aa = a.transmuteCopy(sa.parent(), 1), bb = b.transmuteCopy(sb.parent(), 1);
        for (var type : new net.minecraft.core.component.DataComponentType<?>[]{BuildingPalette.MISSING.get(), BuildingPalette.MATERIAL.get(), BuildingPalette.SHAPE.get()}) {
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
