package com.cimadev.cimpleWaypointSystem.command.persistentData;

import com.cimadev.cimpleWaypointSystem.Colors;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public class PlayerHome {
    private BlockPos position;

    private int yaw;
    private ResourceKey worldRegKey;

    private UUID owner;

    public BlockPos getPosition() {
        return position;
    }

    public int getYaw() {
        return yaw;
    }

    public ResourceKey worldRegistryKey() {
        return worldRegKey;
    }

    public UUID getOwner() {
        return owner;
    }

    public Component positionHover(String text) {
        HoverEvent positionTooltip = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("x: " + position.getX() + ", y: " + position.getY() + ", z: " + position.getZ()));
        MutableComponent formatted = Component.literal(text).withStyle(Colors.LINK, ChatFormatting.UNDERLINE);
        Style waypointStyle = formatted.getStyle();
        formatted.setStyle(waypointStyle.withHoverEvent(positionTooltip));
        return formatted;
    }

    public PlayerHome (BlockPos position, Double yaw, ResourceKey world, UUID owner) {
        this.position = position;
        this.yaw = yaw.intValue();
        this.worldRegKey = world;
        this.owner = owner;
    }

    private PlayerHome ( CompoundTag nbt ) {
        int position[] = nbt.getIntArray("position");
        this.position = new BlockPos( position[0], position[1], position[2] );
        this.yaw = nbt.getInt("yaw");
        Identifier regKeyVal = Identifier.parse(nbt.getString( "worldRegKeyValue" ));
        Identifier regKeyReg = Identifier.parse(nbt.getString( "worldRegKeyRegistry" ));
        this.worldRegKey = ResourceKey.create( ResourceKey.createRegistryKey(regKeyReg), regKeyVal );
        this.owner = nbt.getUuid("owner");
    }

    public static PlayerHome fromNbt(CompoundTag nbt) {
        return new PlayerHome( nbt );
    }

    public CompoundTag toNbt( ) {
        CompoundTag playerStateNbt = new CompoundTag();

        playerStateNbt.putUuid("owner", owner);
        playerStateNbt.putIntArray("position", new int[] {position.getX(), position.getY(), position.getZ()});
        playerStateNbt.putInt("yaw", yaw);
        playerStateNbt.putString("worldRegKeyRegistry", worldRegKey.registry().toString() );
        playerStateNbt.putString("worldRegKeyValue", worldRegKey.identifier().toString() );

        return playerStateNbt;
    }
}
