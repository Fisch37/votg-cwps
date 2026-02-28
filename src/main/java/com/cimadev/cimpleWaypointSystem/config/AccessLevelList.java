package com.cimadev.cimpleWaypointSystem.config;

import com.cimadev.cimpleWaypointSystem.persistentData.AccessLevel;
import de.maxhenkel.configbuilder.custom.AbstractValueList;

import java.util.List;

public class AccessLevelList extends AbstractValueList<AccessLevel> {
    protected AccessLevelList(AccessLevel... accessLevels) { super(accessLevels); }
    protected AccessLevelList(List<AccessLevel> accessLevelList) { super(accessLevelList); }

    public static AccessLevelList of(AccessLevel... accessLevels) { return new AccessLevelList(accessLevels); }
    public static AccessLevelList of(List<AccessLevel> accessLevelList) { return new AccessLevelList(accessLevelList); }
}
