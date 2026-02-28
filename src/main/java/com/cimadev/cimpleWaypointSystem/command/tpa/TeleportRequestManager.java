package com.cimadev.cimpleWaypointSystem.command.tpa;

import com.cimadev.cimpleWaypointSystem.Main;
import de.fisch37.datastructures.mi.MIQueue;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import net.minecraft.world.entity.player.Player;

public class TeleportRequestManager {
    private final HashMap<Player, TeleportRequest> targetToRequest, originToRequest;
    private final MIQueue<TeleportRequest> requests;
    private final static TeleportRequestManager SINGLETON = new TeleportRequestManager();
    private long currentTick = 0;

    protected TeleportRequestManager() {
        this.targetToRequest = new HashMap<>();
        this.originToRequest = new HashMap<>();
        this.requests = new MIQueue<>();
    }

    public void tick() {
        while (true) {
            TeleportRequest head = requests.peek();
            if (head != null && head.isExpired(currentTick)){
                this.removeRequestByTarget(head);
            }
            else break;
            currentTick++;
        }
    }

    public void addRequest(TeleportRequest request) {
        request.setExpirationDate(currentTick + this.getRequestTTL());
        this.requests.add(request);
        this.targetToRequest.put(request.getTarget(), request);
        this.originToRequest.put(request.getOrigin(), request);
    }

    public @Nullable TeleportRequest getRequest(Player target) {
        return targetToRequest.get(target);
    }

    public @Nullable TeleportRequest removeRequestByTarget(Player target) {
        @Nullable TeleportRequest request = this.targetToRequest.get(target);
        if (request != null) {
            if (!removeRequestByTarget(request)) {
                throw new IllegalStateException(
                        "removeRequestByTarget tried to remove a looked-up request, but it didn't exist!"
                        + " This is probably because someone is using TeleportRequestManager in parallel. Don't!"
                );
            }
        }
        return request;
    }
    public boolean removeRequestByTarget(TeleportRequest request) {
        boolean playerToRequestRemoved = this.targetToRequest.remove(request.getTarget(), request);
        boolean originToRequestRemoved = this.originToRequest.remove(request.getOrigin(), request);
        boolean wasRemoved = playerToRequestRemoved | originToRequestRemoved;
        if (playerToRequestRemoved != originToRequestRemoved) {
            Main.LOGGER.warn(
                    "TeleportRequestManager has desynced! Tried to remove request {},"
                    + " but only found it in one of the parallel maps! Target->Req: {}, Origin->Req: {}",
                    request,
                    playerToRequestRemoved,
                    originToRequestRemoved
            );
        }
        // Do not dropout if not in queue
        if (wasRemoved) request.dropout();
        return wasRemoved;
    }

    public @Nullable TeleportRequest removeRequestByOrigin(Player origin) {
        @Nullable TeleportRequest request = this.originToRequest.get(origin);
        if (request != null) {
            if (!removeRequestByTarget(request)) {
                throw new IllegalStateException(
                        "removeRequestByOrigin tried to remove a looked-up request, but it didn't exist!"
                        + " This is most likely because someone is using TeleportRequestManager in parallel."
                );
            }
        }
        return request;
    }

    public boolean hasRequest(Player target) {
        return this.targetToRequest.containsKey(target);
    }

    public boolean hasOpenRequest(Player origin) {
        return this.originToRequest.containsKey(origin);
    }

    public long getRequestTTL() {
        return 2*60*20;
    }

    public static TeleportRequestManager getInstance() {
        return SINGLETON;
    }
}
