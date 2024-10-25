package com.iafenvoy.dragonmounts.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ConfigLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LogUtils.getLogger();

    public static <T> T load(Class<T> clazz, String path, T defaultValue) {
        try {
            FileInputStream stream = new FileInputStream(path);
            InputStreamReader reader = new InputStreamReader(stream);
            return GSON.fromJson(reader, clazz);
        } catch (FileNotFoundException e) {
            LOGGER.error("Failed to load config", e);
            save(path, defaultValue);
            return defaultValue;
        }
    }

    public static <T> void save(String path, T value) {
        try {
            FileUtils.write(new File(path), GSON.toJson(value), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }
}
