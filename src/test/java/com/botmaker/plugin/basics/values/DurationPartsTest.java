package com.botmaker.plugin.basics.values;

import com.botmaker.plugin.basics.values.DurationParts.Unit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DurationPartsTest {

    @Test
    void splits_and_joins() {
        assertEquals(new DurationParts(1, 1, 30, 250), DurationParts.of(3_690_250L));
        assertEquals(3_690_250L, DurationParts.of(3_690_250L).total());
        assertEquals(new DurationParts(0, 0, 0, 0), DurationParts.of(-5));
    }

    @Test
    void a_step_carries_across_units() {
        assertEquals(new DurationParts(0, 1, 0, 0), DurationParts.of(59_000).step(Unit.SECONDS, 1));
        assertEquals(new DurationParts(0, 59, 59, 0), DurationParts.of(3_600_000).step(Unit.SECONDS, -1));
        assertEquals(new DurationParts(2, 0, 0, 0), DurationParts.of(3_600_000).step(Unit.HOURS, 1));
    }

    @Test
    void a_step_never_goes_below_zero() {
        assertEquals(new DurationParts(0, 0, 0, 0), DurationParts.of(0).step(Unit.SECONDS, -1));
        assertEquals(new DurationParts(0, 0, 0, 0), DurationParts.of(500).step(Unit.SECONDS, -1));
    }

    @Test
    void a_typed_unit_is_normalised() {
        assertEquals(new DurationParts(0, 1, 30, 0), DurationParts.of(0).with(Unit.SECONDS, 90).normalised());
        assertEquals(new DurationParts(0, 0, 0, 0), DurationParts.of(0).with(Unit.MINUTES, -3).normalised());
        assertEquals(90L, DurationParts.of(0).with(Unit.SECONDS, 90).get(Unit.SECONDS));
    }

    @Test
    void hours_have_no_upper_bound() {
        assertEquals(new DurationParts(30, 0, 0, 0), DurationParts.of(30 * 3_600_000L));
    }
}
