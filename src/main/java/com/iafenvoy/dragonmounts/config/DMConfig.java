package com.iafenvoy.dragonmounts.config;

import java.util.HashMap;
import java.util.Map;

import static com.iafenvoy.dragonmounts.dragon.breed.DragonBreed.BuiltIn.*;

public class DMConfig {
    private static final Common COMMON = ConfigLoader.load(Common.class, "./config/dragon_mounts/common.json", new Common());
    private static final Client CLIENT = ConfigLoader.load(Client.class, "./config/dragon_mounts/client.json", new Client());

    public static Common getCommonConfig() {
        return COMMON;
    }

    public static Client getClientConfig() {
        return CLIENT;
    }

    public static class Common {
        public boolean allowEggOverride = true;
        public boolean replenishEggs = true;
        public boolean updateHabitats = true;
        //egg chance
        public Map<String, Float> eggGenerateChance = new HashMap<>();
        //-1 = No Limit, 0 = Cannot breed, 2 = Can breed only two times
        public Map<String, Integer> reproduceLimit = new HashMap<>();

        public Common() {
            eggGenerateChance.put(AETHER.getValue().toString(), 0.15F);
            eggGenerateChance.put(FIRE.getValue().toString(), 0.075F);
            eggGenerateChance.put(FOREST.getValue().toString(), 0.3F);
            eggGenerateChance.put(GHOST.getValue().toString(), 0.095F);
            eggGenerateChance.put(ICE.getValue().toString(), 0.2F);
            eggGenerateChance.put(NETHER.getValue().toString(), 0.35F);
            eggGenerateChance.put(WATER.getValue().toString(), 0.175F);
            reproduceLimit.put(AETHER.getValue().toString(), -1);
            reproduceLimit.put(FIRE.getValue().toString(), -1);
            reproduceLimit.put(FOREST.getValue().toString(), -1);
            reproduceLimit.put(GHOST.getValue().toString(), -1);
            reproduceLimit.put(ICE.getValue().toString(), -1);
            reproduceLimit.put(NETHER.getValue().toString(), -1);
            reproduceLimit.put(WATER.getValue().toString(), -1);
        }

        public int getReproduceLimit(String type) {
            return this.reproduceLimit.getOrDefault(type, -1);
        }
    }

    public static class Client {
        public boolean cameraDrivenFlight = true;
        public boolean thirdPersonOnMount = true;
        public CameraOffset backCameraOffset = new CameraOffset();
        public CameraOffset frontCameraOffset = new CameraOffset();
    }

    public static class CameraOffset {
        public double distance = 6;
        public double vertical = 4;
        public double horizontal = 0;
    }
}
