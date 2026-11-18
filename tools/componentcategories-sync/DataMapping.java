package io.quarkiverse.roq.data.runtime.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Temporary code! Remove with https://github.com/quarkusio/quarkusio.github.io/pull/3251.
 * Compile-only stand-in for Roq's {@code @DataMapping}, so that this script can use the website's
 * {@code ComponentCategories} data mapping without the quarkus-roq-data extension (which needs a
 * Roq project to build).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(java.lang.annotation.ElementType.TYPE)
public @interface DataMapping {
    String value();
}
