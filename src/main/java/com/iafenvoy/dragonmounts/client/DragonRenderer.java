package com.iafenvoy.dragonmounts.client;

import com.google.common.collect.ImmutableMap;
import com.iafenvoy.dragonmounts.DragonMounts;
import com.iafenvoy.dragonmounts.data.model.DragonModelPropertiesListener;
import com.iafenvoy.dragonmounts.dragon.TameableDragon;
import com.iafenvoy.dragonmounts.dragon.breed.DragonBreed;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class DragonRenderer extends MobEntityRenderer<TameableDragon, DragonModel> {
    public static final EntityModelLayer MODEL_LOCATION = new EntityModelLayer(Identifier.of(DragonMounts.MOD_ID, "dragon"), "main");
    private static final Identifier[] DEFAULT_TEXTURES = computeTextureCacheFor(DragonBreed.BuiltIn.END.getValue());
    private static final Identifier DISSOLVE_TEXTURE = Identifier.of(DragonMounts.MOD_ID, "textures/entity/dragon/dissolve.png");
    private static final int LAYER_BODY = 0;
    private static final int LAYER_GLOW = 1;
    private static final int LAYER_SADDLE = 2;

    private final DragonModel defaultModel;
    private final Map<Identifier, DragonModel> modelCache;
    private final Map<Identifier, Identifier[]> textureCache = new HashMap<>(8);

    public DragonRenderer(EntityRendererFactory.Context modelBakery) {
        super(modelBakery, new DragonModel(modelBakery.getPart(MODEL_LOCATION)), 2);

        this.defaultModel = this.model;
        this.modelCache = this.bakeModels(modelBakery);

        this.addFeature(this.GLOW_LAYER);
        this.addFeature(this.SADDLE_LAYER);
        this.addFeature(this.DEATH_LAYER);
    }

    @Override
    public boolean shouldRender(TameableDragon dragon, Frustum pCamera, double pCamX, double pCamY, double pCamZ) {
        return dragon.getBreed() != null && super.shouldRender(dragon, pCamera, pCamX, pCamY, pCamZ);
    }

    @Override
    public void render(TameableDragon dragon, float pEntityYaw, float pPartialTicks, MatrixStack pMatrixStack, VertexConsumerProvider pBuffer, int pPackedLight) {
        this.model = this.getModel(dragon);
        super.render(dragon, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
    }

    @SuppressWarnings("ConstantConditions")
    private DragonModel getModel(TameableDragon dragon) {
        DragonBreed breed = dragon.getBreed();
        if (breed == null) return this.defaultModel;

        DragonModel selected = this.modelCache.get(breed.id(MinecraftClient.getInstance().world.getRegistryManager()));
        if (selected == null) return this.defaultModel;

        return selected;
    }

    // During death, do not use the standard rendering and let the death layer handle it. Hacky, but better than mixins.
    @Nullable
    @Override
    protected RenderLayer getRenderLayer(TameableDragon entity, boolean visible, boolean invisToClient, boolean glowing) {
        return entity.deathTime > 0 ? null : super.getRenderLayer(entity, visible, invisToClient, glowing);
    }

    @Override
    public Identifier getTexture(TameableDragon dragon) {
        return this.getTextureForLayer(dragon.getBreed(), LAYER_BODY);
    }

    @SuppressWarnings("ConstantConditions")
    public Identifier getTextureForLayer(@Nullable DragonBreed breed, int layer) {
        if (breed == null) return DEFAULT_TEXTURES[layer];

        // we need to compute texture locations now rather than earlier due to the fact that breeds don't exist then.
        return this.textureCache.computeIfAbsent(breed.id(MinecraftClient.getInstance().world.getRegistryManager()), DragonRenderer::computeTextureCacheFor)[layer];
    }

    @Override
    protected void setupTransforms(TameableDragon dragon, MatrixStack ps, float age, float yaw, float partials) {
        super.setupTransforms(dragon, ps, age, yaw, partials);
        DragonAnimator animator = dragon.getAnimator();
        float scale = dragon.getScaleFactor();
        ps.scale(scale, scale, scale);
        ps.translate(animator.getModelOffsetX(), animator.getModelOffsetY(), animator.getModelOffsetZ());
        ps.translate(0, 1.5, 0.5); // change rotation point
        ps.multiply(RotationAxis.POSITIVE_X.rotationDegrees(animator.getModelPitch(partials))); // rotate near the saddle so we can support the player
        ps.translate(0, -1.5, -0.5); // restore rotation point
    }

    // dragons dissolve during death, not flip.
    @Override
    protected float getLyingAngle(TameableDragon pLivingEntity) {
        return 0;
    }

    private Map<Identifier, DragonModel> bakeModels(EntityRendererFactory.Context bakery) {
        ImmutableMap.Builder<Identifier, DragonModel> builder = ImmutableMap.builder();
        for (Map.Entry<Identifier, EntityModelLayer> entry : DragonModelPropertiesListener.INSTANCE.pollDefinitions().entrySet())
            builder.put(entry.getKey(), new DragonModel(bakery.getPart(entry.getValue())));
        return builder.build();
    }

    private static Identifier[] computeTextureCacheFor(Identifier breedId) {
        final String[] TEXTURES = {"body", "glow", "saddle"}; // 0, 1, 2

        Identifier[] cache = new Identifier[TEXTURES.length];
        for (int i = 0; i < TEXTURES.length; i++)
            cache[i] = new Identifier(breedId.getNamespace(), "textures/entity/dragon/" + breedId.getPath() + "/" + TEXTURES[i] + ".png");
        return cache;
    }

    public final FeatureRenderer<TameableDragon, DragonModel> GLOW_LAYER = new FeatureRenderer<>(this) {
        @Override
        public void render(MatrixStack pMatrixStack, VertexConsumerProvider buffer, int pPackedLight, TameableDragon dragon, float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
            if (dragon.deathTime == 0) {
                RenderLayer type = CustomRenderTypes.glow(DragonRenderer.this.getTextureForLayer(dragon.getBreed(), LAYER_GLOW));
                DragonRenderer.this.model.render(pMatrixStack, buffer.getBuffer(type), pPackedLight, OverlayTexture.DEFAULT_UV, 1f, 1f, 1f, 1f);
            }
        }
    };
    public final FeatureRenderer<TameableDragon, DragonModel> SADDLE_LAYER = new FeatureRenderer<>(this) {
        @Override
        public void render(MatrixStack ps, VertexConsumerProvider buffer, int light, TameableDragon dragon, float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
            if (dragon.isSaddled())
                renderModel(DragonRenderer.this.model, DragonRenderer.this.getTextureForLayer(dragon.getBreed(), LAYER_SADDLE), ps, buffer, light, dragon, 1f, 1f, 1f);
        }
    };
    public final FeatureRenderer<TameableDragon, DragonModel> DEATH_LAYER = new FeatureRenderer<>(this) {
        @Override
        public void render(MatrixStack ps, VertexConsumerProvider buffer, int light, TameableDragon dragon, float limbSwing, float limbSwingAmount, float partials, float age, float yaw, float pitch) {
            if (dragon.deathTime > 0) {
                float delta = dragon.deathTime / (float) dragon.getMaxDeathTime();
                DragonRenderer.this.model.render(ps, buffer.getBuffer(CustomRenderTypes.DISSOLVE), light, OverlayTexture.DEFAULT_UV, 1f, 1f, 1f, delta);
                DragonRenderer.this.model.render(ps, buffer.getBuffer(RenderLayer.getEntityDecal(this.getTexture(dragon))), light, OverlayTexture.getUv(0, true), 1f, 1f, 1f, 1f);
            }
        }
    };

    private static class CustomRenderTypes extends RenderLayer {
        private static final RenderLayer DISSOLVE = RenderLayer.getEntityAlpha(DISSOLVE_TEXTURE);
        private static final Function<Identifier, RenderLayer> GLOW_FUNC = Util.memoize(texture -> of("eyes", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, false, true, MultiPhaseParameters.builder()
                .program(EYES_PROGRAM)
                .texture(new Texture(texture, false, false))
                .transparency(TRANSLUCENT_TRANSPARENCY)
                .writeMaskState(COLOR_MASK)
                .build(false)));

        private static RenderLayer glow(Identifier texture) {
            return GLOW_FUNC.apply(texture);
        }

        private CustomRenderTypes() {
            // dummy
            super(null, null, null, 0, false, true, null, null);
        }
    }
}
