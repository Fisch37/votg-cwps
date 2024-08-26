package com.cimadev.cimpleWaypointSystem.network.packet.waypoints;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import static com.cimadev.cimpleWaypointSystem.network.PacketTypes.WAYPOINT_UPDATE;

public record WaypointUpdate(WaypointInfo waypointInfo) implements CustomPayload {
    public static final PacketCodec<RegistryByteBuf, WaypointUpdate> PACKET_CODEC = PacketCodec.tuple(
            WaypointInfo.PACKET_CODEC, WaypointUpdate::waypointInfo,
            WaypointUpdate::new
    );

    public static void register() {
        PayloadTypeRegistry.playS2C().register(WAYPOINT_UPDATE, PACKET_CODEC);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return WAYPOINT_UPDATE;
    }
}
