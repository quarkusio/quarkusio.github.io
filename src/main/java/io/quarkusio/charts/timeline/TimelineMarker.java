package io.quarkusio.charts.timeline;

import java.time.LocalDate;

import io.quarkusio.releases.ReleaseKind;

public record TimelineMarker(LocalDate date, String label, ReleaseKind kind, String xPct) {
}
