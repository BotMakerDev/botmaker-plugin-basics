package com.botmaker.plugin.basics.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One JSON document, read totally and written whole.
 *
 * <p>The sections went on 2026-09-10 and so did the two tests that held them — a plugin's data is a file of
 * its own now, which is {@link PluginDataTest}'s subject. What is left here is the property every reader in
 * this module depends on: <b>reading never fails</b>, whatever the file is or is not, and writing does.
 */
class ProjectStoreTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String DOCUMENT = """
            {
              "schemaVersion": 3,
              "parameters": [{"name": "rest", "value": ["90s"]}]
            }
            """;

    @Test
    void aDocumentIsReadAsItIsWritten() {
        ProjectStore store = ProjectStore.of(DOCUMENT);

        assertEquals(3, store.root().path("schemaVersion").asInt());
        assertEquals("90s", store.root().path("parameters").get(0).path("value").get(0).asText());
    }

    @Test
    void aStoreRoundTripsThroughAFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("sub").resolve("parameters.json");
        ProjectStore.of(DOCUMENT).write(file);

        assertTrue(Files.isRegularFile(file), "the directory was not created");
        assertEquals("90s", ProjectStore.read(file).root()
                .path("parameters").get(0).path("value").get(0).asText());
    }

    /** A project is created before anything is stored in it, so an absent file is not an error. */
    @Test
    void anAbsentOrUnreadableFileIsAnEmptyStore(@TempDir Path dir) {
        assertTrue(ProjectStore.read(dir.resolve("nothing.json")).root().isEmpty());
        assertTrue(ProjectStore.read(null).root().isEmpty());
        assertTrue(ProjectStore.of("{ not json").root().isEmpty());
        assertTrue(ProjectStore.of((String) null).root().isEmpty());
        assertTrue(ProjectStore.empty().root().isEmpty());
    }

    /** A tree handed in is copied, so a caller that goes on editing it cannot change what was stored. */
    @Test
    void aTreeIsCopiedRatherThanHeld() throws IOException {
        var document = (com.fasterxml.jackson.databind.node.ObjectNode) MAPPER.readTree("{\"a\": 1}");
        ProjectStore store = ProjectStore.of(document);
        document.put("a", 2);

        assertEquals(1, store.root().path("a").asInt());
    }

    /** The values a bot reads are one document's, and today that document is one plugin's own file. */
    @Test
    void theValuesAreOneDocument() {
        assertEquals(List.of("90s"), ProjectValues.in(ProjectStore.of(DOCUMENT)).many("rest"));
        assertEquals(List.of(), ProjectValues.in(null).variables());
    }
}
