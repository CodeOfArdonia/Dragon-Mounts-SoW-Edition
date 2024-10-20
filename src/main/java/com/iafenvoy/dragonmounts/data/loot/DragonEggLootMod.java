package com.iafenvoy.dragonmounts.data.loot;

import com.iafenvoy.dragonmounts.DragonMounts;
import com.iafenvoy.dragonmounts.dragon.breed.BreedRegistry;
import com.iafenvoy.dragonmounts.dragon.breed.DragonBreed;
import com.iafenvoy.dragonmounts.dragon.egg.HatchableEggBlock;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fabricators_of_create.porting_lib.loot.IGlobalLootModifier;
import io.github.fabricators_of_create.porting_lib.loot.LootModifier;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import static com.iafenvoy.dragonmounts.dragon.breed.DragonBreed.BuiltIn.*;

public class DragonEggLootMod extends LootModifier {
    public static final Codec<DragonEggLootMod> CODEC = RecordCodecBuilder.create(i -> codecStart(i)
            .and(Identifier.CODEC.fieldOf("egg_breed").forGetter(m -> m.id))
            .and(Codec.BOOL.optionalFieldOf("replace_first", false).forGetter(m -> m.replaceFirst))
            .apply(i, DragonEggLootMod::new));

    public record Target(RegistryKey<DragonBreed> forBreed, Identifier target, double chance) {
    }

    public static Target[] BUILT_IN_CHANCES = new Target[]{
            new Target(AETHER, LootTables.SIMPLE_DUNGEON_CHEST, 0.15),
            new Target(FIRE, LootTables.DESERT_PYRAMID_CHEST, 0.075),
            new Target(FOREST, LootTables.JUNGLE_TEMPLE_CHEST, 0.3),
            new Target(GHOST, LootTables.WOODLAND_MANSION_CHEST, 0.2),
            new Target(GHOST, LootTables.ABANDONED_MINESHAFT_CHEST, 0.095),
            new Target(ICE, LootTables.IGLOO_CHEST_CHEST, 0.2),
            new Target(NETHER, LootTables.BASTION_TREASURE_CHEST, 0.35),
            new Target(WATER, LootTables.BURIED_TREASURE_CHEST, 0.175)
    };

    private final Identifier id;
    private final boolean replaceFirst;

    public DragonEggLootMod(LootCondition[] conditions, Identifier breed, boolean replaceFirst) {
        super(conditions);
        this.id = breed;
        this.replaceFirst = replaceFirst;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        DynamicRegistryManager reg = context.getWorld().getRegistryManager();
        DragonBreed breed = BreedRegistry.registry(reg).get(this.id);
        if (breed != null) {
            ItemStack egg = HatchableEggBlock.Item.create(breed, reg);
            if (this.replaceFirst) generatedLoot.set(0, egg);
            else generatedLoot.add(egg);
        } else
            DragonMounts.LOGGER.error("Attempted to add a dragon egg to loot with unknown breed id: \"{}\"", this.id);
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
