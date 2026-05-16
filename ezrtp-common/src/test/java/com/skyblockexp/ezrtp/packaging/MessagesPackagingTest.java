package com.skyblockexp.ezrtp.packaging;

import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies packaging/resource layout for message files.
 */
public class MessagesPackagingTest {

    @Test
    public void packagedResourcesUseLocalizedMessages() {
        ClassLoader loader = getClass().getClassLoader();

        // Expect the localized default to be present
        assertNotNull(loader.getResource("messages/en.yml"), "messages/en.yml should be on the classpath");

        // Expect the legacy top-level messages.yml to be absent from EzRTP's own compiled output.
        // Third-party dependency JARs on the test classpath may contain their own messages.yml,
        // so we only fail if the resource originates from a compiled classes directory.
        URL legacyResource = loader.getResource("messages.yml");
        if (legacyResource != null) {
            String url = legacyResource.toString();
            assertFalse(
                    url.contains("/classes/") || url.contains("\\classes\\"),
                    "legacy top-level messages.yml should not be packaged in EzRTP's own output; found at: " + url);
        }
    }
}
