package com.cimadev.cimpleWaypointSystem.command.tpa;

import com.cimadev.cimpleWaypointSystem.Colors;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class TpcancelCommand {
    public static void register(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess commandRegistryAccess,
            CommandManager.RegistrationEnvironment registrationEnvironment
    ) {
        dispatcher.register(CommandManager.literal("tpcancel").executes(TpcancelCommand::execute));
    }

    public static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        TeleportRequest request = TeleportRequestManager.getInstance().removeRequestByOrigin(player);
        if (request == null) {
            player.sendMessage(Text.literal("You have no teleport request open").formatted(Colors.FAILURE));
            return 0;
        } else {
            request.getTarget().sendMessage(
                    player.getName().copy().formatted(Colors.PLAYER)
                            .append(" has cancelled their teleport request.")
                            .formatted(Colors.DEFAULT)
            );
            player.sendMessage(Text.literal("Your teleport request to ")
                    .append(player.getName().copy().formatted(Colors.PLAYER))
                    .append(" has been cancelled")
                    .formatted(Colors.DEFAULT)
            );
            return 1;
        }
    }
}
