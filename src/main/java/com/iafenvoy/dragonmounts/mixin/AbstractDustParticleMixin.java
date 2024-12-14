package com.iafenvoy.dragonmounts.mixin;

import com.iafenvoy.dragonmounts.DMConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.AbstractDustParticle;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.AbstractDustParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(AbstractDustParticle.class)
public abstract class AbstractDustParticleMixin extends SpriteBillboardParticle {
    protected AbstractDustParticleMixin(ClientWorld clientWorld, double d, double e, double f) {
        super(clientWorld, d, e, f);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, AbstractDustParticleEffect parameters, SpriteProvider spriteProvider, CallbackInfo ci) {
        if (DMConstants.shouldForceParticleSpeed) {
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.velocityZ = velocityZ;
        }
    }
}
