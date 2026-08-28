#!/bin/bash
SCRIPTDIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd) || exit 1
cd "$SCRIPTDIR" || exit 1

for cmd in find curl grep sed sort comm head basename date wc tr; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "ERROR: required command not found: $cmd"
    exit 1
  fi
done

PREVIEW_REF="$SCRIPTDIR/.blog-preview-last-run"
PORT="${QUARKUS_HTTP_PORT:-8080}"
BASE_URL="http://127.0.0.1:${PORT}"

# --- Environment detection ---

if command -v wslview >/dev/null 2>&1; then BROWSER_CMD="wslview"
elif command -v xdg-open >/dev/null 2>&1; then BROWSER_CMD="xdg-open"
elif command -v open >/dev/null 2>&1; then BROWSER_CMD="open"
else BROWSER_CMD=""; fi

# --- Post detection helpers ---

all_posts_after() {
  local ref="$1"
  [ -d "$SCRIPTDIR/content/posts" ] || return 0
  [ -f "$ref" ] || return 0
  find "$SCRIPTDIR/content/posts" -maxdepth 1 \
    \( -name '*.adoc' -o -name '*.asciidoc' -o -name '*.md' \) \
    -newer "$ref" -print 2>/dev/null
}

my_changed_posts() {
  {
    git -C "$SCRIPTDIR" diff --name-only --diff-filter=AM HEAD -- 'content/posts/' 2>/dev/null
    git -C "$SCRIPTDIR" diff --name-only --diff-filter=AM --cached HEAD -- 'content/posts/' 2>/dev/null
    git -C "$SCRIPTDIR" log -1 --diff-filter=AM --name-only --pretty=format: HEAD -- 'content/posts/' 2>/dev/null
  } | grep -E '\.(asciidoc|adoc|md)$' | sort -u \
    | while IFS= read -r f; do
        [ -f "$SCRIPTDIR/$f" ] && echo "$SCRIPTDIR/$f"
      done
}

post_slug() {
  local filename
  filename=$(basename "$1")
  echo "$filename" | sed 's/^[0-9]\{4\}-[0-9]\{2\}-[0-9]\{2\}-//; s/\.\(asciidoc\|adoc\|md\)$//'
}

# --- Start dev server in background ---

echo "=== Starting preview server (noguides profile) ==="
QUARKUS_HTTP_PORT="$PORT" QUARKUS_PROFILE=noguides mvn quarkus:dev "$@" &
SERVER_PID=$!

cleanup() {
  kill "$SERVER_PID" 2>/dev/null
  wait "$SERVER_PID" 2>/dev/null
}
trap cleanup EXIT INT TERM

# --- Wait for server to be ready ---

echo "  Waiting for server on port ${PORT}..."
START=$(date +%s)
while true; do
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then
    echo "  Server process exited unexpectedly."
    exit 1
  fi
  if [ "$(curl -s -o /dev/null -w '%{http_code}' "${BASE_URL}" 2>/dev/null)" = "200" ]; then
    ELAPSED=$(( $(date +%s) - START ))
    printf '  Ready in %ds\n' "$ELAPSED"
    break
  fi
  if [ -t 1 ]; then
    printf '\r  Waiting... %ds ' "$(( $(date +%s) - START ))"
  fi
  sleep 2
done

# --- Detect changed posts ---

CHANGED_POSTS=""

if [ -f "$PREVIEW_REF" ]; then
  CHANGED_POSTS=$(all_posts_after "$PREVIEW_REF")
elif command -v git >/dev/null 2>&1 \
    && git -C "$SCRIPTDIR" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  CHANGED_POSTS=$(my_changed_posts)
fi

POST_COUNT=0
if [ -n "$CHANGED_POSTS" ]; then
  POST_COUNT=$(printf '%s\n' "$CHANGED_POSTS" | wc -l | tr -d ' ')
fi

if [ "$POST_COUNT" -ge 5 ] \
    && command -v git >/dev/null 2>&1 \
    && git -C "$SCRIPTDIR" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  GIT_POSTS=$(my_changed_posts)
  if [ -n "$GIT_POSTS" ]; then
    INTERSECTED=$(comm -12 \
      <(printf '%s\n' "$CHANGED_POSTS" | sort) \
      <(printf '%s\n' "$GIT_POSTS" | sort))
    if [ -n "$INTERSECTED" ]; then
      INT_COUNT=$(printf '%s\n' "$INTERSECTED" | wc -l | tr -d ' ')
      if [ "$INT_COUNT" -lt "$POST_COUNT" ]; then
        CHANGED_POSTS="$INTERSECTED"
        POST_COUNT="$INT_COUNT"
      fi
    fi
  fi
fi

PREVIEW_URLS=("${BASE_URL}/blog/")

if [ "$POST_COUNT" -eq 1 ]; then
  POST_FILE=$(printf '%s\n' "$CHANGED_POSTS" | head -1)
  SLUG=$(post_slug "$POST_FILE")
  PREVIEW_URLS+=("${BASE_URL}/blog/${SLUG}/")
  echo "Detected: blog post ($(basename "$POST_FILE"))"
elif [ "$POST_COUNT" -ge 2 ] && [ "$POST_COUNT" -le 4 ]; then
  echo "Detected: $POST_COUNT posts modified"
  while IFS= read -r pf; do
    SLUG=$(post_slug "$pf")
    PREVIEW_URLS+=("${BASE_URL}/blog/${SLUG}/")
    echo "  - $(basename "$pf")"
  done <<< "$CHANGED_POSTS"
elif [ "$POST_COUNT" -ge 5 ]; then
  echo "Detected: $POST_COUNT posts modified."
else
  echo "No recent changes detected."
fi

touch "$PREVIEW_REF" || true

echo ""
if [ -n "$BROWSER_CMD" ]; then
  for url in "${PREVIEW_URLS[@]}"; do
    echo "Opening: $url"
    "$BROWSER_CMD" "$url" 2>/dev/null
    sleep 1
  done
else
  echo "Open in your browser:"
  for url in "${PREVIEW_URLS[@]}"; do
    echo "  $url"
  done
fi

echo ""
echo "Preview is running. Edit a post and save — Roq will rebuild automatically."
echo "Press Ctrl-C to stop."
echo ""

# Bring server to foreground so Ctrl-C reaches it naturally
wait "$SERVER_PID"
