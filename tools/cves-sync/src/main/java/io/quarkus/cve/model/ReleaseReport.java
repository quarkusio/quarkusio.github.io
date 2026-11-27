package io.quarkus.cve.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.quarkus.cve.util.TreeRanges;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The advisories that affect Quarkus releases. Releases are not described here: releases.yaml owns the
 * release list, dates and lineage. This report owns the advisories and which releases each affects.
 *
 * Every advisory is stored once, with the components it names (Maven coordinates) and the releases it
 * affects, as a flat list and as ranges over the release tree.
 */
@JsonPropertyOrder({"statistics", "pending", "releases", "advisories"})
public class ReleaseReport {

    public Statistics statistics = new Statistics();

    /**
     * Published CVEs that name an artifact the scanned releases manage but that no OSV or GitHub advisory
     * covers yet, so there are no version ranges to place them on a release. Found through cve.org.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY) // an empty list would be written as "pending: []", which po4a rejects
    public List<Pending> pending = new ArrayList<>();

    /** The releases that were scanned and where their content was read from, oldest first. */
    public List<ReleaseSource> releases = new ArrayList<>();

    /** Advisory id -> advisory, sorted by id so the file only changes when the data does. */
    public Map<String, Advisory> advisories = new TreeMap<>();

    /** How the scan was run and what it cost. */
    public static class Statistics {
        /** When the scan ran, ISO-8601 with offset. */
        public String scanTime;
        /** The command line arguments the scan was run with. */
        public List<String> arguments = new ArrayList<>();
        /** Wall-clock time from the start of the command until the report was built. */
        public long executionMillis;
        /** Releases in releases.yaml that were scanned. */
        public int releasesScanned;
        /** Releases in releases.yaml with no BOM to read (they are left out and so are not in {@code releases}). */
        public int releasesWithoutContent;
        /** Advisories that could be matched against BOM versions. */
        public int advisoriesEvaluated;
        /** Advisories without usable Maven version ranges (cannot be placed on any release). */
        public int advisoriesNotEvaluable;
        /** CVEs listed under {@code pending}: known to cve.org, without version ranges anywhere yet. */
        public int advisoriesPending;
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

    /** A scanned release and the source its content came from, e.g. {@code maven://io.quarkus:quarkus-bom:3.39.0}. */
    @JsonPropertyOrder({"version", "source"})
    public record ReleaseSource(String version, String source) {}

    @JsonPropertyOrder({"cve", "severity", "published", "summary", "affectedComponents", "affectedReleases"})
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Advisory {
        public String cve;
        public String severity;
        public String published;
        public String summary;
        public List<Component> affectedComponents = new ArrayList<>();
        public AffectedReleases affectedReleases = new AffectedReleases();
    }

    /**
     * A Maven component the advisory names, the version range it says is affected, and the versions of it
     * that scanned releases ship inside that range.
     */
    @JsonPropertyOrder({"component", "range", "core", "shipped"})
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Component {
        /** {@code groupId:artifactId}. */
        public String component;
        /** {@code >=4.1.0 <4.1.100}, {@code <4.1.100} when nothing is said about where it starts, {@code <=2.0} for a last affected version. */
        public String range;
        /** Set (to true) for Quarkus core and its extensions; absent for dependencies. */
        public Boolean core;
        public List<String> shipped = new ArrayList<>();
    }

    /**
     * The releases an advisory affects. {@code list} names them all, oldest first. {@code tree} says the
     * same over the release tree as pieces of {@code root} and {@code cuts}: the root and every release built on
     * it, except the releases under a cut, which is the first release on a branch that is not affected.
     */
    @JsonPropertyOrder({"list", "tree"})
    public static class AffectedReleases {
        public List<String> list = new ArrayList<>();
        public List<TreeRanges.Piece> tree = new ArrayList<>();
    }

    /** A CVE whose affected Quarkus versions cannot be derived yet. */
    @JsonPropertyOrder({"id", "severity", "published", "summary", "artifacts", "link"})
    public static class Pending {
        /** The CVE id. */
        public String id;
        public String severity;
        public String published;
        public String summary;
        /** The BOM-managed artifacts the CVE record names, as {@code group:artifact}. */
        public List<String> artifacts = new ArrayList<>();
        /** The record at cve.org. */
        public String link;
    }
}
