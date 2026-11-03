#!/bin/sh
# Render headless PNG previews from the real pure settings layout classes.
#
# The Minecraft client is intentionally not started. Gradle first compiles the mod's real layout model; this tool then
# compiles its Java2D renderer against those classes and writes inspection-only PNGs under build/ (never dist/).
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
OUTPUT=${1:-"$ROOT/build/apple-layout-previews"}
CLASSES="$ROOT/build/classes/main"
TOOL_OUT="$ROOT/build/apple-layout-renderer"
SOURCE="$ROOT/tools/apple-layout-renderer/AppleSettingsLayoutRenderer.java"

if [ ! -d "$CLASSES" ]; then
    (cd "$ROOT" && ./gradlew classes)
fi

rm -rf "$TOOL_OUT" "$OUTPUT"
mkdir -p "$TOOL_OUT/classes" "$OUTPUT"

javac -encoding UTF-8 -cp "$CLASSES" -d "$TOOL_OUT/classes" "$SOURCE"
java -Djava.awt.headless=true -cp "$TOOL_OUT/classes:$CLASSES" AppleSettingsLayoutRenderer "$OUTPUT"

count=$(find "$OUTPUT" -maxdepth 1 -type f -name '*.png' | wc -l | tr -d ' ')
if [ "$count" -ne 3 ]; then
    echo "expected exactly three Apple layout PNGs; found $count" >&2
    exit 1
fi
find "$OUTPUT" -maxdepth 1 -type f -name '*.png' -printf '%f %s bytes\n' | sort
