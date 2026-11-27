# Release Sync

Keeps `_data/releases.yaml` micro releases current by fetching from the GitHub API,
and upcoming releases current by reading the
[Release Planning wiki](https://github.com/quarkusio/quarkus/wiki/Release-Planning).

## What it does

1. Fetches releases from GitHub API, and the platform BOM versions from Maven Central
2. Groups by minor version (3.36.x becomes "3.36")
3. Lists every micro release of each minor, including candidates, under `micros`
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
- `micros` - Every micro of the minor, newest first, including candidates and betas (e.g. `3.28.0.CR1`, `4.0.0.Beta1`, which
  upcoming releases can have too). Rewritten in full on every run (`Micros.java`). It is the union
  of two sources, which don't quite line up: the GitHub releases of quarkusio/quarkus (final and pre-release), and the
  platform BOMs (`io.quarkus.platform:quarkus-bom`) on Maven Central, which start at 2.0 and skip some X.0 core releases
  (e.g. there is a 3.28.0.CR1 BOM but no 3.28.0 one). Each micro has:
  - `version`
  - `release_date` - the earlier of the GitHub publish date and the Maven Central upload date (they differ, usually
    by a day, for a few releases). A GitHub release published more than 60 days after its tag commit (2.11.0.Final
    was published a year late, which dated it after 2.11.1.Final) is dated from the tag commit instead, with a
    warning. A micro dated before its `parent_release` is also warned about, as a check for any other such case
  - `rhbq.last_in_major`, `ibm.last_in_major` - Set to `true` on the vendor's newest minor of a major stream (3.x, 2.x) once the vendor has shipped a newer major, unless a minor of that stream is already marked. It can be set by hand earlier, e.g. on the last 3.x before 4.0 is out
  - `rhbq.eol_date`, `ibm.eol_date` - Computed from the support policy (`EnterpriseReleaseDates.computeEolDate`). One end date is kept; full support and maintenance aren't distinguished. Red Hat ([policy](https://access.redhat.com/support/policy/updates/red_hat_build_of_quarkus_notes)): a minor that is not `last_in_major` ends six months after the next minor of its major stream is released (the previous minor is in maintenance for six months); until there is a next minor, a year after its own release is a placeholder. IBM: a year after its own release. A `last_in_major` minor ends 3 years (Red Hat) or 5 years (IBM) after the vendor's first release in that major stream, but no earlier than six months after the vendor's first release of the next major: Red Hat's full support starts with a new major and ends when the next major is released, and the older one then stays in maintenance for at least six months. Red Hat's `last_in_major` minor can't be computed until the next major has a build, so until then its date is a hand-edited placeholder. Missing dates are filled in. Where a later release fixes a date, it is recomputed when that release appears and replaces the recorded one: a Red Hat minor's, when its next minor appears; a `last_in_major` minor's, when the next major appears, but that one is never shortened, since Red Hat publishes it and has extended it before (`EnterpriseReleaseDates.eolDateToRecord`). Any other existing date is kept, with a warning if it disagrees with the computed one
  - `candidate: true` for a release candidate (`CR`, or `RC`), `beta: true` for a `Beta`, `alpha: true` for an
    `Alpha`; worked out from the version, and left out for final releases
  - `parent_release` - the micro it was built from (`Lineage.java`), see below
  - `github_url` - its GitHub release; left out if there is none
  - `bom_url` - its platform BOM POM on Maven Central; left out if there is none

  Maven Central is read once per run (one directory listing, with the same retries and circuit breaker as the
  enterprise repositories below). If it can't be read, the BOMs already in the data are kept and new ones are
  added by a later run. There is no `latest_patch` field: the site works the latest final micro out from this
  list (`Release.latestMicro()`, which skips candidates, betas and alphas)
- `release_date` - Date of first final release in this minor (published releases only), from `micros`. Pre-releases
  are recognised by their version (Alpha, Beta, CR, RC), as GitHub marks some of them (e.g. 3.5.0.CR1) as final
- `rhbq.release_date`, `ibm.release_date` - When the minor first appeared in the Red Hat / IBM Maven repository (publish date of the earliest micro build of `quarkus-bom`, not an announced GA date). The provider block is created if the repository has the minor; its `eol_date` stays hand-edited. A date, once set, is never looked up or overwritten, so routine runs make no requests to these servers (the IBM one is rate limited). When a lookup is needed it uses Quarkus REST Client interfaces (`MavenRepositories`) with SmallRye Fault Tolerance: requests are sequential and paced, transient failures (429, 5xx, I/O errors) are retried twice with exponential backoff, and a circuit breaker then stops all further requests to that server for the run; the gaps are filled by a later run. An enterprise `release_date` earlier than the community one is impossible: a scraped one is ignored with a warning, and the run fails before writing if any (including hand-edited) date is inconsistent
- `planned_date` - For upcoming releases: the planned Platform release date from the wiki
- `upcoming` - Set for releases listed as upcoming in the wiki; removed once the release is published
- `lts` - For upcoming releases only, taken from the wiki heading
- `proposal` - `true` for upcoming releases marked `(Proposal)` in the wiki
- `parent_release` - For published 3.x+ LTS releases that don't have one: the non-LTS minor (of the three below it) whose
  branch has the most recent merge-base with the LTS branch (`BranchLineage.java`). Release order and `main` aren't used: LTS
  policy changed over time (n-1 as parent, then concurrent n LTS / n+1 non-LTS releases) and `main` stopped being the trunk
  from 3.38. If candidates tie the newest is used, with a warning; if there are none the field stays blank (as for 2.x).
  Hand-edited values are never overwritten.

- `rhbq.micros`, `ibm.micros` - The vendor's own builds of the minor (`quarkus-bom` in its Maven repository), newest
  first, with `version`, `release_date` (when the build was published), `parent_release` and `bom_url`. A version the
  vendor rebuilt before shipping (`3.27.4.SP3-redhat-00002`, then `-00003`) is listed once, as its latest build. Rewritten
  in full on every run: each run lists both repositories (one request each), and dates any Red Hat build not in the
  data yet (one request each; IBM's listing has the dates). If a repository can't be read, the builds already in the data
  are kept. HeroDevs has no builds of its own
- Micro `parent_release` - The micro each micro was built from. A parent already in the data is never overwritten, so
  hand-edited values win and git is only asked once per micro:
  - A final, from the versions: the main line runs through the pre-releases then `X.Y.0`, `X.Y.1`, ...; four-part
    patches are a side line off it (`X.Y.Z.1` from `X.Y.Z`, `X.Y.Z.2` from `X.Y.Z.1`, and `X.Y.Z+1` from `X.Y.Z`, not
    from its patches). Git agrees: each release tag sits one `[RELEASE]` version bump off its branch
  - A pre-release, from git: the previous pre-release if it contains it (see below), else the earlier minors are
    searched as for a first micro. Pre-releases aren't always built on each other: 3.0's first alphas were each cut from
    a different 2.x line with the Jakarta transformation on top (`3.0.0.Alpha2` from `2.14.3.Final`, `3.0.0.Alpha3`
    and `3.0.0.Alpha4` from `2.16.0.CR1`)
  - The first micro of a minor (usually its first candidate): from git ancestry. Its parent is the newest micro of the
    nearest earlier minor (searching up to six) whose tag it contains, where "contains" means every commit of that tag
    missing from it is a `[RELEASE]` commit (GitHub compare API; a binary search, so a handful of requests per minor).
    Release order isn't used, for the same reasons as for the minor's `parent_release`: e.g. `3.40.0.CR1` descends from
    `3.39.4`, and `4.0.0.Beta1` from `3.37.0.CR1`. Left blank when no earlier micro is found (1.0), or GitHub can't be
    asked (the rest are left for a later run)
  - A vendor build: a service pack (`3.20.1.SP2-redhat-…`) descends from the vendor's previous build of the same
    community micro (`3.20.1.SP1-redhat-…`, or `3.20.1.redhat-…`); any other build from the community micro in its
    version (`3.20.1`, or `2.2.3.Final` for 1.x and 2.x). Which vendor fixes a rebased build carries forward can't be
    seen from outside, so the community micro is used
- Vendor block `parent_release` - When not set, the community micro the vendor's first build of the minor is based on
  (`3.20.0` for `3.20.0.redhat-00002`)
**Manual (edit directly in releases.yaml):**
- `policy` section - LTS cadence, support durations, descriptions
- `support_providers` section - Commercial support providers
- `eol_support` section - EOL support options
- `lts` - Whether this is an LTS release (published releases)
- `eol_date` - End of life date
- `link` - Blog post link
- `rhbq`, `ibm`, `herodevs` - Per-provider blocks; `rhbq.eol_date` of a `last_in_major` minor, until the next major has a build (the other vendor `eol_date`s are computed, see above); `last_in_major` can be set by hand too. `parent_release` (quoted, e.g. `"3.20.1"` or `"3.19"`) is the community release (micro or minor) the vendor forked its build from, when not the release itself; filled in from the vendor's builds when not set. `open_ended: true` means the vendor supports the release with no end date once its other support has ended (IBM after its own `eol_date`, HeroDevs Never-Ending Support after all other support)
- `upcoming`, `planned_date` - Only needed as a fallback when the wiki lists nothing usable for a release (`release_date` is only for published releases)

## Tests

The sync is covered by ordinary unit tests in `src/test/java/io/quarkusio/releases/sync`. The wiki parser is tested
against `src/test/resources/releases-sync/Release-Planning.md`, a **made-up** page that imitates the wiki's layout
(fake versions 98.x/99.x and fake dates, never real planning data):

```bash
./mvnw test -Dtest='io.quarkusio.releases.sync.*Test'
```

## Running locally

The sync is a job inside the site's Quarkus application, enabled by the `data-sync` profile
(`config/application-data-sync.properties`). It runs at startup, rewrites `_data/releases.yaml` in place and exits:

```bash
./mvnw package -DskipTests -Dquarkus.profile=data-sync
GITHUB_TOKEN="your_token" java -Dquarkus.profile=data-sync -jar target/quarkus-app/quarkus-run.jar
git diff _data/releases.yaml   # see what changed
```

## CI

The workflow at `.github/workflows/sync-releases.yml`:
- Runs on a schedule to update micro releases
- On PRs touching this package or the workflow itself, runs the tests and the sync, and shows the diff without committing

## Configuration

- `GITHUB_TOKEN` (`github-token`) - GitHub API token (optional, anonymous access has a lower rate limit)
- `releases-sync.org` - GitHub org (default: quarkusio)
- `releases-sync.repo` - GitHub repo (default: quarkus)
- `releases-sync.predict-upcoming` - predict upcoming releases from the wiki, milestones and cadence (default: true)
