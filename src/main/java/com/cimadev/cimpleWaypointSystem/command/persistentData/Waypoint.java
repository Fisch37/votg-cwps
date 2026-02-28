package com.cimadev.cimpleWaypointSystem.command.persistentData;

import com.cimadev.cimpleWaypointSystem.Colors;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static com.cimadev.cimpleWaypointSystem.Main.*;

public class Waypoint implements Comparable<Waypoint> {
    public static final Codec<Waypoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            WaypointKey.CODEC.fieldOf("key").forGetter(Waypoint::getKey),
            BlockPos.CODEC.fieldOf("position").forGetter(Waypoint::getPosition),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Waypoint::getWorldRegKey),
            Codec.INT.fieldOf("yaw").forGetter(Waypoint::getYaw),
            AccessLevel.CODEC.fieldOf("access").forGetter(Waypoint::getAccess)
    ).apply(instance, Waypoint::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, Waypoint> PACKET_CODEC = StreamCodec.composite(
            WaypointKey.PACKET_CODEC, Waypoint::getKey,
            BlockPos.STREAM_CODEC, Waypoint::getPosition,
            ResourceKey.streamCodec(Registries.DIMENSION), Waypoint::getWorldRegKey,
            ByteBufCodecs.INT, Waypoint::getYaw,
            AccessLevel.PACKET_CODEC, Waypoint::getAccess,
            Waypoint::new
    );

    private final WaypointKey key;
    private BlockPos position;
    private ResourceKey<Level> worldRegKey;
    private int yaw;
    private AccessLevel access;

    public Waypoint(WaypointKey key, BlockPos pos, ResourceKey<Level> world, int yaw, AccessLevel access) {
        this.key = key;
        this.position = pos;
        this.worldRegKey = world;
        this.yaw = yaw;
        this.access = access;
    }

    public String getName() {
        return key.name();
    }

    public BlockPos getPosition() {
        return position;
    }

    public int getYaw() {
        return yaw;
    }

    @Nullable
    public UUID getOwner() {
        return key.owner().orElse(null);
    }

    @Nullable
    public OfflinePlayer getOwnerPlayer() {
        UUID ownerUuid = this.getOwner();
        return serverState.getPlayerByUuid(ownerUuid);
    }

    public WaypointKey getKey() {
        return key;
    }

    public ResourceKey<Level> getWorldRegKey() {
        return worldRegKey;
    }

    /// Creates a new {@link Waypoint} with the same data as the old one,
    /// <em>except</em> its name, which will be set to `name`.
    // Cloning the Waypoint object is necessary, because WaypointKey must always be immutable.
    // Replacing solely Waypoint#key is also dangerous, because doing so may desync a HashMap<WaypointKey, Waypoint>
    public Waypoint withName(String name) {
        return new Waypoint(
                new WaypointKey(this.key.owner(), name),
                this.position,
                this.worldRegKey,
                this.yaw,
                this.access
        );
    }

    public void setPosition( BlockPos position, ResourceKey<Level> dimension ) {
        this.position = position;
        this.worldRegKey = dimension;
    }

    public void setYaw( int yaw ) {
        this.yaw = yaw;
    }

    public void setAccess( AccessLevel access ) {
        if ( access != AccessLevel.OPEN ) {
            this.access = access;
        }
    }

    public AccessLevel getAccess() {
        return access;
    }

    public Component getAccessFormatted() {
        return this.access.getNameFormatted();
    }

    public Component getNameFormatted() {
        HoverEvent waypointTooltip = new HoverEvent.ShowText(
                Component.literal(
                        position.getX()
                                + " " + position.getY()
                                + " " + position.getZ()
                                + " in " + worldRegKey.identifier().toString()
                ));
        ClickEvent waypointCommand;
        try {
            waypointCommand = new ClickEvent.SuggestCommand(
                    "/wps go " + getCommandComponent()
            );
        } catch (IllegalStateException | IllegalArgumentException e) {
            waypointCommand = null;
        }
        MutableComponent waypointName = Component.literal(key.name()).withStyle(Colors.LINK, ChatFormatting.UNDERLINE);
        Style waypointStyle = waypointName.getStyle()
                .withHoverEvent(waypointTooltip);
        if (waypointCommand != null)
            waypointStyle = waypointStyle.withClickEvent(waypointCommand);
        waypointName.setStyle(waypointStyle);
        return waypointName;
    }

    private String wrapString(String s) {
            return "\"" + s + "\"";
    }

    public String getNameForCommand() {
        return wrapString(getName());
    }

    /**
     * Creates a string that would be an allowed identifier for this waypoint in a /wps command.
     * This consists of the waypoint name (quoted if necessary) and the owner (or "open" if the waypoint is open)
     * @return The command component for this waypoint or <code>null</code> if the waypoint is in an invalid state
     * @throws IllegalStateException If the owner of this waypoint does not exist in the cache (which should be impossible)
     * @throws IllegalArgumentException If the waypoint is non-open but does not have an owner (e.g. unowned secret)
     */
    public @Nullable String getCommandComponent() throws IllegalStateException {
        String ownerPart;
        if (access == AccessLevel.OPEN)
            ownerPart = AccessLevel.OPEN.getName();
        else {
            if (getOwner() == null)
                throw new IllegalArgumentException("Waypoint %s is non-open without an owner".formatted(this));
            OfflinePlayer player = getOwnerPlayer();
            if (player == null)
                throw new IllegalStateException(
                        "Waypoint#getCommandComponent Could not find an OfflinePlayer for waypoint %s"
                                .formatted(this)
                );
            ownerPart = player.getName();
        }
        return getNameForCommand() + " " + ownerPart;
    }

    /**
     * Compares two {@link Waypoint}s by their keys.
     * This is shorthand for <code>this.getKey().compareTo(that.getKey())</code>
     * @param that the object to be compared.
     * @return The ordering of the {@link Waypoint}s according to {@link WaypointKey#compareTo}
     */
    @Override
    public int compareTo(@NotNull Waypoint that) {
        return this.key.compareTo(that.key);
    }
}
