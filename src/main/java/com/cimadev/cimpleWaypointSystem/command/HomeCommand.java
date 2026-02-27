package com.cimadev.cimpleWaypointSystem.command;

import com.cimadev.cimpleWaypointSystem.Colors;
import com.cimadev.cimpleWaypointSystem.Main;
import com.cimadev.cimpleWaypointSystem.command.persistentData.OfflinePlayer;
import com.cimadev.cimpleWaypointSystem.command.persistentData.PlayerHome;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.text.*;
import java.util.*;
import java.util.function.Supplier;

import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class HomeCommand {
    private static final String COMMAND_NAME = "home";
    private static final String[] DEFAULT_HELP = {
        "/home : Teleports you to your home position, or your current respawn point if none is set.",
        "/home here : Sets the home to your current position, including your horizontal rotation.",
        "/home clear : Resets your home to your current respawn position.",
        "/home where? : Tells you in chat, where your home is, and contains a click action to go there.",
        ""
    };

    private static final String[] ADMIN_HELP = {
        "Administrator features:",
        "/home of [player] : Tells you in chat, where [player]'s home is, and contains a click action to teleport you there.",
        "/home set [position] [world] [player] : Sets the home of [player] to the [position] in [world].",
        ""
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandRegistryAccess, Commands.CommandSelection registrationEnvironment) {
        dispatcher.register(Commands.literal(COMMAND_NAME)
                .executes(HomeCommand::homeGo)
                .then(Commands.literal("here")
                        .executes(HomeCommand::homeHere)
                )
                .then(Commands.literal("clear")
                        .executes(HomeCommand::homeClear)
                )
                .then(Commands.literal("where?")
                        .executes(HomeCommand::homeWhere)
                )
                .then(Commands.literal("help")
                        .executes(HomeCommand::homeHelp)
                )
                .then(Commands.literal("set")
                        .requires(source -> source.hasPermissionLevel(3))   // only admin and owner may set other's homes
                        .then(Commands.argument("position", BlockPosArgument.blockPos())
                                .then(Commands.argument("world", DimensionArgument.dimension())
                                        .then(Commands.argument("player", word())
                                                .executes(HomeCommand::homeSet)
                ))))
                .then(Commands.literal("of")
                        .requires(source-> source.hasPermissionLevel(3))
                        .then(Commands.argument("owner", word())
                                .executes(HomeCommand::homeOf)
                )));
    }

    public static int homeGo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Supplier<Component> messageText;
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerHome playerHome = Main.serverState.getPlayerHome(player.getUUID());
        ServerLevel world;
        BlockPos homePos;
        float yaw = 0;

        // Determine world, homePos, respawnForced
        if ( playerHome == null ) {
            // Set target location to spawn point
            world = player.getServer().getWorld(player.getSpawnPointDimension());
            homePos = player.getSpawnPointPosition();

            if ( homePos == null ) {
                world = player.getServer().getOverworld();
                homePos = world.getSpawnPos();
            }

            MutableComponent spawnpoint = Component.literal("spawnpoint").withStyle(Colors.LINK, ChatFormatting.UNDERLINE);
            Style style = spawnpoint.getStyle();
            HoverEvent spawncoords = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("x: " + homePos.getX() + ", y: " + homePos.getY() + ", z: " + homePos.getZ()));
            spawnpoint.setStyle(style.withHoverEvent(spawncoords));
            messageText = () -> Component.literal("Teleported to your ")
                    .append(spawnpoint)
                    .append(".")
                    .withStyle(Colors.DEFAULT);
        } else {
            // Set target location to home
            homePos = playerHome.getPosition();
            yaw = playerHome.getYaw();
            world = player.getServer().getWorld(playerHome.worldRegistryKey());

            messageText = () -> Component.literal("Teleported to your ")
                    .append(playerHome.positionHover("home"))
                    .append(".")
                    .withStyle(Colors.DEFAULT);
        }

        WpsUtils.teleport(player, world, homePos.getX(), homePos.getY(), homePos.getZ() , yaw, 0);

        context.getSource().sendSuccess(messageText, false);

        return 1;
    }

    private static int homeHere(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Supplier<Component> messageText;

        ServerPlayer player = context.getSource().getPlayerOrException();
        BlockPos blockPos = new BlockPos(player.blockPosition());
        double yaw = player.getYRot();
        ServerLevel world = player.level();
        PlayerHome playerHome = new PlayerHome(blockPos, yaw, world.dimension(), player.getUUID());
        Main.serverState.setPlayerHome( playerHome );
        messageText = () -> Component.literal("Your ")
                .append(playerHome.positionHover("home"))
                .append(" has been set.")
                .withStyle(Colors.DEFAULT);
        context.getSource().sendSuccess(messageText, false);
        Main.serverState.setDirty();
        return 1;
    }

    private static int homeWhere(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Supplier<Component> messageText;
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerHome playerHome = Main.serverState.getPlayerHome(player.getUUID());
        if ( playerHome != null ) {
            BlockPos pHposition = playerHome.getPosition();
            ServerLevel world = player.getServer().getWorld(playerHome.worldRegistryKey());
            String worldName;
            if ( world != null ) {
                worldName = world.dimensionTypeRegistration().unwrapKey().get().identifier().getPath(); // Iamhere
            } else {
                worldName = "[world could not be identified]";
            }
            MutableComponent here = Component.literal(pHposition.getX() + "x " + pHposition.getY() + "y " + pHposition.getZ() + "z").withStyle(Colors.LINK, ChatFormatting.UNDERLINE);
            Style style = here.getStyle();
            HoverEvent goHomeTooltip = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click here to go home!"));
            ClickEvent goHome = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/home");
            here.setStyle(style.withClickEvent(goHome).withHoverEvent(goHomeTooltip));
            messageText = () -> Component.literal("At ")
                    .append(here)
                    .append(" in the " + worldName + "!")
                    .withStyle(Colors.DEFAULT);
        } else {
            MutableComponent respawnPoint = Component.literal("respawn point").withStyle(Colors.LINK, ChatFormatting.UNDERLINE);
            Style style = respawnPoint.getStyle();
            HoverEvent goHomeTooltip = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click here to go home!"));
            ClickEvent goHome = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/home");
            respawnPoint.setStyle(style.withClickEvent(goHome).withHoverEvent(goHomeTooltip));
            messageText = () -> Component.literal("You have not set a home. You will be teleported to your ")
                    .append(respawnPoint)
                    .append(".")
                    .withStyle(Colors.DEFAULT);
        }
        context.getSource().sendSuccess(messageText, false);
        return 1;
    }

    private static int homeSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Supplier<Component> messageText;
        OfflinePlayer player = OfflinePlayer.fromContext(context, "player");
        BlockPos blockPos = BlockPosArgument.getBlockPos(context, "position");
        ServerLevel world = DimensionArgument.getDimension(context, "world");

        PlayerHome playerHome = new PlayerHome(blockPos, 0.0, world.dimension(), player.getUuid());
        Main.serverState.setPlayerHome( playerHome );
        messageText = () -> Component.literal(player.getName()+ "'s ")
                .append(playerHome.positionHover("home"))
                .append(" has been moved.")
                .withStyle(Colors.DEFAULT);
        context.getSource().sendSuccess(messageText, false);
        Main.serverState.setDirty();
        return 1;
    }

    private static int homeClear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Supplier<Component> messageText;

        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerHome playerHome = Main.serverState.getPlayerHome(player.getUUID());
        Main.serverState.removePlayerHome(player.getUUID());

        messageText = () -> Component.literal("Your ")
                .append(playerHome.positionHover("home"))
                .append(" has been removed.")
                .withStyle(Colors.DEFAULT);
        context.getSource().sendSuccess(messageText, false);
        Main.serverState.setDirty();
        return 1;
    }

    private static int homeOf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Supplier<Component> messageText;

        OfflinePlayer player = OfflinePlayer.fromContext(context, "owner");
        PlayerHome ph = Main.serverState.getPlayerHome(player.getUuid());

        if ( ph != null ) {
            BlockPos homePos = ph.getPosition();

            MutableComponent position = Component.literal(homePos.getX() + "x " + homePos.getY() + "y " + homePos.getZ() + "z").withStyle(Colors.LINK, ChatFormatting.UNDERLINE);
            Style style = position.getStyle();
            HoverEvent goHomeTooltip = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click here to visit " + player.getName() + "'s home!"));
            ClickEvent goHome = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + homePos.getX() + " " + homePos.getY() + " " + homePos.getZ());
            position.setStyle(style.withClickEvent(goHome).withHoverEvent(goHomeTooltip));

            messageText = () -> Component.literal(player.getName() + "'s home is at ")
                    .append(position)
                    .append("!")
                    .withStyle(Colors.DEFAULT);
        } else {
            messageText = () -> Component.literal(player.getName() + " has not set a home.");
        }

        context.getSource().sendSuccess(messageText, false);
        return 1;
    }

    private static int homeHelp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Supplier<Component> messageText;
        CommandSourceStack source = context.getSource();
        messageText = () -> Component.literal("-=- -=- -=- /home help menu -=- -=- -=-").withStyle(Colors.SECONDARY);
        source.sendSuccess(messageText, false);

        for( String help : DEFAULT_HELP ) {
            messageText = () -> Component.literal(help).withStyle(Colors.DEFAULT);
            source.sendSuccess(messageText, false);
        }

        if (context.getSource().hasPermissionLevel(3)) {
            for (String help : ADMIN_HELP ) {
                messageText = () -> Component.literal(help).withStyle(Colors.DEFAULT);
                source.sendSuccess(messageText, false);
            }
        }

        messageText = () -> Component.literal("/home help : Shows this menu.").withStyle(Colors.DEFAULT);
        source.sendSuccess(messageText, false);

        messageText = () -> Component.literal("-=- -=- -=- -=- -=- -=- -=- -=- -=- -=-").withStyle(Colors.SECONDARY);
        source.sendSuccess(messageText, false);

        return 1;
    }
}

