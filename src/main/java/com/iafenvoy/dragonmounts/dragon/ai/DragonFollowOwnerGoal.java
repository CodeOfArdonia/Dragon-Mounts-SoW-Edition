package com.iafenvoy.dragonmounts.dragon.ai;

import com.iafenvoy.dragonmounts.dragon.TameableDragonEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.WorldView;

import java.util.EnumSet;

/**
 * Goal for dragon to follow its owner.
 * <p></p>
 * Mostly copied from <code>FollowOwnerGoal</code>, but with some modifications to fix an issue.
 * Also allows dragon to tp to owner in the air, so they don't get stuck until the owner lands.
 *
 * @author AnimalsWritingCode
 * @see net.minecraft.entity.ai.goal.FollowOwnerGoal
 */
public class DragonFollowOwnerGoal extends Goal {
    private static final int MIN_HORIZONTAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING = 2;
    private static final int MAX_HORIZONTAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING = 3;
    private static final int MIN_VERTICAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING = 0;
    private static final int MAX_VERTICAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING = 1;
    private final TameableDragonEntity dragon;
    private LivingEntity owner;
    private final WorldView level;
    private final double speedModifier;
    private int timeToRecalcPath;
    private final float stopDistance;
    private final float startDistance;
    private final float teleportDistance;
    private float oldWaterCost;

    public DragonFollowOwnerGoal(TameableDragonEntity dragon, double speedModifier, float startDistance, float stopDistance, float teleportDistance) {
        this.dragon = dragon;
        this.level = dragon.getWorld();
        this.speedModifier = speedModifier;
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;
        this.teleportDistance = teleportDistance;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        LivingEntity livingentity = this.dragon.getOwner();
        if (livingentity == null) return false;
        if (livingentity.isSpectator()) return false;
        if (this.dragon.isSitting()) return false;
        if (this.dragon.squaredDistanceTo(livingentity) < (double) (this.startDistance * this.startDistance))
            return false;
        this.owner = livingentity;
        return true;
    }

    @Override
    public boolean shouldContinue() {
        if (this.dragon.getNavigation().isIdle()) return false;
        if (this.dragon.isSitting()) return false;
        return this.dragon.squaredDistanceTo(this.owner) >= (double) (this.stopDistance * this.stopDistance);
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
        this.oldWaterCost = this.dragon.getPathfindingPenalty(PathNodeType.WATER);
        this.dragon.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
    }

    @Override
    public void stop() {
        this.dragon.getNavigation().stop();
        this.dragon.setPathfindingPenalty(PathNodeType.WATER, this.oldWaterCost);
    }

    @Override
    public void tick() {
        this.dragon.getLookControl().lookAt(this.owner, 10.0F, (float) this.dragon.getMaxLookPitchChange());
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = this.getTickCount(10);
            if (!this.dragon.isLeashed() && !this.dragon.hasVehicle()) {
                if (this.dragon.squaredDistanceTo(this.owner) >= (this.teleportDistance * this.teleportDistance))
                    this.teleportToOwner();
                else if (!this.dragon.isInAir() && this.dragon.canFly() && (this.owner.getBlockPos().getY() - this.dragon.getBlockPos().getY()) >= this.startDistance)
                    this.dragon.liftOff();
                else this.dragon.getNavigation().startMovingTo(this.owner, this.speedModifier);
            }
        }
    }

    private void teleportToOwner() {
        BlockPos ownerPos = this.owner.getBlockPos();
        for (int i = 0; i < 10; ++i) {
            BlockPos target = this.randomBlockPosNearPos(
                    ownerPos,
                    MIN_HORIZONTAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING,
                    MAX_HORIZONTAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING,
                    MIN_VERTICAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING,
                    MAX_VERTICAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING
            );
            if (this.maybeTeleportTo(target)) return;
        }
    }

    private boolean maybeTeleportTo(BlockPos pos) {
        if (this.owner.getBlockPos().isWithinDistance(pos, 2.0D)) return false;
        if (!this.canTeleportTo(pos)) return false;
        this.dragon.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ(), this.dragon.getYaw(), this.dragon.getPitch());
        this.dragon.getNavigation().stop();
        return true;
    }

    private boolean canTeleportTo(BlockPos pos) {
        if (!this.dragon.canFly()) {
            PathNodeType blockpathtypes = LandPathNodeMaker.getLandNodeType(this.level, pos.mutableCopy());
            if (blockpathtypes != PathNodeType.WALKABLE)
                return false;
            BlockState blockstate = this.level.getBlockState(pos.down());
            if (blockstate.getBlock() instanceof LeavesBlock) return false;
        }
        BlockPos blockPos = pos.subtract(this.dragon.getBlockPos());
        Box targetBoundingBox = this.dragon.getBoundingBox().offset(blockPos);
        return this.level.isSpaceEmpty(this.dragon, targetBoundingBox) && !this.level.containsFluid(targetBoundingBox);
    }

    private int randomIntInclusive(int min, int max) {
        return this.dragon.getRandom().nextInt(max - min + 1) + min;
    }

    private int randomIntInclusive(int farLow, int nearLow, int nearHigh, int farHigh) {
        if (nearLow == nearHigh)
            return this.randomIntInclusive(farLow, farHigh);
        return this.dragon.getRandom().nextBoolean() ?
                this.randomIntInclusive(farLow, nearLow) :
                this.randomIntInclusive(nearHigh, farHigh);
    }

    private BlockPos randomBlockPosNearPos(BlockPos origin, int minDist, int maxDist, int minYDist, int maxYDist) {
        int x = this.randomIntInclusive(-maxDist, -minDist, minDist, maxDist);
        int y = this.randomIntInclusive(-maxYDist, -minYDist, minYDist, maxYDist);
        int z = this.randomIntInclusive(-maxDist, -minDist, minDist, maxDist);
        return origin.add(x, y, z);
    }
}
