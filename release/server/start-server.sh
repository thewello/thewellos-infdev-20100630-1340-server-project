#!/bin/sh
# Starts the server in this terminal. Java 8 is required.
cd "$(dirname "$0")" || exit 1
exec java -Xms512M -Xmx1G -jar TheWellosInfdevServer.jar nogui
