package com.cimadev.cimpleWaypointSystem.network.packets.waypointAdmin;

import com.cimadev.cimpleWaypointSystem.command.persistentData.Waypoint;
import com.cimadev.cimpleWaypointSystem.network.utilities.AnnotatedPayload;
import com.cimadev.cimpleWaypointSystem.network.utilities.Packet;

import java.util.List;

import static com.cimadev.cimpleWaypointSystem.network.utilities.PacketDirection.TO_CLIENT;

@Packet(id = "all_waypoints", directions = { TO_CLIENT })
public record AllWaypoints(List<Waypoint> waypoints) implements AnnotatedPayload {

}
