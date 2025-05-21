package com.iafenvoy.dragonmounts.abilities;

import com.iafenvoy.dragonmounts.dragon.TameableDragonEntity;
import com.iafenvoy.dragonmounts.event.DragonGriefingCallback;
import net.minecraft.util.math.BlockPos;

public abstract class FootprintAbility implements Ability {
    @Override
    public void onMove(TameableDragonEntity dragon) {
        if (dragon.getAgeProgress() < 0.5 || !dragon.isOnGround()) return;
        if (!DragonGriefingCallback.EVENT.invoker().allow(dragon.getWorld(), dragon)) return;

        float chance = this.getFootprintChance(dragon);
        if (chance == 0) return;

        for (int i = 0; i < 4; i++) {
            // place only if randomly selected
            if (dragon.getRandom().nextFloat() > chance) continue;

            // get footprint position
            int bx = (int) (dragon.getX() + (i % 2 * 2 - 1) * dragon.getScaleFactor());
            int by = (int) dragon.getY();
            int bz = (int) (dragon.getZ() + (i / 2f % 2 * 2 - 1) * dragon.getScaleFactor());
            BlockPos pos = new BlockPos(bx, by, bz);
            this.placeFootprint(dragon, pos);
        }
    }

    protected float getFootprintChance(TameableDragonEntity dragon) {
        return 0.05f;
    }

    protected abstract void placeFootprint(TameableDragonEntity dragon, BlockPos pos);
}
