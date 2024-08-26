package com.cimadev.cimpleWaypointSystem.command.persistentData;

import com.cimadev.cimpleWaypointSystem.Colors;
import com.cimadev.cimpleWaypointSystem.Main;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Uuids;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public class OfflinePlayer implements Comparable<OfflinePlayer> {

    public static final PacketCodec<RegistryByteBuf, OfflinePlayer> PACKET_CODEC_LIMITED = PacketCodec.tuple(
            Uuids.PACKET_CODEC, OfflinePlayer::getUuid,
            PacketCodecs.STRING, OfflinePlayer::getName,
            OfflinePlayer::new
    );
    public static final DynamicCommandExceptionType INVALID_PLAYER_NAME = new DynamicCommandExceptionType(
            /*todo: change to PLAYER_COLOR*/
            o -> Text.literal("Player ").append( Text.literal( o+"" ).formatted(Colors.LINK_INACTIVE) )
                    .append( " not found. Did they change their name?").formatted(Colors.DEFAULT));

    private final UUID uuid;
    private String name;

    public UUID getUuid() {
        return this.uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
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

    OfflinePlayer( NbtCompound nbt ) {
        this.uuid = nbt.getUuid( "uuid" ); /*todo: check invalid uuid*/
        this.name = nbt.getString( "name" );
    }

    public NbtCompound toNbt( ) {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("uuid", uuid);
        nbt.putString("name", name);
        return nbt;
    }

    public static OfflinePlayer fromNbt( NbtCompound nbt ) {
        return new OfflinePlayer( nbt );
    }

    public static OfflinePlayer fromName(String name) throws NullPointerException {
        return Main.serverState.getPlayerByName( name );
    }

    public static OfflinePlayer fromUuid(UUID uuid){
        return Main.serverState.getPlayerByUuid(uuid);
    }

    public static OfflinePlayer fromContext(CommandContext<ServerCommandSource> context, String id ) throws CommandSyntaxException {
        String playerName = StringArgumentType.getString(context, id);
        try {
            return fromName(playerName);
        } catch ( NullPointerException n ) {
            throw INVALID_PLAYER_NAME.create( playerName );
        }
    }

}
