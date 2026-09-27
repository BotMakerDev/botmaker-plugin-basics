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

    /** What an hour, minute or second box takes: a number in range, and nothing else — never a throw. */
    @Test
    void a_typed_field_is_a_number_in_range_or_nothing() {
        assertEquals(7, TimeText.field(" 07 ", 23));
        assertEquals(59, TimeText.field("59", 59));
        assertEquals(null, TimeText.field("60", 59));
        assertEquals(null, TimeText.field("x", 59));
        assertEquals(null, TimeText.field("", 23));
        assertEquals(null, TimeText.field(null, 23));
        assertEquals(null, TimeText.field("-1", 23));
        assertEquals(null, TimeText.field("99999999999", 23));
    }

    @Test
    void an_offset_is_named_from_utc() {
        assertEquals("UTC", TimeText.offset(java.time.ZoneOffset.UTC));
        assertEquals("UTC+02:00", TimeText.offset(java.time.ZoneOffset.ofHours(2)));
        assertEquals("UTC-03:30", TimeText.offset(java.time.ZoneOffset.ofHoursMinutes(-3, -30)));
        assertEquals("07:30 UTC", TimeText.pill(java.time.OffsetTime.of(7, 30, 0, 0, java.time.ZoneOffset.UTC)));
    }

    /** Every offset in use, west to east, UTC among them, whole hours and the half and quarter ones. */
    @Test
    void the_offsets_run_west_to_east() {
        var offsets = TimeText.offsets();
        assertEquals(java.time.ZoneOffset.ofHours(-12), offsets.getFirst());
        assertEquals(java.time.ZoneOffset.ofHours(14), offsets.getLast());
        assertEquals(true, offsets.contains(java.time.ZoneOffset.UTC));
        assertEquals(true, offsets.contains(java.time.ZoneOffset.ofHoursMinutes(5, 30)));
        assertEquals(true, offsets.contains(java.time.ZoneOffset.ofHoursMinutes(5, 45)));
        for (int i = 1; i < offsets.size(); i++) {
            assertEquals(true, offsets.get(i - 1).getTotalSeconds() < offsets.get(i).getTotalSeconds());
        }
    }
}
