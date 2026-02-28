package com.cimadev.cimpleWaypointSystem.command.tpa;

import com.cimadev.cimpleWaypointSystem.command.WpsUtils;
import de.fisch37.datastructures.mi.MINode;
import java.util.HashSet;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class TeleportRequest extends MINode {
        private final ServerPlayer origin;
        private final ServerPlayer target;
        private final boolean inverted;
        private long expiresAt;

        public TeleportRequest(ServerPlayer origin, ServerPlayer target, boolean inverted) {
                this.origin = origin;
                this.target = target;
                this.inverted = inverted;
        }

        public void setExpirationDate(long tick) {
                expiresAt = tick;
        }

        public boolean isExpired(long currentTick) {
                return expiresAt <= currentTick;
        }

        public Player getOrigin() {
                return origin;
        }

        public Player getTarget() {
                return target;
        }

        public boolean isInverted() {
                return inverted;
        }

        private void perform(ServerPlayer tpOrigin, ServerPlayer tpTarget) {
                WpsUtils.teleport(
                        tpOrigin,
                        tpTarget.level(),
                        tpTarget.getX(),
                        tpTarget.getY(),
                        tpTarget.getZ(),
                        tpTarget.getYRot(),
                        tpTarget.getXRot()
                );
        }

        public void perform() {
                if (inverted) {
                        perform(target, origin);
                } else {
                        perform(origin, target);
                }
        }
}
