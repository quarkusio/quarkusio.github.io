# cve-util

Reads which CVEs affect which Quarkus releases, for `_data/cves.yaml`. The workflow `.github/workflows/sync-cves.yml`
runs it every night and on pull requests that change this directory.

```sh
jbang run.java releases --format yaml -v \
  --releases-input ../../_data/releases.yaml \
  --pending-index ../../_data/pending-cves.yaml \
  --output /tmp/cves.yaml
```

Set `GITHUB_TOKEN` (see `.env.example`) to raise GitHub's rate limits.

## `_data/pending-cves.yaml`

A new CVE has no version ranges until GitHub reviews it. cve.org names the affected artifacts, though, so the report
lists such CVEs as *pending*. Finding them means finding every CVE record that mentions Quarkus:

- GitHub code search over `CVEProject/cvelistV5` does that in 2 requests. But code search allows 10 requests a minute,
  and for the token of an Actions run GitHub answers with a refusal, and a `Retry-After` of up to 12 minutes, even
  for the first request.
- So code search is only used to **create** `_data/pending-cves.yaml`, which holds the CVE ids that mention Quarkus and
  the time up to which the CVE list was read (`scanned`).
- A run reads what changed since `scanned` from the delta zips that `cvelistV5` publishes as release assets. They hold
  the full records, so there is nothing to look up per CVE, and they need no token. A CVE whose record now mentions
  Quarkus is added, and one whose record no longer does is dropped.
- The zips are **cumulative over a day**: the one of 14:00 holds everything changed since midnight (UTC). What changed
  from 23:00 to 24:00 is in no hourly zip of that day or the next, but only in the `end_of_day` zip, published shortly
  after midnight. So a run reads the `end_of_day` zip of every day from the day of `scanned` up to yesterday (about
  5 MB each, and **the run fails if one is missing**, as that would lose a day of changes), and then only the newest
  hourly zip of today. A normal night is one end-of-day zip and one hourly zip.
  - `https://github.com/CVEProject/cvelistV5/releases/download/cve_<day>_at_end_of_day/<day>_delta_CVEs_at_end_of_day.zip`
  - `https://github.com/CVEProject/cvelistV5/releases/download/cve_<day>_<hour>00Z/<day>_delta_CVEs_at_<hour>00Z.zip`
    (some are published under the tag of the hour before, and some hours have no zip: the newest one there is is read)
  - If yesterday's end-of-day zip is not out yet, its newest hourly zip is read and `scanned` stops there, so that the
    next run reads the end-of-day zip.
- A CVE stays in the file while it is *pending*: no advisory with version ranges covers it, and its record names an
  artifact that a release manages. Any other CVE is dropped, and comes back through the delta, to be looked at again,
  if its record changes. A CVE that was dropped because no release managed the artifact it names, and that is not
  changed after a release starts to manage it, is only found again when the file is made again (see below). Advisories
  are searched by artifact in every run, so a CVE that has an advisory is found without this file.
- Most pending CVEs are not new: of the 41 in the first version, 33 were published over 60 days before. They are
  waiting for an advisory that may never come, not for GitHub to review them.

The file is written only when the whole run succeeded, and the workflow commits it when its ids change, or when the
committed `scanned` is more than 3 days old. Every run is safe to lose (a cancelled run, or one that was not committed):
the next one reads from the day of the last `scanned` that was. The file is written without an inline list or map,
because po4a (which translates the data files) cannot read those.

### Creating it again

When the file does not exist, or `scanned` is more than 30 days ago (nothing was read for a month), a run makes a new
one with code search. That needs a **personal** token, as the workflow's own token is refused:

1. Create a token that can read public repositories (a fine-grained one needs no permissions at all) and put it in
   `tools/cves-sync/.env` as `GITHUB_TOKEN=...` (the file is not committed).
2. From this directory:
   ```sh
   rm ../../_data/pending-cves.yaml
   jbang run.java releases --format yaml -v \
     --releases-input ../../_data/releases.yaml \
     --pending-index ../../_data/pending-cves.yaml \
     --output ../../_data/cves.yaml
   ```
3. Commit both files. The log says `making it with code search`; it takes about a minute, and waits as long as GitHub
   asks when it is refused. The new file starts a week back, because the index of code search can be behind the CVE
   list, so the first run after it reads a week of end-of-day zips.

Without `--pending-index`, every run uses code search.
