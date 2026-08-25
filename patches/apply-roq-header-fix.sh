#!/usr/bin/env bash
# Patches AsciidocHeaderParser to fix binary incompatibility with asciidoc-java 1.2.16:
#   Header.author() changed from Author to List<Author>
#
# Fixes: https://github.com/yupiik/tools-maven-plugin/issues/92
# Upstream: https://github.com/quarkiverse/quarkus-roq/pull/1184
# TODO: Remove with Roq 2.1.8
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Extract the Roq version from pom.xml
ROQ_VERSION=$(grep -m1 -oP '(?<=quarkus-roq-plugin-asciidoc-jruby</artifactId>\s{0,99}<version>)[^<]+' "$PROJECT_DIR/pom.xml" \
  || grep -oP '(?<=quarkus-roq</artifactId>\s{0,99}<version>)[^<]+' "$PROJECT_DIR/pom.xml" \
  || echo "2.1.7")

M2="$HOME/.m2/repository"
TARGET_JAR="$M2/io/quarkiverse/roq/quarkus-roq-plugin-asciidoc-common-deployment/$ROQ_VERSION/quarkus-roq-plugin-asciidoc-common-deployment-$ROQ_VERSION.jar"
MARKER="$TARGET_JAR.patched-header-author"

if [ -f "$MARKER" ]; then
  echo "AsciidocHeaderParser already patched, skipping"
  exit 0
fi

# The target jar and some compile deps are Quarkus deployment artifacts
# (not regular transitive deps), so download them explicitly if missing.
for gav in \
  "io.quarkiverse.roq:quarkus-roq-plugin-asciidoc-common-deployment:$ROQ_VERSION" \
  "io.quarkiverse.roq:quarkus-roq-frontmatter-deployment:$ROQ_VERSION" \
  "io.yupiik.maven:asciidoc-java:1.2.16"; do
  IFS=: read -r g a v <<< "$gav"
  jar_path="$M2/$(echo "$g" | tr '.' '/')/$a/$v/$a-$v.jar"
  if [ ! -f "$jar_path" ]; then
    echo "Downloading $gav..."
    mvn -B dependency:get -Dartifact="$gav" -q
  fi
done

if [ ! -f "$TARGET_JAR" ]; then
  echo "Target JAR not found at $TARGET_JAR"
  exit 1
fi

WORK_DIR=$(mktemp -d)
trap 'rm -rf "$WORK_DIR"' EXIT

# Build the compilation classpath from resolved dependencies
ASCIIDOC_JAVA=$(find "$M2/io/yupiik/maven/asciidoc-java" -name 'asciidoc-java-1.2.16.jar' | head -1)
FRONTMATTER_DEPLOYMENT=$(find "$M2/io/quarkiverse/roq/quarkus-roq-frontmatter-deployment" -name "*.jar" -not -name '*sources*' | sort -V | tail -1)
FRONTMATTER_RUNTIME=$(find "$M2/io/quarkiverse/roq/quarkus-roq-frontmatter" -name "quarkus-roq-frontmatter-$ROQ_VERSION.jar" | head -1)
VERTX_CORE=$(find "$M2/io/vertx/vertx-core" -name '*.jar' -not -name '*sources*' | sort -V | tail -1)
JBOSS_LOGGING=$(find "$M2/org/jboss/logging/jboss-logging" -name '*.jar' -not -name '*sources*' | sort -V | tail -1)

for dep in ASCIIDOC_JAVA FRONTMATTER_DEPLOYMENT FRONTMATTER_RUNTIME VERTX_CORE JBOSS_LOGGING; do
  if [ -z "${!dep}" ]; then
    echo "Missing dependency: $dep"
    exit 1
  fi
done

CP="$TARGET_JAR:$ASCIIDOC_JAVA:$FRONTMATTER_DEPLOYMENT:$FRONTMATTER_RUNTIME:$VERTX_CORE:$JBOSS_LOGGING"

echo "Compiling patched AsciidocHeaderParser..."
javac --release 21 -cp "$CP" -d "$WORK_DIR" "$SCRIPT_DIR/AsciidocHeaderParser.java"

echo "Patching $TARGET_JAR..."
# Update all generated class files (main class + inner classes)
cd "$WORK_DIR"
for classfile in $(find . -name 'AsciidocHeaderParser*.class'); do
  jar uf "$TARGET_JAR" "$classfile"
done

touch "$MARKER"
echo "AsciidocHeaderParser patched successfully"
