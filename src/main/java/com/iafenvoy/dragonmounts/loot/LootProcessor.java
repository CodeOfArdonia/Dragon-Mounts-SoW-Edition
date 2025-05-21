package com.iafenvoy.dragonmounts.loot;

import com.iafenvoy.dragonmounts.config.DMCommonConfig;
import com.iafenvoy.dragonmounts.dragon.breed.DragonBreed;
import com.iafenvoy.dragonmounts.dragon.egg.HatchableEggBlock;
import com.iafenvoy.dragonmounts.registry.DMBlocks;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.loot.function.LootFunctionTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import java.util.List;

import static com.iafenvoy.dragonmounts.dragon.breed.DragonBreed.BuiltIn.*;

public class LootProcessor {
    private static final List<Target> BUILT_IN_CHANCES = List.of(
            new Target(AETHER, LootTables.SIMPLE_DUNGEON_CHEST),
            new Target(FIRE, LootTables.DESERT_PYRAMID_CHEST),
            new Target(FOREST, LootTables.JUNGLE_TEMPLE_CHEST),
            new Target(GHOST, LootTables.ABANDONED_MINESHAFT_CHEST),
            new Target(ICE, LootTables.IGLOO_CHEST_CHEST),
            new Target(NETHER, LootTables.BASTION_TREASURE_CHEST),
            new Target(WATER, LootTables.BURIED_TREASURE_CHEST));

    public static void init() {
        LootTableEvents.MODIFY.register((resourceManager, lootManager, identifier, builder, lootTableSource) -> {
            for (Target target : BUILT_IN_CHANCES.stream().filter(x -> x.target.equals(identifier)).toList())
                builder.pool(LootPool.builder().with(ItemEntry.builder(DMBlocks.EGG_BLOCK)).apply(new LootFunction() {
                    @Override
                    public LootFunctionType getType() {
                        return LootFunctionTypes.SET_NBT;
                    }

                    @Override
                    public ItemStack apply(ItemStack stack, LootContext lootContext) {
                        return HatchableEggBlock.Item.apply(stack, target.forBreed);
                    }
                }).conditionally(RandomChanceLootCondition.builder(target.getChance())));
        });
    }

    private record Target(RegistryKey<DragonBreed> forBreed, Identifier target) {
        public float getChance() {
            return DMCommonConfig.INSTANCE.COMMON.eggGenerateChance.getValue().getOrDefault(this.forBreed.getValue().toString(), 1.0).floatValue();
        }
    }
}
