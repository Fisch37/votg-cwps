package com.cimadev.cimpleWaypointSystem.network.codecs.primitives;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;

public class NullableCodec<B extends ByteBuf, V> implements PacketCodec<B, V> {
    public final PacketCodec<B, V> parent;

    private NullableCodec(PacketCodec<B, V> parent) {
        this.parent = parent;
    }

    public static <B extends ByteBuf, V> NullableCodec<B, V> of(PacketCodec<B, V> parent) {
        return new NullableCodec<>(parent);
    }

    @Override
    public V decode(B buf) {
        return PacketByteBuf.readNullable(buf, parent);
    }

    @Override
    public void encode(B buf, V value) {
        PacketByteBuf.writeNullable(buf, value, parent);
    }
}
