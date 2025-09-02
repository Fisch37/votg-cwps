package com.cimadev.cimpleWaypointSystem.network.utilities;

import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * De-boilerplated version of {@link net.minecraft.network.packet.CustomPayload}
 */
public interface AnnotatedPayload extends CustomPayload {
    Map<Class<? extends AnnotatedPayload>, Id<? extends AnnotatedPayload>> ID_CACHE = new HashMap<>();

    @SuppressWarnings("unchecked")
    static <T extends AnnotatedPayload> Id<T> getIdForClass(Class<T> clazz) {
        return (Id<T>) ID_CACHE.computeIfAbsent(clazz, clazz_ -> {
            var annotation = clazz_.getAnnotation(Packet.class);
            if (annotation == null) return null;
            return new Id<>(Identifier.of(annotation.namespace(), annotation.id()));
        });
    }

    @Override
    default Id<? extends CustomPayload> getId() {
        return getIdForClass(this.getClass());
    }
}
