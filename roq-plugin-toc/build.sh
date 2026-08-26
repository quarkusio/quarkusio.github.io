#!/usr/bin/env bash
#
# Builds the Roq TOC plugin from rolfedh/quarkus-roq PR #776 at a pinned commit,
# using standalone POMs that decouple it from the Roq monorepo build.
#
# The pinned commit reads each guide's AsciiDoc :toclevels: attribute and registers only
# its two template extensions, so the sources are built unmodified.
#
# Usage:
#   ./roq-plugin-toc/build.sh          # fetch, build, and install to local Maven repo
#
# Set UPSTREAM_REPO and UPSTREAM_COMMIT to build from a local checkout instead, e.g.
#   UPSTREAM_REPO=file:///path/to/quarkus-roq \
#   UPSTREAM_COMMIT=$(git -C /path/to/quarkus-roq rev-parse fix-toc) ./roq-plugin-toc/build.sh
#
# The plugin is then available as:
#   io.quarkiverse.roq:quarkus-roq-plugin-toc:1.0.0-SNAPSHOT
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
UPSTREAM_REPO="${UPSTREAM_REPO:-https://github.com/rolfedh/quarkus-roq.git}"
UPSTREAM_COMMIT="${UPSTREAM_COMMIT:-07015acc83022f964563e1b2cb2a8b5704c7cd94}"

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

echo "Building and installing TOC plugin..."
mvn -B install -f "$SCRIPT_DIR/pom.xml"

echo "Cleaning fetched sources..."
rm -rf "$SCRIPT_DIR/runtime/src/main/java/io" "$SCRIPT_DIR/runtime/src/test/java/io"
rm -rf "$SCRIPT_DIR/deployment/src/main/java/io"

echo "Done. quarkus-roq-plugin-toc:1.0.0-SNAPSHOT installed to local Maven repo."
