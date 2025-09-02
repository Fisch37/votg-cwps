package com.cimadev.cimpleWaypointSystem.network.packets.handshake;

import com.cimadev.cimpleWaypointSystem.command.persistentData.Waypoint;
import com.cimadev.cimpleWaypointSystem.network.utilities.AnnotatedPayload;
import com.cimadev.cimpleWaypointSystem.network.utilities.Packet;

import java.util.List;
import java.util.Optional;

import static com.cimadev.cimpleWaypointSystem.network.utilities.PacketDirection.TO_CLIENT;

@Packet(id = "server_hello", directions = { TO_CLIENT })
public record ServerHello(
        Optional<List<Waypoint>> accessibleWaypoints,
        Optional<List<Waypoint>> allWaypoints
) implements AnnotatedPayload {

}
