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
- A run reads what changed since `scanned` from the hourly delta zips that `cvelistV5` publishes as release assets
  (`https://github.com/CVEProject/cvelistV5/releases/download/cve_<day>_<hour>00Z/<day>_delta_CVEs_at_<hour>00Z.zip`).
  They hold the full records, so there is nothing to look up per CVE, they need no token, and a day of them is about
  20 MB. A CVE whose record now mentions Quarkus is added, one whose record no longer does is dropped, and one that an
  advisory with version ranges covers is dropped too. `scanned` moves on to the last zip that was read.
- Some hours have no zip under their own name. A zip holds what changed since the previous one, so the next one
  covers the hour, and the run goes on. A run fails, and writes nothing, if a zip cannot be read, or if there is no zip
  at all for a few hours.
- A run reads from two hours before `scanned`, in case the last zip was still being filled.

The file is written only when the whole run succeeded, and the workflow commits it only when its ids change (or, to
keep `scanned` from getting old, when it is a week old). It is written without an inline list or map, because po4a
(which translates the data files) cannot read those.

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
   asks when it is refused.

Without `--pending-index`, every run uses code search.
