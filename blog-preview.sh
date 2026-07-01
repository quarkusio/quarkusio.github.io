#!/bin/bash
SCRIPTDIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd) || exit 1
cd "$SCRIPTDIR" || exit 1

for cmd in find mktemp curl grep sed sort comm head tail basename date seq tee wc tr; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "ERROR: required command not found: $cmd"
    exit 1
  fi
done

find "${TMPDIR:-/tmp}" -maxdepth 1 -name 'quarkus-blog-build-*' -type d -mtime +7 -exec rm -rf {} + 2>/dev/null || true
LOGDIR=$(mktemp -d "${TMPDIR:-/tmp}/quarkus-blog-build-XXXXXX") || exit 1

CONTAINER_NAME="quarkus-blog-preview"
JEKYLL_LOCAL_IMAGE="quarkus-blog-jekyll:local"

PREVIEW_REF="$SCRIPTDIR/.blog-preview-last-run"
LOCK_DIR="$SCRIPTDIR/.blog-preview.lock"

# --- Interrupt handling ---

if ! mkdir "$LOCK_DIR" 2>/dev/null; then
  LOCK_PID=$(cat "$LOCK_DIR/pid" 2>/dev/null)
  if [ -n "$LOCK_PID" ] && kill -0 "$LOCK_PID" 2>/dev/null; then
    echo "Another blog preview run is already active (PID $LOCK_PID)."
    echo "If this is stale, remove: $LOCK_DIR"
    exit 1
  fi
  echo "Removing stale lock (PID ${LOCK_PID:-unknown} no longer running)."
  rm -rf "$LOCK_DIR"
  mkdir "$LOCK_DIR" || exit 1
fi

cleanup_on_exit() {
  rm -rf "$LOCK_DIR"
}
cleanup_on_interrupt() {
  echo ""
  echo "Interrupted."
  rm -rf "$LOCK_DIR"
  exit 130
}
trap cleanup_on_exit EXIT
trap cleanup_on_interrupt INT TERM

echo "$$" > "$LOCK_DIR/pid"

# --- Environment detection ---

echo "=== Environment ==="

if command -v podman >/dev/null 2>&1; then CONTAINER_CMD="podman"
elif command -v docker >/dev/null 2>&1; then CONTAINER_CMD="docker"
else echo "ERROR: neither podman nor docker found"; exit 1; fi

if command -v getenforce >/dev/null 2>&1 && [ "$(getenforce 2>/dev/null)" != "Disabled" ]; then
  VOL_FLAG=":z"
else
  VOL_FLAG=""
fi

if command -v wslview >/dev/null 2>&1; then BROWSER_CMD="wslview"
elif command -v xdg-open >/dev/null 2>&1; then BROWSER_CMD="xdg-open"
elif command -v open >/dev/null 2>&1; then BROWSER_CMD="open"
else BROWSER_CMD=""; fi

echo "Container:  $CONTAINER_CMD"
echo "SELinux:    ${VOL_FLAG:-none}"
echo "Browser:    ${BROWSER_CMD:-manual}"
echo "Logs:       $LOGDIR"
echo ""

# --- Container reuse check ---

container_running() {
  [ "$($CONTAINER_CMD inspect -f '{{.State.Running}}' "$CONTAINER_NAME" 2>/dev/null)" = "true" ]
}

preview_ready() {
  [ "$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:4000 2>/dev/null)" = "200" ]
}

PREVIEW_ALREADY_RUNNING=false
if container_running && preview_ready; then
  PREVIEW_ALREADY_RUNNING=true
fi

# --- Port conflict check ---

port_in_use() {
  if command -v lsof >/dev/null 2>&1; then
    lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1
    return $?
  fi
  if command -v nc >/dev/null 2>&1; then
    nc -z 127.0.0.1 "$1" 2>/dev/null
    return $?
  fi
  curl --max-time 1 -s -o /dev/null "http://127.0.0.1:$1" 2>/dev/null
}

if [ "$PREVIEW_ALREADY_RUNNING" != "true" ]; then
  if port_in_use 4000 || port_in_use 35729; then
    if container_running; then
      echo "Stale preview container detected. Removing..."
      $CONTAINER_CMD rm -f "$CONTAINER_NAME" 2>/dev/null || true
      sleep 1
      if port_in_use 4000 || port_in_use 35729; then
        echo "Port 4000 or 35729 is still in use after removing stale container."
        echo "Free the port and try again."
        exit 1
      fi
    else
      echo "Port 4000 or 35729 is already in use by another process."
      echo "Free the port and try again."
      exit 1
    fi
  fi
fi

START_TOTAL=$(date +%s)

if [ "$CONTAINER_CMD" = "podman" ]; then
  podman unshare rm -rf .cache/ 2>/dev/null || rm -rf .cache/ 2>/dev/null || true
else
  rm -rf .cache/ 2>/dev/null || true
fi

# --- Container startup ---

echo "=== Preview server ==="

if [ "$PREVIEW_ALREADY_RUNNING" = "true" ]; then
  if container_running && preview_ready; then
    echo "  Preview server is already running. Reusing it."
  else
    echo "  Previously running preview server is no longer ready. Restarting."
    PREVIEW_ALREADY_RUNNING=false
  fi
fi

