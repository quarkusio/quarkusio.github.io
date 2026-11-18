package io.quarkusio.releases;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkusio.releases")
public interface ReleasesConfig {

    /** The most upcoming releases to show in the releases table and timeline, soonest first. 0 hides them all. */
    @WithDefault("1")
    int maxUpcoming();
}
