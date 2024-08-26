package com.cimadev.cimpleWaypointSystem.command.persistentData;

import com.cimadev.cimpleWaypointSystem.Colors;
import com.cimadev.cimpleWaypointSystem.command.TextProvider;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static com.cimadev.cimpleWaypointSystem.Main.*;

public class Waypoint implements Comparable<Waypoint> {
    public static final PacketCodec<RegistryByteBuf, Waypoint> PACKET_CODEC = PacketCodec.tuple(
            WaypointKey.PACKET_CODEC, Waypoint::getKey,
            BlockPos.PACKET_CODEC, Waypoint::getPosition,
            RegistryKey.createPacketCodec(RegistryKeys.WORLD), Waypoint::getWorldRegKey,
            PacketCodecs.INTEGER, Waypoint::getYaw,
            AccessLevel.PACKET_CODEC, Waypoint::getAccess,
            Waypoint::new
    );

    private static final Logger log = LoggerFactory.getLogger(Waypoint.class);
    private final WaypointKey key;
    private BlockPos position;
    private int yaw;
    private AccessLevel access;

    private final RegistryKey<World> worldRegKey;

    public String getName() {
        return key.getName();
    }

    public BlockPos getPosition() {
        return position;
    }

    public int getYaw() {
        return yaw;
    }

    @Nullable
    public UUID getOwner() {
        return key.getOwner();
    }

    @Nullable
    public OfflinePlayer getOwnerPlayer() {
        UUID ownerUuid = this.getOwner();
        return serverState.getPlayerByUuid(ownerUuid);
    }

    public WaypointKey getKey() {
        return key;
    }

    public RegistryKey<World> getWorldRegKey() {
        return worldRegKey;
    }

    public void setName( String name ) {
        this.key.setName(name);
    }


    public void rename( String name ) {
        setName( name );
    }

    public void setPosition( BlockPos position ) {
        this.position = position;
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

    public Text getAccessFormatted() {
        return this.access.getNameFormatted();
    }

    public Text getNameFormatted() {
        HoverEvent waypointTooltip = TextProvider.getPositionTooltip(position, worldRegKey);
        ClickEvent waypointCommand;
        try {
            waypointCommand = new ClickEvent(
                    ClickEvent.Action.SUGGEST_COMMAND,
                    "/wps go " + getCommandComponent()
            );
        } catch (IllegalStateException | IllegalArgumentException e) {
            waypointCommand = null;
        }
        MutableText waypointName = Text.literal(key.getName()).formatted(Colors.LINK, Formatting.UNDERLINE);
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

    private Waypoint(WaypointKey key, BlockPos pos, RegistryKey<World> world, Integer yaw, AccessLevel access) {
        this.key = key;
        this.position = pos;
        this.worldRegKey = world;
        this.yaw = yaw;
        this.access = access;
    }

    public Waypoint(String name, BlockPos position, Double yaw, RegistryKey<World> world, UUID owner, AccessLevel access) {
        this.key = new WaypointKey(owner, name);
        this.position = position;
        this.yaw = yaw.intValue();
        this.worldRegKey = world;
        this.access = access;
    }

    private Waypoint( NbtCompound nbt ) {
        this.key = WaypointKey.fromNbt(nbt.getCompound("key"));
        int[] position = nbt.getIntArray("position");
        this.position = new BlockPos( position[0], position[1], position[2] );
        this.yaw = nbt.getInt("yaw");
        try {
            this.access = AccessLevel.fromString(nbt.getString("access"));
        } catch (IllegalArgumentException i) {
            this.access = AccessLevel.SECRET;
            log.warn("Found unknown access level while loading waypoint. Set waypoint to secret");
        }
        Identifier regKeyVal = Identifier.of(nbt.getString( "worldRegKeyValue" ));
        Identifier regKeyReg = Identifier.of(nbt.getString( "worldRegKeyRegistry" ));
        this.worldRegKey = RegistryKey.of( RegistryKey.ofRegistry(regKeyReg), regKeyVal );
    }

    public static Waypoint fromNbt( NbtCompound nbt ) {
        return new Waypoint( nbt );
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.put("key", key.toNbt());
        nbt.putIntArray("position", new int[] {position.getX(), position.getY(), position.getZ()});
        nbt.putInt("yaw", yaw);
        nbt.putString("access", access.getName());
        nbt.putString("worldRegKeyRegistry", worldRegKey.getRegistry().toString() );
        nbt.putString("worldRegKeyValue", worldRegKey.getValue().toString() );

        return nbt;
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
