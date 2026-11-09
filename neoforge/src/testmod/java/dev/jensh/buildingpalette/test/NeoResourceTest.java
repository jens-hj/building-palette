package dev.jensh.buildingpalette.test;

import dev.jensh.buildingpalette.*;
import dev.jensh.buildingpalette.core.Shape;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.transfer.item.ItemResource;

public final class NeoResourceTest {
    public static void verify(GameTestHelper test) {
        for (var shape:Shape.values()) for (int units=1;units<=512;units++) {
            var stack=Materials.piece("minecraft:stone",shape,units);
            var resource=ItemResource.of(stack);
            var restored=resource.toStack(stack.getCount());
            test.assertTrue(MaterialStacks.units(restored)==units,"NeoForge resource conversion changed material balance: "+shape+"/"+units);
            test.assertTrue(Materials.get(restored).shape()==shape,"Resource lost selected shape");
            test.assertTrue(resource.getMaxStackSize()==MaterialStacks.capacity(stack),"Resource capacity differs from native stack");
        }
        test.succeed();
    }
}
