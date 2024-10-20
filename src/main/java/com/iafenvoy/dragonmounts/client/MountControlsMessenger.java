package com.iafenvoy.dragonmounts.client;

import com.iafenvoy.dragonmounts.dragon.TameableDragon;
import com.iafenvoy.dragonmounts.registry.DMKeyBindings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

/**
 * A heavily hardcoded class to display a message after the dismount controls display when a player mounts a dragon.
 * {@link MountControlsMessenger#sendControlsMessage()} is called from a dragon when the LocalPlayer mounts.
 * Messages include information about flight controls, such as how to ascend/descend.
 * A hardcoded design was decided as an expanded functionality doesn't really make any sense for the
 * direction of the mod, and would instead be wasted resources.
 */
public class MountControlsMessenger {
    private static int delay = 0;

    public static void sendControlsMessage() {
        // the length the initial "dismount" message is displayed for, in ticks.
        // Our message displays after 60 ticks (after the dismount message.)
        // taken from Gui#setOverlayMessage.
        delay = 60;
    }

    @SuppressWarnings("ConstantConditions")
    public static void tick(MinecraftClient client) {
        if (delay > 0) {
            ClientPlayerEntity player = client.player;
            if (!(player.getVehicle() instanceof TameableDragon)) {
                delay = 0;
                return;
            }
            --delay;
            if (delay == 0)
                player.sendMessage(Text.translatable("mount.dragon.vertical_controls",
                        MinecraftClient.getInstance().options.jumpKey.getBoundKeyLocalizedText(),
                        DMKeyBindings.FLIGHT_DESCENT_KEY.getBoundKeyLocalizedText()), true);
        }
    }
}
