package com.cimadev.cimpleWaypointSystem.network.packet;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

import static com.cimadev.cimpleWaypointSystem.network.PacketTypes.ADD_FRIEND;

public record AddFriendPayload(UUID newFriend) implements CustomPayload {
    public static final PacketCodec<RegistryByteBuf, AddFriendPayload> PACKET_CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, AddFriendPayload::newFriend,
            AddFriendPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ADD_FRIEND;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ADD_FRIEND, PACKET_CODEC);
    }
}
