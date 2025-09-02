package com.cimadev.cimpleWaypointSystem.network.utilities;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.cimadev.cimpleWaypointSystem.Main.MOD_ID;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Packet {
    String namespace() default MOD_ID;
    String id();
    PacketDirection[] directions();
}
