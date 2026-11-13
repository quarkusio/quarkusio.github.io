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

Upcoming releases come from the wiki (`io/quarkusio/releases/sync/WikiReleasePlanning.java`). Each `## <major>.<minor> [LTS] [(Proposal)]`
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
- `rhbq.release_date`, `ibm.release_date` - When the minor first appeared in the Red Hat / IBM Maven repository (publish date of the earliest micro build of `quarkus-bom`, not an announced GA date). The provider block is created if the repository has the minor. A date, once set, is never looked up or overwritten, so routine runs make no requests to these servers (the IBM one is rate limited). When a lookup is needed it uses Quarkus REST Client interfaces (`MavenRepositories`) with SmallRye Fault Tolerance: requests are sequential and paced, transient failures (429, 5xx, I/O errors) are retried twice with exponential backoff, and a circuit breaker then stops all further requests to that server for the run; the gaps are filled by a later run. An enterprise `release_date` earlier than the community one is impossible: a scraped one is ignored with a warning, and the run fails before writing if any (including hand-edited) date is inconsistent
- `rhbq.last_in_major`, `ibm.last_in_major` - Set to `true` on the vendor's newest minor of a major stream (3.x, 2.x) once the vendor has shipped a newer major, unless a minor of that stream is already marked. It can be set by hand earlier, e.g. on the last 3.x before 4.0 is out
- `rhbq.eol_date`, `ibm.eol_date` - Computed from the support policy (`EnterpriseReleaseDates.computeEolDate`). One end date is kept; full support and maintenance aren't distinguished. Red Hat ([policy](https://access.redhat.com/support/policy/updates/red_hat_build_of_quarkus_notes)): a minor that is not `last_in_major` ends six months after the next minor of its major stream is released (the previous minor is in maintenance for six months); until there is a next minor, a year after its own release is a placeholder. IBM: a year after its own release. A `last_in_major` minor ends 3 years (Red Hat) or 5 years (IBM) after the vendor's first release in that major stream, but no earlier than six months after the vendor's first release of the next major: Red Hat's full support starts with a new major and ends when the next major is released, and the older one then stays in maintenance for at least six months. Red Hat's `last_in_major` minor can't be computed until the next major has a build, so until then its date is a hand-edited placeholder. Missing dates are filled in. Where a later release fixes a date, it is recomputed when that release appears and replaces the recorded one: a Red Hat minor's, when its next minor appears; a `last_in_major` minor's, when the next major appears, but that one is never shortened, since Red Hat publishes it and has extended it before (`EnterpriseReleaseDates.eolDateToRecord`). Any other existing date is kept, with a warning if it disagrees with the computed one
- `planned_date` - For upcoming releases: the planned Platform release date from the wiki
- `upcoming` - Set for releases listed as upcoming in the wiki; removed once the release is published
- `lts` - For upcoming releases only, taken from the wiki heading
- `proposal` - `true` for upcoming releases marked `(Proposal)` in the wiki
- `branched_from` - For published 3.x+ LTS releases that don't have one: the non-LTS minor (of the three below it) whose
  branch has the most recent merge-base with the LTS branch (`io/quarkusio/releases/sync/BranchLineage.java`). Release order and `main` aren't used: LTS
  policy changed over time (n-1 as parent, then concurrent n LTS / n+1 non-LTS releases) and `main` stopped being the trunk
  from 3.38. If candidates tie the newest is used, with a warning; if there are none the field stays blank (as for 2.x).
  Hand-edited values are never overwritten.

**Manual (edit directly in releases.yaml):**
- `policy` section - LTS cadence, support durations, descriptions
- `support_providers` section - Commercial support providers
- `eol_support` section - EOL support options
- `lts` - Whether this is an LTS release (published releases)
- `eol_date` - End of life date
- `link` - Blog post link
- `rhbq`, `ibm`, `herodevs` - Per-provider blocks; `rhbq.eol_date` of a `last_in_major` minor, until the next major has a build (the other vendor `eol_date`s are computed, see above); `last_in_major` can be set by hand too. `branched_from` (quoted, e.g. `"3.20"`) is the community release the vendor forked its build from, when not the release itself. `open_ended: true` means the vendor supports the release with no end date once its other support has ended (IBM after its own `eol_date`, HeroDevs Never-Ending Support after all other support)
- `upcoming`, `planned_date` - Only needed as a fallback when the wiki lists nothing usable for a release (`release_date` is only for published releases)

## Tests

The wiki parser is tested against `test/Release-Planning.md`, a **made-up** page that imitates the wiki's layout
(fake versions 98.x/99.x and fake dates, never real planning data):

```bash
cd tools/releases-sync && jbang io/quarkusio/releases/sync/WikiReleasePlanningTest.java
jbang io/quarkusio/releases/sync/EnterpriseReleaseDatesTest.java
jbang io/quarkusio/releases/sync/ReleasesSyncEolTest.java
jbang io/quarkusio/releases/sync/BranchLineageTest.java
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

- `GITHUB_TOKEN` - GitHub API token (optional, anonymous access has a lower rate limit)
- `RELEASES_ORG` - GitHub org (default: quarkusio)
- `RELEASES_REPO` - GitHub repo (default: quarkus)
- `RELEASES_INPUT` - Input file (default: _data/releases.yaml)
- `RELEASES_OUTPUT` - Output file (default: _data/releases.yaml)
