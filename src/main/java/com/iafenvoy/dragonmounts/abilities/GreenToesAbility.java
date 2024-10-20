package com.iafenvoy.dragonmounts.abilities;

import com.iafenvoy.dragonmounts.dragon.TameableDragon;
import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Fertilizable;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;

public class GreenToesAbility extends FootprintAbility implements Ability.Factory<GreenToesAbility> {
    public static final GreenToesAbility INSTANCE = new GreenToesAbility();
    public static final Codec<GreenToesAbility> CODEC = Codec.unit(INSTANCE);

    protected GreenToesAbility() {
    }

    // grow mushrooms and plants
    @Override
    protected void placeFootprint(TameableDragon dragon, BlockPos pos) {
        World world = dragon.getWorld();
        BlockPos groundPos = pos.down();
        BlockState steppingOn = world.getBlockState(groundPos);
        BlockState steppingOver = world.getBlockState(pos);

        if (steppingOn.isOf(Blocks.DIRT)) {// regrow grass on dirt
            world.setBlockState(groundPos, Blocks.GRASS_BLOCK.getDefaultState());
            world.syncWorldEvent(WorldEvents.BONE_MEAL_USED, groundPos, 2);
            return;
        }

        if (steppingOver.isAir()) {// manually place flowers, mushrooms, etc.
            BlockState placing = null;

            if (steppingOn.isIn(BlockTags.MUSHROOM_GROW_BLOCK))
                placing = (world.getRandom().nextBoolean() ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).getDefaultState();
            else if (steppingOn.isIn(BlockTags.DIRT) && !steppingOn.isOf(Blocks.MOSS_BLOCK)) {// different from the actual dirt block, could be grass or podzol.
                // while grass blocks etc. do have defined bone meal behavior, I think our own is more viable.
                placing = world.getRegistryManager().get(RegistryKeys.BLOCK)
                        .getEntryList(BlockTags.SMALL_FLOWERS)
                        .flatMap(tag -> tag.getRandom(dragon.getRandom()))
                        .map(RegistryEntry::value)
                        .filter(b -> b != Blocks.WITHER_ROSE)
                        .orElse(Blocks.DANDELION)
                        .getDefaultState();
            }

            if (placing != null && placing.canPlaceAt(world, pos)) {
                world.setBlockState(pos, placing);
                world.syncWorldEvent(WorldEvents.BONE_MEAL_USED, pos, 0);
                return;
            }
        }

        if (steppingOn.isIn(BlockTags.SAPLINGS) ||
                steppingOver.isIn(BlockTags.SAPLINGS) ||
                steppingOver.isOf(Blocks.BROWN_MUSHROOM) ||
                steppingOver.isOf(Blocks.RED_MUSHROOM) ||
                steppingOver.isOf(Blocks.WARPED_FUNGUS) ||
                steppingOver.isOf(Blocks.CRIMSON_FUNGUS)) {
            return; // if these structures grow on the dragon they could hurt it...
        }

        // perform standard bone meal behavior on steppingOn or steppingOver block.
        BlockPos caret = pos;
        for (int i = 0; i < 2; caret = groundPos) {
            i++;
            BlockState state = world.getBlockState(caret);
            if (!(state.getBlock() instanceof Fertilizable b) || !b.isFertilizable(world, caret, state, world.isClient()))
                continue;

            if (b.canGrow(world, dragon.getRandom(), caret, state)) {
                b.grow((ServerWorld) world, world.getRandom(), caret, state);
                world.syncWorldEvent(WorldEvents.BONE_MEAL_USED, caret, 0);
                return;
            }
        }
    }

    @Override
    public GreenToesAbility create() {
        return this;
    }

    @Override
    public Identifier type() {
        return GREEN_TOES;
    }
}
