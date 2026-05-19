---
title: WorldGuard, GriefPrevention & TeamsAPI
nav_order: 4
parent: Integrations
---

# Protection Integrations (WorldGuard, GriefPrevention, TeamsAPI)

Use these integrations when you want EzRTP to avoid protected regions or claimed chunks.

## What this integration does

When enabled, EzRTP validates candidate RTP locations against configured protection providers and rejects protected points.

Supported provider names in config:

- `worldguard` — WorldGuard protected regions
- `griefprevention` — GriefPrevention claims
- `teamsapi` — Chunk claims managed by any [TeamsAPI](https://modrinth.com/plugin/teams-api)-compatible team plugin
  - Also enables faction RTP claim routing with `/rtp faction` and `/f rtp` (TeamsAPI subcommand API).

If a provider plugin is not installed, EzRTP continues with available providers (safe fallback behavior).

## Where to configure it

File: `plugins/EzRTP/rtp.yml`

```yml
protection:
  avoid-claims: false
  providers:
    - worldguard
    - griefprevention
    - teamsapi
```

### Key settings

- `protection.avoid-claims`
  - `true`: reject locations inside supported protected areas.
  - `false`: ignore protection checks.
- `protection.providers`
  - Priority list of providers EzRTP can query.
  - Keep only providers you actually use to simplify troubleshooting.

Why here: claim/region safety is part of RTP destination validation, so it belongs in `rtp.yml`.

## TeamsAPI claim avoidance

TeamsAPI is a universal bridge between Minecraft team / faction plugins and consumer plugins.
When a team plugin that supports chunk claiming registers a `TeamsClaimService` with TeamsAPI,
EzRTP automatically detects this and avoids claimed chunks during RTP destination search.

**Requirements:**

- [TeamsAPI](https://modrinth.com/plugin/teams-api) plugin installed (soft-depend — not mandatory).
- A compatible team plugin with claim support installed alongside TeamsAPI.

**Behaviour:**

- If TeamsAPI is absent or no claim provider is registered, the `teamsapi` entry in
  `providers` is silently skipped. No errors are logged unless `avoid-claims: true` and
  *all* configured providers are unavailable.
- Claim availability is re-checked dynamically, so a claim plugin that loads after EzRTP
  is picked up without requiring a reload.

## Optional WorldGuard region command mode

You can also enable region-centric command routing:

File: `plugins/EzRTP/rtp.yml` (and compatibility mirror in `config.yml`)

```yml
worldguard:
  region-command:
    enabled: false
    autocomplete: false
```

- Enable this to allow `/rtp <regionId>` behavior.
- Useful for servers with curated gameplay regions.

## Recommended settings by server type

- **Survival / Towny-like protection-heavy servers:** set `avoid-claims: true`.
- **Factions / Teams servers using TeamsAPI:** set `avoid-claims: true` and ensure
  `teamsapi` is in `providers`.
- **Minigame / event servers with custom handling:** keep `avoid-claims: false` unless conflicts occur.
- **Mixed networks:** enable only on servers where claim boundaries matter.
