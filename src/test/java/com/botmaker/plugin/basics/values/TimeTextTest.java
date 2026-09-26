package com.botmaker.plugin.basics.values;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeTextTest {

    @Test
    void the_pill_hides_zero_seconds() {
        assertEquals("07:30", TimeText.pill(LocalTime.of(7, 30)));
        assertEquals("07:30:15", TimeText.pill(LocalTime.of(7, 30, 15)));
        assertEquals("00:00", TimeText.pill(LocalTime.MIDNIGHT));
    }

    @Test
    void words_name_the_part_of_the_day() {
        assertEquals("midnight", TimeText.words(LocalTime.MIDNIGHT));
        assertEquals("noon", TimeText.words(LocalTime.NOON));
        assertEquals("3:10 at night", TimeText.words(LocalTime.of(3, 10)));
        assertEquals("7:30 in the morning", TimeText.words(LocalTime.of(7, 30)));
        assertEquals("13:05 in the afternoon", TimeText.words(LocalTime.of(13, 5)));
        assertEquals("18:05:30 in the evening", TimeText.words(LocalTime.of(18, 5, 30)));
        assertEquals("23:40 at night", TimeText.words(LocalTime.of(23, 40)));
        assertEquals("5:00 in the morning", TimeText.words(LocalTime.of(5, 0)));
        assertEquals("21:00 at night", TimeText.words(LocalTime.of(21, 0)));
    }
}
