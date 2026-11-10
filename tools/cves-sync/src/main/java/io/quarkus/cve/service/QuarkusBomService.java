package io.quarkus.cve.service;

import com.fasterxml.jackson.core.type.TypeReference;
import io.quarkus.cve.client.MavenCentralClient;
import io.quarkus.cve.model.UnifiedVuln;
import io.quarkus.cve.util.Cache;
import io.quarkus.cve.util.Log;
import io.quarkus.cve.util.Retry;
import io.quarkus.cve.util.Stats;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Reads the Quarkus platform BOM ({@code io.quarkus.platform:quarkus-bom}) from Maven Central.
 *
 * Each BOM release lists every artifact that release manages, with its version. That makes it the
 * source of truth for "which artifact ids belong to Quarkus" and, per release, which versions of
 * Quarkus core, extensions and (later) transitive dependencies ship together.
 *
 * Every managed {@code groupId:artifactId:version} of a release is cached on disk, not only the
 * Quarkus ones, so that transitive-dependency support can be added without downloading again.
 * A published release never changes, so those entries never expire; only the release list does.
 */
@ApplicationScoped
public class QuarkusBomService {

    /** The list of published releases grows when Quarkus ships. */
    private static final Duration RELEASES_TTL = Duration.ofHours(6);

    private static final String NS_BOM = "bom";
    private static final String KEY_RELEASES = "releases";

    private static final TypeReference<List<String>> STRINGS_TYPE = new TypeReference<>() {};
    private static final TypeReference<Map<String, String>> STRING_MAP_TYPE = new TypeReference<>() {};

    private static final String KEY_DATES = "release-dates";

    /** One row of Maven Central's directory listing: {@code <a href="3.27.0/" ...>3.27.0/</a>   2025-09-24 12:26} */
    private static final Pattern DIR_ENTRY = Pattern.compile(
            "<a href=\"([^\"/]+)/\"[^>]*>[^<]*</a>\\s+(\\d{4}-\\d{2}-\\d{2}) (\\d{2}:\\d{2})");

    /** 3.40.0.CR1, 4.0.0.Beta1, 3.0.0.Alpha3 ... */
    private static final Pattern PRERELEASE = Pattern.compile("(?i)\\.(alpha|beta|cr|rc)\\d*$");

    @RestClient
    @Inject
    MavenCentralClient mavenCentral;

    /**
     * True for the groups that make up Quarkus core and its extensions: {@code io.quarkus} and
     * {@code io.quarkus.*} (e.g. {@code io.quarkus.resteasy.reactive}, {@code io.quarkus.http}).
     * {@code io.quarkus.platform} only holds BOM/descriptor artifacts, not libraries.
     */
    public static boolean isQuarkusGroup(String groupId) {
        if (groupId == null) return false;
        if (groupId.equals("io.quarkus.platform")) return false;
        return groupId.equals("io.quarkus") || groupId.startsWith("io.quarkus.");
    }

    /**
     * Sorted, distinct {@code groupId:artifactId} of every Quarkus core / extension artifact managed
     * by any of the sampled BOM releases. Sampling is the newest release of each minor line, so
     * artifacts that were later renamed or dropped (and may still carry advisories) are included.
     * Returns an empty list when nothing could be read.
     */
    public List<String> quarkusArtifacts() {
        List<String> releases = selectReleases(listReleases());
        if (releases.isEmpty()) return List.of();

        Log.log(1, "Reading %d Quarkus platform BOM releases (cached after the first download)", releases.size());
        TreeSet<String> artifacts = new TreeSet<>();
        int failed = 0;
        for (int i = 0; i < releases.size(); i++) {
            String version = releases.get(i);
            Log.log(3, "BOM %d/%d: %s", i + 1, releases.size(), version);
            List<String> managed = managedArtifacts(version, null);
            if (managed == null) {
                failed++;
                continue;
            }
            for (String gav : managed) {
                String[] p = gav.split(":");
                if (isQuarkusGroup(p[0])) artifacts.add(p[0] + ":" + p[1]);
            }
        }
        if (failed > 0) {
            Log.warn("could not read %d of %d Quarkus BOM releases; artifact list may be incomplete",
                    failed, releases.size());
        }
        Log.log(1, "BOM: %d Quarkus artifacts across %d releases", artifacts.size(), releases.size() - failed);
        return new ArrayList<>(artifacts);
    }

