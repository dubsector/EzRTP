package com.skyblockexp.ezrtp.bootstrap.component;

import com.skyblockexp.ezrtp.EzRtpPlugin;
import com.skyblockexp.ezrtp.config.EzRtpConfiguration;
import com.skyblockexp.ezrtp.config.performance.PerformanceSettings;
import com.skyblockexp.ezrtp.config.safety.UnsafeLocationSettings;
import com.skyblockexp.ezrtp.message.MessageProvider;
import com.skyblockexp.ezrtp.util.MessageUtil;
import com.skyblockexp.ezrtp.config.RandomTeleportSettings;
import com.skyblockexp.ezrtp.config.ForceRtpConfiguration;
import com.skyblockexp.ezrtp.economy.EconomyService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import com.skyblockexp.ezrtp.message.MessageKey;
import com.skyblockexp.ezrtp.message.MessageProvider;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles configuration reloads, auxiliary file management, and localized message provisioning.
 */
public final class ConfigurationService {

    private final EzRtpPlugin plugin;
    private MessageProvider messageProvider;
    private ConfigurationSection effectiveBaseConfiguration;

    public ConfigurationService(EzRtpPlugin plugin) {
        this.plugin = plugin;
    }

    public EzRtpConfiguration reloadConfiguration() {
        ensureAdditionalConfigFiles();
        plugin.reloadConfig();

        FileConfiguration rtpConfiguration = loadExternalConfiguration("rtp.yml");
        FileConfiguration limitsConfiguration = loadExternalConfiguration("limits.yml");
        FileConfiguration storageConfiguration = loadExternalConfiguration("storage.yml");
        FileConfiguration messagesConfiguration;
        FileConfiguration guiConfiguration;
        FileConfiguration factionGuiConfiguration;
        FileConfiguration queueConfiguration;
        FileConfiguration networkConfiguration;

        effectiveBaseConfiguration = buildEffectiveBaseConfiguration(
                plugin.getConfig(),
                rtpConfiguration,
                limitsConfiguration,
                storageConfiguration);

        setupMessagePrefix();
        setupMessageProvider();

        String language = effectiveBaseConfiguration.getString("language", "en");
        messagesConfiguration = loadExternalConfiguration("messages/" + language + ".yml");
        guiConfiguration = loadExternalConfiguration("gui.yml");
        factionGuiConfiguration = loadExternalConfiguration("faction-gui.yml");
        queueConfiguration = loadExternalConfiguration("queue.yml");
        networkConfiguration = loadExternalConfiguration("network.yml");

        ConfigurationSection fallbackMessages = effectiveBaseConfiguration.getConfigurationSection("messages");
        ConfigurationSection fallbackGui = effectiveBaseConfiguration.getConfigurationSection("gui");
        ConfigurationSection fallbackQueue = effectiveBaseConfiguration.getConfigurationSection("queue");
        ConfigurationSection fallbackNetwork = effectiveBaseConfiguration.getConfigurationSection("network");

        return EzRtpConfiguration.fromConfigurations(
                effectiveBaseConfiguration,
                selectSection(messagesConfiguration, "messages", fallbackMessages),
                selectSection(guiConfiguration, "gui", fallbackGui),
                selectSection(factionGuiConfiguration, "faction-gui", null),
                selectSection(queueConfiguration, "queue", fallbackQueue),
                selectSection(networkConfiguration, "network", fallbackNetwork),
                plugin.getLogger());
    }

    public ForceRtpConfiguration reloadForceRtpConfiguration() {
        FileConfiguration forceRtpConfiguration = loadExternalConfiguration("force-rtp.yml");
        return ForceRtpConfiguration.fromConfiguration(forceRtpConfiguration);
    }

    public MessageProvider getMessageProvider() {
        return messageProvider;
    }

    public ConfigurationSection getEffectiveBaseConfiguration() {
        if (effectiveBaseConfiguration != null) {
            return effectiveBaseConfiguration;
        }
        return plugin.getConfig();
    }

