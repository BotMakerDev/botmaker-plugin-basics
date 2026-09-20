package com.botmaker.plugin.basics;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Most of this module runs in a bot, and a bot has neither the plugin contract nor JavaFX.
 *
 * <p>This jar reaches a bot's classpath because {@code botmaker-sdk} declares it at {@code compile} scope,
 * so it travels with the SDK wherever the SDK goes. The contract and JavaFX are {@code provided} here — the
 * host supplies them, which is right for the editor-side half and false for a bot, which has only what its
 * plugins bring at {@code compile} scope.
 *
 * <p><b>The exemption list is the inverse of the old rule, and that is the improvement.</b> Its ancestor
 * ({@code ToolkitConfigIsBotSafeTest}, in the toolkit, for one day) named the one package that had to be
 * safe, so a new class was unchecked by default. Here a new class is <em>checked</em> by default and has to
 * be named below to escape — which errs in the direction where the mistake is a red test rather than a
 * {@code NoClassDefFoundError} in a stranger's bot.
 *
 * <p><b>It is a source scan, not a classpath scan, and that is deliberate.</b> A {@code provided} dependency
 * is on this module's own test classpath, so every one of these classes loads perfectly here whatever it
 * names. The failure appears only where the dependency is absent. That is the same shape as the three
 * {@code optional}-means-not-transitive bugs this project has shipped, and the same reason
 * {@code StudioSourcesTest} reads source: the way to break the rule is invisible to a build that has the
 * thing being banned.
 */
class BasicsIsBotSafeTest {

    private static final Path SOURCES = Path.of("src/main/java/com/botmaker/plugin/basics");

    /**
     * The editor-side classes, which a bot never loads.
     *
     * <p>{@code BasicsPlugin} <em>is</em> the contract implementation, and {@code BasicsValueTypes} holds the
     * registrations, the labels and the Java literals — everything only a host or a generator asks for.
     * {@code ParameterStore} joined them on 2026-09-10: it takes {@code ParameterRow}s and gives them back,
     * which is the editor's half of the parameter mechanism, and a bot reads the very same file through
     * {@code ProjectValues.forPlugin} as untyped text. {@code StoredForms} joined them on 2026-09-20 for the
     * same reason and with the same reader on the other side: it decodes the type a stored row declares,
     * which is a {@code ValueForm}, and a bot never decodes one. All four are in the same jar as the
     * bot-safe half, exactly as {@code SdkPlugin} sits in the SDK's jar: what matters is that nothing a bot
     * links reaches them.
     */
    private static final Set<String> EDITOR_ONLY = Set.of(
            "BasicsPlugin.java", "BasicsValueTypes.java", "ParameterStore.java", "StoredForms.java");

    /** What a bot's classpath does not have. Javadoc mentions are fine; a source reference is not. */
    private static final List<String> BANNED = List.of("com.botmaker.plugin.api", "javafx.");

    @Test
    void everyExemptedFileStillExists() throws IOException {
        List<String> names = sources().stream().map(p -> p.getFileName().toString()).toList();
        for (String exempt : EDITOR_ONLY) {
            assertTrue(names.contains(exempt),
                    exempt + " is exempted from the bot-safety scan and no longer exists — drop it from the"
                            + " list rather than leaving a hole with nothing behind it");
        }
    }

    @Test
    void nothingABotLoadsNamesATypeABotWillNotHave() throws IOException {
        List<String> offences = new ArrayList<>();
        for (Path file : sources()) {
            if (EDITOR_ONLY.contains(file.getFileName().toString())) continue;
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (isComment(line)) continue;
                for (String banned : BANNED) {
                    if (line.contains(banned)) {
                        offences.add(file.getFileName() + ":" + (i + 1) + " names " + banned);
                    }
                }
            }
        }
        assertEquals(List.of(), offences,
                "this jar is on every bot's classpath, and a bot has neither the contract nor JavaFX");
    }

    /**
     * Whether the line is javadoc or a comment. Crude on purpose: a false <em>negative</em> here is a
     * spurious failure somebody reads and fixes, while the alternative — parsing Java — is a second compiler.
     */
    private static boolean isComment(String line) {
        String trimmed = line.strip();
        return trimmed.startsWith("*") || trimmed.startsWith("/*") || trimmed.startsWith("//");
    }

    private static List<Path> sources() throws IOException {
        assertTrue(Files.isDirectory(SOURCES),
                "expected " + SOURCES.toAbsolutePath() + " — has the package moved?");
        try (Stream<Path> files = Files.walk(SOURCES)) {
            return files.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
    }
}
