package com.iafenvoy.dragonmounts.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.passive.AnimalEntity;

@FunctionalInterface
public interface DragonBreedCallback {
    Event<DragonBreedCallback> EVENT = EventFactory.createArrayBacked(DragonBreedCallback.class, callbacks -> (animal, mate) -> {
        for (DragonBreedCallback callback : callbacks)
            if (!callback.allow(animal, mate))
                return false;
        return true;
    });

    @SuppressWarnings("all")
    boolean allow(AnimalEntity animal, AnimalEntity mate);
}
