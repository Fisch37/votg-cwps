package com.cimadev.cimpleWaypointSystem.command;

import com.cimadev.cimpleWaypointSystem.Colors;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class SpawnCommand {

    private static final String COMMAND_NAME = "spawn";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandRegistryAccess, Commands.CommandSelection registrationEnvironment) {

        dispatcher.register(Commands.literal(COMMAND_NAME)
                .executes(SpawnCommand::goSpawn)
                .then(Commands.literal("help")
                        .executes(SpawnCommand::help)
        ));
    }

    public static int goSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack commandSource = context.getSource();
        ServerPlayer player = commandSource.getPlayerOrException();
        var respawn = context.getSource().getServer().overworld().getRespawnData();
        WpsUtils.teleport(
                player,
                context.getSource().getServer().getLevel(respawn.dimension()),
                respawn.pos(),
                0, 0
        );
        Supplier<Component> messageText = () -> Component.literal("Teleported to the spawnpoint.").withStyle(Colors.DEFAULT);
        commandSource.sendSuccess(messageText, false);
        return 1;
    }

    public static int help(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Supplier<Component> messageText = () -> Component.literal("The command /spawn takes you to the overworld's default spawn point.").withStyle(Colors.DEFAULT);
        context.getSource().sendSuccess(messageText, false);
        return 1;
    }
}
