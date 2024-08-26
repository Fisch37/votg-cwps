package com.cimadev.cimpleWaypointSystem.command.tpa;

import com.cimadev.cimpleWaypointSystem.Colors;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;

public class TphereCommand {
    public static final SimpleCommandExceptionType SELF_TELEPORT_EXC = new SimpleCommandExceptionType(
            Text.literal("You cannot teleport to yourself!")
    );

    public static void register(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment
    ) {
        dispatcher.register(CommandManager.literal("tphere")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(TphereCommand::execute)
        ));
    }

    public static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final ServerPlayerEntity origin = context.getSource().getPlayerOrThrow(),
                target = EntityArgumentType.getPlayer(context, "player");
        if (origin.equals(target)) {
            throw SELF_TELEPORT_EXC.create();
        }
        final TeleportRequest request = new TeleportRequest(origin, target, true);
        if (TpaMessages.handleDuplicateAndBusy(request))
            return 0;
        TeleportRequestManager.getInstance().addRequest(request);
        TpaMessages.sendRequestMessages(request);
        context.getSource().sendFeedback(() ->
                Text.literal("")
                        .append(target.getName().copy().formatted(Colors.PLAYER))
                        .append(" has received your teleport request!")
                        .formatted(Colors.DEFAULT)
                ,
                false
        );
        return 1;
    }
}
