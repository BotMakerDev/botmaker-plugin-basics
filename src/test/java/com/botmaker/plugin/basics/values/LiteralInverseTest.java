package com.botmaker.plugin.basics.values;

import com.botmaker.plugin.api.value.ValueCatalog;
import com.botmaker.plugin.api.value.ValueChoice;
import com.botmaker.plugin.api.value.ValueType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code wireOfLiteral} against the literal it undoes — the property a user parameter's value cell depends
 * on, since a {@code @Param} field's value <em>is</em> its initialiser.
 *
 * <p>The contract's rule is a round trip, so this test is mostly that rule applied to every one of the nine
 * over values chosen to be awkward: the escapes, the boundaries, and the one type whose stored spelling is
 * not its Java spelling.
 */
class LiteralInverseTest {

    private static final ValueCatalog CATALOG = BasicsValueTypes.CATALOG;

    /** Every type, every sample: {@code wireOfLiteral(literal(parse(wire)))} == {@code store(parse(wire))}. */
    @Test
    void everyTypeReadsBackWhatItWrote() {
        record Sample(ValueType type, String wire) {}
        List<Sample> samples = List.of(
                new Sample(BasicsValueTypes.TEXT, "hello"),
                new Sample(BasicsValueTypes.TEXT, ""),
                new Sample(BasicsValueTypes.TEXT, "a \"quoted\" \\ back\nslash\ttab"),
                new Sample(BasicsValueTypes.TEXT, "comma, inside"),
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
            ValueChoice choice = ValueChoice.of(sample.type());
            String canonical = CATALOG.normalize(sample.type().id(), sample.wire());
            String java = CATALOG.initializer(choice, List.of(sample.wire())).orElseThrow();
            assertEquals(Optional.of(List.of(canonical)), CATALOG.valueOfInitializer(choice, java),
                    sample.type().id() + " wrote " + java + " and could not read it back");
        }
    }

    @Test
    void aListRoundTripsItemByItem() {
        ValueChoice list = ValueChoice.listOf(BasicsValueTypes.DURATION);
        String java = CATALOG.initializer(list, List.of("3s", "1m")).orElseThrow();
        assertEquals(Optional.of(List.of("3s", "1m")), CATALOG.valueOfInitializer(list, java));

        // An empty list is a value, and the empty *answer* means something else entirely.
        assertEquals(Optional.of(List.of()),
                CATALOG.valueOfInitializer(list, CATALOG.initializer(list, List.of()).orElseThrow()));
    }

    @Test
    void aListWhoseItemsContainCommasIsStillSplitCorrectly() {
        ValueChoice colors = ValueChoice.listOf(BasicsValueTypes.COLOR);
        // new java.awt.Color(255, 0, 0) has two commas of its own, so a naive split would see six items.
        String java = CATALOG.initializer(colors, List.of("#FF0000", "#00FF00")).orElseThrow();
        assertEquals(Optional.of(List.of("#FF0000", "#00FF00")), CATALOG.valueOfInitializer(colors, java));

        ValueChoice texts = ValueChoice.listOf(BasicsValueTypes.TEXT);
        assertEquals(Optional.of(List.of("a, b", "c")),
                CATALOG.valueOfInitializer(texts,
                        CATALOG.initializer(texts, List.of("a, b", "c")).orElseThrow()));
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
        assertEquals(Optional.empty(), CATALOG.valueOfInitializer(
                ValueChoice.of(BasicsValueTypes.DURATION), "java.time.Duration.ofSeconds(3)"));
        assertEquals(Optional.empty(), CATALOG.valueOfInitializer(
                ValueChoice.of(BasicsValueTypes.DURATION), "REST_BETWEEN"));
        assertEquals(Optional.empty(), CATALOG.valueOfInitializer(
                ValueChoice.of(BasicsValueTypes.COLOR), "java.awt.Color.RED"));
        assertEquals(Optional.empty(), CATALOG.valueOfInitializer(
                ValueChoice.of(BasicsValueTypes.WHOLE_NUMBER), "MAX_ATTEMPTS + 1"));
        assertEquals(Optional.empty(), CATALOG.valueOfInitializer(
                ValueChoice.of(BasicsValueTypes.TEXT), "name()"));
    }

    @Test
    void anImpossibleDateOrTimeIsDeclinedRatherThanThrown() {
        assertEquals(Optional.empty(), CATALOG.valueOfInitializer(
                ValueChoice.of(BasicsValueTypes.DATE), "java.time.LocalDate.of(2026, 13, 40)"));
        assertEquals(Optional.empty(), CATALOG.valueOfInitializer(
                ValueChoice.of(BasicsValueTypes.TIME_OF_DAY), "java.time.LocalTime.of(25, 0, 0)"));
    }

    @Test
    void aListSourceThatIsNotAListCallIsDeclined() {
        ValueChoice list = ValueChoice.listOf(BasicsValueTypes.WHOLE_NUMBER);
        assertEquals(Optional.empty(), CATALOG.valueOfInitializer(list, "42"));
        assertEquals(Optional.empty(), CATALOG.valueOfInitializer(list, "someHelper()"));
        // Unbalanced: declined rather than split wrongly.
        assertEquals(Optional.empty(), CATALOG.valueOfInitializer(list, "java.util.List.of(1, 2"));
    }

    @Test
    void oneUnreadableItemMakesTheWholeListUnreadable() {
        // Half a list is not a value: the host shows the source rather than a partial reading of it.
        assertEquals(Optional.empty(), CATALOG.valueOfInitializer(
                ValueChoice.listOf(BasicsValueTypes.DURATION),
                "java.util.List.of(java.time.Duration.ofMillis(3000L), REST)"));
    }

    @Test
    void whitespaceAsAFormatterWouldLeaveItIsTolerated() {
        assertEquals(Optional.of(List.of("3s")), CATALOG.valueOfInitializer(
                ValueChoice.of(BasicsValueTypes.DURATION), "  java.time.Duration.ofMillis(3000L)  "));
        assertEquals(Optional.of(List.of("#FF0000")), CATALOG.valueOfInitializer(
                ValueChoice.of(BasicsValueTypes.COLOR), "new java.awt.Color( 255 , 0 , 0 )"));
        assertEquals(Optional.of(List.of("3s", "1m")), CATALOG.valueOfInitializer(
                ValueChoice.listOf(BasicsValueTypes.DURATION),
                "java.util.List.of(\n    java.time.Duration.ofMillis(3000L),\n"
                        + "    java.time.Duration.ofMillis(60000L))"));
    }

    @Test
    void aTypeNobodyRegisteredIsDeclinedRatherThanGuessed() {
        assertTrue(CATALOG.valueOfInitializer(
                ValueChoice.of(ValueType.unknown("CHANNEL")), "\"general\"").isEmpty());
    }
}
