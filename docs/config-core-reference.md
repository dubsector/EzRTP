# EzRTP Core Config Reference

This file documents `config.yml` and `force-rtp.yml`.

## `config.yml`

### Top-level

- `debug-rejection-logging`
- `message-prefix`
- `language`

### `rtp`

- `enable_fallback_to_cache`
- `human-readable-cooldown`

### Base search controls

- `min-y`, `max-y`
- `world`
- `center.x`, `center.z`
- `radius.min`, `radius.max`
- `search-pattern` (`random|circle|square|triangle|diamond`)
- `max-attempts`
- `cost`

### Countdown

- `countdown-seconds`
- `countdown.seconds`
- `countdown.bossbar.enabled`
- `countdown.bossbar.title`
- `countdown.bossbar.color`
- `countdown.bossbar.style`
- `countdown.particles.enabled`
- `countdown.particles.type`
- `countdown.particles.points`
- `countdown.particles.radius`
- `countdown.particles.height-offset`
- `countdown.particles.extra`
- `countdown.particles.force`
- `countdown.particles.secondary-particle`
- `countdown.particles.secondary-count`
- `countdown.particles.secondary-offset`
- `countdown.chat-messages`

### EzCountdown integration

- `countdown.ezcountdown.enabled`
- `countdown.ezcountdown.display-types`
- `countdown.ezcountdown.format`

### Safety

- `unsafe-blocks`
- `safety.water.place-block-on-surface`
- `safety.water.material`
- `safety.recovery.enabled`
- `safety.recovery.max-vertical-adjust`

### Chunk loading

- `chunk-loading.enabled`
- `chunk-loading.interval-ticks`
- `chunk-loading.max-chunks-per-tick`

### Chunky integration

- `chunky-integration.enabled`
- `chunky-integration.auto-pregenerate`
- `chunky-integration.shape`
- `chunky-integration.pattern`
- `chunky-integration.memory-safety.enabled`
- `chunky-integration.memory-safety.min-free-memory-mb`
- `chunky-integration.memory-safety.max-coordinator-entries`
- `chunky-integration.memory-safety.low-memory-retention-minutes`

### Biomes

- `biomes.include`
- `biomes.exclude`

#### `biomes.pre-cache`

- `enabled`
- `auto-enable-for-filters`
- `max-per-biome`
- `warmup-size`
- `expiration-minutes`
- `refill-interval-minutes`
- `refill-batch-size`

#### `biomes.rare-biome-optimization`

- `enabled`
- `rare-biomes`
- `use-weighted-search`
- `enable-hotspot-tracking`
- `enable-background-scanning`
- `max-hotspots-per-biome`
- `hotspot-scan-interval-minutes`
- `use-chunk-load-queue`
- `chunk-load-interval-ticks`
- `max-chunks-per-tick`
- `auto-enable-for-filters`

#### `biomes.search`

- `max-wait-seconds`
- `max-wait-seconds-rare`
- `max-biome-rejections`
- `max-biome-rejections-rare`
- `max-chunk-loads`
- `max-chunk-loads-rare`
- `min-biome-attempts`
- `failover-mode` (`CACHE|GENERIC|ABORT`)

### Protection

- `protection.avoid-claims`
- `protection.providers`

### WorldGuard region command

- `worldguard.region-command.enabled`
- `worldguard.region-command.autocomplete`

### Success particles

- `particles.enabled`
- `particles.type`
- `particles.count`
- `particles.offset.x`
- `particles.offset.y`
- `particles.offset.z`
- `particles.extra`
- `particles.force`

### Join teleport

- `on-join.enabled`
- `on-join.only-first-join`
- `on-join.bypass-permission`
- `on-join.delay-ticks`

### Limits and cooldowns

- `rtp-limits.default.cooldown-seconds`
- `rtp-limits.default.daily-limit`
- `rtp-limits.default.weekly-limit`
- `rtp-limits.default.cost`
- `rtp-limits.worlds.<world>.default.*`
- `rtp-limits.worlds.<world>.group.<name>.*`

## `force-rtp.yml`

- `default-world`
- `bypass.cooldown`
- `bypass.permission`
- `bypass.safety`
