package com.iafenvoy.dragonmounts.render.model;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;

public class DragonEggModel extends EntityModel<Entity> {
    private final ModelPart bone;

    public DragonEggModel(ModelPart root) {
        this.bone = root.getChild("bone");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        modelPartData.addChild("bone", ModelPartBuilder.create()
                        .uv(6, 10).cuboid(-10.0F, -16.0F, 6.0F, 4.0F, 1.0F, 4.0F, new Dilation(0.0F))
                        .uv(5, 11).cuboid(-11.0F, -15.0F, 5.0F, 6.0F, 1.0F, 6.0F, new Dilation(0.0F))
                        .uv(4, 12).cuboid(-11.0F, -14.0F, 5.0F, 6.0F, 1.0F, 6.0F, new Dilation(0.0F))
                        .uv(3, 13).cuboid(-13.0F, -13.0F, 3.0F, 10.0F, 2.0F, 10.0F, new Dilation(0.0F))
                        .uv(2, 14).cuboid(-14.0F, -11.0F, 2.0F, 12.0F, 3.0F, 12.0F, new Dilation(0.0F))
                        .uv(1, 15).cuboid(-15.0F, -8.0F, 1.0F, 14.0F, 5.0F, 14.0F, new Dilation(0.0F))
                        .uv(2, 14).cuboid(-14.0F, -3.0F, 2.0F, 12.0F, 2.0F, 12.0F, new Dilation(0.0F))
                        .uv(3, 13).cuboid(-13.0F, -1.0F, 3.0F, 10.0F, 1.0F, 10.0F, new Dilation(0.0F)),
                ModelTransform.pivot(8.0F, 24.0F, -8.0F));
        return TexturedModelData.of(modelData, 16, 16);
    }

    @Override
    public void setAngles(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
        this.bone.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
    }
}