package com.iafenvoy.dragonmounts.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class WorldUtil {
    public static List<Entity> raycastAll(LivingEntity entity, double maxDistance, double expand, Predicate<Entity> except) {
        Vec3d p1 = entity.getEyePos();
        Vec3d p2 = p1.add(entity.getRotationVec(1).multiply(maxDistance));
        Vec3d r = new Vec3d(maxDistance, maxDistance, maxDistance);
        List<Entity> resultList = new ArrayList<>();
        Box searchBox = new Box(entity.getPos().add(r), entity.getPos().subtract(r));
        for (Entity entity3 : entity.getWorld().getOtherEntities(entity, searchBox, e -> e instanceof LivingEntity && !except.test(e))) {
            Box box2 = entity3.getBoundingBox().expand(entity3.getTargetingMargin()).expand(expand);
            Optional<Vec3d> optional = box2.raycast(p1, p2);
            if (box2.contains(p1)) resultList.add(entity3);
            else if (optional.isPresent()) resultList.add(entity3);
        }
        return resultList;
    }
}
