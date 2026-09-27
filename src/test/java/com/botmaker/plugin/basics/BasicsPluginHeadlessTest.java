package com.botmaker.plugin.basics;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link BasicsPlugin}'s type list is readable on a classpath with no JavaFX on it.
 *
 * <p>{@code botmaker plugin validate} and the registry's CI load a plugin headless, and read its
 * {@code types()} to check every declared type round-trips. Until 2026-09-28 this plugin's types could not be
 * read there at all: a preview written {@code ctx -> new Label(…)} answered a {@code Label} where a
 * {@code Node} is declared, so the verifier loaded both classes when {@code BasicsTypes} loaded, and the
 * validator skipped the check with "could not be linked". The previews are {@code BasicsEditors} methods now,
 * named through the toolkit's {@code Types.Drawn}, and nothing JavaFX-shaped is linked until the host draws.
 *
 * <p>The classpath is this test's own minus the JavaFX jars, under the platform loader — the SDK's
 * {@code SdkPluginHeadlessTest} explains why not the application loader.
 */
class BasicsPluginHeadlessTest {

    @Test
    void theTypesAreReadWithNoJavaFx() throws Exception {
        try (URLClassLoader headless = withoutJavaFx()) {
            Class<?> plugin = Class.forName(BasicsPlugin.class.getName(), true, headless);
            Object instance = plugin.getDeclaredConstructor().newInstance();

            List<?> types = (List<?>) plugin.getMethod("types").invoke(instance);
            assertEquals(13, types.size());
        }
    }

    /** The absence is real: without this the test above would pass if the filter stopped matching. */
    @Test
    void theHeadlessClasspathReallyHasNoJavaFx() throws Exception {
        try (URLClassLoader headless = withoutJavaFx()) {
            assertThrows(ClassNotFoundException.class, () -> Class.forName("javafx.scene.Node", false, headless));
        }
    }

    private static URLClassLoader withoutJavaFx() throws Exception {
        List<URL> urls = new ArrayList<>();
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            if (!Path.of(entry).getFileName().toString().startsWith("javafx")) {
                urls.add(Path.of(entry).toUri().toURL());
            }
        }
        assertTrue(urls.size() > 1, "the classpath was not split into entries");
        return new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getPlatformClassLoader());
    }
}
