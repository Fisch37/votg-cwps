package com.cimadev.cimpleWaypointSystem.command;

import com.cimadev.cimpleWaypointSystem.Colors;
import com.cimadev.cimpleWaypointSystem.Main;
import com.cimadev.cimpleWaypointSystem.command.persistentData.*;
import com.cimadev.cimpleWaypointSystem.command.suggestions.AccessSuggestionProvider;
import com.cimadev.cimpleWaypointSystem.command.suggestions.OfflinePlayerSuggestionProvider;
import com.cimadev.cimpleWaypointSystem.command.suggestions.WaypointSuggestionProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

import static com.cimadev.cimpleWaypointSystem.Main.*;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.mojang.brigadier.arguments.StringArgumentType.word;


public class WpsCommand {

    private static final String COMMAND_NAME = "wps";

    private static final String[] DEFAULT_HELP = {
            "/wps go [name] : Teleports you to your waypoint called [waypoint name].",
            "/wps go [name] [owner] : Teleports you [owner]'s waypoint [waypoint name] if allowed.",
            "/wps go [name] open : Teleports you to the open waypoint [waypoint name].",
            "/wps [Arguments] : Shorthand for /wps go [Arguments].",
            "",
            "/wps here [name] <access>: Creates new waypoint [name] (with access level <access>) at your position.",
            "",
            "/wps list : Lists all waypoints accessible to you.",
            "/wps list mine : Lists your waypoints.",
            "/wps list [owner] : Lists [owner]'s waypoints accessible to you.",
            "/wps list open : Lists all open waypoints.",
            "",
            "/wps remove [name] : Removes your waypoint [name].",
            "/wps rename [name] [newName] : Renames your waypoint [name] to [newName].",
            "/wps access [name] [access] : Changes the access level of your waypoint [name] to [access].",
            "",
            "/wps friend add [friend] : Lets [friend] see your private waypoints.",
            "/wps friend remove [friend] : Prevents [friend] from seeing your private waypoints.",
            "",
            "/wps sethome [name] : Sets your /home at the position of the waypoint [name].",
            ""
    };

    private static final String[] ADMIN_HELP = {
            "Administrator features:",
            "/wps here [name] open : Creates open waypoint [name] at your position.",
            "/wps list [owner] all : Lists all of [owner]'s waypoints.",
            "/wps listAll : Lists all waypoints",
            "",
            "/wps remove [name] [owner] : Removes [owner]'s waypoint [name].",
            "/wps remove [name] open : Removes open waypoint [name].",
            "/wps rename [name] [newName] [owner] : Renames [owner]'s waypoint [name].",
            "/wps rename [name] [newName] open : Renames open waypoint [name].",
            ""
    };

