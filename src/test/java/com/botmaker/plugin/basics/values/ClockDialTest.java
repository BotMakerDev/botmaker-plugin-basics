package com.botmaker.plugin.basics.values;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClockDialTest {

    private static final double R = 100;

    /** Where a number sits: clockwise from the top, y growing downward as on screen. */
    private static double[] at(double degrees, double distance) {
        double rad = Math.toRadians(degrees);
        return new double[]{Math.sin(rad) * distance, -Math.cos(rad) * distance};
    }

    @Test
    void the_outer_ring_is_1_to_12_and_the_inner_13_to_00() {
        double[] outerThree = at(90, 85), innerThree = at(90, 55), outerTop = at(0, 85), innerTop = at(0, 55);
        assertEquals(3, ClockDial.hourAt(outerThree[0], outerThree[1], R));
        assertEquals(15, ClockDial.hourAt(innerThree[0], innerThree[1], R));
        assertEquals(12, ClockDial.hourAt(outerTop[0], outerTop[1], R));
        assertEquals(0, ClockDial.hourAt(innerTop[0], innerTop[1], R));
    }

    @Test
    void every_hour_round_trips_through_its_angle() {
        for (int h = 0; h < 24; h++) {
            double[] p = at(ClockDial.hourAngle(h), ClockDial.inner(h) ? 55 : 85);
            assertEquals(h, ClockDial.hourAt(p[0], p[1], R), "hour " + h);
        }
    }

    @Test
    void a_minute_snaps_half_up_to_six_degrees() {
        // 3.1 rather than 3: exactly half a step lands on a float edge (2.9999…), which is no test of the rule.
        double[] two = at(2, 85), three = at(3.1, 85), last = at(359, 85);
        assertEquals(0, ClockDial.minuteAt(two[0], two[1]));
        assertEquals(1, ClockDial.minuteAt(three[0], three[1]));
        assertEquals(0, ClockDial.minuteAt(last[0], last[1]));
        for (int m = 0; m < 60; m++) {
            double[] p = at(ClockDial.minuteAngle(m), 85);
            assertEquals(m, ClockDial.minuteAt(p[0], p[1]), "minute " + m);
        }
    }

    @Test
    void the_exact_centre_answers_a_real_hour_and_minute() {
        int hour = ClockDial.hourAt(0, 0, R);
        int minute = ClockDial.minuteAt(0, 0);
        assertTrue(hour >= 0 && hour < 24, "hour " + hour);
        assertTrue(minute >= 0 && minute < 60, "minute " + minute);
    }
}
