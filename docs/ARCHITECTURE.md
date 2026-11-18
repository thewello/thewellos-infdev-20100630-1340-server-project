# Architecture

## Server

- The server loads the original Infdev classes with a custom class loader and inserts small hook calls with ASM (`GameClassLoader`, `HookTransformer`). Every block change is reported to the server.
- World generation, the save format, water and lava flow, falling blocks and random ticks are the original code.
- Mobs run the original AI on the server. Because the game is single-player by design, mobs look at a "player in the world" field; before every entity update the server sets it to the nearest player's proxy (`TWServerHook`).
- Players have invisible proxies in the game world so mobs can target them (`TWProxy`).
- Chunks are sent to each client on demand; the server sends block, metadata, light and height data.
- Client actions (block changes, items, attacks, health reports, inventory summaries) are validated on the server before they are applied and shared with other players.

## Client

- The patched client receives chunks and entities from the server and does not generate its own world.
- Mobs and arrows are created as real game objects so their models, sounds and death animations come from the game. Their AI is disabled; their position comes from the server.
- Chests and furnaces use a lock: only one player can open a container at a time, which prevents item duplication.

## Protocol

Packets are `[byte id][body]`, written with `DataOutputStream`. The definitions are in `common/src/net/thewellodev/protocol/Protocol.java`. The version is checked on connect.

## Security measures

- Accounts are verified with Mojang's session server (`online-mode=true`). The access token goes only to Mojang and Microsoft.
- Connection limits per IP and in total, a handshake timeout, and a per-connection queue limit.
- Rate limits per player for blocks, items, movement, digging and chat.
- Optional checks (movement, reach, attack rate, health, inventory, chat) live in the anti-cheat plugin, not in the core.

## Plugins

Plugin jars are loaded with one class loader per plugin. Plugins receive events through `@EventHandler` methods and talk to the server through the API (`net.thewellodev.api`). The server implements the API in `server/src/net/thewellodev/server/`.
