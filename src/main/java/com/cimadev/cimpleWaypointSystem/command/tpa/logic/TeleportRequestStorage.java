package com.cimadev.cimpleWaypointSystem.command.tpa.logic;

import de.fisch37.datastructures.mi.MIQueue;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class TeleportRequestStorage {
    private final HashMap<PlayerEntity, @NotNull TeleportRequest> playerToRequest, originToRequest;
    private final MIQueue<@NotNull TeleportRequest> requests;
    private final static TeleportRequestStorage SINGLETON = new TeleportRequestStorage();
    private long currentTick = 0;

    protected TeleportRequestStorage() {
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
        @Nullable TeleportRequest request = this.playerToRequest.get(target);
        return removeRequest(request);
    }

    public @Nullable TeleportRequest removeRequestByOrigin(PlayerEntity origin) {
        @Nullable TeleportRequest request = this.originToRequest.get(origin);
        return removeRequest(request);
    }

    @Contract("_ -> param1")
    public @Nullable TeleportRequest removeRequest(@Nullable TeleportRequest request) {
        if (request != null) {
            boolean wasRemoved = this.playerToRequest.remove(request.getTarget(), request)
                    | this.originToRequest.remove(request.getOrigin(), request);
            // Do not drop out if not in queue
            if (wasRemoved)
                request.dropout();
        }
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

    public static TeleportRequestStorage getInstance() {
        return SINGLETON;
    }
}
