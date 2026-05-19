package com.skyblockexp.ezrtp.integration;

import com.skyblockexp.teamsapi.api.TeamsService;
import com.skyblockexp.teamsapi.model.Team;
import com.skyblockexp.teamsapi.model.TeamClaim;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClaimOwnerResolverTest {

    @Test
    void resolvesReflectiveClaimerWhenAvailable() {
        UUID teamId = UUID.randomUUID();
        UUID claimer = UUID.randomUUID();
        TeamClaim claim = new ReflectiveClaim(teamId, claimer);
        Optional<UUID> resolved = ClaimOwnerResolver.resolvePreferredOwnerUuid(null, claim);
        assertEquals(claimer, resolved.orElse(null));
    }

    @Test
    void fallsBackToTeamOwner() {
        UUID teamId = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        TeamsService teamsService = mock(TeamsService.class);
        Team team = mock(Team.class);
        when(team.getOwnerUUID()).thenReturn(owner);
        when(teamsService.getTeam(teamId)).thenReturn(Optional.of(team));

        TeamClaim claim = new BasicClaim(teamId);
        Optional<UUID> resolved = ClaimOwnerResolver.resolvePreferredOwnerUuid(teamsService, claim);
        assertEquals(owner, resolved.orElse(null));
    }

    @Test
    void returnsEmptyWhenNoIdentityAvailable() {
        UUID teamId = UUID.randomUUID();
        TeamsService teamsService = mock(TeamsService.class);
        when(teamsService.getTeam(teamId)).thenReturn(Optional.empty());
        Optional<UUID> resolved = ClaimOwnerResolver.resolvePreferredOwnerUuid(teamsService, new BasicClaim(teamId));
        assertTrue(resolved.isEmpty());
    }

    private static class BasicClaim implements TeamClaim {
        private final UUID teamId;
        private BasicClaim(UUID teamId) { this.teamId = teamId; }
        @Override public UUID getTeamId() { return teamId; }
        @Override public String getWorldName() { return "world"; }
        @Override public int getChunkX() { return 0; }
        @Override public int getChunkZ() { return 0; }
        @Override public Instant getClaimedAt() { return Instant.now(); }
    }

    private static final class ReflectiveClaim extends BasicClaim {
        private final UUID claimer;
        private ReflectiveClaim(UUID teamId, UUID claimer) {
            super(teamId);
            this.claimer = claimer;
        }
        public UUID getClaimedBy() {
            return claimer;
        }
    }
}
