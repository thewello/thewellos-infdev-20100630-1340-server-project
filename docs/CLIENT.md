# Client

## Setup

1. Patch your original `inf-20100630-1340.jar`: put it in the `client/` folder of the release and run `patch-client.bat`. It creates `inf-20100630-1340-mp.jar`.
2. Install the patched jar with one of the launchers:
   - **BetaCraft**: copy `inf-20100630-1340-mp.jar` and `inf-20100630-1340-mp.info` into `%APPDATA%\.betacraft\versions`. Select `inf-20100630-1340-mp` in BetaCraft.
   - **Minecraft Launcher**: create `%APPDATA%\.minecraft\versions\inf-20100630-1340-mp\`, put the patched jar and `inf-20100630-1340-mp.json` in it, then create a profile that uses this version.
3. Start the game with a logged-in original account.

## Joining a server

Main menu, then **Multiplayer**, enter the address (`host` or `host:port`), then **Connect**.
The player name comes from your launcher account; it is not typed in. If the server uses `online-mode=true`, the account is verified with Mojang.

## Controls

| Key | Action |
|---|---|
| T | Chat |
| / | Command |
| Q | Drop item |
| Esc | Menu (does not pause the game in multiplayer) |
| Double Space (creative only) | Toggle flight; Space up, Shift down |

## Notes

- The inventory is kept on your computer; the server only checks it (invalid items, stack sizes, sudden gains).
- Chests and furnaces are locked while one player uses them; others see "in use by another player".
- A patched client only works with a TheWelloDev server of the same version. Protocol mismatches are reported on connect.

## BetaCraft proxy and skins

The launch profile (`release/client/inf-20100630-1340-mp.info`) sets `-Dhttp.proxyHost=betacraft.uk -Dhttp.proxyPort=11702`.
This is BetaCraft's proxy. It applies skin fixes for old Minecraft versions, so player skins show up when the game is
started through the BetaCraft launcher with this profile. The client loads skins from `http://www.minecraft.net/skin/<name>.png`,
the same way the original game does. The proxy is kept for compatibility. Skins may not load when the game is started
without that proxy setting. The skin request is plain HTTP, so a player's name is visible to anyone on the network path.
