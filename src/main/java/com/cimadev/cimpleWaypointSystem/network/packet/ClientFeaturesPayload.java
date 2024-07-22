package com.cimadev.cimpleWaypointSystem.network.packet;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import static com.cimadev.cimpleWaypointSystem.network.PacketTypes.CLIENT_FEATURES;

public record ClientFeaturesPayload(int features) implements CustomPayload {
    public static PacketCodec<RegistryByteBuf, ClientFeaturesPayload> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, ClientFeaturesPayload::features,
            ClientFeaturesPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return CLIENT_FEATURES;
    }

    private boolean hasFeature(int featureNum) {
        return (this.features & (1<<featureNum)) != 0;
    }

    public boolean waypoints() {
        return this.hasFeature(0);
    }

    public boolean waypointsManagement() {
        return this.hasFeature(1);
    }

    public boolean friends() {
        return this.hasFeature(2);
    }


    public static void register() {
        PayloadTypeRegistry.playC2S().register(CLIENT_FEATURES, PACKET_CODEC);
    }
}
