package com.iafenvoy.dragonmounts.habitats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public record FluidHabitat(float multiplier, TagKey<Fluid> fluidType) implements Habitat {
    public static final Codec<FluidHabitat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Habitat.withMultiplier(0.5f, FluidHabitat::multiplier),
            TagKey.unprefixedCodec(RegistryKeys.FLUID).fieldOf("fluid_tag").forGetter(FluidHabitat::fluidType)
    ).apply(instance, FluidHabitat::new));

    @Override
    public int getHabitatPoints(World world, BlockPos pos) {
        return (int) (BlockPos.stream(pos.add(1, 1, 1), pos.add(-1, -1, -1)).filter(p -> world.getFluidState(p).isIn(this.fluidType)).count() * this.multiplier);
    }

    @Override
    public Identifier type() {
        return Habitat.IN_FLUID;
    }
}
