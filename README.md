# sMaintenance

Maintenance mode plugin for Minecraft. Default language is Russian; English is available with `language: en`.

## Supported platforms

| Platform | Versions | Java |
| --- | --- | --- |
| Paper / Spigot / Folia | 1.8.8 to 26.3 | 8+ |
| Velocity | 3.x and 4.x | 17+ on 3.x, 25 on 4.x |

## Features

- Enable and disable maintenance with an optional timer and reason, schedule a start with countdown warnings, extend a running timer.
- Whitelist by name, plus LuckPerms groups that bypass maintenance.
- Configurable kick and MOTD messages (MiniMessage), bossbar countdown and server list ping on Paper.
- Per-server maintenance and a global mode on Velocity, with players moved to fallback servers and queued back when a server reopens.
- Weekly and daily recurring windows, enable on startup, history of the last 50 enables.
- Discord webhook notifications and an audit log.
- PlaceholderAPI (Paper) and MiniPlaceholders (Velocity) support, public API and events on Paper.
- Optional Paper extras: inventory GUI, view-only mode, limit on simultaneous bypass players.
- Update check through Modrinth and bStats metrics.

## Commands

Main command: `/maint` (aliases `/maintenance`, `/tech`). Run it without arguments to see usage.

## License

GPL-3.0, see [LICENSE](LICENSE).
