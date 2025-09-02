package com.cimadev.cimpleWaypointSystem.network.codecs.primitives;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.lang.reflect.Method;
import java.util.EnumSet;

public class EnumSetPacketCodec<E extends Enum<E>> implements PacketCodec<ByteBuf, EnumSet<E>> {
    private final Class<E> enumClazz;
    private Method valuesMethod;

    public static <T extends Enum<T>> EnumSetPacketCodec<T> of(Class<T> enumClazz) {
        return new EnumSetPacketCodec<>(enumClazz);
    }

    private EnumSetPacketCodec(Class<E> enumClazz) {
        this.enumClazz = enumClazz;
        if (getEnumValues().length > 64)
            throw new IllegalArgumentException("Cannot encode enums with more than 64 values");
    }

    @Override
    public EnumSet<E> decode(ByteBuf buf) {
        E[] enumValues = getEnumValues();
        long encoded = buf.readLong();
        var result = EnumSet.noneOf(enumClazz);
        // This decoder discards values if the encoded enum is larger than the decoded enum
        // this is intentional as it avoids raising incompatibility errors when a client has a newer protocol version
        for (int i = 0; i < enumValues.length; i++) {
            if ((encoded & (1L << i)) == 1)
                result.add(enumValues[i]);
        }
        return result;
    }

    @Override
    public void encode(ByteBuf buf, EnumSet<E> value) {
        long encoded = 0;
        for (E e : value) {
            encoded |= 1L << e.ordinal();
        }
        buf.writeLong(encoded);
    }

    private E[] getEnumValues() throws IllegalStateException {
        return enumClazz.getEnumConstants();
    }
}
