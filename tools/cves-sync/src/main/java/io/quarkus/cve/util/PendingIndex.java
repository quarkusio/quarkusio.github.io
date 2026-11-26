package io.quarkus.cve.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * The CVEs whose records mention Quarkus, kept in {@code _data/pending-cves.yaml} together with the time up to
 * which the CVE list was read. Every CVE whose record mentions Quarkus stays in the file, even when an advisory
 * currently covers it or no scanned release yet manages the artifact it names: if that advisory is later withdrawn
 * or a new release starts managing the artifact, the CVE is looked at again without its record having to change.
 *
 * GitHub code search finds such CVEs, but is limited to 10 requests a minute and refuses a CI run for a long time,
 * so it is only used to create this file. From then on a run reads what changed in the CVE list since
 * {@link #scanned()} ({@link CveDelta}), adds the CVEs that now mention Quarkus and drops those that no longer do.
 *
 * The file is written the way the other data files are, with no inline list or map, which po4a cannot read:
 * <pre>
 * scanned: "2026-11-23T15:00:00Z"
 * pending:
 * - id: "CVE-2026-127106"
 *   noticed: "2026-11-20"
 * </pre>
 * with {@code pending:} and nothing after it when there are none.
 */
public final class PendingIndex {

    /** A delta read this far back is a lot of zips; past it a new list is made with code search instead. */
    public static final Duration MAX_AGE = Duration.ofDays(30);
    /**
     * A list made with code search starts this far back: the index of GitHub code search can be behind the CVE list by
     * some time, and reading a week of end-of-day zips (about 5 MB each) costs little.
     */
    public static final Duration SEED_LOOKBACK = Duration.ofDays(7);
    private static final Pattern CVE_ID = Pattern.compile("CVE-\\d{4}-\\d+");

    /** @param noticed the day this run first saw the CVE, which for the first list is the day it was made */
    public record Entry(String id, String noticed) {}

    private final Instant scanned;
    private final Map<String, String> noticedById;
    private final Set<String> rejected;

    public PendingIndex(Instant scanned, Map<String, String> noticedById) {
        this(scanned, noticedById, Set.of());
    }

    public PendingIndex(Instant scanned, Map<String, String> noticedById, Collection<String> rejected) {
        this.scanned = scanned.truncatedTo(ChronoUnit.SECONDS);
        this.noticedById = new TreeMap<>(noticedById);
        this.rejected = new java.util.TreeSet<>(rejected);
    }

    /** A list made from a code search, dated a week back so that the first delta covers any lag of its index. */
    public static PendingIndex seed(Collection<String> ids, Instant now, LocalDate today) {
        Map<String, String> m = new TreeMap<>();
        ids.forEach(id -> m.put(id, today.toString()));
        return new PendingIndex(now.truncatedTo(ChronoUnit.DAYS).minus(SEED_LOOKBACK), m, Set.of());
    }

    public Instant scanned() {
        return scanned;
    }

    public Set<String> ids() {
        return noticedById.keySet();
    }

    /** Known CVE ids currently suppressed because cve.org marks them REJECTED or disputed while an advisory still lists them. */
    public Set<String> rejected() {
        return java.util.Collections.unmodifiableSet(rejected);
    }

    public PendingIndex withRejected(Collection<String> updatedRejected) {
        return new PendingIndex(scanned, noticedById, updatedRejected);
    }

    public List<Entry> entries() {
        return noticedById.entrySet().stream().map(e -> new Entry(e.getKey(), e.getValue())).toList();
    }

    /** Whether the delta reaches back far enough to bring this list up to date. */
    public boolean usable(Instant now) {
        return !scanned.isBefore(now.minus(MAX_AGE));
    }

    /** The list after reading a delta: new CVEs that mention Quarkus are added, those that stopped mentioning it are dropped. */
    public PendingIndex apply(CveDelta.Result delta, LocalDate today) {
        Map<String, String> m = new TreeMap<>(noticedById);
        delta.mentionsQuarkus().forEach((id, mentions) -> {
            if (mentions) m.putIfAbsent(id, today.toString());
            else m.remove(id);
        });
        Instant next = delta.reached().isAfter(scanned) ? delta.reached() : scanned;
        return new PendingIndex(next, m, rejected);
    }

    /** Whether the two lists hold the same CVEs (pending and rejected), whatever their dates: what decides if a change is worth a commit. */
    public boolean sameCves(PendingIndex other) {
        return noticedById.keySet().equals(other.noticedById.keySet()) && rejected.equals(other.rejected);
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("scanned: \"").append(scanned).append("\"\n");
        sb.append("pending:\n");
        noticedById.forEach((id, noticed) -> sb.append("- id: \"").append(id).append("\"\n")
                .append("  noticed: \"").append(noticed).append("\"\n"));
        if (!rejected.isEmpty()) {
            sb.append("rejected:\n");
            rejected.forEach(id -> sb.append("- \"").append(id).append("\"\n"));
        }
        return sb.toString();
    }

    public byte[] bytes() {
        return render().getBytes(StandardCharsets.UTF_8);
    }

    /** Empty if there is no file; a file that cannot be read is an error, not a reason to start over. */
    public static Optional<PendingIndex> read(Path file) {
        if (!Files.isRegularFile(file)) return Optional.empty();
        try {
            return Optional.of(parse(Files.readString(file, StandardCharsets.UTF_8), file.toString()));
        } catch (IOException ex) {
            throw new IllegalStateException("could not read " + file + ": " + ex.getMessage(), ex);
        }
    }

    static PendingIndex parse(String text, String name) {
        JsonNode root;
        try {
            root = new YAMLMapper().readTree(text);
        } catch (IOException ex) {
            throw new IllegalStateException("could not read " + name + ": " + ex.getMessage(), ex);
        }
        if (root == null || !root.path("scanned").isTextual()) {
            throw new IllegalStateException(name + " has no 'scanned' time");
        }
        Instant scanned;
        try {
            scanned = Instant.parse(root.get("scanned").asText());
        } catch (RuntimeException ex) {
            throw new IllegalStateException(name + " has a 'scanned' time that is not a timestamp: " + root.get("scanned").asText());
        }
        Map<String, String> m = new TreeMap<>();
        for (JsonNode e : root.path("pending")) {
            String id = e.path("id").asText("");
            if (!CVE_ID.matcher(id).matches()) throw new IllegalStateException(name + " has an entry that is not a CVE id: '" + id + "'");
            m.put(id, e.path("noticed").asText(""));
        }
        Set<String> rej = new java.util.TreeSet<>();
        for (JsonNode e : root.path("rejected")) {
            String id = e.asText("");
            if (!CVE_ID.matcher(id).matches()) throw new IllegalStateException(name + " has a rejected entry that is not a CVE id: '" + id + "'");
            rej.add(id);
        }
        return new PendingIndex(scanned, m, rej);
    }
}
