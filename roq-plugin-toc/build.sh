#!/usr/bin/env bash
#
# Builds the Roq TOC plugin from rolfedh/quarkus-roq PR #776 at a pinned commit,
# using standalone POMs that decouple it from the Roq monorepo build.
#
# Usage:
#   ./roq-plugin-toc/build.sh          # fetch, build, and install to local Maven repo
#
# The plugin is then available as:
#   io.quarkiverse.roq:quarkus-roq-plugin-toc:1.0.0-SNAPSHOT
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
UPSTREAM_REPO="https://github.com/rolfedh/quarkus-roq.git"
UPSTREAM_COMMIT="c52d3ec79f3d6dcb4ff57c495666b60bcd98bf94"

WORK_DIR=$(mktemp -d)
trap 'rm -rf "$WORK_DIR"' EXIT

echo "Fetching TOC plugin source from ${UPSTREAM_REPO} at ${UPSTREAM_COMMIT}..."
git clone --no-checkout --filter=blob:none "$UPSTREAM_REPO" "$WORK_DIR/repo" 2>/dev/null
git -C "$WORK_DIR/repo" checkout "$UPSTREAM_COMMIT" -- roq-plugin/toc 2>/dev/null

SRC="$WORK_DIR/repo/roq-plugin/toc"

echo "Copying source into standalone build structure..."
# Runtime sources
mkdir -p "$SCRIPT_DIR/runtime/src/main/java" "$SCRIPT_DIR/runtime/src/test/java"
cp -r "$SRC/runtime/src/main/java/"* "$SCRIPT_DIR/runtime/src/main/java/"
cp -r "$SRC/runtime/src/test/java/"* "$SCRIPT_DIR/runtime/src/test/java/"

# Deployment sources
mkdir -p "$SCRIPT_DIR/deployment/src/main/java"
cp -r "$SRC/deployment/src/main/java/"* "$SCRIPT_DIR/deployment/src/main/java/"

echo "Patching default TOC depth to match AsciiDoc :toclevels: default..."
EXTENSION_FILE="$SCRIPT_DIR/runtime/src/main/java/io/quarkiverse/roq/plugin/toc/runtime/RoqPluginTocTemplateExtension.java"
sed 's/getInteger("content-toc-levels", 6)/getInteger("content-toc-levels", page.data().getInteger("toclevels", 3))/' \
  "$EXTENSION_FILE" > "$EXTENSION_FILE.tmp" && mv "$EXTENSION_FILE.tmp" "$EXTENSION_FILE"

echo "Building and installing TOC plugin..."
mvn -B install -f "$SCRIPT_DIR/pom.xml"

echo "Cleaning fetched sources..."
rm -rf "$SCRIPT_DIR/runtime/src/main/java/io" "$SCRIPT_DIR/runtime/src/test/java/io"
rm -rf "$SCRIPT_DIR/deployment/src/main/java/io"

echo "Done. quarkus-roq-plugin-toc:1.0.0-SNAPSHOT installed to local Maven repo."
