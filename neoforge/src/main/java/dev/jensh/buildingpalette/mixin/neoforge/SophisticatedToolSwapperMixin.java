package dev.jensh.buildingpalette.mixin.neoforge;

import dev.jensh.buildingpalette.MultipartBlock;
import dev.jensh.buildingpalette.MultipartEntity;
import dev.jensh.buildingpalette.Placement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Resolve once using the real player's aim, before the swapper evaluates its
 * inventory with a fake player. This also keys its last-mined cache by material. */
@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedbackpacks.upgrades.toolswapper.ToolSwapperUpgradeWrapper", remap = false)
public abstract class SophisticatedToolSwapperMixin {
    @Redirect(method = "onBlockClick", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState palette$targetMaterial(Level level, BlockPos pos, Player player, BlockPos clickedPos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof MultipartBlock && level.getBlockEntity(pos) instanceof MultipartEntity entity) {
            var hit = Placement.pick(player);
            if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)) {
                String material = entity.cells().at(Placement.hitCell(hit, false));
                if (material != null) return MultipartBlock.material(material);
            }
        }
        return state;
    }
}
