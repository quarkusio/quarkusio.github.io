package io.quarkusio.releases.sync;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.artifact.versioning.ComparableVersion;

import io.quarkus.logging.Log;
import io.quarkusio.releases.Micro;
import io.quarkusio.releases.Releases;
import io.quarkusio.releases.sync.GitHubReleases.GitHubMilestone;
import io.quarkusio.releases.sync.GitHubReleases.GitHubRelease;

/**
 * Computes the updated {@link Releases} data from GitHub releases and the release planning wiki.
 * <p>
 * Pure logic, no I/O. Field ownership (which fields are automated and which are hand-edited) is described in
 * {@code README.md} in this package.
 */
public final class ReleasesSync {

    private static final List<String> PROVIDERS = List.of("rhbq", "ibm", "herodevs");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");
    private static final Pattern MILESTONE_VERSION = Pattern.compile("^(\\d+\\.\\d+)(?:\\s|$|\\s*-|\\s*\\.|$)");
    private static final Pattern PRE_RELEASE_VERSION = Pattern.compile("^(\\d+\\.\\d+)\\.\\d+\\.(CR|Alpha|Beta|RC)");

    /**
     * @param source "wiki", "milestone", "prerelease" or "cadence"
     * @param dueDate ISO date (or date-time), can be null
     */
    public record UpcomingRelease(String version, String source, String dueDate, boolean lts, boolean proposal) {

        UpcomingRelease(String version, String source, String dueDate) {
            this(version, source, dueDate, false, false);
        }
    }

    /**
     * Looks up the merge-base of two release branches (see {@link BranchLineage}).
     */
    @FunctionalInterface
    public interface MergeBases {
        /**
         * @return the merge-base, or null if either branch doesn't exist
         */
        BranchLineage.MergeBase find(String base, String head) throws Exception;
    }

    private ReleasesSync() {
    }

    // ---- Upcoming releases ----

    /**
     * Upcoming releases from the release planning wiki, which is authoritative for upcoming releases.
     */
    public static List<UpcomingRelease> wikiUpcoming(String page, List<GitHubRelease> githubReleases, LocalDate today) {
        Set<String> published = githubReleases.stream().map(r -> minorVersion(r.tagName())).collect(Collectors.toSet());
        Consumer<String> onUnparsable = version -> {
            // Old releases have no Platform date; only a release that isn't published yet matters
            if (!published.contains(version)) {
                Log.warnf("Release planning wiki lists %s but no Platform release date could be parsed; "
                        + "keeping any existing entry for it", version);
            }
        };
        return WikiReleasePlanning.upcoming(WikiReleasePlanning.parse(page, onUnparsable), today).stream()
                .map(r -> new UpcomingRelease(r.version(), "wiki", r.platformDate().toString(), r.lts(), r.proposal()))
                .toList();
    }

    /**
     * Fallback predictions, used when neither the wiki nor the data has upcoming releases.
     */
    public static List<UpcomingRelease> predictUpcoming(List<GitHubRelease> releases, List<GitHubRelease> preReleases,
            List<GitHubMilestone> milestones) {
        Set<String> existingVersions = releases.stream().map(r -> minorVersion(r.tagName())).collect(Collectors.toSet());
        List<UpcomingRelease> upcoming = new ArrayList<>();
        for (GitHubMilestone milestone : milestones) {
            String version = match(MILESTONE_VERSION, milestone.title());
            if (version != null && !existingVersions.contains(version)) {
                upcoming.add(new UpcomingRelease(version, "milestone", milestone.dueOn()));
            }
        }
        for (GitHubRelease preRelease : preReleases) {
            String version = match(PRE_RELEASE_VERSION, preRelease.tagName());
            if (version != null && !existingVersions.contains(version)) {
                upcoming.add(new UpcomingRelease(version, "prerelease", null));
            }
        }
        // next minor, based on cadence
        releases.stream().map(r -> minorVersion(r.tagName())).filter(ReleasesSync::isValidVersion)
                .max(ReleasesSync::compareVersions)
                .map(latest -> {
                    String[] parts = latest.split("\\.");
                    return parts[0] + "." + (Integer.parseInt(parts[1]) + 1);
                })
                .filter(next -> !existingVersions.contains(next))
                .ifPresent(next -> upcoming.add(new UpcomingRelease(next, "cadence", null)));

        // distinct by version, first source wins
        Map<String, UpcomingRelease> distinct = new LinkedHashMap<>();
        upcoming.forEach(u -> distinct.putIfAbsent(u.version(), u));
        return distinct.values().stream().sorted((a, b) -> compareVersions(b.version(), a.version())).toList();
    }

