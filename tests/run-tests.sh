#!/bin/sh
# Compiles and runs every standalone logic/config/layout/migration test. No game is started.
#
# Needs a JDK 8, compiled classes (build/classes/main), and either TEST_CP (the complete Forge test classpath)
# or the MC_JAR, LOG4J_JAR and GSON_JAR paths used by the legacy invocation.
# Example after `gradle build dumpTestClasspath`:
#   TEST_CP="$(cat build/test-classpath.txt)" sh tests/run-tests.sh
set -e
HERE=$(cd "$(dirname "$0")/.." && pwd)
CLASSES="${CLASSES:-$HERE/build/classes/main}"
OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT
if [ -n "${TEST_CP:-}" ]; then
  CP="$CLASSES:$TEST_CP"
else
  : "${MC_JAR:?set MC_JAR to minecraft_merged-1.8.9.jar, or set TEST_CP}"
  : "${LOG4J_JAR:?set LOG4J_JAR to the log4j-api jar, or set TEST_CP}"
  : "${GSON_JAR:?set GSON_JAR to the gson jar, or set TEST_CP}"
  CP="$CLASSES:$MC_JAR:$LOG4J_JAR:$GSON_JAR"
fi

# Compile every test first, including source-layout contracts. This catches a stale retained-test reference before
# any individual suite starts. A response file avoids shell argument limits on the deliberately broad merged suite.
find "$HERE/tests" -name '*.java' -type f | sort > "$OUT/sources.txt"
javac -nowarn -encoding UTF-8 -cp "$CP" -d "$OUT" @"$OUT/sources.txt"

# Every test class with a public main is executed. The class name is derived from its package-shaped path, so a
# retained friend regression cannot silently be omitted when a new test is added.
COUNT=0
while IFS= read -r source; do
  if grep -q 'public[[:space:]]\+static[[:space:]]\+void[[:space:]]\+main' "$source"; then
    relative=${source#"$HERE/tests/"}
    class=${relative%.java}
    class=$(printf '%s' "$class" | tr '/' '.')
    echo "=== $class ==="
    java -cp "$OUT:$CP" "$class"
    COUNT=$((COUNT + 1))
  fi
done < "$OUT/sources.txt"
echo "All Lunaria standalone tests passed ($COUNT suites)."
