package com.cimadev.cimpleWaypointSystem.network.features;

import com.cimadev.cimpleWaypointSystem.Main;
import com.cimadev.cimpleWaypointSystem.command.WpsUtils;
import com.cimadev.cimpleWaypointSystem.command.persistentData.Waypoint;
import com.cimadev.cimpleWaypointSystem.network.packet.ClientFeaturesPayload;
import com.cimadev.cimpleWaypointSystem.network.packet.WaypointInfo;
import com.cimadev.cimpleWaypointSystem.network.packet.WaypointsPayload;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class WaypointsFeature implements FeatureHandler{
    @Override
    public boolean qualifies(ClientFeaturesPayload features) {
        return features.waypoints();
    }

    @Override
    public void onQualification(ServerPlayerEntity player, PacketSender sender) {
        List<Waypoint> waypoints = WpsUtils.getAccessibleWaypoints(player);
        List<WaypointInfo> waypointInfos = new ArrayList<>(waypoints.size());
        for (Waypoint waypoint : waypoints) {
            waypointInfos.add(new WaypointInfo(
                    waypoint,
                    Main.serverState.waypointAccess(waypoint,player)
            ));
        }
        sender.sendPacket(new WaypointsPayload(waypointInfos));
    }
}
