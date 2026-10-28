package io.quarkus.cve.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quarkus.cve.util.UnquotedDateOrStringSerializer;
import io.quarkus.cve.util.UnquotedDateSerializer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Release-centric view of the advisories: for each Quarkus platform release, which advisories are
 * active, and for each release after the first of its release group (major.minor line), what changed
 * since the release before it. Also contains release metadata (policy, support providers, EOL dates, LTS status).
 */
@JsonPropertyOrder({
    "policy", "support_providers", "eol_support", "from", "to", "statistics", "releaseGroups"
})
public class ReleaseReport {

    public Map<String, Object> policy;

    @JsonProperty("support_providers")
    public Map<String, Object> supportProviders;

    @JsonProperty("eol_support")
    public List<Map<String, Object>> eolSupport;

    /** Lower and upper major.minor requested; {@code to} is null when open-ended. */
    public String from;
    public String to;

    public Statistics statistics = new Statistics();

    public List<ReleaseGroup> releaseGroups = new ArrayList<>();

    /** How the scan was run and what it cost. */
    public static class Statistics {
        /** When the scan ran, ISO-8601 with offset. */
        public String scanTime;
        /** The command line arguments the scan was run with. */
        public List<String> arguments = new ArrayList<>();
        /** Wall-clock time from the start of the command until the report was built. */
        public long executionMillis;
        /** Advisories that could be matched against BOM versions. */
        public int advisoriesEvaluated;
        /** Advisories without usable Maven version ranges (cannot be placed on any release). */
        public int advisoriesNotEvaluable;
        /** HTTP requests actually sent (a conditional request answered with 304 counts here). */
        public Counts httpRequests = new Counts();
        /** Of those requests, the ones answered 304 Not Modified (the cached copy was still current). */
        public Counts notModified = new Counts();
        /**
         * Share of requests that were not 304, per provider with at least one request, in percent.
         * A 304 is free against the GitHub rate limit; every other response is charged.
         */
        public Map<String, Double> chargedPercent = new LinkedHashMap<>();
        /** Lookups answered from the local cache without any request. */
        public Counts cacheHits = new Counts();
    }

    /** A total and its split per provider (osv, github, maven-central, quarkus-registry). */
    public static class Counts {
        public long total;
        public Map<String, Long> perProvider = new LinkedHashMap<>();

        public static Counts of(Map<String, Long> perProvider) {
            Counts c = new Counts();
            c.perProvider.putAll(perProvider);
            c.total = perProvider.values().stream().mapToLong(Long::longValue).sum();
            return c;
        }
    }

    /** One release group (a major.minor line): its metadata and releases, newest first. */
    @JsonPropertyOrder({
        "releaseGroup", "version", "release_date", "eol_date", "lts", "upcoming",
        "latest_patch", "latest_patch_date", "link", "rhbq_eol", "ibm_eol", "herodevs_eol", "releases"
    })
    public static class ReleaseGroup {
        public String releaseGroup;
        public String version;

        @JsonProperty("release_date")
        public String releaseDate;

        @JsonProperty("eol_date")
        public String eolDate;

        public Boolean lts;
        public Boolean upcoming;

        @JsonProperty("latest_patch")
        public String latestPatch;

        @JsonProperty("latest_patch_date")
        public String latestPatchDate;

        public String link;

        @JsonProperty("rhbq_eol")
        public String rhbqEol;

        @JsonProperty("ibm_eol")
        public String ibmEol;

        @JsonProperty("herodevs_eol")
        public Object herodevsEol;

        public List<Release> releases = new ArrayList<>();
    }

    @JsonPropertyOrder({
        "version", "date", "release_date", "first", "mostRecent", "link", "cves"
    })
    public static class Release {
        public String version;
        /** Upload date/time of the BOM on Maven Central ({@code yyyy-MM-dd HH:mm}); null if unknown. */
        public String date;

        @JsonProperty("release_date")
        public String releaseDate;

        /** Oldest release of its group (last in the list): {@code active} is the starting point and there is no delta. */
        public boolean first;
        /** Newest release of its group (first in the list). */
        public boolean mostRecent;

        public String link;

        public Cves cves = new Cves();
    }

    /** The advisories of one release. */
    public static class Cves {
        /** Every advisory active in this release. */
        public Split<Finding> active = new Split<>();
        /** Active here but not in the previous release of the group. */
        public Split<Change> introduced = new Split<>();
        /** Active in the previous release of the group but not here. */
        public Split<Change> resolved = new Split<>();
    }

    /**
     * Entries divided into Quarkus core and extensions ({@code io.quarkus}, {@code io.quarkus.*}) and
     * everything else the BOM manages.
     */
    public static class Split<T> {
        public List<T> core = new ArrayList<>();
        public List<T> dependencies = new ArrayList<>();

        public void add(boolean isCore, T entry) {
            (isCore ? core : dependencies).add(entry);
        }

        /** Number of entries in both parts. */
        public int count() {
            return core.size() + dependencies.size();
        }
    }

    @JsonPropertyOrder({
        "id", "cve", "severity", "score", "published", "fixed_in", "summary", "affects"
    })
    public static class Finding {
        public String id;
        public String cve;
        public String severity;
        public String score;
        public String published;
        public String summary;
        /** The managed artifacts that put this release in range, as "artifact version". */
        public List<String> affects = new ArrayList<>();

        /** The Quarkus platform release version that fixed this advisory, or null if not yet fixed. */
        @JsonProperty("fixed_in")
        public String fixedIn;
    }

    public static class Change {
        public Finding finding;
        /** What differs between the two releases for the affected artifacts. */
        public String cause;
    }
}