    // ---- Merge ----

    /**
     * @param githubReleases the final GitHub releases
     * @param micros every micro of each minor, newest first, from {@link Micros#byMinor}
     */
    public static Releases sync(Releases existing, List<GitHubRelease> githubReleases, List<UpcomingRelease> predicted,
            List<UpcomingRelease> wikiUpcoming, MergeBases mergeBases, Map<String, List<Releases.MicroEntry>> micros) {
        // Some candidates are marked as final releases on GitHub (e.g. 3.5.0.CR1): they don't make a minor published
        Map<String, List<GitHubRelease>> releasesByMinor = githubReleases.stream()
                .filter(r -> r.tagName() != null && isValidVersion(r.tagName()) && !Micro.isPreRelease(r.tagName()))
                .collect(Collectors.groupingBy(r -> minorVersion(r.tagName())));

        List<WorkingRelease> existingReleases = existing.releases() == null ? List.of()
                : existing.releases().stream().map(WorkingRelease::from).toList();
        Map<String, WorkingRelease> existingByVersion = existingReleases.stream()
                .collect(Collectors.toMap(r -> r.version, r -> r, (a, b) -> a));

        List<WorkingRelease> processed = new ArrayList<>();
        for (Map.Entry<String, List<GitHubRelease>> entry : releasesByMinor.entrySet()) {
            List<GitHubRelease> minorReleases = new ArrayList<>(entry.getValue());
            minorReleases.sort((a, b) -> compareVersions(b.tagName(), a.tagName()));
            processed.add(WorkingRelease.published(entry.getKey(), minorReleases, micros.get(entry.getKey()),
                    existingByVersion.get(entry.getKey())));
        }

        // Upcoming releases from the planning wiki replace the existing entry for the same version
        List<UpcomingRelease> wikiPending = wikiUpcoming.stream()
                .filter(u -> !releasesByMinor.containsKey(u.version()))
                .toList();
        Set<String> wikiVersions = wikiPending.stream().map(UpcomingRelease::version).collect(Collectors.toSet());

        // Other existing upcoming releases are kept as manually curated (e.g. the wiki date couldn't be parsed)
        Set<String> manualUpcomingVersions = new HashSet<>();
        for (WorkingRelease release : existingReleases) {
            // Compare by minor version so a hand-edited "4.0.0" is covered by the wiki's "4.0"
            String minor = minorVersion(release.version);
            if (!Boolean.TRUE.equals(release.upcoming) || wikiVersions.contains(minor)) {
                continue;
            }
            if (releasesByMinor.containsKey(minor)) {
                Log.infof("Upcoming release %s is now published", release.version);
            } else {
                processed.add(release);
                manualUpcomingVersions.add(release.version);
                Log.infof("Preserving manually curated upcoming release: %s", release.version);
            }
        }

        for (UpcomingRelease wiki : wikiPending) {
            processed.add(WorkingRelease.upcoming(wiki));
            Log.infof("Upcoming release from wiki: %s (%s)", wiki.version(), wiki.dueDate());
        }

        // Nothing upcoming in the wiki or curated by hand: fall back to predicting the next release
        if (wikiPending.isEmpty() && manualUpcomingVersions.isEmpty()) {
            String latestReleased = processed.stream()
                    .filter(r -> !Boolean.TRUE.equals(r.upcoming))
                    .map(r -> r.version)
                    .max(ReleasesSync::compareVersions)
                    .orElse(null);
            predicted.stream()
                    .filter(u -> latestReleased == null || compareVersions(u.version(), latestReleased) > 0)
                    .filter(u -> !releasesByMinor.containsKey(u.version()))
                    .min((a, b) -> compareVersions(a.version(), b.version()))
                    .ifPresent(next -> {
                        processed.add(WorkingRelease.upcoming(next));
                        Log.infof("Predicted upcoming release: %s (source: %s)", next.version(), next.source());
                    });
        }

        // Upcoming releases can already have candidates or betas (e.g. 4.0.0.Beta1)
        for (WorkingRelease release : processed) {
            if (Boolean.TRUE.equals(release.upcoming)) {
                release.micros = micros.get(minorVersion(release.version));
            }
        }

        processed.sort((a, b) -> compareVersions(b.version, a.version));

        fillParentRelease(processed, mergeBases);

        // Non-LTS releases are supported "until the next minor release":
        // when not set explicitly, their EOL date is the release date of the next minor.
        for (int i = 1; i < processed.size(); i++) {
            WorkingRelease release = processed.get(i);
            if (release.hasExplicitEolDate || Boolean.TRUE.equals(release.upcoming) || Boolean.TRUE.equals(release.lts)) {
                continue;
            }
            WorkingRelease next = processed.get(i - 1);
            LocalDate nextDate = next.releaseDate != null ? next.releaseDate : next.plannedDate;
            if (nextDate != null) {
                release.eolDate = nextDate;
            }
        }

        return new Releases(existing.policy(), existing.supportProviders(), existing.eolSupport(),
                processed.stream().map(WorkingRelease::toEntry).toList());
    }

