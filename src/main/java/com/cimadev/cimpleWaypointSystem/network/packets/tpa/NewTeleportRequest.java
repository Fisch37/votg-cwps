package com.cimadev.cimpleWaypointSystem.network.packets.tpa;

import com.cimadev.cimpleWaypointSystem.network.utilities.AnnotatedPayload;
import com.cimadev.cimpleWaypointSystem.network.utilities.Packet;
import com.cimadev.cimpleWaypointSystem.network.utilities.PacketDirection;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Packet(id = "teleport_request", directions = { PacketDirection.TO_CLIENT })
public record NewTeleportRequest(@NotNull UUID from, @NotNull UUID to, boolean isHere) implements AnnotatedPayload {

}
