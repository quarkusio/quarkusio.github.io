///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17+
//DEPS io.quarkus.platform:quarkus-bom:3.40.1@pom
//DEPS io.quarkus:quarkus-picocli
//DEPS io.quarkus:quarkus-rest-client-jackson
//DEPS io.quarkus:quarkus-jackson
//DEPS org.apache.maven:maven-artifact
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-yaml
//SOURCES src/main/java/io/quarkus/cve/model/OsvPackage.java
//SOURCES src/main/java/io/quarkus/cve/model/OsvVersion.java
//SOURCES src/main/java/io/quarkus/cve/model/OsvRange.java
//SOURCES src/main/java/io/quarkus/cve/model/OsvAffected.java
//SOURCES src/main/java/io/quarkus/cve/model/OsvSeverity.java
//SOURCES src/main/java/io/quarkus/cve/model/OsvReference.java
//SOURCES src/main/java/io/quarkus/cve/model/OsvVulnerability.java
//SOURCES src/main/java/io/quarkus/cve/model/OsvQueryRequest.java
//SOURCES src/main/java/io/quarkus/cve/model/OsvQueryResponse.java
//SOURCES src/main/java/io/quarkus/cve/model/OsvBatchQueryRequest.java
//SOURCES src/main/java/io/quarkus/cve/model/OsvBatchQueryResponse.java
//SOURCES src/main/java/io/quarkus/cve/client/OsvApiClient.java
//SOURCES src/main/java/io/quarkus/cve/client/QuarkusRegistryClient.java
//SOURCES src/main/java/io/quarkus/cve/client/MavenCentralClient.java
//SOURCES src/main/java/io/quarkus/cve/client/GitHubAdvisoryClient.java
//SOURCES src/main/java/io/quarkus/cve/client/GitHubAuthFilter.java
//SOURCES src/main/java/io/quarkus/cve/client/HttpLogFilter.java
//SOURCES src/main/java/io/quarkus/cve/model/UnifiedVuln.java
//SOURCES src/main/java/io/quarkus/cve/model/ReleaseReport.java
//SOURCES src/main/java/io/quarkus/cve/service/QuarkusBomService.java
//SOURCES src/main/java/io/quarkus/cve/service/ReleaseReportService.java
//SOURCES src/main/java/io/quarkus/cve/service/QuarkusArtifactService.java
//SOURCES src/main/java/io/quarkus/cve/service/GitHubReleaseSyncService.java
//SOURCES src/main/java/io/quarkus/cve/util/UnquotedDateSerializer.java
//SOURCES src/main/java/io/quarkus/cve/util/UnquotedDateOrStringSerializer.java
//SOURCES src/main/java/io/quarkus/cve/util/Log.java
//SOURCES src/main/java/io/quarkus/cve/util/Cache.java
//SOURCES src/main/java/io/quarkus/cve/util/Stats.java
//SOURCES src/main/java/io/quarkus/cve/util/Retry.java
//SOURCES src/main/java/io/quarkus/cve/util/OutputFormat.java
//SOURCES src/main/java/io/quarkus/cve/util/ReleaseReportRenderer.java
//SOURCES src/main/java/io/quarkus/cve/util/Printer.java
//SOURCES src/main/java/io/quarkus/cve/command/CveUtilCommand.java
//SOURCES src/main/java/io/quarkus/cve/command/ListCommand.java
//SOURCES src/main/java/io/quarkus/cve/command/GetCommand.java
//SOURCES src/main/java/io/quarkus/cve/command/SearchCommand.java
//SOURCES src/main/java/io/quarkus/cve/command/ReleasesCommand.java
//SOURCES src/main/java/io/quarkus/cve/command/CveUtilApplication.java
//FILES src/main/resources/application.properties
//JAVAC_OPTIONS -parameters
//Q:CONFIG quarkus.banner.enabled=false
//Q:CONFIG quarkus.log.level=WARN
//Q:CONFIG quarkus.rest-client.osv-api.url=https://api.osv.dev
//Q:CONFIG quarkus.rest-client.maven-central.url=https://repo1.maven.org/maven2
//Q:CONFIG quarkus.rest-client.maven-central.connect-timeout=10000
//Q:CONFIG quarkus.rest-client.maven-central.read-timeout=30000
//Q:CONFIG quarkus.rest-client.quarkus-registry.url=https://registry.quarkus.io
//Q:CONFIG quarkus.rest-client.github-api.url=https://api.github.com
//Q:CONFIG quarkus.rest-client.osv-api.connect-timeout=10000
//Q:CONFIG quarkus.rest-client.osv-api.read-timeout=30000
//Q:CONFIG quarkus.rest-client.quarkus-registry.connect-timeout=10000
//Q:CONFIG quarkus.rest-client.quarkus-registry.read-timeout=30000
//Q:CONFIG quarkus.rest-client.github-api.connect-timeout=10000
//Q:CONFIG quarkus.rest-client.github-api.read-timeout=30000

// Entry point for jbang — delegates to the Quarkus @QuarkusMain class
// via the CveUtilApplication class that is included via //SOURCES above.
//
// Launch:
//   jbang run.java list                                    # scan all 329 Quarkus artifacts
//   jbang run.java list --package io.quarkus:quarkus-oidc  # single artifact
//   jbang run.java get GHSA-xxxx-xxxx-xxxx
//   jbang run.java get CVE-2024-12345
//   jbang run.java search --keyword "injection"
//   jbang run.java search --keyword "bypass" --severity HIGH
//
// Install as a global alias:
//   jbang app install --name cve-util run.java
//   cve-util list

import io.quarkus.runtime.Quarkus;

public class run {
    public static void main(String... args) {
        Quarkus.run(io.quarkus.cve.command.CveUtilApplication.class, args);
    }
}