    /**
     * Fills in parent_release for published 3.x+ LTS releases which don't have one, from git ancestry
     * (see BranchLineage). Hand-edited values are never touched; where git gives no answer the field is left blank.
     */
    private static void fillParentRelease(List<WorkingRelease> releases, MergeBases mergeBases) {
        List<String> published = releases.stream()
                .filter(r -> !Boolean.TRUE.equals(r.upcoming))
                .map(r -> r.version).toList();
        List<String> nonLts = releases.stream()
                .filter(r -> !Boolean.TRUE.equals(r.upcoming) && !Boolean.TRUE.equals(r.lts))
                .map(r -> r.version).toList();
        for (WorkingRelease r : releases) {
            if (!Boolean.TRUE.equals(r.lts) || Boolean.TRUE.equals(r.upcoming) || r.parentRelease != null
                    || BranchLineage.major(r.version) < 3) {
                continue;
            }
            try {
                List<String> window = BranchLineage.minorsBelow(r.version, published).stream()
                        .filter(nonLts::contains).toList();
                Map<String, BranchLineage.MergeBase> bases = new HashMap<>();
                for (String v : window) {
                    BranchLineage.MergeBase base = mergeBases.find(v, r.version);
                    if (base != null) {
                        bases.put(v, base);
                    }
                }
                BranchLineage.Result result = BranchLineage.pick(bases);
                if (result.version() == null) {
                    Log.warnf("No candidate branch found for %s; leaving parent_release blank", r.version);
                    continue;
                }
                if (result.ambiguous()) {
                    Log.warnf("%s has the same most recent merge-base with %s; using %s, please check", r.version,
                            result.candidates(), result.version());
                }
                r.parentRelease = result.version();
                Log.infof("%s branched from %s (most recent merge-base)", r.version, r.parentRelease);
            } catch (Exception e) {
                Log.warnf("Could not work out parent_release for %s: %s", r.version, e.getMessage());
            }
        }
    }

    // ---- Enterprise release dates ----

