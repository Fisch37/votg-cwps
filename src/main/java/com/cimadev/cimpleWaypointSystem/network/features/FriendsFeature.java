package com.cimadev.cimpleWaypointSystem.network.features;

import com.cimadev.cimpleWaypointSystem.command.persistentData.OfflinePlayer;
import com.cimadev.cimpleWaypointSystem.network.packet.ClientFeaturesPayload;
import com.cimadev.cimpleWaypointSystem.network.packet.FriendsListPayload;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public class FriendsFeature implements FeatureHandler{
    @Override
    public boolean qualifies(ClientFeaturesPayload features) {
        return features.friends();
    }

    @Override
    public void onQualification(ServerPlayerEntity player, PacketSender sender) {
        OfflinePlayer storedData = OfflinePlayer.fromUuid(player.getUuid());
        if (storedData == null) {
            sender.sendPacket(new FriendsListPayload(List.of()));
        } else {
            sender.sendPacket(new FriendsListPayload(storedData.getFriends()));
        }
    }
}
