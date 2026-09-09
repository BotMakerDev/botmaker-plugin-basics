package com.botmaker.plugin.basics.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two properties the store exists for: <b>a plugin's data survives an editor that has never heard of
 * it</b>, and <b>a project written before sections existed still reads</b>.
 */
class ProjectStoreTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String SECTIONED = """
            {
              "schemaVersion": 3,
              "plugins": {
                "com.botmaker.basics": {"variables": [{"name": "rest", "value": ["90s"]}]},
                "com.example.discord": {"webhook": "https://example.invalid/hook"}
              }
            }
            """;

    /** What every project written so far holds: activities and variables at the top level, no owner. */
    private static final String LEGACY = """
            {
              "schemaVersion": 2,
              "activities": [{"name": "Mining", "enabled": true}],
              "variables": [{"name": "rest", "value": ["90s"]}]
            }
            """;

    @Test
    void aSectionIsOnlyItsOwnersData() {
        ProjectStore store = ProjectStore.of(SECTIONED);
        assertTrue(store.isSectioned());
        assertEquals(List.of("com.botmaker.basics", "com.example.discord"), store.sections());
        assertEquals("https://example.invalid/hook",
                store.section("com.example.discord").path("webhook").asText());
        assertTrue(store.section("com.example.discord").path("variables").isMissingNode());
    }

    @Test
    void anIdNobodyStoredAnythingUnderIsMissingRatherThanEmpty() {
        assertTrue(ProjectStore.of(SECTIONED).section("com.example.nothing").isMissingNode());
        assertTrue(ProjectStore.of(SECTIONED).section(null).isMissingNode());
        // An empty store is unsectioned, so it takes the legacy arm and answers its root — which holds
        // nothing. Reading it is the same experience either way; only the node differs.
        assertTrue(ProjectStore.empty().section("com.botmaker.basics").isEmpty());
    }

    /** A legacy file is one unsectioned document, so every id reads it — see the class note on the store. */
    @Test
    void aFileWrittenBeforeSectionsAnswersItsRootForEveryId() {
        ProjectStore store = ProjectStore.of(LEGACY);
        assertFalse(store.isSectioned());
        assertEquals(List.of(), store.sections());
        assertEquals("Mining", store.section("com.botmaker.basics").path("activities").get(0)
                .path("name").asText());
        assertEquals("Mining", store.section("com.botmaker.sdk").path("activities").get(0)
                .path("name").asText());
    }

    /**
     * The rule that makes one file safe for several plugins: writing one section copies every other one
     * through. An editor without the Discord plugin installed must not save the webhook away.
     */
    @Test
    void writingOneSectionCarriesEveryOtherOneThrough() throws IOException {
        ProjectStore written = ProjectStore.of(SECTIONED)
                .withSection("com.botmaker.basics", MAPPER.readTree("{\"variables\": []}"));

        assertEquals("https://example.invalid/hook",
                written.section("com.example.discord").path("webhook").asText());
        assertEquals(0, written.section("com.botmaker.basics").path("variables").size());
        assertEquals(3, written.root().path("schemaVersion").asInt(), "the rest of the file is untouched");
    }

    @Test
    void aStoreIsNotChangedByWritingASectionOfIt() throws IOException {
        ProjectStore original = ProjectStore.of(SECTIONED);
        original.withSection("com.botmaker.basics", MAPPER.readTree("{\"variables\": []}"));
        assertEquals(1, original.section("com.botmaker.basics").path("variables").size(),
                "withSection mutated the store it was called on");
    }

    /** Converting a legacy file adds the sections beside the old keys rather than instead of them. */
    @Test
    void writingASectionOfALegacyFileLeavesTheOldKeysAlone() throws IOException {
        ProjectStore converted = ProjectStore.of(LEGACY)
                .withSection("com.botmaker.basics", MAPPER.readTree("{\"variables\": []}"));

        assertTrue(converted.isSectioned());
        assertEquals(0, converted.section("com.botmaker.basics").path("variables").size());
        assertEquals("Mining", converted.root().path("activities").get(0).path("name").asText());
    }

    @Test
    void aSectionMustBelongToSomebody() {
        assertThrows(IllegalArgumentException.class,
                () -> ProjectStore.empty().withSection("  ", MAPPER.createObjectNode()));
    }

    @Test
    void aStoreRoundTripsThroughAFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("sub").resolve(ProjectStore.FILE);
        ProjectStore.of(SECTIONED).write(file);

        assertTrue(Files.isRegularFile(file), "the directory was not created");
        ProjectStore back = ProjectStore.read(file);
        assertEquals(List.of("com.botmaker.basics", "com.example.discord"), back.sections());
        assertEquals("90s", back.section("com.botmaker.basics")
                .path("variables").get(0).path("value").get(0).asText());
    }

    /** A project is created before anything is stored in it, so an absent file is not an error. */
    @Test
    void anAbsentOrUnreadableFileIsAnEmptyStore(@TempDir Path dir) {
        assertTrue(ProjectStore.read(dir.resolve("nothing.json")).sections().isEmpty());
        assertTrue(ProjectStore.read(null).sections().isEmpty());
        assertTrue(ProjectStore.of("{ not json").sections().isEmpty());
    }

    /** The values a bot reads are one section of the store, and today that section is the legacy root. */
    @Test
    void theValuesAreOneSectionOfTheStore() {
        assertEquals(List.of("90s"),
                ProjectValues.in(ProjectStore.of(SECTIONED), ProjectStore.BASICS_ID).many("rest"));
        assertEquals(List.of("90s"),
                ProjectValues.in(ProjectStore.of(LEGACY), ProjectStore.BASICS_ID).many("rest"));
        assertTrue(ProjectValues.in(ProjectStore.of(SECTIONED), "com.example.nothing").variables()
                .isEmpty());
    }
}
