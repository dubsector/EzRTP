package com.skyblockexp.ezrtp.integration;

import com.skyblockexp.ezrtp.gui.ClaimSnapshot;
import com.skyblockexp.ezrtp.util.MessageUtil;
import com.skyblockexp.teamsapi.api.TeamsAPI;
import com.skyblockexp.teamsapi.api.TeamsClaimService;
import com.skyblockexp.teamsapi.api.TeamsService;
import com.skyblockexp.teamsapi.model.Team;
import com.skyblockexp.teamsapi.model.TeamClaim;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Fetches a player's faction claim data from TeamsAPI and converts it to {@link ClaimSnapshot}
 * DTOs that contain only standard Java types.
 *
 * <p>This class intentionally holds all TeamsAPI type references so that
 * {@link com.skyblockexp.ezrtp.gui.FactionClaimSelectionGuiManager} — a Bukkit
 * {@code Listener} — can call {@link #fetch(Player)} from a method body without having
 * TeamsAPI types appear in its own class structure (field descriptors, method descriptors).
 * Bukkit's event registration uses reflection to process listener classes; any unresolvable
 * type in a method descriptor causes {@code NoClassDefFoundError} during
 * {@code registerEvents}. By isolating those references here, the listener loads cleanly even
 * when TeamsAPI is absent.
 */
public final class TeamsApiClaimFetcher {

    private TeamsApiClaimFetcher() {}

    /**
     * Fetches the player's team claims as {@link ClaimSnapshot} instances, resolving owner UUIDs
     * and sorting the results. Sends an appropriate error message to the player and returns
     * {@link Optional#empty()} if TeamsAPI is unavailable or any required data is missing.
     *
     * @param player the player whose team claims to load
     * @return an Optional containing the fetch result, or empty if any precondition fails
     */
    public static Optional<FetchResult> fetch(Player player) {
        if (!TeamsAPI.isAvailable() || !TeamsAPI.isClaimAvailable()) {
            MessageUtil.send(player, "<red>TeamsAPI is not available on this server.</red>");
            return Optional.empty();
        }

        TeamsService teamsService = TeamsAPI.getService();
        TeamsClaimService claimService = TeamsAPI.getClaimService();
        if (teamsService == null || claimService == null) {
            MessageUtil.send(player, "<red>TeamsAPI services are not available right now.</red>");
            return Optional.empty();
        }

        Team team = teamsService.getPlayerTeam(player.getUniqueId()).orElse(null);
        if (team == null) {
            MessageUtil.send(player, "<red>You are not in a faction/team.</red>");
            return Optional.empty();
        }

        Collection<TeamClaim> rawClaims = claimService.getTeamClaims(team.getId());
        if (rawClaims.isEmpty()) {
            MessageUtil.send(player, "<red>Your faction/team has no claims.</red>");
            return Optional.empty();
        }

        List<ClaimSnapshot> snapshots = new ArrayList<>(rawClaims.size());
        for (TeamClaim claim : rawClaims) {
            UUID ownerUuid = ClaimOwnerResolver.resolvePreferredOwnerUuid(teamsService, claim).orElse(null);
            snapshots.add(new ClaimSnapshot(claim.getWorldName(), claim.getChunkX(), claim.getChunkZ(), ownerUuid));
        }
        snapshots.sort(
                Comparator.comparing(ClaimSnapshot::worldName)
                        .thenComparingInt(ClaimSnapshot::chunkX)
                        .thenComparingInt(ClaimSnapshot::chunkZ));

        return Optional.of(new FetchResult(snapshots, team.getDisplayName(), team.getId()));
    }

    /**
     * Holds the resolved claim data returned by {@link #fetch(Player)}.
     */
    public record FetchResult(List<ClaimSnapshot> claims, String teamDisplayName, UUID teamId) {}
}
