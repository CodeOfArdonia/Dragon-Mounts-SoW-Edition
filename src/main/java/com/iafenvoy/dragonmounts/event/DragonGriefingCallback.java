package com.iafenvoy.dragonmounts.event;

import com.iafenvoy.dragonmounts.dragon.TameableDragonEntity;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.World;

@FunctionalInterface
public interface DragonGriefingCallback {
    Event<DragonGriefingCallback> EVENT = EventFactory.createArrayBacked(DragonGriefingCallback.class, callbacks -> (world, dragon) -> {
        for (DragonGriefingCallback callback : callbacks)
            if (!callback.allow(world, dragon))
                return false;
        return true;
    });

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean allow(World world, TameableDragonEntity dragon);
}
