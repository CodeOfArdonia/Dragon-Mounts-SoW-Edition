package com.iafenvoy.dragonmounts.habitats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public record BiomeHabitat(int points, TagKey<Biome> biomeTag) implements Habitat {
    public static final Codec<BiomeHabitat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Habitat.withPoints(2, BiomeHabitat::points),
            TagKey.unprefixedCodec(RegistryKeys.BIOME).fieldOf("biome_tag").forGetter(BiomeHabitat::biomeTag)
    ).apply(instance, BiomeHabitat::new));

    @Override
    public int getHabitatPoints(World level, BlockPos pos) {
        return level.getBiome(pos).isIn(this.biomeTag) ? this.points : 0;
    }

    @Override
    public Identifier type() {
        return Habitat.BIOMES;
    }
}
