package com.iafenvoy.dragonmounts;

import com.iafenvoy.dragonmounts.client.DragonEggModel;
import com.iafenvoy.dragonmounts.client.MountControlsMessenger;
import com.iafenvoy.dragonmounts.data.model.DragonModelPropertiesListener;
import com.iafenvoy.dragonmounts.dragon.DragonSpawnEgg;
import com.iafenvoy.dragonmounts.dragon.breed.BreedRegistry;
import com.iafenvoy.dragonmounts.dragon.breed.DragonBreed;
import com.iafenvoy.dragonmounts.dragon.egg.HatchableEggBlockEntity;
import com.iafenvoy.dragonmounts.registry.DMItems;
import com.iafenvoy.dragonmounts.registry.DMKeyBindings;
import com.iafenvoy.dragonmounts.registry.DMRenderers;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import io.github.fabricators_of_create.porting_lib.models.geometry.RegisterGeometryLoadersCallback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.config.ModConfig;

public class DragonMountsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ForgeConfigRegistry.INSTANCE.register(DragonMounts.MOD_ID, ModConfig.Type.CLIENT, DMLConfig.CLIENT_SPEC);
        DMKeyBindings.init();
        ColorProviderRegistry.ITEM.register(DragonSpawnEgg::getColor, DMItems.SPAWN_EGG);
        DMRenderers.registerRenderers();
        ClientTickEvents.END_CLIENT_TICK.register(MountControlsMessenger::tick);
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(DragonModelPropertiesListener.INSTANCE);
        RegisterGeometryLoadersCallback.EVENT.register(map -> map.put(new Identifier(DragonMounts.MOD_ID, "dragon_egg"), DragonEggModel.Loader.INSTANCE));
        ClientPlayNetworking.registerGlobalReceiver(Static.DRAGON_EGG_TYPE_SYNC, (client, handler, buf, sender) -> {
            BlockPos pos = buf.readBlockPos();
            String type = buf.readString();
            client.execute(() -> {
                DragonBreed breed = handler.getRegistryManager().get(BreedRegistry.REGISTRY_KEY).get(Identifier.tryParse(type));
                if (client.world != null && client.world.getBlockEntity(pos) instanceof HatchableEggBlockEntity blockEntity)
                    blockEntity.setBreed(() -> breed);
            });
        });
    }
}
