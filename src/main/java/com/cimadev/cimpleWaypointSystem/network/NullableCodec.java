package com.cimadev.cimpleWaypointSystem.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class NullableCodec<B extends ByteBuf, V> implements StreamCodec<B, V> {
    public final StreamCodec<B, V> parent;

    public NullableCodec(StreamCodec<B, V> parent) {
        this.parent = parent;
    }

    @Override
    public V decode(B buf) {
        return FriendlyByteBuf.readNullable(buf, parent);
    }

    @Override
    public void encode(B buf, V value) {
        FriendlyByteBuf.writeNullable(buf, value, parent);
    }
}
