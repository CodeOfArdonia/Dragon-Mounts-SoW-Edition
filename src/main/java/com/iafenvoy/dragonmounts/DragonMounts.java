package com.iafenvoy.dragonmounts;

import com.iafenvoy.dragonmounts.data.CrossBreedingManager;
import com.iafenvoy.dragonmounts.dragon.breed.BreedRegistry;
import com.iafenvoy.dragonmounts.dragon.breed.DragonBreed;
import com.iafenvoy.dragonmounts.dragon.egg.HatchableEggBlock;
import com.iafenvoy.dragonmounts.registry.*;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.block.Blocks;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DragonMounts implements ModInitializer {
    public static final String MOD_ID = "dragon_mounts";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ForgeConfigRegistry.INSTANCE.register(MOD_ID, ModConfig.Type.COMMON, DMLConfig.COMMON_SPEC);
        DMBlocks.init();
        DMEntities.init();
        DMItems.init();
        DMLoots.init();
        DMSounds.init();
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(CrossBreedingManager.INSTANCE);
        DynamicRegistries.registerSynced(BreedRegistry.REGISTRY_KEY, DragonBreed.CODEC, DragonBreed.NETWORK_CODEC, DynamicRegistries.SyncOption.SKIP_WHEN_EMPTY);
        UseBlockCallback.EVENT.register((player, world, hand, blockHitResult) -> {
            BlockPos pos = blockHitResult.getBlockPos();
            if (DMLConfig.allowEggOverride() && world.getBlockState(pos).isOf(Blocks.DRAGON_EGG)) {
                var end = BreedRegistry.registry(world.getRegistryManager()).getOrEmpty(DragonBreed.BuiltIn.END);
                if (end.isPresent()) {
                    if (world instanceof ServerWorld serverWorld) {
                        var state = DMBlocks.EGG_BLOCK.getDefaultState().with(HatchableEggBlock.HATCHING, true);
                        HatchableEggBlock.place(serverWorld, pos, state, end.get());
                    }
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });
    }
}