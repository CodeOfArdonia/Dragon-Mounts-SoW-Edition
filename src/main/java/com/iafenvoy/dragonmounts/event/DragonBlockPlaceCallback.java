package com.iafenvoy.dragonmounts.event;

import com.iafenvoy.dragonmounts.dragon.TameableDragonEntity;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@FunctionalInterface
public interface DragonBlockPlaceCallback {
    Event<DragonBlockPlaceCallback> EVENT = EventFactory.createArrayBacked(DragonBlockPlaceCallback.class, callbacks -> (dragon, world, pos, direction) -> {
        for (DragonBlockPlaceCallback callback : callbacks)
            if (!callback.canPlace(dragon, world, pos, direction))
                return false;
        return true;
    });

    boolean canPlace(TameableDragonEntity dragon, World world, BlockPos pos, Direction direction);
}
