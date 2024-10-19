package com.iafenvoy.dragonmounts.util;

import com.iafenvoy.dragonmounts.DragonMounts;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public class DMLUtil {
    public static final Codec<Integer> HEX_CODEC = Codec.STRING.comapFlatMap(s -> {
        try {
            return DataResult.success(Integer.parseInt(s, 16));
        } catch (NumberFormatException e) {
            return DataResult.error(() -> String.format("[%s] Hexadecimal Codec error: '%s' is not a valid hex value.", DragonMounts.MOD_ID, s));
        }
    }, Integer::toHexString);
}
