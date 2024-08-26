package com.cimadev.cimpleWaypointSystem.command.tpa;

import de.fisch37.datastructures.mi.MIQueue;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class TeleportRequestManager {
    private final HashMap<PlayerEntity, TeleportRequest> playerToRequest, originToRequest;
    private final MIQueue<TeleportRequest> requests;
    private final static TeleportRequestManager SINGLETON = new TeleportRequestManager();
    private long currentTick = 0;

    protected TeleportRequestManager() {
        this.playerToRequest = new HashMap<>();
        this.originToRequest = new HashMap<>();
        this.requests = new MIQueue<>();
    }

    public void tick() {
        while (true) {
            TeleportRequest head = requests.peek();
            if (head != null && head.isExpired(currentTick)){
                this.removeRequest(head);
            }
            else break;
            currentTick++;
        }
    }

    public void addRequest(TeleportRequest request) {
        request.setExpirationDate(currentTick + this.getRequestTTL());
        this.requests.add(request);
        this.playerToRequest.put(request.getTarget(), request);
        this.originToRequest.put(request.getOrigin(), request);
    }

    public @Nullable TeleportRequest getRequest(PlayerEntity target) {
        return playerToRequest.get(target);
    }

    public @Nullable TeleportRequest removeRequest(PlayerEntity target) {
        @Nullable TeleportRequest request = this.playerToRequest.remove(target);
        if (request != null) request.dropout();
        return request;
    }
    public boolean removeRequest(TeleportRequest request) {
        boolean wasRemoved = this.playerToRequest.remove(request.getTarget(), request);
        // Do not dropout if not in queue
        if (wasRemoved) request.dropout();
        return wasRemoved;
    }

    public @Nullable TeleportRequest removeRequestByOrigin(PlayerEntity origin) {
        @Nullable TeleportRequest request = this.originToRequest.remove(origin);
        if (request != null) request.dropout();
        return request;
    }

    public boolean hasRequest(PlayerEntity target) {
        return this.playerToRequest.containsKey(target);
    }

    public boolean hasOpenRequest(PlayerEntity origin) {
        return this.originToRequest.containsKey(origin);
    }

    public long getRequestTTL() {
        return 2*60*20;
    }

    public static TeleportRequestManager getInstance() {
        return SINGLETON;
    }
}
