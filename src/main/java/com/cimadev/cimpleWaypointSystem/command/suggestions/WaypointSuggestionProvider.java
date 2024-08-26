package com.cimadev.cimpleWaypointSystem.command.suggestions;
import com.cimadev.cimpleWaypointSystem.command.WpsUtils;
import com.cimadev.cimpleWaypointSystem.command.persistentData.Waypoint;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;

public class WaypointSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaypointSuggestionProvider.class);

    private final boolean withOwner, removeImpossible;
    private final @Nullable WaypointValidator predicate;
    private final String waypointNameArgument;

    public WaypointSuggestionProvider() { this(false, false); }
    public WaypointSuggestionProvider(boolean withOwner, boolean removeImpossible) {
        this(withOwner, removeImpossible, (WaypointValidator) null);
    }
    public WaypointSuggestionProvider(boolean withOwner, boolean removeImpossible, @NotNull String targetArgument) {
        this(withOwner, removeImpossible, null, targetArgument);
    }
    public WaypointSuggestionProvider(
            boolean withOwner,
            boolean removeImpossible,
            WaypointValidator predicate
    ) {
        this(withOwner, removeImpossible, predicate, "name");
    }
    
    public WaypointSuggestionProvider(
            boolean withOwner,
            boolean removeImpossible,
            @Nullable WaypointValidator predicate,
            String targetArgument
    ) {
        this.withOwner = withOwner;
        this.removeImpossible = removeImpossible;
        this.predicate = predicate;
        this.waypointNameArgument = targetArgument;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        String currentName;
        try {
            currentName = StringArgumentType.getString(context, waypointNameArgument);
        } catch (IllegalArgumentException e) {
            // NOTE: This fucks up everything when someone mistypes the argument name in the constructor
            //      There is nothing we can do about this. If the player hasn't input anything, then the argument
            //      does not exist. Same as mistyping. Fuck that.
            currentName = "";
        }
        List<Waypoint> waypoints = WpsUtils.getAccessibleWaypoints(player,null, false, false);
        for (Waypoint waypoint : waypoints) {
            if (removeImpossible && !waypoint.getName().toLowerCase().startsWith(currentName.toLowerCase()))
                continue;
            if (predicate != null && !predicate.test(context.getSource(), waypoint))
                continue;
            String suggestion;
            if (!withOwner)
                suggestion = waypoint.getNameForCommand();
            else {
                try {
                    suggestion = waypoint.getCommandComponent();
                } catch (IllegalStateException e) {
                    LOGGER.warn("Could not find owner of waypoint {}", waypoint.getName());
                    continue;
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Unowned secret (or other broken waypoint state) encountered", e);
                    continue;
                }
            }
            builder.suggest(suggestion);
        }

        return builder.buildFuture();
    }
    
    @FunctionalInterface
    public interface WaypointValidator {
        boolean test(ServerCommandSource source, Waypoint waypoint);
    }
}