    /**
     * Fills in the provider's release_date for published releases that don't have one yet. A date, once
     * known, is never looked up again, so a normal run with nothing new makes no requests to the provider.
     * A provider that can't be reached just leaves the gaps for the next run.
     *
     * @param lookup the provider's release dates, by minor version, for the wanted minor versions
     */
    public static Releases applyEnterpriseReleaseDates(Releases data, String provider,
            Function<Set<String>, Map<String, LocalDate>> lookup) {
        Set<String> wanted = data.releases().stream()
                .filter(r -> !r.upcoming())
                .filter(r -> providerDates(r, provider) == null || providerDates(r, provider).releaseDate() == null)
                .map(Releases.Entry::version)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, LocalDate> dates = lookup.apply(wanted);
        Log.infof("Looked for %d %s release dates, found %d", wanted.size(), provider, dates.size());
        List<Releases.Entry> releases = new ArrayList<>();
        for (Releases.Entry release : data.releases()) {
            LocalDate date = dates.get(release.version());
            if (date == null || !wanted.contains(release.version())) {
                releases.add(release);
                continue;
            }
            Optional<String> problem = EnterpriseReleaseDates.checkNotBeforeCommunity(
                    provider, release.version(), WorkingRelease.date(release.releaseDate()), date);
            if (problem.isPresent()) {
                Log.warnf("Ignoring scraped date: %s", problem.get());
                releases.add(release);
                continue;
            }
            Releases.EnterpriseDates existing = providerDates(release, provider);
            Releases.EnterpriseDates dated = existing == null
                    ? new Releases.EnterpriseDates(date.toString(), null, null, false, false, null)
                    : new Releases.EnterpriseDates(date.toString(), existing.eolDate(), existing.parentRelease(),
                            existing.openEnded(), existing.lastInMajor(), existing.micros());
            releases.add(withProviderDates(release, provider, dated));
        }
        return new Releases(data.policy(), data.supportProviders(), data.eolSupport(), releases);
    }

    // ---- Vendor builds ----

    /**
     * The vendor's builds already in the data, with their dates, so they aren't looked up again.
     */
    public static Map<String, LocalDate> knownVendorBuilds(Releases data, String provider) {
        Map<String, LocalDate> known = new HashMap<>();
        for (Releases.Entry release : data.releases()) {
            Releases.EnterpriseDates block = providerDates(release, provider);
            if (block != null && block.micros() != null) {
                block.micros().forEach(m -> known.put(m.version(), parseDateOrNull(m.releaseDate())));
            }
        }
        return known;
    }

    /**
     * Lists the vendor's builds of each published minor as the micros of its vendor block, newest first, creating the
     * block if the minor has none. The list is rewritten in full; parents are filled in by {@link #applyParents}.
     *
     * @param builds every build in the vendor's repository (the latest build of each version) to its publish date,
     *        null when unknown; null if the repository couldn't be read, which leaves the data as it is
     * @param bomUrl a build's BOM in the vendor's repository
     */
    public static Releases applyVendorMicros(Releases data, String provider, Map<String, LocalDate> builds,
            Function<String, String> bomUrl) {
        if (builds == null) {
            Log.warnf("Keeping the %s builds already in the data", provider);
            return data;
        }
        Map<String, List<String>> byMinor = new HashMap<>();
        for (String version : builds.keySet()) {
            String minor = EnterpriseReleaseDates.minorOf(version);
            if (minor != null) {
                byMinor.computeIfAbsent(minor, k -> new ArrayList<>()).add(version);
            }
        }
        List<Releases.Entry> releases = new ArrayList<>();
        for (Releases.Entry release : data.releases()) {
            List<String> versions = byMinor.get(release.version());
            if (release.upcoming() || versions == null) {
                releases.add(release);
                continue;
            }
            List<Releases.MicroEntry> micros = versions.stream().sorted(Lineage.VENDOR_ORDER.reversed())
                    .map(v -> new Releases.MicroEntry(v, builds.get(v) == null ? null : builds.get(v).toString(), null,
                            bomUrl.apply(v), false, false, false, null))
                    .toList();
            Releases.EnterpriseDates existing = providerDates(release, provider);
            Releases.EnterpriseDates block = existing == null
                    ? new Releases.EnterpriseDates(null, null, null, false, false, micros)
                    : new Releases.EnterpriseDates(existing.releaseDate(), existing.eolDate(), existing.parentRelease(),
                            existing.openEnded(), existing.lastInMajor(), micros);
            releases.add(withProviderDates(release, provider, block));
        }
        return new Releases(data.policy(), data.supportProviders(), data.eolSupport(), releases);
    }

    // ---- Parents ----

