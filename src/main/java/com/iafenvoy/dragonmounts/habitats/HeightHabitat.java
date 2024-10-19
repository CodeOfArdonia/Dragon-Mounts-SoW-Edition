package com.iafenvoy.dragonmounts.habitats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public record HeightHabitat(int points, boolean below, int height) implements Habitat {
    public static final Codec<HeightHabitat> CODEC = RecordCodecBuilder.create(func -> func.group(
            Habitat.withPoints(3, HeightHabitat::points),
            Codec.BOOL.optionalFieldOf("below", false).forGetter(HeightHabitat::below),
            Codec.INT.fieldOf("height").forGetter(HeightHabitat::height)
    ).apply(func, HeightHabitat::new));

    @Override
    public int getHabitatPoints(World level, BlockPos pos) {
        int y = pos.getY();
        int max = this.height;
        return (this.below ? (y < max && !level.isSkyVisible(pos)) : y > max) ? this.points : 0;
    }

    @Override
    public Identifier type() {
        return Habitat.WORLD_HEIGHT;
    }
}
