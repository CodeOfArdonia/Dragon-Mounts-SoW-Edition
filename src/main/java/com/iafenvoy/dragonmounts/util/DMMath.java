package com.iafenvoy.dragonmounts.util;

import net.minecraft.util.math.Vec3d;

public class DMMath {
    public static Vec3d getRotationVector(float pitch, float yaw) {
        double f = Math.toRadians(pitch);
        double g = -Math.toRadians(yaw);
        double h = Math.cos(g);
        double i = Math.sin(g);
        double j = Math.cos(f);
        double k = Math.sin(f);
        return new Vec3d(i * j, -k, h * j);
    }

    public static Vec3d getRotationVectorUnit(float pitch, float yaw) {
        return toUnit(getRotationVector(pitch, yaw));
    }

    public static Vec3d toUnit(Vec3d origin) {
        return origin.length() == 0 ? origin : origin.multiply(1 / origin.length());
    }
}
