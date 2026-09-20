package com.botmaker.plugin.basics.values;

import com.botmaker.plugin.api.value.ValueCatalog;
import com.botmaker.plugin.api.value.ValueForm;
import com.botmaker.plugin.api.value.ValueType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code valueOfLiteral} against the literal it undoes — the property a user parameter's value cell depends
 * on, since a {@code @Param} field's value <em>is</em> its initialiser.
 *
 * <p>The contract's rule is a round trip, so this test is mostly that rule applied to every one of the nine
 * over values chosen to be awkward: the escapes, the boundaries, and the one type whose stored spelling is
 * not its Java spelling.
 */
class LiteralInverseTest {

    private static final ValueCatalog CATALOG = BasicsValueTypes.CATALOG;

    private static ValueForm one(ValueType type) {
        return ValueForm.of(type);
    }

    private static ValueForm list(ValueType type) {
        return ValueForm.listOf(ValueForm.of(type));
    }

    /** Every type, every sample: {@code store(valueOfLiteral(literal(parse(wire))))} == {@code store(parse(wire))}. */
    @Test
    void everyTypeReadsBackWhatItWrote() {
        record Sample(ValueType type, String wire) {}
        List<Sample> samples = List.of(
                new Sample(BasicsValueTypes.TEXT, "hello"),
                new Sample(BasicsValueTypes.TEXT, ""),
                new Sample(BasicsValueTypes.TEXT, "a \"quoted\" \\ back\nslash\ttab"),
                new Sample(BasicsValueTypes.TEXT, "comma, inside"),
                // A control character, which a user pastes without ever seeing it. The writer escapes it as
                // \\u0007 and the reader refused that escape until 2026-09-20 — so exactly the values
                // nobody can see were the ones written and then shown read-only.
                new Sample(BasicsValueTypes.TEXT, "bell\u0007 and del\u007f"),
                new Sample(BasicsValueTypes.YES_NO, "true"),
                new Sample(BasicsValueTypes.YES_NO, "false"),
                new Sample(BasicsValueTypes.WHOLE_NUMBER, "0"),
                new Sample(BasicsValueTypes.WHOLE_NUMBER, "-2147483648"),
                new Sample(BasicsValueTypes.DECIMAL_NUMBER, "1.5"),
                new Sample(BasicsValueTypes.DECIMAL_NUMBER, "-0.25"),
                new Sample(BasicsValueTypes.CHARACTER, "x"),
                new Sample(BasicsValueTypes.CHARACTER, "'"),
                new Sample(BasicsValueTypes.CHARACTER, "\n"),
                new Sample(BasicsValueTypes.COLOR, "#FF0000"),
                new Sample(BasicsValueTypes.COLOR, "#000000"),
                new Sample(BasicsValueTypes.DATE, "2026-09-17"),
                new Sample(BasicsValueTypes.TIME_OF_DAY, "07:30:15"),
                new Sample(BasicsValueTypes.DURATION, "3s"),
                new Sample(BasicsValueTypes.DURATION, "1h30m"),
                new Sample(BasicsValueTypes.DURATION, "0s"));

        for (Sample sample : samples) {
            ValueForm form = one(sample.type());
            String canonical = CATALOG.normalize(sample.type().id(), sample.wire());
            String java = CATALOG.initializerOfWires(form, List.of(sample.wire())).orElseThrow();
            assertEquals(Optional.of(List.of(canonical)), CATALOG.wiresOfInitializer(form, java),
                    sample.type().id() + " wrote " + java + " and could not read it back");
        }
    }

    @Test
    void aListRoundTripsItemByItem() {
        ValueForm list = list(BasicsValueTypes.DURATION);
        String java = CATALOG.initializerOfWires(list, List.of("3s", "1m")).orElseThrow();
        assertEquals(Optional.of(List.of("3s", "1m")), CATALOG.wiresOfInitializer(list, java));

        // An empty list is a value, and the empty *answer* means something else entirely.
        assertEquals(Optional.of(List.of()), CATALOG.wiresOfInitializer(
                list, CATALOG.initializerOfWires(list, List.of()).orElseThrow()));
    }

