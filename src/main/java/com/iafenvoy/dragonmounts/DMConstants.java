package com.iafenvoy.dragonmounts;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class DMConstants {
    @Nullable
    public static MinecraftServer server = null;
    public static boolean shouldForceParticleSpeed = false;
    public static final Identifier DRAGON_EGG_TYPE_SYNC = new Identifier(DragonMounts.MOD_ID, "dragon_egg_type_sync");
    public static final Identifier DRAGON_ATTACK = new Identifier(DragonMounts.MOD_ID, "dragon_attack");
}
