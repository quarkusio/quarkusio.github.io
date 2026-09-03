#!/usr/bin/env bash
# Patches the Roq AsciidocJInclude class to fix AsciiDoc tag handling:
#   - Split tags on ";" (not just ",")
#   - Support "!" negation (e.g. tags=example;!ignore)
#
# Fixes: https://github.com/quarkusio/quarkusio.github.io/issues/2939
# Upstream: https://github.com/quarkiverse/quarkus-roq/issues/1178
# TODO: Remove once Roq ships the fix and we upgrade
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Extract the Roq version from pom.xml
ROQ_VERSION=$(grep -m1 -oP '(?<=quarkus-roq-plugin-asciidoc-jruby</artifactId>\s{0,99}<version>)[^<]+' "$PROJECT_DIR/pom.xml" \
  || grep -oP '(?<=quarkus-roq</artifactId>\s{0,99}<version>)[^<]+' "$PROJECT_DIR/pom.xml" \
  || echo "2.1.9")

ROQ_JAR="$HOME/.m2/repository/io/quarkiverse/roq/quarkus-roq-plugin-asciidoc-jruby/$ROQ_VERSION/quarkus-roq-plugin-asciidoc-jruby-$ROQ_VERSION.jar"
MARKER="$ROQ_JAR.patched-issue-2939"

if [ ! -f "$ROQ_JAR" ]; then
  echo "Roq JAR not found at $ROQ_JAR — run 'mvn dependency:resolve' first"
  exit 1
fi

if [ -f "$MARKER" ]; then
  echo "Roq JAR already patched, skipping"
  exit 0
fi

WORK_DIR=$(mktemp -d)
trap 'rm -rf "$WORK_DIR"' EXIT

# Build the compilation classpath from the existing JAR's dependencies
ASCIIDOCTORJ_API=$(find "$HOME/.m2/repository/org/asciidoctor/asciidoctorj-api" -name '*.jar' -not -name '*sources*' | sort -V | tail -1)
STRING_PATHS=$(find "$HOME/.m2/repository/io/quarkiverse/tools/string-paths" -name '*.jar' -not -name '*sources*' | sort -V | tail -1)

if [ -z "$ASCIIDOCTORJ_API" ] || [ -z "$STRING_PATHS" ]; then
  echo "Missing compilation dependencies — run 'mvn dependency:resolve' first"
  exit 1
fi

CP="$ROQ_JAR:$ASCIIDOCTORJ_API:$STRING_PATHS"

echo "Compiling patched AsciidocJInclude..."
javac --release 21 -cp "$CP" -d "$WORK_DIR" "$SCRIPT_DIR/AsciidocJInclude.java"

echo "Patching $ROQ_JAR..."
jar uf "$ROQ_JAR" -C "$WORK_DIR" io/quarkiverse/roq/plugin/asciidoctorj/runtime/AsciidocJInclude.class

touch "$MARKER"
echo "Roq JAR patched successfully"
