package com.iafenvoy.dragonmounts.registry;

import com.iafenvoy.dragonmounts.DragonMounts;
import com.iafenvoy.dragonmounts.dragon.TameableDragon;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class DMEntities {
    public static final EntityType<TameableDragon> DRAGON = register("dragon", EntityType.Builder.create(TameableDragon::new, SpawnGroup.CREATURE).setDimensions(TameableDragon.BASE_WIDTH, TameableDragon.BASE_HEIGHT).maxTrackingRange(10).trackingTickInterval(3));

    private static <T extends Entity> EntityType<T> register(String id, EntityType.Builder<T> type) {
        return Registry.register(Registries.ENTITY_TYPE, new Identifier(DragonMounts.MOD_ID, id), type.build(id));
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(DMEntities.DRAGON, TameableDragon.createAttributes());
    }
}
