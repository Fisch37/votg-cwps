package com.cimadev.cimpleWaypointSystem.network.packet;

import com.cimadev.cimpleWaypointSystem.command.persistentData.Waypoint;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record WaypointInfo(Waypoint waypoint, boolean accessible) {
    public static final StreamCodec<RegistryFriendlyByteBuf, WaypointInfo> PACKET_CODEC = StreamCodec.composite(
            Waypoint.PACKET_CODEC, WaypointInfo::waypoint,
            ByteBufCodecs.BOOL, WaypointInfo::accessible,
            WaypointInfo::new
    );
}
