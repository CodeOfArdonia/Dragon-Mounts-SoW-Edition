package com.iafenvoy.dragonmounts.render;

import com.iafenvoy.dragonmounts.client.DragonEggModel;
import com.iafenvoy.dragonmounts.dragon.breed.DragonBreed;
import com.iafenvoy.dragonmounts.dragon.egg.HatchableEggBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.random.CheckedRandom;

@Environment(EnvType.CLIENT)
public class DragonEggBlockEntityRenderer implements BlockEntityRenderer<HatchableEggBlockEntity> {
    @Override
    public void render(HatchableEggBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        matrices.push();
//        matrices.translate(-0.005f, -0.005f, -0.005f);
//        matrices.scale(1.01f, 1.01f, 1.01f);
        DragonBreed breed = entity.getBreed();
        MinecraftClient client = MinecraftClient.getInstance();
        BakedModel model = DragonEggModel.getModel(breed != null && client.world != null ? breed.id(client.world.getRegistryManager()).toString() : "");
        for (BakedQuad quad : model.getQuads(null, null, new CheckedRandom(0)))
            vertexConsumers.getBuffer(RenderLayers.getBlockLayer(Blocks.DRAGON_EGG.getDefaultState())).quad(matrices.peek(), quad, 1, 1, 1, light, overlay);
        matrices.pop();
    }
}
