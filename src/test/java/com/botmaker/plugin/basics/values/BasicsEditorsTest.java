package com.botmaker.plugin.basics.values;

import com.botmaker.plugin.toolkit.testing.TestContexts;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The editors' pills and their one write rule. No JavaFX needed. */
class BasicsEditorsTest {

    @Test
    void a_duration_pill_says_the_length_the_source_or_an_invitation() {
        var readable = TestContexts.typedSlot(Duration.class, "Duration.ofMillis(90000L)")
                .withValue(Duration.ofMillis(90_000));
        assertEquals("1m30s", BasicsEditors.durationLabel(readable, Duration.ofMillis(90_000)));
        var unreadable = TestContexts.typedSlot(Duration.class, "Duration.ofSeconds(3)");
        assertEquals("Duration.ofSeconds(3)", BasicsEditors.durationLabel(unreadable, null));
        assertEquals("Duration…", BasicsEditors.durationLabel(TestContexts.typedSlot(Duration.class, ""), null));
    }

    @Test
    void ok_with_nothing_picked_writes_nothing() {
        var slot = TestContexts.typedSlot(Duration.class, "Duration.ofMillis(1500L)").withValue(Duration.ofMillis(1500));
        assertFalse(BasicsEditors.commit(slot, Duration.ofMillis(1500), Duration.ofMillis(1500), false));
        assertFalse(BasicsEditors.commit(slot, Duration.ofMillis(1500), Duration.ofMillis(1500), true));
        assertEquals(0, slot.writes());
    }

    @Test
    void an_unreadable_source_is_written_only_once_something_is_picked() {
        var slot = TestContexts.typedSlot(Duration.class, "Duration.ofSeconds(3)");
        assertFalse(BasicsEditors.commit(slot, null, Duration.ofMillis(1000), false));
        assertEquals(0, slot.writes());
        assertTrue(BasicsEditors.commit(slot, null, Duration.ofMillis(1000), true));
        assertEquals(Duration.ofMillis(1000), slot.value());
    }

    @Test
    void a_changed_value_is_written() {
        var slot = TestContexts.typedSlot(Duration.class, "Duration.ofMillis(1500L)").withValue(Duration.ofMillis(1500));
        assertTrue(BasicsEditors.commit(slot, Duration.ofMillis(1500), Duration.ofMillis(800), true));
        assertEquals(Duration.ofMillis(800), slot.value());
        assertEquals(1, slot.writes());
    }

    @Test
    void a_time_pill_says_the_time_the_source_or_an_invitation() {
        assertEquals("07:30", BasicsEditors.timeLabel(
                TestContexts.typedSlot(LocalTime.class, "LocalTime.of(7, 30, 0)").withValue(LocalTime.of(7, 30))));
        assertEquals("LocalTime.now()", BasicsEditors.timeLabel(TestContexts.typedSlot(LocalTime.class, "LocalTime.now()")));
        assertEquals("Time…", BasicsEditors.timeLabel(TestContexts.typedSlot(LocalTime.class, "")));
    }

    /** A value with nanoseconds opens truncated; OK untouched must not write the truncation back. */
    @Test
    void nanoseconds_survive_an_untouched_ok() {
        LocalTime held = LocalTime.of(7, 30, 0, 500);
        var slot = TestContexts.typedSlot(LocalTime.class, "LocalTime.of(7, 30, 0, 500)").withValue(held);
        assertFalse(BasicsEditors.commit(slot, held, held.withNano(0), false));
        assertEquals(0, slot.writes());
    }
}
