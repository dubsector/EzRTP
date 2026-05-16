# Changelog

All notable changes to EzRTP are documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Release tags use the `v` prefix (e.g. `v3.0.2`).

---

## [Unreleased]

### Added

### Changed

### Fixed

### Removed

---

## [3.2.3] - 2026-05-16

### Fixed

- **Folia teleport crash** (`UnsupportedOperationException: Must use teleportAsync while in
  region threading`): `BukkitPlatformScheduler` now overrides `teleportAsync` and, when running
  on Folia (`regionizedRuntime` capability), calls `player.teleportAsync(Location)` via
  reflection instead of the forbidden synchronous `player.teleport()`. The Bukkit module was
  the only scheduler that still used the synchronous fallback from the interface default;
  `PaperPlatformScheduler` already called `teleportAsync` directly.

---

## [3.2.2] - 2026-05-12

### Fixed

- `BukkitPlatformScheduler` now handles Folia servers correctly. Previously, running EzRTP on
  Folia without the Paper runtime module caused an `UnsupportedOperationException` during plugin
  enable because the Bukkit `CraftScheduler` rejects all synchronous task scheduling on Folia.
  The scheduler now detects `regionizedRuntime` capabilities and routes all scheduling calls
  through Folia's `GlobalRegionScheduler` / `RegionScheduler` via reflection, matching the
  behaviour already present in `PaperPlatformScheduler`.

---

## [3.2.1] - 2026-05-11

### Fixed

- `PerformanceSnapshot#fmt` now passes `Locale.US` to `String#format`, ensuring decimal separators are always locale-independent (dots, not commas).

### Changed

- Dependency scope for `ezrtp-common` in the `ezrtp-paper` and `ezrtp-purpur` modules changed to `provided`; the common module is bundled by the main plugin JAR and must not be re-included by platform modules.

---

## [3.2.0] - 2026-05-11

### Added

- **TeamsAPI claim avoidance**: EzRTP now integrates with [TeamsAPI](https://modrinth.com/plugin/teams-api) to skip chunk-claimed areas during RTP destination search.
  - New protection provider id: `teamsapi`. Works alongside the existing `worldguard` and `griefprevention` providers.
  - Enabled automatically when TeamsAPI (with a compatible claim provider) is installed. Silently skipped when absent, no errors, no configuration changes required.
  - `teamsapi` added to the default `protection.providers` list in `rtp.yml`.
  - `TeamsAPI` added to `softdepend` in `plugin.yml`.
  - Claim availability is evaluated dynamically per check, so a claim plugin that loads after EzRTP is picked up without a reload.

---

## [3.1.0] - 2026-05-09

### Added

- **Message suppression via config** (`config.yml`):
  - `messages.suppress-player`: when `true`, silences all teleport-related messages to players globally (searching, countdown, queue position, success, failure, cost).
  - `messages.suppress-console`: when `true`, silences the executor notification that `/forcertp` sends to the command sender globally.
- **`--skip-message` command flag**: can be appended to `/rtp`, `/rtp <center|region>`, `/forcertp <player> [world]`, and `/rtp forcertp <player> [world]` to suppress both player-facing and executor messages for that single invocation. Tab-completion suggests the flag.
- **`BiomeCompat` utility** (`ezrtp-common`): reflection-based `safeName(Biome)` and `safeValueOf(String)` helpers that work correctly whether `org.bukkit.block.Biome` is an enum (Spigot/Bukkit ≤ Paper 25) or an interface (Paper 26+), preventing `IncompatibleClassChangeError` at runtime.
- **Movement-cancel during countdown**: if a player moves too far from their starting position while a countdown is running, the teleport is cancelled.
  - `countdown.cancel-on-move` (default `true`) — enable or disable the feature.
  - `countdown.cancel-distance` (default `2.0`) — distance in blocks that triggers cancellation.
  - `countdown.warn-distance` (default `1.0`) — distance in blocks that sends a one-time warning before cancellation. Set to `0` to disable the warning.
  - Two new message keys: `countdown-move-warn` and `countdown-move-cancel` (configurable in `messages/en.yml`).

### Changed

- **Minecraft version support expanded to 1.13+**: `api-version` in `plugin.yml` lowered from `1.21` to `1.13`; plugin will now load on any server from MC 1.13 onwards.
- **Java 17 output bytecode**: `maven.compiler.release` changed from `25` to `17` so the built JARs run on Java 17+ hosts. The build toolchain still requires JDK 25 to compile against `paper-api`.
- **Modrinth `game-versions` broadened to `>=1.13`** in release and nightly workflows (was `>=26.1`).
- **`RareBiomeRegistry.getDefaultRareBiomes()`**: replaced a single try/catch wrapping all `Biome.valueOf()` calls with per-biome `BiomeCompat.safeValueOf()` guards, so a biome absent on the running server version (e.g. `MODIFIED_JUNGLE` removed in 1.18, `DEEP_DARK` added in 1.19) no longer silently prevents the remaining biomes from being registered.
- All `biome.name()` call sites replaced with `BiomeCompat.safeName(biome)` across `RareBiomeRegistry`, `GuiSettings`, `BiomeLocationCache`, `StatsSubcommand`, and `HeatmapSubcommand`.

### Fixed

- Stale `<fork>`, `<executable>`, and `<jvm>` references to deleted `java25.javac` / `java25.java` properties removed from root `pom.xml` compiler and Surefire plugin configuration.

---

## [3.0.2] - 2026-05-09

### Added

- Initial changelog entry. See repository history for prior changes.

---
