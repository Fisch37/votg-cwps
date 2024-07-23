package com.cimadev.cimpleWaypointSystem.network;

import com.cimadev.cimpleWaypointSystem.network.packet.*;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static net.minecraft.network.packet.CustomPayload.Id;
import static com.cimadev.cimpleWaypointSystem.Main.MOD_ID;

public abstract class PacketTypes {
    public static final Id<ClientFeaturesPayload> CLIENT_FEATURES = id("client_features");
    public static final Id<WaypointsPayload> WAYPOINTS = id("waypoints");
    public static final Id<FriendsListPayload> FRIENDS = id("friends");
    public static final Id<AddFriendPayload> ADD_FRIEND = id("add_friend");
    public static final Id<RemFriendPayload> REM_FRIEND = id("rem_friend");

    private static <T extends CustomPayload> Id<T> id(String name) {
        return new Id<>(Identifier.of(MOD_ID, name));
    }

    public static void register() {
        WaypointsPayload.register();
        FriendsListPayload.register();
        AddFriendPayload.register();
        RemFriendPayload.register();
        ClientFeaturesPayload.register();
    }
}