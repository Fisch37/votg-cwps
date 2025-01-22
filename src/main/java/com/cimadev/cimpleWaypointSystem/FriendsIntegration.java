package com.cimadev.cimpleWaypointSystem;

import com.cimadev.cimpleWaypointSystem.command.persistentData.OfflinePlayer;
import net.fabricmc.loader.api.FabricLoader;

import java.util.*;
import java.util.function.Supplier;

import static com.cimadev.cimpleWaypointSystem.FriendsEntrypoint.api;

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
 *          <li>
 *              Always use {@link FriendsIntegration#runIfPossible}, {@link FriendsIntegration#runIfPossibleDelayable}
 *              or {@link FriendsIntegration#getIfPossible}
 *              to execute code that uses the API in <em>any</em> way.
 *              Ideally the exposed methods should just be wrappers into one of the methods mentioned above.
 *          </li>
 *          <li>Some other stuff, idk. Just test afterwards. Test well</li>
 *      </ol>
 */
public abstract class FriendsIntegration {
    private static final String INTEGRATION = "fischyfriends";
    private static final LinkedList<Runnable> delayedActions = new LinkedList<>();

    static void executeDelayedActions() {
        while (!delayedActions.isEmpty()) {
            delayedActions.pop().run();
        }
    }

    private static boolean isInstalled() {
        return FabricLoader.getInstance().isModLoaded(INTEGRATION);
    }

    private static boolean isLoaded() {
        return api != null;
    }

    private static void runIfPossible(Runnable action) {
        if (isInstalled() && isLoaded()) {
            action.run();
        }
    }


    private static void runIfPossibleDelayable(Runnable action) {
        if (isInstalled()) {
            if (isLoaded()) {
                action.run();
            } else {
                delayedActions.add(action);
            }
        }
    }

    private static <T> T getIfPossible(Supplier<T> supplier, Supplier<T> defaultSupplier) {
        if (isInstalled()) {
            if (!isLoaded()) throw new RuntimeException("FriendsAPI is not loaded yet");
            return supplier.get();
        } else {
            return defaultSupplier.get();
        }
    }

    public static Collection<UUID> getFriendUuids(UUID uuid) {
        return getIfPossible(() -> api.getFriends(uuid), List::of);
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
        if (a.equals(b))
            // Self-hate is not part of our user model
            return true;
        return getIfPossible(() -> api.areFriends(a, b), () -> false);
    }

    public static void sendFriendRequest(UUID from, UUID to) {
        runIfPossibleDelayable(
                () -> api.getRequestManager().addFriendRequest(
                                new de.fisch37.fischyfriends.api.FriendRequest(from, to)
                        )
        );
    }

    public static void makeFriends(UUID a, UUID b) {
        runIfPossibleDelayable(() -> api.addFriendship(a, b));
    }
}
