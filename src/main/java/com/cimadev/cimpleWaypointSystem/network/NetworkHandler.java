package com.cimadev.cimpleWaypointSystem.network;

import com.cimadev.cimpleWaypointSystem.PermissionHelpers;
import com.cimadev.cimpleWaypointSystem.Main;
import com.cimadev.cimpleWaypointSystem.command.WpsUtils;
import com.cimadev.cimpleWaypointSystem.command.persistentData.Waypoint;
import com.cimadev.cimpleWaypointSystem.network.packet.WaypointInfo;
import com.cimadev.cimpleWaypointSystem.network.packet.WaypointsPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.ArrayList;
import java.util.List;

public abstract class NetworkHandler {
    private static void registerReceivers() {

    }

    public static void register() {
        PacketTypes.register();
        NetworkHandler.registerReceivers();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            final ServerPlayer player = handler.getPlayer();

            List<Waypoint> waypoints;
            if (PermissionHelpers.hasPermissionLevel(handler.getPlayer(), PermissionLevel.OWNERS)) {
                waypoints = WpsUtils.getAllWaypoints();
            } else {
                waypoints = WpsUtils.getAccessibleWaypoints(player);
            }

            List<WaypointInfo> waypointInfos = new ArrayList<>(waypoints.size());
            for (Waypoint waypoint : waypoints) {
                waypointInfos.add(new WaypointInfo(
                        waypoint,
                        Main.serverState.waypointAccess(waypoint,player)
                ));
            }

            sender.sendPacket(new WaypointsPayload(waypointInfos));
        });
    }
}
