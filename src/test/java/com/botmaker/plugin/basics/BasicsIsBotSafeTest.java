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
     * <p>{@code BasicsPlugin} <em>is</em> the contract implementation; {@code BasicsTypes} declares the nine
     * and {@code BasicsEditors} draws them, which is everything only a host asks for. All three are in the
     * same jar as the bot-safe half, exactly as {@code SdkPlugin} sits in the SDK's jar: what matters is
     * that nothing a bot links reaches them.
     *
     * <p>{@code ParameterStore} and {@code StoredForms} were exempted here too, from 2026-09-10 and
     * 2026-09-20; {@code JdkText} and {@code BasicsValueTypes} were the bot-safe and editor-side halves of
     * the stored-text reader. All four are deleted: a parameter is a {@code @Param} field in the bot's own
     * Java and a value is the Java that writes it, so nothing stores text and nothing reads any back.
     */
    private static final Set<String> EDITOR_ONLY =
            Set.of("BasicsPlugin.java", "BasicsTypes.java", "BasicsEditors.java");

    /** What a bot's classpath does not have. Javadoc mentions are fine; a source reference is not. */
    private static final List<String> BANNED = List.of("com.botmaker.plugin.api", "javafx.");

    /**
     * The two contract packages that <b>are</b> on a bot's classpath, deliberately, since 2026-09-22.
     *
     * <p>{@code @Param} sits on a bot's own fields and {@code @Managed} on a bot's own methods, so they
     * were held in this module precisely because the rest of the contract is {@code provided} and absent
     * from a bot. They are the contract's now and the SDK brings them at {@code compile}, which is what
     * {@code ManagedValues} — bot-side, and the thing that reads {@code @Managed} at run time — reflects
     * against.
     *
     * <p><b>The exemption is by package and stays that way.</b> Naming the two that travel is the same
     * shape as {@code EDITOR_ONLY} above and errs in the same direction: a third contract package reaching
     * a bot is a red test rather than a {@code NoClassDefFoundError} in a stranger's bot.
     */
    private static final List<String> ON_A_BOT = List.of(
            "com.botmaker.plugin.api.params", "com.botmaker.plugin.api.managed");

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
                    if (line.contains(banned) && !travelsToABot(line)) {
                        offences.add(file.getFileName() + ":" + (i + 1) + " names " + banned);
                    }
                }
            }
        }
        assertEquals(List.of(), offences,
                "this jar is on every bot's classpath, and a bot has neither the contract nor JavaFX");
    }

    /** Whether the line names one of the two contract packages a bot genuinely has. */
    private static boolean travelsToABot(String line) {
        return ON_A_BOT.stream().anyMatch(line::contains);
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
