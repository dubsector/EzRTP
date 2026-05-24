---
title: rtp.yml
nav_order: 2
parent: Config Reference
---

# rtp.yml

The main configuration file for random teleport behaviour — where players land,
how far they can travel, what's considered safe, and optional features like
biome filtering and countdown timers.

---

## Destination bounds

These settings define where a teleport destination can be.

| Key | Default | Description |
| :--- | :--- | :--- |
| `world` | `world` | The world players are teleported into. Use the exact world folder name (e.g. `world_nether`). Set to `auto` to always teleport within the player's current world — handy on multi-world servers. |
| `center.x` / `center.z` | `0` / `0` | The X/Z coordinate used as the centre of the search circle. Defaults to 0,0 (world spawn area). |
| `radius.min` | `500` | Closest a destination can be from the centre, in blocks. |
| `radius.max` | `2000` | Furthest a destination can be from the centre, in blocks. Leave this out to use the world border as the maximum. |
| `min-y` | `54` | Lowest Y level a destination is considered valid at. |
| `max-y` | `320` | Highest Y level a destination is considered valid at. |

```yml
world: world
center:
  x: 0
  z: 0
radius:
  min: 500
  max: 2000
min-y: 54
max-y: 320
```

---

## Search settings

| Key | Default | Description |
| :--- | :--- | :--- |
| `search-pattern` | `random` | The shape used when picking candidate coordinates. See [Search Patterns](search-patterns) for all options. |
| `max-attempts` | `16` | How many candidate locations EzRTP tries before giving up and sending a failure message. Higher values reduce failed teleports but cost slightly more CPU per attempt. |

---

## Cost

```yml
cost: 0.0
```

Economy cost charged per teleport (requires Vault). `0.0` means free. Per-world
and per-group cost overrides go in `limits.yml`.

---

## Countdown

A countdown timer that shows before the teleport happens. Players who move too
far during the countdown will have their teleport cancelled.

| Key | Default | Description |
| :--- | :--- | :--- |
| `countdown-seconds` | `5` | How many seconds to count down. `0` disables the countdown entirely. |
| `countdown.cancel-on-move` | `true` | Cancel the teleport if the player moves beyond `cancel-distance` during the countdown. |
| `countdown.cancel-distance` | `2.0` | Distance in blocks from the starting position that triggers cancellation. |
| `countdown.warn-distance` | `1.0` | Distance in blocks that sends a one-time warning before cancellation. Set to `0` to disable the warning. |
| `countdown.bossbar.enabled` | `true` | Shows a bossbar with the countdown. |
| `countdown.bossbar.title` | *(see below)* | MiniMessage text shown in the bossbar. `<seconds>` is replaced with the remaining time. |
| `countdown.particles.enabled` | `true` | Plays a particle effect around the player during countdown. |
| `countdown.chat-messages` | `false` | Also prints countdown seconds into chat. |

```yml
countdown-seconds: 5
countdown:
  cancel-on-move: true
  cancel-distance: 2.0
  warn-distance: 1.0
  bossbar:
    enabled: true
    title: "<yellow>Teleporting in <white><seconds></white> seconds...</yellow>"
    color: YELLOW
    style: SOLID
  particles:
    enabled: true
    type: ENCHANTMENT_TABLE
    points: 12
    radius: 1.2
  chat-messages: false
```

### EzCountdown display (optional)

When [EzCountdown](../integrations/ezcountdown) is installed, EzRTP can delegate
the countdown display to it for richer, configurable channels.

| Key | Default | Description |
| :--- | :--- | :--- |
| `countdown.ezcountdown.enabled` | `false` | Hand countdown display off to EzCountdown. Falls back to the built-in display when `false` or when EzCountdown is absent. |
| `countdown.ezcountdown.display-types` | `[ACTION_BAR, BOSS_BAR]` | Display channels EzCountdown renders. Accepted values: `ACTION_BAR`, `BOSS_BAR`, `TITLE`, `CHAT`, `SCOREBOARD`, `DIALOG`. |
| `countdown.ezcountdown.format` | `<yellow>Teleporting in <white>{formatted}</white>...</yellow>` | MiniMessage string shown by each active display channel. `{formatted}` is replaced with the remaining time. |

```yml
countdown:
  ezcountdown:
    enabled: false
    display-types:
      - ACTION_BAR
      - BOSS_BAR
    format: "<yellow>Teleporting in <white>{formatted}</white>...</yellow>"
```

---

## Safety

EzRTP will not place players inside water, lava, or other hazardous blocks.
The `unsafe-blocks` list controls what counts as unsafe. You can add or remove
any Bukkit material name.

```yml
unsafe-blocks:
  - WATER
  - LAVA
  - MAGMA_BLOCK
  - POWDER_SNOW
  - FIRE
  - CAMPFIRE
  - SOUL_FIRE
```

### Surface recovery

When the chosen location is mid-air or underground, EzRTP scans up or down to
find solid footing before confirming the destination.

| Key | Default | Description |
| :--- | :--- | :--- |
| `safety.recovery.enabled` | `true` | Enables vertical scanning to find a safe Y level. |
| `safety.recovery.max-vertical-adjust` | `6` | Maximum blocks to move up or down during surface recovery. |
| `safety.recovery.max-surface-scan-depth` | `20` | How far below the candidate to scan for a footing block in normal worlds. Keep this small for performance. |
| `safety.recovery.max-surface-scan-depth-nether` | `128` | Same, but in the Nether. Larger because candidates often start near the Nether roof. |

### Water landing

```yml
safety:
  water:
    place-block-on-surface: true
    material: ICE
```

When `place-block-on-surface: true`, players who land on water get a temporary
block placed under them so they don't drown. `material` sets what block is placed
(default `ICE`).

---

## Chunk loading

