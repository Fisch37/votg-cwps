package com.cimadev.cimpleWaypointSystem.network;

import com.cimadev.cimpleWaypointSystem.network.packet.WaypointsPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import static com.cimadev.cimpleWaypointSystem.Main.MOD_ID;

public abstract class PacketTypes {
    public static final CustomPacketPayload.Type<WaypointsPayload> WAYPOINTS = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "waypoints"));;

    public static void register() {
        WaypointsPayload.register();
    }
}