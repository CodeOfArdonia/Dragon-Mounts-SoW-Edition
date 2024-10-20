package com.iafenvoy.dragonmounts.abilities;

import com.iafenvoy.dragonmounts.dragon.TameableDragon;
import com.iafenvoy.dragonmounts.event.DragonBlockPlaceCallback;
import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FrostedIceBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;


public class FrostWalkerAbility implements Ability, Ability.Factory<FrostWalkerAbility> {
    public static final Codec<FrostWalkerAbility> CODEC = Codec.FLOAT
            .xmap(FrostWalkerAbility::new, a -> a.radiusMultiplier)
            .fieldOf("radius_multiplier")
            .codec();

    private final float radiusMultiplier;

    protected FrostWalkerAbility(float radiusMultiplier) {
        this.radiusMultiplier = radiusMultiplier;
    }

    public static FrostWalkerAbility create(float radiusMultiplier) {
        return new FrostWalkerAbility(radiusMultiplier);
    }

    @Override
    public void initialize(TameableDragon dragon) {
        dragon.setPathfindingPenalty(PathNodeType.WATER, 0);
    }

    @Override
    public void close(TameableDragon dragon) {
        dragon.setPathfindingPenalty(PathNodeType.WATER, PathNodeType.WATER.getDefaultPenalty());
    }

    @Override
    public void tick(TameableDragon dragon) {
        World world = dragon.getWorld();
        if (dragon.age % 3 != 0) return; // no need for expensive calcs EVERY tick
        if (world.isClient || dragon.getAgeProgress() < 0.5)
            return; // only juveniles and older can frost walk

        // taken from and modified of FrostWalkerEnchantment#onEntityMoved
        int radius = (int) (Math.max(this.radiusMultiplier * dragon.getScaleFactor(), 1) + 3);
        BlockPos pos = dragon.getBlockPos();

        for (BlockPos carat : BlockPos.iterate(pos.add((-radius), -2, (-radius)), pos.add(radius, -1, radius))) {
            if (!carat.isWithinDistance(dragon.getPos(), radius)) continue; // circle
            BlockState currentState = world.getBlockState(carat);
            if (currentState != FrostedIceBlock.getMeltedState()) continue;
            if (DragonBlockPlaceCallback.EVENT.invoker().canPlace(dragon, world, carat, Direction.UP)) continue;
            BlockState ice = Blocks.FROSTED_ICE.getDefaultState();
            if (!ice.canPlaceAt(world, carat) || !world.canPlace(ice, carat, ShapeContext.absent())) continue;
            BlockPos mPos = carat.mutableCopy().move(0, 1, 0);
            if (!world.getBlockState(mPos).isAir()) continue;
            world.setBlockState(carat, ice);
            world.scheduleBlockTick(carat, Blocks.FROSTED_ICE, MathHelper.nextInt(dragon.getRandom(), 60, 120));
        }
    }

    @Override
    public FrostWalkerAbility create() {
        return this;
    }

    @Override
    public Identifier type() {
        return FROST_WALKER;
    }
}
