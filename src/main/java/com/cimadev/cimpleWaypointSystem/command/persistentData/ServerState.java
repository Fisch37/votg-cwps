package com.cimadev.cimpleWaypointSystem.command.persistentData;

import com.cimadev.cimpleWaypointSystem.FriendsIntegration;
import com.cimadev.cimpleWaypointSystem.Main;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ServerState extends PersistentState {

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

    public void setPlayer(ServerPlayerEntity player) {
        String playerName = player.getName().getString();
        UUID playerUuid = player.getUuid();

        setPlayer( playerName, playerUuid );
    }

    public boolean waypointAccess(Waypoint waypoint, ServerPlayerEntity player) {
        return waypointAccess(waypoint, player.getUuid());
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

        this.markDirty();
    }

    private void loadPlayer(OfflinePlayer player) {
        playersByUuid.put(player.getUuid(), player);
        playersByName.put(player.getName(), player);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        // todo: build a playerList, waypointList and homesList NbtElement to avoid redundancy of key (if possible)
        NbtList pList = new NbtList();
        playersByUuid.values().forEach( offlinePlayer -> pList.add(offlinePlayer.toNbt()) );
        nbt.put("playerList", pList);

        NbtList waypointList = new NbtList();
        worldWideWaypoints.values().forEach( waypoint -> waypointList.add(waypoint.toNbt()) );
        nbt.put("waypoints",waypointList);

        NbtList playerHomesList = new NbtList();
        playerHomes.values().forEach( playerHome -> playerHomesList.add(playerHome.toNbt()) );
        nbt.put("playerHomes", playerHomesList);

        DataFixer.setToCurrentVersion(nbt);

        return nbt;
    }

    public static ServerState createFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag = DataFixer.fixData(tag);

        ServerState serverState = new ServerState();
        NbtList pList = tag.getList("playerList", NbtElement.COMPOUND_TYPE);
        pList.forEach( nbt -> serverState.loadPlayer( OfflinePlayer.fromNbt((NbtCompound) nbt)) );

        NbtList waypointList = tag.getList("waypoints", NbtElement.COMPOUND_TYPE);
        waypointList.forEach( nbt -> serverState.setWaypoint( Waypoint.fromNbt((NbtCompound) nbt) ) );

        NbtList playerHomesCompound = tag.getList("playerHomes", NbtElement.COMPOUND_TYPE);
        playerHomesCompound.forEach(compound -> serverState.setPlayerHome( PlayerHome.fromNbt((NbtCompound) compound) ) );

        return serverState;
    }


    private final static Type<ServerState> type = new Type<>(
            ServerState::new,
            ServerState::createFromNbt,
            null
    );

    public static ServerState getServerState(MinecraftServer server) {
        // FIXME: This breaks mod compatibility when a mod removes the overworld. Yes that can happen.
        PersistentStateManager persistentStateManager = server
                .getWorld(World.OVERWORLD).getPersistentStateManager();

        return persistentStateManager.getOrCreate(
                type,
                Main.MOD_ID
        );
    }
}
