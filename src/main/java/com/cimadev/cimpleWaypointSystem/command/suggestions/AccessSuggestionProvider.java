package com.cimadev.cimpleWaypointSystem.command.suggestions;

import com.cimadev.cimpleWaypointSystem.persistentData.AccessLevel;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;

import static com.cimadev.cimpleWaypointSystem.Main.config;

public class AccessSuggestionProvider implements SuggestionProvider<CommandSourceStack> {

    private static final AccessLevel[] ALL_ACCESS_LEVELS = AccessLevel.values();
    private final AccessLevel[] includeAccessLevels;
    private final Predicate<CommandSourceStack> override;

    public AccessSuggestionProvider(AccessLevel... exclude) {
        /* Exclude any access level from a suggestion provider by passing it to the access provider. */
        this(source -> (false), exclude);
    }

    /**
     *
     * @param override pass a function which overrides the
     *                 exclusion of access levels in getSuggestions.
     *                 Intended for permission levels.
     * @param exclude any number of access levels which to
     *                exclude from suggestion if not overridden
     */
    public AccessSuggestionProvider(final Predicate<CommandSourceStack> override, AccessLevel... exclude) {
        this.override = override;
        int includedCounter = 0;
        final List<AccessLevel> CONFIG_EXCLUDED = config.disabledAccessLevels.get();
        includeAccessLevels = new AccessLevel[
                ALL_ACCESS_LEVELS.length
                        - exclude.length
                        - CONFIG_EXCLUDED.size()
                ];
        for (AccessLevel access : ALL_ACCESS_LEVELS) {
            boolean include = true;
            for (AccessLevel e : CONFIG_EXCLUDED) {
                if (access == e) {
                    include = false;
                    break;
                }
            }
            for (AccessLevel e : exclude) {
                if (access == e) {
                    include = false;
                    break;
                }
            }
            if (include) {
                includeAccessLevels[includedCounter] = access;
                includedCounter++;
            }
        }
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        AccessLevel[] appliedLevels = ( override.test(context.getSource()) ? ALL_ACCESS_LEVELS : includeAccessLevels );
        for (AccessLevel access : appliedLevels) {
            builder.suggest(access.getName());
        }
        return builder.buildFuture();
    }
}
