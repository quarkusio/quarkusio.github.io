package io.quarkus.tools.migration.asciidoc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ConfigTableTreeprocessorTest {

    static Asciidoctor asciidoctor;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
    }

    @AfterAll
    static void cleanup() {
        asciidoctor.close();
    }

    private String convert(String adoc) {
        return asciidoctor.convert(adoc, Options.builder()
                .attributes(Attributes.builder()
                        .attribute("add-copy-button-to-config-props", "true")
                        .attribute("add-copy-button-to-env-var", "true")
                        .build())
                .build());
    }

    static final String CONFIG_TABLE = """
            [.configuration-legend]
            Configuration property fixed at build time
            [.configuration-reference.searchable, cols="80,.^10,.^10"]
            |===

            h|Configuration property
            h|Type
            h|Default

            a|`quarkus.redis.hosts`
            config_property_copy_button:quarkus.redis.hosts[]

            [.description]
            --
            The redis hosts to use while connecting to the redis server.

            Environment variable: env_var_with_copy_button:QUARKUS_REDIS_HOSTS[]
            --
            |string
            |`localhost:6379`

            a|`quarkus.redis.timeout`

            [.description]
            --
            Environment variable: env_var_with_copy_button:QUARKUS_REDIS_TIMEOUT[]
            --
            |Duration link:#duration-note-anchor-{summaryTableId}[icon:question-circle[], title=More information about the Duration format]
            |`10`

            |===
            """;

    @Test
    void addsAllRowsClassToTable() {
        String html = convert(CONFIG_TABLE);
        assertTrue(html.contains("configuration-reference-all-rows"));
    }

    @Test
    void collapsibleDescriptionGetsId() {
        String html = convert(CONFIG_TABLE);
        assertTrue(html.contains("conf-collapsible-desc-"));
    }

    @Test
    void collapsibleDescriptionGetsDecoration() {
        String html = convert(CONFIG_TABLE);
        assertTrue(html.contains("description-decoration"));
        assertTrue(html.contains("fa fa-chevron-down"));
        assertTrue(html.contains("Show more"));
    }

    @Test
    void nonCollapsibleDescriptionGetsNonCollapsibleId() {
        String html = convert(CONFIG_TABLE);
        assertTrue(html.contains("conf-non-collapsible-desc-"));
    }

    @Test
    void collapsibleRowGetsRowClasses() {
        String html = convert(CONFIG_TABLE);
        assertTrue(html.contains("row-collapsible"));
        assertTrue(html.contains("row-collapsed"));
    }

    @Test
    void searchInputInjected() {
        String html = convert(CONFIG_TABLE);
        assertTrue(html.contains("FILTER CONFIGURATION"));
        assertTrue(html.contains("type=\"search\""));
    }

    @Test
    void nonSearchableTableHasNoSearchInput() {
        String adoc = """
                [.configuration-reference, cols="80,.^10,.^10"]
                |===

                h|Property
                h|Type
                h|Default

                a|`quarkus.test`

                [.description]
                --
                A simple property.

                Environment variable: env_var_with_copy_button:QUARKUS_TEST[]
                --
                |string
                |

                |===
                """;
        String html = convert(adoc);
        assertFalse(html.contains("FILTER CONFIGURATION"));
        assertTrue(html.contains("configuration-reference-all-rows"));
    }

    @Test
    void searchableTableRendersViaDocConvert() {
        // Roq uses asciidoctor.load() + doc.convert(), not asciidoctor.convert().
        // Calling getContent() on a sibling block during tree processing can
        // trigger premature conversion that causes doc.convert() to silently
        // drop the table. This test uses the same code path as Roq.
        var doc = asciidoctor.load(CONFIG_TABLE, Options.builder()
                .attributes(Attributes.builder()
                        .attribute("add-copy-button-to-config-props", "true")
                        .attribute("add-copy-button-to-env-var", "true")
                        .build())
                .build());
        String html = doc.convert();
        assertTrue(html.contains("<table"), "Table should be present in doc.convert() output");
        assertTrue(html.contains("FILTER CONFIGURATION"), "Search input should be present");
        assertTrue(html.contains("quarkus.redis.hosts"), "Config property content should be present");
    }

    @Test
    void legendKeepsItsRole() {
        String html = convert(CONFIG_TABLE);
        assertTrue(html.contains("configuration-legend"),
                "The [.configuration-legend] role must survive the search input injection");
    }

    @Test
    void attributeDefinedAboveTheLegendIsExpandedInCells() {
         String adoc = """
                = A guide

                Some prose, so the attribute entry below lands in the document body.

                :summaryTableId: quarkus-redis
                """ + CONFIG_TABLE;
        String html = convert(adoc);
        assertFalse(html.contains("{summaryTableId}"),
                "{summaryTableId} should be expanded in table cells");
        assertTrue(html.contains("duration-note-anchor-quarkus-redis"));
    }

    @Test
    void attributeDefinedAboveANonSearchableTableIsExpandedInCells() {
        // The other shape the generated config docs come in: no legend, so the
        // attribute entry is attached to the table itself. See #2962.
        String adoc = """
                = A guide

                Some prose, so the attribute entry below lands in the document body.

                :summaryTableId: quarkus-redis
                [.configuration-reference, cols="80,.^10,.^10"]
                |===

                h|Configuration property
                h|Type
                h|Default

                a|`quarkus.redis.timeout`

                [.description]
                --
                A timeout.
                --
                |Duration link:#duration-note-anchor-{summaryTableId}[icon:question-circle[]]
                |`10`

                |===
                """;
        String html = convert(adoc);
        assertFalse(html.contains("{summaryTableId}"),
                "{summaryTableId} should be expanded in table cells");
        assertTrue(html.contains("duration-note-anchor-quarkus-redis"));
    }

    @Test
    void noConfigTableLeavesDocumentUnchanged() {
        String html = convert("== Just a heading\n\nSome text.");
        assertFalse(html.contains("configuration-reference"));
        assertFalse(html.contains("row-collapsible"));
    }
}
