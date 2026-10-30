package io.quarkusio.releases;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonCreator;

/** herodevs_eol in the data is either a date or the string "nes" (never-ending support). */
public record HeroDevsEol(LocalDate date) {

    public static final HeroDevsEol NES = new HeroDevsEol(null);

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static HeroDevsEol parse(String value) {
        return "nes".equalsIgnoreCase(value) ? NES : new HeroDevsEol(LocalDate.parse(value));
    }

    public boolean isNes() {
        return date == null;
    }
}
