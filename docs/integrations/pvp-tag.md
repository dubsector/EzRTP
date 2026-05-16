---
title: PvP Tag (CombatLogX / PvPManager / Simple Combat Log)
nav_order: 5
parent: Integrations
---

# PvP Tag Integration

Use this integration when you want EzRTP to cancel a pending teleport or active
countdown the moment a player enters combat.

## Supported plugins

All three plugins are optional soft-dependencies. EzRTP auto-detects whichever
ones are present and registers them at startup. You can have more than one
installed simultaneously.

| Plugin | Where to get it |
| :--- | :--- |
| [CombatLogX](https://www.spigotmc.org/resources/combatlogx.31689/) | SpigotMC (597 K+ downloads) |
| [PvPManager](https://modrinth.com/plugin/pvpmanager) | Modrinth · [SpigotMC (free)](https://www.spigotmc.org/resources/pvpmanager-lite.845/) |
| [Simple Combat Log](https://modrinth.com/plugin/simple-combatlog) | Modrinth · [GitHub](https://github.com/NikeyV1/CombatLog) |

No configuration change is required on servers that do not use any of them.

## What this integration does

- **Countdown cancellation** — if a player receives a PvP tag while a countdown is
  ticking, the teleport is cancelled and a configurable message is sent.
- **Queue cancellation** — if a queued teleport slot is dispatched while the player
  is already tagged, the teleport is skipped and the player must re-queue.

## Where to configure it

File: `plugins/EzRTP/rtp.yml`

```yml
pvp-tag-integration:
  cancel-countdown-on-pvp-tag: true
  cancel-queued-on-pvp-tag: true
```

### Key settings

- `cancel-countdown-on-pvp-tag`
  - `true`: cancel the active countdown as soon as the player is tagged.
  - `false`: ignore combat tags during countdown (teleport proceeds regardless).
- `cancel-queued-on-pvp-tag`
  - `true`: skip the queued teleport if the player is in combat at dispatch time.
  - `false`: dispatch the queued teleport even while in combat.

## Messages

Add or override these keys in `plugins/EzRTP/messages/en.yml`:

| Key | Default text |
| :--- | :--- |
| `countdown-pvp-cancel` | `<red>Teleport cancelled — you entered combat!</red>` |
| `queue-pvp-tag-cancel` | `<red>Teleport skipped — you are in combat!</red>` |

## Startup log

When a supported plugin is detected, EzRTP logs a confirmation on enable:

```text
[EzRTP] PvP tag integration: CombatLogX detected.
[EzRTP] PvP tag integration: PvPManager detected.
[EzRTP] PvP tag integration: Simple Combat Log detected.
```

If none of the supported plugins are installed, no message is printed and the
feature is effectively disabled.
