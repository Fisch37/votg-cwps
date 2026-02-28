package com.cimadev.cimpleWaypointSystem.command.persistentData;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class WaypointKey implements Comparable<WaypointKey> {
    public static final Codec<WaypointKey> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(WaypointKey::getOwner),
            Codec.STRING.fieldOf("name").forGetter(WaypointKey::getName)
    ).apply(instance, WaypointKey::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, WaypointKey> PACKET_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), WaypointKey::getOwner,
            ByteBufCodecs.STRING_UTF8, WaypointKey::getName,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), WaypointKey::getOwnerName,
            (uuid, name, ownerName) -> new WaypointKey(uuid, name)
    );

    private final @NotNull Optional<UUID> owner;
    // FIXME: WaypointKey.name should be final, because it is used in hashCode.
    //  Changing the hash of a key while it is part of a hash map is undefined behaviour.
    private String name;

    public @NotNull Optional<UUID> getOwner() {
        return owner;
    }

    private Optional<String> getOwnerName() {
        return owner.map(OfflinePlayer::fromUuid)
                .map(OfflinePlayer::getName)
                ;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public WaypointKey(@NotNull UUID owner, String name) {
        this.owner = Optional.of(owner);
        this.name = name;
    }
    public WaypointKey(@NotNull Optional<@NotNull UUID> owner, @NotNull String name) {
        this.owner = owner;
        this.name = name;
    }

    public String toString() {
        return owner.map(owner -> name + "/" + owner)
                .orElseGet(() -> name + "/");
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof WaypointKey that) )
            return false;

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
        if (Objects.equals(this.owner, that.owner)) {
            return Math.clamp(this.name.compareToIgnoreCase(that.name), -1, 1);
        } else {
            if (this.owner.isEmpty())
                return -1;
            else if (that.owner.isEmpty())
                return 1;
            return Math.clamp(this.owner.get().compareTo(that.owner.get()), -1, 1);
        }
    }
}
