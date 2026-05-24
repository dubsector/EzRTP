---
title: EzCountdown
nav_order: 6
parent: Integrations
---

# EzCountdown Integration

Use this integration when you want to show the RTP countdown using
[EzCountdown](https://modrinth.com/plugin/ezcountdown) display effects instead
of (or alongside) the built-in bossbar. EzCountdown supports more display
channels and richer formatting.

## What this integration does

When EzCountdown is installed and the integration is enabled in `rtp.yml`,
EzRTP delegates its per-player countdown display to EzCountdown rather than
rendering a bossbar/chat message itself. Each teleporting player receives an
ephemeral, private countdown that expires when the teleport fires or is
cancelled.

- Movement detection and PvP cancellation still apply exactly as with the
  built-in countdown.
- If EzCountdown is absent or `ezcountdown.enabled: false`, EzRTP falls back to
  its own bossbar/chat countdown automatically.
- The integration is activated once per server start when EzCountdown is
  detected; a `/rtp reload` restarts the display settings but does not
  re-initialize the bridge.

## Requirements

| Plugin | Where to get it |
| :--- | :--- |
| [EzCountdown](https://modrinth.com/plugin/ezcountdown) | Modrinth · [GitHub](https://github.com/ez-plugins/EzCountdown) |

EzCountdown is a soft dependency — servers without it work normally with the
built-in display.

## Where to configure it

File: `plugins/EzRTP/rtp.yml`, nested under `countdown:`

```yml
countdown:
  # ... existing built-in countdown settings ...
  ezcountdown:
    enabled: false
    display-types:
      - ACTION_BAR
      - BOSS_BAR
    format: "<yellow>Teleporting in <white>{formatted}</white>...</yellow>"
```

### Key settings

- `enabled`
  - `true`: hand countdown display off to EzCountdown (built-in display is
    skipped for players where EzCountdown starts successfully).
  - `false`: use the built-in bossbar/chat display (default).
- `display-types`
  - One or more display channels EzCountdown renders simultaneously.
  - See [Display types](#display-types) below for all valid values.
- `format`
  - MiniMessage string shown by each active display channel.
  - `{formatted}` is replaced with the remaining time in human-readable form
    (e.g. `5s`, `1m 30s`).

## Display types

The following values are accepted in the `display-types` list:

| Value | Where it appears |
| :--- | :--- |
| `ACTION_BAR` | Above the hotbar |
| `BOSS_BAR` | Bar across the top of the screen |
| `TITLE` | Large centre-screen overlay |
| `CHAT` | Chat messages |
| `SCOREBOARD` | Sidebar scoreboard |
| `DIALOG` | Dialogue box (requires server support) |

You can combine any number of them. Example showing three at once:

```yml
display-types:
  - ACTION_BAR
  - BOSS_BAR
  - TITLE
```

## Startup log

When EzCountdown is detected, EzRTP prints one line during enable:

```text
[EzRTP] EzCountdown integration enabled.
```

If EzCountdown is not installed, nothing is printed and the built-in countdown
is used without any configuration change.

## Interaction with the built-in countdown

The built-in countdown settings (`countdown.bossbar`, `countdown.chat-messages`,
`countdown.particles`, etc.) remain active for all display that does **not** go
through EzCountdown. When `ezcountdown.enabled: true`:

- The EzCountdown display replaces the EzRTP bossbar for that player.
- Particle effects (if configured) still run independently.
- Movement cancellation and warn-distance checks still apply.

If EzCountdown fails to start a countdown for a player for any reason, EzRTP
falls back to its built-in display for that player.
