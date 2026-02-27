package com.cimadev.cimpleWaypointSystem.command.tpa;

import com.cimadev.cimpleWaypointSystem.Colors;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.text.*;

public class TphereCommand {
    public static final SimpleCommandExceptionType SELF_TELEPORT_EXC = new SimpleCommandExceptionType(
            Component.literal("You cannot teleport to yourself!")
    );

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess,
            Commands.CommandSelection environment
    ) {
        dispatcher.register(Commands.literal("tphere")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(TphereCommand::execute)
        ));
    }

    public static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ServerPlayer origin = context.getSource().getPlayerOrException(),
                target = EntityArgument.getPlayer(context, "player");
        if (origin.equals(target)) {
            throw SELF_TELEPORT_EXC.create();
        }
        final TeleportRequest request = new TeleportRequest(origin, target, true);
        if (TpaMessages.handleDuplicateAndBusy(request))
            return 0;
        TeleportRequestManager.getInstance().addRequest(request);
        TpaMessages.sendRequestMessages(request);
        context.getSource().sendSuccess(() ->
                Component.literal("")
                        .append(target.getName().copy().withStyle(Colors.PLAYER))
                        .append(" has received your teleport request!")
                        .withStyle(Colors.DEFAULT)
                ,
                false
        );
        return 1;
    }
}
