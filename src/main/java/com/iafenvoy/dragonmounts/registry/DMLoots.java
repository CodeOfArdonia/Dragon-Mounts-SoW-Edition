package com.iafenvoy.dragonmounts.registry;

import com.iafenvoy.dragonmounts.DragonMounts;
import com.iafenvoy.dragonmounts.data.loot.conditions.RandomChanceByConfig;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class DMLoots {
    public static final LootConditionType RANDOM_CHANCE_CONFIG_CONDITION = register("random_chance_by_config", new LootConditionType(new RandomChanceByConfig.Serializer()));

    private static LootConditionType register(String id, LootConditionType type) {
        return Registry.register(Registries.LOOT_CONDITION_TYPE, new Identifier(DragonMounts.MOD_ID, id), type);
    }

    public static void init() {
    }
}
