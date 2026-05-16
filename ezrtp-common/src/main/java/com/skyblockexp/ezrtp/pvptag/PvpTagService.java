package com.skyblockexp.ezrtp.pvptag;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;

/** Aggregates zero or more {@link PvpTagProvider}s and exposes a unified {@code isInCombat} check. */
public final class PvpTagService {

    private final List<PvpTagProvider> providers = new ArrayList<>();

    public void registerProvider(PvpTagProvider provider) {
        providers.add(provider);
    }

    /** Returns {@code true} when at least one provider is registered. */
    public boolean isEnabled() {
        return !providers.isEmpty();
    }

    /**
     * Returns {@code true} when any available provider reports {@code player} as being in combat.
     * Returns {@code false} when {@code player} is {@code null} or no providers are registered.
     */
    public boolean isInCombat(Player player) {
        if (player == null) {
            return false;
        }
        for (PvpTagProvider provider : providers) {
            if (provider.isAvailable() && provider.isInCombat(player)) {
                return true;
            }
        }
        return false;
    }
}