    public void validateEconomyConfiguration(EzRtpConfiguration configuration, EconomyService economyService) {
        if (economyService.isEnabled()) {
            return;
        }
        boolean requiresEconomy = false;
        RandomTeleportSettings defaultSettings = configuration.getDefaultSettings();
        if (defaultSettings != null && defaultSettings.getTeleportCost() > 0) {
            requiresEconomy = true;
        }
        if (!requiresEconomy && configuration.getGuiSettings() != null) {
            requiresEconomy = configuration.getGuiSettings().getWorldOptions().stream()
                    .anyMatch(option -> option.getSettings() != null
                            && option.getSettings().getTeleportCost() > 0);
        }
        if (requiresEconomy) {
            plugin.getLogger().warning("Teleport costs are configured but Vault is unavailable. Teleports will not deduct currency.");
        }
    }

    public void ensureAdditionalConfigFiles() {
        saveResourceIfMissing("rtp.yml");
        saveResourceIfMissing("limits.yml");
        saveResourceIfMissing("storage.yml");
        saveResourceIfMissing("gui.yml");
        saveResourceIfMissing("faction-gui.yml");
        saveResourceIfMissing("queue.yml");
        saveResourceIfMissing("network.yml");
        saveResourceIfMissing("force-rtp.yml");
        saveResourceIfMissing("performance.yml");
        saveResourceIfMissing("unsafe-location-monitoring.yml");
    }

    public PerformanceSettings reloadPerformanceConfiguration() {
        FileConfiguration performanceConfiguration = loadExternalConfiguration("performance.yml");
        return PerformanceSettings.fromConfiguration(performanceConfiguration);
    }

    public UnsafeLocationSettings reloadUnsafeLocationConfiguration() {
        FileConfiguration config = loadExternalConfiguration("unsafe-location-monitoring.yml");
        return UnsafeLocationSettings.fromConfiguration(config);
    }

    private void setupMessagePrefix() {
        String prefix = effectiveBaseConfiguration.getString("message-prefix", "");
        MessageUtil.setPrefix(prefix);
    }

