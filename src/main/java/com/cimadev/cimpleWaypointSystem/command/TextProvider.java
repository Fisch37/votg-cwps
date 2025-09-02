package com.cimadev.cimpleWaypointSystem.command;

import com.cimadev.cimpleWaypointSystem.Colors;
import com.cimadev.cimpleWaypointSystem.command.persistentData.AccessLevel;
import com.cimadev.cimpleWaypointSystem.command.persistentData.OfflinePlayer;
import com.cimadev.cimpleWaypointSystem.command.persistentData.Waypoint;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
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

    public static HoverEvent getPositionTooltip(BlockPos position, RegistryKey<World> dimension) {
        return new HoverEvent(
            HoverEvent.Action.SHOW_TEXT,
            Text.literal(
                    position.getX()
                            + " " + position.getY()
                            + " " + position.getZ()
                            + " in " + dimension.getValue().toString()
        ));
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

    public static Supplier<Text> waypointMoveSuccess(
            @NotNull Waypoint waypoint,
            @NotNull BlockPos oldPos,
            @NotNull RegistryKey<World> oldDimension,
            @NotNull AccessLevel oldAccess
    ) {
        return () -> {
            HoverEvent movedTooltip = getPositionTooltip(oldPos, oldDimension);
            MutableText moved = Text.literal("Moved").formatted(Formatting.UNDERLINE);
            moved.setStyle(moved.getStyle().withHoverEvent(movedTooltip));

            MutableText message = Text.literal("")
                    .append(moved);
            if (waypoint.getAccess() == AccessLevel.OPEN) message.append(" the ").append(waypoint.getAccessFormatted());
            else message.append(" your ").append(oldAccess.getNameFormatted());
            message.append(" waypoint ")
                    .append(waypoint.getNameFormatted())
                    .append(".")
                    .formatted(Colors.DEFAULT);
            if ( oldAccess != waypoint.getAccess() ) {
                message.append(" It is now ")
                        .append(waypoint.getAccessFormatted())
                        .append(".")
                        .formatted(Colors.DEFAULT);
            }
            return message;
        };
    }
}
