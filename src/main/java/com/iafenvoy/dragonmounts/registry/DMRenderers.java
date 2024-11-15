package com.iafenvoy.dragonmounts.registry;

import com.iafenvoy.dragonmounts.client.DragonModel;
import com.iafenvoy.dragonmounts.client.DragonRenderer;
import com.iafenvoy.dragonmounts.render.DragonEggBlockEntityRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.impl.client.rendering.BlockEntityRendererRegistryImpl;

@Environment(EnvType.CLIENT)
public class DMRenderers {
    public static void registerRenderers() {
        EntityRendererRegistry.register(DMEntities.DRAGON, DragonRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(DragonRenderer.MODEL_LOCATION, () -> DragonModel.createBodyLayer(DragonModel.Properties.STANDARD));
        BlockEntityRendererRegistryImpl.register(DMBlocks.EGG_BLOCK_ENTITY, ctx -> new DragonEggBlockEntityRenderer());
    }
}
