package com.iafenvoy.dragonmounts.dragon.breed;

import com.iafenvoy.dragonmounts.DragonMounts;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

public class BreedRegistry {
    public static final RegistryKey<Registry<DragonBreed>> REGISTRY_KEY = RegistryKey.ofRegistry(Identifier.of(DragonMounts.MOD_ID, "dragon_breeds"));

    @Nullable
    public static DragonBreed get(String byString, DynamicRegistryManager reg) {
        return get(new Identifier(byString), reg);
    }

    @Nullable
    public static DragonBreed get(Identifier byId, DynamicRegistryManager reg) {
        return registry(reg).get(byId);
    }

    public static DragonBreed getRandom(DynamicRegistryManager reg, Random random) {
        return registry(reg).getRandom(random).orElseThrow().value();
    }

    public static Registry<DragonBreed> registry(DynamicRegistryManager reg) {
        return reg.getOptional(REGISTRY_KEY).orElseGet(() -> DynamicRegistryManager.EMPTY.get(REGISTRY_KEY));
    }
}
