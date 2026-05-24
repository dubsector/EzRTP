---
title: Config Reference
nav_order: 7
has_children: true
---

# Config Reference

EzRTP uses a set of focused configuration files. Browse the pages below for
full details on each file.

- `config.yml`
- `rtp.yml`
- `limits.yml`
- `storage.yml`
- `gui.yml`
- `faction-gui.yml`
- `queue.yml`
- `network.yml`
- `messages/*.yml` (top-level keys and nested `messages.*` keys are both supported)

Notes:

- Missing message keys in language files are backfilled automatically on startup.
- Existing translated/customized message values are preserved during backfill.
