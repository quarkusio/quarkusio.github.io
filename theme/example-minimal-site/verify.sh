#!/bin/bash
# Checks that the example site generated with the packaged theme jar renders the theme.
# Usage: verify.sh <generated site dir>
SITE=${1:-target/roq}
INDEX="$SITE/index.html"
status=0
check() {
  if ! grep -q "$2" "$3"; then
    echo "FAIL: $1 (expected '$2' in $3)"
    status=1
  fi
}
check "theme layout" '<div class="content">' "$INDEX"
check "page content" 'class="example-title">Quarkus theme example' "$INDEX"
check "header" 'class="nav-wrapper"' "$INDEX"
check "navigation menus" '>WHAT IS QUARKUS?</a>' "$INDEX"
check "menu links to quarkus.io" '<a href="https://quarkus.io/about"' "$INDEX"
check "call to action" 'button-cta secondary white">START CODING' "$INDEX"
check "colour mode toggle" "id='theme-toggle'" "$INDEX"
check "footer" 'Example footer text' "$INDEX"
check "minimal footer" 'class="content cf-footer"' "$INDEX"
check "theme styles" '.nav-wrapper' "$(ls "$SITE"/static/bundle/*.css | head -1)"
check "theme scripts" 'function setTheme' "$SITE/assets/javascript/colormode.js"
check "theme images" '' "$SITE/assets/images/quarkus_logo_horizontal_rgb_reverse.svg"
if grep -q '{=' "$INDEX"; then
  echo "FAIL: unprocessed {=...} expressions (is templates/.qute missing from the jar?)"
  status=1
fi
[ $status -eq 0 ] && echo "Theme example site OK"
exit $status
