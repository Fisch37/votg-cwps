package com.cimadev.cimpleWaypointSystem.command.persistentData;

import com.cimadev.cimpleWaypointSystem.Colors;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class PlayerHome {
    private BlockPos position;

    private int yaw;
    private RegistryKey worldRegKey;

    private UUID owner;

    public BlockPos getPosition() {
        return position;
    }

    public int getYaw() {
        return yaw;
    }

    public RegistryKey worldRegistryKey() {
        return worldRegKey;
    }

    public UUID getOwner() {
        return owner;
    }

    public Text positionHover(String text) {
        HoverEvent positionTooltip = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("x: " + position.getX() + ", y: " + position.getY() + ", z: " + position.getZ()));
        MutableText formatted = Text.literal(text).formatted(Colors.LINK, Formatting.UNDERLINE);
        Style waypointStyle = formatted.getStyle();
        formatted.setStyle(waypointStyle.withHoverEvent(positionTooltip));
        return formatted;
    }

    public PlayerHome (BlockPos position, Double yaw, RegistryKey world, UUID owner) {
        this.position = position;
        this.yaw = yaw.intValue();
        this.worldRegKey = world;
        this.owner = owner;
    }

    private PlayerHome ( NbtCompound nbt ) {
        int position[] = nbt.getIntArray("position");
        this.position = new BlockPos( position[0], position[1], position[2] );
        this.yaw = nbt.getInt("yaw");
        Identifier regKeyVal = Identifier.of(nbt.getString( "worldRegKeyValue" ));
        Identifier regKeyReg = Identifier.of(nbt.getString( "worldRegKeyRegistry" ));
        this.worldRegKey = RegistryKey.of( RegistryKey.ofRegistry(regKeyReg), regKeyVal );
        this.owner = nbt.getUuid("owner");
    }

    public static PlayerHome fromNbt(NbtCompound nbt) {
        return new PlayerHome( nbt );
    }

    public NbtCompound toNbt( ) {
        NbtCompound playerStateNbt = new NbtCompound();

        playerStateNbt.putUuid("owner", owner);
        playerStateNbt.putIntArray("position", new int[] {position.getX(), position.getY(), position.getZ()});
        playerStateNbt.putInt("yaw", yaw);
        playerStateNbt.putString("worldRegKeyRegistry", worldRegKey.getRegistry().toString() );
        playerStateNbt.putString("worldRegKeyValue", worldRegKey.getValue().toString() );

        return playerStateNbt;
    }
}
