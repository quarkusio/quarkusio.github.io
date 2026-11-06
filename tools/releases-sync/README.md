# Release Sync

Keeps `_data/releases.yaml` patch versions current by fetching from the GitHub API,
and upcoming releases current by reading the
[Release Planning wiki](https://github.com/quarkusio/quarkus/wiki/Release-Planning).

## What it does

1. Fetches releases from GitHub API
2. Groups by minor version (3.36.x becomes "3.36")
3. Updates `latest_patch` and `latest_patch_date` for each minor
4. Reads the Release Planning wiki (raw markdown) and replaces the upcoming entries with every
   release whose Platform release date is today or later (including `(Proposal)` entries)
5. Preserves all manually-edited fields

## Upcoming releases

Upcoming releases come from the wiki (`WikiReleasePlanning.java`). Each `## <major>.<minor> [LTS] [(Proposal)]`
heading with a "Full Platform release ... on <date>" bullet becomes an entry with `upcoming: true`,
`planned_date` set to that Platform date, `lts` set from the heading, and `proposal: true` if the heading is marked `(Proposal)`. Other headings
(e.g. `4.0 Beta1`) are ignored.

Existing `upcoming: true` entries are replaced by the wiki's entry for the same version, but
kept when the wiki has no usable entry for them (e.g. its date couldn't be parsed; a warning is
logged). Delete such an entry by hand once it is no longer planned.

If the wiki can't be read and there are no hand-edited upcoming entries, the next release is
predicted from GitHub milestones, pre-release tags, or by cadence (next minor).

## Field ownership

**Automated (updated by this script):**
- `latest_patch` - Latest patch version
- `latest_patch_date` - Date of latest patch
- `release_date` - Date of first release in this minor (published releases only)
- `planned_date` - For upcoming releases: the planned Platform release date from the wiki
- `upcoming` - Set for releases listed as upcoming in the wiki; removed once the release is published
- `lts` - For upcoming releases only, taken from the wiki heading
- `proposal` - `true` for upcoming releases marked `(Proposal)` in the wiki

**Manual (edit directly in releases.yaml):**
- `policy` section - LTS cadence, support durations, descriptions
- `support_providers` section - Commercial support providers
- `eol_support` section - EOL support options
- `lts` - Whether this is an LTS release (published releases)
- `eol_date` - End of life date
- `link` - Blog post link
- `rhbq`, `ibm`, `herodevs` - Per-provider blocks with `release_date` and `eol_date` (herodevs `eol_date` can be a date or string like "nes")
- `upcoming`, `planned_date` - Only needed as a fallback when the wiki lists nothing usable for a release (`release_date` is only for published releases)

## Tests

The wiki parser is tested against `test/Release-Planning.md`, a **made-up** page that imitates the wiki's layout
(fake versions 98.x/99.x and fake dates, never real planning data):

```bash
cd tools/releases-sync && jbang WikiReleasePlanningTest.java
```

## Running locally

```bash
# Dry run (show what would change)
GITHUB_TOKEN="your_token" jbang tools/releases-sync/main.java --dry-run

# Actually update
GITHUB_TOKEN="your_token" jbang tools/releases-sync/main.java
```

## CI

The workflow at `.github/workflows/sync-releases.yml`:
- Runs on a schedule to update patch versions
- Runs in dry-run mode on PRs touching `tools/releases-sync/` or the workflow itself

## Environment variables

- `GITHUB_TOKEN` - GitHub API token (required)
- `RELEASES_ORG` - GitHub org (default: quarkusio)
- `RELEASES_REPO` - GitHub repo (default: quarkus)
- `RELEASES_INPUT` - Input file (default: _data/releases.yaml)
- `RELEASES_OUTPUT` - Output file (default: _data/releases.yaml)
