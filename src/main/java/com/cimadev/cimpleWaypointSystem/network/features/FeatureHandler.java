package com.cimadev.cimpleWaypointSystem.network.features;

import com.cimadev.cimpleWaypointSystem.network.packet.ClientFeaturesPayload;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.network.ServerPlayerEntity;

public interface FeatureHandler {
    boolean qualifies(ClientFeaturesPayload features);

    void onQualification(ServerPlayerEntity player, PacketSender sender);
}
