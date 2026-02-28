package com.cimadev.cimpleWaypointSystem.command.persistentData;

import com.cimadev.cimpleWaypointSystem.Colors;
import com.cimadev.cimpleWaypointSystem.Main;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;


public class OfflinePlayer implements Comparable<OfflinePlayer> {
    public static final Codec<OfflinePlayer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("uuid").forGetter(OfflinePlayer::getUuid),
            Codec.STRING.fieldOf("name").forGetter(OfflinePlayer::getName)
    ).apply(instance, OfflinePlayer::new));

    public static final DynamicCommandExceptionType INVALID_PLAYER_NAME = new DynamicCommandExceptionType(
            /*todo: change to PLAYER_COLOR*/
            o -> Component.literal("Player ").append( Component.literal( o+"" ).withStyle(Colors.LINK_INACTIVE) )
                    .append( " not found. Did they change their name?").withStyle(Colors.DEFAULT));

    private final @NotNull UUID uuid;
    private @NotNull String name;

    public @NotNull UUID getUuid() {
        return this.uuid;
    }

    public @NotNull String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public void emptyName() {
        this.name = "";
    }

    @Override
    public int compareTo(@NotNull OfflinePlayer that) {
        return this.uuid.compareTo(that.getUuid());
    }

    @Override
    public boolean equals(Object that) {
        if ( that instanceof OfflinePlayer ) return this.uuid.equals( ( (OfflinePlayer) that ).getUuid() );
        return false;
    }

    public OfflinePlayer (@NotNull UUID uuid, @NotNull String name) {
        this.name = name;
        this.uuid = uuid;
    }

    public static OfflinePlayer fromName(String name) throws NullPointerException {
        return Main.serverState.getPlayerByName(name);
    }

    public static @Nullable OfflinePlayer fromUuid(UUID uuid){
        return Main.serverState.getPlayerByUuid(uuid);
    }

    public static OfflinePlayer fromContext(CommandContext<CommandSourceStack> context, String id ) throws CommandSyntaxException {
        String playerName = StringArgumentType.getString(context, id);
        try {
            return fromName(playerName);
        } catch ( NullPointerException n ) {
            throw INVALID_PLAYER_NAME.create( playerName );
        }
    }

}
