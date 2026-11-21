# Multi-version support

One server program runs one Infdev build at a time. This page describes the model, how a build is selected and
recognised, how worlds move between builds, and how a new build is added. The notes of each build are in
[versions/](versions/).

## The model

- **One server, one build at a time.** The operator picks the build with `game-version` in `server.properties`. The
  server then runs exactly that build's game code: its blocks, items, mobs, world generation and file formats.
- **Builds are independent.** Nothing is transferred from a newer build to an older one. A server that runs
  `inf-20100227-1433` has that build's content only (for example, no mobs, because that build has none).
- **Worlds are per build.** Each build keeps its worlds in `worlds/<game-version>/<level-name>/`.
- **Clients match the server.** A player needs the client patched from the same build. A client of another build is
  refused at login with a message that names the build the server runs. The protocol number of each build is part of
  the handshake (see the table below).
- **Code per build.** Each build has its own bridge (`server/bridge<build>/`), client mod (`client/mod<build>/`) and
  plugin API (`api/<build>/`). The game classes are obfuscated and differ between builds, so a shared code path with a
  table of names was not used; the duplication is deliberate.
- **The multiplayer menu** is the only feature that is added from outside the original game: no original Infdev build has one.

## Supported builds

| Build | Protocol | SHA-256 of the original jar |
|---|---|---|
| `inf-20100227-1433` | 10 | `507cecfad21caa2e137a00c57d63ac6ebc299cb1191e703f1c49274d53e7fef3` |
| `inf-20100313` | 12 | `4974d31eb0af16ddbaaa2d81e88943baeaf8bbc0106a8db20cda19f4a824152d` |
| `inf-20100316` | 13 | `a363943f5e2c147ab0515930317dd535e9a2229fa0eb952c73fb6f078cfbda20` |
| `inf-20100630-1340` | 11 | `92e4969e76cd242a53ae9ca10206ece9e6596b441ff9896fd9a34b9b61ad072e` |

The registry is `common/src/net/thewellodev/protocol/GameVersion.java`.

## Selection and checks

- **Startup.** The server looks for the game jar in its folder. A jar is identified by the manifest attribute that the
  patcher writes (`TheWelloDev-Game-Version`) or, for an original jar, by its SHA-256. If no jar of the chosen build is
  found, the server does not start and names what it expected. Jars of other builds in the folder are skipped.
- **Class loader.** `GameClassLoader` loads the game classes of the chosen build and applies the build's ASM hooks.
  For builds 227, 313 and 316 it also replaces the unseeded `new Random()` calls of the terrain generators with a seeded
  generator (`TWSeed`), so `level-seed` works and the same seed gives the same land.
- **Login.** The client sends the protocol number of its build. A different number is refused.
- **Commands.** Built-in commands that make no sense for a build (for example `/give` on a build without items) are not
  registered; `/help` lists only what exists.

## Worlds and the takeover of older worlds

A world of an older build can be played on a newer one. The step is lazy, so large worlds start at once and a
half-finished conversion never leaves a world that cannot be opened:

1. If the target build has no world for `level-name` but an older build has one, the server creates the new world
   folder with `level.dat`, the players' positions and saved inventories, and a marker file `legacy.properties`
   (`from=<build>`, `complete=false`).
2. The bridge of the new build reads a chunk from the old world's folder the first time it is needed, converts it in
   memory and saves it in the new folder with the next save. A chunk that already exists in the new folder always wins.
3. `/convertworld` converts the chunks nobody has visited yet, a batch of files every tick while the server keeps
   running, and then sets `complete=true`. While it is `false` the console reminds at every start, and a missing old
   folder is reported.
4. The old folder is only read, never changed. Do not delete it before the conversion is complete.

Steps that exist: 227 to 313, 313 to 316, and 227 to 316. The chunk files of these builds use the same container and
tags; the light of a 227 chunk (which has none) is worked out when the chunk is read. The terrain generators of 313 and
316 are the same code, so land made after a 313 to 316 takeover fits the converted land (apart from the random scatter
of flowers). Converting back to an older build is not supported. There is no step to `inf-20100630-1340`: many builds lie
between it and the others that are not supported yet, and its world format differs.

A new build adds a case to `WorldConverter.supportsStep`, and its bridge converts the chunk it reads.

## Inventories across builds

Builds 227, 313 and 316 keep the inventory on the client, and each build starts a player with its own default one. The
client reports its inventory to the server (`INVENTORY_REPORT`, 32-bit counts); the server saves it as
`players/<name>.inv` and sends it back after login (`INVENTORY_SYNC`). A player who was saved keeps the inventory when
the server moves to a newer build; a player who was never saved starts with the default of the build he joins.

## Items on the ground in build 316

The game of each 316 client runs item entities. The client reports each new item (`DROP_SPAWN`) and each item that is
gone (`DROP_REMOVE`); the server numbers them and passes them to the other players, whose clients show a copy. Items on
the ground before a player joined are not sent to him. Arrows that cross another player's body report a hit to the
server, which applies the PvP rules.

## Freeze reports

The server (`logs/freeze-*.txt`, a tick over 8 seconds) and the clients of builds 227, 313 and 316
(`%USERPROFILE%\.thewellodev\freeze-*.txt`, no tick for 8 seconds while connected) write the stack of the stuck thread.

## Adding a build

1. Identify the build: add it to `GameVersion` (id, protocol number, SHA-256) and to `GameVersion.chronological()` if
   worlds can be taken over into it.
2. Map the classes and members the hooks and the bridge use (compare with the nearest build; the notes of each build in
   [versions/](versions/) list what changed).
3. Server: write `server/bridge<build>/TWBridge<build>.java` (a neighbouring bridge is the best start), and add the class
   loader case in `GameClassLoader` (probe class, seeded generator classes).
4. Client: write `client/mod<build>/`, add `createTransformer<build>` to the patcher and the detection in
   `ClientPatcher.detect`.
5. Plugin API: see [plugin-api/README.md](plugin-api/README.md).
6. `build.sh`: add the build to the lists, add `release/client/<build>-mp.json` and `.info`.
7. Test: server start, new world, takeover of a world, two clients, blocks both ways, other players, plugins.
