package com.cimadev.cimpleWaypointSystem.command.tpa;

import com.cimadev.cimpleWaypointSystem.Colors;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

public class TpaMessages {
    private static MutableText literalCommand(String command, String hover) {
        return Text.literal(command).setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover)))
        );
    }

    private static void sendDuplicateRequestError(PlayerEntity origin) {
        origin.sendMessage(Text.literal("You already have a teleport request waiting.")
                        .append(" Use")
                        .append(literalCommand("/tpcancel", "Click to cancel"))
                        .append(" to cancel it.")
                .formatted(Colors.FAILURE)
        );
    }

    private static void sendBusyTargetError(TeleportRequest request) {
        request.getOrigin().sendMessage(
                request.getTarget().getName().copy().formatted(Colors.PLAYER)
                .append(Text.literal(" already has a teleport request waiting. Try again later")
                        .formatted(Formatting.RED)
                )
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
        request.getTarget().sendMessage(Text.literal("")
                .append(request.getOrigin().getName().copy().formatted(Colors.PLAYER))
                .append(" wants to teleport ")
                .append(Text.literal(request.isInverted() ? "you to them" : "themselves to you")
                        .formatted(Colors.SECONDARY)
                )
                .append("! Type ")
                .append(literalCommand("/tpaccept", "Click to accept").formatted(Formatting.GREEN))
                .append(" to accept or ")
                .append(literalCommand("/tpdeny", "Click to deny").formatted(Formatting.RED))
                .append(" to deny it. The request expires in ")
                .append(Text.literal(Long.toString(TeleportRequestManager.getInstance().getRequestTTL()))
                        .append(" seconds.")
                        .formatted(Colors.TIME)
                )
                .formatted(Colors.DEFAULT)
        );
    }

    private static void sendRequestConfirmation(TeleportRequest request) {
        request.getOrigin().sendMessage(
                Text.literal("")
                        .append(request.getTarget().getName().copy().formatted(Colors.PLAYER))
                        .append(" has received your teleport request.")
                        .formatted(Colors.DEFAULT)
        );
    }

    public static void sendRequestMessages(TeleportRequest request) {
        sendRequestText(request);
        sendRequestConfirmation(request);
    }
}
