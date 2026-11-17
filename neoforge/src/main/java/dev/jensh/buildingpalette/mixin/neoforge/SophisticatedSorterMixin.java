package dev.jensh.buildingpalette.mixin.neoforge;

import dev.jensh.buildingpalette.*;
import java.util.*;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

/** Only the sorter's temporary counts use material units. Keep its ordering, memory
 * slots, exclusions and accessibility rules, then write exact stacks back to storage. */
@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.util.InventorySorter", remap = false)
public abstract class SophisticatedSorterMixin {
    @Redirect(method = "sortHandler(Lnet/p3pp3rf1y/sophisticatedcore/inventory/InventoryHandler;Ljava/util/Comparator;Ljava/util/Set;Ljava/util/Set;Ljava/util/function/BiPredicate;)V", at = @At(value = "INVOKE", target =
        "Lnet/p3pp3rf1y/sophisticatedcore/util/InventoryHelper;getCompactedStacks(Lnet/p3pp3rf1y/sophisticatedcore/inventory/InventoryHandler;Ljava/util/Set;Z)Ljava/util/Map;"))
    private static Map<ItemStackKey, Integer> palette$balances(InventoryHandler handler, Set<Integer> skipped, boolean virtual) {
        Map<ItemStackKey, Integer> balances = new HashMap<>();
        for (int slot = 0; slot < handler.size(); slot++) {
            if (skipped.contains(slot)) continue;
            ItemStack stack = handler.getInternalStack(slot);
            if (stack.isEmpty()) continue;
            ItemStack key = stack.copy();
            boolean material = Materials.get(stack) != null;
            int amount = material ? MaterialStacks.units(stack) : stack.getCount();
            if (material) MaterialStacks.clearRemainder(key);
            key.setCount(1);
            balances.merge(ItemStackKey.of(key), amount, Math::addExact);
        }
        return balances;
    }

    @ModifyExpressionValue(method = "placeStack(Lnet/p3pp3rf1y/sophisticatedcore/inventory/ItemStackKey;IIZLnet/p3pp3rf1y/sophisticatedcore/util/InventorySorter$IStackLimitGetter;Lnet/p3pp3rf1y/sophisticatedcore/util/InventorySorter$ISlotStackGetter;Lnet/p3pp3rf1y/sophisticatedcore/util/InventorySorter$ISlotStackSetter;)I", at = @At(value = "INVOKE", target =
        "Lnet/p3pp3rf1y/sophisticatedcore/util/InventorySorter$IStackLimitGetter;getStackLimit(ILnet/minecraft/world/item/ItemStack;)I"))
    private static int palette$capacityUnits(int limit, @Local ItemStack copy) {
        return MaterialStacks.storageCapacity(copy, limit);
    }

    @Redirect(method = "placeStack(Lnet/p3pp3rf1y/sophisticatedcore/inventory/ItemStackKey;IIZLnet/p3pp3rf1y/sophisticatedcore/util/InventorySorter$IStackLimitGetter;Lnet/p3pp3rf1y/sophisticatedcore/util/InventorySorter$ISlotStackGetter;Lnet/p3pp3rf1y/sophisticatedcore/util/InventorySorter$ISlotStackSetter;)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;setCount(I)V"))
    private static void palette$exactStack(ItemStack copy, int amount) {
        if (Materials.get(copy) == null) copy.setCount(amount);
        else InventoryTransactions.assign(copy, MaterialStacks.withStoredUnits(copy, amount));
    }

    @ModifyArg(method = "sortHandler(Lnet/p3pp3rf1y/sophisticatedcore/inventory/InventoryHandler;Ljava/util/Comparator;Ljava/util/Set;Ljava/util/Set;Ljava/util/function/BiPredicate;)V", at = @At(value = "INVOKE", target = "Ljava/util/List;sort(Ljava/util/Comparator;)V"), index = 0)
    private static Comparator<Map.Entry<ItemStackKey, Integer>> palette$displayCounts(Comparator<Map.Entry<ItemStackKey, Integer>> comparator) {
        return (a, b) -> comparator.compare(palette$displayEntry(a), palette$displayEntry(b));
    }
    @Unique private static Map.Entry<ItemStackKey, Integer> palette$displayEntry(Map.Entry<ItemStackKey, Integer> entry) {
        var selection = Materials.get(entry.getKey().stack());
        if (selection == null) return entry;
        int units = entry.getValue(), cost = selection.shape().cost;
        return Map.entry(entry.getKey(), units / cost + (units % cost == 0 ? 0 : 1));
    }
}
