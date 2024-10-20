package com.iafenvoy.dragonmounts.mixin;

import net.minecraft.entity.Flutterer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow
    private boolean vehicleFloating;

    /**
     * Purpose: to ensure players who ride flight-capable vehicles don't get kicked for flying
     * without needing to compromise server security by disabling "Kick for flying"
     */
    @Redirect(method = "onVehicleMove", at = @At(value = "FIELD", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;vehicleFloating:Z", opcode = Opcodes.PUTFIELD))
    private void dragonmounts_ensureSafeFlyingVehicle(ServerPlayNetworkHandler impl, boolean flag) {
        this.vehicleFloating = (!(impl.getPlayer().getRootVehicle() instanceof Flutterer a) || !a.isInAir()) && flag;
    }
}
