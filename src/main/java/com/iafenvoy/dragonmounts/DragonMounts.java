package com.iafenvoy.dragonmounts;

import com.iafenvoy.dragonmounts.config.DMConfig;
import com.iafenvoy.dragonmounts.data.CrossBreedingManager;
import com.iafenvoy.dragonmounts.loot.LootProcessor;
import com.iafenvoy.dragonmounts.dragon.breed.BreedRegistry;
import com.iafenvoy.dragonmounts.dragon.breed.DragonBreed;
import com.iafenvoy.dragonmounts.dragon.egg.HatchableEggBlock;
import com.iafenvoy.dragonmounts.registry.DMBlocks;
import com.iafenvoy.dragonmounts.registry.DMEntities;
import com.iafenvoy.dragonmounts.registry.DMItems;
import com.iafenvoy.dragonmounts.registry.DMSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class DragonMounts implements ModInitializer {
    public static final String MOD_ID = "dragon_mounts";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        DMBlocks.init();
        DMEntities.init();
        DMItems.init();
        DMSounds.init();
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(CrossBreedingManager.INSTANCE);
        DynamicRegistries.registerSynced(BreedRegistry.REGISTRY_KEY, DragonBreed.CODEC, DragonBreed.NETWORK_CODEC);
        UseBlockCallback.EVENT.register((player, world, hand, blockHitResult) -> {
            BlockPos pos = blockHitResult.getBlockPos();
            if (DMConfig.COMMON.allowEggOverride && world.getBlockState(pos).isOf(Blocks.DRAGON_EGG)) {
                Optional<DragonBreed> end = BreedRegistry.registry(world.getRegistryManager()).getOrEmpty(DragonBreed.BuiltIn.END);
                if (end.isPresent()) {
                    if (world instanceof ServerWorld serverWorld) {
                        BlockState state = DMBlocks.EGG_BLOCK.getDefaultState().with(HatchableEggBlock.HATCHING, true);
                        HatchableEggBlock.place(serverWorld, pos, state, end.get());
                    }
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });
        LootProcessor.init();
    }
}