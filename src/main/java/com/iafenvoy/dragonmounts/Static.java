package com.iafenvoy.dragonmounts;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class Static {
    @Nullable
    public static MinecraftServer server = null;
    public static final Identifier DRAGON_EGG_TYPE_SYNC = new Identifier(DragonMounts.MOD_ID, "dragon_egg_type_sync");
}
