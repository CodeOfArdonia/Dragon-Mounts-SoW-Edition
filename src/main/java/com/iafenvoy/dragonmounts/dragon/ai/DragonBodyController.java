package com.iafenvoy.dragonmounts.dragon.ai;

import com.iafenvoy.dragonmounts.dragon.TameableDragon;
import net.minecraft.entity.ai.control.BodyControl;
import net.minecraft.util.math.MathHelper;

public class DragonBodyController extends BodyControl {
    private final TameableDragon dragon;

    public DragonBodyController(TameableDragon dragon) {
        super(dragon);
        this.dragon = dragon;
    }

    @Override
    public void tick() {
        // sync the body to the yRot; no reason to have any other random rotations.
        this.dragon.bodyYaw = this.dragon.getYaw();
        // clamp head rotations so necks don't fucking turn inside out
        this.dragon.headYaw = MathHelper.clampAngle(this.dragon.headYaw, this.dragon.bodyYaw, this.dragon.getMaxHeadRotation());
    }
}