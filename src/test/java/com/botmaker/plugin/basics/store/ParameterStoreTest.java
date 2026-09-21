package com.botmaker.plugin.basics.store;

import com.botmaker.plugin.api.parameters.ParameterEdit;
import com.botmaker.plugin.api.parameters.ParameterRow;
import com.botmaker.plugin.api.value.Range;
import com.botmaker.plugin.api.value.ValueCatalog;
import com.botmaker.plugin.api.value.ValueForm;
import com.botmaker.plugin.api.value.ValueType;
import com.botmaker.plugin.api.value.Visibility;
import com.botmaker.plugin.basics.values.BasicsValueTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The mechanism a plugin declares parameters through: the rows it serves, and where a changed value goes.
 *
 * <p>It was {@code SdkParametersTest} in the SDK until 2026-09-10, over {@code activities.json} and the
 * SDK's own records. What it asserts is unchanged, because what moved is the owner rather than the rules:
 * that a row's components survive the crossing, that a group this store does not own answers nothing rather
 * than everything, that an edit reaches the file rather than only the screen, and that the coercion the
 * editor owes — canonicalise, clamp, prune — runs on the way in.
 */
class ParameterStoreTest {

    /** The group the tests own — blank, the same id {@code ParameterGroup.DEFAULT_ID} carries. */
    private static final String OURS = "";

    private static final String PLUGIN = "com.example.tests";

    private static final ValueCatalog CATALOG = BasicsValueTypes.CATALOG;

    private static final ValueType TEXT = CATALOG.text();
    private static final ValueType WHOLE = CATALOG.type("WHOLE_NUMBER");

    private static ParameterStore over(Path resourcesDir) {
        return store(resourcesDir, OURS);
    }

    private static ParameterStore store(Path resourcesDir, String groupId) {
        return new ParameterStore(PluginData.of(resourcesDir, PLUGIN), groupId, CATALOG);
    }

    /** The file every one of these tests writes into, for the cases that read it as text. */
    private static Path file(Path resourcesDir) {
        return PluginData.of(resourcesDir, PLUGIN).file(PluginData.PARAMETERS);
    }

    /** {@code retries}: a bounded whole number with every optional component set to something. */
    private static ParameterRow retries(Path dir) {
        ParameterStore ours = over(dir);
        ours.declare("retries", ValueForm.of(WHOLE));
        ours.setBounds("retries", new Range("1", "5"));
        ours.setCategory("retries", "Timing");
        ours.setVisibility("retries", Visibility.EDITOR_ONLY);
        ours.setDescription("retries", "How many tries");
        return ours.apply(ParameterEdit.of(OURS, "retries", "2")).orElseThrow();
    }

    @Test
    void everyComponentOfAStoredRowSurvivesTheFile(@TempDir Path dir) {
        retries(dir);

        ParameterRow row = over(dir).rows(OURS).getFirst();

        assertEquals("retries", row.name());
        assertEquals("WHOLE_NUMBER", row.form().leaf().id());
        assertEquals("2", row.value());
        assertEquals("How many tries", row.description());
        assertEquals("Timing", row.category());
        assertEquals(Visibility.EDITOR_ONLY, row.visibility());
        assertEquals(new Range("1", "5"), row.bounds());
    }

    @Test
    void aGroupThisStoreDoesNotOwnAnswersNothing(@TempDir Path dir) {
        retries(dir);

        assertEquals(List.of(), over(dir).rows("discord"));
        assertEquals(1, over(dir).rows(OURS).size());
    }

    /** A row filed under another group is in the same file and is still not this store's. */
    @Test
    void anotherGroupsRowInTheSameFileIsNotOurs(@TempDir Path dir) {
        retries(dir);
        store(dir, "discord").declare("channel", ValueForm.of(TEXT));

        List<ParameterRow> ours = over(dir).rows(OURS);

        assertEquals(1, ours.size());
        assertEquals("retries", ours.getFirst().name());
        assertEquals(List.of("channel"), store(dir, "discord").rows("discord").stream()
                .map(ParameterRow::name).toList());
    }

