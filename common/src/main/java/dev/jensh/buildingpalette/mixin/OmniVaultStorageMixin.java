package dev.jensh.buildingpalette.mixin;

import com.nightbeam.omnivault.network.NetworkStorage;
import com.nightbeam.omnivault.storage.ItemKey;
import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.core.Shape;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** OmniVault's network stores "item x count", which cannot hold a shape's remainder. Material
 * entering a network is therefore kept as whole blocks plus fewer than eight corners, so every
 * shape of a material merges into one entry and no unit is rounded away. */
@Pseudo
@Mixin(targets = "com.nightbeam.omnivault.network.NetworkStorage", remap = false)
public abstract class OmniVaultStorageMixin {
    @Shadow @Final private List<NetworkStorage.InsertInterceptor> interceptors;
    @Unique private boolean palette$converting;

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true, require = 0)
    private void palette$wholeBlocks(ItemKey key, long amount, boolean simulate, CallbackInfoReturnable<Long> cir) {
        if (palette$converting || amount <= 0 || amount > 4096 || !PaletteState.ready) return;
        ItemStack view = key.view();
        var selection = Materials.get(view);
        if (selection == null || (selection.shape() == Shape.BLOCK && MaterialStacks.missing(view) == 0)) return;
        // A crafting job waiting for exactly this item takes it as it is.
        for (var interceptor : interceptors) if (interceptor.intercept(key, amount, true) > 0) return;
        ItemStack block = null, corner = null;
        for (ItemStack entry : Materials.palette(view)) {
            Shape shape = Materials.get(entry).shape();
            if (shape == Shape.BLOCK) block = ScrollPayload.reshape(view, entry, Shape.BLOCK.cost);
            if (shape == Shape.CORNER) corner = ScrollPayload.reshape(view, entry, Shape.CORNER.cost);
        }
        if (block == null || corner == null || block.isEmpty() || corner.isEmpty()) return;
        ItemStack incoming = view.copy();
        incoming.setCount((int)amount);
        NetworkStorage self = (NetworkStorage)(Object)this;
        ItemKey blockKey = ItemKey.of(block), cornerKey = ItemKey.of(corner);
        long loose = self.getCount(cornerKey), total = loose + MaterialStacks.units(incoming);
        long blocks = total / Shape.BLOCK.cost, corners = total % Shape.BLOCK.cost - loose;
        if (selection.shape() == Shape.CORNER && MaterialStacks.missing(view) == 0 && blocks == 0) return;
        palette$converting = true;
        try {
            boolean fits = (blocks == 0 || self.insert(blockKey, blocks, true) == blocks)
                && (corners <= 0 || self.insert(cornerKey, corners, true) == corners)
                && (corners >= 0 || self.extract(cornerKey, -corners, true) == -corners);
            if (fits && !simulate) {
                if (corners < 0) self.extract(cornerKey, -corners, false);
                if (blocks > 0) self.insert(blockKey, blocks, false);
                if (corners > 0) self.insert(cornerKey, corners, false);
            }
            cir.setReturnValue(fits ? amount : 0L);
        } finally {
            palette$converting = false;
        }
    }
}
