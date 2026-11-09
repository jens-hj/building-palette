package dev.jensh.buildingpalette.test;

import com.tiviacz.travelersbackpack.attachment.AttachmentUtils;
import com.tiviacz.travelersbackpack.init.ModItems;
import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.core.Shape;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Runs only with the optional NeoForge backpack fixture installed. */
public final class NeoBackpackTest {
    public static void prepare(ServerPlayer player) {
        player.getInventory().clearContent();
        for (int i=0;i<9;i++) player.getInventory().setItem(i,new ItemStack(Items.DIRT,64));
        player.getInventory().setSelectedSlot(0);
        AttachmentUtils.equipBackpack(player,ModItems.STANDARD_TRAVELERS_BACKPACK.get().getDefaultInstance());
        if (!AttachmentUtils.isWearingBackpack(player)) throw new AssertionError("Backpack not equipped");
        var storage=AttachmentUtils.getBackpackWrapper(player,AttachmentUtils.STORAGE_ONLY.get()).getStorageForInputOutput();
        var stack=Materials.piece("minecraft:stone",Shape.SLAB,65);
        if (MaterialStacks.units(ItemResource.of(stack).toStack(stack.getCount()))!=65) throw new AssertionError("Resource normalization lost remainder");
        try(var tx=Transaction.openRoot()) {
            if(storage.insert(0,ItemResource.of(stack),stack.getCount(),tx)!=stack.getCount()) throw new AssertionError("Backpack refused fractional stack");
            tx.commit();
        }
        // Aborted transactions must not alter either the resource or its amount.
        try(var tx=Transaction.openRoot()) { storage.extract(0,ItemResource.of(stack),stack.getCount(),tx); }
        if(storage.getAmountAsLong(0)!=stack.getCount()) throw new AssertionError("Backpack transaction rollback failed");
        var corners=Materials.piece("minecraft:oak_planks",Shape.CORNER,512);
        try(var tx=Transaction.openRoot()) {
            if(storage.insert(1,ItemResource.of(corners),corners.getCount(),tx)!=512) throw new AssertionError("Backpack cannot hold full material balance");
            tx.commit();
        }
        player.inventoryMenu.broadcastChanges();
    }
    public static void check(ServerPlayer player,Shape shape) {
        var stack=player.getMainHandItem(); var selected=Materials.get(stack);
        if(selected==null || !selected.material().equals("minecraft:stone") || selected.shape()!=shape || MaterialStacks.units(stack)!=65) throw new AssertionError("Backpack pick lost shape or remainder: expected="+shape+", actual="+selected+", units="+MaterialStacks.units(stack)+", held="+stack);
        var storage=AttachmentUtils.getBackpackWrapper(player,AttachmentUtils.STORAGE_ONLY.get()).getStorageForInputOutput();
        if(!storage.getResource(0).toStack(1).is(Items.DIRT)||storage.getAmountAsLong(0)!=64) throw new AssertionError("Backpack swap lost displaced stack");
        if(MaterialStacks.units(storage.getResource(1).toStack((int)storage.getAmountAsLong(1)))!=512) throw new AssertionError("Backpack pick changed unrelated material");
    }
}
