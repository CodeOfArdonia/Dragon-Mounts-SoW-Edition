package com.iafenvoy.dragonmounts.abilities;

import com.iafenvoy.dragonmounts.DragonMounts;
import com.iafenvoy.dragonmounts.dragon.TameableDragonEntity;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Each ability type has its own number of factories. One type can have multiple factories
 * to specify behavior.
 * Factories create instances that are unique to each dragon entity
 * this is due to the fact that sometimes an ability needs entity-specific actions,
 * and sometimes we don't (so we memoize the instance instead of making dozens of copies)
 * For example:
 * - Ability X is defined in the datapack to place blocks in Y radius for Dragon Breed A.
 * - Ability X is defined in the datapack to place blocks in Z radius for Dragon Breed B.
 * - Ability X also requires a cooldown timer that has to be specific to each entity.
 * Breed A has:
 * - Ability X:
 * - final int radius = Y; // finals are Breed Specific variables
 * - int cooldown = 50 // other variables are Dragon-entity specific.
 * Breed B has:
 * - Ability X:
 * - final int radius = Z;
 * - int cooldown = 1000
 * <br>
 * Same ability type, different constants, instance-based variables.
 * <br>
 * If the ability DOES NOT require per-entity data, it may be better to off to implement {@link Factory} directly and
 * return itself.
 */
public interface Ability {
    Map<Identifier, Codec<? extends Factory<Ability>>> REGISTRY = new HashMap<>();

    Codec<Factory<Ability>> CODEC = Identifier.CODEC.dispatch(Factory::type, REGISTRY::get);

    Identifier FROST_WALKER = reg("frost_walker", FrostWalkerAbility.CODEC);
    Identifier GREEN_TOES = reg("green_toes", GreenToesAbility.CODEC);
    Identifier SNOW_STEPPER = reg("snow_stepper", SnowStepperAbility.CODEC);
    Identifier HOT_FEET = reg("hot_feet", HotFeetAbility.CODEC);
    Identifier REAPER_STEP = reg("reaper_step", ReaperStepAbility.CODEC);
    Identifier HYDRO_STEP = reg("hydro_step", HydroStepAbility.CODEC);

    @SuppressWarnings("unchecked")
    static <T extends Ability> Identifier register(Identifier name, Codec<? extends Factory<T>> codec) {
        REGISTRY.put(name, (Codec) codec); // hacky generics cast
        return name;
    }

    private static <T extends Ability> Identifier reg(String name, Codec<? extends Factory<T>> codec) {
        return register(Identifier.of(DragonMounts.MOD_ID, name), codec);
    }

    default void initialize(TameableDragonEntity dragon) {
    }

    default void close(TameableDragonEntity dragon) {
    }

    default void write(TameableDragonEntity dragon, NbtCompound nbt) {
    }

    default void read(TameableDragonEntity dragon, NbtCompound nbt) {
    }

    default void tick(TameableDragonEntity dragon) {
    }

    /**
     * Only called on the server
     */
    default void onMove(TameableDragonEntity dragon) {
    }

    /**
     * The Ability Factory is responsible for creating the instances of an ability.
     * If an ability is meant to be breed specific and needs no per-entity data, this interface can be implemented
     * directly by the ability class, and return itself in {@link Factory#create}. This effectively removes the need for
     * a new factory type instance that just returns a new 'effective singleton', which is unnecessary for memory.
     * Most abilities in DML are simplistic in nature and follow this approach. {@link FrostWalkerAbility} is a good
     * example.
     * <br><br>
     * It is crucially important that if you are extending a class already implementing {@link Factory} that
     * you override {@link Factory#create} and {@link Factory#type}. <br>
     * If the parent ability class is a breed specific type, and your type is an entity specific, {@link Factory#create} and
     * {@link Factory#type} do not matter since your codec SHOULD NOT USE THEM! You MUST create a new factory type for per-entity
     * implementations, or use {@link Ability#simpleFactory}
     */
    interface Factory<T extends Ability> {
        /**
         * @return an ability instance of type T
         */
        T create();

        Identifier type();
    }

    /**
     * While this is here, I don't necessarily recommend using it because the type has to be created to serialize
     * its values, but it's not exactly a deal-breaker at data-generation. Just... impractical. <br>
     * Example usage:
     * <pre>
     * {@code Codec<Factory<MyAbility>> CODEC = Codec.FLOAT
     *  .xmap(myFloat -> Ability.simpleFactory(() -> new MyAbility(myfloat)), myFactory -> myFactory.create().myFloat)
     *  .fieldOf("my_float")
     *  .codec();}
     * </pre>
     */
    static <T extends Ability> Factory<T> simpleFactory(Identifier id, Supplier<T> factory) {
        return new Factory<>() {
            @Override
            public T create() {
                return factory.get();
            }

            @Override
            public Identifier type() {
                return id;
            }
        };
    }
}
