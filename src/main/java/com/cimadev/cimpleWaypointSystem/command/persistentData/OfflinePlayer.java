package com.cimadev.cimpleWaypointSystem.command.persistentData;

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

import static com.cimadev.cimpleWaypointSystem.Main.DEFAULT_COLOR;
import static com.cimadev.cimpleWaypointSystem.Main.LINK_INACTIVE_COLOR;

public class OfflinePlayer implements Comparable<OfflinePlayer> {

    public static final PacketCodec<RegistryByteBuf, OfflinePlayer> PACKET_CODEC_LIMITED = PacketCodec.tuple(
            Uuids.PACKET_CODEC, OfflinePlayer::getUuid,
            PacketCodecs.STRING, OfflinePlayer::getName,
            OfflinePlayer::new
    );
    public static final DynamicCommandExceptionType INVALID_PLAYER_NAME = new DynamicCommandExceptionType(
            /*todo: change to PLAYER_COLOR*/
            o -> Text.literal("Player ").append( Text.literal( o+"" ).formatted(LINK_INACTIVE_COLOR) )
                    .append( " not found. Did they change their name?").formatted(DEFAULT_COLOR));

    private final UUID uuid;
    private String name;
    private final TreeSet<UUID> friends = new TreeSet<>();

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

    public boolean addFriend(OfflinePlayer friend) {
        return friends.add(friend.getUuid());
    }

    public boolean removeFriend(OfflinePlayer notFriend) {
        return friends.remove(notFriend.getUuid());
    }

    public List<OfflinePlayer> getFriends() {
        LinkedList<OfflinePlayer> playerFriends = new LinkedList<>();
        for (UUID friendUuid : friends) {
            playerFriends.add(OfflinePlayer.fromUuid(friendUuid));
        }
        return playerFriends;
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

        NbtCompound friendList = nbt.getCompound("friendList");
        friendList.getKeys().forEach(key -> {
            this.friends.add( UUID.fromString(key) );
            this.friends.add( UUID.fromString(friendList.getString(key)) );
        });
    }

    public boolean likes(UUID maybeFriend) {
        if ( maybeFriend.equals(uuid) ) return true;
        return friends.contains(maybeFriend);
    }

    public NbtCompound toNbt( ) {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("uuid", uuid);
        nbt.putString("name", name);
        int friendnum = friends.size();

        NbtCompound friendList = new NbtCompound();
        while( friends.size() > 1 ) {
            friendList.putString( friends.pollFirst().toString() , friends.pollLast().toString() );
        }
        if (friendnum == 1) {
            String lastUuid = friends.pollFirst().toString();
            friendList.putString(lastUuid, lastUuid);
        }

        nbt.put("friendList", friendList);
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
