package com.iafenvoy.dragonmounts.client;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.iafenvoy.dragonmounts.dragon.egg.HatchableEggBlock;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A dynamic BakedModel which returns quads based on the given breed of the tile entity.
 */
public class DragonEggModel implements IUnbakedGeometry<DragonEggModel> {
    private final ImmutableMap<String, JsonUnbakedModel> models;

    public DragonEggModel(ImmutableMap<String, JsonUnbakedModel> models) {
        this.models = models;
    }

    @Override
    public BakedModel bake(JsonUnbakedModel jsonUnbakedModel, Baker baker, Function<SpriteIdentifier, Sprite> spriteGetter, ModelBakeSettings modelState, ModelOverrideList overrides, Identifier modelLocation, boolean b) {
        var baked = ImmutableMap.<String, BakedModel>builder();
        for (var entry : this.models.entrySet()) {
            var unbaked = entry.getValue();
            unbaked.setParents(baker::getOrLoadModel);
            baked.put(entry.getKey(), unbaked.bake(baker, unbaked, spriteGetter, modelState, modelLocation, true));
        }
        return new Baked(baked.build(), overrides);
    }

    public static class Baked implements BakedModel {
        private static final Supplier<BakedModel> FALLBACK = Suppliers.memoize(() -> MinecraftClient.getInstance().getBlockRenderManager().getModel(Blocks.DRAGON_EGG.getDefaultState()));

        private final ImmutableMap<String, BakedModel> models;
        private final ModelOverrideList overrides;

        public Baked(ImmutableMap<String, BakedModel> models, ModelOverrideList overrides) {
            this.models = models;
            this.overrides = new ItemModelResolver(this, overrides);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
            return FALLBACK.get().getQuads(state, face, random);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }

        @Override
        public boolean hasDepth() {
            return true;
        }

        @Override
        public boolean isSideLit() {
            return true;
        }

        @Override
        public boolean isBuiltin() {
            return false;
        }

        @Override
        public Sprite getParticleSprite() {
            return FALLBACK.get().getParticleSprite();
        }

        @Override
        public ModelTransformation getTransformation() {
            return FALLBACK.get().getTransformation();
        }

        @Override
        public ModelOverrideList getOverrides() {
            return this.overrides;
        }
    }

    public static class ItemModelResolver extends ModelOverrideList {
        private final Baked owner;
        private final ModelOverrideList nested;

        public ItemModelResolver(Baked owner, ModelOverrideList nested) {
            this.owner = owner;
            this.nested = nested;
        }

        @Nullable
        @Override
        public BakedModel apply(BakedModel original, ItemStack stack, @Nullable ClientWorld level, @Nullable LivingEntity entity, int pSeed) {
            var override = this.nested.apply(original, stack, level, entity, pSeed);
            if (override != original) return override;

            var tag = BlockItem.getBlockEntityNbt(stack);
            if (tag != null) {
                var model = this.owner.models.get(tag.getString(HatchableEggBlock.NBT_BREED));
                if (model != null) return model;
            }

            return original;
        }
    }

    public static class Loader implements IGeometryLoader<DragonEggModel> {
        public static final Loader INSTANCE = new Loader();

        private Loader() {
        }

        @Override
        public DragonEggModel read(JsonObject jsonObject, JsonDeserializationContext deserializer) throws JsonParseException {
            var models = ImmutableMap.<String, JsonUnbakedModel>builder();
            var dir = "models/block/dragon_eggs";
            var length = "models/".length();
            var suffixLength = ".json".length();
            for (var entry : MinecraftClient.getInstance().getResourceManager().findResources(dir, f -> f.getPath().endsWith(".json")).entrySet()) {
                var rl = entry.getKey();
                var path = rl.getPath();
                path = path.substring(length, path.length() - suffixLength);
                var id = String.format("%s:%s", rl.getNamespace(), path.substring("block/dragon_eggs/".length(), path.length() - "_dragon_egg".length()));

                try (var reader = entry.getValue().getReader()) {
                    models.put(id, JsonUnbakedModel.deserialize(reader));
                } catch (IOException e) {
                    throw new JsonParseException(e);
                }
            }

            return new DragonEggModel(models.build());
        }
    }
}
