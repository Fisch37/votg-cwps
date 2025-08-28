package com.cimadev.cimpleWaypointSystem.network.packets.waypoints;

import com.cimadev.cimpleWaypointSystem.command.persistentData.Waypoint;
import com.cimadev.cimpleWaypointSystem.network.utilities.AnnotatedPayload;
import com.cimadev.cimpleWaypointSystem.network.utilities.Packet;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.cimadev.cimpleWaypointSystem.network.utilities.PacketDirection.TO_CLIENT;

@Packet(id = "waypoint_list", directions = { TO_CLIENT })
public record AccessibleWaypoints(@NotNull List<@NotNull Waypoint> waypoints) implements AnnotatedPayload {

}
