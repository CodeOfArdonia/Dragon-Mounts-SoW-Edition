package com.iafenvoy.dragonmounts.event;

import com.iafenvoy.dragonmounts.dragon.TameableDragon;
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

    boolean allow(World world, TameableDragon dragon);
}
