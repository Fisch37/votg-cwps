package com.cimadev.cimpleWaypointSystem.network.packet.friends;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

import static com.cimadev.cimpleWaypointSystem.network.PacketTypes.REM_FRIEND;

public record RemFriendPayload(UUID friend) implements CustomPayload {
    public static final PacketCodec<RegistryByteBuf, RemFriendPayload> PACKET_CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, RemFriendPayload::friend,
            RemFriendPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return REM_FRIEND;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(REM_FRIEND, PACKET_CODEC);
    }
}
