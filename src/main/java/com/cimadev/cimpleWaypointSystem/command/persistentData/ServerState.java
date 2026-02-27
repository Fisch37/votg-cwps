package com.cimadev.cimpleWaypointSystem.command.persistentData;

import com.cimadev.cimpleWaypointSystem.FriendsIntegration;
import com.cimadev.cimpleWaypointSystem.Main;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ServerState extends SavedData {
    private static final Codec<ServerState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            // TODO: Is this truly better than using Maps?
            //  +: Lists use the minimum amount of space to express all information
            //  +: Lists reduce the cross-dependency of our stored data, meaning it is easier to parse in data-fixers
            //  -: Map codecs are likely faster, saving on a serialisation layer during serialization.
            //  -: But maybe not that much, since we have a considerable O(n) layer anyway.
            Waypoint.CODEC.listOf()
                    .fieldOf("waypoints")
                    .forGetter(o -> o.worldWideWaypoints.values().stream().toList()),
            PlayerHome.CODEC.listOf()
                    .fieldOf("homes")
                    .forGetter(o -> o.playerHomes.values().stream().toList()),
            OfflinePlayer.CODEC.listOf()
                    .fieldOf("player_cache")
                    .forGetter(o -> o.playersByUuid.values().stream().toList())
    ).apply(instance, ServerState::new));

    private final Map<WaypointKey, Waypoint> worldWideWaypoints;
    private final Map<UUID, PlayerHome> playerHomes;
    private final Map<String, OfflinePlayer> playersByName;
    private final Map<UUID, OfflinePlayer> playersByUuid;

    public ServerState() {
        worldWideWaypoints = new HashMap<>();
        playerHomes = new HashMap<>();
        playersByName = new HashMap<>();
        playersByUuid = new HashMap<>();
    }
    private ServerState(
            List<Waypoint> waypoints,
            List<PlayerHome> homes,
            List<OfflinePlayer> playerCache
    ) {
        worldWideWaypoints = waypoints.stream()
                .collect(Collectors.toMap(Waypoint::getKey, Function.identity()));
        playerHomes = homes.stream()
                .collect(Collectors.toMap(PlayerHome::getOwner, Function.identity()));
        playersByUuid = new HashMap<>();
        playersByName = new HashMap<>();
        for (var player : playerCache) {
            playersByUuid.put(player.getUuid(), player);
            playersByName.put(player.getName(), player);
        }
    }

    public void setPlayerHome( PlayerHome playerHome ) {
        playerHomes.put(playerHome.getOwner(), playerHome);
    }

    public void removePlayerHome(UUID uuid) {
        playerHomes.remove(uuid);
    }

    public PlayerHome getPlayerHome(UUID uuid) {
        return playerHomes.get(uuid);
    }

    public void setWaypoint(Waypoint waypoint) {
        worldWideWaypoints.put(waypoint.getKey(), waypoint);
    }

    public void removeWaypoint(WaypointKey waypointKey) {
        worldWideWaypoints.remove(waypointKey);
    }

    public @Nullable Waypoint getWaypoint(WaypointKey waypointKey) {
        return worldWideWaypoints.get(waypointKey);
    }

    public boolean waypointExists(WaypointKey waypointKey) {
        return worldWideWaypoints.containsKey(waypointKey);
    }

    public Collection<Waypoint> getAllWaypoints() {
        return worldWideWaypoints.values();
    }

    public @Nullable OfflinePlayer getPlayerByName(String name) {
        return playersByName.get(name);
    }

    public @Nullable OfflinePlayer getPlayerByUuid(UUID uuid) {
        return playersByUuid.get(uuid);
    }

    public Iterable<String> getPlayerNames() {
        Stack<String> playerNames = new Stack<>();
        playersByName.forEach((name, uuid) -> playerNames.push(name));
        return playerNames;
    }

    public void setPlayer(ServerPlayer player) {
        String playerName = player.getName().getString();
        UUID playerUuid = player.getUUID();

        setPlayer( playerName, playerUuid );
    }

    public boolean waypointAccess(Waypoint waypoint, ServerPlayer player) {
        return waypointAccess(waypoint, player.getUUID());
    }

    public boolean waypointAccess(Waypoint waypoint, OfflinePlayer player) {
        return waypointAccess(waypoint, player.getUuid());
    }

    public boolean waypointAccess(Waypoint waypoint, UUID playerUuid) {
        OfflinePlayer owner = waypoint.getOwnerPlayer();
        if ( waypoint.getAccess() == AccessLevel.OPEN || waypoint.getAccess() == AccessLevel.PUBLIC ) return true;       // all public waypoints freely accessible

        // only happens if access type of an open waypoint was corrupted in NBT. In this case, ownerUuid == null && AccessLevel.SECRET
        // waypoint only visible by admins by listing all waypoints
        if ( owner == null ) return false;

        if ( waypoint.getAccess() == AccessLevel.PRIVATE && FriendsIntegration.areFriends(owner.getUuid(), playerUuid) ) {
            return true;
        }

        return waypoint.getAccess() == AccessLevel.SECRET && owner.getUuid().equals(playerUuid);
    }

    private void setPlayer(String playerName, UUID playerUuid) {
        OfflinePlayer pByUuid = playersByUuid.get(playerUuid);
        OfflinePlayer pByName = playersByName.get(playerName);

        if ( pByUuid == null && pByName == null ) {     // player is new and there is not a player with the same name in the tables
            OfflinePlayer newPlayer = new OfflinePlayer(playerUuid, playerName);
            playersByUuid.put(playerUuid, newPlayer);
            playersByName.put(playerName, newPlayer);
        } else if ( pByUuid == null ) {                 // player is new but there is a player with the same name in the tables
            OfflinePlayer newPlayer = new OfflinePlayer(playerUuid, playerName);
            pByName.emptyName();
            playersByUuid.put(playerUuid, newPlayer);
            playersByName.put(playerName, newPlayer);
        } else if ( pByName == null ) {                 // player is not new but has changed name
            playersByName.remove(pByUuid.getName());
            playersByName.put(playerName, pByUuid);
            pByUuid.setName(playerName);
        } else {
            if ( pByUuid.equals(pByName) ) return;      // player is already in table, all good (shouldn't happen)

            // player is not new, but has a new name that another player had previously
            pByName.emptyName();
            pByUuid.setName(playerName);
            playersByName.remove(pByUuid.getName());
            playersByName.put(playerName, pByUuid);
        }

        this.setDirty();
    }

    private void loadPlayer(OfflinePlayer player) {
        playersByUuid.put(player.getUuid(), player);
        playersByName.put(player.getName(), player);
    }

    private static final SavedDataType<ServerState> TYPE = new SavedDataType<>(
            Main.MOD_ID,
            ServerState::new,
            CODEC,
            // FIXME: Passing 'null' argument to parameter annotated as @NotNu
            //  Figure out what should go here instead.
            null
    );

    public static ServerState getServerState(MinecraftServer server) {
        return Optional.ofNullable(server.getLevel(Level.OVERWORLD))
                .map(level -> level.getDataStorage().computeIfAbsent(TYPE))
                .orElseGet(ServerState::new);
    }
}
