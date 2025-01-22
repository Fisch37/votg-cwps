package com.cimadev.cimpleWaypointSystem.command.persistentData;

import com.cimadev.cimpleWaypointSystem.FriendsIntegration;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSortedMap;

public class DataFixer {
    private final static Logger LOGGER = LoggerFactory.getLogger("CWPS/DataFixer");
    public final static int CURRENT_VERSION = 1;

    /**
     * Map of Old Data Version -> [(FunctionOutputVersion, FixerFunctions)]
     * <p>
     * Where FunctionOutputVersion is always <em>decreasing</em> within a list.
     * Ideally there should always be a path from any data version to any higher data version.
     * <p>
     * The fix algorithm will always choose the largest jump possible.
     * It is up to developers to ensure that functions that skip versions will still allow updates to (any higher version).
     */
    private final static Map<Integer, ImmutableSortedMap<Integer, FixerFunction>> FIXER_MAP = Map.of(
            0, ImmutableSortedMap.of(
                    1, DataFixer::FIX_transferFriendsToIntegration
            )
    );

    /**
     * Ensures that this tag is marked with the current version.
     * @param tag The tag to modify
     */
    public static void setToCurrentVersion(NbtCompound tag) {
        tag.putInt("data_version", CURRENT_VERSION);
    }

    public static NbtCompound fixData(NbtCompound data) {
        int dataVersion = data.getInt("data_version");
        if (dataVersion == CURRENT_VERSION) {
            return data;
        } else if (dataVersion > CURRENT_VERSION) {
            LOGGER.error("Downgrade dectected! Downgrades are not supported and will lead to bugs, data loss, and/or crashes");
            return data;
        }

        LOGGER.info("Updating data from version {} to version {}", dataVersion, CURRENT_VERSION);
        ImmutableSortedMap<Integer, FixerFunction> fixes = FIXER_MAP.get(dataVersion);
        Map.Entry<Integer, FixerFunction> fix = fixes.entrySet()
                .stream()
                .filter(entry -> entry.getKey() <= CURRENT_VERSION)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No valid fixer found for " + dataVersion + " -> " + CURRENT_VERSION));
        int outputVersion = fix.getKey();
        FixerFunction function = fix.getValue();
        NbtCompound fixed = applyFix(data, outputVersion, function);
        return fixData(fixed); // This doesn't loop infinitely because of the exit-condition above :)
    }

    private static NbtCompound applyFix(NbtCompound data, int outputVersion, FixerFunction function) {
        NbtCompound fixedData = data.copy();
        function.fix(fixedData);
        fixedData.putInt("data_version", outputVersion);
        return fixedData;
    }

    @FunctionalInterface
    private interface FixerFunction {
        @Contract(mutates = "param")  // This is non-experimental as of annotations 26
        void fix(NbtCompound data);
    }


    /*
     * Put all FixerFunctions here. FixerFunctions should always be private, static and begin with "FIX_".
     * Since they are void methods, they mutate their parameter.
     */

    /**
     * Transfers old-style friends into the FischyFriends mod (if available)
     * This fix is a bit wonky in that the transfer will only work if the mod is installed at first launch
     * (with this version).
     * Otherwise, the data will be lost.
     */
    private static void FIX_transferFriendsToIntegration(NbtCompound input) {
        // There is probably a bespoke data structure & algorithm that would cost less space & performance
        // Given how little this method will be called (if ever, at all), I don't see a reason to figure it out.
        Set<Pair<UUID, UUID>> friendProposals = new HashSet<>();
        input.getList("playerList", NbtElement.COMPOUND_TYPE).forEach(nbt -> {
            NbtCompound playerData = (NbtCompound) nbt;
            NbtCompound friendsList = playerData.getCompound("friendList"); // Yes this is correct
            playerData.remove("friendList");
            UUID player = playerData.getUuid("uuid");
            // friendList is a compound and follows this pattern:
            /*
              friend1 -> friend2
              friend3 -> friend4
                     ...
              friendN -> friendN (only if odd)
             */
            // This means every key corresponds to (usually) 2 friends
            // Since it won't cause any problems (I hope) we can ignore that usually and maybe issue two equal requests
            friendsList.getKeys()
                    .stream()
                    .flatMap(key -> Stream.of(key, friendsList.getString(key)))
                    .map(UUID::fromString)
                    .filter(uuid -> !uuid.equals(player))
                    .map(friend -> new Pair<>(player, friend))
                    .forEach(pair -> {
                        final Pair<UUID, UUID> opposite = new Pair<>(pair.getRight(), pair.getLeft());
                        // If two people are each-other's friends we say that qualifies as a full friendship
                        if (friendProposals.remove(opposite)) {
                            FriendsIntegration.makeFriends(pair.getLeft(), pair.getRight());
                        } else {
                            friendProposals.add(pair);
                        }
                    });
        });
        for (Pair<UUID, UUID> request : friendProposals) {
            FriendsIntegration.sendFriendRequest(request.getLeft(), request.getRight());
        }
    }
}
