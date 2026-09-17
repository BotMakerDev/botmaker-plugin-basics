package com.botmaker.plugin.basics.store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A plugin's own state, written as a record and read back as one — and every way that can fail. */
class PluginStoreTest {

    private static final String ID = "com.example.plugin";

    record Targets(String window, List<String> images) {}

    record Narrower(String window) {}

    @Test
    void aRecordSurvivesTheRoundTrip(@TempDir Path resources) throws IOException {
        PluginStore store = PluginStore.of(resources, ID);
        Targets written = new Targets("Game", List.of("hp.png", "mp.png"));

        assertFalse(store.has("capture"));
        store.write("capture", written);
        assertTrue(store.has("capture"));
        assertEquals(Optional.of(written), store.read("capture", Targets.class));
    }

    @Test
    void itWritesWherePluginDataSays(@TempDir Path resources) throws IOException {
        PluginStore.of(resources, "com.botmaker.sdk").write("capture", new Narrower("Game"));
        // The layout is PluginData's and is asserted here too, because this is the class a plugin author
        // uses — if the two ever disagreed, a plugin would write one file and read another.
        assertTrue(Files.isRegularFile(resources.resolve("plugins/com.botmaker/sdk/capture.json")));
    }

    @Test
    void nothingStoredReadsAsEmpty(@TempDir Path resources) {
        assertEquals(Optional.empty(), PluginStore.of(resources, ID).read("capture", Targets.class));
        assertEquals(List.of(), PluginStore.of(resources, ID).readAll("capture", Targets.class));
    }

    @Test
    void unparseableAndWronglyShapedBothReadAsEmpty(@TempDir Path resources) throws IOException {
        PluginData data = PluginData.of(resources, ID);
        Files.createDirectories(data.folder());
        Files.writeString(data.file("broken"), "{ not json");
        Files.writeString(data.file("wrong"), "\"a bare string\"");

        PluginStore store = PluginStore.of(resources, ID);
        assertEquals(Optional.empty(), store.read("broken", Targets.class));
        assertEquals(Optional.empty(), store.read("wrong", Targets.class));
    }

    @Test
    void aFieldTheRecordNoLongerDeclaresIsIgnored(@TempDir Path resources) throws IOException {
        PluginStore store = PluginStore.of(resources, ID);
        store.write("capture", new Targets("Game", List.of("hp.png")));

        // The plugin dropped `images` from its record in a later version: the old file still reads.
        assertEquals(Optional.of(new Narrower("Game")), store.read("capture", Narrower.class));
    }

    @Test
    void aFieldTheFileOmitsTakesTheTypesOwnDefault(@TempDir Path resources) throws IOException {
        PluginData data = PluginData.of(resources, ID);
        Files.createDirectories(data.folder());
        Files.writeString(data.file("capture"), "{\"window\":\"Game\"}");

        assertEquals(Optional.of(new Targets("Game", null)),
                PluginStore.of(resources, ID).read("capture", Targets.class));
    }

    @Test
    void aListRoundTripsAndANonArrayReadsAsEmpty(@TempDir Path resources) throws IOException {
        PluginStore store = PluginStore.of(resources, ID);
        List<Narrower> windows = List.of(new Narrower("Game"), new Narrower("Launcher"));
        store.write("windows", windows);
        assertEquals(windows, store.readAll("windows", Narrower.class));

        store.write("one", new Narrower("Game"));
        assertEquals(List.of(), store.readAll("one", Narrower.class));
    }

    @Test
    void writingWhereNothingCanBeWrittenThrows(@TempDir Path resources) throws IOException {
        // A save that silently did not happen is the one failure a user cannot see, so this is the one
        // call in the class that does not answer totally. The folder is taken by a file of the same name.
        Path taken = resources.resolve("plugins");
        Files.writeString(taken, "not a directory");
        assertThrows(IOException.class, () -> PluginStore.of(resources, ID).write("capture",
                new Narrower("Game")));
    }
}
