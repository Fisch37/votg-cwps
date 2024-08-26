package com.cimadev.cimpleWaypointSystem.network;

import com.cimadev.cimpleWaypointSystem.network.packet.*;
import com.cimadev.cimpleWaypointSystem.network.packet.waypoints.*;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static net.minecraft.network.packet.CustomPayload.Id;
import static com.cimadev.cimpleWaypointSystem.Main.MOD_ID;

public abstract class PacketTypes {
    public static final Id<ClientFeaturesPayload> CLIENT_FEATURES = id("client_features");

    public static final Id<WaypointsPayload> WAYPOINTS = id("waypoints");
    public static final Id<WaypointTeleport> TELEPORT = id("teleport");
    public static final Id<WaypointUpdate> WAYPOINT_UPDATE = id("waypoint_update");

    private static <T extends CustomPayload> Id<T> id(String name) {
        return new Id<>(Identifier.of(MOD_ID, name));
    }

    public static void register() {
        ClientFeaturesPayload.register();

        WaypointsPayload.register();
        WaypointTeleport.register();
        WaypointUpdate.register();
    }
}