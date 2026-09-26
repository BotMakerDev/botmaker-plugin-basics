package com.botmaker.plugin.basics.values;

/**
 * The time-of-day dial's geometry. Pure. Positions are offsets from the dial's centre in screen pixels (y grows
 * downward); angles are degrees clockwise from the top. Hours sit on two rings — outer 1–12, inner 13–23 and 00
 * — so any hour is one click; minutes on one ring, six degrees each.
 */
final class ClockDial {

    /** A click closer to the centre than this share of the radius is on the inner ring. */
    static final double INNER = 0.7;

    private ClockDial() {}

    /** Clockwise from the top, 0 ≤ angle < 360. The exact centre reads as the bottom, harmlessly. */
    static double degrees(double dx, double dy) {
        return (Math.toDegrees(Math.atan2(dx, -dy)) + 360.0) % 360.0;
    }

    static int hourAt(double dx, double dy, double radius) {
        int position = (int) Math.floor(degrees(dx, dy) / 30.0 + 0.5) % 12;
        boolean innerRing = Math.hypot(dx, dy) < INNER * radius;
        if (innerRing) return position == 0 ? 0 : position + 12;
        return position == 0 ? 12 : position;
    }

    static int minuteAt(double dx, double dy) {
        return (int) Math.floor(degrees(dx, dy) / 6.0 + 0.5) % 60;
    }

    static double hourAngle(int hour) {
        return (hour % 12) * 30.0;
    }

    static double minuteAngle(int minute) {
        return (minute % 60) * 6.0;
    }

    /** Whether {@code hour} is drawn on the inner ring: 13–23 and 00. */
    static boolean inner(int hour) {
        return hour == 0 || hour > 12;
    }
}
