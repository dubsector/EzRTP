package com.skyblockexp.ezrtp.gui;

import com.skyblockexp.ezrtp.EzRtpPlugin;
import com.skyblockexp.ezrtp.config.EzRtpConfiguration;
import com.skyblockexp.ezrtp.config.RandomTeleportSettings;
import com.skyblockexp.ezrtp.config.gui.FactionGuiSettings;
import com.skyblockexp.ezrtp.integration.ClaimOwnerResolver;
import com.skyblockexp.ezrtp.message.MessageKey;
import com.skyblockexp.ezrtp.message.MessageProvider;
import com.skyblockexp.ezrtp.platform.PlatformRuntimeRegistry;
import com.skyblockexp.ezrtp.storage.RtpUsageStorage;
import com.skyblockexp.ezrtp.teleport.RandomTeleportService;
import com.skyblockexp.ezrtp.teleport.TeleportReason;
import com.skyblockexp.ezrtp.util.MessageUtil;
import com.skyblockexp.teamsapi.api.TeamsAPI;
import com.skyblockexp.teamsapi.api.TeamsClaimService;
import com.skyblockexp.teamsapi.api.TeamsService;
import com.skyblockexp.teamsapi.model.Team;
import com.skyblockexp.teamsapi.model.TeamClaim;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class FactionClaimSelectionGuiManager implements Listener {

    private final EzRtpPlugin plugin;
    private final Supplier<RandomTeleportService> teleportServiceSupplier;
    private final Supplier<EzRtpConfiguration> configurationSupplier;
    private final Supplier<MessageProvider> messageSupplier;
    private final RtpUsageStorage usageStorage;
    private final Map<UUID, Session> sessions = new HashMap<>();

    public FactionClaimSelectionGuiManager(EzRtpPlugin plugin,
                                           Supplier<RandomTeleportService> teleportServiceSupplier,
                                           Supplier<EzRtpConfiguration> configurationSupplier,
                                           Supplier<MessageProvider> messageSupplier,
                                           RtpUsageStorage usageStorage) {
        this.plugin = plugin;
        this.teleportServiceSupplier = teleportServiceSupplier;
        this.configurationSupplier = configurationSupplier;
        this.messageSupplier = messageSupplier;
        this.usageStorage = usageStorage;
    }

    public boolean openSelection(Player player) {
        if (player == null || !TeamsAPI.isAvailable() || !TeamsAPI.isClaimAvailable()) {
            MessageUtil.send(player, "<red>TeamsAPI is not available on this server.</red>");
            return true;
        }

        TeamsService teamsService = TeamsAPI.getService();
        TeamsClaimService claimService = TeamsAPI.getClaimService();
        if (teamsService == null || claimService == null) {
            MessageUtil.send(player, "<red>TeamsAPI services are not available right now.</red>");
            return true;
        }

        Team team = teamsService.getPlayerTeam(player.getUniqueId()).orElse(null);
        if (team == null) {
            MessageUtil.send(player, "<red>You are not in a faction/team.</red>");
            return true;
        }

        List<TeamClaim> claims = new ArrayList<>(claimService.getTeamClaims(team.getId()));
        if (claims.isEmpty()) {
            MessageUtil.send(player, "<red>Your faction/team has no claims.</red>");
            return true;
        }
        claims.sort(Comparator
                .comparing(TeamClaim::getWorldName)
                .thenComparingInt(TeamClaim::getChunkX)
                .thenComparingInt(TeamClaim::getChunkZ));

        EzRtpConfiguration configuration = configurationSupplier.get();
        if (configuration == null || !configuration.getFactionGuiSettings().enabled()) {
            MessageUtil.send(player, "<red>Faction RTP GUI is disabled.</red>");
            return true;
        }
        openPage(player, new Session(claims, team.getDisplayName(), team.getId(), teamsService, 0));
        return true;
    }

    public void closeAll() {
        for (UUID playerId : new ArrayList<>(sessions.keySet())) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.closeInventory();
            }
        }
        sessions.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Session session = sessions.get(player.getUniqueId());
        if (session == null || !event.getView().getTopInventory().equals(session.inventory)) {
            return;
        }

        event.setCancelled(true);
        event.setResult(Event.Result.DENY);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= session.settings.size()) {
            return;
        }

        if (slot == session.settings.previousSlot() && session.page > 0) {
            openPage(player, new Session(session.claims, session.teamDisplayName, session.teamId, session.teamsService, session.page - 1));
            return;
        }
        if (slot == session.settings.nextSlot() && (session.page + 1) * session.claimsPerPage() < session.claims.size()) {
            openPage(player, new Session(session.claims, session.teamDisplayName, session.teamId, session.teamsService, session.page + 1));
            return;
        }
        if (slot >= session.claimsPerPage()) {
            return;
        }

        int claimIndex = session.page * session.claimsPerPage() + slot;
        if (claimIndex < 0 || claimIndex >= session.claims.size()) {
            return;
        }

        TeamClaim claim = session.claims.get(claimIndex);
        teleportToClaimCenter(player, claim);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Session session = sessions.get(player.getUniqueId());
        if (session == null || !event.getView().getTopInventory().equals(session.inventory)) {
            return;
        }
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Session session = sessions.get(player.getUniqueId());
        if (session != null && session.inventory.equals(event.getInventory())) {
            sessions.remove(player.getUniqueId());
        }
    }

    private void openPage(Player player, Session session) {
        EzRtpConfiguration configuration = configurationSupplier.get();
        FactionGuiSettings settings = configuration != null ? configuration.getFactionGuiSettings() : FactionGuiSettings.fromConfiguration(null);
        session.settings = settings;
        int page = session.page;
        int claimsPerPage = session.claimsPerPage();
        int totalPages = Math.max(1, (int) Math.ceil((double) session.claims.size() / claimsPerPage));
        Inventory inventory = org.bukkit.Bukkit.createInventory(
                null,
                settings.size(),
                format(settings.title(), page + 1, totalPages));

        int start = page * claimsPerPage;
        int end = Math.min(start + claimsPerPage, session.claims.size());
        for (int i = start; i < end; i++) {
            TeamClaim claim = session.claims.get(i);
            inventory.setItem(i - start, createClaimItem(claim, i + 1, settings, session.teamsService));
        }

        if (page > 0) {
            inventory.setItem(settings.previousSlot(), createNavItem(Material.ARROW, settings.previousName()));
        }
        if (end < session.claims.size()) {
            inventory.setItem(settings.nextSlot(), createNavItem(Material.ARROW, settings.nextName()));
        }

        session.inventory = inventory;
        sessions.put(player.getUniqueId(), session);
        player.openInventory(inventory);
    }

    private void teleportToClaimCenter(Player player, TeamClaim claim) {
        World claimWorld = plugin.getServer().getWorld(claim.getWorldName());
        if (claimWorld == null) {
            MessageUtil.send(player, "<red>Claim world '<white>" + claim.getWorldName() + "</white>' is not loaded.</red>");
            return;
        }

        EzRtpConfiguration configuration = configurationSupplier.get();
        RandomTeleportService service = teleportServiceSupplier.get();
        MessageProvider messages = messageSupplier.get();
        if (configuration == null || service == null || messages == null) {
            MessageUtil.send(player, "<red>EzRTP is not fully initialized.</red>");
            return;
        }

        RandomTeleportSettings baseSettings = configuration.getSettingsForWorld(claim.getWorldName());
        if (baseSettings == null) {
            MessageUtil.send(player, "<red>No RTP settings found for world '<white>" + claim.getWorldName() + "</white>'.</red>");
            return;
        }

        String worldName = claim.getWorldName();
        String group = configuration.resolveGroup(player, worldName);
        boolean bypass = player.isOp();
        if (!bypass) {
            for (String perm : configuration.getBypassPermissions()) {
                if (player.hasPermission(perm)) {
                    bypass = true;
                    break;
                }
            }
        }
        if (!bypass) {
            com.skyblockexp.ezrtp.config.teleport.RtpLimitSettings limit = configuration.getLimitSettings(worldName, group);
            long now = System.currentTimeMillis();
            long lastRtp = usageStorage.getLastRtpTime(player.getUniqueId(), worldName);
            int daily = usageStorage.getUsageCount(player.getUniqueId(), worldName, "daily");
            int weekly = usageStorage.getUsageCount(player.getUniqueId(), worldName, "weekly");
            if (limit.getCooldownSeconds() > 0 && lastRtp > 0 && (now - lastRtp) < limit.getCooldownSeconds() * 1000L) {
                long wait = (limit.getCooldownSeconds() * 1000L - (now - lastRtp)) / 1000L;
                com.skyblockexp.ezrtp.util.PluginMessageHelper.sendCooldownMessage(player, plugin, configuration, wait);
                return;
            }
            if (!limit.isDisableDailyLimit() && limit.getDailyLimit() > 0 && daily >= limit.getDailyLimit()) {
                MessageUtil.send(player, MessageUtil.parseMiniMessage(messages.getMessage(MessageKey.LIMIT_DAILY)));
                return;
            }
            if (!limit.isDisableDailyLimit() && limit.getWeeklyLimit() > 0 && weekly >= limit.getWeeklyLimit()) {
                MessageUtil.send(player, MessageUtil.parseMiniMessage(messages.getMessage(MessageKey.LIMIT_WEEKLY)));
                return;
            }
        }

        MemoryConfiguration override = new MemoryConfiguration();
        override.set("world", worldName);
        override.set("center.x", claim.getChunkX() * 16 + 8);
        override.set("center.z", claim.getChunkZ() * 16 + 8);
        RandomTeleportSettings claimCentered = RandomTeleportSettings.fromConfiguration(override, plugin.getLogger(), baseSettings);

        player.closeInventory();
        service.teleportPlayer(player, claimCentered, TeleportReason.COMMAND, success -> {
            if (success) {
                usageStorage.setLastRtpTime(player.getUniqueId(), worldName, System.currentTimeMillis());
                usageStorage.incrementUsage(player.getUniqueId(), worldName, "daily");
                usageStorage.incrementUsage(player.getUniqueId(), worldName, "weekly");
                PlatformRuntimeRegistry.get().scheduler().executeAsync(usageStorage::save);
            }
        });
    }

    private ItemStack createClaimItem(TeamClaim claim, int index, FactionGuiSettings settings, TeamsService teamsService) {
        ItemStack item = createClaimIcon(claim, settings, teamsService);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(replaceClaimPlaceholders(settings.claimNameFormat(), claim, index));
            List<String> lore = new ArrayList<>();
            for (String line : settings.claimLore()) {
                lore.add(replaceClaimPlaceholders(line, claim, index));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createClaimIcon(TeamClaim claim, FactionGuiSettings settings, TeamsService teamsService) {
        if (!settings.skullEnabled()) {
            return new ItemStack(settings.fallbackMaterial());
        }
        UUID preferredOwner = ClaimOwnerResolver.resolvePreferredOwnerUuid(teamsService, claim).orElse(null);
        if (preferredOwner == null) {
            return new ItemStack(settings.fallbackMaterial());
        }
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = skull.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(preferredOwner);
            skullMeta.setOwningPlayer(offlinePlayer);
            skull.setItemMeta(skullMeta);
            return skull;
        }
        return new ItemStack(settings.fallbackMaterial());
    }

    private ItemStack createNavItem(Material material, String title) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(title);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String replaceClaimPlaceholders(String text, TeamClaim claim, int index) {
        return text
                .replace("<index>", String.valueOf(index))
                .replace("<world>", claim.getWorldName())
                .replace("<chunk_x>", String.valueOf(claim.getChunkX()))
                .replace("<chunk_z>", String.valueOf(claim.getChunkZ()))
                .replace("<center_x>", String.valueOf(claim.getChunkX() * 16 + 8))
                .replace("<center_z>", String.valueOf(claim.getChunkZ() * 16 + 8));
    }

    private String format(String titleTemplate, int page, int pages) {
        return titleTemplate.replace("<page>", String.valueOf(page)).replace("<pages>", String.valueOf(pages));
    }

    private static final class Session {
        private final List<TeamClaim> claims;
        private final String teamDisplayName;
        private final UUID teamId;
        private final TeamsService teamsService;
        private final int page;
        private FactionGuiSettings settings;
        private Inventory inventory;

        private Session(List<TeamClaim> claims, String teamDisplayName, UUID teamId, TeamsService teamsService, int page) {
            this.claims = claims;
            this.teamDisplayName = teamDisplayName;
            this.teamId = teamId;
            this.teamsService = teamsService;
            this.page = page;
        }

        private int claimsPerPage() {
            int reserved = 9;
            return Math.max(1, settings.size() - reserved);
        }
    }
}
