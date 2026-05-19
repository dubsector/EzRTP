## Advanced Random Teleport Plugin


Production-ready random teleport plugin for modern Minecraft servers.

Latest release and docs: https://github.com/ez-plugins/EzRTP  
Java 17+, packaged for Bukkit/Paper/Spigot/Purpur/Folia.

[![Documentation](https://i.ibb.co/dskCvgLP/documentation-button-1.png)](https://github.com/ez-plugins/EzRTP/blob/main/README.md)
[![Support](https://i.ibb.co/Wpy2w1cH/support-button-1.png)](https://discord.gg/yWP95XfmBS)

[Modrinth](https://modrinth.com/plugin/ezplugins-ezrtp) • [SpigotMC](https://www.spigotmc.org/resources/1-19-1-21-%E2%AC%85%EF%B8%8F-ezrtp-%E2%9E%A1%EF%B8%8F-highly-configurable-rtp-plugin-for-minecraft-servers.129828/) • [Hangar](https://hangar.papermc.io/EzPlugins/EzRTP) • [Discord](https://discord.gg/yWP95XfmBS)

---

## Feature Summary

- Safe random teleport with configurable unsafe-block filtering and recovery behavior.
- Biome include/exclude filtering, pre-cache, and rare-biome optimization (weighted search + hotspot tracking).
- Search pattern control: `random`, `circle`, `square`, `triangle`, `diamond`.
- Queue controls and cooldown/usage limits per world and per group.
- GUI destination selector with per-destination permissions.
- Heatmap tools (`/rtp heatmap`, `/rtp heatmap save`) and simulated samples (`/rtp fake`).
- Named centers (`/rtp addcenter`, `/rtp <center-name>`) and center editing (`/rtp setcenter`).
- Optional network selector entries via `network.yml`.
- Optional Chunky pre-generation command (`/rtp pregenerate [world] [radius]`).
- TeamsAPI-powered faction claim RTP selection via `/rtp faction` and `/f rtp`.
- Optional performance and unsafe-location monitoring exports.
- Folia-compatible runtime behavior for modern server scheduling models.

### Feature Preview

#### Biome Statistics Dashboard

![Biome Statistics](https://i.ibb.co/YBy2z8vZ/image.png)

#### Heatmap Visualization

![RTP Heatmap](https://i.ibb.co/wZWfpn7B/image.png)

#### World Selector GUI

![World Selector GUI](https://i.ibb.co/fGCZwJFL/image.png)

---

## Subcommands

### Main Commands

| Command | Description | Permission |
|---|---|---|
| `/rtp` | Default RTP flow (GUI or direct teleport based on setup) | `ezrtp.use` |
| `/rtp faction` | Open claim GUI and RTP around selected faction claim center | `ezrtp.use` |
| `/forcertp <player> [world]` | Force RTP for a target player | `ezrtp.forcertp` |

### `/rtp` Subcommands

| Subcommand | Usage | Permission | Notes |
|---|---|---|---|
| `reload` | `/rtp reload` | `ezrtp.reload` | Reload configuration |
| `stats` | `/rtp stats` | `ezrtp.stats` | Overall RTP metrics |
| `stats biomes` | `/rtp stats biomes [page]` | `ezrtp.stats` | Biome activity + paging |
| `stats rare-biomes` | `/rtp stats rare-biomes` | `ezrtp.stats` | Rare-biome optimization stats |
| `stats performance` | `/rtp stats performance` | `ezrtp.stats` | Percentile/timing metrics |
| `unsafe-stats` | `/rtp unsafe-stats` | `ezrtp.stats` | Unsafe rejection breakdown |
| `heatmap` | `/rtp heatmap [biome]` | `ezrtp.heatmap` | Gives in-game heatmap map |
| `heatmap save` | `/rtp heatmap save` | `ezrtp.heatmap` | Saves heatmap PNG to plugin folder |
| `fake` | `/rtp fake <amount|clear> [world]` | `ezrtp.heatmap.fake` | Add/clear simulated heatmap samples |
| `setcenter` | `/rtp setcenter <x> <z>` or `/rtp setcenter <world> <x> <z>` | `ezrtp.setcenter` | Updates world center and reloads |
| `addcenter` | `/rtp addcenter <name>` | `ezrtp.setcenter` | Stores named center at player location |
| `pregenerate` | `/rtp pregenerate [world] [radius]` | `ezrtp.pregenerate` | Starts Chunky-assisted warmup tasks |

### Extra RTP Routing

- `/rtp <center-name>`: teleports using `centers.named.<name>` from `rtp.yml`.
- `/rtp <region-id>`: WorldGuard region-based RTP when `worldguard.region-command.enabled: true`.

---

## Permissions

### Declared in `plugin.yml`

| Permission | Default | Purpose |
|---|---|---|
| `ezrtp.use` | `true` | Use `/rtp` |
| `ezrtp.reload` | `op` | Use `/rtp reload` |
| `ezrtp.stats` | `op` | Use stats/unsafe-stats |
| `ezrtp.heatmap` | `op` | Use heatmap generation/export |
| `ezrtp.heatmap.fake` | `op` | Use `/rtp fake` |
| `ezrtp.queue.bypass` | `op` | Bypass queue |
| `ezrtp.forcertp` | `op` | Use `/forcertp` |

### Used by Runtime/Config

| Permission | Source | Purpose |
|---|---|---|
| `ezrtp.setcenter` | command code | Access `setcenter` and `addcenter` |
| `ezrtp.pregenerate` | command code | Access `/rtp pregenerate` |
| `ezrtp.bypass.cooldown` | `limits.yml` defaults | Cooldown bypass |
| `ezrtp.bypass.limit` | `limits.yml` defaults | Daily/weekly limit bypass |
| `ezrtp.gui.<name>` | `gui.yml` entries | Per-destination GUI access |

---

## Configuration Files

EzRTP now splits configuration by concern.

| File | Scope | Example Keys |
|---|---|---|
| `config.yml` | Core plugin/global options | `message-prefix`, `language`, `enable-bstats`, `worldguard.region-command.*` |
| `rtp.yml` | RTP behavior and safety | `search-pattern`, `radius.*`, `countdown.*`, `unsafe-blocks`, `biomes.*`, `chunky-integration.*` |
| `limits.yml` | Cooldowns/usage/cost profiles | `rtp-limits.default`, `rtp-limits.worlds`, `allow-gui-during-cooldown` |
| `storage.yml` | Limits storage backend | `rtp-limits.storage`, `rtp-limits.mysql.*` |
| `gui.yml` | GUI layout and destinations | `enabled`, `rows`, `worlds.*.icon`, `disable-cache-filtering`, `rare_biomes.*` |
| `queue.yml` | Queue throttling | `enabled`, `max-size`, `start-delay-ticks`, `interval-ticks` |
| `network.yml` | Proxy/server selector | `enabled`, `lobby`, `servers.<name>.*` |
| `force-rtp.yml` | `/forcertp` behavior | `default-world`, `bypass.cooldown`, `bypass.permission`, `bypass.safety` |
| `performance.yml` | Optional perf monitoring | `performance.monitoring.enabled`, `warnings.*`, `metrics.*`, `percentiles.*` |
| `unsafe-location-monitoring.yml` | Optional unsafe tracking | `unsafe-location-monitoring.monitoring.enabled`, `logging.*`, `metrics.*` |
| `messages/en.yml` | User-facing text templates | `teleport-*`, `queue-*`, `forcertp-*`, `heatmap-*`, `fake-*`, `stats-*` |

### Current Core Defaults (selected)

```yaml
# config.yml
message-prefix: "&7[&bEzRTP&7] &r"
language: en
enable-bstats: true
worldguard:
  region-command:
    enabled: false
    autocomplete: false
```

```yaml
# rtp.yml
search-pattern: random
max-attempts: 16
countdown-seconds: 5
heatmap:
  enabled: false
biomes:
  enabled: true
  pre-cache:
    enabled: false
  rare-biome-optimization:
    enabled: false
chunky-integration:
  enabled: false
```

```yaml
# limits.yml
rtp-limits:
  default:
    cooldown-seconds: 300
    daily-limit: 10
    weekly-limit: 50
  bypass-permissions:
    - ezrtp.bypass.cooldown
    - ezrtp.bypass.limit
  allow-gui-during-cooldown: true
```

```yaml
# storage.yml
rtp-limits:
  storage: yaml # yaml or mysql
```

---

## Requirements

- Java 17 or newer.
- Bukkit, Paper, Purpur, Spigot, or Folia 1.7+.
- Optional: Vault/EzEconomy for pricing.
- Optional: WorldGuard/GriefPrevention for claim-aware behavior.
- Optional: Chunky for pre-generation workflows.

## Support

- Discord: https://discord.gg/yWP95XfmBS
- Project and releases: https://github.com/ez-plugins/EzRTP

[![Try Other EzPlugins](https://i.ibb.co/PzfjNjh0/ezplugins-try-other-plugins.png)](https://modrinth.com/collection/Q98Ov6dA)