    /** And no verb here may touch it — the rule the withdrawn sectioned file had between plugins. */
    @Test
    void anotherGroupsRowIsUntouchableFromHere(@TempDir Path dir) {
        store(dir, "discord").declare("channel", ValueForm.of(TEXT));

        assertTrue(over(dir).rename("channel", "room").isEmpty());
        assertFalse(over(dir).remove("channel"));
        assertEquals("channel", store(dir, "discord").rows("discord").getFirst().name());
    }

    /**
     * A project with no file yet is a project with no parameters — the state a freshly created one is in,
     * and not a reason to refuse to draw the window.
     */
    @Test
    void aProjectWithNoFileHasNoRows(@TempDir Path dir) {
        assertEquals(List.of(), over(dir).rows(OURS));
        assertEquals(Optional.empty(), over(dir).apply(ParameterEdit.of(OURS, "retries", "3")));
        assertFalse(Files.exists(file(dir)), "reading must not create anything");
    }

    @Test
    void anEditReachesTheFileAndComesBackAsTheStoredRow(@TempDir Path dir) throws IOException {
        retries(dir);

        Optional<ParameterRow> stored = over(dir).apply(ParameterEdit.of(OURS, "retries", "4"));

        assertEquals("4", stored.orElseThrow().value());
        assertTrue(Files.readString(file(dir)).contains("\"4\""));
        // Read back through the surface, not only through the file: the window asks again after an edit.
        assertEquals("4", over(dir).rows(OURS).getFirst().value());
    }

    /** Everything the edit did not name is still there — an edit is not a rewrite of the row. */
    @Test
    void anEditChangesNothingButTheValue(@TempDir Path dir) {
        retries(dir);

        ParameterRow stored = over(dir).apply(ParameterEdit.of(OURS, "retries", "4")).orElseThrow();

        assertEquals("How many tries", stored.description());
        assertEquals("Timing", stored.category());
        assertEquals(Visibility.EDITOR_ONLY, stored.visibility());
        assertEquals(new Range("1", "5"), stored.bounds());
    }

    /**
     * Empty is <i>not mine</i>, and the file is left alone — a host reads it as "leave the screen alone",
     * so an edit that matched nothing must not be answerable with a row and must not have written.
     */
    @Test
    void anEditNamingNothingWeHoldIsDeclined(@TempDir Path dir) throws IOException {
        retries(dir);
        String before = Files.readString(file(dir));

        assertEquals(Optional.empty(), over(dir).apply(ParameterEdit.of(OURS, "nobody", "4")));
        assertEquals(Optional.empty(), over(dir).apply(ParameterEdit.of("discord", "retries", "4")));
        assertEquals(before, Files.readString(file(dir)));
    }

    /**
     * A list-shaped row crosses as the one initialiser that builds it, and an edit replaces the whole list.
     *
     * <p>The file underneath still holds one stored entry per item — that is what the coercion rules are
     * written over — so this is also the assertion that the two spellings agree at the boundary.
     */
    @Test
    void aListShapedRowCrossesAsOneInitializer(@TempDir Path dir) throws IOException {
        over(dir).declare("hotkeys", ValueForm.listOf(ValueForm.of(TEXT)));
        over(dir).apply(new ParameterEdit(OURS, "hotkeys", "java.util.List.of(\"F1\", \"F2\")"));

        assertEquals("java.util.List.of(\"F1\", \"F2\")", over(dir).rows(OURS).getFirst().value());
        assertTrue(Files.readString(file(dir)).contains("\"F1\""));

        ParameterRow stored = over(dir)
                .apply(new ParameterEdit(OURS, "hotkeys", "java.util.List.of(\"F1\", \"F2\", \"F3\")"))
                .orElseThrow();

        assertEquals("java.util.List.of(\"F1\", \"F2\", \"F3\")", stored.value());
        assertEquals("java.util.List<String>", stored.form().sourceName());
    }

    // ---- the declaration verbs, and the coercion that comes with being the editor ------------------------

    @Test
    void aDeclaredParameterIsSeededWithItsTypesDefault(@TempDir Path dir) {
        ParameterRow fresh = over(dir).declare("retries", ValueForm.of(WHOLE)).orElseThrow();

        assertEquals("0", fresh.value(), "a seeded value has to be one the bot can compile");
        assertEquals("retries", over(dir).rows(OURS).getFirst().name());
        // A list has no items until the user adds one; seeding one would put a blank row in every new list.
        assertEquals("java.util.List.of()",
                over(dir).declare("keys", ValueForm.listOf(ValueForm.of(TEXT))).orElseThrow().value());
    }

