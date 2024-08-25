package com.cimadev.cimpleWaypointSystem.command;

import com.cimadev.cimpleWaypointSystem.Colors;
import com.cimadev.cimpleWaypointSystem.command.persistentData.OfflinePlayer;
import com.cimadev.cimpleWaypointSystem.command.persistentData.Waypoint;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public abstract class TextProvider {
    private static String getOwnerName(@Nullable OfflinePlayer owner, ServerPlayerEntity source) {
        if (owner != null) {
            if (owner.getUuid().equals(source.getUuid()))
                return "your ";
            else return owner.getName() + "'s ";
        } else {
            return "";
        }
    }

    public static Supplier<Text> noWaypointFound(
            @Nullable OfflinePlayer nonOwner,
            String waypointName,
            ServerPlayerEntity source
    ) {
        final String ownerName = getOwnerName(nonOwner, source);
        return () -> Text.literal(ownerName).formatted(Colors.PLAYER)
                .append(Text.literal(" waypoint "))
                .append(Text.literal( waypointName ).formatted(Colors.LINK_INACTIVE))
                .append(Text.literal(" could not be found."))
                .formatted(Colors.DEFAULT);
    }

    public static Supplier<Text> waypointTeleportSuccess(
            ServerPlayerEntity source,
            Waypoint waypoint
    ) {
        final String ownerName = getOwnerName(waypoint.getOwnerPlayer(), source);
        return () -> Text.literal("Teleported to ")
                .append(Text.literal(ownerName).formatted(Colors.PLAYER))
                .append(waypoint.getAccessFormatted())
                .append(Text.literal(" waypoint "))
                .append(waypoint.getNameFormatted())
                .append(Text.literal("."))
                .formatted(Colors.DEFAULT);
    }
}
