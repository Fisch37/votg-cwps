package com.cimadev.cimpleWaypointSystem.network.packet;

import com.cimadev.cimpleWaypointSystem.Main;
import com.cimadev.cimpleWaypointSystem.command.persistentData.Waypoint;
import com.cimadev.cimpleWaypointSystem.network.NetworkHandler;
import com.cimadev.cimpleWaypointSystem.network.packet.waypoints.WaypointInfo;
import com.cimadev.cimpleWaypointSystem.network.packet.waypoints.WaypointUpdate;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public class NetworkProcessor extends Thread {
    private final LinkedBlockingQueue<Waypoint> updateQueue = new LinkedBlockingQueue<>();

    public NetworkProcessor() {
        super("cWPS-NetworkProcessor");
    }

    private void sendUpdate(Waypoint waypoint) {
        for (Map.Entry<ServerPlayNetworkHandler, ClientFeaturesPayload> entry : NetworkHandler.getFeatures()) {
            ServerPlayerEntity player = entry.getKey().player;
            final boolean hasAccess = Main.serverState.waypointAccess(waypoint, player);
            final WaypointInfo info = new WaypointInfo(waypoint, hasAccess);
            ClientFeaturesPayload features = entry.getValue();
            if ((hasAccess && features.waypoints()) || features.waypointsManagement()) {
                ServerPlayNetworking.send(player, new WaypointUpdate(info));
            }
        }
    }

    public void run() {
        try {
            while (true) {
                Waypoint waypoint = updateQueue.take();
                sendUpdate(waypoint);
            }
        } catch (InterruptedException e) {
            Main.LOGGER.error("NetworkProcessor was interrupted and will now exit.", e);
        }
    }

    public void markDirty(Waypoint waypoint) {
        updateQueue.offer(waypoint);
    }
}
