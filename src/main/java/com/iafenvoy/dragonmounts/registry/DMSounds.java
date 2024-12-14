package com.iafenvoy.dragonmounts.registry;

import com.iafenvoy.dragonmounts.DragonMounts;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class DMSounds {
    public static final SoundEvent DRAGON_AMBIENT_SOUND = register("entity.dragon.ambient");
    public static final SoundEvent DRAGON_STEP_SOUND = register("entity.dragon.step");
    public static final SoundEvent DRAGON_DEATH_SOUND = register("entity.dragon.death");
    public static final SoundEvent GHOST_DRAGON_AMBIENT = register("entity.dragon.ambient.ghost");

    private static SoundEvent register(String id) {
        return Registry.register(Registries.SOUND_EVENT, new Identifier(DragonMounts.MOD_ID, id), SoundEvent.of(Identifier.of(DragonMounts.MOD_ID, id)));
    }

    public static void init() {
    }
}