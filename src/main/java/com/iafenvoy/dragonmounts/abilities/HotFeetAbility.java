package com.iafenvoy.dragonmounts.abilities;

import com.iafenvoy.dragonmounts.DragonMounts;
import com.iafenvoy.dragonmounts.dragon.TameableDragonEntity;
import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class HotFeetAbility extends FootprintAbility implements Ability.Factory<HotFeetAbility> {
    public static final HotFeetAbility INSTANCE = new HotFeetAbility();
    public static final Codec<HotFeetAbility> CODEC = Codec.unit(INSTANCE);

    public static final TagKey<Block> BURNABLES_TAG = TagKey.of(RegistryKeys.BLOCK, Identifier.of(DragonMounts.MOD_ID, "hot_feet_burnables"));

    @Override
    protected void placeFootprint(TameableDragonEntity dragon, BlockPos pos) {
        World world = dragon.getWorld();
        BlockState steppingOn = world.getBlockState(pos);
        if (steppingOn.isIn(BURNABLES_TAG)&&world instanceof ServerWorld serverWorld) {
            world.removeBlock(pos, false);
            world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, dragon.getSoundCategory(), 0.1f, 2f);
            serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE, pos.getX(), pos.getY(), pos.getZ(), 0, 0, 1, 0, 0.05);
        }
    }

    @Override
    public HotFeetAbility create() {
        return this;
    }

    @Override
    public Identifier type() {
        return HOT_FEET;
    }
}
