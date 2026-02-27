package com.cimadev.cimpleWaypointSystem.command.persistentData;

import com.cimadev.cimpleWaypointSystem.Colors;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class PlayerHome {
    public static final Codec<PlayerHome> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("position").forGetter(PlayerHome::getPosition),
            Codec.INT.fieldOf("yaw").forGetter(PlayerHome::getYaw),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(PlayerHome::worldRegistryKey),
            UUIDUtil.CODEC.fieldOf("owner").forGetter(PlayerHome::getOwner)
    ).apply(instance, PlayerHome::new));

    private BlockPos position;

    private int yaw;
    private ResourceKey<Level> worldRegKey;

    private UUID owner;

    public BlockPos getPosition() {
        return position;
    }

    public int getYaw() {
        return yaw;
    }

    public ResourceKey<Level> worldRegistryKey() {
        return worldRegKey;
    }

    public UUID getOwner() {
        return owner;
    }

    public Component positionHover(String text) {
        HoverEvent positionTooltip = new HoverEvent.ShowText(
                Component.literal("x: " + position.getX() + ", y: " + position.getY() + ", z: " + position.getZ())
        );
        MutableComponent formatted = Component.literal(text).withStyle(Colors.LINK, ChatFormatting.UNDERLINE);
        Style waypointStyle = formatted.getStyle();
        formatted.setStyle(waypointStyle.withHoverEvent(positionTooltip));
        return formatted;
    }

    public PlayerHome (BlockPos position, int yaw, ResourceKey<Level> world, UUID owner) {
        this.position = position;
        this.yaw = yaw;
        this.worldRegKey = world;
        this.owner = owner;
    }
}
