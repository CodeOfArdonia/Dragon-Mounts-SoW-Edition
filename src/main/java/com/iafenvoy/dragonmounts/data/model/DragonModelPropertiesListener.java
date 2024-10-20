package com.iafenvoy.dragonmounts.data.model;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.iafenvoy.dragonmounts.DragonMounts;
import com.iafenvoy.dragonmounts.client.DragonModel;
import com.iafenvoy.dragonmounts.client.DragonRenderer;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.profiler.Profiler;

import java.util.HashMap;
import java.util.Map;

public class DragonModelPropertiesListener extends JsonDataLoader implements IdentifiableResourceReloadListener {
    public static final DragonModelPropertiesListener INSTANCE = new DragonModelPropertiesListener();

    private static final String FOLDER = "models/entity/dragon/breed/properties";

    private final Map<Identifier, EntityModelLayer> definitions = new HashMap<>(3);

    public DragonModelPropertiesListener() {
        super(new GsonBuilder().create(), FOLDER);
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> map, ResourceManager pResourceManager, Profiler pProfiler) {
        this.definitions.clear();
        for (Map.Entry<Identifier, JsonElement> entry : map.entrySet()) {
            Identifier breedId = entry.getKey();
            DragonModel.Properties properties = DragonModel.Properties.CODEC.parse(JsonOps.INSTANCE, entry.getValue()).getOrThrow(false, Util.addPrefix("Unable to parse Dragon Breed Properties: " + breedId, DragonMounts.LOGGER::error));
            EntityModelLayer modelLoc = new EntityModelLayer(DragonRenderer.MODEL_LOCATION.getId(), breedId.toString());
            EntityModelLayerRegistry.registerModelLayer(modelLoc, () -> DragonModel.createBodyLayer(properties));
            this.definitions.put(entry.getKey(), modelLoc);
        }
    }

    /**
     * Gets and clears this listener's model definitions.
     */
    public Map<Identifier, EntityModelLayer> pollDefinitions() {
        Map<Identifier, EntityModelLayer> map = Map.copyOf(this.definitions);
        this.definitions.clear();
        return map;
    }

    @Override
    public Identifier getFabricId() {
        return new Identifier(DragonMounts.MOD_ID, "dragon_model_properties");
    }
}
