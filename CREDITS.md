# Credits

TheWello's inf-20100630-1340 Server Project was written for this repository. It builds on, was inspired by, or
depends on the projects below. Thank you to everyone who made them.

## The game

- **Minecraft Infdev `inf-20100630-1340`**: created by Markus "Notch" Persson and Mojang AB. The game code is not
  included in this repository; you need your own copy. Minecraft is a trademark of Mojang AB.
- **Mojang and Microsoft services**: the session server (`sessionserver.mojang.com`) and the profile service
  (`api.minecraftservices.com`) verify accounts when `online-mode=true`. This project is not affiliated with them.
- **BetaCraft**: a launcher for old Minecraft versions. The client instructions mention it as one way to start the
  patched game.

## Plugin API and plugin inspiration

- **LilyBukkit**, <https://github.com/LilyBukkit>: the plugin API follows the Bukkit-style design used by the
  LilyBukkit project. The LilyBukkit repositories are licensed under GPL-3.0.
- **Bukkit**, <https://github.com/Bukkit/Bukkit>: the Bukkit API whose design (`JavaPlugin`, `@EventHandler`,
  `plugin.yml`, commands and permissions) the plugin system follows.
- **NoCheat** by Wilfried Pasquazzo (Evenprime), <https://github.com/Evenprime/NoCheat>: the anti-cheat plugin
  `TheWelloNoCheat` is inspired by NoCheat. The NoCheat jar ships under the MIT License; its GitHub repository is
  listed as GPL-3.0.
- **NoLagg** by bergerkiller, <https://github.com/bergerkiller/NoLagg>: the performance plugin `TheWelloNoLagg` is
  inspired by NoLagg.
- **OpenInv** by lishid, <https://github.com/lishid/OpenInv>: the inventory plugin `TheWelloOpenInv` is inspired by
  OpenInv. Its GitHub repository is listed as GPL-3.0.

## Libraries

- **ASM** (OW2 project), <https://asm.ow2.io>: bytecode transformation used by the server and the client patcher.
  Bundled in `lib/` under the BSD 3-Clause License. See [lib/ASM-LICENSE.txt](lib/ASM-LICENSE.txt).
- **LWJGL 2**, <https://www.lwjgl.org>: used only at compile time. It is not bundled here, and the release does not
  include it.

## Tools

- **Claude** (Anthropic): this project was written with AI-assisted "vibe coding" with Claude. Its code was
  reviewed and tested by its maintainer, but it has not been formally audited.
