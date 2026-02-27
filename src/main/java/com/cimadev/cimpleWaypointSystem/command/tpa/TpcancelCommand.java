package com.cimadev.cimpleWaypointSystem.command.tpa;

import com.cimadev.cimpleWaypointSystem.Colors;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TpcancelCommand {
    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext commandRegistryAccess,
            Commands.CommandSelection registrationEnvironment
    ) {
        dispatcher.register(Commands.literal("tpcancel").executes(TpcancelCommand::execute));
    }

    public static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        TeleportRequest request = TeleportRequestManager.getInstance().removeRequestByOrigin(player);
        if (request == null) {
            player.sendSystemMessage(Component.literal("You have no teleport request open").withStyle(Colors.FAILURE));
            return 0;
        } else {
            request.getTarget().displayClientMessage(
                    player.getName().copy().withStyle(Colors.PLAYER)
                            .append(" has cancelled their teleport request.")
                            .withStyle(Colors.DEFAULT),
                    false
            );
            player.sendSystemMessage(Component.literal("Your teleport request to ")
                    .append(player.getName().copy().withStyle(Colors.PLAYER))
                    .append(" has been cancelled")
                    .withStyle(Colors.DEFAULT)
            );
            return 1;
        }
    }
}
