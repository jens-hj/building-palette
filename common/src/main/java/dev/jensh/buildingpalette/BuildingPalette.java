package dev.jensh.buildingpalette;

import com.mojang.serialization.Codec;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Loader-neutral content identities. Handles resolve only after registration. */
public final class BuildingPalette {
    public static final String ID = "building_palette";
    public static final Handle<DataComponentType<Integer>> MISSING = new Handle<>();
    public static final Handle<DataComponentType<String>> MATERIAL = new Handle<>();
    public static final Handle<DataComponentType<Integer>> SHAPE = new Handle<>();
    public static final Handle<Item> PIECE = new Handle<>();
    public static final Handle<MultipartBlock> MULTIPART = new Handle<>();
    public static final Handle<HostShapeBlock> HOST_SHAPE = new Handle<>();
    public static final Handle<BlockEntityType<MultipartEntity>> ENTITY = new Handle<>();
    public static final class Handle<T> implements Supplier<T> {
        private T value;
        public void bind(T value) { if (this.value != null) throw new IllegalStateException("Content registered twice"); this.value = value; }
        @Override public T get() { if (value == null) throw new IllegalStateException("Building Palette content accessed before registration"); return value; }
    }
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(ID, path); }
    public static void components(BiConsumer<Identifier, DataComponentType<?>> register) {
        MISSING.bind(DataComponentType.<Integer>builder().persistent(Codec.intRange(0, 7)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        MATERIAL.bind(DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
        SHAPE.bind(DataComponentType.<Integer>builder().persistent(Codec.intRange(0, 8)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        register.accept(id("missing_units"), MISSING.get()); register.accept(id("material"), MATERIAL.get()); register.accept(id("shape"), SHAPE.get());
    }
    public static void items(BiConsumer<Identifier, Item> register) {
        PIECE.bind(new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("piece")))));
        register.accept(id("piece"), PIECE.get());
    }
    public static void blocks(BiConsumer<Identifier, Block> register) {
        MULTIPART.bind(new MultipartBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id("multipart"))).strength(2.0f).dynamicShape()));
        register.accept(id("multipart"), MULTIPART.get());
        HOST_SHAPE.bind(new HostShapeBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id("host_shape"))).noCollision()));
        register.accept(id("host_shape"), HOST_SHAPE.get());
    }
}
