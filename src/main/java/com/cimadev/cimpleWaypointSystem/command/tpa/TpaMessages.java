package com.cimadev.cimpleWaypointSystem.command.tpa;

import com.cimadev.cimpleWaypointSystem.Colors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;

public class TpaMessages {
    private static MutableComponent literalCommand(String command, String hover) {
        return Component.literal(command).setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent.SuggestCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(hover)))
        );
    }

    private static void sendDuplicateRequestError(Player origin) {
        origin.displayClientMessage(Component.literal("You already have a teleport request waiting.")
                        .append(" Use")
                        .append(literalCommand("/tpcancel", "Click to cancel"))
                        .append(" to cancel it.")
                .withStyle(Colors.FAILURE),
                false
        );
    }

    private static void sendBusyTargetError(TeleportRequest request) {
        request.getOrigin().displayClientMessage(
                request.getTarget().getName().copy().withStyle(Colors.PLAYER)
                .append(Component.literal(" already has a teleport request waiting. Try again later")
                        .withStyle(ChatFormatting.RED)
                ),
                false
        );
    }

    public static boolean handleDuplicateAndBusy(TeleportRequest request) {
        if (TeleportRequestManager.getInstance().hasRequest(request.getTarget())) {
            sendBusyTargetError(request);
            return true;
        }
        if (TeleportRequestManager.getInstance().hasOpenRequest(request.getOrigin())) {
            sendDuplicateRequestError(request.getOrigin());
            return true;
        }
        return false;
    }

    private static void sendRequestText(TeleportRequest request) {
        request.getTarget().displayClientMessage(Component.literal("")
                .append(request.getOrigin().getName().copy().withStyle(Colors.PLAYER))
                .append(" wants to teleport ")
                .append(Component.literal(request.isInverted() ? "you to them" : "themselves to you")
                        .withStyle(Colors.SECONDARY)
                )
                .append("! Type ")
                .append(literalCommand("/tpaccept", "Click to accept").withStyle(ChatFormatting.GREEN))
                .append(" to accept or ")
                .append(literalCommand("/tpdeny", "Click to deny").withStyle(ChatFormatting.RED))
                .append(" to deny it. The request expires in ")
                .append(Component.literal(Long.toString(TeleportRequestManager.getInstance().getRequestTTL()))
                        .append(" seconds.")
                        .withStyle(Colors.TIME)
                )
                .withStyle(Colors.DEFAULT),
                false
        );
    }

    private static void sendRequestConfirmation(TeleportRequest request) {
        request.getOrigin().displayClientMessage(
                Component.literal("")
                        .append(request.getTarget().getName().copy().withStyle(Colors.PLAYER))
                        .append(" has received your teleport request.")
                        .withStyle(Colors.DEFAULT),
                false
        );
    }

    public static void sendRequestMessages(TeleportRequest request) {
        sendRequestText(request);
        sendRequestConfirmation(request);
    }
}
