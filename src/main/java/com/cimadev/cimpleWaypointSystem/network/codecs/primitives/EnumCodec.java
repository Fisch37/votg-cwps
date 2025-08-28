package com.cimadev.cimpleWaypointSystem.network.codecs.primitives;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;

public class EnumCodec<E extends Enum<E>> implements PacketCodec<ByteBuf,E> {
    private final Class<E> clazz;

    private EnumCodec(Class<E> clazz) {
        this.clazz = clazz;
    }

    public static <T extends Enum<T>> EnumCodec<T> of(Class<T> clazz) {
        return new EnumCodec<>(clazz);
    }

    @Override
    public E decode(ByteBuf buf) {
        return clazz.getEnumConstants()[buf.readInt()];
    }

    @Override
    public void encode(ByteBuf buf, E value) {
        buf.writeInt(value.ordinal());
    }
}
