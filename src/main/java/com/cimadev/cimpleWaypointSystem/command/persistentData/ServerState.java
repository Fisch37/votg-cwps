package com.cimadev.cimpleWaypointSystem.command.persistentData;

import Type;
import com.cimadev.cimpleWaypointSystem.FriendsIntegration;
import com.cimadev.cimpleWaypointSystem.Main;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ServerState extends SavedData {

    private final HashMap<WaypointKey, Waypoint> worldWideWaypoints = new HashMap<>();
    private final HashMap<UUID, PlayerHome> playerHomes = new HashMap<>();
    private final HashMap<String, OfflinePlayer> playersByName = new HashMap<>();
    private final HashMap<UUID, OfflinePlayer> playersByUuid = new HashMap<>();

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

    @Override
    public CompoundTag writeNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        // todo: build a playerList, waypointList and homesList NbtElement to avoid redundancy of key (if possible)
        ListTag pList = new ListTag();
        playersByUuid.values().forEach( offlinePlayer -> pList.add(offlinePlayer.toNbt()) );
        nbt.put("playerList", pList);

        ListTag waypointList = new ListTag();
        worldWideWaypoints.values().forEach( waypoint -> waypointList.add(waypoint.toNbt()) );
        nbt.put("waypoints",waypointList);

        ListTag playerHomesList = new ListTag();
        playerHomes.values().forEach( playerHome -> playerHomesList.add(playerHome.toNbt()) );
        nbt.put("playerHomes", playerHomesList);

        DataFixer.setToCurrentVersion(nbt);

        return nbt;
    }

    public static ServerState createFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        CompoundTag oldTag = tag;
        tag = DataFixer.fixData(tag);


        ServerState serverState = new ServerState();
        if (!DataFixer.isCurrentVersion(oldTag))
            serverState.setDirty();
        ListTag pList = tag.getList("playerList", Tag.TAG_COMPOUND);
        pList.forEach( nbt -> serverState.loadPlayer( OfflinePlayer.fromNbt((CompoundTag) nbt)) );

        ListTag waypointList = tag.getList("waypoints", Tag.TAG_COMPOUND);
        waypointList.forEach( nbt -> serverState.setWaypoint( Waypoint.fromNbt((CompoundTag) nbt) ) );

        ListTag playerHomesCompound = tag.getList("playerHomes", Tag.TAG_COMPOUND);
        playerHomesCompound.forEach(compound -> serverState.setPlayerHome( PlayerHome.fromNbt((CompoundTag) compound) ) );

        return serverState;
    }


    private final static Type<ServerState> type = new Type<>(
            ServerState::new,
            ServerState::createFromNbt,
            null
    );

    public static ServerState getServerState(MinecraftServer server) {
        // FIXME: This breaks mod compatibility when a mod removes the overworld. Yes that can happen.
        DimensionDataStorage persistentStateManager = server
                .getLevel(Level.OVERWORLD).getDataStorage();

        return persistentStateManager.computeIfAbsent(
                type,
                Main.MOD_ID
        );
    }
}
