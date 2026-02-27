package com.cimadev.cimpleWaypointSystem.command.persistentData;

import com.cimadev.cimpleWaypointSystem.Colors;
import com.cimadev.cimpleWaypointSystem.Main;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;


public class OfflinePlayer implements Comparable<OfflinePlayer> {

    public static final DynamicCommandExceptionType INVALID_PLAYER_NAME = new DynamicCommandExceptionType(
            /*todo: change to PLAYER_COLOR*/
            o -> Component.literal("Player ").append( Component.literal( o+"" ).withStyle(Colors.LINK_INACTIVE) )
                    .append( " not found. Did they change their name?").withStyle(Colors.DEFAULT));

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

    OfflinePlayer( CompoundTag nbt ) {
        this.uuid = nbt.getUuid( "uuid" ); /*todo: check invalid uuid*/
        this.name = nbt.getString( "name" );
    }

    public CompoundTag toNbt( ) {
        CompoundTag nbt = new CompoundTag();
        nbt.putUuid("uuid", uuid);
        nbt.putString("name", name);
        return nbt;
    }

    public static OfflinePlayer fromNbt( CompoundTag nbt ) {
        return new OfflinePlayer( nbt );
    }

    public static OfflinePlayer fromName(String name) throws NullPointerException {
        return Main.serverState.getPlayerByName( name );
    }

    public static OfflinePlayer fromUuid(UUID uuid){
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
