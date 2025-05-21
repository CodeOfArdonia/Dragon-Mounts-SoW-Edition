package com.iafenvoy.dragonmounts.registry;

import com.iafenvoy.dragonmounts.DragonMounts;
import com.iafenvoy.dragonmounts.particle.DragonParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class DMParticles {
    public static final ParticleType<DragonParticleEffect> DRAGON_DROP = register("dragon_drop", new DragonParticleEffect(null, -1, 1));
    public static final ParticleType<DragonParticleEffect> DRAGON_DUST = register("dragon_dust", new DragonParticleEffect(null, -1, 1));
    public static final ParticleType<DragonParticleEffect> DRAGON_WIND = register("dragon_wind", new DragonParticleEffect(null, -1, 1));

    private static <T extends ParticleEffect> ParticleType<T> register(String name, ParticleType<T> particle) {
        return Registry.register(Registries.PARTICLE_TYPE, Identifier.of(DragonMounts.MOD_ID, name), particle);
    }

    public static void init() {
    }
}
