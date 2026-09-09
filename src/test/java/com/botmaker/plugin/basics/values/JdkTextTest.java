package com.botmaker.plugin.basics.values;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.Color;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The grammar of the nine, now that it lives here rather than in the SDK.
 *
 * <p>The SDK's {@code WireTextTest} still runs the same assertions through {@code WireText}, which delegates
 * — deliberately, because the thing that would actually break is the delegation, and a test that only ran
 * here would pass while a bot read something else.
 */
class JdkTextTest {

    /** Nothing throws and nothing is null: a bot never fails to start because of its own file. */
    @Test
    void everyReaderAnswersItsTypesDefaultRatherThanThrowing() {
        // `text` is absent on purpose: it keeps whatever it is given, so it has no default to assert here.
        for (String nothing : new String[]{null, "", "   ", "not a value at all"}) {
            assertEquals(false, JdkText.flag(nothing));
            assertEquals(0, JdkText.whole(nothing));
            assertEquals(0.0, JdkText.decimal(nothing));
            assertEquals(Duration.ZERO, JdkText.duration(nothing));
            assertEquals(Color.WHITE, JdkText.color(nothing));
            assertEquals(LocalTime.MIDNIGHT, JdkText.time(nothing));
            assertEquals(LocalDate.of(2000, 1, 1), JdkText.date(nothing));
        }
    }

    @Test
    void textIsKeptExactlyAsStoredAndAbsentTextIsEmpty() {
        assertEquals("  spaced  ", JdkText.text("  spaced  "), "a trailing space may be the point");
        assertEquals("", JdkText.text(null));
    }

    @Test
    void theOrdinaryReadings() {
        assertEquals(true, JdkText.flag("true"));
        assertEquals(42, JdkText.whole("42"));
        assertEquals(3, JdkText.whole("3.0"), "a hand-edited decimal still reads as a whole number");
        assertEquals(0.75, JdkText.decimal("0.75"));
        assertEquals('x', JdkText.letter("xyz"), "the first character, not a refusal");
        assertEquals('a', JdkText.letter(""));
        assertEquals(LocalDate.of(2026, 8, 25), JdkText.date("2026-08-25"));
        assertEquals(LocalTime.of(7, 30, 15), JdkText.time("07:30:15"));
        assertEquals(new Color(0x33, 0x66, 0xFF), JdkText.color("#3366FF"));
        assertEquals(new Color(0x1A, 0x2B, 0x3C), JdkText.color("1a2b3c"), "the hash is optional");
    }

    @ParameterizedTest
    @ValueSource(strings = {"90s", "90 s", "1m30s", "30s1m", "90000ms", "90000", "1M30S"})
    void everySpellingOfNinetySecondsReadsTheSame(String text) {
        assertEquals(Duration.ofSeconds(90), JdkText.duration(text));
    }

    @ParameterizedTest
    @ValueSource(strings = {"5w", "s", "m30", "99999999999s"})
    void anythingUnreadableIsZero(String text) {
        assertEquals(Duration.ZERO, JdkText.duration(text));
    }

    @Test
    void spellingIsCanonical() {
        assertEquals("0s", JdkText.spellDuration(0));
        assertEquals("0s", JdkText.spellDuration(-1));
        assertEquals("250ms", JdkText.spellDuration(250));
        assertEquals("1m30s", JdkText.spellDuration(90_000));
        assertEquals("1h30m", JdkText.spellDuration(5_400_000));
        assertEquals("1h1m1s1ms", JdkText.spellDuration(3_661_001));
    }

    @Test
    void aColourIsSpelledBackTheWayItIsRead() {
        assertEquals("#3366FF", JdkText.spellColor(new Color(0x33, 0x66, 0xFF)));
        assertEquals(new Color(0x33, 0x66, 0xFF), JdkText.color(JdkText.spellColor(new Color(0x33, 0x66,
                0xFF))));
    }
}
