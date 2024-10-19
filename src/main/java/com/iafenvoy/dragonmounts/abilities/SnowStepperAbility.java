package com.iafenvoy.dragonmounts.abilities;

import com.iafenvoy.dragonmounts.dragon.TameableDragon;
import com.mojang.serialization.Codec;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class SnowStepperAbility extends FootprintAbility implements Ability.Factory<SnowStepperAbility> {
    public static final SnowStepperAbility INSTANCE = new SnowStepperAbility();
    public static final Codec<SnowStepperAbility> CODEC = Codec.unit(INSTANCE);

    @Override
    protected void placeFootprint(TameableDragon dragon, BlockPos pos) {
        var level = dragon.getWorld();
        var state = Blocks.SNOW.getDefaultState();
        if (level.getBlockState(pos).isAir() && state.canPlaceAt(level, pos)) {
            level.setBlockState(pos, state);
            ((ServerWorld) level).spawnParticles(ParticleTypes.SNOWFLAKE,
                    pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    dragon.getRandom().nextInt(6) + 2,
                    0.5, 0.5, 0.5, 0);
        }
    }

    @Override
    protected float getFootprintChance(TameableDragon dragon) {
        var pos = dragon.getBlockPos();
        return dragon.getWorld().getBiome(pos).value().isCold(pos) ? 0.5f : 0;
    }

    @Override
    public SnowStepperAbility create() {
        return this;
    }

    @Override
    public Identifier type() {
        return SNOW_STEPPER;
    }
}
