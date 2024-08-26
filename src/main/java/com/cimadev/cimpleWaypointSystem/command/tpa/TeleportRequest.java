package com.cimadev.cimpleWaypointSystem.command.tpa;

import de.fisch37.datastructures.mi.MINode;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashSet;

public class TeleportRequest extends MINode {
        private final PlayerEntity origin, target;
        private final boolean inverted;
        private long expiresAt;

        public TeleportRequest(PlayerEntity origin, PlayerEntity target, boolean inverted) {
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

        public PlayerEntity getOrigin() {
                return origin;
        }

        public PlayerEntity getTarget() {
                return target;
        }

        public boolean isInverted() {
                return inverted;
        }

        private void perform(PlayerEntity tpOrigin, PlayerEntity tpTarget) {
                tpOrigin.teleport(
                        tpTarget.getServer().getWorld(tpTarget.getWorld().getRegistryKey()),
                        tpTarget.getX(),
                        tpTarget.getY(),
                        tpTarget.getZ(),
                        new HashSet<>(),
                        tpTarget.getYaw(),
                        tpTarget.getPitch()
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
