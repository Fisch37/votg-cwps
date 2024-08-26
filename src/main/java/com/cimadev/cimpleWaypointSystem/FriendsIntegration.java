package com.cimadev.cimpleWaypointSystem;

import com.cimadev.cimpleWaypointSystem.command.persistentData.OfflinePlayer;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static de.fisch37.fischyfriends.FischyFriends.getAPI;

/**
 *  <h1>IMPORTANT!!!</h1>
 *  <p>
 *  This code is <em>very</em> fragile since it has to avoid a crash
 *  when FischyFriends is not installed.
 *  <p>
 *  Try to avoid changing this code if you don't know what you're doing.
 *  If you do have to change this code:
 *      <ol>
 *          <li>Never explicitly reference classes from the API</li>
 *          <li>Always use <code>isInstalled()</code> before using <code>getAPI()</code></li>
 *          <li>Some other stuff, idk. Just test afterwards. Test well</li>
 *      </ol>
 */
public abstract class FriendsIntegration {
    private static final String INTEGRATION = "fischyfriends";

    private static boolean isInstalled() {
        return FabricLoader.getInstance().isModLoaded(INTEGRATION);
    }

    public static Collection<UUID> getFriendUuids(UUID uuid) {
        if (isInstalled()) {
            return getAPI().getFriends(uuid);
        } else {
            return List.of();
        }
    }
    public static Collection<UUID> getFriendUuids(OfflinePlayer player) {
        return getFriendUuids(player.getUuid());
    }

    public static Collection<OfflinePlayer> getFriends(UUID uuid) {
        Collection<UUID> friends = getFriendUuids(uuid);

        ArrayList<OfflinePlayer> friendPlayers = new ArrayList<>(friends.size());
        for (UUID friend : friends) {
            friendPlayers.add(Main.serverState.getPlayerByUuid(friend));
        }
        return friendPlayers;
    }
    public static Collection<OfflinePlayer> getFriends(OfflinePlayer player) {
        return getFriends(player.getUuid());
    }

    public static boolean areFriends(UUID a, UUID b) {
        if (isInstalled()) {
            return getAPI().areFriends(a, b);
        } else {
            return false;
        }
    }
}
