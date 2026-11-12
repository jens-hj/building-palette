package dev.jensh.buildingpalette;

import dev.jensh.buildingpalette.core.Shape;
import dev.jensh.buildingpalette.platform.Platform;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;

/** Resolve live shape maps after datapack reload; never cache registry or tag state. */
public final class Materials {
    public static final TagKey<Item> EXCLUDED = TagKey.create(Registries.ITEM, BuildingPalette.id("excluded"));
    public static final TagKey<Item> INCLUDED = TagKey.create(Registries.ITEM, BuildingPalette.id("included"));
    public record Selection(Item parent, Shape shape) {
        public String material() { return BuiltInRegistries.ITEM.getKey(parent).toString(); }
    }
    private Materials() {}
    public static Selection get(ItemStack stack) {
        if (!PaletteState.ready || stack.isEmpty()) return null;
        if (stack.getItem() == BuildingPalette.PIECE.get()) {
            String key = stack.get(BuildingPalette.MATERIAL.get());
            ResourceLocation id = key == null ? null : ResourceLocation.tryParse(key);
            Item parent = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
            int shape = stack.getOrDefault(BuildingPalette.SHAPE.get(), Shape.CORNER.ordinal());
            if (shape < 0 || shape >= Shape.values().length) return null;
            return parent instanceof BlockItem && eligible(parent) ? new Selection(parent, Shape.values()[shape]) : null;
        }
        Item item = stack.getItem();
        Item parent = Platform.INSTANCE.parent(item);
        if (parent == null) parent = item;
        if (parent == item && item instanceof BlockItem b && (b.getBlock() instanceof FenceBlock || b.getBlock() instanceof WallBlock)) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            String stem = key.getPath().replaceFirst("_(fence|wall)$", "");
            for (String suffix : new String[]{"_planks", "", "s", "_block"}) {
                Item candidate = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(key.getNamespace(), stem + suffix));
                if (candidate != null && candidate != Items.AIR && eligible(candidate)) { parent = candidate; break; }
            }
        }
        if (!eligible(parent) || !(item instanceof BlockItem blockItem)) return null;
        Shape shape = classify(blockItem.getBlock());
        if (shape == Shape.BLOCK && item != parent) return null;
        return new Selection(parent, shape);
    }
    private static boolean eligible(Item parent) {
        if (!(parent instanceof BlockItem b) || b.getBlock() instanceof EntityBlock || classify(b.getBlock()) != Shape.BLOCK
                || parent.builtInRegistryHolder().is(EXCLUDED)) return false;
        return parent.builtInRegistryHolder().is(INCLUDED) || Platform.INSTANCE.shapes(parent).stream().anyMatch(item ->
            item instanceof BlockItem bi && (bi.getBlock() instanceof SlabBlock || bi.getBlock() instanceof StairBlock
                || bi.getBlock() instanceof FenceBlock || bi.getBlock() instanceof WallBlock));
    }
    public static Shape classify(Block block) {
        String name = block.getClass().getSimpleName();
        if (name.contains("VerticalSlab")) return Shape.VERTICAL_SLAB;
        if (name.contains("VerticalStep")) return Shape.VERTICAL_STEP;
        if (name.contains("StepBlock")) return Shape.STEP;
        if (block instanceof SlabBlock) return Shape.SLAB;
        if (block instanceof StairBlock) return Shape.STAIR;
        if (block instanceof FenceBlock) return Shape.FENCE;
        if (block instanceof WallBlock) return Shape.WALL;
        return Shape.BLOCK;
    }
    public static List<ItemStack> palette(ItemStack source) {
        Selection selection = get(source);
        if (selection == null) return List.of();
        List<ItemStack> result = new ArrayList<>();
        List<Item> nativeShapes = new ArrayList<>(Platform.INSTANCE.shapes(selection.parent()));
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(selection.parent());
        String stem = key.getPath().replaceFirst("_planks$", "");
        for (String suffix : new String[]{"_fence", "_wall"}) {
            Item candidate = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(key.getNamespace(), stem + suffix));
            if (candidate != null && candidate != Items.AIR && !nativeShapes.contains(candidate)) nativeShapes.add(candidate);
        }
        for (Shape shape : Shape.values()) {
            Item nativeItem = shape == Shape.BLOCK ? selection.parent() : nativeShapes.stream()
                .filter(i -> i instanceof BlockItem bi && classify(bi.getBlock()) == shape).findFirst().orElse(null);
            if (nativeItem == null && !shape.isMultipart()) continue;
            ItemStack stack = new ItemStack(nativeItem == null ? BuildingPalette.PIECE.get() : nativeItem);
            if (nativeItem == null) {
                stack.set(BuildingPalette.MATERIAL.get(), selection.material());
                stack.set(BuildingPalette.SHAPE.get(), shape.ordinal());
            }
            result.add(stack);
        }
        return result;
    }
    public static ItemStack piece(String material, Shape shape, int units) {
        ItemStack seed = new ItemStack(BuildingPalette.PIECE.get());
        seed.set(BuildingPalette.MATERIAL.get(), material);
        seed.set(BuildingPalette.SHAPE.get(), shape.ordinal());
        if (get(seed) == null) return ItemStack.EMPTY;
        ItemStack target = palette(seed).stream().filter(s -> get(s).shape() == shape).findFirst().orElse(seed);
        return MaterialStacks.withUnits(target, units);
    }
}
