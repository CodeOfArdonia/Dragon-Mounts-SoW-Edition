package com.iafenvoy.dragonmounts.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;

@Environment(EnvType.CLIENT)
public class DragonParticle extends SpriteBillboardParticle {
    protected DragonParticle(DragonParticleEffect data, ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider spriteSet) {
        super(world, x, y, z);
        int color = data.getColor();
        this.red = (float) ((color >> 16) & 0xFF) / 0xFF;
        this.green = (float) ((color >> 8) & 0xFF) / 0xFF;
        this.blue = (float) (color & 0xFF) / 0xFF;
        this.scale = data.getScale() * 0.3f;
        this.setBoundingBoxSpacing(0.2F, 0.2F);
        this.maxAge = 36;
        this.gravityStrength = 0.0F;
        this.collidesWithWorld = false;
        this.velocityX = vx;
        this.velocityY = vy;
        this.velocityZ = vz;
        this.setSpriteForAge(spriteSet);
    }

    public static DragonParticleProvider provider(SpriteProvider spriteSet) {
        return new DragonParticleProvider(spriteSet);
    }

    @Override
    public int getBrightness(float partialTick) {
        return 15728880;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_LIT;
    }

    public static class DragonParticleProvider implements ParticleFactory<DragonParticleEffect> {
        private final SpriteProvider spriteSet;

        public DragonParticleProvider(SpriteProvider spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(DragonParticleEffect typeIn, ClientWorld worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new DragonParticle(typeIn, worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
        }
    }
}