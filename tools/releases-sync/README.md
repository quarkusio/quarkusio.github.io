# Release Sync

Keeps `_data/releases.yaml` patch versions current by fetching from the GitHub API.

## What it does

1. Fetches releases from GitHub API
2. Groups by minor version (3.36.x becomes "3.36")
3. Updates `latest_patch` and `latest_patch_date` for each minor
4. Preserves all manually-edited fields

## Field ownership

**Automated (updated by this script):**
- `latest_patch` - Latest patch version
- `latest_patch_date` - Date of latest patch
- `release_date` - Date of first release in this minor

**Manual (edit directly in releases.yaml):**
- `policy` section - LTS cadence, support durations, descriptions
- `support_providers` section - Commercial support providers
- `eol_support` section - EOL support options
- `lts` - Whether this is an LTS release
- `eol_date` - End of life date
- `link` - Blog post link
- `rhbq_eol`, `ibm_eol` - Commercial EOL dates
- `upcoming` - For releases not yet published

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
