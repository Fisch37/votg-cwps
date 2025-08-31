package com.cimadev.cimpleWaypointSystem.command.tpa.logic;

import com.cimadev.cimpleWaypointSystem.Colors;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static com.cimadev.cimpleWaypointSystem.Main.handler;

public class TPAManager {
    private static final TPAManager INSTANCE = new TPAManager();
    private static final Text NO_TPA_ERROR_MESSAGE =
            Text.literal("There is no ").formatted(Colors.FAILURE)
                    .append(Text.literal("/tpa").formatted(Formatting.YELLOW))
                    .append(Text.literal(" request open for you").formatted(Colors.FAILURE));

    public static TPAManager getInstance() {
        return INSTANCE;
    }

    private final TeleportRequestStorage storage = TeleportRequestStorage.getInstance();
    private MinecraftServer server;

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    public boolean requestTeleport(TeleportRequest request) {
        if (TpaMessages.handleDuplicateAndBusy(request))
            return false;
        storage.addRequest(request);
        TpaMessages.sendRequestMessages(request);
        handler.sendTpRequest(server, request);
        return true;
    }

    public boolean acceptTeleport(PlayerEntity target) {
        var request = storage.removeRequest(target);
        if (request == null)
            target.sendMessage(NO_TPA_ERROR_MESSAGE);
        else
            request.perform();
        return request != null;
    }

    public boolean denyTeleport(PlayerEntity target) {
        var request = storage.removeRequest(target);
        if (request == null)
            target.sendMessage(NO_TPA_ERROR_MESSAGE);
        else {
            PlayerEntity origin = request.getOrigin();
            target.sendMessage(
                    Text.literal("The teleport request from ")
                            .append(origin.getName().copy().formatted(Colors.PLAYER))
                            .append(" has been denied!")
                            .formatted(Colors.DEFAULT)
            );
            origin.sendMessage(
                    target.getName().copy().formatted(Colors.PLAYER)
                            .append(Text.literal(" has denied your teleport request").formatted(Formatting.RED))
            );
        }
        return request != null;
    }

    public boolean cancelTeleport(PlayerEntity origin) {
        TeleportRequest request = TeleportRequestStorage.getInstance().removeRequestByOrigin(origin);
        if (request == null) {
            origin.sendMessage(Text.literal("You have no teleport request open").formatted(Colors.FAILURE));
        } else {
            request.getTarget().sendMessage(
                    origin.getName().copy().formatted(Colors.PLAYER)
                            .append(" has cancelled their teleport request.")
                            .formatted(Colors.DEFAULT)
            );
            origin.sendMessage(Text.literal("Your teleport request to ")
                    .append(origin.getName().copy().formatted(Colors.PLAYER))
                    .append(" has been cancelled")
                    .formatted(Colors.DEFAULT)
            );
        }
        return request != null;
    }
}