if [ "$PREVIEW_ALREADY_RUNNING" != "true" ]; then
  $CONTAINER_CMD rm -f "$CONTAINER_NAME" 2>/dev/null || true

  start_jekyll() {
    local image="$1" mount="$2" run_log="$3"
    if ! $CONTAINER_CMD run -d --name "$CONTAINER_NAME" \
      -p 127.0.0.1:4000:4000 -p 127.0.0.1:35729:35729 \
      -v "$(pwd):${mount}${VOL_FLAG}" \
      -v quarkus-blog-jekyll-bundles:/usr/local/bundle \
      "$image" \
      bundle exec jekyll serve --host 0.0.0.0 \
      --livereload --incremental --future \
      --config _config.yml,_config_dev.yml,_noguides_config.yml \
      > "$run_log" 2>&1; then
      return 1
    fi
    local _i
    for _i in $(seq 1 10); do
      if [ "$($CONTAINER_CMD inspect -f '{{.State.Running}}' "$CONTAINER_NAME" 2>/dev/null)" = "true" ]; then
        return 0
      fi
      sleep 1
    done
    $CONTAINER_CMD logs "$CONTAINER_NAME" >> "$run_log" 2>&1
    return 1
  }

  if [ ! -d "jekyll-container" ] || [ ! -f "jekyll-container/Dockerfile" ]; then
    echo "  ERROR: jekyll-container/Dockerfile not found."
    echo "  This file is required to build the Jekyll preview image."
    exit 1
  fi

  echo "  Checking Jekyll image (build cache)..."
  if ! $CONTAINER_CMD build -t "$JEKYLL_LOCAL_IMAGE" jekyll-container/ > "$LOGDIR/build.log" 2>&1; then
    echo "  Image build failed. Check $LOGDIR/build.log"
    exit 1
  fi

  if ! start_jekyll "$JEKYLL_LOCAL_IMAGE" "/site" "$LOGDIR/start.log"; then
    echo "  Container failed to start. Check $LOGDIR/start.log"
    $CONTAINER_CMD rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
    exit 1
  fi

  echo "  Waiting for Jekyll to generate the site..."
  for i in $(seq 1 150); do
    if [ "$($CONTAINER_CMD inspect -f '{{.State.Running}}' "$CONTAINER_NAME" 2>/dev/null)" != "true" ]; then
      echo "  Container exited during startup."
      $CONTAINER_CMD logs "$CONTAINER_NAME" 2>&1 | tail -50 | tee "$LOGDIR/crash.log"
      $CONTAINER_CMD rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
      exit 1
    fi
    if [ "$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:4000 2>/dev/null)" = "200" ]; then
      local_elapsed=$(( $(date +%s) - START_TOTAL ))
      printf '  Preview server ready in %ds\n' "$local_elapsed"
      break
    fi
    if [ "$i" -eq 150 ]; then
      echo "  Timeout after 300s."
      $CONTAINER_CMD logs "$CONTAINER_NAME" 2>&1 | tail -50 | tee "$LOGDIR/timeout.log"
      $CONTAINER_CMD rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
      exit 1
    fi
    local_elapsed=$(( $(date +%s) - START_TOTAL ))
    if [ -t 1 ]; then
      printf '\r  Waiting for Jekyll... %ds ' "$local_elapsed"
    else
      printf '  Waiting for Jekyll... %ds\n' "$local_elapsed"
    fi
    sleep 2
  done
fi

# --- Detect changes and open browser ---

echo ""
END_TOTAL=$(date +%s)
TOTAL_TIME=$((END_TOTAL - START_TOTAL))

echo "==============================="
echo "Done in ${TOTAL_TIME}s"
echo "==============================="
echo ""

all_posts_after() {
  local ref="$1"
  [ -d "$SCRIPTDIR/_posts" ] || return 0
  [ -f "$ref" ] || return 0
  find "$SCRIPTDIR/_posts" -maxdepth 1 \
    \( -name '*.adoc' -o -name '*.asciidoc' -o -name '*.md' \) \
    -newer "$ref" -print 2>/dev/null
}

my_changed_posts() {
  {
    git -C "$SCRIPTDIR" diff --name-only --diff-filter=AM HEAD -- '_posts/' 2>/dev/null
    git -C "$SCRIPTDIR" diff --name-only --diff-filter=AM --cached HEAD -- '_posts/' 2>/dev/null
    git -C "$SCRIPTDIR" log -1 --diff-filter=AM --name-only --pretty=format: HEAD -- '_posts/' 2>/dev/null
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

PREVIEW_URLS=()
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

PREVIEW_URLS=("http://127.0.0.1:4000/blog/")

if [ "$POST_COUNT" -eq 1 ]; then
  POST_FILE=$(echo "$CHANGED_POSTS" | head -1)
  SLUG=$(post_slug "$POST_FILE")
  PREVIEW_URLS+=("http://127.0.0.1:4000/blog/${SLUG}/")
  echo "Detected: blog post ($(basename "$POST_FILE"))"
elif [ "$POST_COUNT" -ge 2 ] && [ "$POST_COUNT" -le 4 ]; then
  echo "Detected: $POST_COUNT posts modified"
  while IFS= read -r pf; do
    SLUG=$(post_slug "$pf")
    PREVIEW_URLS+=("http://127.0.0.1:4000/blog/${SLUG}/")
    echo "  - $(basename "$pf")"
  done <<< "$CHANGED_POSTS"
elif [ "$POST_COUNT" -ge 5 ]; then
  echo "Detected: $POST_COUNT posts modified."
else
  echo "No recent changes detected."
fi

touch "$PREVIEW_REF" || exit 1

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
echo "Preview is ready. LiveReload is active on port 35729."
echo "Edit a post, save, and the browser will refresh automatically."
echo ""
echo "Container: $CONTAINER_NAME"
echo "Stop:      $CONTAINER_CMD rm -f $CONTAINER_NAME"
echo "Logs:      $LOGDIR"
