package com.botmaker.plugin.basics.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The three properties the folder tree exists for: <b>a folder per author and a folder per plugin</b>,
 * <b>a file created on demand and never twice under two spellings</b>, and <b>a bot that enumerates
 * nothing</b>.
 */
class PluginDataTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void theTreeIsDerivedFromTheIdAlone(@TempDir Path dir) {
        assertEquals(dir.resolve("plugins/com.botmaker/sdk"),
                PluginData.of(dir, "com.botmaker.sdk").folder());
        assertEquals(dir.resolve("plugins/com.botmaker/basics/settings.json"),
                PluginData.of(dir, PluginData.BASICS_ID).file("settings"));
    }

    /** An id with no author in it has no author folder to invent. */
    @Test
    void anIdWithNoDotIsOneFolder(@TempDir Path dir) {
        assertEquals(dir.resolve("plugins/discord"), PluginData.of(dir, "discord").folder());
        assertEquals(dir.resolve("plugins/com.example."), PluginData.of(dir, "com.example.").folder());
    }

    @Test
    void aFileIsCreatedOnDemandAndReadsBack(@TempDir Path dir) throws IOException {
        PluginData data = PluginData.of(dir, "com.example.discord");
        assertFalse(data.has("webhooks"));
        assertTrue(data.read("webhooks").isEmpty(), "nothing stored reads as an empty document");

        data.write("webhooks", MAPPER.readTree("{\"url\": \"https://example.invalid/hook\"}"));

        assertTrue(data.has("webhooks"));
        assertTrue(Files.isRegularFile(dir.resolve("plugins/com.example/discord/webhooks.json")));
        assertEquals("https://example.invalid/hook",
                data.read("webhooks").path("url").asText());
    }

    /**
     * The name is normalised, so one file cannot become several that shadow each other — the thing a folder
     * of files can do that a single document could not.
     */
    @Test
    void oneNameIsOneFileHoweverItIsSpelled(@TempDir Path dir) throws IOException {
        PluginData data = PluginData.of(dir, "com.example.discord");
        data.write("Webhooks", MAPPER.readTree("{\"url\": \"one\"}"));
        data.write("webhooks.json", MAPPER.readTree("{\"url\": \"two\"}"));

        assertEquals("two", data.read("WEBHOOKS").path("url").asText());
        try (var files = Files.list(dir.resolve("plugins/com.example/discord"))) {
            assertEquals(1, files.count(), "two spellings became two files");
        }
    }

    @Test
    void aNameThatLeavesNoCharactersIsRefused(@TempDir Path dir) {
        PluginData data = PluginData.of(dir, "com.example.discord");
        assertThrows(IllegalArgumentException.class, () -> data.file(" "));
        assertThrows(IllegalArgumentException.class, () -> data.file("///"));
        assertThrows(IllegalArgumentException.class, () -> data.file(".json"));
        assertEquals("my-data", PluginData.normalize("My Data"));
    }

    @Test
    void dataBelongsToAPluginAndToAProject(@TempDir Path dir) {
        assertThrows(IllegalArgumentException.class, () -> PluginData.of(dir, "  "));
        assertThrows(IllegalArgumentException.class, () -> PluginData.of(dir, null));
        assertThrows(IllegalArgumentException.class, () -> PluginData.of(null, "com.example.discord"));
    }

    /** A bot resolves one resource path from the id and the name, and never lists a directory. */
    @Test
    void aBotResolvesOnePathWithoutScanning() {
        assertEquals("/plugins/com.botmaker/sdk/settings.json",
                PluginData.resource("com.botmaker.sdk", "settings"));
        assertEquals("/plugins/discord/webhooks.json",
                PluginData.resource("discord", "Webhooks.json"));
    }
}
