package com.iafenvoy.dragonmounts.client;

import com.iafenvoy.dragonmounts.config.DMConfig;
import com.iafenvoy.dragonmounts.dragon.TameableDragon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;

public class MountCameraManager {
    private static Perspective previousPerspective = Perspective.FIRST_PERSON;

    public static void onDragonMount() {
        if (DMConfig.getClientConfig().thirdPersonOnMount) {
            previousPerspective = MinecraftClient.getInstance().options.getPerspective();
            MinecraftClient.getInstance().options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }
    }

    public static void onDragonDismount() {
        if (DMConfig.getClientConfig().thirdPersonOnMount)
            MinecraftClient.getInstance().options.setPerspective(previousPerspective);
    }

    @SuppressWarnings("ConstantConditions") // player should never be null at time of calling
    public static void setMountCameraAngles(Camera camera) {
        if (MinecraftClient.getInstance().player.getVehicle() instanceof TameableDragon && !MinecraftClient.getInstance().options.getPerspective().isFirstPerson()) {
            DMConfig.CameraOffset offset = MinecraftClient.getInstance().options.getPerspective() == Perspective.THIRD_PERSON_BACK ? DMConfig.getClientConfig().backCameraOffset : DMConfig.getClientConfig().frontCameraOffset;
            camera.moveBy(0, offset.vertical, offset.horizontal);
            camera.moveBy(-camera.clipToSpace(offset.distance), 0, 0); // do distance calcs AFTER our new position is set
        }
    }
}