    /**
     * Fills in parent_release for every micro, community and vendor, and for vendor blocks (see {@link Lineage}). A
     * parent already in {@code previous} is kept, so hand-edited values are never overwritten and git is only asked
     * about micros it hasn't answered for yet. Finals get their parent from the versions; pre-releases, and the first
     * micro of each minor, from git. If git can't be asked (e.g. the GitHub rate limit), those are left without a parent
     * for a later run.
     *
     * @param previous the data as read, before this run
     */
    public static Releases applyParents(Releases data, Releases previous, Lineage.Ancestry ancestry) {
        Map<String, String> known = new HashMap<>();
        for (Releases.Entry release : previous.releases()) {
            allMicros(release).filter(m -> m.parentRelease() != null)
                    .forEach(m -> known.put(m.version(), m.parentRelease()));
        }
        Set<String> community = data.releases().stream().filter(r -> r.micros() != null)
                .flatMap(r -> r.micros().stream()).map(Releases.MicroEntry::version).collect(Collectors.toSet());
        // Nearest earlier minors first: the data lists releases newest first
        List<Releases.Entry> withMicros = data.releases().stream()
                .filter(r -> r.micros() != null && !r.micros().isEmpty()).toList();

        boolean[] gitAvailable = { true };
        List<Releases.Entry> releases = new ArrayList<>();
        for (Releases.Entry release : data.releases()) {
            List<Releases.MicroEntry> micros = release.micros();
            if (micros != null && !micros.isEmpty()) {
                Map<String, String> computed = Lineage.communityParents(micros);
                String first = Lineage.firstMicro(micros);
                micros = micros.stream().map(m -> {
                    String parent = known.get(m.version());
                    if (parent != null) {
                        return m.withParentRelease(parent);
                    }
                    parent = computed.get(m.version());
                    // Pre-releases need git: they aren't always built on each other (see Lineage)
                    boolean needsGit = m.version().equals(first) || Micro.isPreRelease(m.version());
                    if (!needsGit) {
                        return m.withParentRelease(parent);
                    }
                    String previousMicro = parent;
                    parent = null;
                    if (m.githubUrl() != null && gitAvailable[0]) {
                        int index = withMicros.indexOf(release);
                        List<List<Releases.MicroEntry>> earlier = withMicros.subList(index + 1, withMicros.size())
                                .stream().map(Releases.Entry::micros).toList();
                        try {
                            parent = Lineage.preReleaseParent(m.version(), previousMicro, earlier, ancestry);
                            if (parent == null) {
                                Log.warnf("No earlier micro found in %s; leaving its parent_release blank", m.version());
                            } else if (!parent.equals(previousMicro)) {
                                Log.infof("%s descends from %s", m.version(), parent);
                            }
                        } catch (Exception e) {
                            Log.warnf("Could not work out the parent of %s (%s); leaving the rest for a later run",
                                    m.version(), e.getMessage());
                            gitAvailable[0] = false;
                        }
                    }
                    return m.withParentRelease(parent);
                }).toList();
            }
            Releases.Entry updated = new Releases.Entry(release.version(), release.releaseDate(), release.plannedDate(),
                    release.eolDate(), release.lts(), release.upcoming(), release.proposal(), release.link(),
                    release.rhbq(), release.ibm(), release.herodevs(), release.parentRelease(), micros);
            for (String provider : PROVIDERS) {
                Releases.EnterpriseDates block = providerDates(updated, provider);
                if (block != null && block.micros() != null && !block.micros().isEmpty()) {
                    updated = withProviderDates(updated, provider, withVendorParents(block, known, community));
                }
            }
            releases.add(updated);
        }
        return new Releases(data.policy(), data.supportProviders(), data.eolSupport(), releases);
    }

    /**
     * The block's builds with their parents, and the block's own parent_release, if not set, filled in with the
     * community micro its first build is based on.
     */
    private static Releases.EnterpriseDates withVendorParents(Releases.EnterpriseDates block, Map<String, String> known,
            Set<String> community) {
        Map<String, String> computed = Lineage.vendorParents(block.micros(), community);
        List<Releases.MicroEntry> micros = block.micros().stream()
                .map(m -> m.withParentRelease(known.getOrDefault(m.version(), computed.get(m.version())))).toList();
        String parent = block.parentRelease();
        if (parent == null) {
            parent = micros.stream().map(Releases.MicroEntry::version).min(Lineage.VENDOR_ORDER)
                    .map(v -> Lineage.communityBase(v, community)).orElse(null);
        }
        return new Releases.EnterpriseDates(block.releaseDate(), block.eolDate(), parent, block.openEnded(), block.lastInMajor(), micros);
    }

