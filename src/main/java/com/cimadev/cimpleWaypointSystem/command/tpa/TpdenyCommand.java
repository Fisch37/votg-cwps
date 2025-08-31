package com.cimadev.cimpleWaypointSystem.command.tpa;

import com.cimadev.cimpleWaypointSystem.Colors;
import com.cimadev.cimpleWaypointSystem.command.tpa.logic.TPAManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;


public class TpdenyCommand {
    private static final String COMMAND_NAME = "tpdeny";
    private static final Text NO_TPA_ERROR =
            Text.literal("There is no").formatted(Colors.FAILURE)
            .append(Text.literal("/tpa").formatted(Formatting.YELLOW))
            .append(Text.literal(" request open for you").formatted(Colors.FAILURE));


    public static void register(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment
    ) {
        dispatcher.register(
                CommandManager.literal(COMMAND_NAME)
                        .executes(TpdenyCommand::denyTeleport)
        );
    }

    public static int denyTeleport(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return TPAManager.getInstance().denyTeleport(context.getSource().getPlayerOrThrow()) ? 1 : 0;
    }
}
