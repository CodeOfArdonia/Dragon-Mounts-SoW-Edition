package com.iafenvoy.dragonmounts.render;

import com.iafenvoy.dragonmounts.dragon.breed.DragonBreed;
import com.iafenvoy.dragonmounts.dragon.egg.HatchableEggBlock;
import com.iafenvoy.dragonmounts.dragon.egg.HatchableEggBlockEntity;
import com.iafenvoy.dragonmounts.render.model.DragonEggModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class DragonEggRenderer implements BlockEntityRenderer<HatchableEggBlockEntity>, BuiltinItemRendererRegistry.DynamicItemRenderer {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private static final Identifier DRAGON_EGG = new Identifier(Identifier.DEFAULT_NAMESPACE, "block/dragon_egg");

    @Override
    public void render(HatchableEggBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        Identifier id = null;
        DragonBreed breed = entity.getBreed();
        if (breed != null && CLIENT.world != null)
            id = entity.getBreed().id(CLIENT.world.getRegistryManager());
        render(matrices, vertexConsumers, id, light, overlay);
    }

    @Override
    public void render(ItemStack stack, ModelTransformationMode modelTransformationMode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        RegistryKey<DragonBreed> breed = HatchableEggBlock.Item.get(stack);
        render(matrices, vertexConsumers, breed == null ? null : breed.getValue(), light, overlay);
    }

    private static void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, @Nullable Identifier breed, int light, int overlay) {
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
        matrices.translate(0.5, -1.5, -0.5);
        DragonEggModel model = new DragonEggModel(DragonEggModel.getTexturedModelData().createModel());
        Identifier id = breed == null ? DRAGON_EGG : breed.withPrefixedPath("textures/block/").withSuffixedPath("_dragon_egg.png");
        model.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutout(id)), light, overlay, 1, 1, 1, 1);
        matrices.pop();
    }
}