    // -----------------------------------------------------------------------
    // Release catalogue (used by the per-release report)
    // -----------------------------------------------------------------------

    /** True for {@code groupId:artifactId} coordinates in the Quarkus core / extension groups. */
    public static boolean isQuarkusArtifact(String groupArtifact) {
        int colon = groupArtifact.indexOf(':');
        return colon > 0 && isQuarkusGroup(groupArtifact.substring(0, colon));
    }

    /** Maven version ordering, so that 3.27.0.CR1 &lt; 3.27.0 &lt; 3.27.3.1 &lt; 3.27.4 and 3.9 &lt; 3.10. */
    public static int compareVersions(String a, String b) {
        return new ComparableVersion(a).compareTo(new ComparableVersion(b));
    }

    /** Every published platform release, oldest first. */
    public List<String> sortedReleases() {
        List<String> releases = new ArrayList<>(listReleases());
        releases.sort(QuarkusBomService::compareVersions);
        return releases;
    }

    /**
     * Release version -&gt; upload date and time ({@code yyyy-MM-dd HH:mm}) as shown by Maven Central.
     * Cached like the release list. Empty when unavailable; callers must tolerate missing dates.
     */
    public Map<String, String> releaseDates() {
        Optional<Cache.Entry<Map<String, String>>> cached = Cache.read(NS_BOM, KEY_DATES, STRING_MAP_TYPE);
        if (cached.isPresent() && cached.get().fresh(RELEASES_TTL)) {
            Stats.cacheHit(Stats.MAVEN_CENTRAL);
            return cached.get().data();
        }
        try {
            String html = Retry.call("Maven Central BOM directory", () -> mavenCentral.bomDirectory());
            Map<String, String> dates = parseDirectory(html);
            if (!dates.isEmpty()) {
                Cache.write(NS_BOM, KEY_DATES, dates, null);
                return dates;
            }
            Log.warn("Maven Central returned no Quarkus BOM release dates");
        } catch (RuntimeException ex) {
            Log.warn("could not read Quarkus BOM release dates from Maven Central (%s)", ex.getMessage());
        }
        Optional<Cache.Entry<Map<String, String>>> stale = Cache.readFallback(NS_BOM, KEY_DATES, STRING_MAP_TYPE);
        if (stale.isPresent()) {
            Log.warn("using cached Quarkus release dates from %s ago", stale.get().ageText());
            return stale.get().data();
        }
        return Map.of();
    }

    static Map<String, String> parseDirectory(String html) {
        Map<String, String> out = new LinkedHashMap<>();
        Matcher m = DIR_ENTRY.matcher(html);
        while (m.find()) out.put(m.group(1), m.group(2) + " " + m.group(3));
        return out;
    }

    /**
     * {@code groupId:artifactId -> version} for everything the release's BOM manages, or null when the
     * BOM could not be read. {@code onDownload} runs once per BOM that has to be fetched (not cached).
     */
    public Map<String, String> managedVersions(String release, Runnable onDownload) {
        List<String> managed = managedArtifacts(release, onDownload);
        if (managed == null) return null;
        Map<String, String> out = new HashMap<>();
        for (String gav : managed) {
            int last = gav.lastIndexOf(':');
            out.putIfAbsent(gav.substring(0, last), gav.substring(last + 1));
        }
        return out;
    }

    // -----------------------------------------------------------------------
    // Affected releases
    // -----------------------------------------------------------------------

