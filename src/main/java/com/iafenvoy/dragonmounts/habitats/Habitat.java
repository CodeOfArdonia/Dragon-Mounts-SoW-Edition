package com.iafenvoy.dragonmounts.habitats;

import com.iafenvoy.dragonmounts.DragonMounts;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public interface Habitat {
    Map<Identifier, Codec<? extends Habitat>> REGISTRY = new HashMap<>();

    Codec<Habitat> CODEC = Identifier.CODEC.dispatch(Habitat::type, REGISTRY::get);

    Identifier PICKY = reg("picky", PickyHabitat.CODEC);
    Identifier BIOMES = reg("biome", BiomeHabitat.CODEC);
    Identifier IN_FLUID = reg("in_fluid", FluidHabitat.CODEC);
    Identifier WORLD_HEIGHT = reg("world_height", HeightHabitat.CODEC);
    Identifier LIGHT = reg("light", LightHabitat.CODEC);
    Identifier NEARBY_BLOCKS = reg("nearby_blocks", NearbyBlocksHabitat.CODEC);
    Identifier DRAGON_BREATH = reg("dragon_breath", DragonBreathHabitat.CODEC);

    static Identifier register(Identifier name, Codec<? extends Habitat> codec) {
        REGISTRY.put(name, codec);
        return name;
    }

    private static Identifier reg(String name, Codec<? extends Habitat> codec) {
        return register(Identifier.of(DragonMounts.MOD_ID, name), codec);
    }

    static <T extends Habitat> RecordCodecBuilder<T, Integer> withPoints(int defaultTo, Function<T, Integer> getter) {
        return Codec.INT.optionalFieldOf("points", defaultTo).forGetter(getter);
    }

    static <T extends Habitat> RecordCodecBuilder<T, Float> withMultiplier(float defaultTo, Function<T, Float> getter) {
        return Codec.FLOAT.optionalFieldOf("point_multiplier", defaultTo).forGetter(getter);
    }

    int getHabitatPoints(World level, BlockPos pos);

    Identifier type();
}
