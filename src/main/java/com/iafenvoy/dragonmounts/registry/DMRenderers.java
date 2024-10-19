package com.iafenvoy.dragonmounts.registry;

import com.iafenvoy.dragonmounts.client.DragonModel;
import com.iafenvoy.dragonmounts.client.DragonRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

@Environment(EnvType.CLIENT)
public class DMRenderers {
    public static void registerRenderers() {
        EntityRendererRegistry.register(DMEntities.DRAGON, DragonRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(DragonRenderer.MODEL_LOCATION, () -> DragonModel.createBodyLayer(DragonModel.Properties.STANDARD));
    }
}
