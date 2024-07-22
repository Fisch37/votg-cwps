package com.cimadev.cimpleWaypointSystem.network.packet;

import com.cimadev.cimpleWaypointSystem.command.persistentData.OfflinePlayer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

import static com.cimadev.cimpleWaypointSystem.network.PacketTypes.FRIENDS;

public record FriendsListPayload(List<OfflinePlayer> friends) implements CustomPayload {
    public static final PacketCodec<RegistryByteBuf, FriendsListPayload> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.collection(ArrayList::new, OfflinePlayer.PACKET_CODEC_LIMITED),
            FriendsListPayload::friends,
            FriendsListPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return FRIENDS;
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(FRIENDS, PACKET_CODEC);
    }
}
