package com.cimadev.cimpleWaypointSystem;

import com.cimadev.cimpleWaypointSystem.command.WpsUtils;
import com.cimadev.cimpleWaypointSystem.command.persistentData.Waypoint;
import com.cimadev.cimpleWaypointSystem.command.persistentData.WaypointKey;
import com.cimadev.cimpleWaypointSystem.command.tpa.logic.TPAManager;
import com.cimadev.cimpleWaypointSystem.command.tpa.logic.TeleportRequest;
import com.cimadev.cimpleWaypointSystem.network.classes.ChannelFlags;
import com.cimadev.cimpleWaypointSystem.network.packets.handshake.ClientHello;
import com.cimadev.cimpleWaypointSystem.network.packets.handshake.ServerHello;
import com.cimadev.cimpleWaypointSystem.network.packets.tpa.NewTeleportRequest;
import com.cimadev.cimpleWaypointSystem.network.packets.tpa.TeleportEvent;
import com.cimadev.cimpleWaypointSystem.network.packets.waypoints.WaypointUpdate;
import com.cimadev.cimpleWaypointSystem.network.utilities.AnnotatedPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.cimadev.cimpleWaypointSystem.Main.serverState;

public class NetworkHandler {
    private final Set<ServerPlayerEntity> tpaWatchers = new LinkedHashSet<>();
    private final Set<WaypointWatcher> waypointWatchers = new LinkedHashSet<>();

    public void registerReceivers() {
        ServerPlayConnectionEvents.DISCONNECT.register(this::onDisconnect);
        receive(ClientHello.class, this::onClientHello);
        receive(TeleportEvent.class, this::onTeleportEvent);
    }

    public void sendTpRequest(MinecraftServer server, TeleportRequest request) {
        var player = Objects.requireNonNull(server.getPlayerManager().getPlayer(request.getTarget().getUuid()));
        if (tpaWatchers.contains(player))
            ServerPlayNetworking.send(player,
                    new NewTeleportRequest(
                            request.getOrigin().getUuid(),
                            request.getTarget().getUuid(),
                            request.isInverted()
                    )
            );
    }

    public void sendWaypointAddedOrMoved(@NotNull Waypoint waypoint) {
        sendWaypointRenamed(waypoint.getKey(), waypoint);
    }

    public void sendWaypointRenamed(@NotNull WaypointKey oldKey, @NotNull Waypoint waypoint) {
        final var predicate = makeAccessPredicate(waypoint);
        sendWaypointUpdate(
                predicate,
                p -> true,
                player -> new WaypointUpdate(
                        oldKey, waypoint,
                        predicate.test(player)
                )
        );
    }

    public void sendWaypointRemoved(@NotNull Waypoint waypoint) {
        sendWaypointAccessChanged(waypoint, null);
    }

    public void sendWaypointAccessChanged(@NotNull Waypoint old, @Nullable Waypoint newWaypoint) {
        final var predicate = makeAccessPredicate(old)
                .or(newWaypoint == null ? (p -> false) : makeAccessPredicate(newWaypoint));
        sendWaypointUpdate(
                predicate,
                p -> true,
                player -> new WaypointUpdate(
                        old.getKey(), newWaypoint,
                        predicate.test(player)
                )
        );
    }

    private void sendWaypointUpdate(
            Predicate<ServerPlayerEntity> predicate, Predicate<ServerPlayerEntity> allPredicate,
            Function<ServerPlayerEntity, WaypointUpdate> function
    ) {
        for (WaypointWatcher watcher : waypointWatchers) {
            if ((watcher.all ? allPredicate : predicate).test(watcher.player))
                ServerPlayNetworking.send(watcher.player, function.apply(watcher.player));
        }
    }

    private Predicate<ServerPlayerEntity> makeAccessPredicate(@NotNull Waypoint waypoint) {
        return player -> serverState.waypointAccess(waypoint, player);
    }

    private void onDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
        tpaWatchers.remove(handler.player);
    }

    private void onClientHello(ClientHello hello, ServerPlayNetworking.Context context) {
        final var player = context.player();

        Optional<List<Waypoint>> accessible = Optional.empty(), all = Optional.empty();
        boolean receiveWaypoints = false, admin = false;
        for (ChannelFlags channel : hello.requestedChannels()) {
            switch (channel) {
                case WAYPOINTS:
                    accessible = Optional.of(WpsUtils.getAccessibleWaypoints(player));
                    receiveWaypoints = true;
                    break;
                case WAYPOINTS_ADMIN:
                    admin = player.hasPermissionLevel(3);
                    if (admin)
                        all = Optional.of(WpsUtils.getAllWaypoints());
                    receiveWaypoints = true;
                    break;
                case TELEPORT_REQUESTS:
                    tpaWatchers.add(player);
                    break;
            }
        }
        context.responseSender().sendPacket(new ServerHello(accessible, all));
        if (receiveWaypoints)
            waypointWatchers.add(new WaypointWatcher(player, admin));
    }

    private void onTeleportEvent(TeleportEvent event, ServerPlayNetworking.Context context) {
        var manager = TPAManager.getInstance();
        switch (event.action()) {
            case ACCEPT -> manager.acceptTeleport(context.player());
            case DENY -> manager.denyTeleport(context.player());
            case CANCEL -> manager.cancelTeleport(context.player());
        }
    }

    private <T extends AnnotatedPayload> void receive(Class<T> clazz, ServerPlayNetworking.PlayPayloadHandler<T> handler) {
        ServerPlayNetworking.registerGlobalReceiver(AnnotatedPayload.getIdForClass(clazz), handler);
    }

    private record WaypointWatcher(ServerPlayerEntity player, boolean all) { }
}
