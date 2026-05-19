package com.skyblockexp.ezrtp.integration;

import com.skyblockexp.teamsapi.api.TeamsService;
import com.skyblockexp.teamsapi.model.Team;
import com.skyblockexp.teamsapi.model.TeamClaim;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

public final class ClaimOwnerResolver {

    private ClaimOwnerResolver() {
    }

    public static Optional<UUID> resolvePreferredOwnerUuid(TeamsService teamsService, TeamClaim claim) {
        if (claim == null) {
            return Optional.empty();
        }
        Optional<UUID> reflective = tryReflectivePlayerUuid(claim);
        if (reflective.isPresent()) {
            return reflective;
        }
        if (teamsService == null) {
            return Optional.empty();
        }
        Team team = teamsService.getTeam(claim.getTeamId()).orElse(null);
        if (team == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(team.getOwnerUUID());
    }

    private static Optional<UUID> tryReflectivePlayerUuid(TeamClaim claim) {
        String[] candidates = {"getPlayerUUID", "getClaimedBy", "getClaimerUUID", "getOwnerUUID"};
        for (String methodName : candidates) {
            try {
                Method method = claim.getClass().getMethod(methodName);
                Object value = method.invoke(claim);
                if (value instanceof UUID uuid) {
                    return Optional.of(uuid);
                }
                if (value instanceof String text) {
                    return Optional.of(UUID.fromString(text));
                }
            } catch (Exception ignored) {
            }
        }
        return Optional.empty();
    }
}