    /**
     * Fills {@link UnifiedVuln#affectedReleases}: the Quarkus platform releases whose BOM manages at
     * least one affected package at a version inside an affected interval.
     *
     * Unlike {@link #quarkusArtifacts()}, this reads <em>every</em> published release, because a
     * patch release (e.g. 3.15.3.1) can be affected or fixed independently of its minor line.
     * Pre-releases are included. Only the managed versions of packages named by the given
     * advisories are kept in memory. Advisories without version intervals are left untouched (null).
     */
    public void annotateAffectedReleases(Collection<UnifiedVuln> vulns) {
        Set<String> needed = new HashSet<>();
        for (UnifiedVuln v : vulns) {
            for (UnifiedVuln.AffectedPkg p : mavenPackages(v)) needed.add(p.name);
        }
        if (needed.isEmpty()) return;

        List<String> releases = new ArrayList<>(listReleases());
        if (releases.isEmpty()) {
            Log.warn("no Quarkus BOM release list available; affected Quarkus releases not computed");
            return;
        }
        Map<String, ComparableVersion> parsed = new HashMap<>();
        releases.sort(Comparator.comparing(r -> parsed.computeIfAbsent(r, ComparableVersion::new)));

        // release -> (groupId:artifactId -> managed version), restricted to the packages we care about
        Map<String, Map<String, String>> index = new LinkedHashMap<>();
        boolean[] announced = {false};
        int failed = 0;
        Log.log(1, "Matching %d advisories against %d Quarkus platform releases", vulns.size(), releases.size());
        for (String release : releases) {
            List<String> managed = managedArtifacts(release, () -> {
                if (!announced[0]) {
                    announced[0] = true;
                    System.err.printf("Downloading Quarkus platform BOMs not yet cached "
                            + "(one-time, up to %d releases)...%n", releases.size());
                }
            });
            if (managed == null) {
                failed++;
                continue;
            }
            Map<String, String> versions = new HashMap<>();
            for (String gav : managed) {
                int last = gav.lastIndexOf(':');
                String ga = gav.substring(0, last);
                if (needed.contains(ga)) versions.putIfAbsent(ga, gav.substring(last + 1));
            }
            index.put(release, versions);
        }
        if (failed > 0) {
            Log.warn("could not read %d of %d Quarkus BOM releases; affected release lists may be incomplete",
                    failed, releases.size());
        }

        List<String> readable = new ArrayList<>(index.keySet());
        for (UnifiedVuln v : vulns) {
            List<UnifiedVuln.AffectedPkg> pkgs = mavenPackages(v);
            if (pkgs.isEmpty()) continue;
            boolean[] hit = new boolean[readable.size()];
            for (int i = 0; i < readable.size(); i++) {
                Map<String, String> managed = index.get(readable.get(i));
                for (UnifiedVuln.AffectedPkg p : pkgs) {
                    String version = managed.get(p.name);
                    if (version != null && isAffected(version, p, parsed)) {
                        hit[i] = true;
                        break;
                    }
                }
            }
            v.affectedReleases = compress(readable, hit);
        }
    }

    /** Maven packages of the advisory that carry version intervals (the only ones we can evaluate). */
    static List<UnifiedVuln.AffectedPkg> mavenPackages(UnifiedVuln v) {
        List<UnifiedVuln.AffectedPkg> out = new ArrayList<>();
        if (v.affected == null) return out;
        for (UnifiedVuln.AffectedPkg p : v.affected) {
            if (p.name != null && "maven".equalsIgnoreCase(p.ecosystem) && !p.intervals.isEmpty()) out.add(p);
        }
        return out;
    }

    /**
     * A CR build is the candidate for {@code x.y.z} and often the only {@code x.y.0} ever published,
     * but Maven orders {@code 3.30.0.CR1} before {@code 3.30.0}. Advisories say "introduced 3.30.0",
     * so for that lower bound a CR build is treated as the {@code x.y.z} it belongs to. The upper
     * bound (fixed / last_affected) uses the real version, so a CR build older than a fix stays affected.
     */
    private static final Pattern CR_SUFFIX = Pattern.compile("(?i)\\.CR\\d+$");