    /** Declaring is the one verb that has to work on a project whose file does not exist yet. */
    @Test
    void declaringCreatesTheFileOnDemand(@TempDir Path dir) {
        assertTrue(over(dir).declare("retries", ValueForm.of(WHOLE)).isPresent());

        assertTrue(Files.isRegularFile(file(dir)), "the folders were not created on the way");
        assertEquals(1, over(dir).rows(OURS).size());
    }

    @Test
    void aNameMustBeAJavaIdentifierAndFreeInThisGroup(@TempDir Path dir) {
        retries(dir);
        ParameterStore ours = over(dir);

        assertTrue(ours.declare("retries", ValueForm.of(WHOLE)).isEmpty(), "already taken here");
        assertTrue(ours.declare("2fast", ValueForm.of(TEXT)).isEmpty(), "not an identifier");
        assertTrue(ours.declare("has space", ValueForm.of(TEXT)).isEmpty());
        assertTrue(ours.declare(" ", ValueForm.of(TEXT)).isEmpty());
        assertEquals(1, ours.rows(OURS).size(), "a refusal writes nothing");
    }

    /** One name may be taken once per group, which is what lets two windows both offer a {@code timeout}. */
    @Test
    void oneNameIsFreeAgainInAnotherGroup(@TempDir Path dir) {
        over(dir).declare("timeout", ValueForm.of(WHOLE));

        assertTrue(store(dir, "discord").declare("timeout", ValueForm.of(WHOLE)).isPresent());
        assertEquals(1, over(dir).rows(OURS).size());
        assertEquals(1, store(dir, "discord").rows("discord").size());
    }

    @Test
    void removingTakesTheRowOutOfTheFile(@TempDir Path dir) {
        retries(dir);

        assertTrue(over(dir).remove("retries"));
        assertEquals(List.of(), over(dir).rows(OURS));
        assertFalse(over(dir).remove("retries"), "gone is gone, and saying so is not an error");
    }

    @Test
    void renamingRefusesANameAlreadyTakenHere(@TempDir Path dir) {
        retries(dir);
        over(dir).declare("rest", ValueForm.of(TEXT));

        assertEquals("attempts", over(dir).rename("retries", "attempts").orElseThrow().name());
        assertTrue(over(dir).rename("attempts", "rest").isEmpty());
        assertTrue(over(dir).rename("attempts", "2fast").isEmpty());
        assertEquals("attempts", over(dir).rows(OURS).getFirst().name());
    }

    /** A rename carries the rest of the row across — only the name changes. */
    @Test
    void renamingKeepsEverythingElse(@TempDir Path dir) {
        retries(dir);

        ParameterRow renamed = over(dir).rename("retries", "attempts").orElseThrow();

        assertEquals("How many tries", renamed.description());
        assertEquals(new Range("1", "5"), renamed.bounds());
        assertEquals("2", renamed.value());
    }

    /**
     * Retyping resets the value and drops the bounds — a date is not a number, and pretending otherwise
     * stores something the editor would have to explain away on the next open.
     */
    @Test
    void retypingResetsTheValueAndDropsTheBounds(@TempDir Path dir) {
        retries(dir);

        ParameterRow retyped = over(dir).retype("retries", ValueForm.of(TEXT)).orElseThrow();

        assertEquals(TEXT.id(), retyped.form().leaf().id());
        assertEquals("\"\"", retyped.value(), "the empty text, written as the Java that produces it");
        assertTrue(retyped.bounds().isEmpty());
    }

    /** Options survive a change of container over one leaf type, and never a change of leaf. */
    @Test
    void declaredOptionsSurviveAContainerChangeAndNotATypeChange(@TempDir Path dir) {
        over(dir).declare("mode", ValueForm.of(TEXT));
        over(dir).setOptions("mode", List.of("fast", "safe"));

        ParameterRow many = over(dir)
                .retype("mode", ValueForm.listOf(ValueForm.of(TEXT))).orElseThrow();
        assertEquals(List.of("fast", "safe"), many.options());

        ParameterRow asNumber = over(dir).retype("mode", ValueForm.of(WHOLE)).orElseThrow();
        assertEquals(List.of(), asNumber.options(), "they are not values of the new type");
    }

