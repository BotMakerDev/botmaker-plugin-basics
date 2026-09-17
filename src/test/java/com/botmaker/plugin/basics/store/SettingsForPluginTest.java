package com.botmaker.plugin.basics.store;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The bot's side of {@link PluginStore}: the same files, read off the classpath instead of a directory.
 *
 * <p>The fixtures are real resources under {@code src/test/resources/plugins/…} rather than a temporary
 * directory, because <b>the thing being tested is the classpath path</b> — a test that handed the class a
 * folder would pass while every bot read nothing.
 */
class SettingsForPluginTest {

    private static final String ID = "com.example.plugin";

    record Targets(String window, List<String> images) {}

    record Window(String window) {}

    @Test
    void aPluginsRecordIsReadOffTheClasspath() {
        assertEquals(Optional.of(new Targets("Game", List.of("hp.png", "mp.png"))),
                Settings.forPlugin(ID).read("capture", Targets.class));
    }

    @Test
    void aListIsReadTheSameWay() {
        assertEquals(List.of(new Window("Game"), new Window("Launcher")),
                Settings.forPlugin(ID).readAll("windows", Window.class));
    }

    @Test
    void aPluginThatStoredNothingReadsAsEmpty() {
        // Also what a misspelled id reads as, and the two are deliberately indistinguishable: a bot cannot
        // tell "not installed" from "installed and silent", and neither answer is one it can act on.
        assertEquals(Optional.empty(), Settings.forPlugin(ID).read("nothing-here", Targets.class));
        assertEquals(Optional.empty(), Settings.forPlugin("com.example.absent").read("capture", Targets.class));
        assertEquals(List.of(), Settings.forPlugin("com.example.absent").readAll("windows", Window.class));
    }

    @Test
    void aDocumentThatIsNotAnArrayReadsAsAnEmptyList() {
        assertEquals(List.of(), Settings.forPlugin(ID).readAll("capture", Window.class));
    }

    @Test
    void theHandleRemembersItsPlugin() {
        assertEquals(ID, Settings.forPlugin(ID).pluginId());
    }
}
