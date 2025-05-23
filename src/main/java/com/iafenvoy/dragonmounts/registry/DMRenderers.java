package com.iafenvoy.dragonmounts.registry;

import com.iafenvoy.dragonmounts.particle.DragonParticle;
import com.iafenvoy.dragonmounts.render.DragonEggRenderer;
import com.iafenvoy.dragonmounts.render.DragonRenderer;
import com.iafenvoy.dragonmounts.render.model.DragonModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.impl.client.particle.ParticleFactoryRegistryImpl;
import net.fabricmc.fabric.impl.client.rendering.BlockEntityRendererRegistryImpl;

@Environment(EnvType.CLIENT)
public final class DMRenderers {
    public static void registerRenderers() {
        EntityRendererRegistry.register(DMEntities.DRAGON, DragonRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(DragonRenderer.MODEL_LOCATION, () -> DragonModel.createBodyLayer(DragonModel.Properties.STANDARD));
        BlockEntityRendererRegistryImpl.register(DMBlocks.EGG_BLOCK_ENTITY, ctx -> new DragonEggRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(DMBlocks.EGG_BLOCK, new DragonEggRenderer());

        ParticleFactoryRegistryImpl.INSTANCE.register(DMParticles.DRAGON_DROP, DragonParticle::provider);
        ParticleFactoryRegistryImpl.INSTANCE.register(DMParticles.DRAGON_DUST, DragonParticle::provider);
        ParticleFactoryRegistryImpl.INSTANCE.register(DMParticles.DRAGON_WIND, DragonParticle::provider);
    }
}