    private static boolean isAffected(String version, UnifiedVuln.AffectedPkg p, Map<String, ComparableVersion> parsed) {
        return matching(version, p, parsed) != null;
    }

    /** The affected interval of {@code p} that contains {@code version}, or null if the version is not affected. */
    static UnifiedVuln.Interval matching(String version, UnifiedVuln.AffectedPkg p, Map<String, ComparableVersion> parsed) {
        ComparableVersion v = parsed.computeIfAbsent(version, ComparableVersion::new);
        ComparableVersion forLowerBound = parsed.computeIfAbsent(
                CR_SUFFIX.matcher(version).replaceFirst(""), ComparableVersion::new);
        for (UnifiedVuln.Interval iv : p.intervals) {
            if (iv.introduced != null && !iv.introduced.equals("0")
                    && forLowerBound.compareTo(parsed.computeIfAbsent(iv.introduced, ComparableVersion::new)) < 0) continue;
            if (iv.fixed != null
                    && v.compareTo(parsed.computeIfAbsent(iv.fixed, ComparableVersion::new)) >= 0) continue;
            if (iv.lastAffected != null
                    && v.compareTo(parsed.computeIfAbsent(iv.lastAffected, ComparableVersion::new)) > 0) continue;
            return iv;
        }
        return null;
    }

