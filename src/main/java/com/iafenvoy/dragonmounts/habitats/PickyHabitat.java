package com.iafenvoy.dragonmounts.habitats;

import com.mojang.serialization.Codec;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public record PickyHabitat(List<Habitat> habitats) implements Habitat {
    public static final Codec<PickyHabitat> CODEC = Habitat.CODEC
            .listOf()
            .fieldOf("required_habitats")
            .xmap(PickyHabitat::new, PickyHabitat::habitats)
            .codec();

    @Override
    public int getHabitatPoints(World world, BlockPos pos) {
        int points = 0;
        for (Habitat habitat : this.habitats) {
            int i = habitat.getHabitatPoints(world, pos);
            if (i == 0) return 0; // ALL habitat conditions must be met. Otherwise, nope.
            points += i;
        }
        return points;
    }

    @Override
    public Identifier type() {
        return Habitat.PICKY;
    }
}