    /** An option the author has just deleted must stop being a stored value. */
    @Test
    void replacingTheOptionsPrunesTheValue(@TempDir Path dir) {
        over(dir).declare("mode", ValueForm.of(TEXT));
        over(dir).setOptions("mode", List.of("fast", "safe"));
        over(dir).apply(ParameterEdit.of(OURS, "mode", "\"safe\""));

        ParameterRow pruned = over(dir).setOptions("mode", List.of("fast", "careful")).orElseThrow();

        assertEquals(List.of("fast", "careful"), pruned.options());
        assertEquals("\"fast\"", pruned.value(), "the value it held is no longer on offer");
    }

    /** A range is advice and a clamp, never a validation that can fail. */
    @Test
    void declaringARangePullsTheValueIntoIt(@TempDir Path dir) {
        over(dir).declare("retries", ValueForm.of(WHOLE));
        over(dir).apply(ParameterEdit.of(OURS, "retries", "900"));

        assertEquals("5", over(dir).setBounds("retries", new Range("1", "5")).orElseThrow().value());
        assertEquals("1", over(dir).apply(ParameterEdit.of(OURS, "retries", "-4")).orElseThrow()
                .value(), "an edit is clamped by the same rules");
    }

    /** The editor's rules run on the way in, so a value is canonicalised rather than stored as typed. */
    @Test
    void aValueIsCanonicalisedByItsOwnType(@TempDir Path dir) {
        over(dir).declare("retries", ValueForm.of(WHOLE));

        assertEquals("7", over(dir).apply(ParameterEdit.of(OURS, "retries", " 7 ")).orElseThrow()
                .value());
    }

    @Test
    void theSmallDeclarationsAreStoredAsGiven(@TempDir Path dir) {
        retries(dir);
        ParameterStore ours = over(dir);

        assertEquals("Vision", ours.setCategory("retries", "Vision").orElseThrow().category());
        assertEquals(Visibility.PUBLIC, ours.setVisibility("retries", Visibility.PUBLIC).orElseThrow()
                .visibility());
        assertEquals("how many", ours.setDescription("retries", "how many").orElseThrow().description());
        assertEquals("how many", ours.rows(OURS).getFirst().displayLabel());
    }

    /** Every verb answers empty for a row this group does not hold, and writes nothing. */
    @Test
    void everyVerbDeclinesARowWeDoNotHold(@TempDir Path dir) throws IOException {
        retries(dir);
        ParameterStore ours = over(dir);
        String before = Files.readString(file(dir));

        assertTrue(ours.rename("nobody", "somebody").isEmpty());
        assertTrue(ours.retype("nobody", ValueForm.of(TEXT)).isEmpty());
        assertTrue(ours.setOptions("nobody", List.of("a")).isEmpty());
        assertTrue(ours.setBounds("nobody", new Range("1", "2")).isEmpty());
        assertTrue(ours.setCategory("nobody", "Timing").isEmpty());
        assertTrue(ours.setVisibility("nobody", Visibility.PUBLIC).isEmpty());
        assertTrue(ours.setDescription("nobody", "x").isEmpty());
        assertEquals(before, Files.readString(file(dir)));
    }

    // Six tests of declared(ParameterDeclaration) stood here until 2026-09-17 — an add, a rename, a retype's
    // reset, an undo putting a type and its value back, a removal, a refusal. They went with the method: the
    // host no longer declares a row here, because a user parameter is a @Param field in the bot's own Java.
    // The verbs those tests reached through are still covered one at a time above.

    /**
     * What a bot reads is the same file, resolved from the id and the name.
     *
     * <p>It read it as <em>untyped text</em> through {@code ProjectValues} until 2026-09-21, which is the
     * half of that class that went with {@code activities.json}. The path is what mattered and the path is
     * unchanged: {@code Settings.forPlugin(id).read(name, …)} resolves exactly this resource.
     */
    @Test
    void aBotResolvesTheSameFileFromTheIdAndTheName(@TempDir Path dir) {
        retries(dir);

        assertTrue(Files.isRegularFile(file(dir)));
        assertEquals("/plugins/com.example/tests/parameters.json",
                PluginData.resource(PLUGIN, PluginData.PARAMETERS));
    }
}
