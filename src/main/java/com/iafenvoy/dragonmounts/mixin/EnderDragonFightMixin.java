package com.iafenvoy.dragonmounts.mixin;

import com.iafenvoy.dragonmounts.config.DMCommonConfig;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnderDragonFight.class)
public class EnderDragonFightMixin {
    /**
     * Purpose: To implement a way to replenish dragon eggs after death of the ender dragon
     * <br>
     * This mixin redirects the 'if' statement in setDragonKilled that tests if the ender dragon was previously killed
     * essentially, instead of {@code if (!previouslyKilled) {...}},
     * we do {@code if (!dragonmounts_replenishDragonEgg) {...}}
     */
    @Redirect(method = "dragonKilled", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/boss/dragon/EnderDragonFight;previouslyKilled:Z", opcode = Opcodes.GETFIELD))
    private boolean dragonmounts_replenishDragonEgg(EnderDragonFight instance) {
        // return the inverse of what we want because the target check inverts the result... yeah.
        return instance.hasPreviouslyKilled() && !DMCommonConfig.INSTANCE.COMMON.replenishEggs.getValue();
    }
}
