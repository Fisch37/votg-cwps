package com.cimadev.cimpleWaypointSystem.network;

import com.cimadev.cimpleWaypointSystem.network.features.*;
import com.cimadev.cimpleWaypointSystem.network.packet.ClientFeaturesPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayNetworkHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class NetworkHandler {
    private static final Map<ServerPlayNetworkHandler, ClientFeaturesPayload> FEATURE_REGISTRY = new HashMap<>();
    private static final List<FeatureHandler> FEATURE_HANDLERS = List.of(
            new WaypointsFeature(),
            new WaypointManagerFeature()
    );

    private static void updateFeatures(ClientFeaturesPayload features, ServerPlayNetworking.Context context) {
        FEATURE_REGISTRY.put(
                context.player().networkHandler,
                features
        );
        for (FeatureHandler handler : FEATURE_HANDLERS) {
            if (handler.qualifies(features))
                handler.onQualification(context.player(), context.responseSender());
        }
    }

    private static void registerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(PacketTypes.CLIENT_FEATURES, NetworkHandler::updateFeatures);
    }

    public static void register() {
        PacketTypes.register();
        NetworkHandler.registerReceivers();

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> FEATURE_REGISTRY.remove(handler));
    }
}
