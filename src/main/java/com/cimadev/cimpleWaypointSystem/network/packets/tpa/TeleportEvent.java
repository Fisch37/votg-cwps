package com.cimadev.cimpleWaypointSystem.network.packets.tpa;

import com.cimadev.cimpleWaypointSystem.network.utilities.AnnotatedPayload;
import com.cimadev.cimpleWaypointSystem.network.utilities.Packet;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.cimadev.cimpleWaypointSystem.network.utilities.PacketDirection.TO_SERVER;

@Packet(id = "teleport_event", directions = { TO_SERVER })
public record TeleportEvent(@NotNull UUID target, Action action) implements AnnotatedPayload {
    public enum Action {
        ACCEPT, DENY, CANCEL
    }
}
