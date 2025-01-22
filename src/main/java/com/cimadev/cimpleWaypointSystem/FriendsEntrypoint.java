package com.cimadev.cimpleWaypointSystem;

import de.fisch37.fischyfriends.api.FischyFriendsEntrypoint;
import de.fisch37.fischyfriends.api.FriendsAPI;

public class FriendsEntrypoint implements FischyFriendsEntrypoint {
    static FriendsAPI api = null;

    @Override
    public void onFriendsInitialised(FriendsAPI friendsAPI) {
        api = friendsAPI;
        FriendsIntegration.executeDelayedActions();
    }
}
