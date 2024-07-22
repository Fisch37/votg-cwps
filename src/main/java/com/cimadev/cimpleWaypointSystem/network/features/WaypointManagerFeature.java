package com.cimadev.cimpleWaypointSystem.network.features;

import com.cimadev.cimpleWaypointSystem.network.packet.ClientFeaturesPayload;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.network.ServerPlayerEntity;

public class WaypointManagerFeature implements FeatureHandler {
    @Override
    public boolean qualifies(ClientFeaturesPayload features) {
        return features.waypointsManagement();
    }

    @Override
    public void onQualification(ServerPlayerEntity player, PacketSender sender) {
        // Coming at some point
    }
}
