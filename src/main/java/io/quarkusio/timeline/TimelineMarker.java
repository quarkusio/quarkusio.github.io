package io.quarkusio.timeline;

import java.time.LocalDate;

public record TimelineMarker(LocalDate date, String label, String kind, String xPct) {
}
