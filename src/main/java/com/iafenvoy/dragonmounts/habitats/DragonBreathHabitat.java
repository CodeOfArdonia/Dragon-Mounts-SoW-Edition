package com.iafenvoy.dragonmounts.habitats;

import com.mojang.serialization.Codec;
import net.minecraft.entity.EntityType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public enum DragonBreathHabitat implements Habitat {
    INSTANCE;

    public static final Codec<DragonBreathHabitat> CODEC = Codec.unit(INSTANCE);

    @Override
    public int getHabitatPoints(World level, BlockPos pos) {
        return !level.getEntitiesByType(EntityType.AREA_EFFECT_CLOUD,
                new Box(pos),
                c -> c.getParticleType() == ParticleTypes.DRAGON_BREATH).isEmpty() ? 10 : 0;
    }

    @Override
    public Identifier type() {
        return Habitat.DRAGON_BREATH;
    }
}
