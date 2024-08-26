package com.cimadev.cimpleWaypointSystem.registry;

import com.cimadev.cimpleWaypointSystem.command.*;
import com.cimadev.cimpleWaypointSystem.command.tpa.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class ModRegistries {
    private final static CommandRegistrationCallback[] COMMAND_REGISTRATION_CALLBACKS = new CommandRegistrationCallback[]{
            WpsCommand::register,
            HomeCommand::register,
            SpawnCommand::register,
            TpaCommand::register,
            TpacceptCommand::register,
            TpdenyCommand::register,
            TphereCommand::register,
            TpcancelCommand::register
    };

    public static void registerAll() {
        registerCommands();
    }

    private static void registerCommands() {
        for (CommandRegistrationCallback callback : COMMAND_REGISTRATION_CALLBACKS){
            CommandRegistrationCallback.EVENT.register(callback);
        }
        //ArgumentTypeRegistry.registerArgumentType(new Identifier("cimple-waypoint-system", "access"), AccessArgumentType.class, ConstantArgumentSerializer.of(AccessArgumentType::access));
        //ArgumentTypeRegistry.registerArgumentType(new Identifier("cimple-waypoint-system", "offlineplayer"), OfflinePlayerArgumentType.class, ConstantArgumentSerializer.of(OfflinePlayerArgumentType::offlinePlayer));
    }
}
