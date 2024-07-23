package com.cimadev.cimpleWaypointSystem.network.packet.waypoints;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

import static com.cimadev.cimpleWaypointSystem.network.PacketTypes.WAYPOINTS;

public record WaypointsPayload(List<WaypointInfo> waypoints) implements CustomPayload {
    public static final PacketCodec<RegistryByteBuf, WaypointsPayload> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.collection(size -> new ArrayList<>(), WaypointInfo.PACKET_CODEC),
            WaypointsPayload::waypoints,
            WaypointsPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return WAYPOINTS;
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(WAYPOINTS, PACKET_CODEC);
    }
}
