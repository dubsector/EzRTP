---
title: faction-gui.yml
nav_order: 5
parent: Config Reference
---

# faction-gui.yml

Configures the faction/team claim selection GUI used by `/rtp faction` and `/f rtp`.

## Top-level settings

| Key | Default | Description |
| :--- | :--- | :--- |
| `enabled` | `true` | Enables faction claim GUI routing. |
| `title` | `Faction RTP Claims (<page>/<pages>)` | GUI title. Supports `<page>` and `<pages>`. |
| `size` | `54` | Inventory size (multiple of 9, up to 54). |

## Claim item settings

| Key | Default | Description |
| :--- | :--- | :--- |
| `items.claim.use-player-skulls` | `true` | Uses player skulls for claim owner/claimer when identity is available. |
| `items.claim.fallback-material` | `GRASS_BLOCK` | Used when skull identity is unavailable. |
| `items.claim.name` | `Claim #<index>` | Item display name. |
| `items.claim.lore` | *(see default file)* | Lore lines with placeholders. |

Supported placeholders:

- `<index>`
- `<world>`
- `<chunk_x>`
- `<chunk_z>`
- `<center_x>`
- `<center_z>`

## Navigation settings

| Key | Default | Description |
| :--- | :--- | :--- |
| `navigation.previous.slot` | `45` | Previous page button slot. |
| `navigation.previous.name` | `Previous Page` | Previous page button name. |
| `navigation.next.slot` | `53` | Next page button slot. |
| `navigation.next.name` | `Next Page` | Next page button name. |
