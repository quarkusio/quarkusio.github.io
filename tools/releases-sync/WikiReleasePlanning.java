import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the Quarkus "Release Planning" wiki page (raw markdown).
 *
 * <p>Each release is a level-2 heading such as {@code ## 4.2 (Proposal)} or {@code ## 3.40 LTS},
 * followed by bullets with free-form dates ("March 17th 2027"). Headings with any other suffix
 * (e.g. "4.0 Beta1") or no bullet with a Platform release date are ignored.
 */
public class WikiReleasePlanning {

    public static final String WIKI_URL =
            "https://raw.githubusercontent.com/wiki/quarkusio/quarkus/Release-Planning.md";

    private static final Pattern HEADING = Pattern.compile(
            "^##\\s+(\\d+\\.\\d+)(\\s+LTS)?(\\s+\\(Proposal\\))?\\s*$");
    private static final Pattern PLATFORM_LINE = Pattern.compile(
            "^\\s*[-*]\\s+Full Platform release\\b.*?\\bon\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE = Pattern.compile(
            "([A-Za-z]{3,9})\\.?\\s+(\\d{1,2})(?:st|nd|rd|th)?,?\\s+(\\d{4})");

    public record PlannedRelease(String version, boolean lts, boolean proposal, LocalDate platformDate) {
    }

    /** All releases on the page that have a Platform release date, in page order (newest first). */
    public static List<PlannedRelease> parse(String markdown) {
        return parse(markdown, version -> { });
    }

    /**
     * Like {@link #parse(String)}, but reports the version of every release heading that was skipped
     * because no Platform release date could be read from it.
     */
    public static List<PlannedRelease> parse(String markdown, Consumer<String> onSkipped) {
        List<PlannedRelease> result = new ArrayList<>();
        String version = null;
        boolean lts = false;
        boolean proposal = false;
        LocalDate platformDate = null;

        for (String line : markdown.split("\\R")) {
            if (line.startsWith("#")) {
                addIfComplete(result, version, lts, proposal, platformDate, onSkipped);
                Matcher heading = HEADING.matcher(line.stripTrailing());
                if (heading.matches()) {
                    version = heading.group(1);
                    lts = heading.group(2) != null;
                    proposal = heading.group(3) != null;
                } else {
                    version = null;
                }
                platformDate = null;
                continue;
            }
            if (version == null || platformDate != null) {
                continue;
            }
            Matcher platform = PLATFORM_LINE.matcher(line);
            if (platform.matches()) {
                platformDate = parseDate(platform.group(1));
            }
        }
        addIfComplete(result, version, lts, proposal, platformDate, onSkipped);
        return result;
    }

    /** Releases whose Platform release date is today or later. */
    public static List<PlannedRelease> upcoming(List<PlannedRelease> planned, LocalDate today) {
        return planned.stream()
                .filter(r -> !r.platformDate().isBefore(today))
                .toList();
    }

    static LocalDate parseDate(String text) {
        Matcher m = DATE.matcher(text);
        if (!m.find()) {
            return null;
        }
        Month month = parseMonth(m.group(1));
        if (month == null) {
            return null;
        }
        try {
            return LocalDate.of(Integer.parseInt(m.group(3)), month, Integer.parseInt(m.group(2)));
        } catch (java.time.DateTimeException e) {
            return null;
        }
    }

    private static Month parseMonth(String name) {
        String prefix = name.substring(0, 3).toLowerCase(Locale.ROOT);
        for (Month month : Month.values()) {
            if (month.name().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                return month;
            }
        }
        return null;
    }

    private static void addIfComplete(List<PlannedRelease> result, String version, boolean lts,
            boolean proposal, LocalDate platformDate, Consumer<String> onSkipped) {
        if (version == null) {
            return;
        }
        if (platformDate != null) {
            result.add(new PlannedRelease(version, lts, proposal, platformDate));
        } else {
            onSkipped.accept(version);
        }
    }
}
