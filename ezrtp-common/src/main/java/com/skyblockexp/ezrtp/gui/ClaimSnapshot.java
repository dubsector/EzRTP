package com.skyblockexp.ezrtp.gui;

import java.util.UUID;

/**
 * Immutable snapshot of a faction claim's data, using only standard Java types so that
 * {@link FactionClaimSelectionGuiManager} (a Bukkit {@code Listener}) can reference it
 * without pulling TeamsAPI types into the class structure.
 *
 * <p>All TeamsAPI-dependent resolution (owner UUID, sorting) is performed up-front by
 * {@link com.skyblockexp.ezrtp.integration.TeamsApiClaimFetcher} before a snapshot is created.
 */
public record ClaimSnapshot(String worldName, int chunkX, int chunkZ, UUID ownerUuid) {}
