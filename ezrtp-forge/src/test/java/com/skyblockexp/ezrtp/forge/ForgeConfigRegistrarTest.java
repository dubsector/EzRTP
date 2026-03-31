package com.skyblockexp.ezrtp.forge;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

public class ForgeConfigRegistrarTest {

    @Test
    public void testRegisterClientConfigReturnsFalseWhenForgeAbsent() {
        Logger logger = Logger.getLogger("EzRTP-Test");
        Assertions.assertFalse(ForgeConfigRegistrar.registerClientConfig(logger));
    }
}
