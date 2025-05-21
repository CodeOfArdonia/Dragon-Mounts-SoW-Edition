package com.iafenvoy.dragonmounts.registry;

import com.iafenvoy.dragonmounts.DMConstants;
import com.iafenvoy.dragonmounts.DragonMounts;
import com.iafenvoy.dragonmounts.config.DMClientConfig;
import com.iafenvoy.dragonmounts.dragon.TameableDragonEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.impl.client.keybinding.KeyBindingRegistryImpl;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class DMKeyBindings {
    public static final KeyBinding FLIGHT_DESCENT_KEY = keymap("flight_descent", GLFW.GLFW_KEY_Z);
    public static final KeyBinding DRAGON_ATTACK = keymap("dragon_attack", GLFW.GLFW_KEY_G);
    public static final KeyBinding CAMERA_CONTROLS = keymap("camera_flight", GLFW.GLFW_KEY_F6);

    private static KeyBinding keymap(String name, int defaultMapping) {
        return new KeyBinding(String.format("key.%s.%s", DragonMounts.MOD_ID, name), defaultMapping, "key.categories.movement");
    }

    public static void init() {
        KeyBindingRegistryImpl.registerKeyBinding(FLIGHT_DESCENT_KEY);
        KeyBindingRegistryImpl.registerKeyBinding(DRAGON_ATTACK);
        KeyBindingRegistryImpl.registerKeyBinding(CAMERA_CONTROLS);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.player.getVehicle() instanceof TameableDragonEntity dragon) {
                if (CAMERA_CONTROLS.wasPressed()) {
                    DMClientConfig.INSTANCE.MISC.cameraDrivenFlight.setValue(!DMClientConfig.INSTANCE.MISC.cameraDrivenFlight.getValue());
                    client.player.sendMessage(Text.translatable("mount.dragon.camera_controls." + (DMClientConfig.INSTANCE.MISC.cameraDrivenFlight.getValue() ? "enabled" : "disabled"), dragon.getDisplayName()), true);
                }
                if (DRAGON_ATTACK.isPressed())
                    ClientPlayNetworking.send(DMConstants.DRAGON_ATTACK, PacketByteBufs.create());
            }
        });
    }
}
