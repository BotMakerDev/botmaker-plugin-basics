package com.botmaker.plugin.basics.values;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Month;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The rules behind basics' reviewed pickers (picker 6e3), apart from their widgets. */
class PickRulesTest {

    @Test
    void daysAndMonthsAreNamedShortAndLong() {
        assertEquals("Mon", PickRules.shortName(DayOfWeek.MONDAY));
        assertEquals("Sun", PickRules.shortName(DayOfWeek.SUNDAY));
        assertEquals("Sep", PickRules.shortName(Month.SEPTEMBER));
        assertEquals("September", PickRules.longName(Month.SEPTEMBER));
    }

    @Test
    void aWholeNumberStepsByOneOrTenWithShift() {
        assertEquals(4, PickRules.step(3, true, "3", 1, false));
        assertEquals(-7, PickRules.step(3, true, "3", -1, true));
    }

    @Test
    void aDecimalStepsByItsLastWrittenPlace() {
        assertEquals(1.26, PickRules.step(1.25, false, "1.25", 1, false), 1e-9);
        assertEquals(1.35, PickRules.step(1.25, false, "1.25", 1, true), 1e-9);
        // One decimal place at least: "2" typed into a decimal field steps by a tenth.
        assertEquals(1.9, PickRules.step(2.0, false, "2", -1, false), 1e-9);
    }

    @Test
    void aStepLandsOnTheGridWithoutFloatingPointNoise() {
        assertEquals(0.3, PickRules.step(0.2, false, "0.2", 1, false));
        assertEquals(0.30000000000000004, 0.2 + 0.1, "the noise the rounding exists to remove");
    }

    @Test
    void anInvisibleCharacterIsShownByName() {
        assertEquals("space", PickRules.charLabel(' '));
        assertEquals("tab", PickRules.charLabel('\t'));
        assertEquals("newline", PickRules.charLabel('\n'));
        assertEquals("a", PickRules.charLabel('a'));
    }

    @Test
    void aTypedNameOrTheFirstCharacterIsTheCharacter() {
        assertEquals(Optional.of(' '), PickRules.charFrom("space"));
        assertEquals(Optional.of('\t'), PickRules.charFrom("TAB"));
        assertEquals(Optional.of('w'), PickRules.charFrom("word"));
        assertEquals(Optional.empty(), PickRules.charFrom(""));
    }

    @Test
    void aSwitchSaysItsStateInWords() {
        assertEquals("On", PickRules.flagLabel(true));
        assertEquals("Off", PickRules.flagLabel(false));
    }
}
