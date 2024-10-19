package com.iafenvoy.dragonmounts.registry;

import com.iafenvoy.dragonmounts.DragonMounts;
import com.iafenvoy.dragonmounts.dragon.DragonSpawnEgg;
import com.iafenvoy.dragonmounts.dragon.egg.HatchableEggBlock;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class DMItems {
    public static final Item SPAWN_EGG = register("spawn_egg", new DragonSpawnEgg());

    private static <T extends Item> T register(String id, T item) {
        return Registry.register(Registries.ITEM, new Identifier(DragonMounts.MOD_ID, id), item);
    }

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(DragonSpawnEgg::populateTab);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(HatchableEggBlock::populateTab);
    }
}