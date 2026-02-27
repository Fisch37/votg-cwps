package com.cimadev.cimpleWaypointSystem.command.persistentData;

import com.cimadev.cimpleWaypointSystem.network.NullableCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class WaypointKey implements Comparable<WaypointKey> {
    public static final StreamCodec<RegistryFriendlyByteBuf, WaypointKey> PACKET_CODEC = StreamCodec.composite(
            new NullableCodec<>(UUIDUtil.STREAM_CODEC), WaypointKey::getOwner,
            ByteBufCodecs.STRING_UTF8, WaypointKey::getName,
            new NullableCodec<>(ByteBufCodecs.STRING_UTF8), WaypointKey::getOwnerName,
            (uuid, name, ownerName) -> new WaypointKey(uuid, name)
    );

    @Nullable
    private final UUID owner;
    private String name;

    public @Nullable UUID getOwner() {
        return owner;
    }

    private @Nullable String getOwnerName() {
        return owner == null ? null : OfflinePlayer.fromUuid(owner).getName();
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public WaypointKey(@Nullable UUID owner, String name) {
        this.owner = owner;
        this.name = name;
    }

    public String toString() {
        if ( this.owner == null ) return name+"/";
        return name+"/"+owner;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("name", this.name);
        if (this.owner != null) nbt.putUuid("owner", this.owner);

        return nbt;
    }

    public static WaypointKey fromNbt( CompoundTag nbt ) {
        return new WaypointKey(
                nbt.contains("owner") ? nbt.getUuid("owner") : null,
                nbt.getString("name")
        );
    }

    @Override
    public int hashCode() {
        if ( this.owner == null ) return name.toLowerCase().hashCode();
        return owner.hashCode() * name.toLowerCase().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof WaypointKey that) ) return false;

        final boolean sameName = this.name.equalsIgnoreCase(that.name);
        return sameName && Objects.equals(this.owner, that.owner);
    }

    /**
     * Compares two {@link WaypointKey} objects according to their lexicographical ordering.
     * <p>
     * There are two steps to the comparison:
     * 1. Comparison of ownership. If two {@link WaypointKey} objects have different owners,
     *      the ordering of the keys matches that of their owners (by name).
     *      Unowned waypoints are always considered less than owned waypoints.
     * 2. Comparison of name. If two {@link WaypointKey} objects have the same owners (or both have no owner),
     *      their ordering is determined by the lexicographical ordering of their names.
     * <p>
     * <em>API Note:</em> For consistency, the only possible values returned by this comparator are -1, 0, 1.
     */
    @Override
    public int compareTo(@NotNull WaypointKey that) {
        if (Objects.equals(this.owner, that.getOwner())) {
            return Math.clamp(this.name.compareToIgnoreCase(that.name), -1, 1);
        } else {
            if (this.owner == null)
                return -1;
            else if (that.owner == null)
                return 1;
            return Math.clamp(this.owner.compareTo(that.owner), -1, 1);
        }
    }
}