    private static java.util.stream.Stream<Releases.MicroEntry> allMicros(Releases.Entry release) {
        java.util.stream.Stream<Releases.MicroEntry> micros = release.micros() == null ? java.util.stream.Stream.empty()
                : release.micros().stream();
        for (Releases.EnterpriseDates block : java.util.Arrays.asList(release.rhbq(), release.ibm(), release.herodevs())) {
            if (block != null && block.micros() != null) {
                micros = java.util.stream.Stream.concat(micros, block.micros().stream());
            }
        }
        return micros;
    }

    /**
     * Micros dated before the micro they were built from, which can't be right: a release can't predate its
     * parent_release. This is how a release published to GitHub long after it was tagged shows up. Checks community
     * and vendor micros alike, whatever minor each parent is in; a parent that isn't listed is skipped.
     *
     * @return one description per problem, empty if the dates are consistent
     */
    public static List<String> microsBeforeTheirParents(Releases data) {
        Map<String, LocalDate> dates = new HashMap<>();
        data.releases().forEach(r -> allMicros(r).forEach(m -> {
            LocalDate date = parseDateOrNull(m.releaseDate());
            if (date != null) {
                dates.put(m.version(), date);
            }
        }));
        List<String> problems = new ArrayList<>();
        data.releases().forEach(r -> allMicros(r).forEach(m -> {
            LocalDate date = dates.get(m.version());
            LocalDate parent = m.parentRelease() == null ? null : dates.get(m.parentRelease());
            if (date != null && parent != null && date.isBefore(parent)) {
                problems.add("%s (%s) is dated before its parent_release %s (%s)".formatted(m.version(), date,
                        m.parentRelease(), parent));
            }
        }));
        return problems;
    }

    /**
     * Fails the run, before anything is written, if any enterprise release_date (scraped or hand-edited)
     * is earlier than the community release_date of the same minor.
     */
    public static void validateEnterpriseReleaseDates(Releases data) {
        List<String> problems = new ArrayList<>();
        for (Releases.Entry release : data.releases()) {
            for (String provider : PROVIDERS) {
                LocalDate date = parseDateOrNull(providerDates(release, provider) == null ? null
                        : providerDates(release, provider).releaseDate());
                if (date != null) {
                    EnterpriseReleaseDates.checkNotBeforeCommunity(provider, release.version(),
                            WorkingRelease.date(release.releaseDate()), date).ifPresent(problems::add);
                }
            }
        }
        problems.forEach(problem -> Log.errorf("Inconsistent release dates: %s", problem));
        if (!problems.isEmpty()) {
            throw new IllegalStateException(problems.size() + " enterprise release date(s) are before the community "
                    + "release date; first: " + problems.get(0));
        }
    }

