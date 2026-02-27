package com.cimadev.cimpleWaypointSystem.command.persistentData;

import com.cimadev.cimpleWaypointSystem.Colors;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;

import static com.cimadev.cimpleWaypointSystem.Main.*;

public enum AccessLevel {
    SECRET("secret", Colors.SECRET),
    PRIVATE("private", Colors.PRIVATE),
    PUBLIC("public", Colors.PUBLIC),
    OPEN("open", Colors.OPEN);

    public static final StreamCodec<RegistryFriendlyByteBuf, AccessLevel> PACKET_CODEC = StreamCodec.ofMember(
            (val, buf) -> buf.writeByte(val.ordinal()),
            buf -> AccessLevel.values()[buf.readByte()]
    );

    private final String name;
    private final Component nameFormatted;

    AccessLevel(String name, ChatFormatting color) {
        this.name = name;
        this.nameFormatted = Component.literal(this.name).withStyle(color);
    }

    public static AccessLevel fromString(String name) throws IllegalArgumentException {
        return switch (name) {
            case "secret" -> AccessLevel.SECRET;
            case "private" -> AccessLevel.PRIVATE;
            case "public" -> AccessLevel.PUBLIC;
            case "open" ->  AccessLevel.OPEN;
            default -> throw new IllegalArgumentException(name + " is not an acceptable access name.");
        };
    }

    public static AccessLevel fromContext(CommandContext<CommandSourceStack> context, String id)
            throws CommandSyntaxException {

        String access = StringArgumentType.getString( context , id );
        AccessLevel result;
        try {
            result = fromString(access);
        } catch (IllegalArgumentException e) {
            throw new SimpleCommandExceptionType(() -> "Invalid access type " + access + ".").create();
        }
        for (AccessLevel globalExclusion : config.disabledAccessLevels.get()) {
            if (result == globalExclusion)
                throw new SimpleCommandExceptionType(() ->
                        "Access type "
                        + access
                        + " has been disabled by the admins."
                ).create();
        }
        return result;
    }

    public String getName() {
        return name;
    }

    public Component getNameFormatted() {
        return nameFormatted;
    }
}
