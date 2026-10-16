#!/bin/sh
# Offline verification of the Terminal GUI (gui/TerminalScreen and dungeon/TerminalSolver).
#
# The harness drives the real screen class: it calls the same drawScreen, mouseClicked and keyTyped
# methods Minecraft calls, with a pixel canvas instead of OpenGL and stub Minecraft API classes
# instead of the game. It checks that the fourteen numbered buttons are drawn, that every button has
# a hit box, that clicks land on the chest slot of the right number, and that the transparency
# toggle, the timer, R and Esc behave.
#
# Needs a JDK that can compile for Java 8 (JDK 8 is what the project uses) and python3 for the stub
# generator. Run it from this folder:
#
#   sh run.sh              # verify the current source
#   sh run.sh before       # run the same checks against an older TerminalScreen, to see the bug
#   SRC=/path/to/TerminalScreen.java SRC2=/path/to/TerminalSolver.java sh run.sh before
#
# Results are written to out/ after each run: report-after.txt, report-before-all.txt and the
# rendered pictures.

set -e
HERE=$(cd "$(dirname "$0")" && pwd)
cd "$HERE"

MODE=${1:-after}
JAVAC=${JAVAC:-javac}
JAVA=${JAVA:-java}

# The source under test. Defaults to this project's own files.
SRC=${SRC:-$HERE/../../src/main/java/com/arena/pekaclient/gui/TerminalScreen.java}
SRC2=${SRC2:-$HERE/../../src/main/java/com/arena/pekaclient/dungeon/TerminalSolver.java}

if [ "$MODE" = "before" ] || [ "$MODE" = "before-all" ]; then
  MODE_ARG=before-all
else
  MODE_ARG=after
fi

OUT="$HERE/out/$MODE"
rm -rf "$OUT"
mkdir -p "$OUT"

echo "source under test: $SRC"
"$JAVAC" -source 1.8 -target 1.8 -nowarn -encoding UTF-8 -d "$OUT" \
  $(find "$HERE/stubs" "$HERE/harness" -name '*.java') "$SRC" "$SRC2"

"$JAVA" -cp "$OUT" com.arena.pekaclient.gui.TerminalVerify "$MODE_ARG" "$OUT"