    @Test
    void aListWhoseItemsContainCommasIsStillSplitCorrectly() {
        ValueForm colors = list(BasicsValueTypes.COLOR);
        // new java.awt.Color(255, 0, 0) has two commas of its own, so a naive split would see six items.
        String java = CATALOG.initializerOfWires(colors, List.of("#FF0000", "#00FF00")).orElseThrow();
        assertEquals(Optional.of(List.of("#FF0000", "#00FF00")), CATALOG.wiresOfInitializer(colors, java));

        ValueForm texts = list(BasicsValueTypes.TEXT);
        assertEquals(Optional.of(List.of("a, b", "c")),
                CATALOG.wiresOfInitializer(texts,
                        CATALOG.initializerOfWires(texts, List.of("a, b", "c")).orElseThrow()));
    }

    /**
     * A spelling this plugin never writes is declined rather than read generously.
     *
     * <p>{@code Duration.ofSeconds(3)} means what the canonical literal means, and recognising it would be
     * recognising somebody else's Java — the author wrote that on purpose, and the window shows it as
     * written instead of quietly rewriting it the moment the file is opened.
     */
    @Test
    void anInitializerThisPluginDoesNotWriteIsDeclined() {
        assertEquals(Optional.empty(), CATALOG.wiresOfInitializer(
                one(BasicsValueTypes.DURATION), "java.time.Duration.ofSeconds(3)"));
        assertEquals(Optional.empty(), CATALOG.wiresOfInitializer(
                one(BasicsValueTypes.DURATION), "REST_BETWEEN"));
        assertEquals(Optional.empty(), CATALOG.wiresOfInitializer(
                one(BasicsValueTypes.COLOR), "java.awt.Color.RED"));
        assertEquals(Optional.empty(), CATALOG.wiresOfInitializer(
                one(BasicsValueTypes.WHOLE_NUMBER), "MAX_ATTEMPTS + 1"));
        assertEquals(Optional.empty(), CATALOG.wiresOfInitializer(
                one(BasicsValueTypes.TEXT), "name()"));
    }

    @Test
    void anImpossibleDateOrTimeIsDeclinedRatherThanThrown() {
        assertEquals(Optional.empty(), CATALOG.wiresOfInitializer(
                one(BasicsValueTypes.DATE), "java.time.LocalDate.of(2026, 13, 40)"));
        assertEquals(Optional.empty(), CATALOG.wiresOfInitializer(
                one(BasicsValueTypes.TIME_OF_DAY), "java.time.LocalTime.of(25, 0, 0)"));
    }

    @Test
    void aListSourceThatIsNotAListCallIsDeclined() {
        ValueForm list = list(BasicsValueTypes.WHOLE_NUMBER);
        assertEquals(Optional.empty(), CATALOG.wiresOfInitializer(list, "42"));
        assertEquals(Optional.empty(), CATALOG.wiresOfInitializer(list, "someHelper()"));
        // Unbalanced: declined rather than split wrongly.
        assertEquals(Optional.empty(), CATALOG.wiresOfInitializer(list, "java.util.List.of(1, 2"));
    }

    @Test
    void oneUnreadableItemMakesTheWholeListUnreadable() {
        // Half a list is not a value: the host shows the source rather than a partial reading of it.
        assertEquals(Optional.empty(), CATALOG.wiresOfInitializer(
                list(BasicsValueTypes.DURATION),
                "java.util.List.of(java.time.Duration.ofMillis(3000L), REST)"));
    }

    @Test
    void whitespaceAsAFormatterWouldLeaveItIsTolerated() {
        assertEquals(Optional.of(List.of("3s")), CATALOG.wiresOfInitializer(
                one(BasicsValueTypes.DURATION), "  java.time.Duration.ofMillis(3000L)  "));
        assertEquals(Optional.of(List.of("#FF0000")), CATALOG.wiresOfInitializer(
                one(BasicsValueTypes.COLOR), "new java.awt.Color( 255 , 0 , 0 )"));
        assertEquals(Optional.of(List.of("3s", "1m")), CATALOG.wiresOfInitializer(
                list(BasicsValueTypes.DURATION),
                "java.util.List.of(\n    java.time.Duration.ofMillis(3000L),\n"
                        + "    java.time.Duration.ofMillis(60000L))"));
    }

    @Test
    void aTypeNobodyRegisteredIsDeclinedRatherThanGuessed() {
        assertTrue(CATALOG.wiresOfInitializer(
                one(ValueType.unknown("CHANNEL")), "\"general\"").isEmpty());
    }
}
