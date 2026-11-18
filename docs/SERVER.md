# Server

## Starting

Double-click `start-server.bat` (Windows) or run:

```sh
java -Xmx1G -jar TheWelloDev-Server.jar          # with the control panel
java -Xmx1G -jar TheWelloDev-Server.jar nogui    # console only
```

The server needs the patched game jar `inf-20100630-1340-mp.jar` (or the original `inf-20100630-1340.jar`) in its folder.
It loads the game from there.

Java 8 is recommended. Use `-Xmx2G` for more than about 20 players.

## server.properties

| Key | Default | Description |
|---|---|---|
| `server-port` | 25565 | TCP port |
| `server-ip` | (empty) | Bind to a specific address |
| `motd` | `&eA TheWelloDev server` | Message shown in the server list (`&` colour codes) |
| `max-players` | 20 | Player limit |
| `upnp` | true | Open the port on the router automatically |
| `online-mode` | true | Verify accounts with Mojang. Set to false only for offline testing |
| `white-list` | false | Only players in `white-list.txt` may join |
| `level-name` | world | World folder |
| `level-seed` | (empty) | Seed; empty means random |
| `view-distance` | 6 | Chunk radius sent to players (2-15) |
| `spawn-protection` | 0 | Radius around spawn where non-operators cannot change blocks |
| `pvp` | true | Players can damage each other |
| `difficulty` | 2 | 0 peaceful, 1 easy, 2 normal, 3 hard |
| `spawn-monsters` | true | Natural monster spawning |
| `spawn-animals` | true | Natural animal spawning |
| `mob-cap-monsters` | 100 | Monsters per player (within 128 blocks) |
| `mob-cap-animals` | 20 | Animals per player (within 128 blocks) |
| `autosave-minutes` | 5 | Interval for saving the world |
| `game-jar` | auto | Game jar to load; `auto` picks `inf-20100630-1340*.jar` in the server folder |

## Operators and permissions

- Operators are listed in `ops.txt`, or set with `/op <name>` in the console.
- `permissions.txt` gives non-operators extra permissions: `player: perm.one, perm.two`, or `*: perm` for everyone.
- Plugin permissions that end in `.use` or `.default` are open to everyone; others need operator status or an entry in `permissions.txt`.

## Commands

Type `/help` in the console or in chat. `/help <word>` searches commands; `/help <command>` shows one command.

## Plugins

Put plugin jars in the `plugins/` folder and restart (or use `/reload`). See [PLUGIN-API.md](PLUGIN-API.md).

## Files

| File | Purpose |
|---|---|
| `world/` | World data and player positions |
| `ops.txt`, `white-list.txt`, `banned-players.txt`, `banned-ips.txt` | Access lists |
| `permissions.txt` | Extra permissions |
| `logs/latest.log` | Log of the current run |
| `plugins/` | Plugin jars and their configuration folders |
