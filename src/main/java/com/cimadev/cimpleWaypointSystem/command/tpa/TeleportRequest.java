package com.cimadev.cimpleWaypointSystem.command.tpa;

import com.cimadev.cimpleWaypointSystem.command.WpsUtils;
import de.fisch37.datastructures.mi.MINode;
import java.util.HashSet;
import net.minecraft.world.entity.player.Player;

public class TeleportRequest extends MINode {
        private final Player origin, target;
        private final boolean inverted;
        private long expiresAt;

        public TeleportRequest(Player origin, Player target, boolean inverted) {
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

        private void perform(Player tpOrigin, Player tpTarget) {
                WpsUtils.teleport(
                        tpOrigin,
                        tpTarget.getServer().getWorld(tpTarget.getWorld().getRegistryKey()),
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
