package com.cimadev.cimpleWaypointSystem.config;

import com.cimadev.cimpleWaypointSystem.persistentData.AccessLevel;
import de.maxhenkel.configbuilder.entry.serializer.ValueSerializer;

import java.util.ArrayList;

public class AccessLevelListSerializer implements ValueSerializer<AccessLevelList> {
    private static final String DELIMITER = ",";
    private static final AccessLevelSerializer SERIALIZER = AccessLevelSerializer.INSTANCE;
    public static final AccessLevelListSerializer INSTANCE = new AccessLevelListSerializer();

    /**
     * Deserializes the string to {@link T}.
     *
     * @param str the string to deserialize
     * @return the deserialized value
     */
    @Override
    public AccessLevelList deserialize(String str) {
        String[] arr = str.split(DELIMITER);
        ArrayList<AccessLevel> list = new ArrayList<>(arr.length);
        for (String val : arr) {
            if (val.isEmpty()) continue;
            list.add(SERIALIZER.deserialize(val));
        }
        return new AccessLevelList(list);
    }

    /**
     * Serializes the value to a string.
     *
     * @param val the value to serialize
     * @return the serialized value
     */
    @Override
    public String serialize(AccessLevelList val) {
        ArrayList<String> list = new ArrayList<>(val.size());
        for (AccessLevel accessLevel : val) {
            list.add(SERIALIZER.serialize(accessLevel));
        }
        return String.join(DELIMITER, list);
    }
}
