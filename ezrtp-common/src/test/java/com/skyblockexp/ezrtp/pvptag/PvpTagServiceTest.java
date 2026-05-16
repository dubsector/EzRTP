package com.skyblockexp.ezrtp.pvptag;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class PvpTagServiceTest {

    @Test
    void isEnabled_noProviders_returnsFalse() {
        PvpTagService service = new PvpTagService();
        assertFalse(service.isEnabled());
    }

    @Test
    void isEnabled_withProvider_returnsTrue() {
        PvpTagService service = new PvpTagService();
        service.registerProvider(stubProvider("Test", true, false));
        assertTrue(service.isEnabled());
    }

    @Test
    void isInCombat_noProviders_returnsFalse() {
        PvpTagService service = new PvpTagService();
        Player player = mock(Player.class);
        assertFalse(service.isInCombat(player));
    }

    @Test
    void isInCombat_nullPlayer_returnsFalse() {
        PvpTagService service = new PvpTagService();
        service.registerProvider(stubProvider("Test", true, true));
        assertFalse(service.isInCombat(null));
    }

    @Test
    void isInCombat_notInCombat_returnsFalse() {
        PvpTagService service = new PvpTagService();
        Player player = mock(Player.class);
        service.registerProvider(stubProvider("Test", true, false));
        assertFalse(service.isInCombat(player));
    }

    @Test
    void isInCombat_inCombat_returnsTrue() {
        PvpTagService service = new PvpTagService();
        Player player = mock(Player.class);
        service.registerProvider(stubProvider("Test", true, true));
        assertTrue(service.isInCombat(player));
    }

    @Test
    void isInCombat_unavailableProvider_skipped() {
        PvpTagService service = new PvpTagService();
        Player player = mock(Player.class);
        service.registerProvider(stubProvider("Unavailable", false, true));
        assertFalse(service.isInCombat(player));
    }

    @Test
    void isInCombat_firstProviderFalse_secondProviderTrue_returnsTrue() {
        PvpTagService service = new PvpTagService();
        Player player = mock(Player.class);
        service.registerProvider(stubProvider("First", true, false));
        service.registerProvider(stubProvider("Second", true, true));
        assertTrue(service.isInCombat(player));
    }

    @Test
    void isInCombat_multipleProviders_allFalse_returnsFalse() {
        PvpTagService service = new PvpTagService();
        Player player = mock(Player.class);
        service.registerProvider(stubProvider("First", true, false));
        service.registerProvider(stubProvider("Second", true, false));
        assertFalse(service.isInCombat(player));
    }

    private static PvpTagProvider stubProvider(String name, boolean available, boolean inCombat) {
        PvpTagProvider provider = mock(PvpTagProvider.class);
        when(provider.getName()).thenReturn(name);
        when(provider.isAvailable()).thenReturn(available);
        when(provider.isInCombat(org.mockito.ArgumentMatchers.any())).thenReturn(inCombat);
        return provider;
    }
}
