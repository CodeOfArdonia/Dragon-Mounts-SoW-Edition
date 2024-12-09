package com.iafenvoy.dragonmounts.registry;

import com.iafenvoy.dragonmounts.DragonMounts;
import com.iafenvoy.dragonmounts.dragon.egg.HatchableEggBlock;
import com.iafenvoy.dragonmounts.dragon.egg.HatchableEggBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class DMBlocks {
    public static final HatchableEggBlock EGG_BLOCK = register("dragon_egg", new HatchableEggBlock(), HatchableEggBlock.Item::new);
    public static final BlockEntityType<HatchableEggBlockEntity> EGG_BLOCK_ENTITY = register("dragon_egg", BlockEntityType.Builder.create(HatchableEggBlockEntity::new, EGG_BLOCK).build(null));

    private static <T extends Block> T register(String id, T block, Function<T, BlockItem> itemFunction) {
        T b = Registry.register(Registries.BLOCK, new Identifier(DragonMounts.MOD_ID, id), block);
        Registry.register(Registries.ITEM, new Identifier(DragonMounts.MOD_ID, id), itemFunction.apply(block));
        return b;
    }

    private static <T extends BlockEntity> BlockEntityType<T> register(String id, BlockEntityType<T> blockEntity) {
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(DragonMounts.MOD_ID, id), blockEntity);
    }

    public static void init() {
    }
}