# Building TheWelloDev

## Requirements

- JDK 8 (the game and the client code target Java 8).
- The original `inf-20100630-1340.jar` (Infdev client). Not included; obtain it yourself.
- LWJGL 2 jar (compile-time only). The one shipped with the BetaCraft or Minecraft launcher folders works.

## Build

Windows (Git Bash, or any `sh`):

```sh
CLIENT_JAR=original/inf-20100630-1340.jar LWJGL_JAR=path/to/lwjgl.jar sh build.sh
```

Windows (command prompt): use `build.bat`, which expects the same inputs in the same places.

The build runs eight steps:

1. Compile `common/` (protocol, ASM hook tool).
2. Compile the client patcher.
3. Create a public copy of the original jar for compiling the mod.
4. Compile the client mod and create `dist/inf-20100630-1340-mp.jar` (the patched client) and `dist/TheWelloDev-ClientPatcher.jar`.
5. Build the API jar `dist/TheWelloDev-API.jar` (and a sources jar).
6. Build the server `dist/TheWelloDev-Server.jar`.
7. Build every plugin in `plugins/` into `dist/plugins/`.
8. Assemble the release bundle in `../TheWelloDev-Release` (override with `RELEASE_DIR=...`).

## Output

- `dist/`: development jars. The patched client `dist/inf-20100630-1340-mp.jar` is a Minecraft derivative; keep it local.
- `../TheWelloDev-Release/`: the bundle for other people. It contains no Minecraft code.

## Running from the build

Copy `dist/inf-20100630-1340-mp.jar` into the server folder (the server loads the game jar from there), then start `TheWelloDev-Server.jar`.
