package com.iafenvoy.dragonmounts;

import com.iafenvoy.dragonmounts.config.DMClientConfig;
import com.iafenvoy.dragonmounts.config.DMCommonConfig;
import com.iafenvoy.jupiter.render.screen.ConfigSelectScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.text.Text;

public class ModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ConfigSelectScreen<>(Text.translatable("config.%s.title".formatted(DragonMounts.MOD_ID)), parent, DMCommonConfig.INSTANCE, DMClientConfig.INSTANCE);
    }
}
