package com.iafenvoy.dragonmounts.habitats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public record NearbyBlocksHabitat(float multiplier, TagKey<Block> tag) implements Habitat {
    public static final Codec<NearbyBlocksHabitat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Habitat.withMultiplier(0.5f, NearbyBlocksHabitat::multiplier),
            TagKey.unprefixedCodec(RegistryKeys.BLOCK).fieldOf("block_tag").forGetter(NearbyBlocksHabitat::tag)
    ).apply(instance, NearbyBlocksHabitat::new));

    @Override
    public int getHabitatPoints(World world, BlockPos basePos) {
        return (int) (BlockPos.stream(basePos.add(1, 1, 1), basePos.add(-1, -1, -1)).filter(p -> world.getBlockState(p).isIn(this.tag)).count() * this.multiplier);
    }

    @Override
    public Identifier type() {
        return Habitat.NEARBY_BLOCKS;
    }
}
