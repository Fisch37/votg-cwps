package com.cimadev.cimpleWaypointSystem.command;

import com.cimadev.cimpleWaypointSystem.Colors;
import com.cimadev.cimpleWaypointSystem.PermissionHelpers;
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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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
            !source.isPlayer()
                    || Objects.requireNonNull(source.getPlayer())
                    .getUUID()
                    .equals(waypoint.getOwner());
    private static final AccessSuggestionProvider accessSuggestionsAdminsOpen = new AccessSuggestionProvider(PermissionHelpers::hasAdmin, AccessLevel.OPEN);
    private static final AccessSuggestionProvider accessSuggestionsNoOpen = new AccessSuggestionProvider(AccessLevel.OPEN);
    private static final WaypointSuggestionProvider waypointSuggestionsOnlySelf = new WaypointSuggestionProvider(
            false, true,
            ONLY_SELF_PREDICATE
    );
    private static final WaypointSuggestionProvider waypointsFilteredWithOwner =
            new WaypointSuggestionProvider(true, true);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandRegistryAccess, Commands.CommandSelection registrationEnvironment) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(COMMAND_NAME)
                .then(Commands.argument("name", string())
                        .executes(WpsCommand::wpsGoDerived)
                        .then(Commands.argument("owner", word())
                                .suggests(new OfflinePlayerSuggestionProvider())
                                .executes(WpsCommand::wpsGoOwned)
                        )
                        .then(Commands.literal("open")
                                .executes(WpsCommand::wpsGoOpen)
                ))
                .then(Commands.literal("help")
                        .executes(WpsCommand::wpsHelp)
                )
                .then(Commands.literal("go")
                        .then(Commands.argument("name", string())
                                .suggests(waypointsFilteredWithOwner)
                                .executes(WpsCommand::wpsGoDerived)
                                .then(Commands.argument("owner", word())
                                        .suggests(new OfflinePlayerSuggestionProvider())
                                        .executes(WpsCommand::wpsGoOwned)
                                )
                                .then(Commands.literal("open")
                                        .executes(WpsCommand::wpsGoOpen)
                )))
                .then(Commands.literal("here")
                        .then(Commands.argument("name", string())
                                .executes(WpsCommand::wpsHereMine)
                                .then(Commands.argument("access", word())
                                        .suggests(accessSuggestionsAdminsOpen)
                                        .executes(WpsCommand::wpsHereMine)
                )))
                .then(Commands.literal("add")
                        .then(Commands.argument("name", string())
                                .executes(WpsCommand::wpsAddMine)
                                .then(Commands.argument("access", word())
                                        .suggests(accessSuggestionsAdminsOpen)
                                        .executes(WpsCommand::wpsAddMine)
                )))
                .then(Commands.literal("set")
                        .requires(PermissionHelpers::hasAdmin)
                        .then(Commands.argument("name", string())
                                .then(Commands.argument("owner", word())
                                        .suggests(new OfflinePlayerSuggestionProvider())
                                        .then(Commands.argument("access", word())
                                                .suggests(accessSuggestionsNoOpen)
                                                .executes(WpsCommand::wpsSet)
                                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                        .executes(WpsCommand::wpsSet)
                                                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                                                .executes(WpsCommand::wpsSet)
                                                                .then(Commands.argument(
                                                                            "yaw",
                                                                            DoubleArgumentType.doubleArg(-90, 90)
                                                                    )
                                                                        .executes(WpsCommand::wpsSet)
                                )))))
                                .then(Commands.literal("open")
                                        .executes(WpsCommand::wpsSetOpen)
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .executes(WpsCommand::wpsSetOpen)
                                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                                        .executes(WpsCommand::wpsSet)
                                                        .then(Commands.argument("yaw", DoubleArgumentType.doubleArg(-90, 90))
                                                                .executes(WpsCommand::wpsSetOpen)
                ))))))
                .then(Commands.literal("list")
                        .executes(WpsCommand::wpsListAccessible)
                        .then(Commands.argument("owner", word())
                                .suggests(new OfflinePlayerSuggestionProvider())
                                .executes(WpsCommand::wpsListOwnedAccessible)
                                .then(Commands.literal("all")
                                        .requires(PermissionHelpers::hasAdmin)
                                        .executes(WpsCommand::wpsListOwnedAll)
                        ))
                        .then(Commands.literal("mine")
                                .executes(WpsCommand::wpsListMine)
                        )
                        .then(Commands.literal("open")
                                .executes(WpsCommand::wpsListOpen)
                ))
                .then(Commands.literal("remove")
                        .then(Commands.argument("name", string())
                                .suggests(waypointSuggestionsOnlySelf)
                                .executes(WpsCommand::wpsRemoveMine)
                                .then(Commands.argument("owner", word())
                                        .requires(PermissionHelpers::hasAdmin)
                                        .executes(WpsCommand::wpsRemoveOwned)
                                )
                                .then(Commands.literal("open")
                                        .requires(PermissionHelpers::hasAdmin)
                                        .executes(WpsCommand::wpsRemoveOpen)
                )))
                .then(Commands.literal("rename")
                        .then(Commands.argument("oldName", string())
                                .suggests(new WaypointSuggestionProvider(
                                        false,
                                        true,
                                        ONLY_SELF_PREDICATE,
                                        "oldName"
                                ))
                                .then(Commands.argument("newName", string())
                                        .executes(WpsCommand::wpsRenameMine)
                                        .then(Commands.argument("owner", word())
                                                .requires(PermissionHelpers::hasAdmin)
                                                .executes(WpsCommand::wpsRenameOwned))
                                        .then(Commands.literal("open")
                                                .requires(PermissionHelpers::hasAdmin)
                                                .executes(WpsCommand::wpsRenameOpen)
                ))))
                .then(Commands.literal("access")
                        .then(Commands.argument("name", string())
                                .then(Commands.argument("access", word())
                                        .suggests(accessSuggestionsNoOpen)
                                        .executes(WpsCommand::wpsSetAccess)
                )))
                .then(Commands.literal("sethome")
                        .executes(WpsCommand::wpsSetHome)
                        .then(Commands.argument("name", string())
                                .suggests(waypointsFilteredWithOwner)
                                .then(Commands.argument("owner", word())
                                        .suggests(new OfflinePlayerSuggestionProvider())
                                        .executes(WpsCommand::wpsSetHome))
                                .then(Commands.literal("open")
                                        .executes(WpsCommand::wpsSetHome)
                )));

        dispatcher.register(command);

        // administrator wps options
        dispatcher.register(Commands.literal(COMMAND_NAME)
                .then(Commands.literal("listAll")
                        .requires(PermissionHelpers::hasOwner) // only meant for printing to console
                        .executes(WpsCommand::wpsListAll)));
    }

    private static int wpsHelp(CommandContext<CommandSourceStack> context) {
        Supplier<Component> messageText;
        CommandSourceStack source = context.getSource();
        messageText = () -> Component.literal("-=- -=- -=- /wps help menu -=- -=- -=-").withStyle(Colors.SECONDARY);
        source.sendSuccess(messageText, false);

        for( String help : DEFAULT_HELP ) {
            messageText = () -> Component.literal(help).withStyle(Colors.DEFAULT);
            source.sendSuccess(messageText, false);
        }

        if (PermissionHelpers.hasAdmin(context.getSource())) {
            for (String help : ADMIN_HELP ) {
                messageText = () -> Component.literal(help).withStyle(Colors.DEFAULT);
                source.sendSuccess(messageText, false);
            }
        }

        messageText = () -> Component.literal("/wps help : Shows this menu.").withStyle(Colors.DEFAULT);
        source.sendSuccess(messageText, false);

        messageText = () -> Component.literal("-=- -=- -=- -=- -=- -=- -=- -=- -=- -=-").withStyle(Colors.SECONDARY);
        source.sendSuccess(messageText, false);

        return 1;
    }

    private static int wpsGoOpen(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeWpsGo(context, null);
    }

    private static int wpsGoDerived(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String waypointName = StringArgumentType.getString(context, "name");
        boolean preferOpen = config.preferOpenForDerived.get();
        var player = context.getSource().getPlayerOrException();
        final CommandFunction forOpen = () -> executeWpsGo(context, null);
        final CommandFunction forSelf = () -> executeWpsGo(
                context,
                OfflinePlayer.fromUuid(player.getUUID())
        );
        Optional<UUID> owner = preferOpen
                ? Optional.empty()
                : Optional.of(player.getUUID());
        if (Main.serverState.waypointExists(new WaypointKey(owner, waypointName)))
            return preferOpen ? forOpen.run() : forSelf.run();
        else
            return preferOpen ? forSelf.run() : forOpen.run();
    }
    private static int wpsGoOwned(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeWpsGo(context, OfflinePlayer.fromContext(context, "owner"));
    }

    private static int executeWpsGo(
            CommandContext<CommandSourceStack> context,
            @Nullable OfflinePlayer owner
    ) throws CommandSyntaxException {
        Supplier<Component> messageText;
        CommandSourceStack commandSource = context.getSource();
        ServerPlayer player = commandSource.getPlayerOrException();
        MinecraftServer server = commandSource.getServer();
        String name = StringArgumentType.getString(context, "name");

        Optional<UUID> ownerUuid = Optional.ofNullable(owner).map(OfflinePlayer::getUuid);
        Waypoint waypoint = Main.serverState.getWaypoint(new WaypointKey(ownerUuid, name));
        String ownerName = ownerUuid
                .map(uuid -> uuid.equals(player.getUUID()) ? "your " : owner.getName() + "'s ")
                .orElse("");

        if (waypoint != null && Main.serverState.waypointAccess(waypoint, player)) {
            Vec3 wpPos = waypoint.getPosition().getBottomCenter();
            ServerLevel world = server.getLevel(waypoint.getWorldRegKey());
            if ( world == null ) return -1;
            int yaw = waypoint.getYaw();
            WpsUtils.teleport(player, world, wpPos.x(), wpPos.y(), wpPos.z(), yaw, 0);

            messageText = () -> Component.literal("Teleported to ")
                    .append(Component.literal(ownerName).withStyle(Colors.PLAYER))
                    .append(waypoint.getAccessFormatted())
                    .append(Component.literal(" waypoint "))
                    .append(waypoint.getNameFormatted())
                    .append(Component.literal("."))
                    .withStyle(Colors.DEFAULT);
        } else {
            messageText = () -> Component.literal(ownerName).withStyle(Colors.PLAYER)
                    .append(Component.literal(" waypoint "))
                    .append(Component.literal( name ).withStyle(Colors.LINK_INACTIVE))
                    .append(Component.literal(" could not be found."))
                    .withStyle(Colors.DEFAULT);
        }

        commandSource.sendSuccess(messageText, false);
        return 1;
    }

    // wpsAddMine and wpsAddOpen for compatibility reasons
    private static int wpsAddMine(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return wpsHere(context, false);
    }

    private static int wpsHereMine(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return wpsHere(context, true);
    }

    private static int wpsHere(CommandContext<CommandSourceStack> context, boolean moveIfExists) throws CommandSyntaxException {
        /* todo:
         * redesign the whole wpsAdd and wpsHere thing to a) be consistent, b) not pingpong c) not change the access on move if not specified (currently changes to private)
         */
        Supplier<Component> messageText;

        ServerPlayer player = context.getSource().getPlayerOrException();
        BlockPos blockPos = new BlockPos(player.blockPosition());
        double yaw = player.getYRot();
        ServerLevel world = player.level();

        String name = StringArgumentType.getString(context, "name");
        AccessLevel access = config.defaultAccess.get();
        if ( context.getNodes().size() == 4 ) {
            access = AccessLevel.fromContext(context, "access");
        }
        if (access == AccessLevel.OPEN && !PermissionHelpers.hasAdmin(context.getSource())) {
            AccessLevel finalAccess = access;
            throw new SimpleCommandExceptionType(() -> "Invalid access type " + finalAccess.getName() + ".").create();
        }

        Optional<UUID> owner = (access == AccessLevel.OPEN ? Optional.empty() : Optional.of(player.getUUID()));
        Waypoint newWaypoint = new Waypoint(
                new WaypointKey(owner, name),
                blockPos,
                world.dimension(),
                (int)yaw,
                access
        );
        Waypoint oldWaypoint = Main.serverState.getWaypoint(newWaypoint.getKey());
        if ( oldWaypoint == null ) {
            messageText = () -> wpsAdd(newWaypoint);
        } else if ( moveIfExists ) {
            messageText = () -> wpsMove(oldWaypoint, newWaypoint);
        } else {
            messageText = () -> Component.literal("Your ")
                    .append(oldWaypoint.getAccessFormatted())
                    .append( " waypoint " )
                    .append(oldWaypoint.getNameFormatted())
                    .append(" already exists!").withStyle(Colors.DEFAULT);
        }

        context.getSource().sendSuccess(messageText, false);
        Main.serverState.setDirty();
        return 1;
    }

    private static MutableComponent wpsAdd(Waypoint newWaypoint) {
        Main.serverState.setWaypoint( newWaypoint );
        return Component.literal("Set ")
                .append(newWaypoint.getAccessFormatted())
                .append(" waypoint ")
                .append(newWaypoint.getNameFormatted())
                .append(".")
                .withStyle(Colors.DEFAULT);
    }

    private static MutableComponent wpsMove(Waypoint oldWaypoint, Waypoint newWaypoint) {
        BlockPos nwp = newWaypoint.getPosition();
        AccessLevel access = newWaypoint.getAccess();
        BlockPos owp = oldWaypoint.getPosition();
        oldWaypoint.setPosition(nwp);
        oldWaypoint.setYaw(newWaypoint.getYaw());
        oldWaypoint.setAccess(access);

        HoverEvent movedTooltip = new HoverEvent.ShowText(
                Component.literal(" Formerly at x: "  + owp.getX() + ", y: " + owp.getY() + ", z: " + owp.getZ())
        );
        MutableComponent moved = Component.literal("Moved").withStyle(ChatFormatting.UNDERLINE);
        Style waypointStyle = moved.getStyle();
        moved.setStyle(waypointStyle.withHoverEvent(movedTooltip));
        Component oldAccess = oldWaypoint.getAccessFormatted();

        MutableComponent message = Component.literal("")
                .append(moved);
        if (access == AccessLevel.OPEN) message.append(" the ").append(access.getNameFormatted());
        else message.append(" your ").append(oldAccess);
        message.append(" waypoint ")
                .append(newWaypoint.getNameFormatted())
                .append(".")
                .withStyle(Colors.DEFAULT);
        if ( oldWaypoint.getAccess() != access ) {
            message.append(" It is now ")
                    .append(newWaypoint.getAccessFormatted())
                    .append(".")
                    .withStyle(Colors.DEFAULT);
        }
        return message;
    }

    private static int wpsSetOpen(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return wpsSet(context, true);
    }

    private static int wpsSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return wpsSet(context, false);
    }
    private static int wpsSet(
            CommandContext<CommandSourceStack> context,
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

        CommandSourceStack source = context.getSource();
        final String name = StringArgumentType.getString(context, "name");

        Optional<UUID> owner;
        // I swear this was the best option available
        if (!isOpen) {
            UUID ownerDefinite;
            try {
                ownerDefinite = OfflinePlayer.fromContext(context, "owner").getUuid();
            } catch (CommandSyntaxException e) {
                ownerDefinite = UuidArgument.getUuid(context, "owner");
            }
            owner = Optional.of(ownerDefinite);
        } else {
            owner = Optional.empty();
        }
        BlockPos pos;
        try {
            pos = BlockPosArgument.getBlockPos(context, "pos");
        } catch (IllegalArgumentException e) {
            pos = BlockPos.containing(source.getPosition());
        }
        double yaw;
        try {
            yaw = DoubleArgumentType.getDouble(context, "yaw");
        } catch (IllegalArgumentException e) {
            yaw = source.getRotation().x;
        }
        ResourceKey<Level> world;
        try {
            world = DimensionArgument
                    .getDimension(context, "dimension")
                    .dimension();
        } catch (IllegalArgumentException e) {
            world = source.getLevel().dimension();
        }

        Waypoint waypoint = new Waypoint(
                new WaypointKey(owner, name),
                pos, world,
                (int)yaw,
                accessLevel
        );
        serverState.setWaypoint(waypoint);

        source.sendSuccess(
                () -> Component.literal("Created new waypoint ")
                        .append(
                                Component.literal(name)
                                // TODO: Figure out if we can embed /wps go
                                .withStyle(Colors.LINK_INACTIVE)
                        )
                        .append("!") // Most important append of all time
                        .withStyle(Colors.DEFAULT)
                ,
                true
        );

        return 1;
    }

    private static int wpsListAccessible(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayer();
        if ( player == null ) return wpsListAll(context);
        else {
            List<Waypoint> waypoints = WpsUtils.getAccessibleWaypoints(player, null, false, false);
            printWaypointsToUser(context, waypoints);
        }
        return 1;
    }

    private static int wpsListOwnedAccessible(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayer();
        if ( player == null ) return wpsListOwnedAll(context);
        else {
            OfflinePlayer owner = OfflinePlayer.fromContext(context, "owner");
            /*todo: if( owner == null ) error "not a valid player", return 1*/
            List<Waypoint> waypoints = WpsUtils.getAccessibleWaypoints(player, owner, false, false);
            printWaypointsToUser(context, waypoints);
        }
        return 1;
    }

    private static int wpsListAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        List<Waypoint> waypoints = WpsUtils.getAllWaypoints();
        printWaypointsToUser(context, waypoints);
        return 1;
    }
    private static int wpsListOwnedAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {

        ServerPlayer player = context.getSource().getPlayer();
        OfflinePlayer owner = OfflinePlayer.fromContext(context, "owner");
        /*todo: if( owner == null ) error "not a valid player", return 1*/
        List<Waypoint> waypoints = WpsUtils.getAccessibleWaypoints(player, owner, true, false);
        printWaypointsToUser(context, waypoints);
        return 1;

    }
    private static int wpsListMine(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {

        ServerPlayer player = context.getSource().getPlayerOrException();
        OfflinePlayer alsoPlayer = Main.serverState.getPlayerByUuid(player.getUUID());
        List<Waypoint> waypoints = WpsUtils.getAccessibleWaypoints(player, alsoPlayer, false, false);
        printWaypointsToUser(context, waypoints);
        return 1;

    }
    private static int wpsListOpen(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {

        ServerPlayer player = context.getSource().getPlayer();
        List<Waypoint> waypoints = WpsUtils.getAccessibleWaypoints(player, null, false, true);
        printWaypointsToUser(context, waypoints);
        return 1;

    }

    private static void printWaypointsToUser(CommandContext<CommandSourceStack> context, List<Waypoint> waypoints) {
        Supplier<Component> messageText;
        ServerPlayer player = context.getSource().getPlayer();
        UUID playerUuid = ( player == null ) ? null : player.getUUID();

        if ( waypoints.isEmpty() ) {
            messageText = () -> Component.literal("No waypoints found.").withStyle(Colors.DEFAULT);
            context.getSource().sendSuccess(messageText, false);
            return;
        }

        messageText = () -> Component.literal("-=- -=- -=- /" + context.getInput() + " -=- -=- -=-").withStyle(Colors.SECONDARY);
        context.getSource().sendSuccess(messageText, false);
        for (Waypoint waypoint : waypoints) {
            UUID ownerUuid = waypoint.getOwner();
            MutableComponent ownerTitle;
            if (ownerUuid == null) {
                if (waypoint.getAccess() == AccessLevel.SECRET)
                    ownerTitle = Component.literal("Unowned secret").withStyle(Colors.SECRET);
                else ownerTitle = Component.literal("Open").withStyle(Colors.PUBLIC);
            } else {
                OfflinePlayer owner = Main.serverState.getPlayerByUuid(ownerUuid);
                if (ownerUuid.equals(playerUuid)) {
                    ownerTitle = Component.literal("Your ").withStyle(Colors.PLAYER);
                } else if (owner == null) {
                    ownerTitle = Component.literal("[Error finding name]'s ").withStyle(Colors.SECONDARY);
                } else {
                    ownerTitle = Component.literal(owner.getName() + "'s ").withStyle(Colors.PLAYER);
                }
            }
            messageText = () -> Component.literal("")
                    .append(ownerTitle)
                    .append((ownerUuid == null) ? Component.literal("") : waypoint.getAccessFormatted()) // dirty because lazy
                    .append(" waypoint ")
                    .append(waypoint.getNameFormatted())
                    .append(".")
                    .withStyle(Colors.DEFAULT);

            context.getSource().sendSuccess(messageText, false);
        }
        messageText = () -> Component.literal("-=- -=- -=- -=- -=- -=- -=- -=- -=- -=-").withStyle(Colors.SECONDARY);
        context.getSource().sendSuccess(messageText, false);
    }

    private static int wpsRemoveMine(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name");
        WaypointKey wpKey = new WaypointKey(player.getUUID(), name);
        Waypoint waypoint = Main.serverState.getWaypoint(wpKey);
        return wpsRemove(context, waypoint, name, true);
    }

    private static int wpsRemoveOpen(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        WaypointKey wpKey = new WaypointKey(Optional.empty(), name);
        Waypoint waypoint = Main.serverState.getWaypoint(wpKey);
        return wpsRemove(context, waypoint, name, false);
    }

    private static int wpsRemoveOwned(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        OfflinePlayer owner = OfflinePlayer.fromContext(context, "owner");
        WaypointKey wpKey = new WaypointKey(owner.getUuid(), name);
        Waypoint waypoint = Main.serverState.getWaypoint(wpKey);
        return wpsRemove(context, waypoint, name, false);
    }

    private static int wpsRemove(CommandContext<CommandSourceStack> context, @Nullable Waypoint waypoint, String inputName, boolean ownedByCaller) {
        Supplier<Component> messageText;

        MutableComponent ownerTitle;
        UUID ownerUuid = (waypoint == null) ? null : waypoint.getOwner();
        if ( ownedByCaller ) ownerTitle = Component.literal("Your ");
        else {
            if ( ownerUuid == null ) {
                ownerTitle = Component.literal("The ").append(Component.literal("open ").withStyle(Colors.PUBLIC));
            } else {
                ownerTitle = Component.literal(Main.serverState.getPlayerByUuid(ownerUuid).getName() + "'s ");
            }
        }

        if (waypoint == null) {
            messageText = () -> ownerTitle.append("waypoint ")
                    .append(Component.literal( inputName ).withStyle(Colors.LINK_INACTIVE))
                    .append(Component.literal(" could not be found."))
                    .withStyle(Colors.DEFAULT);
        } else {
            Component waypointNameFormatted = waypoint.getNameFormatted();
            Main.serverState.removeWaypoint(waypoint.getKey());
            Main.serverState.setDirty();
            MutableComponent message = Component.literal("")
                    .append(ownerTitle);
            if ( ownerUuid != null ) message.append(waypoint.getAccessFormatted());
            message.append(" waypoint ")
                    .append(waypointNameFormatted)
                    .append(Component.literal(" has been removed."))
                    .withStyle(Colors.DEFAULT);
            messageText = () -> message;
        }
        context.getSource().sendSuccess(messageText, false);
        return 1;
    }

    private static int wpsSetAccess(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Supplier<Component> messageText;

        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name");
        WaypointKey wpKey = new WaypointKey(player.getUUID(), name);
        Waypoint waypoint = Main.serverState.getWaypoint(wpKey);
        if (waypoint == null) {
            messageText = () -> Component.literal("Your waypoint ")
                    .append(Component.literal( name ).withStyle(Colors.LINK_INACTIVE))
                    .append(Component.literal(" could not be found."))
                    .withStyle(Colors.DEFAULT);
        } else {
            AccessLevel access = AccessLevel.fromContext(context, "access");
            Component oldAccess = waypoint.getAccessFormatted();
            waypoint.setAccess(access);
            messageText = () -> Component.literal("Your ")
                    .append(oldAccess)
                    .append(" waypoint ")
                    .append(waypoint.getNameFormatted())
                    .append(Component.literal(" is now "))
                    .append(waypoint.getAccessFormatted())
                    .append(".")
                    .withStyle(Colors.DEFAULT);
            Main.serverState.setDirty();
        }

        context.getSource().sendSuccess(messageText, false);
        return 1;
    }

    private static int wpsRenameMine(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String oldName = StringArgumentType.getString(context, "oldName");
        String newName = StringArgumentType.getString(context, "newName");
        WaypointKey wpKey = new WaypointKey(player.getUUID(), oldName);
        Waypoint waypoint = Main.serverState.getWaypoint(wpKey);
        return wpsRename(context, waypoint, oldName, newName, true);
    }

    private static int wpsRenameOpen(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String oldName = StringArgumentType.getString(context, "oldName");
        String newName = StringArgumentType.getString(context, "newName");
        WaypointKey wpKey = new WaypointKey(Optional.empty(), oldName);
        Waypoint waypoint = Main.serverState.getWaypoint(wpKey);
        return wpsRename(context, waypoint, oldName, newName, false);
    }

    private static int wpsRenameOwned(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String oldName = StringArgumentType.getString(context, "oldName");
        String newName = StringArgumentType.getString(context, "newName");
        OfflinePlayer owner = OfflinePlayer.fromContext(context, "owner");
        WaypointKey wpKey = new WaypointKey(owner.getUuid(), oldName);
        Waypoint waypoint = Main.serverState.getWaypoint(wpKey);
        return wpsRename(context, waypoint, oldName, newName, false);
    }

    private static int wpsRename(CommandContext<CommandSourceStack> context, Waypoint waypoint, String oldName, String newName, boolean ownedByCaller) throws CommandSyntaxException {
        Supplier<Component> messageText;

        MutableComponent ownerTitle;
        UUID ownerUuid = (waypoint == null) ? null : waypoint.getOwner();
        if ( ownedByCaller ) ownerTitle = Component.literal("Your ");
        else {
            if ( ownerUuid == null ) {
                ownerTitle = Component.literal("The ").append(Component.literal("open ").withStyle(Colors.PUBLIC));
            } else {
                ownerTitle = Component.literal(Main.serverState.getPlayerByUuid(ownerUuid).getName() + "'s ");
            }
        }

        if (waypoint == null) {
            String finalOldName = oldName;
            messageText = () -> ownerTitle.append("waypoint ")
                    .append(Component.literal(finalOldName).withStyle(Colors.LINK_INACTIVE))
                    .append(Component.literal(" could not be found."))
                    .withStyle(Colors.DEFAULT);
        } else {
            Main.serverState.removeWaypoint(waypoint.getKey());
            oldName = waypoint.getName();
            waypoint.rename(newName);
            Main.serverState.setWaypoint( waypoint );
            String finalOldName = oldName;
            MutableComponent message = Component.literal("Your waypoint ");
            if ( ownerUuid != null ) message.append(Component.literal(finalOldName).withStyle(Colors.LINK_INACTIVE));
            message.append(Component.literal(" is now called "))
                    .append(waypoint.getNameFormatted())
                    .append(".")
                    .withStyle(Colors.DEFAULT);
            messageText = () -> message;
            Main.serverState.setDirty();
        }

        context.getSource().sendSuccess(messageText, false);
        return 1;
    }

    private static int wpsSetHome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Supplier<Component> messageText;

        ServerPlayer player = context.getSource().getPlayerOrException();
        UUID playerUuid = player.getUUID();
        String name = StringArgumentType.getString(context, "name");
        OfflinePlayer owner;
        MutableComponent ownerName;
        UUID ownerUuid;
        if (context.getNodes().size() == 4) {
            try {
                owner = OfflinePlayer.fromContext(context, "owner");
                ownerName = Component.literal(owner.getName() + "'s ");
                ownerUuid = owner.getUuid();
            } catch (Exception e) {
                ownerName = Component.literal("open ").withStyle(Colors.PUBLIC);
                ownerUuid = null;
            }
        } else {
            owner = Main.serverState.getPlayerByUuid(playerUuid);
            ownerName = Component.literal("Your ");
            ownerUuid = owner.getUuid();
        }

        MutableComponent finalOwnerName = ownerName;
        messageText = () -> Component.literal("").append(finalOwnerName).append("waypoint ")
                .append(Component.literal( name ).withStyle(Colors.LINK_INACTIVE))
                .append(Component.literal(" could not be found."))
                .withStyle(Colors.DEFAULT);

        WaypointKey wpKey = new WaypointKey(ownerUuid, name);
        Waypoint waypoint = Main.serverState.getWaypoint(wpKey);
        if (waypoint != null) {
            if ( Main.serverState.waypointAccess(waypoint, player) ) {
                PlayerHome home = new PlayerHome(waypoint.getPosition(), waypoint.getYaw(), waypoint.getWorldRegKey(), playerUuid);
                Main.serverState.setPlayerHome( home );
                messageText = () -> Component.literal("Your ")
                        .append(home.positionHover("home"))
                        .append(" has been moved to ")
                        .append(finalOwnerName)
                        .append("waypoint ")
                        .append(waypoint.getNameFormatted())
                        .append(Component.literal("."))
                        .withStyle(Colors.DEFAULT);
            }
        }

        context.getSource().sendSuccess(messageText, false);
        return 1;
    }
}
