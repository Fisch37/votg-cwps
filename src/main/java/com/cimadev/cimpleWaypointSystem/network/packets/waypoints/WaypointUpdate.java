package com.cimadev.cimpleWaypointSystem.network.packets.waypoints;

import com.cimadev.cimpleWaypointSystem.command.persistentData.Waypoint;
import com.cimadev.cimpleWaypointSystem.command.persistentData.WaypointKey;
import com.cimadev.cimpleWaypointSystem.network.utilities.AnnotatedPayload;
import com.cimadev.cimpleWaypointSystem.network.utilities.Packet;
import org.jetbrains.annotations.Nullable;

import static com.cimadev.cimpleWaypointSystem.network.utilities.PacketDirection.TO_CLIENT;

@Packet(id = "waypoint_update", directions = { TO_CLIENT })
public record WaypointUpdate(WaypointKey key, @Nullable Waypoint waypoint, boolean accessible)
        implements AnnotatedPayload {

}
