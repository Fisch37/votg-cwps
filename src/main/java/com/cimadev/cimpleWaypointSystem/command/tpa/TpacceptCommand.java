package com.cimadev.cimpleWaypointSystem.command.tpa;

import com.cimadev.cimpleWaypointSystem.Colors;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class TpacceptCommand {
    private static final String COMMAND_NAME = "tpaccept";
    private static final Component NO_TPA_ERROR_MESSAGE =
            Component.literal("There is no ").withStyle(Colors.FAILURE)
            .append(Component.literal("/tpa").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" request open for you").withStyle(Colors.FAILURE));

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext commandRegistryAccess,
            Commands.CommandSelection registrationEnvironment
    ) {
        dispatcher.register(Commands.literal(COMMAND_NAME)
                .executes(TpacceptCommand::acceptTeleport)
        );

    }

    private static int acceptTeleport(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        TeleportRequest request = TeleportRequestManager.getInstance().removeRequestByTarget(
                context.getSource().getPlayerOrException()
        );
        if (request == null) {
            context.getSource().sendSuccess(() -> NO_TPA_ERROR_MESSAGE, false);
            return 0;
        }
        request.perform();
        return 1;
    }
}
