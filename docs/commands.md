---
title: Commands
nav_order: 3
---

# Commands

## Player command

- `/rtp`
  - Opens GUI when GUI is enabled.
  - Falls back to direct teleport when GUI is disabled or unavailable.
- `/rtp faction`
  - Opens a faction/team claim GUI using TeamsAPI claims.
  - Selecting a claim uses that claim chunk center and then applies normal configured RTP behavior for that world.
  - GUI layout and item rendering are configured in `faction-gui.yml`.
- `/f rtp`
  - TeamsAPI subcommand integration that opens the same faction/team claim GUI as `/rtp faction`.
- `/rtp <centerName>`
  - Teleports using the named center configured under `centers.named` in `rtp.yml`.
  - Named center is applied as a center override only; the world's normal RTP settings still apply.
- `/rtp [centerName|regionId] --skip-message`
  - Suppresses all teleport-related messages to the player for this invocation.
  - Can be combined with a named center or WorldGuard region argument.

## RTP subcommands

- `/rtp reload`
- `/rtp stats [page]`
- `/rtp stats biomes [page]`
- `/rtp stats rare-biomes [page]`
- `/rtp heatmap [biome]`
- `/rtp heatmap save`
- `/rtp heatmap claims-overlay`
- `/rtp heatmap save claims-overlay`
- `/rtp fake <amount> [world]`
- `/rtp fake clear [world]`
- `/rtp fake <amount> claims [world]`
- `/rtp setcenter <x> <z>`
- `/rtp setcenter <world> <x> <z>`
- `/rtp addcenter <name>`
- `/rtp pregenerate [world] [radius]`

## Force command

- `/forcertp <player> [world] [--skip-message]`
  - If world is omitted, EzRTP uses `force-rtp.yml` `default-world`.
  - `--skip-message`: suppresses the executor notification and all player-facing teleport messages for this invocation. The flag may appear anywhere in the argument list.

## WorldGuard region mode (optional)

When enabled:

- `/rtp <regionId>`

This centers RTP around the specified WorldGuard region and can apply per-region overrides.

## Quick admin cheatsheet

- Reload config: `/rtp reload`
- Force player RTP: `/forcertp Steve world`
- Stats: `/rtp stats`
- Biome stats: `/rtp stats biomes`
- Heatmap map item: `/rtp heatmap`
- Heatmap map + claim overlay: `/rtp heatmap claims-overlay`
- Save heatmap image: `/rtp heatmap save`
- Save heatmap with claim overlay: `/rtp heatmap save claims-overlay`
- Add fake points: `/rtp fake 100 world`
- Add fake points on your faction claims: `/rtp fake 100 claims`
- Clear fake points: `/rtp fake clear world`
- Set RTP center (current world): `/rtp setcenter 0 0`
- Save a named center from your current position: `/rtp addcenter spawn`
