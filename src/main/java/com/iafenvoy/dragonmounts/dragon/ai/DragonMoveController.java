package com.iafenvoy.dragonmounts.dragon.ai;

import com.iafenvoy.dragonmounts.dragon.TameableDragonEntity;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.MathHelper;

public class DragonMoveController extends MoveControl {
    private final TameableDragonEntity dragon;

    public DragonMoveController(TameableDragonEntity dragon) {
        super(dragon);
        this.dragon = dragon;
    }

    @Override
    public void tick() {
        // original movement behavior if the entity isn't flying
        if (!this.dragon.isInAir()) {
            super.tick();
            return;
        }

        if (this.state == State.MOVE_TO) {
            this.state = State.WAIT;
            double xDif = this.targetX - this.entity.getX();
            double yDif = this.targetY - this.entity.getY();
            double zDif = this.targetZ - this.entity.getZ();
            double sq = xDif * xDif + yDif * yDif + zDif * zDif;
            if (sq < (double) 2.5000003E-7F) {
                this.entity.setUpwardSpeed(0.0F);
                this.entity.setForwardSpeed(0.0F);
                return;
            }

            float speed = (float) (this.speed * this.entity.getAttributeValue(EntityAttributes.GENERIC_FLYING_SPEED));
            double distSq = Math.sqrt(xDif * xDif + zDif * zDif);
            this.entity.setMovementSpeed(speed);
            if (Math.abs(yDif) > (double) 1.0E-5F || Math.abs(distSq) > (double) 1.0E-5F)
                this.entity.setUpwardSpeed((float) yDif * speed);

            float yaw = (float) (MathHelper.atan2(zDif, xDif) * (double) (180F / (float) Math.PI)) - 90.0F;
            this.entity.setYaw(this.wrapDegrees(this.entity.getYaw(), yaw, 6));
        } else {
            this.entity.setUpwardSpeed(0);
            this.entity.setForwardSpeed(0);
        }
    }
}