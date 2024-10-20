package com.iafenvoy.dragonmounts.habitats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public record LightHabitat(int points, boolean below, int light) implements Habitat {
    public static final Codec<LightHabitat> CODEC = RecordCodecBuilder.create(func -> func.group(
            Habitat.withPoints(3, LightHabitat::points),
            Codec.BOOL.optionalFieldOf("below", false).forGetter(LightHabitat::below),
            Codec.INT.fieldOf("light").forGetter(LightHabitat::light)
    ).apply(func, LightHabitat::new));

    @Override
    public int getHabitatPoints(World world, BlockPos pos) {
        int lightEmission = world.getLuminance(pos);
        return (this.below ? lightEmission < this.light : lightEmission > this.light) ? this.points : 0;
    }

    @Override
    public Identifier type() {
        return Habitat.LIGHT;
    }
}
