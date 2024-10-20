package com.iafenvoy.dragonmounts.data;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.iafenvoy.dragonmounts.DragonMounts;
import com.iafenvoy.dragonmounts.dragon.breed.BreedRegistry;
import com.iafenvoy.dragonmounts.dragon.breed.DragonBreed;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class CrossBreedingManager extends JsonDataLoader implements IdentifiableResourceReloadListener {
    public static final CrossBreedingManager INSTANCE = new CrossBreedingManager();
    private static final String PATH = "dragon_mounts/cross_breeding"; // data/[pack_name]/dragon_mounts/cross_breeds/whatever.json

    private final Map<Couple, RegistryKey<DragonBreed>> crosses = new HashMap<>();

    private CrossBreedingManager() {
        super(new GsonBuilder().create(), PATH);
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> entries, ResourceManager pResourceManager, Profiler pProfiler) {
        this.crosses.clear();
        for (Map.Entry<Identifier, JsonElement> entry : entries.entrySet()) {
            Identifier id = entry.getKey();
            JsonElement json = entry.getValue();
            CrossBreedResult cross = CrossBreedResult.CODEC.parse(JsonOps.INSTANCE, json)
                    .getOrThrow(false, Util.addPrefix("Unable to parse Cross Breeding result for: " + id, DragonMounts.LOGGER::error));
            this.crosses.put(new Couple(cross.parent1(), cross.parent2()), cross.child());
        }
    }

    @Nullable
    public DragonBreed getCrossBreed(DragonBreed parent, DragonBreed mate, DynamicRegistryManager ra) {
        Registry<DragonBreed> reg = BreedRegistry.registry(ra);
        RegistryKey<DragonBreed> parentKey = reg.getKey(parent).orElseThrow();
        RegistryKey<DragonBreed> mateKey = reg.getKey(mate).orElseThrow();
        RegistryKey<DragonBreed> result = this.crosses.get(new Couple(parentKey, mateKey));
        return result == null ? null : reg.get(result);
    }

    @Override
    public Identifier getFabricId() {
        return new Identifier(DragonMounts.MOD_ID, "cross_breeding");
    }

    public record CrossBreedResult(RegistryKey<DragonBreed> parent1, RegistryKey<DragonBreed> parent2,
                                   RegistryKey<DragonBreed> child) {
        public static final Codec<CrossBreedResult> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                RegistryKey.createCodec(BreedRegistry.REGISTRY_KEY).fieldOf("parent1").forGetter(CrossBreedResult::parent1),
                RegistryKey.createCodec(BreedRegistry.REGISTRY_KEY).fieldOf("parent2").forGetter(CrossBreedResult::parent2),
                RegistryKey.createCodec(BreedRegistry.REGISTRY_KEY).fieldOf("child").forGetter(CrossBreedResult::child)
        ).apply(instance, CrossBreedResult::new));
    }

    private record Couple(RegistryKey<DragonBreed> parent1, RegistryKey<DragonBreed> parent2) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || this.getClass() != o.getClass()) return false;
            Couple couple = (Couple) o;
            return (this.parent1 == couple.parent1 && this.parent2 == couple.parent2) ||
                    (this.parent1 == couple.parent2 && this.parent2 == couple.parent1);
        }

        @Override
        public int hashCode() {
            return this.parent1.hashCode() + this.parent2.hashCode();
        }
    }
}
