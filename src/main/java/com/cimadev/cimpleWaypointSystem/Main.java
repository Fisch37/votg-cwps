package com.cimadev.cimpleWaypointSystem;

import com.cimadev.cimpleWaypointSystem.command.persistentData.ServerState;
import com.cimadev.cimpleWaypointSystem.network.NetworkHandler;
import com.cimadev.cimpleWaypointSystem.registry.ModRegistries;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements ModInitializer {

    public static final String MOD_ID = "cimple-waypoint-system";
    public static final Logger LOGGER = LoggerFactory.getLogger("CWPS");


    public static ServerState serverState;
    public static Config config;
    @Override
    public void onInitialize() {
        config = Config.build();
        ModRegistries.registerAll();
        NetworkHandler.register();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            System.out.println("Initializing server state!");
            serverState = ServerState.getServerState(server);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> serverState.setPlayer(handler.player));
    }
}
