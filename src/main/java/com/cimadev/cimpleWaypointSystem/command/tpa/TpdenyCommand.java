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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;


public class TpdenyCommand {
    private static final String COMMAND_NAME = "tpdeny";
    private static final Component NO_TPA_ERROR =
            Component.literal("There is no").withStyle(Colors.FAILURE)
            .append(Component.literal("/tpa").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" request open for you").withStyle(Colors.FAILURE));


    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess,
            Commands.CommandSelection environment
    ) {
        dispatcher.register(
                Commands.literal(COMMAND_NAME)
                        .executes(TpdenyCommand::denyTeleport)
        );
    }

    public static int denyTeleport(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        TeleportRequest request = TeleportRequestManager.getInstance().removeRequest(player);
        if (request == null) {
            player.sendSystemMessage(NO_TPA_ERROR);
            return 0;
        }
        Player origin = request.getOrigin();
        player.sendSystemMessage(
                Component.literal("The teleport request from ")
                        .append(origin.getName().copy().withStyle(Colors.PLAYER))
                        .append(" has been denied!")
                        .withStyle(Colors.DEFAULT)
        );
        origin.displayClientMessage(
                player.getName().copy().withStyle(Colors.PLAYER)
                .append(Component.literal(" has denied your teleport request").withStyle(ChatFormatting.RED)),
                false
        );
        return 1;
    }
}
