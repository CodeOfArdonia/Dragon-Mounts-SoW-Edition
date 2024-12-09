package com.iafenvoy.dragonmounts.mixin;

import com.iafenvoy.dragonmounts.render.util.ModelPartAccess;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.class)
public class ModelPartMixin implements ModelPartAccess {
    @Unique
    public float dm_xScale = 1;
    @Unique
    public float dm_yScale = 1;
    @Unique
    public float dm_zScale = 1;

    @Inject(method = "rotate(Lnet/minecraft/client/util/math/MatrixStack;)V", at = @At(value = "TAIL"))
    public void dragonmounts_scalePoseStack(MatrixStack pPoseStack, CallbackInfo cbi) {
        pPoseStack.scale(this.dm_xScale, this.dm_yScale, this.dm_zScale);
    }

    @Override
    public float getXScale() {
        return this.dm_xScale;
    }

    @Override
    public float getYScale() {
        return this.dm_yScale;
    }

    @Override
    public float getZScale() {
        return this.dm_zScale;
    }

    @Override
    public void setXScale(float x) {
        this.dm_xScale = x;
    }

    @Override
    public void setYScale(float y) {
        this.dm_yScale = y;
    }

    @Override
    public void setZScale(float z) {
        this.dm_zScale = z;
    }
}
