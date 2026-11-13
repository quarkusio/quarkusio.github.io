package io.quarkusio.releases;

import java.time.LocalDate;

/**
 * One micro release of a minor, e.g. 3.27.6.1 of 3.27. The release date is null when unknown.
 */
public record Micro(String version, LocalDate releaseDate) {
}
