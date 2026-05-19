package com.skyblockexp.ezrtp.command.subcommands;

import com.skyblockexp.ezrtp.EzRtpPlugin;
import com.skyblockexp.ezrtp.config.EzRtpConfiguration;
import com.skyblockexp.ezrtp.config.RandomTeleportSettings;
import com.skyblockexp.ezrtp.config.teleport.SearchPattern;
import com.skyblockexp.ezrtp.message.MessageKey;
import com.skyblockexp.ezrtp.message.MessageProvider;
import com.skyblockexp.ezrtp.teleport.RandomTeleportService;
import com.skyblockexp.ezrtp.teleport.search.BiomeSearchStrategy;
import com.skyblockexp.ezrtp.teleport.search.CircularSearchStrategy;
import com.skyblockexp.ezrtp.teleport.search.TriangleSearchStrategy;
import com.skyblockexp.ezrtp.teleport.search.DiamondSearchStrategy;
import com.skyblockexp.ezrtp.teleport.search.SquareSearchStrategy;
import com.skyblockexp.ezrtp.teleport.search.UniformSearchStrategy;
import com.skyblockexp.ezrtp.teleport.heatmap.HeatmapSimulationStore;
import com.skyblockexp.teamsapi.api.TeamsAPI;
import com.skyblockexp.teamsapi.api.TeamsClaimService;
import com.skyblockexp.teamsapi.api.TeamsService;
import com.skyblockexp.teamsapi.model.Team;
import com.skyblockexp.teamsapi.model.TeamClaim;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import com.skyblockexp.ezrtp.platform.PlatformRuntimeRegistry;
import com.skyblockexp.ezrtp.util.MessageUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Handles the /rtp fake subcommand for heatmap simulation.
 */
public class FakeSubcommand extends Subcommand {

    private static final int MAX_FAKE_POINTS_PER_COMMAND = 5000;

    private final EzRtpPlugin plugin;
    private final Supplier<RandomTeleportService> teleportServiceSupplier;
    private final Supplier<EzRtpConfiguration> configurationSupplier;
    private final HeatmapSimulationStore heatmapSimulationStore;

    public FakeSubcommand(EzRtpPlugin plugin,
                         Supplier<RandomTeleportService> teleportServiceSupplier,
                         Supplier<EzRtpConfiguration> configurationSupplier,
                         HeatmapSimulationStore heatmapSimulationStore) {
        super("fake", "ezrtp.heatmap.fake");
        this.plugin = plugin;
        this.teleportServiceSupplier = teleportServiceSupplier;
        this.configurationSupplier = configurationSupplier;
        this.heatmapSimulationStore = heatmapSimulationStore;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        handleHeatmapSimulationCommand(sender, args);
        return true;
    }

