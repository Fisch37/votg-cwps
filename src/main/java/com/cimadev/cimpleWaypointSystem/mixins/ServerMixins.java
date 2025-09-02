package com.cimadev.cimpleWaypointSystem.mixins;

import com.cimadev.cimpleWaypointSystem.command.tpa.logic.TeleportRequestStorage;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class ServerMixins {
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci){
        TeleportRequestStorage.getInstance().tick();
    }
}