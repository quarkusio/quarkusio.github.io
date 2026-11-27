package io.quarkusio.releases.sync;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkiverse.roq.data.runtime.DataMappingWriter;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.StartupEvent;
import io.quarkusio.releases.Releases;
import io.quarkusio.releases.sync.GitHubReleases.FetchResult;
import io.quarkusio.releases.sync.GitHubReleases.GitHubMilestone;
import io.quarkusio.releases.sync.ReleasesSync.UpcomingRelease;

/**
 * Syncs _data/releases.yaml from GitHub and the release planning wiki, then exits.
 * <p>
 * Only part of the application built with the {@code data-sync} profile (see config/application-data-sync.properties):
 *
 * <pre>
 * ./mvnw package -DskipTests -Dquarkus.profile=data-sync
 * GITHUB_TOKEN=... java -Dquarkus.profile=data-sync -jar target/quarkus-app/quarkus-run.jar
 * </pre>
 *
 * The file is updated in place, use {@code git diff} to see the changes.
 */
@Singleton
@IfBuildProfile("data-sync")
public class ReleasesSyncJob {

    @ConfigProperty(name = "github-token")
    Optional<String> token;

    @ConfigProperty(name = "releases-sync.org", defaultValue = "quarkusio")
    String org;

    @ConfigProperty(name = "releases-sync.repo", defaultValue = "quarkus")
    String repo;

    @ConfigProperty(name = "releases-sync.predict-upcoming", defaultValue = "true")
    boolean predictUpcoming;

    @Inject
    Releases releases;

    @Inject
    MavenRepositories mavenRepositories;

    @Inject
    DataMappingWriter writer;

    void run(@Observes StartupEvent event) {
        int exitCode;
        try {
            sync();
            exitCode = 0;
        } catch (Exception e) {
            Log.errorf(e, "Release sync failed: %s", e.getMessage());
            exitCode = 1;
        }
        Quarkus.asyncExit(exitCode);
    }

    private void sync() throws Exception {
        if (token.isEmpty()) {
            Log.warnf("GITHUB_TOKEN is not set, using anonymous GitHub API access (lower rate limit)");
        }
        Log.infof("Syncing releases for %s/%s", org, repo);
        GitHubReleases github = new GitHubReleases(org, repo, token.orElse(null));

        FetchResult fetched = github.fetchReleases();
        Log.infof("Fetched %d releases from GitHub (%d pre-releases)", fetched.releases().size(),
                fetched.preReleases().size());

        List<UpcomingRelease> predicted = List.of();
        List<UpcomingRelease> wiki = List.of();
        if (predictUpcoming) {
            try {
                predicted = ReleasesSync.predictUpcoming(fetched.releases(), fetched.preReleases(), milestones(github));
                Log.infof("Predicted %d upcoming releases", predicted.size());
            } catch (Exception e) {
                Log.warnf("Upcoming release prediction failed: %s", e.getMessage());
            }
            // The release planning wiki is authoritative for upcoming releases
            wiki = wikiUpcoming(github, fetched);
        }

        // Micros: every GitHub release (final and pre-release), merged with the platform BOMs on Maven Central
        Map<String, LocalDate> boms = mavenRepositories.platformBomDates();
        if (boms == null) {
            Log.warnf("Keeping the platform BOMs already in the data; new ones will be added by a later run");
            boms = Micros.bomsFromExisting(releases);
        } else {
            Log.infof("Found %d platform BOMs on Maven Central", boms.size());
        }
        List<Micros.GitHubTag> allGitHub = new ArrayList<>();
        for (GitHubReleases.GitHubRelease r : fetched.releases()) {
            allGitHub.add(new Micros.GitHubTag(r.tagName(), r.publishedAt(), r.htmlUrl()));
        }
        for (GitHubReleases.GitHubRelease r : fetched.preReleases()) {
            allGitHub.add(new Micros.GitHubTag(r.tagName(), r.publishedAt(), r.htmlUrl()));
        }
        Map<String, List<Releases.MicroEntry>> micros = Micros.byMinor(allGitHub, boms);

        Releases updated = ReleasesSync.sync(releases, fetched.releases(), predicted, wiki, github::fetchMergeBase,
                micros);
        updated = ReleasesSync.applyEnterpriseReleaseDates(updated, "rhbq", mavenRepositories::rhbqDates);
        updated = ReleasesSync.applyEnterpriseReleaseDates(updated, "ibm", mavenRepositories::ibmDates);
        // The vendors' own builds, then the parent of every micro, community and vendor
        updated = ReleasesSync.applyVendorMicros(updated, "rhbq",
                mavenRepositories.rhbqBuilds(ReleasesSync.knownVendorBuilds(updated, "rhbq")),
                MavenRepositories::rhbqBomUrl);
        updated = ReleasesSync.applyVendorMicros(updated, "ibm", mavenRepositories.ibmBuilds(),
                MavenRepositories::ibmBomUrl);
        updated = ReleasesSync.applyParents(updated, releases, github::contains);
        ReleasesSync.validateEnterpriseReleaseDates(updated);
        ReleasesSync.microsBeforeTheirParents(updated)
                .forEach(problem -> Log.warnf("Check this release date: %s", problem));
        updated = ReleasesSync.applyEnterpriseEolDates(updated);
        Log.infof("Processed into %d release entries", updated.releases().size());

        Path file = writer.save(updated);
        Log.infof("Release data saved to %s", file);
    }

    private static List<GitHubMilestone> milestones(GitHubReleases github) {
        try {
            return github.fetchOpenMilestones();
        } catch (Exception e) {
            Log.warnf("Failed to fetch milestones: %s", e.getMessage());
            return List.of();
        }
    }

    private static List<UpcomingRelease> wikiUpcoming(GitHubReleases github, FetchResult fetched) {
        try {
            String page = github.fetchReleasePlanningWiki();
            if (page == null) {
                return List.of();
            }
            List<UpcomingRelease> upcoming = ReleasesSync.wikiUpcoming(page, fetched.releases(),
                    LocalDate.now(ZoneOffset.UTC));
            Log.infof("Found %d upcoming releases in the release planning wiki", upcoming.size());
            return upcoming;
        } catch (Exception e) {
            Log.warnf("Failed to read release planning wiki: %s", e.getMessage());
            return List.of();
        }
    }
}
