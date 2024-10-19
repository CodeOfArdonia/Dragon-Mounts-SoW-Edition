package com.iafenvoy.dragonmounts.dragon.ai;

import com.iafenvoy.dragonmounts.dragon.TameableDragon;
import com.iafenvoy.dragonmounts.event.DragonBreedCallback;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ai.goal.AnimalMateGoal;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;

import java.util.List;

public class DragonBreedGoal extends AnimalMateGoal {
    private final TameableDragon dragon;

    public DragonBreedGoal(TameableDragon animal) {
        super(animal, 1);
        this.dragon = animal;
    }

    @Override
    public boolean canStart() {
        if (!this.dragon.isAdult()) return false;
        if (!this.dragon.isInLove()) return false;
        else return (this.mate = this.getNearbyMate()) != null;
    }

    public TameableDragon getNearbyMate() {
        List<TameableDragon> list = this.world.getNonSpectatingEntities(TameableDragon.class, this.dragon.getBoundingBox().expand(8d));
        double dist = Double.MAX_VALUE;
        TameableDragon closest = null;
        for (TameableDragon entity : list)
            if (this.dragon.canBreedWith(entity) && this.dragon.squaredDistanceTo(entity) < dist) {
                closest = entity;
                dist = this.dragon.squaredDistanceTo(entity);
            }

        return closest;
    }

    @Override
    protected void breed() {
        if (this.mate == null) return;
        // Respect Mod compatibility
        if (!DragonBreedCallback.EVENT.invoker().allow(this.animal, this.mate)) {
            // Reset the "inLove" state for the animals
            this.animal.setBreedingAge(6000);
            this.mate.setBreedingAge(6000);
            return;
        }
        this.animal.resetLoveTicks();
        this.mate.resetLoveTicks();
        this.dragon.breed((ServerWorld) this.world, this.mate);
        this.world.sendEntityStatus(this.animal, (byte) 18);
        if (this.world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT))
            this.world.spawnEntity(new ExperienceOrbEntity(this.world, this.animal.getX(), this.animal.getY(), this.animal.getZ(), this.animal.getRandom().nextInt(7) + 1));
    }
}