    // Yeah, yeah, functional interfaces in constants
    private static final WaypointSuggestionProvider.WaypointValidator ONLY_SELF_PREDICATE = (source, waypoint) ->
            !source.isExecutedByPlayer()
                    || Objects.requireNonNull(source.getPlayer())
                    .getUuid()
                    .equals(waypoint.getOwner());
    private static final AccessSuggestionProvider accessSuggestionsAdminsOpen = new AccessSuggestionProvider(source -> source.hasPermissionLevel(3), AccessLevel.OPEN);
    private static final AccessSuggestionProvider accessSuggestionsNoOpen = new AccessSuggestionProvider(AccessLevel.OPEN);
    private static final WaypointSuggestionProvider waypointSuggestionsOnlySelf = new WaypointSuggestionProvider(
            false, true,
            ONLY_SELF_PREDICATE
    );
    private static final WaypointSuggestionProvider waypointsFilteredWithOwner =
            new WaypointSuggestionProvider(true, true);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        LiteralArgumentBuilder<ServerCommandSource> command = CommandManager.literal(COMMAND_NAME)
                .then(CommandManager.argument("name", string())
                        .executes(WpsCommand::wpsGoDerived)
                        .then(CommandManager.argument("owner", word())
                                .suggests(new OfflinePlayerSuggestionProvider())
                                .executes(WpsCommand::wpsGoOwned)
                        )
                        .then(CommandManager.literal("open")
                                .executes(WpsCommand::wpsGoOpen)
                ))
                .then(CommandManager.literal("help")
                        .executes(WpsCommand::wpsHelp)
                )
                .then(CommandManager.literal("go")
                        .then(CommandManager.argument("name", string())
                                .suggests(waypointsFilteredWithOwner)
                                .executes(WpsCommand::wpsGoDerived)
                                .then(CommandManager.argument("owner", word())
                                        .suggests(new OfflinePlayerSuggestionProvider())
                                        .executes(WpsCommand::wpsGoOwned)
                                )
                                .then(CommandManager.literal("open")
                                        .executes(WpsCommand::wpsGoOpen)
                )))
                .then(CommandManager.literal("here")
                        .then(CommandManager.argument("name", string())
                                .executes(WpsCommand::wpsHereMine)
                                .then(CommandManager.argument("access", word())
                                        .suggests(accessSuggestionsAdminsOpen)
                                        .executes(WpsCommand::wpsHereMine)
                )))
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("name", string())
                                .executes(WpsCommand::wpsAddMine)
                                .then(CommandManager.argument("access", word())
                                        .suggests(accessSuggestionsAdminsOpen)
                                        .executes(WpsCommand::wpsAddMine)
                )))
                .then(CommandManager.literal("set")
                        .requires(source -> source.hasPermissionLevel(3))
                        .then(CommandManager.argument("name", string())
                                .then(CommandManager.argument("owner", word())
                                        .suggests(new OfflinePlayerSuggestionProvider())
                                        .then(CommandManager.argument("access", word())
                                                .suggests(accessSuggestionsNoOpen)
                                                .executes(WpsCommand::wpsSet)
                                                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                                        .executes(WpsCommand::wpsSet)
                                                        .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                                                                .executes(WpsCommand::wpsSet)
                                                                .then(CommandManager.argument(
                                                                            "yaw",
                                                                            DoubleArgumentType.doubleArg(-90, 90)
                                                                    )
                                                                        .executes(WpsCommand::wpsSet)
                                )))))
                                .then(CommandManager.literal("open")
                                        .executes(WpsCommand::wpsSetOpen)
                                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                                .executes(WpsCommand::wpsSetOpen)
                                                .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                                                        .executes(WpsCommand::wpsSet)
                                                        .then(CommandManager.argument("yaw", DoubleArgumentType.doubleArg(-90, 90))
                                                                .executes(WpsCommand::wpsSetOpen)
                ))))))
                .then(CommandManager.literal("list")
                        .executes(WpsCommand::wpsListAccessible)
                        .then(CommandManager.argument("owner", word())
                                .suggests(new OfflinePlayerSuggestionProvider())
                                .executes(WpsCommand::wpsListOwnedAccessible)
                                .then(CommandManager.literal("all")
                                        .requires(source -> source.hasPermissionLevel(3))
                                        .executes(WpsCommand::wpsListOwnedAll)
                        ))
                        .then(CommandManager.literal("mine")
                                .executes(WpsCommand::wpsListMine)
                        )
                        .then(CommandManager.literal("open")
                                .executes(WpsCommand::wpsListOpen)
                ))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("name", string())
                                .suggests(waypointSuggestionsOnlySelf)
                                .executes(WpsCommand::wpsRemoveMine)
                                .then(CommandManager.argument("owner", word())
                                        .requires(source -> source.hasPermissionLevel(3))
                                        .executes(WpsCommand::wpsRemoveOwned)
                                )
                                .then(CommandManager.literal("open")
                                        .requires(source -> source.hasPermissionLevel(3))
                                        .executes(WpsCommand::wpsRemoveOpen)
                )))
                .then(CommandManager.literal("rename")
                        .then(CommandManager.argument("oldName", string())
                                .suggests(new WaypointSuggestionProvider(
                                        false,
                                        true,
                                        ONLY_SELF_PREDICATE,
                                        "oldName"
                                ))
                                .then(CommandManager.argument("newName", string())
                                        .executes(WpsCommand::wpsRenameMine)
                                        .then(CommandManager.argument("owner", word())
                                                .requires(source -> source.hasPermissionLevel(3))
                                                .executes(WpsCommand::wpsRenameOwned))
                                        .then(CommandManager.literal("open")
                                                .requires(source -> source.hasPermissionLevel(3))
                                                .executes(WpsCommand::wpsRenameOpen)
                ))))
                .then(CommandManager.literal("access")
                        .then(CommandManager.argument("name", string())
                                .then(CommandManager.argument("access", word())
                                        .suggests(accessSuggestionsNoOpen)
                                        .executes(WpsCommand::wpsSetAccess)
                )))
                .then(CommandManager.literal("sethome")
                        .executes(WpsCommand::wpsSetHome)
                        .then(CommandManager.argument("name", string())
                                .suggests(waypointsFilteredWithOwner)
                                .then(CommandManager.argument("owner", word())
                                        .suggests(new OfflinePlayerSuggestionProvider())
                                        .executes(WpsCommand::wpsSetHome))
                                .then(CommandManager.literal("open")
                                        .executes(WpsCommand::wpsSetHome)
                )));

        dispatcher.register(command);

        // administrator wps options
        dispatcher.register(CommandManager.literal(COMMAND_NAME)
                .then(CommandManager.literal("listAll")
                        .requires(source -> source.hasPermissionLevel(4)) // only meant for printing to console
                        .executes(WpsCommand::wpsListAll)));
    }

    private static int wpsHelp(CommandContext<ServerCommandSource> context) {
        Supplier<Text> messageText;
        ServerCommandSource source = context.getSource();
        messageText = () -> Text.literal("-=- -=- -=- /wps help menu -=- -=- -=-").formatted(Colors.SECONDARY);
        source.sendFeedback(messageText, false);

        for( String help : DEFAULT_HELP ) {
            messageText = () -> Text.literal(help).formatted(Colors.DEFAULT);
            source.sendFeedback(messageText, false);
        }

        if (context.getSource().hasPermissionLevel(3)) {
            for (String help : ADMIN_HELP ) {
                messageText = () -> Text.literal(help).formatted(Colors.DEFAULT);
                source.sendFeedback(messageText, false);
            }
        }

        messageText = () -> Text.literal("/wps help : Shows this menu.").formatted(Colors.DEFAULT);
        source.sendFeedback(messageText, false);

        messageText = () -> Text.literal("-=- -=- -=- -=- -=- -=- -=- -=- -=- -=-").formatted(Colors.SECONDARY);
        source.sendFeedback(messageText, false);

        return 1;
    }

    private static int wpsGoOpen(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return executeWpsGo(context, null);
    }

    private static int wpsGoDerived(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String waypointName = StringArgumentType.getString(context, "name");
        boolean preferOpen = config.preferOpenForDerived.get();
        final CommandFunction forOpen = () -> executeWpsGo(context, null);
        final CommandFunction forSelf = () -> executeWpsGo(
                context,
                OfflinePlayer.fromUuid(context.getSource().getPlayerOrThrow().getUuid())
        );
        UUID owner = preferOpen ? null : context.getSource().getPlayerOrThrow().getUuid();
        if (Main.serverState.waypointExists(new WaypointKey(owner, waypointName)))
            return preferOpen ? forOpen.run() : forSelf.run();
        else
            return preferOpen ? forSelf.run() : forOpen.run();
    }
    private static int wpsGoOwned(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return executeWpsGo(context, OfflinePlayer.fromContext(context, "owner"));
    }

    private static int executeWpsGo(
            CommandContext<ServerCommandSource> context,
            @Nullable OfflinePlayer owner
    ) throws CommandSyntaxException {
        Supplier<Text> messageText;
        ServerCommandSource commandSource = context.getSource();
        ServerPlayerEntity player = commandSource.getPlayerOrThrow();
        MinecraftServer server = commandSource.getServer();
        String name = StringArgumentType.getString(context, "name");

        UUID ownerUuid = owner == null ? null : owner.getUuid();
        Waypoint waypoint = Main.serverState.getWaypoint(new WaypointKey(ownerUuid, name));
        String ownerName;
        if (owner != null) {
            if (ownerUuid.equals(player.getUuid())) ownerName = "your ";
            else ownerName = owner.getName() + "'s ";
        } else {
            ownerName = "";
        }

        if (waypoint != null && Main.serverState.waypointAccess(waypoint, player)) {
            Vec3d wpPos = waypoint.getPosition().toBottomCenterPos();
            ServerWorld world = server.getWorld(waypoint.getWorldRegKey());
            if ( world == null ) return -1;
            int yaw = waypoint.getYaw();
            player.teleport(world, wpPos.getX(), wpPos.getY(), wpPos.getZ(), yaw, 0);

            messageText = () -> Text.literal("Teleported to ")
                    .append(Text.literal(ownerName).formatted(Colors.PLAYER))
                    .append(waypoint.getAccessFormatted())
                    .append(Text.literal(" waypoint "))
                    .append(waypoint.getNameFormatted())
                    .append(Text.literal("."))
                    .formatted(Colors.DEFAULT);
        } else {
            messageText = () -> Text.literal(ownerName).formatted(Colors.PLAYER)
                    .append(Text.literal(" waypoint "))
                    .append(Text.literal( name ).formatted(Colors.LINK_INACTIVE))
                    .append(Text.literal(" could not be found."))
                    .formatted(Colors.DEFAULT);
        }

        commandSource.sendFeedback(messageText, false);
        return 1;
    }

    // wpsAddMine and wpsAddOpen for compatibility reasons
    private static int wpsAddMine(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return wpsHere(context, false);
    }

    private static int wpsHereMine(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return wpsHere(context, true);
    }

    private static int wpsHere(CommandContext<ServerCommandSource> context, boolean moveIfExists) throws CommandSyntaxException {
        /* todo:
         * redesign the whole wpsAdd and wpsHere thing to a) be consistent, b) not pingpong c) not change the access on move if not specified (currently changes to private)
         */
        Supplier<Text> messageText;

        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        BlockPos blockPos = new BlockPos(player.getBlockPos());
        double yaw = player.getYaw();
        ServerWorld world = player.getServerWorld();

        String name = StringArgumentType.getString(context, "name");
        AccessLevel access = config.defaultAccess.get();
        if ( context.getNodes().size() == 4 ) {
            access = AccessLevel.fromContext(context, "access");
        }
        if (access == AccessLevel.OPEN && !context.getSource().hasPermissionLevel(3)) {
            AccessLevel finalAccess = access;
            throw new SimpleCommandExceptionType(() -> "Invalid access type " + finalAccess.getName() + ".").create();
        }

        UUID owner = (access == AccessLevel.OPEN ? null : player.getUuid());
        Waypoint newWaypoint = new Waypoint(name, blockPos, yaw, world.getRegistryKey(), owner, access);
        Waypoint oldWaypoint = Main.serverState.getWaypoint(newWaypoint.getKey());
        if ( oldWaypoint == null ) {
            messageText = () -> wpsAdd(newWaypoint);
        } else if ( moveIfExists ) {
            messageText = () -> wpsMove(oldWaypoint, newWaypoint);
        } else {
            messageText = () -> Text.literal("Your ")
                    .append(oldWaypoint.getAccessFormatted())
                    .append( " waypoint " )
                    .append(oldWaypoint.getNameFormatted())
                    .append(" already exists!").formatted(Colors.DEFAULT);
        }

        context.getSource().sendFeedback(messageText, false);
        Main.serverState.markDirty();
        return 1;
    }

    private static MutableText wpsAdd(Waypoint newWaypoint) {
        Main.serverState.setWaypoint( newWaypoint );
        return Text.literal("Set ")
                .append(newWaypoint.getAccessFormatted())
                .append(" waypoint ")
                .append(newWaypoint.getNameFormatted())
                .append(".")
                .formatted(Colors.DEFAULT);
    }

    private static MutableText wpsMove(Waypoint oldWaypoint, Waypoint newWaypoint) {
        BlockPos nwp = newWaypoint.getPosition();
        AccessLevel access = newWaypoint.getAccess();
        BlockPos owp = oldWaypoint.getPosition();
        oldWaypoint.setPosition(nwp);
        oldWaypoint.setYaw(newWaypoint.getYaw());
        oldWaypoint.setAccess(access);

        HoverEvent movedTooltip = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(" Formerly at x: "  + owp.getX() + ", y: " + owp.getY() + ", z: " + owp.getZ()));
        MutableText moved = Text.literal("Moved").formatted(Formatting.UNDERLINE);
        Style waypointStyle = moved.getStyle();
        moved.setStyle(waypointStyle.withHoverEvent(movedTooltip));
        Text oldAccess = oldWaypoint.getAccessFormatted();

        MutableText message = Text.literal("")
                .append(moved);
        if (access == AccessLevel.OPEN) message.append(" the ").append(access.getNameFormatted());
        else message.append(" your ").append(oldAccess);
        message.append(" waypoint ")
                .append(newWaypoint.getNameFormatted())
                .append(".")
                .formatted(Colors.DEFAULT);
        if ( oldWaypoint.getAccess() != access ) {
            message.append(" It is now ")
                    .append(newWaypoint.getAccessFormatted())
                    .append(".")
                    .formatted(Colors.DEFAULT);
        }
        return message;
    }

    private static int wpsSetOpen(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return wpsSet(context, true);
    }

    private static int wpsSet(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return wpsSet(context, false);
    }
    private static int wpsSet(
            CommandContext<ServerCommandSource> context,
            boolean isOpen
    ) throws CommandSyntaxException {
        AccessLevel accessLevel = AccessLevel.OPEN;
        if (!isOpen) {
            accessLevel = AccessLevel.fromContext(context, "access");
        }
        if (!isOpen && accessLevel == AccessLevel.OPEN) {
            throw new SimpleCommandExceptionType(() -> "Cannot use access " + AccessLevel.OPEN.getNameFormatted() + " with owned waypoints." +
                    " For free access to an owned waypoint, use " + AccessLevel.PUBLIC.getNameFormatted() + ".").create();
        }

        ServerCommandSource source = context.getSource();
        String name = StringArgumentType.getString(context, "name");
        BlockPos pos = BlockPos.ofFloored(source.getPosition());
        double yaw = source.getRotation().x;
        RegistryKey<World> world = source.getWorld().getRegistryKey();

        UUID owner = null;
        // I swear this was the best option available
        if (!isOpen) {
            try {
                owner = OfflinePlayer.fromContext(context, "owner").getUuid();
            } catch (CommandSyntaxException e) {
                owner = UuidArgumentType.getUuid(context, "owner");
            }
        }
        try {
            pos = BlockPosArgumentType.getBlockPos(context, "pos");
        } catch (IllegalArgumentException e) { }
        try {
            yaw = DoubleArgumentType.getDouble(context, "yaw");
        } catch (IllegalArgumentException e) { }
        try {
            world = DimensionArgumentType
                    .getDimensionArgument(context, "dimension")
                    .getRegistryKey();
        } catch (IllegalArgumentException e) { }

        Waypoint waypoint = new Waypoint(name, pos, yaw, world, owner, accessLevel);
        serverState.setWaypoint(waypoint);

        source.sendFeedback(
                () -> Text.literal("Created new waypoint ")
                        .append(
                                Text.literal(name)
                                // TODO: Figure out if we can embed /wps go
                                .formatted(Colors.LINK_INACTIVE)
                        )
                        .append("!") // Most important append of all time
                        .formatted(Colors.DEFAULT)
                ,
                true
        );

        return 1;
    }

    private static int wpsListAccessible(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if ( player == null ) return wpsListAll(context);
        else {
            List<Waypoint> waypoints = WpsUtils.getAccessibleWaypoints(player, null, false, false);
            printWaypointsToUser(context, waypoints);
        }
        return 1;
    }

    private static int wpsListOwnedAccessible(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if ( player == null ) return wpsListOwnedAll(context);
        else {
            OfflinePlayer owner = OfflinePlayer.fromContext(context, "owner");
            /*todo: if( owner == null ) error "not a valid player", return 1*/
            List<Waypoint> waypoints = WpsUtils.getAccessibleWaypoints(player, owner, false, false);
            printWaypointsToUser(context, waypoints);
        }
        return 1;
    }

    private static int wpsListAll(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        List<Waypoint> waypoints = WpsUtils.getAllWaypoints();
        printWaypointsToUser(context, waypoints);
        return 1;
    }
    private static int wpsListOwnedAll(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {

        ServerPlayerEntity player = context.getSource().getPlayer();
        OfflinePlayer owner = OfflinePlayer.fromContext(context, "owner");
        /*todo: if( owner == null ) error "not a valid player", return 1*/
        List<Waypoint> waypoints = WpsUtils.getAccessibleWaypoints(player, owner, true, false);
        printWaypointsToUser(context, waypoints);
        return 1;

    }
    private static int wpsListMine(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {

        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        OfflinePlayer alsoPlayer = Main.serverState.getPlayerByUuid(player.getUuid());
        List<Waypoint> waypoints = WpsUtils.getAccessibleWaypoints(player, alsoPlayer, false, false);
        printWaypointsToUser(context, waypoints);
        return 1;

    }
    private static int wpsListOpen(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {

        ServerPlayerEntity player = context.getSource().getPlayer();
        List<Waypoint> waypoints = WpsUtils.getAccessibleWaypoints(player, null, false, true);
        printWaypointsToUser(context, waypoints);
        return 1;

    }

    private static void printWaypointsToUser(CommandContext<ServerCommandSource> context, List<Waypoint> waypoints) {
        Supplier<Text> messageText;
        ServerPlayerEntity player = context.getSource().getPlayer();
        UUID playerUuid = ( player == null ) ? null : player.getUuid();

        if ( waypoints.isEmpty() ) {
            messageText = () -> Text.literal("No waypoints found.").formatted(Colors.DEFAULT);
            context.getSource().sendFeedback(messageText, false);
            return;
        }

        messageText = () -> Text.literal("-=- -=- -=- /" + context.getInput() + " -=- -=- -=-").formatted(Colors.SECONDARY);
        context.getSource().sendFeedback(messageText, false);
        for (Waypoint waypoint : waypoints) {
            UUID ownerUuid = waypoint.getOwner();
            MutableText ownerTitle;
            if (ownerUuid == null) {
                if (waypoint.getAccess() == AccessLevel.SECRET)
                    ownerTitle = Text.literal("Unowned secret").formatted(Colors.SECRET);
                else ownerTitle = Text.literal("Open").formatted(Colors.PUBLIC);
            } else {
                OfflinePlayer owner = Main.serverState.getPlayerByUuid(ownerUuid);
                if (ownerUuid.equals(playerUuid)) {
                    ownerTitle = Text.literal("Your ").formatted(Colors.PLAYER);
                } else if (owner == null) {
                    ownerTitle = Text.literal("[Error finding name]'s ").formatted(Colors.SECONDARY);
                } else {
                    ownerTitle = Text.literal(owner.getName() + "'s ").formatted(Colors.PLAYER);
                }
            }
            messageText = () -> Text.literal("")
                    .append(ownerTitle)
                    .append((ownerUuid == null) ? Text.literal("") : waypoint.getAccessFormatted()) // dirty because lazy
                    .append(" waypoint ")
                    .append(waypoint.getNameFormatted())
                    .append(".")
                    .formatted(Colors.DEFAULT);

            context.getSource().sendFeedback(messageText, false);
        }
        messageText = () -> Text.literal("-=- -=- -=- -=- -=- -=- -=- -=- -=- -=-").formatted(Colors.SECONDARY);
        context.getSource().sendFeedback(messageText, false);
    }

    private static int wpsRemoveMine(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String name = StringArgumentType.getString(context, "name");
        WaypointKey wpKey = new WaypointKey(player.getUuid(), name);
        Waypoint waypoint = Main.serverState.getWaypoint(wpKey);
        return wpsRemove(context, waypoint, name, true);
    }

    private static int wpsRemoveOpen(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        WaypointKey wpKey = new WaypointKey(null, name);
        Waypoint waypoint = Main.serverState.getWaypoint(wpKey);
        return wpsRemove(context, waypoint, name, false);
    }

    private static int wpsRemoveOwned(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        OfflinePlayer owner = OfflinePlayer.fromContext(context, "owner");
        WaypointKey wpKey = new WaypointKey(owner.getUuid(), name);
        Waypoint waypoint = Main.serverState.getWaypoint(wpKey);
        return wpsRemove(context, waypoint, name, false);
    }

    private static int wpsRemove(CommandContext<ServerCommandSource> context, @Nullable Waypoint waypoint, String inputName, boolean ownedByCaller) {
        Supplier<Text> messageText;

        MutableText ownerTitle;
        UUID ownerUuid = (waypoint == null) ? null : waypoint.getOwner();
        if ( ownedByCaller ) ownerTitle = Text.literal("Your ");
        else {
            if ( ownerUuid == null ) {
                ownerTitle = Text.literal("The ").append(Text.literal("open ").formatted(Colors.PUBLIC));
            } else {
                ownerTitle = Text.literal(Main.serverState.getPlayerByUuid(ownerUuid).getName() + "'s ");
            }
        }

        if (waypoint == null) {
            messageText = () -> ownerTitle.append("waypoint ")
                    .append(Text.literal( inputName ).formatted(Colors.LINK_INACTIVE))
                    .append(Text.literal(" could not be found."))
                    .formatted(Colors.DEFAULT);
        } else {
            Text waypointNameFormatted = waypoint.getNameFormatted();
            Main.serverState.removeWaypoint(waypoint.getKey());
            Main.serverState.markDirty();
            MutableText message = Text.literal("")
                    .append(ownerTitle);
            if ( ownerUuid != null ) message.append(waypoint.getAccessFormatted());
            message.append(" waypoint ")
                    .append(waypointNameFormatted)
                    .append(Text.literal(" has been removed."))
                    .formatted(Colors.DEFAULT);
            messageText = () -> message;
        }
        context.getSource().sendFeedback(messageText, false);
        return 1;
    }

    private static int wpsSetAccess(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Supplier<Text> messageText;

        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String name = StringArgumentType.getString(context, "name");
        WaypointKey wpKey = new WaypointKey(player.getUuid(), name);
        Waypoint waypoint = Main.serverState.getWaypoint(wpKey);
        if (waypoint == null) {
            messageText = () -> Text.literal("Your waypoint ")
                    .append(Text.literal( name ).formatted(Colors.LINK_INACTIVE))
                    .append(Text.literal(" could not be found."))
                    .formatted(Colors.DEFAULT);
        } else {
            AccessLevel access = AccessLevel.fromContext(context, "access");
            Text oldAccess = waypoint.getAccessFormatted();
            waypoint.setAccess(access);
            messageText = () -> Text.literal("Your ")
                    .append(oldAccess)
                    .append(" waypoint ")
                    .append(waypoint.getNameFormatted())
                    .append(Text.literal(" is now "))
                    .append(waypoint.getAccessFormatted())
                    .append(".")
                    .formatted(Colors.DEFAULT);
            Main.serverState.markDirty();
        }

        context.getSource().sendFeedback(messageText, false);
        return 1;
    }

    private static int wpsRenameMine(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String oldName = StringArgumentType.getString(context, "oldName");
        String newName = StringArgumentType.getString(context, "newName");
        WaypointKey wpKey = new WaypointKey(player.getUuid(), oldName);
        Waypoint waypoint = Main.serverState.getWaypoint(wpKey);
        return wpsRename(context, waypoint, oldName, newName, true);
    }

    private static int wpsRenameOpen(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String oldName = StringArgumentType.getString(context, "oldName");
        String newName = StringArgumentType.getString(context, "newName");
        WaypointKey wpKey = new WaypointKey(null, oldName);
        Waypoint waypoint = Main.serverState.getWaypoint(wpKey);
        return wpsRename(context, waypoint, oldName, newName, false);
    }

    private static int wpsRenameOwned(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String oldName = StringArgumentType.getString(context, "oldName");
        String newName = StringArgumentType.getString(context, "newName");
        OfflinePlayer owner = OfflinePlayer.fromContext(context, "owner");
        WaypointKey wpKey = new WaypointKey(owner.getUuid(), oldName);
        Waypoint waypoint = Main.serverState.getWaypoint(wpKey);
        return wpsRename(context, waypoint, oldName, newName, false);
    }

    private static int wpsRename(CommandContext<ServerCommandSource> context, Waypoint waypoint, String oldName, String newName, boolean ownedByCaller) throws CommandSyntaxException {
        Supplier<Text> messageText;

        MutableText ownerTitle;
        UUID ownerUuid = (waypoint == null) ? null : waypoint.getOwner();
        if ( ownedByCaller ) ownerTitle = Text.literal("Your ");
        else {
            if ( ownerUuid == null ) {
                ownerTitle = Text.literal("The ").append(Text.literal("open ").formatted(Colors.PUBLIC));
            } else {
                ownerTitle = Text.literal(Main.serverState.getPlayerByUuid(ownerUuid).getName() + "'s ");
            }
        }

        if (waypoint == null) {
            String finalOldName = oldName;
            messageText = () -> ownerTitle.append("waypoint ")
                    .append(Text.literal(finalOldName).formatted(Colors.LINK_INACTIVE))
                    .append(Text.literal(" could not be found."))
                    .formatted(Colors.DEFAULT);
        } else {
            Main.serverState.removeWaypoint(waypoint.getKey());
            oldName = waypoint.getName();
            waypoint.rename(newName);
            Main.serverState.setWaypoint( waypoint );
            String finalOldName = oldName;
            MutableText message = Text.literal("Your waypoint ");
            if ( ownerUuid != null ) message.append(Text.literal(finalOldName).formatted(Colors.LINK_INACTIVE));
            message.append(Text.literal(" is now called "))
                    .append(waypoint.getNameFormatted())
                    .append(".")
                    .formatted(Colors.DEFAULT);
            messageText = () -> message;
            Main.serverState.markDirty();
        }

        context.getSource().sendFeedback(messageText, false);
        return 1;
    }

    private static int wpsSetHome(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Supplier<Text> messageText;

        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        UUID playerUuid = player.getUuid();
        String name = StringArgumentType.getString(context, "name");
        OfflinePlayer owner;
        MutableText ownerName;
        UUID ownerUuid;
        if (context.getNodes().size() == 4) {
            try {
                owner = OfflinePlayer.fromContext(context, "owner");
                ownerName = Text.literal(owner.getName() + "'s ");
                ownerUuid = owner.getUuid();
            } catch (Exception e) {
                ownerName = Text.literal("open ").formatted(Colors.PUBLIC);
                ownerUuid = null;
            }
        } else {
            owner = Main.serverState.getPlayerByUuid(playerUuid);
            ownerName = Text.literal("Your ");
            ownerUuid = owner.getUuid();
        }

        MutableText finalOwnerName = ownerName;
        messageText = () -> Text.literal("").append(finalOwnerName).append("waypoint ")
                .append(Text.literal( name ).formatted(Colors.LINK_INACTIVE))
                .append(Text.literal(" could not be found."))
                .formatted(Colors.DEFAULT);

        WaypointKey wpKey = new WaypointKey(ownerUuid, name);
        Waypoint waypoint = Main.serverState.getWaypoint(wpKey);
        if (waypoint != null) {
            if ( Main.serverState.waypointAccess(waypoint, player) ) {
                PlayerHome home = new PlayerHome(waypoint.getPosition(), (double) waypoint.getYaw(), waypoint.getWorldRegKey(), playerUuid);
                Main.serverState.setPlayerHome( home );
                messageText = () -> Text.literal("Your ")
                        .append(home.positionHover("home"))
                        .append(" has been moved to ")
                        .append(finalOwnerName)
                        .append("waypoint ")
                        .append(waypoint.getNameFormatted())
                        .append(Text.literal("."))
                        .formatted(Colors.DEFAULT);
            }
        }

        context.getSource().sendFeedback(messageText, false);
        return 1;
    }
}
