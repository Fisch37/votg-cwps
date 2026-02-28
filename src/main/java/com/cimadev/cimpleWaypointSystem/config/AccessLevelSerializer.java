package com.cimadev.cimpleWaypointSystem.config;

import com.cimadev.cimpleWaypointSystem.persistentData.AccessLevel;
import de.maxhenkel.configbuilder.entry.serializer.ValueSerializer;

public class AccessLevelSerializer implements ValueSerializer<AccessLevel> {
    public static final AccessLevelSerializer INSTANCE = new AccessLevelSerializer();

    /**
     * Deserializes the string to {@link AccessLevel}.
     *
     * @param str the string to deserialize
     * @return the deserialized value
     */
    @Override
    public AccessLevel deserialize(String str) {
        return AccessLevel.fromString(str);
    }

    /**
     * Serializes the value to a string.
     *
     * @param val the value to serialize
     * @return the serialized value
     */
    @Override
    public String serialize(AccessLevel val) {
        return val.getName();
    }
}
