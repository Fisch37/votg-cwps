package com.cimadev.cimpleWaypointSystem.command.tpa;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.server.level.ServerPlayer;

public class TpaCommand {
    private static final String COMMAND_NAME = "tpa";

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext commandRegistryAccess,
            Commands.CommandSelection registrationEnvironment
    ) {
        dispatcher.register(Commands.literal(COMMAND_NAME)
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(TpaCommand::askTp)
                )
        );

    }

    private static int askTp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer origin = context.getSource().getPlayerOrException();
        EntitySelector entity = context.getArgument("target", EntitySelector.class);
        ServerPlayer target = entity.findSinglePlayer(context.getSource());

        final TeleportRequest request = new TeleportRequest(origin, target, false);
        if (TpaMessages.handleDuplicateAndBusy(request))
            return 0;
        TeleportRequestManager.getInstance().addRequest(request);
        TpaMessages.sendRequestMessages(request);

        return 1;
    }
}
