package com.iafenvoy.dragonmounts.client;

import com.iafenvoy.dragonmounts.DMLConfig;
import com.iafenvoy.dragonmounts.dragon.TameableDragon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraftforge.common.ForgeConfigSpec;

public class MountCameraManager {
    private static Perspective previousPerspective = Perspective.FIRST_PERSON;

    public static void onDragonMount() {
        if (DMLConfig.thirdPersonOnMount()) {
            previousPerspective = MinecraftClient.getInstance().options.getPerspective();
            MinecraftClient.getInstance().options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }
    }

    public static void onDragonDismount() {
        if (DMLConfig.thirdPersonOnMount())
            MinecraftClient.getInstance().options.setPerspective(previousPerspective);
    }

    @SuppressWarnings("ConstantConditions") // player should never be null at time of calling
    public static void setMountCameraAngles(Camera camera) {
        if (MinecraftClient.getInstance().player.getVehicle() instanceof TameableDragon && !MinecraftClient.getInstance().options.getPerspective().isFirstPerson()) {
            ForgeConfigSpec.DoubleValue[] offsets = DMLConfig.getCameraPerspectiveOffset(MinecraftClient.getInstance().options.getPerspective() == Perspective.THIRD_PERSON_BACK);
            camera.moveBy(0, offsets[1].get(), offsets[2].get());
            camera.moveBy(-camera.clipToSpace(offsets[0].get()), 0, 0); // do distance calcs AFTER our new position is set
        }
    }
}
