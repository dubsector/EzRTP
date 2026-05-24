package com.skyblockexp.ezrtp.update;

import com.github.ezplugins.updater.ChainedUpdateChecker;
import com.github.ezplugins.updater.ModrinthUpdateChecker;
import com.github.ezplugins.updater.UpdateChecker;
import com.github.ezplugins.updater.UpdateResult;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;

public final class ReleaseUpdateChecker {
    private static final String MODRINTH_PROJECT_SLUG = "ezplugins-ezrtp";
    private static final String GITHUB_OWNER = "ez-plugins";
    private static final String GITHUB_REPO = "EzRTP";
    private final JavaPlugin plugin;
    private final String userAgent;

    public ReleaseUpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.userAgent = "ez-plugins/ezrtp-update-checker (" + GITHUB_OWNER + "/" + GITHUB_REPO + ")";
    }

    public void checkForUpdates() {
        String currentVersion = plugin.getDescription().getVersion();
        if (currentVersion != null && currentVersion.contains("-nightly.")) {
            plugin.getLogger()
                    .info("Running a nightly build (" + currentVersion
                            + "); skipping update check. Check Modrinth for the latest stable release.");
            return;
        }

        ModrinthUpdateChecker modrinthChecker = ModrinthUpdateChecker
                .builder(MODRINTH_PROJECT_SLUG, currentVersion)
                .loaders(List.of("paper", "spigot", "purpur", "bukkit"))
                .includeChangelog(false)
                .userAgent(userAgent)
                .build();
        UpdateChecker githubChecker = UpdateChecker
                .builder(GITHUB_OWNER, GITHUB_REPO, currentVersion)
                .build();

        ChainedUpdateChecker
                .builder()
                .primary(modrinthChecker)
                .backup(githubChecker)
                .build()
                .checkNowAsync()
                .thenAccept(chainedResult -> logResult(currentVersion, chainedResult.getResult()))
                .exceptionally(ex -> {
                    plugin.getLogger().warning("Failed to check for EzRTP updates: " + ex.getMessage());
                    return null;
                });
    }

    private void logResult(String currentVersion, UpdateResult result) {
        if (result == null) {
            plugin.getLogger().warning("Update check did not return a result.");
            return;
        }
        if (result.hasError()) {
            plugin.getLogger().warning("Update check failed: "
                    + result.getError().map(Throwable::getMessage).orElse("unknown error"));
            return;
        }
        if (result.isUpdateAvailable()) {
            String latest = result.getLatestVersion().orElse("unknown");
            String releaseUrl = result.getReleaseUrl().orElse("https://modrinth.com/plugin/ezplugins-ezrtp");
            plugin.getLogger().info("A new EzRTP version is available: " + latest
                    + " (current: " + currentVersion + "). Download: " + releaseUrl);
            return;
        }
        plugin.getLogger().info("EzRTP is up to date.");
    }
}