    /**
     * For the Red Hat and IBM builds: marks a minor as the vendor's last of its major stream once the vendor
     * has shipped a newer major (unless a minor of that stream is already marked, by hand), then fills in any
     * missing eol_date from the support policy and warns about an existing one that disagrees with it. An
     * existing eol_date is kept, except where a later release fixes it (see
     * {@link EnterpriseReleaseDates#eolDateToRecord}). Red Hat's last minor, until the next major, is a hand-edited
     * placeholder. See {@link EnterpriseReleaseDates#computeEolDate}.
     */
    public static Releases applyEnterpriseEolDates(Releases data) {
        Map<String, Map<String, Releases.EnterpriseDates>> updated = new LinkedHashMap<>();
        for (String provider : List.of("rhbq", "ibm")) {
            Map<String, EnterpriseReleaseDates.Build> builds = new LinkedHashMap<>();
            for (Releases.Entry r : data.releases()) {
                Releases.EnterpriseDates dates = providerDates(r, provider);
                LocalDate released = dates == null ? null : parseDateOrNull(dates.releaseDate());
                if (released != null) {
                    builds.put(r.version(), new EnterpriseReleaseDates.Build(released, dates.lastInMajor()));
                }
            }
            Set<String> newlyLast = EnterpriseReleaseDates.newlyLastInMajor(builds);
            newlyLast.forEach(v -> {
                Log.infof("Marking %s %s as the last of its major stream: the vendor has released a newer major", provider, v);
                builds.put(v, new EnterpriseReleaseDates.Build(builds.get(v).releaseDate(), true));
            });
            Map<String, Releases.EnterpriseDates> result = new LinkedHashMap<>();
            for (Releases.Entry r : data.releases()) {
                Releases.EnterpriseDates dates = providerDates(r, provider);
                if (dates == null || !builds.containsKey(r.version())) {
                    continue;
                }
                String eol = dates.eolDate();
                LocalDate actual = parseDateOrNull(eol);
                eol = EnterpriseReleaseDates.eolDateToRecord(provider, r.version(), builds, actual)
                        .map(LocalDate::toString).orElse(eol);
                if (actual != null && !actual.toString().equals(eol)) {
                    Log.infof("Setting %s %s eol_date to %s (was %s): a later release fixes it", provider, r.version(),
                            eol, actual);
                } else if (actual != null) {
                    EnterpriseReleaseDates.checkEolDate(provider, r.version(), builds, actual)
                            .ifPresent(problem -> Log.warnf("Check this hand-edited date: %s", problem));
                }
                result.put(r.version(), new Releases.EnterpriseDates(dates.releaseDate(), eol, dates.parentRelease(),
                        dates.openEnded(), builds.get(r.version()).lastInMajor(), dates.micros()));
            }
            updated.put(provider, result);
        }
        List<Releases.Entry> releases = new ArrayList<>();
        for (Releases.Entry r : data.releases()) {
            Releases.Entry e = r;
            for (String provider : List.of("rhbq", "ibm")) {
                Releases.EnterpriseDates dates = updated.get(provider).get(r.version());
                if (dates != null) {
                    e = withProviderDates(e, provider, dates);
                }
            }
            releases.add(e);
        }
        return new Releases(data.policy(), data.supportProviders(), data.eolSupport(), releases);
    }

    private static Releases.EnterpriseDates providerDates(Releases.Entry release, String provider) {
        return switch (provider) {
            case "rhbq" -> release.rhbq();
            case "ibm" -> release.ibm();
            case "herodevs" -> release.herodevs();
            default -> throw new IllegalArgumentException("Unknown provider " + provider);
        };
    }

    private static Releases.Entry withProviderDates(Releases.Entry r, String provider, Releases.EnterpriseDates dates) {
        return new Releases.Entry(r.version(), r.releaseDate(), r.plannedDate(), r.eolDate(), r.lts(), r.upcoming(),
                r.proposal(), r.link(),
                provider.equals("rhbq") ? dates : r.rhbq(), provider.equals("ibm") ? dates : r.ibm(),
                provider.equals("herodevs") ? dates : r.herodevs(), r.parentRelease(), r.micros());
    }

