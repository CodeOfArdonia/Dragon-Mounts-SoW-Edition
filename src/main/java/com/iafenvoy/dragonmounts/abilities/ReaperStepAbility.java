package com.iafenvoy.dragonmounts.abilities;

import com.iafenvoy.dragonmounts.DragonMounts;
import com.iafenvoy.dragonmounts.dragon.TameableDragon;
import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ReaperStepAbility extends FootprintAbility implements Ability.Factory<ReaperStepAbility> {
    public static final ReaperStepAbility INSTANCE = new ReaperStepAbility();
    public static final Codec<ReaperStepAbility> CODEC = Codec.unit(INSTANCE);

    public static final TagKey<Block> PLANT_DEATH_TAG = TagKey.of(RegistryKeys.BLOCK, Identifier.of(DragonMounts.MOD_ID, "reaper_plant_death"));
    public static final TagKey<Block> PLANT_DESTRUCTION_TAG = TagKey.of(RegistryKeys.BLOCK, Identifier.of(DragonMounts.MOD_ID, "reaper_plant_destruction"));
    public static final TagKey<Block> REAPER_TRANSFORM = TagKey.of(RegistryKeys.BLOCK, Identifier.of(DragonMounts.MOD_ID, "reaper_transform"));

    @Override
    protected void placeFootprint(TameableDragon dragon, BlockPos pos) {
        var level = dragon.getWorld();
        var steppingOn = level.getBlockState(pos);
        if (steppingOn.isIn(PLANT_DEATH_TAG)) {
            level.removeBlock(pos, false);
            level.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, dragon.getSoundCategory(), 0.1f, 2f);
            ((ServerWorld) level).spawnParticles(ParticleTypes.SOUL, pos.getX(), pos.getY(), pos.getZ(), 0, 0, 1, 0, 0.05);

            var bs = (dragon.getRandom().nextDouble() < 0.05 ? Blocks.WITHER_ROSE : Blocks.DEAD_BUSH).getDefaultState();
            level.setBlockState(pos, bs, Block.NOTIFY_ALL);
        } else if (steppingOn.isIn(PLANT_DESTRUCTION_TAG)) {
            level.breakBlock(pos, false);
            level.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, dragon.getSoundCategory(), 0.1f, 2f);
            ((ServerWorld) level).spawnParticles(ParticleTypes.SOUL, pos.getX(), pos.getY(), pos.getZ(), 0, 0, 1, 0, 0.05);

            var sticks = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.STICK));
            sticks.setPickupDelay(40);
            level.spawnEntity(sticks);
        } else if ((steppingOn = level.getBlockState(pos = pos.down())).isIn(REAPER_TRANSFORM)) // todo: this isn't very customizable...
        {
            if (steppingOn.isOf(Blocks.GRASS_BLOCK))
                destroyAndReplace(level, Blocks.DIRT.getDefaultState(), pos);
            else if (steppingOn.isIn(BlockTags.SAND))
                destroyAndReplace(level, Blocks.SOUL_SAND.getDefaultState(), pos);
            else if (steppingOn.isIn(BlockTags.DIRT))
                destroyAndReplace(level, Blocks.SOUL_SOIL.getDefaultState(), pos);

        }
    }

    @Override
    protected float getFootprintChance(TameableDragon dragon) {
        return 0.025f;
    }

    private static void destroyAndReplace(World level, BlockState state, BlockPos pos) {
        level.breakBlock(pos, false);
        level.setBlockState(pos, state, Block.NOTIFY_ALL);
    }

    @Override
    public ReaperStepAbility create() {
        return this;
    }

    @Override
    public Identifier type() {
        return REAPER_STEP;
    }
}
