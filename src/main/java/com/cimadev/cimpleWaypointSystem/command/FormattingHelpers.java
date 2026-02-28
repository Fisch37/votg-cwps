package com.cimadev.cimpleWaypointSystem.command;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public interface FormattingHelpers {
    static MutableComponent getPositionFormatted(BlockPos position) {
        return Component.literal("x: " + position.getX() + ", y: " + position.getY() + ", z: " + position.getZ());
    }
}
