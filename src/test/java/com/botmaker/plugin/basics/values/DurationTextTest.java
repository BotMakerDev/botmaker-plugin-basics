package com.botmaker.plugin.basics.values;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DurationTextTest {

    @Test
    void spells_the_pill() {
        assertEquals("0s", DurationText.spell(0));
        assertEquals("250ms", DurationText.spell(250));
        assertEquals("1m30s", DurationText.spell(90_000));
        assertEquals("1h5s", DurationText.spell(3_605_000));
        assertEquals("1s500ms", DurationText.spell(1_500));
    }

    @Test
    void says_it_in_words() {
        assertEquals("no wait", DurationText.words(0));
        assertEquals("1 minute 30 seconds", DurationText.words(90_000));
        assertEquals("1 hour 5 seconds", DurationText.words(3_605_000));
        assertEquals("250 milliseconds", DurationText.words(250));
        assertEquals("2 hours 1 minute", DurationText.words(7_260_000));
    }

    @Test
    void offers_twelve_presets_in_order() {
        assertEquals(List.of(100L, 250L, 500L, 1_000L, 2_000L, 5_000L,
                10_000L, 30_000L, 60_000L, 300_000L, 600_000L, 3_600_000L), DurationText.PRESETS);
    }
}
