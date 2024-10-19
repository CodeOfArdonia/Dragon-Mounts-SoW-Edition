package com.iafenvoy.dragonmounts.client;

import com.iafenvoy.dragonmounts.DMLConfig;
import com.iafenvoy.dragonmounts.DragonMounts;
import com.iafenvoy.dragonmounts.dragon.TameableDragon;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.impl.client.keybinding.KeyBindingRegistryImpl;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class KeyMappings {
    public static final KeyBinding FLIGHT_DESCENT_KEY = keymap("flight_descent", GLFW.GLFW_KEY_Z, "key.categories.movement");
    public static final KeyBinding CAMERA_CONTROLS = keymap("camera_flight", GLFW.GLFW_KEY_F6, "key.categories.movement");

    private static KeyBinding keymap(String name, int defaultMapping, String category) {
        return new KeyBinding(String.format("key.%s.%s", DragonMounts.MOD_ID, name), defaultMapping, category);
    }

    public static void init() {
        KeyBindingRegistryImpl.registerKeyBinding(FLIGHT_DESCENT_KEY);
        KeyBindingRegistryImpl.registerKeyBinding(CAMERA_CONTROLS);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (CAMERA_CONTROLS.wasPressed() && client.player != null && client.player.getVehicle() instanceof TameableDragon d) {
                DMLConfig.CAMERA_DRIVEN_FLIGHT.set(!DMLConfig.cameraDrivenFlight());
                client.player.sendMessage(Text.translatable("mount.dragon.camera_controls." + (DMLConfig.cameraDrivenFlight() ? "enabled" : "disabled"), d.getDisplayName()), true);
            }
        });
    }
}
