package com.cimadev.cimpleWaypointSystem.command.tpa;

import com.cimadev.cimpleWaypointSystem.Colors;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TpacceptCommand {
    private static final String COMMAND_NAME = "tpaccept";
    private static final Text NO_TPA_ERROR_MESSAGE =
            Text.literal("There is no ").formatted(Colors.FAILURE)
            .append(Text.literal("/tpa").formatted(Formatting.YELLOW))
            .append(Text.literal(" request open for you").formatted(Colors.FAILURE));

    public static void register(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess commandRegistryAccess,
            CommandManager.RegistrationEnvironment registrationEnvironment
    ) {
        dispatcher.register(CommandManager.literal(COMMAND_NAME)
                .executes(TpacceptCommand::acceptTeleport)
        );

    }

    private static int acceptTeleport(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        TeleportRequest request = TeleportRequestManager.getInstance().removeRequest(
                context.getSource().getPlayerOrThrow()
        );
        if (request == null) {
            context.getSource().sendFeedback(() -> NO_TPA_ERROR_MESSAGE, false);
            return 0;
        }
        request.perform();
        return 1;
    }
}