    @Override
    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!hasPermission(sender)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("clear");
            suggestions.add("<amount>");
            return suggestions;
        }

        if (args.length == 2) {
            if ("claims".equalsIgnoreCase(args[1])) {
                List<String> suggestions = new ArrayList<>();
                for (World world : Bukkit.getWorlds()) {
                    suggestions.add(world.getName());
                }
                return suggestions;
            }
            // Suggest world names
            List<String> suggestions = new ArrayList<>();
            for (World world : Bukkit.getWorlds()) {
                suggestions.add(world.getName());
            }
            return suggestions;
        }

        return Collections.emptyList();
    }

    /**
     * Handles /rtp fake operations for injecting or clearing simulated heatmap points.
     */
    private void handleHeatmapSimulationCommand(CommandSender sender, String[] args) {
        if (!hasPermission(sender)) {
            MessageUtil.send(sender, plugin.getMessageProvider().format(MessageKey.COMMAND_NO_PERMISSION));
            return;
        }

        if (heatmapSimulationStore == null) {
            MessageUtil.send(sender, plugin.getMessageProvider().format(MessageKey.FAKE_SIMULATION_STORE_MISSING));
            return;
        }

        if (args.length < 1) {
            MessageUtil.send(sender, plugin.getMessageProvider().format(MessageKey.FAKE_USAGE));
            return;
        }

        boolean claimsMode = args.length >= 2 && "claims".equalsIgnoreCase(args[1]);
        String worldArgument = claimsMode
                ? (args.length >= 3 ? args[2] : null)
                : (args.length >= 2 ? args[1] : null);
        boolean worldProvided = worldArgument != null && !worldArgument.isBlank();
        World targetWorld = claimsMode ? null : resolveSimulationWorld(sender, worldArgument);
        if (targetWorld == null) {
            if (!claimsMode) {
                return;
            }
        }

        String action = args[0];
        if ("clear".equalsIgnoreCase(action)) {
            int cleared = heatmapSimulationStore.clearWorld(targetWorld.getName());
                MessageUtil.send(sender, plugin.getMessageProvider().format(MessageKey.HEATMAP_SIMULATION_CLEARED, Map.of(
                "count", String.valueOf(cleared),
                "s", cleared == 1 ? "" : "s",
                "world", targetWorld.getName()
            )));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            MessageUtil.send(sender, plugin.getMessageProvider().format(MessageKey.FAKE_INVALID_AMOUNT, Map.of("amount", args[0])));
            return;
        }

        if (amount <= 0) {
            MessageUtil.send(sender, plugin.getMessageProvider().format(MessageKey.FAKE_AMOUNT_NEGATIVE));
            return;
        }

        int commandLimit = Math.min(MAX_FAKE_POINTS_PER_COMMAND, heatmapSimulationStore.getPerWorldCapacity());
        if (amount > commandLimit) {
            MessageUtil.send(sender, plugin.getMessageProvider().format(MessageKey.FAKE_AMOUNT_TOO_LARGE, Map.of("limit", String.valueOf(commandLimit))));
            return;
        }

        if (claimsMode) {
            handleFactionClaimSimulation(sender, amount, worldArgument);
            return;
        }

        EzRtpConfiguration configuration = configurationSupplier.get();
        if (configuration == null) {
            MessageUtil.send(sender, plugin.getMessageProvider().format(MessageKey.FAKE_CONFIG_MISSING));
            return;
        }

        RandomTeleportSettings settings = configuration.getSettingsForWorld(targetWorld.getName());
        if (settings == null) {
            MessageUtil.send(sender, plugin.getMessageProvider().format(MessageKey.FAKE_SETTINGS_MISSING, Map.of("world", targetWorld.getName())));
            return;
        }

        BiomeSearchStrategy strategy = resolveSearchStrategy(settings.getSearchPattern());
        int minRadius = Math.max(0, settings.getMinimumRadius());
        int maxRadius = Math.max(minRadius, resolveMaximumRadius(targetWorld, settings));

        int centerX = settings.getCenterX();
        int centerZ = settings.getCenterZ();
        if (settings.useWorldBorderRadius()) {
            java.util.Optional<Double> radius = com.skyblockexp.ezrtp.util.compat.WorldBorderCompat.getBorderRadius(targetWorld);
            if (radius.isPresent()) {
                int resolved = (int) Math.floor(radius.get());
                maxRadius = Math.max(minRadius, resolved);
            }
        }

        double sampleY = resolveSimulationY(targetWorld, settings);
        // If generating many samples, perform coordinate computation off the main thread
        final int ASYNC_THRESHOLD = 200;
        if (amount >= ASYNC_THRESHOLD) {
            MessageUtil.send(sender, plugin.getMessageProvider().format(MessageKey.FAKE_SIMULATION_HINT));
            // Capture locals into finals for safe use inside lambdas
            final BiomeSearchStrategy strategyFinal = strategy;
            final World targetWorldFinal = targetWorld;
            final int centerXFinal = centerX;
            final int centerZFinal = centerZ;
            final int minRadiusFinal = minRadius;
            final int maxRadiusFinal = maxRadius;
            final RandomTeleportSettings settingsFinal = settings;
            final double sampleYFinal = sampleY;
            final CommandSender senderFinal = sender;

            // Compute integer coordinates asynchronously to avoid blocking server thread
            PlatformRuntimeRegistry.get().scheduler().executeAsync(() -> {
                final java.util.List<int[]> coords = new java.util.ArrayList<>(amount);
                for (int i = 0; i < amount; i++) {
                    int[] c = strategyFinal.generateCandidateCoordinates(targetWorldFinal, centerXFinal, centerZFinal, minRadiusFinal, maxRadiusFinal, settingsFinal.getBiomeInclude(), null);
                    if (c != null && c.length >= 2) coords.add(new int[]{c[0], c[1]});
                }

                // Switch back to main thread to create Location objects and add to store
                PlatformRuntimeRegistry.get().scheduler().executeGlobal(() -> {
                    java.util.List<Location> generatedLocations = new java.util.ArrayList<>(coords.size());
                    for (int[] c : coords) {
                        generatedLocations.add(new Location(targetWorldFinal, c[0], sampleYFinal, c[1]));
                    }
                    int inserted = heatmapSimulationStore.addSamples(targetWorldFinal.getName(), generatedLocations);
                    int totalSimulated = heatmapSimulationStore.getSamples(targetWorldFinal.getName()).size();

                    MessageUtil.send(senderFinal, plugin.getMessageProvider().format(MessageKey.HEATMAP_SIMULATION_ADDED, Map.of(
                        "count", String.valueOf(inserted),
                        "s", inserted == 1 ? "" : "s",
                        "world", targetWorldFinal.getName(),
                        "pattern", settingsFinal.getSearchPattern().getConfigKey()
                    )));
                    MessageUtil.send(senderFinal, plugin.getMessageProvider().format(MessageKey.FAKE_SIMULATION_STATUS, Map.of(
                        "count", String.valueOf(totalSimulated),
                        "s", totalSimulated == 1 ? "" : "s",
                        "capacity", String.valueOf(heatmapSimulationStore.getPerWorldCapacity())
                    )));
                });
            });
            return;
        }

        List<Location> generated = new ArrayList<>(amount);
        for (int i = 0; i < amount; i++) {
            int[] coordinates = strategy.generateCandidateCoordinates(targetWorld, centerX, centerZ, minRadius, maxRadius, settings.getBiomeInclude(), null);
            if (coordinates != null && coordinates.length >= 2) {
                Location loc = new Location(targetWorld, coordinates[0], sampleY, coordinates[1]);
                generated.add(loc);
            }
        }

        int inserted = heatmapSimulationStore.addSamples(targetWorld.getName(), generated);
        int totalSimulated = heatmapSimulationStore.getSamples(targetWorld.getName()).size();

        MessageUtil.send(sender, plugin.getMessageProvider().format(MessageKey.HEATMAP_SIMULATION_ADDED, Map.of(
            "count", String.valueOf(inserted),
            "s", inserted == 1 ? "" : "s",
            "world", targetWorld.getName(),
            "pattern", settings.getSearchPattern().getConfigKey()
        )));
        MessageUtil.send(sender, plugin.getMessageProvider().format(MessageKey.FAKE_SIMULATION_STATUS, Map.of(
            "count", String.valueOf(totalSimulated),
            "s", totalSimulated == 1 ? "" : "s",
            "capacity", String.valueOf(heatmapSimulationStore.getPerWorldCapacity())
        )));
        MessageUtil.send(sender, plugin.getMessageProvider().format(MessageKey.FAKE_SIMULATION_HINT));
    }

    private World resolveSimulationWorld(CommandSender sender, String explicitWorld) {
        if (explicitWorld != null && !explicitWorld.isBlank()) {
            World world = Bukkit.getWorld(explicitWorld);
            if (world == null) {
                MessageUtil.send(sender, plugin.getMessageProvider().format(MessageKey.FAKE_WORLD_MISSING, Map.of("world", explicitWorld)));
                return null;
            }
            return world;
        }
        if (sender instanceof Player player) {
            return player.getWorld();
        }
        MessageUtil.send(sender, plugin.getMessageProvider().format(MessageKey.FAKE_WORLD_REQUIRED_CONSOLE));
        return null;
    }

    private void handleFactionClaimSimulation(CommandSender sender, int amount, String worldFilter) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "<red>Claim-based simulation requires a player context.</red>");
            return;
        }
        if (!TeamsAPI.isAvailable() || !TeamsAPI.isClaimAvailable()) {
            MessageUtil.send(sender, "<red>TeamsAPI claim service is unavailable.</red>");
            return;
        }
        TeamsService teamsService = TeamsAPI.getService();
        TeamsClaimService claimService = TeamsAPI.getClaimService();
        if (teamsService == null || claimService == null) {
            MessageUtil.send(sender, "<red>TeamsAPI services are unavailable right now.</red>");
            return;
        }
        Team team = teamsService.getPlayerTeam(player.getUniqueId()).orElse(null);
        if (team == null) {
            MessageUtil.send(sender, "<red>You are not in a faction/team.</red>");
            return;
        }
        List<TeamClaim> allClaims = new ArrayList<>(claimService.getTeamClaims(team.getId()));
        if (worldFilter != null && !worldFilter.isBlank()) {
            allClaims.removeIf(c -> !worldFilter.equalsIgnoreCase(c.getWorldName()));
        }
        if (allClaims.isEmpty()) {
            MessageUtil.send(sender, "<red>Your faction/team has no claims for this simulation scope.</red>");
            return;
        }

        Random random = new Random();
        List<Location> generated = new ArrayList<>(amount);
        int invalidWorldClaims = 0;
        for (int i = 0; i < amount; i++) {
            TeamClaim claim = allClaims.get(random.nextInt(allClaims.size()));
            World world = Bukkit.getWorld(claim.getWorldName());
            if (world == null) {
                invalidWorldClaims++;
                continue;
            }
            int chunkBaseX = claim.getChunkX() * 16;
            int chunkBaseZ = claim.getChunkZ() * 16;
            int x = chunkBaseX + random.nextInt(16);
            int z = chunkBaseZ + random.nextInt(16);
            generated.add(new Location(world, x, resolveSimulationY(world, null), z));
        }
        // Group generated locations per world and save into existing per-world storage buckets.
        Map<String, List<Location>> byWorld = new java.util.HashMap<>();
        for (Location location : generated) {
            byWorld.computeIfAbsent(location.getWorld().getName(), ignored -> new ArrayList<>()).add(location);
        }
        int insertedTotal = 0;
        for (Map.Entry<String, List<Location>> entry : byWorld.entrySet()) {
            insertedTotal += heatmapSimulationStore.addSamples(entry.getKey(), entry.getValue());
        }
        MessageUtil.send(sender, "<green>Added <white>" + insertedTotal + "</white> simulated RTP points on faction claims.</green>");
        MessageUtil.send(sender, "<gray>Faction claim chunks used: <white>" + allClaims.size() + "</white></gray>");
        if (invalidWorldClaims > 0) {
            MessageUtil.send(sender, "<yellow>Skipped <white>" + invalidWorldClaims + "</white> samples due to unloaded claim worlds.</yellow>");
        }
    }

    private BiomeSearchStrategy resolveSearchStrategy(SearchPattern pattern) {
        if (pattern == null) {
            return new UniformSearchStrategy();
        }
        return switch (pattern) {
            case CIRCLE -> new CircularSearchStrategy();
            case DIAMOND -> new DiamondSearchStrategy();
            case TRIANGLE -> new TriangleSearchStrategy();
            case SQUARE -> new SquareSearchStrategy();
            case RANDOM -> new UniformSearchStrategy();
        };
    }

    private int resolveMaximumRadius(World world, RandomTeleportSettings settings) {
        if (!settings.useWorldBorderRadius()) {
            return settings.getMaximumRadius();
        }
        java.util.Optional<Double> radius = com.skyblockexp.ezrtp.util.compat.WorldBorderCompat.getBorderRadius(world);
        if (radius.isPresent()) {
            int resolved = (int) Math.floor(radius.get());
            return Math.max(settings.getMinimumRadius(), resolved);
        }
        return settings.getMaximumRadius();
    }

    private double resolveSimulationY(World world, RandomTeleportSettings settings) {
        if (settings == null) {
            Location spawn = world.getSpawnLocation();
            return spawn != null ? spawn.getY() : 64.0D;
        }
        if (settings.getMaxY() != null) {
            return (settings.getMinY() + settings.getMaxY()) / 2.0D;
        }
        if (settings.getMinY() != null) {
            return settings.getMinY() + 10.0D;
        }
        Location spawn = world.getSpawnLocation();
        return spawn != null ? spawn.getY() : 64.0D;
    }
}
