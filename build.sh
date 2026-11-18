#!/bin/sh
# Builds TheWelloDev. Requires JDK 8.
#
# Inputs (not included in this repository):
#   CLIENT_JAR  original Infdev jar, default: original/inf-20100630-1340.jar
#   LWJGL_JAR   LWJGL 2 jar (compile-time only), default: lib/lwjgl.jar
# Output:
#   dist/       jars built from the source
#   $RELEASE_DIR  the release bundle (no Minecraft code inside)
set -e
cd "$(dirname "$0")"

JBIN="${JAVA_HOME:+$JAVA_HOME/bin/}"
JAVAC="${JBIN}javac"
JAR="${JBIN}jar"
JAVA="${JBIN}java"
CLIENT_JAR="${CLIENT_JAR:-original/inf-20100630-1340.jar}"
LWJGL_JAR="${LWJGL_JAR:-lib/lwjgl.jar}"
RELEASE_DIR="${RELEASE_DIR:-../TheWelloDev-Release}"
S="${SEP:-;}"
JOPT="-nowarn -encoding UTF-8 -source 1.8 -target 1.8"
ASM="lib/asm-9.8.jar${S}lib/asm-tree-9.8.jar"

if [ ! -f "$CLIENT_JAR" ]; then
  echo "Missing $CLIENT_JAR. Put the original inf-20100630-1340.jar there or set CLIENT_JAR."
  exit 1
fi
if [ ! -f "$LWJGL_JAR" ]; then
  echo "Missing $LWJGL_JAR. Put an LWJGL 2 jar there or set LWJGL_JAR."
  exit 1
fi

rm -rf build
mkdir -p build/common build/patcher build/mod build/api build/server build/bridge/bridge build/plugins dist

echo "[1/8] common (protocol and ASM hook tool)"
"$JAVAC" $JOPT -cp "$ASM" -d build/common $(find common/src -name '*.java')

echo "[2/8] client patcher"
"$JAVAC" $JOPT -cp "$ASM${S}build/common" -d build/patcher $(find client/patcher/src -name '*.java')

echo "[3/8] public client jar (for compiling)"
"$JAVA" -cp "$ASM${S}build/common${S}build/patcher" net.thewellodev.patcher.ClientPatcher "$CLIENT_JAR" build/client-public.jar --publicize-only

echo "[4/8] client mod, patcher jar and patched client"
"$JAVAC" $JOPT -cp "$LWJGL_JAR${S}build/client-public.jar${S}build/common" -d build/mod $(find client/mod -name '*.java')
mkdir -p build/patcher/mod
cp -r build/mod/* build/patcher/mod/
(cd build/common && find net/thewellodev/protocol -name '*.class') | while read f; do
  mkdir -p "build/patcher/mod/$(dirname "$f")"; cp "build/common/$f" "build/patcher/mod/$f"; done
(cd build/patcher/mod && find . -name '*.class' | sed 's|^\./||' | sort) > build/patcher/mod/index.txt
mkdir -p build/patcher-all && cp -r build/common/* build/patcher/* build/patcher-all/
(cd build/patcher-all && "$JAR" xf ../../lib/asm-9.8.jar && "$JAR" xf ../../lib/asm-tree-9.8.jar && rm -rf META-INF module-info.class)
printf 'Main-Class: net.thewellodev.patcher.ClientPatcher\n' > build/patcher.mf
"$JAR" cfm dist/TheWelloDev-ClientPatcher.jar build/patcher.mf -C build/patcher-all .
"$JAVA" -jar dist/TheWelloDev-ClientPatcher.jar "$CLIENT_JAR" dist/inf-20100630-1340-mp.jar

echo "[5/8] API"
"$JAVAC" $JOPT -d build/api $(find api/src -name '*.java')
mkdir -p build/api-src && cp -r api/src/* build/api-src/
"$JAR" cf dist/TheWelloDev-API.jar -C build/api .
"$JAR" cf dist/TheWelloDev-API-sources.jar -C build/api-src .

echo "[6/8] server"
"$JAVAC" $JOPT -cp "$ASM${S}build/common${S}build/api" -d build/server $(find server/src -name '*.java')
"$JAVAC" $JOPT -cp "build/client-public.jar${S}build/common${S}build/server" -d build/bridge/bridge $(find server/bridge -name '*.java')
mkdir -p build/server-all && cp -r build/common/* build/api/* build/server/* build/bridge/* build/server-all/
(cd build/server-all && "$JAR" xf ../../lib/asm-9.8.jar && "$JAR" xf ../../lib/asm-tree-9.8.jar && rm -rf META-INF module-info.class)
printf 'Main-Class: net.thewellodev.server.Main\n' > build/server.mf
"$JAR" cfm dist/TheWelloDev-Server.jar build/server.mf -C build/server-all .

echo "[7/8] plugins"
mkdir -p dist/plugins
for dir in plugins/*/; do
  name=$(basename "$dir")
  out="build/plugins/$name"
  mkdir -p "$out"
  "$JAVAC" $JOPT -cp "dist/TheWelloDev-API.jar" -d "$out" $(find "$dir/src" -name '*.java')
  cp "$dir/src/plugin.yml" "$out/"
  jarname=$(grep '^name:' "$dir/src/plugin.yml" | sed 's/^name: *//' | tr -d '\r')
  "$JAR" cf "dist/plugins/$jarname.jar" -C "$out" .
done

echo "[8/8] release bundle -> $RELEASE_DIR"
rm -rf "$RELEASE_DIR"
mkdir -p "$RELEASE_DIR/client" "$RELEASE_DIR/server/plugins" "$RELEASE_DIR/server/optional-plugins" "$RELEASE_DIR/plugin-api/example-plugin"
cp release/README.txt release/THIRD-PARTY-LICENSES.txt lib/ASM-LICENSE.txt LICENSE "$RELEASE_DIR/"
cp dist/TheWelloDev-ClientPatcher.jar release/client/patch-client.bat "$RELEASE_DIR/client/"
cp release/client/inf-20100630-1340-mp.json release/client/inf-20100630-1340-mp.info "$RELEASE_DIR/client/"
cp dist/TheWelloDev-Server.jar release/server/start-server.bat release/server/server.properties "$RELEASE_DIR/server/"
cp dist/plugins/*.jar "$RELEASE_DIR/server/optional-plugins/"
rm -f "$RELEASE_DIR/server/optional-plugins/HelloPlugin.jar"
cp dist/TheWelloDev-API.jar dist/TheWelloDev-API-sources.jar "$RELEASE_DIR/plugin-api/"
cp docs/PLUGIN-API.md "$RELEASE_DIR/plugin-api/"
cp -r plugins/example/src "$RELEASE_DIR/plugin-api/example-plugin/source"
cp dist/plugins/HelloPlugin.jar "$RELEASE_DIR/plugin-api/example-plugin/" 2>/dev/null || true

echo "Done. Development jars are in dist/, the release bundle is in $RELEASE_DIR."
