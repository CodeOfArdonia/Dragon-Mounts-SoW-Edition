package com.iafenvoy.dragonmounts.data.loot.conditions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.iafenvoy.dragonmounts.DMLConfig;
import com.iafenvoy.dragonmounts.registry.DMLoots;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.loot.context.LootContext;
import net.minecraft.util.JsonHelper;

public class RandomChanceByConfig implements LootCondition {
    private final String configTargetID;

    public RandomChanceByConfig(String forTarget) {
        this.configTargetID = forTarget;
    }

    @Override
    public LootConditionType getType() {
        return DMLoots.RANDOM_CHANCE_CONFIG_CONDITION;
    }

    @Override
    public boolean test(LootContext lootContext) {
        if (!DMLConfig.useLootTables()) return false;

        // non-existing config targets fail silently with probability -1f
        return lootContext.getRandom().nextFloat() < DMLConfig.getEggChanceFor(this.configTargetID);
    }

    public static class Serializer implements net.minecraft.util.JsonSerializer<RandomChanceByConfig> {
        @Override
        public void toJson(JsonObject json, RandomChanceByConfig value, JsonSerializationContext context) {
            json.addProperty("config_chance_target", value.configTargetID);
        }

        @Override
        public RandomChanceByConfig fromJson(JsonObject json, JsonDeserializationContext context) {
            return new RandomChanceByConfig(JsonHelper.getString(json, "config_chance_target"));
        }
    }
}
