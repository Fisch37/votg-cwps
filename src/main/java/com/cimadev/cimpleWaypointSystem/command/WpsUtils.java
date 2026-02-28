package com.cimadev.cimpleWaypointSystem.command;

import com.cimadev.cimpleWaypointSystem.Main;
import com.cimadev.cimpleWaypointSystem.persistentData.OfflinePlayer;
import com.cimadev.cimpleWaypointSystem.persistentData.Waypoint;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class WpsUtils {
    public static List<Waypoint> getAccessibleWaypoints(@Nullable ServerPlayer player) {
        return getAccessibleWaypoints(player, null, false, false);
    }

    public static List<Waypoint> getAccessibleWaypoints(
            @Nullable ServerPlayer caller,
            @Nullable OfflinePlayer wantedOwner,
            boolean overrideAccessibility,
            boolean onlyOpen
    ) {
        List<Waypoint> waypoints = getAllWaypoints();
        LinkedList<Waypoint> goodWaypoints = new LinkedList<>();

        for (Waypoint waypoint : waypoints) {
            OfflinePlayer waypointOwner = Main.serverState.getPlayerByUuid(waypoint.getOwner());

            boolean canAccess;
            if (caller == null) canAccess = true;
            else canAccess = (Main.serverState.waypointAccess(waypoint, caller) || overrideAccessibility);
            if (waypointOwner == null) {
                if ((wantedOwner == null) && canAccess) {
                    goodWaypoints.add(waypoint);
                }
            } else if ((waypointOwner.equals(wantedOwner) || wantedOwner == null)
                    && canAccess && !onlyOpen) {
                goodWaypoints.add(waypoint);
            }
        }

        return goodWaypoints;
    }

    public static List<Waypoint> getAllWaypoints() {
        ArrayList<Waypoint> waypointsList = new ArrayList<>(Main.serverState.getAllWaypoints());
        waypointsList.sort(null);  // null means "use built-in comparator (see Waypoint)
        return waypointsList;
    }

    public static void teleport(
            Player player,
            ServerLevel world,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
        player.teleportTo(world, x, y, z, Set.of(), yaw, pitch, false);
    }

    public static void teleport(
            Player player,
            ServerLevel world,
            BlockPos pos,
            float yaw,
            float pitch
    ) {
        teleport(player, world, pos.getX(), pos.getY(), pos.getZ(), yaw, pitch);
    }
}
