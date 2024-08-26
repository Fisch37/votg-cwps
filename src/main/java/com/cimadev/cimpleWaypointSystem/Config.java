package com.cimadev.cimpleWaypointSystem;

import com.cimadev.cimpleWaypointSystem.command.persistentData.AccessLevel;
import com.cimadev.cimpleWaypointSystem.config.AccessLevelList;
import com.cimadev.cimpleWaypointSystem.config.AccessLevelListSerializer;
import com.cimadev.cimpleWaypointSystem.config.AccessLevelSerializer;
import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.configbuilder.custom.StringList;
import de.maxhenkel.configbuilder.entry.ConfigEntry;

import java.nio.file.Path;
import java.util.List;

import static com.cimadev.cimpleWaypointSystem.Main.LOGGER;
import static com.cimadev.cimpleWaypointSystem.Main.MOD_ID;

public class Config {
    public final ConfigEntry<AccessLevel> defaultAccess;
    public final ConfigEntry<List<AccessLevel>> disabledAccessLevels;
    public final ConfigEntry<Boolean> preferOpenForDerived;

    private Config(ConfigBuilder builder) {
        builder.header(
                "Configuration for Cimple Waypoint System"
        );
        defaultAccess = builder.entry(
                "default-access-level",
                AccessLevel.PRIVATE,
                "The default access level when setting a new waypoint. May be any of the defined access levels",
                "private by default"
        );
        disabledAccessLevels = builder.entry(
                "disabled-levels",
                AccessLevelList.of(),
                "Add access levels here to remove them.",
                "Format is comma-separated values",
                "Previously created waypoints of those access levels will persist, but no new ones can be created."
        );
        preferOpenForDerived = builder.booleanEntry(
                "prefer-open-for-derived",
                true,
                "Sets behaviour on the /wps go <name> shorthand.",
                "When set to true, first checks for an open waypoint matching <name>,",
                "otherwise checks for a self-owned waypoint first."
        );
    }

    public static Config build() {
        Config config =  ConfigBuilder.builder(Config::new)
                .addValueSerializer(AccessLevel.class, AccessLevelSerializer.INSTANCE)
                .addValueSerializer(AccessLevelList.class, AccessLevelListSerializer.INSTANCE)
                .path(getConfigFile())
                .saveSyncAfterBuild(true)
                .build();
        for (AccessLevel e : config.disabledAccessLevels.get()) {
            if (config.defaultAccess.get() == e) {
                LOGGER.warn(
                        "Default access level is in the disabled list. "
                        + "Players will still be able to use the default access level even though it's disabled"
                );
                break;
            }
        }

        return config;
    }

    private static Path getConfigFile() {
        Path path = Path.of(".")
                .resolve("config")
                .resolve(MOD_ID);
        try {
            path.toFile().mkdirs();
        } catch (SecurityException e) {
            LOGGER.error("Cannot access config file at {}. Using default", path);
        }
        return path.resolve("config.properties");
    }
}
