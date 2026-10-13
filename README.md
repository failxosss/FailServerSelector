# ServerSelector

Lobby plugin for **Paper 1.21.x** (Java 21) by **Failxos**.

* Hotbar **Server Selector** item (middle slot) that opens a fully configurable GUI
* Hotbar **Players Shown/Hidden** toggle
* Everything configurable in `config.yml`: items, names, lore, slots, commands/actions, filler layout
* Per-world availability of the hotbar items (e.g. only in `lobby`, not in `spawn`)
* Optional **ItemsAdder** support for every item (falls back to a normal material)
* Heads by skin name, Base64 texture, texture URL or UUID
* PlaceholderAPI support (optional), MiniMessage + legacy `&` colors

## Installation

1. Download the jar from Releases (or build it, see below).
2. Put it in `plugins/` and restart.
3. Edit `plugins/ServerSelector/config.yml`, then run `/ss reload`.

## Build

```
mvn package
```
The jar is created in `target/`.

## Commands & permissions

| Command | Permission | Description |
|---|---|---|
| `/ss reload` | `serverselector.admin` | Reload the config |
| `/ss open <menu> [player]` | `serverselector.open` (`.admin` for others) | Open a menu |
| `/ss refresh [player]` | `serverselector.admin` | Re-give the hotbar items |

Other permissions: `serverselector.bypass.hide` (never hidden), `serverselector.bypass.protect` (can move/drop plugin items).

## Configuration highlights

Limit an item to some worlds:

```yaml
worlds:
  mode: WHITELIST   # or BLACKLIST
  list: [lobby]     # empty = every world
```

Use an ItemsAdder item:

```yaml
itemsadder: "namespace:item_id"
material: NETHER_STAR   # fallback if ItemsAdder is missing
```

Make a menu slot a player head and run commands:

```yaml
server_1:
  slot: 11
  head: "SkinName"          # or "texture:BASE64", "url:...", "uuid:..."
  name: "<green>Survival"
  lore:
    - "<gray>Click to join!"
  actions:
    - "[server] survival"                 # BungeeCord / Velocity
    - "[console] say %player% joined"
```

Action tags: `[player]`, `[console]`, `[op]`, `[message]`, `[broadcast]`, `[actionbar]`, `[title]`, `[sound]`, `[server]`, `[open]`, `[close]`, `[toggle-players]`, `[delay]`. See `config.yml` for details.

## License

MIT - see [LICENSE](LICENSE).