Controls how EzRTP loads chunks while searching for destinations. On Paper 1.21+
the async API is used automatically for the best performance.

| Key | Default | Description |
| :--- | :--- | :--- |
| `chunk-loading.use-paper-async-api` | `auto-detect` | `auto-detect` uses Paper's async chunk API when available. `always` forces it. `never` falls back to the legacy throttle on all platforms. |
| `legacy-throttle.enabled` | `true` | Enables the tick-based throttle used on Spigot/Bukkit or older Paper. |
| `legacy-throttle.interval-ticks` | `10` | Ticks between chunk load batches. |
| `legacy-throttle.max-chunks-per-tick` | `1` | Maximum chunks loaded per batch. |

---

## Biomes

Biome filtering lets you restrict (or require) specific biomes for RTP destinations.
The full feature set includes pre-caching and rare-biome optimisation.

| Key | Default | Description |
| :--- | :--- | :--- |
| `biomes.enabled` | `true` | Master switch. Set to `false` to disable all biome features. |
| `biomes.include` | `[]` | If non-empty, only destinations in one of these biomes are accepted. Use Bukkit biome names, e.g. `FOREST`, `PLAINS`. |
| `biomes.exclude` | `[]` | Destinations in any listed biome are rejected. |

### Pre-cache

Pre-caching runs searches in the background and stores valid locations so players
get instant teleports instead of waiting for a search.

```yml
biomes:
  pre-cache:
    enabled: false
    auto-enable-for-filters: true  # automatically enable when include/exclude lists are set
    max-per-biome: 16
    warmup-size: 8
    expiration-minutes: 15
    refill-interval-minutes: 10
    refill-batch-size: 3
```

### Rare biome optimisation

For biomes that are hard to find (e.g. Mushroom Fields, Deep Dark), this tracks
discovered hotspot coordinates to speed up future searches.

```yml
biomes:
  rare-biome-optimization:
    enabled: false
    rare-biomes:
      - MUSHROOM_FIELDS
      - DEEP_DARK
    use-weighted-search: true
    enable-hotspot-tracking: true
    max-hotspots-per-biome: 20
```

### Search budget (performance caps)

These caps prevent a single biome search from blocking the server too long.

| Key | Default | Description |
| :--- | :--- | :--- |
| `biome-filtering.performance-budget.max-total-time-ms` | `0` | Maximum wall-clock search time for normal searches (ms). `0` = auto (100 ms when filters active). |
| `max-total-time-ms-rare` | `8000` | Maximum search time for rare biomes. |
| `max-biome-rejections` | `0` | Maximum number of biome mismatches before giving up. `0` = auto. |
| `fallback-on-timeout` | `cached-location` | What to do when the budget is exhausted: `cached-location`, `generic`, or `abort`. |

---

## Protection (claim avoidance)

Prevents EzRTP from landing players inside protected claims or regions.

```yml
protection:
  avoid-claims: false
  providers:
    - worldguard
    - griefprevention
    - teamsapi
```

Set `avoid-claims: true` to enable. Only providers that are installed will be used.
Supported values: `worldguard`, `griefprevention`, `teamsapi`.

---

## PvP tag integration

Cancels a pending teleport or active countdown when the player enters combat.
Requires one of the supported PvP tag plugins to be installed (all are soft-dependencies):
[CombatLogX](https://www.spigotmc.org/resources/combatlogx.31689/),
[PvPManager](https://modrinth.com/plugin/pvpmanager), or
[Simple Combat Log](https://modrinth.com/plugin/simple-combatlog).

| Key | Default | Description |
| :--- | :--- | :--- |
| `pvp-tag-integration.cancel-countdown-on-pvp-tag` | `true` | Cancel an active countdown the moment the player receives a PvP tag. |
| `pvp-tag-integration.cancel-queued-on-pvp-tag` | `true` | Skip a queued teleport if the player is already in combat when their slot is dispatched. |

```yml
pvp-tag-integration:
  cancel-countdown-on-pvp-tag: true
  cancel-queued-on-pvp-tag: true
```

If no supported PvP tag plugin is installed the section is silently ignored.
See the [PvP Tag integration guide](../integrations/pvp-tag) for setup details.

---

## Particles (arrival effect)

An optional particle burst played at the destination when the player arrives.

```yml
particles:
  enabled: false
  type: PORTAL
  count: 40
  offset:
    x: 0.5
    y: 1.0
    z: 0.5
```

---

## On-join teleport

Automatically teleport new (or returning) players when they join the server.

| Key | Default | Description |
| :--- | :--- | :--- |
| `on-join.enabled` | `false` | Enables automatic RTP on join. |
| `on-join.only-first-join` | `false` | When `true`, only teleports players who have never joined before. |
| `on-join.bypass-permission` | `""` | Players with this permission are **not** teleported on join (e.g. for staff). |
| `on-join.delay-ticks` | `40` | Ticks to wait after joining before teleporting (40 = 2 seconds). |

```yml
on-join:
  enabled: false
  only-first-join: false
  bypass-permission: ""
  delay-ticks: 40
```

---

## Named centers

Named centers let you create multiple RTP zones that players can access with
`/rtp <name>`. Each center can override the world, coordinates, and radius.

```yml
centers:
  named:
    spawn:
      world: world
      center:
        x: 0
        z: 0
    north:
      world: world
      center:
        x: 0
        z: -5000
```

- Add or update a center in-game with `/rtp addcenter <name>`.
- Players use `/rtp <name>` to teleport using that center's settings.

---

## Debug logging

```yml
debug-rejection-logging: false
```

Set to `true` to log every rejected candidate location with the reason why it was
rejected (wrong biome, unsafe block, inside a claim, etc.). Useful when tuning
biome filters or safety settings. Disable on production servers as it generates
a lot of output.
