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
import net.minecraft.world.WorldEvents;

public class GreenToesAbility extends FootprintAbility implements Ability.Factory<GreenToesAbility> {
    public static final GreenToesAbility INSTANCE = new GreenToesAbility();
    public static final Codec<GreenToesAbility> CODEC = Codec.unit(INSTANCE);

    protected GreenToesAbility() {
    }

    // grow mushrooms and plants
    @Override
    protected void placeFootprint(TameableDragon dragon, BlockPos pos) {
        var level = dragon.getWorld();
        var groundPos = pos.down();
        var steppingOn = level.getBlockState(groundPos);
        var steppingOver = level.getBlockState(pos);

        if (steppingOn.isOf(Blocks.DIRT)) // regrow grass on dirt
        {
            level.setBlockState(groundPos, Blocks.GRASS_BLOCK.getDefaultState());
            level.syncWorldEvent(WorldEvents.BONE_MEAL_USED, groundPos, 2);
            return;
        }

        if (steppingOver.isAir()) // manually place flowers, mushrooms, etc.
        {
            BlockState placing = null;

            if (steppingOn.isIn(BlockTags.MUSHROOM_GROW_BLOCK))
                placing = (level.getRandom().nextBoolean() ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).getDefaultState();
            else if (steppingOn.isIn(BlockTags.DIRT) && !steppingOn.isOf(Blocks.MOSS_BLOCK)) // different from the actual dirt block, could be grass or podzol.
            {
                // while grass blocks etc. do have defined bone meal behavior, I think our own is more viable.
                placing = level.getRegistryManager().get(RegistryKeys.BLOCK)
                        .getEntryList(BlockTags.SMALL_FLOWERS)
                        .flatMap(tag -> tag.getRandom(dragon.getRandom()))
                        .map(RegistryEntry::value)
                        .filter(b -> b != Blocks.WITHER_ROSE)
                        .orElse(Blocks.DANDELION)
                        .getDefaultState();
            }

            if (placing != null && placing.canPlaceAt(level, pos)) {
                level.setBlockState(pos, placing);
                level.syncWorldEvent(WorldEvents.BONE_MEAL_USED, pos, 0);
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
        var caret = pos;
        for (int i = 0; i < 2; caret = groundPos) {
            i++;
            var state = level.getBlockState(caret);
            if (!(state.getBlock() instanceof Fertilizable b) || !b.isFertilizable(level, caret, state, level.isClient()))
                continue;

            if (b.canGrow(level, dragon.getRandom(), caret, state)) {
                b.grow((ServerWorld) level, level.getRandom(), caret, state);
                level.syncWorldEvent(WorldEvents.BONE_MEAL_USED, caret, 0);
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
