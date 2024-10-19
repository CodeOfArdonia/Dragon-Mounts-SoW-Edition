package com.iafenvoy.dragonmounts.abilities;

import com.iafenvoy.dragonmounts.DragonMounts;
import com.iafenvoy.dragonmounts.dragon.TameableDragon;
import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class HotFeetAbility extends FootprintAbility implements Ability.Factory<HotFeetAbility> {
    public static final HotFeetAbility INSTANCE = new HotFeetAbility();
    public static final Codec<HotFeetAbility> CODEC = Codec.unit(INSTANCE);

    public static final TagKey<Block> BURNABLES_TAG = TagKey.of(RegistryKeys.BLOCK, Identifier.of(DragonMounts.MOD_ID, "hot_feet_burnables"));

    @Override
    protected void placeFootprint(TameableDragon dragon, BlockPos pos) {
        var level = dragon.getWorld();
        var steppingOn = level.getBlockState(pos);
        if (steppingOn.isIn(BURNABLES_TAG)) {
            level.removeBlock(pos, false);
            level.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, dragon.getSoundCategory(), 0.1f, 2f);
            ((ServerWorld) level).spawnParticles(ParticleTypes.LARGE_SMOKE, pos.getX(), pos.getY(), pos.getZ(), 0, 0, 1, 0, 0.05);
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