    /**
     * @return the date, or null for a missing value or a value which isn't a date (e.g. "nes")
     */
    private static LocalDate parseDateOrNull(String value) {
        try {
            return value == null ? null : LocalDate.parse(value);
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ---- Versions ----

    // The leading numbers are read with a regex rather than Maven's DefaultArtifactVersion, because that
    // only understands x.y.z[-qualifier]. Quarkus also uses a fourth number for some patch releases
    // (e.g. "3.40.1.1"), which it can't parse: it reports 0.0.0 with the whole string as the qualifier.
    // ComparableVersion (see compareVersions) handles such versions fine for ordering, but doesn't expose
    // the individual numbers.
    static boolean isValidVersion(String version) {
        return VERSION_PATTERN.matcher(version).find();
    }

    static String minorVersion(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);
        return matcher.find() ? matcher.group(1) + "." + matcher.group(2) : version;
    }

    static int compareVersions(String v1, String v2) {
        return new ComparableVersion(v1).compareTo(new ComparableVersion(v2));
    }

    private static String match(Pattern pattern, String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * A mutable release, while it's being synced.
     */
    private static final class WorkingRelease {
        String version;
        LocalDate releaseDate;
        LocalDate plannedDate;
        LocalDate eolDate;
        /**
         * Only set eol dates count as explicit: the bean can't tell an empty {@code eol_date:} from a missing one.
         */
        boolean hasExplicitEolDate;
        Boolean lts;
        Boolean upcoming;
        Boolean proposal;
        String latestPatch;
        LocalDate latestPatchDate;
        String link;
        /**
         * The version this release branched from (hand-edited, or filled in for LTS releases).
         */
        String parentRelease;
        Releases.EnterpriseDates rhbq;
        Releases.EnterpriseDates ibm;
        Releases.EnterpriseDates herodevs;
        /**
         * The published micro releases, newest first; null when there are none.
         */
        List<Releases.MicroEntry> micros;

        static WorkingRelease from(Releases.Entry entry) {
            WorkingRelease release = new WorkingRelease();
            release.version = entry.version();
            release.releaseDate = date(entry.releaseDate());
            release.plannedDate = date(entry.plannedDate());
            release.eolDate = date(entry.eolDate());
            release.hasExplicitEolDate = entry.eolDate() != null;
            release.lts = entry.lts();
            release.upcoming = entry.upcoming();
            release.proposal = entry.proposal();
            release.link = entry.link();
            release.parentRelease = entry.parentRelease();
            release.rhbq = entry.rhbq();
            release.ibm = entry.ibm();
            release.herodevs = entry.herodevs();
            release.micros = entry.micros();
            return release;
        }

        /**
         * @param githubReleases the final GitHub releases of this minor, newest first
         * @param micros every micro of this minor, newest first, including pre-releases
         */
        static WorkingRelease published(String minorVersion, List<GitHubRelease> githubReleases,
                List<Releases.MicroEntry> micros, WorkingRelease existing) {
            WorkingRelease release = new WorkingRelease();
            release.version = minorVersion;
            release.micros = micros;
            // The date of the first final micro (not a candidate), the earlier of GitHub and Maven Central
            release.releaseDate = micros == null ? null
                    : micros.stream().filter(m -> !m.candidate() && !m.beta() && !m.alpha() && m.releaseDate() != null)
                            .reduce((newer, older) -> older).map(m -> LocalDate.parse(m.releaseDate())).orElse(null);
            if (release.releaseDate == null) {
                release.releaseDate = gitHubDate(githubReleases.get(githubReleases.size() - 1).publishedAt());
            }
            // Preserve manual fields from the existing release
            if (existing != null) {
                release.lts = existing.lts;
                release.eolDate = existing.eolDate;
                release.hasExplicitEolDate = existing.hasExplicitEolDate;
                release.link = existing.link;
                release.parentRelease = existing.parentRelease;
                release.rhbq = existing.rhbq;
                release.ibm = existing.ibm;
                release.herodevs = existing.herodevs;
            } else {
                release.lts = false;
            }
            return release;
        }

        static WorkingRelease upcoming(UpcomingRelease upcoming) {
            WorkingRelease release = new WorkingRelease();
            release.version = upcoming.version();
            release.lts = upcoming.lts();
            release.upcoming = true;
            release.proposal = upcoming.proposal() ? true : null;
            if (upcoming.dueDate() != null) {
                try {
                    release.plannedDate = LocalDate.parse(upcoming.dueDate().split("T")[0]);
                } catch (RuntimeException e) {
                    Log.debugf("Failed to parse due date for upcoming release %s: %s", upcoming.version(), e.getMessage());
                }
            }
            return release;
        }

        Releases.Entry toEntry() {
            boolean isUpcoming = Boolean.TRUE.equals(upcoming);
            return new Releases.Entry(version, string(releaseDate), string(plannedDate), string(eolDate),
                    Boolean.TRUE.equals(lts), isUpcoming, isUpcoming && Boolean.TRUE.equals(proposal),
                    link, rhbq, ibm, herodevs, parentRelease, micros == null || micros.isEmpty() ? null : micros);
        }

        private static LocalDate date(String value) {
            return value == null || value.isBlank() ? null : LocalDate.parse(value);
        }

        private static String string(LocalDate date) {
            return date == null ? null : date.toString();
        }

        private static LocalDate gitHubDate(String isoDate) {
            return Instant.parse(isoDate).atOffset(ZoneOffset.UTC).toLocalDate();
        }
    }
}
