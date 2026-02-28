package com.cimadev.cimpleWaypointSystem;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelData;

public interface RespawnHelper {
    /// Get the {@link LevelData.RespawnData} for a given player.
    /// If no player-specific respawn is available, defaults to the world spawn.
    static LevelData.RespawnData getRespawnData(ServerPlayer player) {
        var playerRespawn = player.getRespawnConfig();
        if (playerRespawn == null) {
            return player.level().getServer().getRespawnData();
        } else {
            return playerRespawn.respawnData();
        }
    }
}
