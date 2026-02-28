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

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record WaypointKey(@NotNull Optional<UUID> owner, String name) implements Comparable<WaypointKey> {
    public static final Codec<WaypointKey> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(WaypointKey::owner),
            Codec.STRING.fieldOf("name").forGetter(WaypointKey::name)
    ).apply(instance, WaypointKey::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, WaypointKey> PACKET_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), WaypointKey::owner,
            ByteBufCodecs.STRING_UTF8, WaypointKey::name,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), WaypointKey::getOwnerName,
            (uuid, name, ownerName) -> new WaypointKey(uuid, name)
    );

    private Optional<String> getOwnerName() {
        return owner.map(OfflinePlayer::fromUuid)
                .map(OfflinePlayer::getName)
                ;
    }

    public WaypointKey(@NotNull UUID owner, String name) {
        this(Optional.of(owner), name);
    }

    public WaypointKey(@NotNull Optional<@NotNull UUID> owner, @NotNull String name) {
        this.owner = owner;
        this.name = name;
    }

    public @NotNull String toString() {
        return owner.map(owner -> name + "/" + owner)
                .orElseGet(() -> name + "/");
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WaypointKey(Optional<UUID> owner1, String name1)))
            return false;

        final boolean sameName = this.name.equalsIgnoreCase(name1);
        return sameName && Objects.equals(this.owner, owner1);
    }

    /**
     * Compares two {@link WaypointKey} objects according to their lexicographical ordering.
     * <p>
     * There are two steps to the comparison:
     * <ol>
     *     <li>
     *         Comparison of ownership. If two {@link WaypointKey} objects have different owners,
     *         the ordering of the keys matches that of their owners (by name).
     *         Unowned waypoints are always considered less than owned waypoints.
     *     </li>
     *     <li>
     *         Comparison of name. If two {@link WaypointKey} objects have the same owners (or both have no owner),
     *         their ordering is determined by the lexicographical ordering of their names.
     *     </li>
     * </ol>
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
