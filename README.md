# TheWello's inf-20100630-1340 Server Project

Multiplayer for the Minecraft Infdev build `inf-20100630-1340`, which was released without multiplayer.
This project adds a dedicated server, a client patcher that adds multiplayer to your own copy of the game,
a plugin API and a set of optional plugins.

Internally the project is called **TheWelloDev** (the Java packages are `net.thewellodev`).

> Not affiliated with Mojang AB, Microsoft or Markus "Notch" Persson. Minecraft is a trademark of Mojang AB.
> This repository contains **no game code**. You need your own legal copy of `inf-20100630-1340.jar`.

## About this project

This project was built almost entirely with **vibe coding**: the code was written by an AI assistant (Claude, by
Anthropic) from conversational instructions, then tested by running a server and playing on it. It has not been
independently audited or formally reviewed. Read the code before you run a server that is open to the internet,
and see [SECURITY.md](SECURITY.md).

## Features

**Server**
- Runs the original Infdev world code headless, with multiplayer sessions, chunk streaming and mobs.
- Plugin system with a Bukkit-style API (see [`api/`](api/) and [docs/PLUGIN-API.md](docs/PLUGIN-API.md)).
- Control panel with console, memory and TPS readings, and a per-player inventory window for operators.
- Account verification with Mojang's session server (`online-mode=true`).
- Operators, whitelist, bans, mob caps and automatic port mapping (UPnP).
- Built-in checks for health and inventory updates.

**Client**
- A patcher that turns your original jar into a multiplayer-capable jar.
- Direct connect, chat, remote players, and chests and furnaces that are locked while one player uses them.

**Optional plugins** (in the release bundle, `server/optional-plugins/`)
- `NameColor`: colours player names by role.
- `NoIllegals`: stops players from holding items that cannot be obtained in survival.
- `TheWelloNoCheat`: movement, reach, attack-rate and chat-spam checks.
- `TheWelloNoLagg`: limits dropped items, TNT and mob build-up.
- `TheWelloOpenInv`: lets operators look at and edit the inventory of an online player.

## Quick start

1. Download the release bundle from the [Releases](../../releases) page and unzip it.
2. Put your original `inf-20100630-1340.jar` into the `client/` folder and run `patch-client.bat`.
   It creates `inf-20100630-1340-mp.jar`.
3. Copy `inf-20100630-1340-mp.jar` into the `server/` folder and run `start-server.bat`. Java 8 is required.
4. Change `server/server.properties` to suit your server.
5. Install the patched client as described in the bundle's `README.txt` or [docs/CLIENT.md](docs/CLIENT.md).

## Platform and tested setup

- The start scripts (`start-server.bat`, `patch-client.bat`) are for Windows.
  On other systems run `java -jar TheWelloDev-Server.jar` in the server folder. This has not been tested.
- Developed and tested on Windows with Java 8 (Temurin). Java 8 is required.
- Known limitations are listed in [SECURITY.md](SECURITY.md).

## Important: tied to one game build

The client mod and the server bridge refer to the obfuscated class, field and method names of the original
`inf-20100630-1340.jar` (for example `aw` for the game class, `fe` for the world, `dd` for the player). The project only works with that exact jar. Other Infdev builds
need a new name mapping and are not supported. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Documentation

- [Build from source](docs/BUILD.md)
- [Server setup and settings](docs/SERVER.md)
- [Client setup](docs/CLIENT.md)
- [Plugin API](docs/PLUGIN-API.md)
- [Architecture](docs/ARCHITECTURE.md)

## Repository layout

| Path | Contents |
|---|---|
| `common/` | Protocol definitions and the ASM hook tool, shared by server and client |
| `api/` | Public plugin API (`net.thewellodev.api`) |
| `server/src/` | Server core and control panel |
| `server/bridge/` | Bridge between the server and the original game classes |
| `client/patcher/` | Patcher that adds multiplayer support to the original client jar |
| `client/mod/` | Client-side multiplayer code, compiled into the patched client |
| `plugins/` | Example and bundled plugins (each has `src/` with a `plugin.yml`) |
| `release/` | Templates for the release bundle (end-user README, scripts, server settings) |
| `docs/` | Documentation |
| `lib/` | ASM 9.8 (BSD 3-Clause) |

## Building from source

Requires JDK 8 and an LWJGL 2 jar (compile-time only). See [docs/BUILD.md](docs/BUILD.md).

```sh
CLIENT_JAR=original/inf-20100630-1340.jar LWJGL_JAR=path/to/lwjgl.jar sh build.sh
```

## Credits

This project builds on and was inspired by other projects, including LilyBukkit, the Bukkit API, NoCheat, NoLagg
and OpenInv. See [CREDITS.md](CREDITS.md) for the full list and licenses.

## License

MIT. See [LICENSE](LICENSE). Bundled third-party code: [ASM](lib/ASM-LICENSE.txt) (BSD 3-Clause).
