# Changelog

## 2.0.0

The project is now **TheWello's Infdev Server Project**. Its goal changed: instead of one server for one Infdev build
(`inf-20100630-1340`), one server program now runs any of several Infdev builds, one at a time, each with its own game
code, worlds, client and plugin API.

### Added

- **Multi-version server.** `game-version` in `server.properties` selects the build: `inf-20100630-1340`,
  `inf-20100227-1433`, `inf-20100313` or `inf-20100316`. The server identifies the game jar by its contents, keeps worlds
  per build in `worlds/<game-version>/<level-name>/`, and refuses clients of another build.
- **Three new builds**, each with a server bridge, a client mod and a plugin API: `inf-20100227-1433`, `inf-20100313`
  and `inf-20100316`.
- **World takeover** from an older build to a newer one (227 to 313 to 316): lazy, chunk by chunk, with `/convertworld`
  for the rest. The old world is never modified. Positions, spawn point and saved inventories come along.
- **Seeded terrain** for builds 227, 313 and 316: `level-seed` works and the same seed gives the same land.
- **Saved inventories** for builds 227, 313 and 316, which keep the inventory on the client.
- **Client patcher** with automatic build detection (`patch-client.bat` / `patch-client.sh`) and optional BetaCraft install.
- **Items on the ground** in build 316 are shared between players, and arrows hurt other players.
- **Freeze reports**: `logs/freeze-*.txt` on the server and `freeze-*.txt` on the clients of builds 227, 313 and 316.
- **Plugin API per build** (`api227`, `api313`, `api316` next to the original API) and the four bundled plugins for every build.
- **Release bundle without Minecraft code** and its zip archive, built by `build.sh`.

### Changed

- Project name, jar names (`TheWellosInfdevServer.jar`, `TheWellosInfdevClientPatcher.jar`), documentation and
  console messages (now English only).
- `/time` uses the day length of the running build (12000 ticks in 313, 24000 in the others).
- Two game windows on one computer no longer turn each other's camera.

### Fixed

- Walking animation of other players (they were always drawn standing).
- Knockback between two players on the same spot could kick the victim (NaN position).
- Tools and arrows were lost from the saved inventory of builds 227, 313 and 316.
- Chunks that were not redrawn when they arrived in build 316.

### Known limits

- Build 316: items on the ground before a player joins are not shown to him; an arrow that spawns a mob does it only in
  the shooter's game; the game has no explosions and fire cannot be punched out.
- Builds 227, 313 and 316 keep inventory (and, in 316, items) on the client, so the server can only limit it. See
  [SECURITY.md](SECURITY.md).
- Tested on Windows 11 with Java 8 only.

## 1.0.0

Multiplayer for `inf-20100630-1340`: dedicated server, client patcher, plugin API and optional plugins.
