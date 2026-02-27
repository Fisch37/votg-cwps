package com.cimadev.cimpleWaypointSystem;

import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.PermissionSetSupplier;
import net.minecraft.world.entity.player.Player;

public interface PermissionHelpers {
    static boolean includesLevel(PermissionSet permissions, PermissionLevel level) {
        return permissions.hasPermission(new Permission.HasCommandLevel(level));
    }

    static boolean hasPermissionLevel(PermissionSetSupplier permissionHolder, PermissionLevel level) {
        return includesLevel(permissionHolder.permissions(), level);
    }
    // Why is a Player not a PermissionSetSupplier, even though he implements the interface?
    static boolean hasPermissionLevel(Player permissionHolder, PermissionLevel level) {
        return includesLevel(permissionHolder.permissions(), level);
    }

    static boolean hasAdmin(PermissionSetSupplier permissionHolder) {
        return hasPermissionLevel(permissionHolder, PermissionLevel.ADMINS);
    }

    static boolean hasOwner(PermissionSetSupplier permissionHolder) {
        return hasPermissionLevel(permissionHolder, PermissionLevel.OWNERS);
    }
}
