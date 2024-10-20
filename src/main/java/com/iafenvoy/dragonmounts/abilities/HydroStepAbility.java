package com.iafenvoy.dragonmounts.abilities;

import com.iafenvoy.dragonmounts.dragon.TameableDragon;
import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.Oxidizable;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class HydroStepAbility extends FootprintAbility implements Ability.Factory<HydroStepAbility> {
    public static final HydroStepAbility INSTANCE = new HydroStepAbility();
    public static final Codec<HydroStepAbility> CODEC = Codec.unit(INSTANCE);

    @Override
    protected void placeFootprint(TameableDragon dragon, BlockPos pos) {
        World world = dragon.getWorld();
        BlockPos groundPos = pos.down();
        BlockState steppingOn = world.getBlockState(groundPos);

        if(world instanceof ServerWorld serverWorld)
            serverWorld.spawnParticles(ParticleTypes.FALLING_WATER, pos.getX(), pos.getY(), pos.getZ(), 10, 0.25, 0, 0.25, 0);

        // moisten farmland
        // soak sponges
        // extinguish fire
        // magmablock -> blackstone
        // copper -> rust

        if (steppingOn.isOf(Blocks.FARMLAND)) {
            world.setBlockState(groundPos, steppingOn.with(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE));
            return;
        }

        if (steppingOn.isOf(Blocks.SPONGE)) {
            world.setBlockState(groundPos, Blocks.WET_SPONGE.getDefaultState());
            return;
        }

        if (steppingOn.isOf(Blocks.MAGMA_BLOCK)) {
            world.setBlockState(groundPos, Blocks.BLACKSTONE.getDefaultState());
            return;
        }

        Identifier steppingOnName = steppingOn.getBlock().getRegistryEntry().registryKey().getValue();
        if (steppingOnName.getNamespace().equals("minecraft") && steppingOnName.getPath().contains("copper")) {// yeah fuck that copper complex this game's got going on
            Oxidizable.getIncreasedOxidationBlock(steppingOn.getBlock()).ifPresent(b -> world.setBlockState(groundPos, b.getStateWithProperties(steppingOn)));
            return;
        }

        if (world.getBlockState(pos).isIn(BlockTags.FIRE))
            world.removeBlock(pos, false);
    }

    @Override
    protected float getFootprintChance(TameableDragon dragon) {
        return 1f; // guaranteed
    }

    @Override
    public HydroStepAbility create() {
        return this;
    }

    @Override
    public Identifier type() {
        return HYDRO_STEP;
    }
}
