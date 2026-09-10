package com.botmaker.plugin.basics.store;

import com.botmaker.plugin.api.ParameterDeclaration;
import com.botmaker.plugin.api.ParameterEdit;
import com.botmaker.plugin.api.ParameterRow;
import com.botmaker.plugin.api.value.Range;
import com.botmaker.plugin.api.value.ValueCatalog;
import com.botmaker.plugin.api.value.ValueChoice;
import com.botmaker.plugin.api.value.ValueShape;
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
        ours.declare("retries", ValueChoice.of(WHOLE));
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
        assertEquals("WHOLE_NUMBER", row.type().type().id());
        assertEquals(List.of("2"), row.value());
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
        store(dir, "discord").declare("channel", ValueChoice.of(TEXT));

        List<ParameterRow> ours = over(dir).rows(OURS);

        assertEquals(1, ours.size());
        assertEquals("retries", ours.getFirst().name());
        assertEquals(List.of("channel"), store(dir, "discord").rows("discord").stream()
                .map(ParameterRow::name).toList());
    }

    /** And no verb here may touch it — the rule the withdrawn sectioned file had between plugins. */
    @Test
    void anotherGroupsRowIsUntouchableFromHere(@TempDir Path dir) {
        store(dir, "discord").declare("channel", ValueChoice.of(TEXT));

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

        assertEquals("4", stored.orElseThrow().singleValue());
        assertTrue(Files.readString(file(dir)).contains("\"4\""));
        // Read back through the surface, not only through the file: the window asks again after an edit.
        assertEquals("4", over(dir).rows(OURS).getFirst().singleValue());
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

    /** A list-shaped row crosses one entry per item, and an edit to it replaces the whole list. */
    @Test
    void aListShapedRowCrossesAndIsEditedItemByItem(@TempDir Path dir) {
        over(dir).declare("hotkeys", ValueChoice.listOf(TEXT));
        over(dir).apply(new ParameterEdit(OURS, "hotkeys", List.of("F1", "F2")));

        assertEquals(List.of("F1", "F2"), over(dir).rows(OURS).getFirst().value());

        ParameterRow stored = over(dir)
                .apply(new ParameterEdit(OURS, "hotkeys", List.of("F1", "F2", "F3"))).orElseThrow();

        assertEquals(List.of("F1", "F2", "F3"), stored.value());
        assertTrue(stored.type().isList());
    }

    // ---- the declaration verbs, and the coercion that comes with being the editor ------------------------

    @Test
    void aDeclaredParameterIsSeededWithItsTypesDefault(@TempDir Path dir) {
        ParameterRow fresh = over(dir).declare("retries", ValueChoice.of(WHOLE)).orElseThrow();

        assertEquals("0", fresh.singleValue(), "a seeded value has to be one the bot can compile");
        assertEquals("retries", over(dir).rows(OURS).getFirst().name());
        // A list has no items until the user adds one; seeding one would put a blank row in every new list.
        assertEquals(List.of(), over(dir).declare("keys", ValueChoice.listOf(TEXT)).orElseThrow().value());
    }

    /** Declaring is the one verb that has to work on a project whose file does not exist yet. */
    @Test
    void declaringCreatesTheFileOnDemand(@TempDir Path dir) {
        assertTrue(over(dir).declare("retries", ValueChoice.of(WHOLE)).isPresent());

        assertTrue(Files.isRegularFile(file(dir)), "the folders were not created on the way");
        assertEquals(1, over(dir).rows(OURS).size());
    }

    @Test
    void aNameMustBeAJavaIdentifierAndFreeInThisGroup(@TempDir Path dir) {
        retries(dir);
        ParameterStore ours = over(dir);

        assertTrue(ours.declare("retries", ValueChoice.of(WHOLE)).isEmpty(), "already taken here");
        assertTrue(ours.declare("2fast", ValueChoice.of(TEXT)).isEmpty(), "not an identifier");
        assertTrue(ours.declare("has space", ValueChoice.of(TEXT)).isEmpty());
        assertTrue(ours.declare(" ", ValueChoice.of(TEXT)).isEmpty());
        assertEquals(1, ours.rows(OURS).size(), "a refusal writes nothing");
    }

    /** One name may be taken once per group, which is what lets two windows both offer a {@code timeout}. */
    @Test
    void oneNameIsFreeAgainInAnotherGroup(@TempDir Path dir) {
        over(dir).declare("timeout", ValueChoice.of(WHOLE));

        assertTrue(store(dir, "discord").declare("timeout", ValueChoice.of(WHOLE)).isPresent());
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
        over(dir).declare("rest", ValueChoice.of(TEXT));

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
        assertEquals(List.of("2"), renamed.value());
    }

    /**
     * Retyping resets the value and drops the bounds — a date is not a number, and pretending otherwise
     * stores something the editor would have to explain away on the next open.
     */
    @Test
    void retypingResetsTheValueAndDropsTheBounds(@TempDir Path dir) {
        retries(dir);

        ParameterRow retyped = over(dir).retype("retries", ValueChoice.of(TEXT)).orElseThrow();

        assertEquals(TEXT.id(), retyped.type().type().id());
        assertEquals("", retyped.singleValue());
        assertTrue(retyped.bounds().isEmpty());
    }

    /** Options survive a change of shape over one base type, and never a change of base type. */
    @Test
    void declaredOptionsSurviveAShapeChangeAndNotATypeChange(@TempDir Path dir) {
        over(dir).declare("mode", new ValueChoice(TEXT, ValueShape.ONE_OF));
        over(dir).setOptions("mode", List.of("fast", "safe"));

        ParameterRow many = over(dir).retype("mode", new ValueChoice(TEXT, ValueShape.ANY_OF)).orElseThrow();
        assertEquals(List.of("fast", "safe"), many.options());

        ParameterRow asNumber = over(dir).retype("mode", ValueChoice.of(WHOLE)).orElseThrow();
        assertEquals(List.of(), asNumber.options(), "they are not values of the new type");
    }

    /** An option the author has just deleted must stop being a stored value. */
    @Test
    void replacingTheOptionsPrunesTheValue(@TempDir Path dir) {
        over(dir).declare("mode", new ValueChoice(TEXT, ValueShape.ONE_OF));
        over(dir).setOptions("mode", List.of("fast", "safe"));
        over(dir).apply(ParameterEdit.of(OURS, "mode", "safe"));

        ParameterRow pruned = over(dir).setOptions("mode", List.of("fast", "careful")).orElseThrow();

        assertEquals(List.of("fast", "careful"), pruned.options());
        assertEquals("fast", pruned.singleValue(), "the value it held is no longer on offer");
    }

    /** A range is advice and a clamp, never a validation that can fail. */
    @Test
    void declaringARangePullsTheValueIntoIt(@TempDir Path dir) {
        over(dir).declare("retries", ValueChoice.of(WHOLE));
        over(dir).apply(ParameterEdit.of(OURS, "retries", "900"));

        assertEquals("5", over(dir).setBounds("retries", new Range("1", "5")).orElseThrow().singleValue());
        assertEquals("1", over(dir).apply(ParameterEdit.of(OURS, "retries", "-4")).orElseThrow()
                .singleValue(), "an edit is clamped by the same rules");
    }

    /** The editor's rules run on the way in, so a value is canonicalised rather than stored as typed. */
    @Test
    void aValueIsCanonicalisedByItsOwnType(@TempDir Path dir) {
        over(dir).declare("retries", ValueChoice.of(WHOLE));

        assertEquals("7", over(dir).apply(ParameterEdit.of(OURS, "retries", " 7 ")).orElseThrow()
                .singleValue());
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
        assertTrue(ours.retype("nobody", ValueChoice.of(TEXT)).isEmpty());
        assertTrue(ours.setOptions("nobody", List.of("a")).isEmpty());
        assertTrue(ours.setBounds("nobody", new Range("1", "2")).isEmpty());
        assertTrue(ours.setCategory("nobody", "Timing").isEmpty());
        assertTrue(ours.setVisibility("nobody", Visibility.PUBLIC).isEmpty());
        assertTrue(ours.setDescription("nobody", "x").isEmpty());
        assertEquals(before, Files.readString(file(dir)));
    }

    // ---- the one contract call the verbs above are the implementation of --------------------------------

    /** A declaration against a row nothing holds is an add, whatever the host believed. */
    @Test
    void declaringARowThatIsNotHereAddsIt(@TempDir Path dir) {
        ParameterRow wanted = ParameterRow.named("retries", ValueChoice.of(WHOLE))
                .value("3").description("How many tries").category("Timing")
                .visibility(Visibility.EDITOR_ONLY).bounds(new Range("1", "5")).build();

        ParameterRow stored = over(dir).declared(ParameterDeclaration.added(OURS, wanted)).orElseThrow();

        assertEquals("retries", stored.name());
        assertEquals("3", stored.singleValue());
        assertEquals("Timing", stored.category());
        assertEquals(Visibility.EDITOR_ONLY, stored.visibility());
        assertEquals(new Range("1", "5"), stored.bounds());
    }

    /** Two names in one declaration is a rename, and everything else in the row lands with it. */
    @Test
    void aDeclarationWhoseTwoNamesDifferRenames(@TempDir Path dir) {
        retries(dir);
        ParameterRow wanted = over(dir).rows(OURS).getFirst().toBuilder().build();

        ParameterRow stored = over(dir).declared(new ParameterDeclaration(OURS, "retries",
                copyNamed(wanted, "attempts"))).orElseThrow();

        assertEquals("attempts", stored.name());
        assertEquals(List.of("attempts"), over(dir).rows(OURS).stream().map(ParameterRow::name).toList());
    }

    /**
     * A window retyping a row hands back the row it is showing, whose value is still the old type's text.
     * The reset is this store's rule and wins over that.
     */
    @Test
    void aDeclarationThatOnlyRetypesStillResetsTheValue(@TempDir Path dir) {
        retries(dir);
        ParameterRow showing = over(dir).rows(OURS).getFirst();

        ParameterRow stored = over(dir).declared(ParameterDeclaration.of(OURS,
                retyped(showing, ValueChoice.of(TEXT)))).orElseThrow();

        assertEquals(TEXT.id(), stored.type().type().id());
        assertEquals("", stored.singleValue(), "the old type's text is not carried across");
        assertTrue(stored.bounds().isEmpty());
    }

    /**
     * An undo is a declaration too, and it puts the old type <em>and</em> its old value back — which is why
     * a value the host actually changed is applied even across a retype.
     */
    @Test
    void aDeclarationCarryingBothAnOldTypeAndItsOldValuePutsBothBack(@TempDir Path dir) {
        retries(dir);
        ParameterRow before = over(dir).rows(OURS).getFirst();
        over(dir).declared(ParameterDeclaration.of(OURS, retyped(before, ValueChoice.of(TEXT))));

        ParameterRow back = over(dir).declared(ParameterDeclaration.of(OURS, before)).orElseThrow();

        assertEquals("WHOLE_NUMBER", back.type().type().id());
        assertEquals("2", back.singleValue());
        assertEquals(new Range("1", "5"), back.bounds());
    }

    @Test
    void aDeclarationWithNoRowRemoves(@TempDir Path dir) {
        retries(dir);

        assertEquals(Optional.empty(), over(dir).declared(ParameterDeclaration.removed(OURS, "retries")));
        assertEquals(List.of(), over(dir).rows(OURS));
    }

    /** Empty is also how a refusal and a group this store does not own read — the host redraws either way. */
    @Test
    void aDeclarationForAnotherGroupOrARefusedNameIsEmpty(@TempDir Path dir) {
        retries(dir);
        ParameterRow taken = ParameterRow.named("retries", ValueChoice.of(TEXT)).build();

        assertEquals(Optional.empty(), over(dir).declared(ParameterDeclaration.added("discord", taken)));
        assertEquals(Optional.empty(), over(dir).declared(ParameterDeclaration.added(OURS, taken)));
        assertEquals(1, over(dir).rows(OURS).size());
        assertEquals("WHOLE_NUMBER", over(dir).rows(OURS).getFirst().type().type().id());
    }

    private static ParameterRow copyNamed(ParameterRow row, String name) {
        return ParameterRow.named(name, row.type()).value(row.value()).description(row.description())
                .category(row.category()).visibility(row.visibility()).options(row.options())
                .bounds(row.bounds()).build();
    }

    private static ParameterRow retyped(ParameterRow row, ValueChoice type) {
        return ParameterRow.named(row.name(), type).value(row.value()).description(row.description())
                .category(row.category()).visibility(row.visibility()).options(row.options())
                .bounds(row.bounds()).build();
    }

    /** What a bot reads is the same file, resolved from the id and the name, and answered as text. */
    @Test
    void aBotReadsTheSameFileAsUntypedText(@TempDir Path dir) {
        retries(dir);

        ProjectValues values = ProjectValues.in(ProjectStore.read(file(dir)));

        assertTrue(values.declares("retries"));
        assertEquals("2", values.one("retries"));
        assertEquals("WHOLE_NUMBER", values.typeId("retries"));
        assertEquals("/plugins/com.example/tests/parameters.json",
                PluginData.resource(PLUGIN, PluginData.PARAMETERS));
    }
}