    /** Consecutive affected releases collapse into "first - last"; a ".Final" suffix is dropped for display. */
    static List<String> compress(List<String> releases, boolean[] hit) {
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < hit.length) {
            if (!hit[i]) {
                i++;
                continue;
            }
            int j = i;
            while (j + 1 < hit.length && hit[j + 1]) j++;
            String first = display(releases.get(i));
            out.add(i == j ? first : first + " - " + display(releases.get(j)));
            i = j + 1;
        }
        return out;
    }

    private static String display(String release) {
        return release.endsWith(".Final") ? release.substring(0, release.length() - ".Final".length()) : release;
    }

    // -----------------------------------------------------------------------
    // Releases
    // -----------------------------------------------------------------------

    /** All published release versions, oldest first, as listed by Maven Central. Empty on failure. */
    private List<String> listReleases() {
        Optional<Cache.Entry<List<String>>> cached = Cache.read(NS_BOM, KEY_RELEASES, STRINGS_TYPE);
        if (cached.isPresent() && cached.get().fresh(RELEASES_TTL)) {
            Stats.cacheHit(Stats.MAVEN_CENTRAL);
            Log.log(2, "BOM: using cached release list (%d releases, age %s)",
                    cached.get().data().size(), cached.get().ageText());
            return cached.get().data();
        }
        try {
            String xml = Retry.call("Maven Central BOM metadata", () -> mavenCentral.bomMetadata());
            List<String> versions = parseVersions(xml);
            if (!versions.isEmpty()) {
                Cache.write(NS_BOM, KEY_RELEASES, versions, null);
                return versions;
            }
            Log.warn("Maven Central returned no Quarkus BOM releases");
        } catch (RuntimeException ex) {
            Log.warn("could not read the Quarkus BOM release list from Maven Central (%s)", ex.getMessage());
        }
        Optional<Cache.Entry<List<String>>> stale = Cache.readFallback(NS_BOM, KEY_RELEASES, STRINGS_TYPE);
        if (stale.isPresent()) {
            Log.warn("using cached Quarkus BOM release list from %s ago", stale.get().ageText());
            return stale.get().data();
        }
        return List.of();
    }

    /**
     * One release per {@code major.minor} line: the newest final release, or the newest pre-release
     * if the line has no final release yet (e.g. 4.0.0.Beta1).
     */
    static List<String> selectReleases(List<String> all) {
        Map<String, String> byMinor = new LinkedHashMap<>();
        for (String v : all) {
            String minor = minorOf(v);
            String current = byMinor.get(minor);
            if (current == null || !isPrerelease(v) || isPrerelease(current)) byMinor.put(minor, v);
        }
        return new ArrayList<>(byMinor.values());
    }

    private static String minorOf(String version) {
        String[] p = version.split("\\.");
        return p.length >= 2 ? p[0] + "." + p[1] : version;
    }

    private static boolean isPrerelease(String version) {
        return PRERELEASE.matcher(version).find();
    }

    // -----------------------------------------------------------------------
    // One BOM
    // -----------------------------------------------------------------------

    /** {@code groupId:artifactId:version} for everything the release manages, or null if unavailable. */
    private List<String> managedArtifacts(String version, Runnable onDownload) {
        String key = "managed-" + version;
        Optional<Cache.Entry<List<String>>> cached = Cache.read(NS_BOM, key, STRINGS_TYPE);
        if (cached.isPresent()) { // releases are immutable: no TTL
            Stats.cacheHit(Stats.MAVEN_CENTRAL);
            return cached.get().data();
        }
        if (onDownload != null) onDownload.run();
        try {
            String pom = Retry.call("Quarkus BOM " + version, () -> mavenCentral.bomPom(version));
            List<String> managed = parseManaged(pom);
            if (managed.isEmpty()) throw new IllegalStateException("no managed dependencies found");
            Cache.write(NS_BOM, key, managed, null);
            return managed;
        } catch (RuntimeException ex) {
            Log.error(2, "Quarkus BOM " + version + " unavailable", ex);
            Optional<Cache.Entry<List<String>>> stale = Cache.readFallback(NS_BOM, key, STRINGS_TYPE);
            return stale.isPresent() ? stale.get().data() : null;
        }
    }

    // -----------------------------------------------------------------------
    // XML
    // -----------------------------------------------------------------------

    static List<String> parseVersions(String metadataXml) {
        Document doc = parse(metadataXml);
        NodeList versions = doc.getElementsByTagName("versions");
        List<String> out = new ArrayList<>();
        if (versions.getLength() == 0) return out;
        for (Element v : children((Element) versions.item(0), "version")) {
            String text = v.getTextContent().trim();
            if (!text.isEmpty()) out.add(text);
        }
        return out;
    }

    /**
     * Entries of the BOM's {@code dependencyManagement}. Only library jars/poms are kept; the
     * exe/klib/json/properties/archetype entries and classifier variants are not CVE-relevant.
     */
    static List<String> parseManaged(String pomXml) {
        Document doc = parse(pomXml);
        NodeList dm = doc.getElementsByTagName("dependencyManagement");
        List<String> out = new ArrayList<>();
        if (dm.getLength() == 0) return out;
        NodeList deps = ((Element) dm.item(0)).getElementsByTagName("dependency");
        for (int i = 0; i < deps.getLength(); i++) {
            Element d = (Element) deps.item(i);
            String type = childText(d, "type");
            if (type != null && !type.equals("jar") && !type.equals("pom")) continue;
            if (childText(d, "classifier") != null) continue;
            String g = childText(d, "groupId");
            String a = childText(d, "artifactId");
            String v = childText(d, "version");
            if (g != null && a != null && v != null) out.add(g + ":" + a + ":" + v);
        }
        return out;
    }

    private static Document parse(String xml) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            return f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            throw new IllegalStateException("could not parse XML: " + ex.getMessage(), ex);
        }
    }

    /** Direct children only: a plain getElementsByTagName would also match nested exclusions. */
    private static List<Element> children(Element parent, String name) {
        List<Element> out = new ArrayList<>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && e.getTagName().equals(name)) out.add(e);
        }
        return out;
    }

    private static String childText(Element parent, String name) {
        List<Element> c = children(parent, name);
        if (c.isEmpty()) return null;
        String t = c.get(0).getTextContent().trim();
        return t.isEmpty() ? null : t;
    }
}
