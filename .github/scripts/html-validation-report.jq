# Summarizes the W3C Nu Html Checker's JSON output (--format json) for the generated site, comparing it with a
# baseline of known ("grandfathered") errors, so that only new errors fail the build.
#
#   jq -r --arg site <dir> --arg mode <mode> --rawfile baseline <baseline file> -f html-validation-report.jq results.json
#
# Modes:
#   log        every error as "page:line: message", with known errors marked as such
#   summary    a Markdown report, new errors first
#   count-new  the number of errors that are not in the baseline
#   baseline   a new baseline file holding every current error (pass an empty baseline, such as /dev/null)
#
# An error is identified by its message, without the quoted values in it, and the markup around it, reduced to its
# tags: attribute values, the text between tags and the partial fragments at either end of the checker's snippet are
# dropped. So an error in a template is one baseline entry however many pages show it, including pages added later,
# and editing nearby text does not make a known error new.

def page:
  (.url // "") | sub("^file:"; "")
  | split($site) | (if length > 1 then .[1:] | join($site) else .[0] end)
  | sub("index\\.html$"; "")
  | if . == "" then "/" else . end;

def one_line($max):
  (. // "") | gsub("\\s+"; " ") | ltrimstr(" ") | rtrimstr(" ")
  | if length > $max then .[0:($max - 1)] + "…" else . end;

def md: gsub("\\|"; "\\|") | gsub("<"; "&lt;") | gsub(">"; "&gt;");

# The tags in the checker's snippet, without attribute values, the text between tags, or the partial fragments at
# either end
def skeleton:
  # Regular expressions rather than index/rindex, which count bytes instead of characters in jq 1.7
  (. // "") | gsub("\\s+"; " ")
  | sub("^[^<]*"; "") | sub("[^>]*$"; "")
  | gsub(">[^<]*<"; "><")
  | gsub("=\"[^\"]*\""; "=\"\"") | gsub("='[^']*'"; "=''");

# Quoted values in messages, such as a bad URL holding a post title, are dropped too: the markup identifies the error
def key: "\(.message | one_line(1000) | gsub("“[^”]*”"; "“…”"))\t\(.markup)";

($baseline | split("\n") | map(select(length > 0 and (startswith("#") | not))) | unique) as $known
| ($known | map({(.): true}) | add // {}) as $is_known
| [.messages[] | select(.type == "error")
  | {page: page, line: (.lastLine // "?"), message: (.message // ""), extract: (.extract | one_line(100)),
     markup: (.extract | skeleton)}
  | . + {known: ($is_known[key] // false)}]
| . as $errors
| ($errors | map(select(.known | not))) as $new
| ($errors | map(select(.known))) as $old

# Groups errors by message and markup, then merges groups with the same message on exactly the same pages: those
# almost always come from one template line, for example a stray tag inside a loop that renders many links.
| def groups:
    group_by([.message, .extract])
    | map({message: .[0].message, extract: .[0].extract, hits: length,
           pagelist: (map(.page) | unique), example: "\(.[0].page):\(.[0].line)"})
    | group_by([.message, .pagelist])
    | map({message: .[0].message, hits: (map(.hits) | add), pages: (.[0].pagelist | length),
           extract: (.[0].extract + (if length > 1 then " (and \(length - 1) similar)" else "" end)),
           example: .[0].example})
    | sort_by(-.pages, -.hits);
  def table($rows):
    ["| Pages | Errors | Message | Markup | Example |", "|---:|---:|---|---|---|"]
    + ($rows[0:50] | map("| \(.pages) | \(.hits) | \(.message | one_line(150) | md) | <code>\(.extract | md)</code> | \(.example | md) |"))
    + (if ($rows | length) > 50 then ["", "…and \(($rows | length) - 50) more. The job log lists every error."] else [] end);

if $mode == "log" then
  $errors | sort_by(.page, .line)[] | "\(.page):\(.line): \(.message)\(if .known then " [known]" else "" end)"
elif $mode == "count-new" then
  $new | length
elif $mode == "baseline" then
  ([
    "# Known HTML validation errors, which the Check HTML job in .github/workflows/build.yml does not fail on.",
    "# Each line is an error message without its quoted values and, after a tab, the markup around it reduced to its",
    "# tags and attribute names.",
    "# Fix errors rather than adding them here. To remove fixed errors, or exceptionally to accept new ones, regenerate",
    "# this file from the html-validation artifact of a Check HTML run:",
    "#   jq -r --arg site generated-site --arg mode baseline --rawfile baseline /dev/null \\",
    "#     -f .github/scripts/html-validation-report.jq html-validation.json > .github/html-validation-baseline.txt"
  ] + ($errors | map(key) | unique)) | join("\n")
else
  ($known - ($errors | map(key) | unique)) as $fixed
  | ($old | map(.page) | unique | length) as $old_pages
  | ([
      if ($new | length) > 0 then
        "### HTML validation found \($new | length) new errors on \($new | map(.page) | unique | length) pages.",
        "",
        "These are not in the baseline of known errors (`.github/html-validation-baseline.txt`). A template bug repeats on every page that uses it, so fixing the rows with the most pages first clears the most errors.",
        ""
      else
        "### HTML validation found no new errors.",
        ""
      end
    ]
    + (if ($new | length) > 0 then table($new | groups) + [""] else [] end)
    + (if ($old | length) > 0 then
        ["<details><summary>\($old | length) known errors on \($old_pages) pages, from the baseline</summary>", ""]
        + table($old | groups) + ["", "</details>", ""]
      else [] end)
    + (if ($fixed | length) > 0 then
        ["\($fixed | length) baseline entries no longer occur, so they can be removed from the baseline; see the instructions at the top of `.github/html-validation-baseline.txt`."]
      else [] end)
    ) | join("\n")
end
