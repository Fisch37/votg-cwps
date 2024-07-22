package com.cimadev.cimpleWaypointSystem.network;

import com.cimadev.cimpleWaypointSystem.network.packet.ClientFeaturesPayload;
import com.cimadev.cimpleWaypointSystem.network.packet.WaypointsPayload;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static net.minecraft.network.packet.CustomPayload.Id;
import static com.cimadev.cimpleWaypointSystem.Main.MOD_ID;

public abstract class PacketTypes {
    public static final Id<ClientFeaturesPayload> CLIENT_FEATURES = id("client_features");
    public static final Id<WaypointsPayload> WAYPOINTS = id("waypoints");

    private static <T extends CustomPayload> Id<T> id(String name) {
        return new Id<>(Identifier.of(MOD_ID, name));
    }

    public static void register() {
        WaypointsPayload.register();
        ClientFeaturesPayload.register();
    }
}