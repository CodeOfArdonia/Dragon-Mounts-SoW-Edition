package com.iafenvoy.dragonmounts.config;

import com.iafenvoy.dragonmounts.DragonMounts;
import com.iafenvoy.jupiter.config.container.AutoInitConfigContainer;
import com.iafenvoy.jupiter.config.entry.BooleanEntry;
import com.iafenvoy.jupiter.config.entry.DoubleEntry;
import com.iafenvoy.jupiter.interfaces.IConfigEntry;
import net.minecraft.util.Identifier;

public class DMClientConfig extends AutoInitConfigContainer {
    public static final String MOD_ID = DragonMounts.MOD_ID;
    public static final DMClientConfig INSTANCE = new DMClientConfig();
    public final Misc MISC = new Misc();
    public final Camera BACK = new Camera("back");
    public final Camera FRONT = new Camera("front");

    private DMClientConfig() {
        super(Identifier.of(MOD_ID, "client"), "config.%s.client.title".formatted(MOD_ID), "./config/dragon-mounts/client.json");
    }

    public static class Misc extends AutoInitConfigCategoryBase {
        public IConfigEntry<Boolean> cameraDrivenFlight = new BooleanEntry("config.%s.client.cameraDrivenFlight".formatted(MOD_ID), true).json("cameraDrivenFlight");
        public IConfigEntry<Boolean> thirdPersonOnMount = new BooleanEntry("config.%s.client.thirdPersonOnMount".formatted(MOD_ID), true).json("thirdPersonOnMount");

        public Misc() {
            super("misc", "config.%s.client.misc".formatted(MOD_ID));
        }
    }

    public static class Camera extends AutoInitConfigCategoryBase {
        public IConfigEntry<Double> distance = new DoubleEntry("config.%s.client.distance".formatted(MOD_ID), 6).json("distance");
        public IConfigEntry<Double> vertical = new DoubleEntry("config.%s.client.vertical".formatted(MOD_ID), 4).json("vertical");
        public IConfigEntry<Double> horizontal = new DoubleEntry("config.%s.client.horizontal".formatted(MOD_ID), 0).json("horizontal");

        public Camera(String id) {
            super(id, "config.%s.client.%s".formatted(MOD_ID, id));
        }
    }
}
