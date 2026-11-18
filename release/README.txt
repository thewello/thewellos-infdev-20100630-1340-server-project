TheWelloDev - Infdev multiplayer server (inf-20100630-1340)
==========================================================

This package contains the server, the client patcher and the plugin API.
It does NOT contain Minecraft. You need your own copy of the original
Infdev jar, named inf-20100630-1340.jar.

Contents
  client/       patcher and launcher profile templates
  server/       server jar, start script, server.properties, optional plugins
  plugin-api/   API jar, API documentation, example plugin

1. Patch the client
   Put your original inf-20100630-1340.jar in the client/ folder and run
   patch-client.bat. It creates inf-20100630-1340-mp.jar.

2. Install the client
   BetaCraft: copy inf-20100630-1340-mp.jar and inf-20100630-1340-mp.info into
   the BetaCraft versions folder (%APPDATA%\.betacraft\versions).
   Minecraft Launcher: create the folder
   %APPDATA%\.minecraft\versions\inf-20100630-1340-mp\ and put the patched jar
   and inf-20100630-1340-mp.json in it, then create a launcher profile for it.

3. Run the server
   Copy inf-20100630-1340-mp.jar (from step 1) into server/ and double-click
   start-server.bat. Java 8 is required. Allow the Windows firewall when asked.
   Settings are in server.properties.

4. Optional plugins
   Copy any jar from server/optional-plugins/ into server/plugins/ and restart.
   See plugin-api/PLUGIN-API.md to write your own plugins.

Operators: type /op <name> in the server console, or add the name to ops.txt.

Notes
  - online-mode=true requires original (paid) Minecraft accounts.
  - The server and the client must be the same TheWelloDev version.
  - Third-party licenses: THIRD-PARTY-LICENSES.txt.
