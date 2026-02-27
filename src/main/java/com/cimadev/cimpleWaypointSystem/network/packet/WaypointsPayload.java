package com.cimadev.cimpleWaypointSystem.network.packet;

import com.cimadev.cimpleWaypointSystem.command.persistentData.Waypoint;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.ArrayList;
import java.util.List;

import static com.cimadev.cimpleWaypointSystem.network.PacketTypes.WAYPOINTS;

public record WaypointsPayload(List<WaypointInfo> waypoints) implements CustomPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, WaypointsPayload> PACKET_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(size -> new ArrayList<>(), WaypointInfo.PACKET_CODEC),
            WaypointsPayload::waypoints,
            WaypointsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return WAYPOINTS;
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(WAYPOINTS, PACKET_CODEC);
    }
}
