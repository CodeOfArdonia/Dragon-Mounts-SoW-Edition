package com.iafenvoy.dragonmounts.dragon.ai;

import com.iafenvoy.dragonmounts.dragon.TameableDragonEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

import java.util.EnumSet;

public class DragonLandGoal extends Goal {
    private final TameableDragonEntity dragon;
    private BlockPos landingPos;

    public DragonLandGoal(TameableDragonEntity dragon) {
        this.dragon = dragon;
        this.setControls(EnumSet.of(Control.MOVE, Control.JUMP, Control.TARGET));
    }

    @Override
    public boolean canStart() {
        return !this.dragon.isNearGround() && this.dragon.getControllingPassenger() == null && this.dragon.isInAir() && this.findLandingBlock();
    }

    @Override
    public boolean shouldContinue() {
        return !this.dragon.isNearGround() && this.dragon.getControllingPassenger() == null && this.dragon.isInAir();
    }

    @Override
    public void tick() {
        if (this.dragon.getNavigation().isIdle()) this.start();
    }

    @Override
    public void start() {
        this.dragon.getNavigation().stop();
        this.dragon.getNavigation().startMovingTo(this.landingPos.getX(), this.landingPos.getY(), this.landingPos.getZ(), 1);
    }

    @SuppressWarnings("deprecation")
    private boolean findLandingBlock() {
        Random rand = this.dragon.getRandom();
        this.landingPos = this.dragon.getBlockPos();
        int followRange = MathHelper.floor(this.dragon.getAttributeValue(EntityAttributes.GENERIC_FOLLOW_RANGE));
        int ox = followRange - rand.nextInt(followRange) * 2;
        int oz = followRange - rand.nextInt(followRange) * 2;
        this.landingPos = this.landingPos.add(ox, 0, oz);
        this.landingPos = this.dragon.getWorld().getTopPosition(Heightmap.Type.WORLD_SURFACE, this.landingPos);
        return this.dragon.getWorld().getBlockState(this.landingPos.down()).isSolid();
    }
}
