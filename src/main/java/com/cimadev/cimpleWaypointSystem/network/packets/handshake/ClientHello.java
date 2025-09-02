package com.cimadev.cimpleWaypointSystem.network.packets.handshake;

import com.cimadev.cimpleWaypointSystem.network.classes.ChannelFlags;
import com.cimadev.cimpleWaypointSystem.network.utilities.AnnotatedPayload;
import com.cimadev.cimpleWaypointSystem.network.utilities.Packet;

import java.util.EnumSet;

import static com.cimadev.cimpleWaypointSystem.network.utilities.PacketDirection.TO_SERVER;

@Packet(id = "client_hello", directions = { TO_SERVER })
public record ClientHello(EnumSet<ChannelFlags> requestedChannels) implements AnnotatedPayload {

}
