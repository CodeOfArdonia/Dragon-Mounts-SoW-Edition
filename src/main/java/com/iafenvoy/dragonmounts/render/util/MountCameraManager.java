package com.iafenvoy.dragonmounts.render.util;

import com.iafenvoy.dragonmounts.config.DMClientConfig;
import com.iafenvoy.dragonmounts.dragon.TameableDragon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;

public class MountCameraManager {
    private static Perspective previousPerspective = Perspective.FIRST_PERSON;

    public static void onDragonMount() {
        if (DMClientConfig.INSTANCE.MISC.thirdPersonOnMount.getValue()) {
            previousPerspective = MinecraftClient.getInstance().options.getPerspective();
            MinecraftClient.getInstance().options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }
    }

    public static void onDragonDismount() {
        if (DMClientConfig.INSTANCE.MISC.thirdPersonOnMount.getValue())
            MinecraftClient.getInstance().options.setPerspective(previousPerspective);
    }

    @SuppressWarnings("ConstantConditions") // player should never be null at time of calling
    public static void setMountCameraAngles(Camera camera) {
        if (MinecraftClient.getInstance().player.getVehicle() instanceof TameableDragon && !MinecraftClient.getInstance().options.getPerspective().isFirstPerson()) {
            DMClientConfig.Camera offset = MinecraftClient.getInstance().options.getPerspective() == Perspective.THIRD_PERSON_BACK ? DMClientConfig.INSTANCE.BACK : DMClientConfig.INSTANCE.FRONT;
            camera.moveBy(0, offset.vertical.getValue(), offset.horizontal.getValue());
            camera.moveBy(-camera.clipToSpace(offset.distance.getValue()), 0, 0); // do distance calcs AFTER our new position is set
        }
    }
}
