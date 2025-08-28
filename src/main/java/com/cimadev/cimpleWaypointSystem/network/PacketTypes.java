package com.cimadev.cimpleWaypointSystem.network;

import com.cimadev.cimpleWaypointSystem.command.persistentData.Waypoint;
import com.cimadev.cimpleWaypointSystem.command.persistentData.WaypointKey;
import com.cimadev.cimpleWaypointSystem.network.classes.ChannelFlags;
import com.cimadev.cimpleWaypointSystem.network.codecs.primitives.EnumCodec;
import com.cimadev.cimpleWaypointSystem.network.codecs.primitives.EnumSetPacketCodec;
import com.cimadev.cimpleWaypointSystem.network.codecs.primitives.NullableCodec;
import com.cimadev.cimpleWaypointSystem.network.packets.handshake.ClientHello;
import com.cimadev.cimpleWaypointSystem.network.packets.tpa.NewTeleportRequest;
import com.cimadev.cimpleWaypointSystem.network.packets.tpa.TeleportEvent;
import com.cimadev.cimpleWaypointSystem.network.packets.waypointAdmin.AllWaypoints;
import com.cimadev.cimpleWaypointSystem.network.packets.waypoints.AccessibleWaypoints;
import com.cimadev.cimpleWaypointSystem.network.packets.waypoints.WaypointUpdate;
import com.cimadev.cimpleWaypointSystem.network.utilities.AnnotatedPayload;
import com.cimadev.cimpleWaypointSystem.network.utilities.Packet;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.ArrayList;
import java.util.Map;

import static net.minecraft.network.packet.CustomPayload.Id;

public abstract class PacketTypes {
    public static final Map<Class<? extends AnnotatedPayload>, PacketCodec<RegistryByteBuf, ? extends AnnotatedPayload>> PACKET_TYPES = Map.of(
            ClientHello.class,
            PacketCodec.tuple(
                    EnumSetPacketCodec.of(ChannelFlags.class), ClientHello::requestedChannels,
                    ClientHello::new
            ),
            NewTeleportRequest.class,
            PacketCodec.tuple(
                    Uuids.PACKET_CODEC, NewTeleportRequest::from,
                    Uuids.PACKET_CODEC, NewTeleportRequest::to,
                    PacketCodecs.BOOL, NewTeleportRequest::isHere,
                    NewTeleportRequest::new
            ),
            TeleportEvent.class,
            PacketCodec.tuple(
                    EnumCodec.of(TeleportEvent.Action.class), TeleportEvent::action,
                    TeleportEvent::new
            ),
            AllWaypoints.class,
            PacketCodec.tuple(
                    PacketCodecs.collection(ArrayList::new, Waypoint.PACKET_CODEC), AllWaypoints::waypoints,
                    AllWaypoints::new
            ),
            AccessibleWaypoints.class,
            PacketCodec.tuple(
                    PacketCodecs.collection(ArrayList::new, Waypoint.PACKET_CODEC), AccessibleWaypoints::waypoints,
                    AccessibleWaypoints::new
            ),
            WaypointUpdate.class,
            PacketCodec.tuple(
                    WaypointKey.PACKET_CODEC, WaypointUpdate::key,
                    NullableCodec.of(Waypoint.PACKET_CODEC), WaypointUpdate::waypoint,
                    WaypointUpdate::new
            )
    );

    public static void register() {
        PACKET_TYPES.forEach(PacketTypes::registerClassUnsafe);
    }

    @SuppressWarnings("unchecked")
    private static void registerClassUnsafe(
            Class<? extends AnnotatedPayload> clazz,
            PacketCodec<? super RegistryByteBuf, ? extends AnnotatedPayload> codec
    ) {
        registerClass((Class<AnnotatedPayload>)clazz, (PacketCodec<? super RegistryByteBuf, AnnotatedPayload>) codec);
    }

    private static <T extends AnnotatedPayload> void registerClass(Class<T> clazz, PacketCodec<? super RegistryByteBuf, T> codec) {
        var annotation = clazz.getAnnotation(Packet.class);
        if (annotation == null)
            throw new IllegalStateException("Packet type " + clazz.getName() + " is not annotated with @Packet");
        var id = AnnotatedPayload.getIdForClass(clazz);
        for (var direction : annotation.directions()) {
            switch (direction) {
                case TO_CLIENT -> registerS2C(id, codec);
                case TO_SERVER -> registerC2S(id, codec);
            }
        }
    }

    private static <T extends CustomPayload> void registerC2S(Id<T> id, PacketCodec<? super RegistryByteBuf, T> codec) {
        PayloadTypeRegistry.playC2S().register(id, codec);
    }

    private static <T extends CustomPayload> void registerS2C(Id<T> id, PacketCodec<? super RegistryByteBuf, T> codec) {
        PayloadTypeRegistry.playS2C().register(id, codec);
    }
}