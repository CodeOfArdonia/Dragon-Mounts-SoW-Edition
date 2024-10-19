package com.iafenvoy.dragonmounts.dragon.breed;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iafenvoy.dragonmounts.DMLConfig;
import com.iafenvoy.dragonmounts.DragonMounts;
import com.iafenvoy.dragonmounts.abilities.Ability;
import com.iafenvoy.dragonmounts.dragon.TameableDragon;
import com.iafenvoy.dragonmounts.dragon.egg.HatchableEggBlock;
import com.iafenvoy.dragonmounts.habitats.Habitat;
import com.iafenvoy.dragonmounts.registry.DMEntities;
import com.iafenvoy.dragonmounts.util.DMLUtil;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.attribute.DefaultAttributeRegistry;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.item.Item;
import net.minecraft.loot.LootTables;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public record DragonBreed(int primaryColor, int secondaryColor, Optional<ParticleEffect> hatchParticles,
                          Map<EntityAttribute, Double> attributes, List<Ability.Factory<Ability>> abilityTypes,
                          List<Habitat> habitats,
                          RegistryEntryList<DamageType> immunities, Optional<RegistryEntry<SoundEvent>> ambientSound,
                          Identifier deathLoot, int growthTime, float hatchChance, float sizeModifier,
                          RegistryEntryList<Item> tamingItems, RegistryEntryList<Item> breedingItems,
                          Either<Integer, String> reproLimit) {
    public static final Codec<DragonBreed> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DMLUtil.HEX_CODEC.fieldOf("primary_color").forGetter(DragonBreed::primaryColor),
            DMLUtil.HEX_CODEC.fieldOf("secondary_color").forGetter(DragonBreed::secondaryColor),
            ParticleTypes.TYPE_CODEC.optionalFieldOf("hatch_particles").forGetter(DragonBreed::hatchParticles),
            Codec.unboundedMap(Registries.ATTRIBUTE.getCodec(), Codec.DOUBLE).optionalFieldOf("attributes", ImmutableMap.of()).forGetter(DragonBreed::attributes),
            Ability.CODEC.listOf().optionalFieldOf("abilities", ImmutableList.of()).forGetter(DragonBreed::abilityTypes),
            Habitat.CODEC.listOf().optionalFieldOf("habitats", ImmutableList.of()).forGetter(DragonBreed::habitats),
            RegistryCodecs.entryList(RegistryKeys.DAMAGE_TYPE).optionalFieldOf("immunities", RegistryEntryList.of()).forGetter(DragonBreed::immunities),
            SoundEvent.ENTRY_CODEC.optionalFieldOf("ambient_sound").forGetter(DragonBreed::ambientSound),
            Identifier.CODEC.optionalFieldOf("death_loot", LootTables.EMPTY).forGetter(DragonBreed::deathLoot),
            Codec.INT.optionalFieldOf("growth_time", TameableDragon.BASE_GROWTH_TIME).forGetter(DragonBreed::growthTime),
            Codec.FLOAT.optionalFieldOf("hatch_chance", HatchableEggBlock.DEFAULT_HATCH_CHANCE).forGetter(DragonBreed::hatchChance),
            Codec.FLOAT.optionalFieldOf("size_modifier", TameableDragon.BASE_SIZE_MODIFIER).forGetter(DragonBreed::sizeModifier),
            RegistryCodecs.entryList(RegistryKeys.ITEM).optionalFieldOf("taming_items", Registries.ITEM.getOrCreateEntryList(ItemTags.FISHES)).forGetter(DragonBreed::tamingItems),
            RegistryCodecs.entryList(RegistryKeys.ITEM).optionalFieldOf("breeding_items", Registries.ITEM.getOrCreateEntryList(ItemTags.FISHES)).forGetter(DragonBreed::breedingItems),
            Codec.either(Codec.INT, Codec.STRING).optionalFieldOf("reproduction_limit", Either.left(-1)).forGetter(DragonBreed::reproLimit)
    ).apply(instance, DragonBreed::new));
    public static final Codec<DragonBreed> NETWORK_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("primary_color").forGetter(DragonBreed::primaryColor),
            Codec.INT.fieldOf("secondary_color").forGetter(DragonBreed::secondaryColor),
            ParticleTypes.TYPE_CODEC.optionalFieldOf("hatch_particles").forGetter(DragonBreed::hatchParticles),
            SoundEvent.ENTRY_CODEC.optionalFieldOf("ambient_sound").forGetter(DragonBreed::ambientSound),
            Codec.INT.fieldOf("growth_time").forGetter(DragonBreed::growthTime),
            Codec.FLOAT.optionalFieldOf("size_modifier", TameableDragon.BASE_SIZE_MODIFIER).forGetter(DragonBreed::sizeModifier)
    ).apply(instance, DragonBreed::fromNetwork));

    public static DragonBreed fromNetwork(int primaryColor, int secondaryColor, Optional<ParticleEffect> hatchParticles, Optional<RegistryEntry<SoundEvent>> ambientSound, int growthTime, float sizeModifier) {
        return new DragonBreed(primaryColor, secondaryColor, hatchParticles, Map.of(), List.of(), List.of(), RegistryEntryList.of(), ambientSound, LootTables.EMPTY, growthTime, 0, sizeModifier, RegistryEntryList.of(), RegistryEntryList.of(), Either.left(0));
    }

    public void initialize(TameableDragon dragon) {
        this.applyAttributes(dragon);
        for (var factory : this.abilityTypes()) {
            var instance = factory.create();
            dragon.getAbilities().add(instance);
            instance.initialize(dragon);
        }
    }

    public void close(TameableDragon dragon) {
        this.cleanAttributes(dragon);
        for (Ability ability : dragon.getAbilities()) ability.close(dragon);
        dragon.getAbilities().clear();
    }

    public int getReproductionLimit() {
        return this.reproLimit().map(Function.identity(), DMLConfig::getReproLimitFor);
    }

    public Identifier id(DynamicRegistryManager reg) {
        return BreedRegistry.registry(reg).getId(this);
    }

    public static String getTranslationKey(String resourceLocation) {
        return "dragon_breed." + resourceLocation.replace(':', '.');
    }

    private void applyAttributes(TameableDragon dragon) {
        float healthFrac = dragon.getHealthFraction(); // in case max health is changed
        this.attributes().forEach((att, value) -> {
            EntityAttributeInstance inst = dragon.getAttributeInstance(att);
            if (inst != null) inst.setBaseValue(value);
        });
        dragon.setHealth(dragon.getMaxHealth() * healthFrac);
    }

    private void cleanAttributes(TameableDragon dragon) {
        float healthFrac = dragon.getHealthFraction(); // in case max health is changed
        var defaults = DefaultAttributeRegistry.get(DMEntities.DRAGON);
        this.attributes().forEach((att, value) -> {
            var instance = dragon.getAttributeInstance(att);
            if (instance != null) {
                instance.clearModifiers();
                instance.setBaseValue(defaults.getBaseValue(att));
            }
        });
        dragon.setHealth(dragon.getMaxHealth() * healthFrac);
    }

    public static final class BuiltIn {
        public static final RegistryKey<DragonBreed> AETHER = key("aether");
        public static final RegistryKey<DragonBreed> END = key("end");
        public static final RegistryKey<DragonBreed> FIRE = key("fire");
        public static final RegistryKey<DragonBreed> FOREST = key("forest");
        public static final RegistryKey<DragonBreed> GHOST = key("ghost");
        public static final RegistryKey<DragonBreed> ICE = key("ice");
        public static final RegistryKey<DragonBreed> NETHER = key("nether");
        public static final RegistryKey<DragonBreed> WATER = key("water");

        private static RegistryKey<DragonBreed> key(String id) {
            return RegistryKey.of(BreedRegistry.REGISTRY_KEY, Identifier.of(DragonMounts.MOD_ID, id));
        }

        private BuiltIn() {
        }
    }
}