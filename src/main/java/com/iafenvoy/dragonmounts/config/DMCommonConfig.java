package com.iafenvoy.dragonmounts.config;

import com.google.common.collect.ImmutableMap;
import com.iafenvoy.dragonmounts.DragonMounts;
import com.iafenvoy.dragonmounts.dragon.breed.DragonBreed;
import com.iafenvoy.jupiter.config.container.AutoInitConfigContainer;
import com.iafenvoy.jupiter.config.entry.BooleanEntry;
import com.iafenvoy.jupiter.config.entry.IntegerEntry;
import com.iafenvoy.jupiter.config.entry.MapDoubleEntry;
import com.iafenvoy.jupiter.config.entry.MapIntegerEntry;
import com.iafenvoy.jupiter.interfaces.IConfigEntry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.Map;

import static com.iafenvoy.dragonmounts.dragon.breed.DragonBreed.BuiltIn.*;

public class DMCommonConfig extends AutoInitConfigContainer {
    public static final String MOD_ID = DragonMounts.MOD_ID;
    public static final DMCommonConfig INSTANCE = new DMCommonConfig();
    public final Common COMMON = new Common();

    private DMCommonConfig() {
        super(Identifier.of(MOD_ID, "common"), "config.%s.common.title".formatted(MOD_ID), "./config/dragon-mounts/common.json");
    }

    public static class Common extends AutoInitConfigCategoryBase {
        public final IConfigEntry<Boolean> allowEggOverride = new BooleanEntry("config.%s.common.allowEggOverride".formatted(MOD_ID), true).json("allowEggOverride");
        public final IConfigEntry<Boolean> replenishEggs = new BooleanEntry("config.%s.common.replenishEggs".formatted(MOD_ID), true).json("replenishEggs");
        public final IConfigEntry<Boolean> updateHabitats = new BooleanEntry("config.%s.common.updateHabitats".formatted(MOD_ID), true).json("updateHabitats");
        public final IConfigEntry<Boolean> randomTickHatch = new BooleanEntry("config.%s.common.randomTickHatch".formatted(MOD_ID), true).json("randomTickHatch");
        public final IConfigEntry<Integer> maxMountPerDragon = new IntegerEntry("config.%s.common.maxMountPerDragon".formatted(MOD_ID), 2, 0, 10).json("maxMountPerDragon");
        //egg chance
        public final IConfigEntry<Map<String, Double>> eggGenerateChance = new MapDoubleEntry("config.%s.common.eggGenerateChance".formatted(MOD_ID), Map.of(
                AETHER.getValue().toString(), 0.15,
                FIRE.getValue().toString(), 0.075,
                FOREST.getValue().toString(), 0.3,
                GHOST.getValue().toString(), 0.095,
                ICE.getValue().toString(), 0.2,
                NETHER.getValue().toString(), 0.35,
                WATER.getValue().toString(), 0.175
        )).json("eggGenerateChance");
        //-1 = No Limit, 0 = Cannot breed, 2 = Can breed only two times
        public final IConfigEntry<Map<String, Integer>> reproduceLimit = new MapIntegerEntry("config.%s.common.reproduceLimit".formatted(MOD_ID), Util.make(() -> {
            ImmutableMap.Builder<String, Integer> builder = new ImmutableMap.Builder<>();
            for (RegistryKey<DragonBreed> key : BUILTIN) builder.put(key.getValue().toString(), -1);
            return builder.build();
        })).json("reproduceLimit");
        //Hatch time in ticks, default to 20*60*30=36000
        public final IConfigEntry<Map<String, Integer>> hatchTime = new MapIntegerEntry("config.%s.common.hatchTime".formatted(MOD_ID), Util.make(() -> {
            ImmutableMap.Builder<String, Integer> builder = new ImmutableMap.Builder<>();
            for (RegistryKey<DragonBreed> key : BUILTIN) builder.put(key.getValue().toString(), 36000);
            return builder.build();
        })).json("hatchTime");

        public Common() {
            super("common", "config.%s.common.name".formatted(MOD_ID));
        }

        public int getReproduceLimit(String type) {
            return this.reproduceLimit.getValue().getOrDefault(type, -1);
        }

        public int getHatchTime(String type) {
            return this.hatchTime.getValue().getOrDefault(type, 36000);
        }
    }
}
