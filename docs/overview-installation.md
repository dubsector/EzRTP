---
title: Getting Started
nav_order: 2
---

# Getting Started

## What EzRTP does

EzRTP is a random-teleport plugin designed for survival and network servers. It supports:

- `/rtp` player teleports
- `/forcertp` admin-forced teleports
- Optional GUI destination selection
- Queue throttling for high-load servers
- Economy integration (Vault/EzEconomy)
- Claim/region avoidance (WorldGuard/GriefPrevention)
- Biome include/exclude filtering
- Biome pre-cache and rare-biome optimizations
- Network destination entries in RTP GUI
- Statistics and heatmap tooling

## Compatibility and requirements

- Java 17
- Bukkit API level `1.13+`

Optional integrations (soft dependencies):

- Vault
- EzEconomy
- WorldGuard
- GriefPrevention
- PlaceholderAPI
- Chunky
- TeamsAPI
- EzCountdown
- CombatLogX / PvPManager / Simple Combat Log

If an integration is missing, EzRTP still works and disables only that integration path.

## Installation

EzRTP uses a modular build. Install the base plugin plus the platform module that
matches your server software.

### Choose the correct module(s)

| Server software | Required jar(s) in `plugins/` | Notes |
| --- | --- | --- |
| Bukkit / CraftBukkit | `EzRTP-<version>.jar` | Base runtime already includes Bukkit-compatible adapters. |
| Spigot | `EzRTP-<version>.jar` | Spigot runs on the Bukkit-compatible base runtime. |
| Paper | `EzRTP-<version>.jar` + `ezrtp-paper-<version>.jar` | Paper module registers Paper-specific runtime adapters. |
| Purpur | `EzRTP-<version>.jar` + `ezrtp-purpur-<version>.jar` | Purpur module adds Purpur-specific behavior and depends on EzRTP. |

> Important: install only the module that matches your platform (Paper **or**
> Purpur), and keep all EzRTP jars on the same version.

### Install steps

1. Stop the server.
2. Place the required jar(s) for your platform in `plugins/`.
3. Start the server once to generate defaults.
4. Stop the server.
5. Edit configuration files in `plugins/EzRTP/`.
6. Start server and run `/rtp reload` (or restart) after changes.

### Building from source (module outputs)

If you build locally with Maven, the modules map as follows:

- `ezrtp-bukkit` → base plugin jar (`EzRTP-<version>.jar`)
- `ezrtp-paper` → Paper platform module (`ezrtp-paper-<version>.jar`)
- `ezrtp-purpur` → Purpur platform module (`ezrtp-purpur-<version>.jar`)
- `ezrtp-spigot` → Spigot-oriented module in the multi-module build

### Module-specific installation docs

Use these pages when you want platform-focused setup details:

- [Bukkit / CraftBukkit module install notes](./modules/installation-bukkit.md)
- [Spigot module install notes](./modules/installation-spigot.md)
- [Paper module install notes](./modules/installation-paper.md)
- [Purpur module install notes](./modules/installation-purpur.md)

## File layout

Generated files map to these defaults:

- `config.yml` (core RTP settings)
- `messages/en.yml` (messages)
- `queue.yml` (queue behavior)
- `gui.yml` (GUI options)
- `network.yml` (network/proxy entries)
- `force-rtp.yml` (forced RTP behavior)
