package dev.jensh.buildingpalette.mixin.client;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.platform.ClientPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class PickBlockMixin {
    @Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
    private void palette$materialPick(CallbackInfo ci) {
        Minecraft mc = (Minecraft)(Object)this;
        if (mc.player == null || mc.level == null || mc.getConnection() == null || !(mc.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) return;
        // Creative Ctrl-pick copies block entity data; leave it to vanilla.
        if (mc.player.hasInfiniteMaterials() && Screen.hasControlDown()) return;
        var state = mc.level.getBlockState(hit.getBlockPos());
        if (!(mc.level.getBlockEntity(hit.getBlockPos()) instanceof MultipartEntity)
                && Materials.get(new ItemStack(state.getBlock().asItem())) == null) return;
        if (ClientPlatform.INSTANCE.sendPick(new PickPayload(hit.getBlockPos()))) ci.cancel();
    }
}