    private void setupMessageProvider() {
        String language = effectiveBaseConfiguration.getString("language", "en");
        File messagesDir = new File(plugin.getDataFolder(), "messages");

        if (!messagesDir.exists()) {
            messagesDir.mkdirs();
        }

        File defaultLanguageFile = new File(messagesDir, "en.yml");
        if (!defaultLanguageFile.exists()) {
            try {
                plugin.saveResource("messages/en.yml", false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Could not save default language file: " + e.getMessage());
            }
        }

        // Backwards compatibility: if a legacy messages.yml exists in the plugin
        // data folder (root), prefer it when no language file is present or when
        // it appears newer than the existing language file. This mirrors the
        // historical behaviour where a single messages.yml at plugin root was
        // used and preserves admin edits when upgrading to the new messages/ dir.
        File legacyMessages = new File(plugin.getDataFolder(), "messages.yml");
        File targetLanguageFile = new File(messagesDir, language + ".yml");
        if (legacyMessages.exists()) {
            try {
                boolean shouldCopy = false;
                if (!targetLanguageFile.exists()) {
                    shouldCopy = true;
                } else if (legacyMessages.lastModified() > targetLanguageFile.lastModified()) {
                    shouldCopy = true;
                }

                if (shouldCopy) {
                    java.nio.file.Files.copy(legacyMessages.toPath(), targetLanguageFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    plugin.getLogger().info("Detected legacy messages.yml - migrated to messages/" + targetLanguageFile.getName());
                }
            } catch (java.io.IOException e) {
                plugin.getLogger().warning("Failed to migrate legacy messages.yml: " + e.getMessage());
            }
        }

        repairCorruptedLanguageFile(language, targetLanguageFile);
        ensureLanguageFileHasAllKeys(language, targetLanguageFile);
        messageProvider = MessageProvider.load(messagesDir, language, plugin.getLogger());
    }

    private void repairCorruptedLanguageFile(String language, File languageFile) {
        if (languageFile == null || !languageFile.exists()) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(languageFile.toPath(), StandardCharsets.UTF_8);
            boolean javaSnippetDetected = lines.stream().anyMatch(line ->
                    line.contains("package com.skyblockexp.ezrtp.message")
                            || line.contains("case TELEPORTING ->"));
            if (!javaSnippetDetected) {
                return;
            }

            File backup = new File(languageFile.getParentFile(), language.toLowerCase() + ".yml.bak");
            Files.copy(languageFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            if ("en".equalsIgnoreCase(language)) {
                plugin.saveResource("messages/en.yml", true);
                plugin.getLogger().warning("Detected corrupted messages/en.yml. "
                        + "Backed up to messages/en.yml.bak and restored defaults.");
            } else {
                YamlConfiguration restored = new YamlConfiguration();
                for (MessageKey key : MessageKey.values()) {
                    restored.set(key.getKey(), MessageProvider.createDefault("en", plugin.getLogger()).getMessage(key));
                }
                restored.save(languageFile);
                plugin.getLogger().warning("Detected corrupted language file '" + language + ".yml'. "
                        + "Backed up to " + backup.getName() + " and restored default keys in English.");
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to verify/repair messages/" + language + ".yml: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Failed to restore bundled messages/" + language + ".yml: " + ex.getMessage());
        }
    }

    private void ensureLanguageFileHasAllKeys(String language, File languageFile) {
        if (languageFile == null || !languageFile.exists()) {
            return;
        }
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(languageFile);
            ConfigurationSection section = config.isConfigurationSection("messages")
                    ? config.getConfigurationSection("messages")
                    : config;
            if (section == null) {
                section = config;
            }
            int added = 0;
            MessageProvider defaults = MessageProvider.createDefault("en", plugin.getLogger());
            for (MessageKey key : MessageKey.values()) {
                if (!section.contains(key.getKey())) {
                    section.set(key.getKey(), defaults.getMessage(key));
                    added++;
                }
            }
            if (added > 0) {
                config.save(languageFile);
                plugin.getLogger().info("Added " + added + " missing message keys to messages/" + language + ".yml");
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to backfill missing keys for messages/" + language + ".yml: " + ex.getMessage());
        }
    }

    private void saveResourceIfMissing(String resource) {
        File target = new File(plugin.getDataFolder(), resource);
        if (target.exists()) {
            return;
        }
        try {
            plugin.saveResource(resource, false);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Unable to save default resource '" + resource + "': " + exception.getMessage());
        }
    }

    private FileConfiguration loadExternalConfiguration(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            saveResourceIfMissing(fileName);
        }
        if (!file.exists()) {
            return null;
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private ConfigurationSection selectSection(FileConfiguration configuration, String nestedKey,
                                               ConfigurationSection fallback) {
        ConfigurationSection section = null;
        if (configuration != null) {
            if (nestedKey != null && configuration.isConfigurationSection(nestedKey)) {
                section = configuration.getConfigurationSection(nestedKey);
            }
            if (section == null && !configuration.getKeys(false).isEmpty()) {
                section = configuration;
            }
        }
        if (section != null && !section.getKeys(true).isEmpty()) {
            return section;
        }
        if (fallback != null && !fallback.getKeys(true).isEmpty()) {
            return fallback;
        }
        return null;
    }

    private ConfigurationSection buildEffectiveBaseConfiguration(ConfigurationSection base,
                                                                 ConfigurationSection rtpConfiguration,
                                                                 ConfigurationSection limitsConfiguration,
                                                                 ConfigurationSection storageConfiguration) {
        MemoryConfiguration merged = new MemoryConfiguration();
        copySection(base, merged);
        copySection(rtpConfiguration, merged);
        copySection(limitsConfiguration, merged);
        copySection(storageConfiguration, merged);
        return merged;
    }

    private void copySection(ConfigurationSection source, ConfigurationSection target) {
        if (source == null || target == null) {
            return;
        }
        for (String key : source.getKeys(false)) {
            Object value = source.get(key);
            if (value instanceof ConfigurationSection sectionValue) {
                ConfigurationSection existing = target.getConfigurationSection(key);
                if (existing == null) {
                    existing = target.createSection(key);
                }
                copySection(sectionValue, existing);
            } else {
                target.set(key, value);
            }
        }
    }
}
