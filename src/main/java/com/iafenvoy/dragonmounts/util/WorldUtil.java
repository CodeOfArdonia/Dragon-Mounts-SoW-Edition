package com.iafenvoy.dragonmounts.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class WorldUtil {
    @Nullable
    public static EntityHitResult raycastNearest(LivingEntity entity, double maxDistance, double expand) {
        Vec3d p1 = entity.getEyePos(), p2 = p1.add(entity.getRotationVec(1).multiply(maxDistance)), r = new Vec3d(maxDistance, maxDistance, maxDistance);
        double e1 = maxDistance * maxDistance;
        Entity entity2 = null;
        Vec3d vec3d = null;
        for (Entity entity3 : entity.getWorld().getOtherEntities(entity, new Box(entity.getPos().add(r), entity.getPos().subtract(r)), e -> e instanceof LivingEntity)) {
            Box box2 = entity3.getBoundingBox().expand(entity3.getTargetingMargin()).expand(expand);
            Optional<Vec3d> optional = box2.raycast(p1, p2);
            if (box2.contains(p1)) {
                if (e1 >= 0.0) {
                    entity2 = entity3;
                    vec3d = optional.orElse(p1);
                    e1 = 0.0;
                }
            } else if (optional.isPresent()) {
                Vec3d vec3d2 = optional.get();
                double f = p1.squaredDistanceTo(vec3d2);
                if (f < e1 || e1 == 0.0) {
                    if (entity3.getRootVehicle() == entity.getRootVehicle()) {
                        if (e1 == 0.0) {
                            entity2 = entity3;
                            vec3d = vec3d2;
                        }
                    } else {
                        entity2 = entity3;
                        vec3d = vec3d2;
                        e1 = f;
                    }
                }
            }
        }
        if (entity2 == null) return null;
        return new EntityHitResult(entity2, vec3d);
    }
}
