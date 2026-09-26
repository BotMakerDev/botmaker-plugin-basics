package com.botmaker.plugin.basics.values;

import java.time.LocalTime;

/** What a time of day says: the pill and the summary line. Pure, 24h. */
final class TimeText {

    private TimeText() {}

    /** {@code 07:30}, or {@code 07:30:15} when the seconds are not zero. */
    static String pill(LocalTime t) {
        return t.getSecond() == 0
                ? String.format("%02d:%02d", t.getHour(), t.getMinute())
                : String.format("%02d:%02d:%02d", t.getHour(), t.getMinute(), t.getSecond());
    }

    /** {@code 7:30 in the morning}, {@code midnight}, {@code noon}. */
    static String words(LocalTime t) {
        LocalTime whole = t.withNano(0);
        if (whole.equals(LocalTime.MIDNIGHT)) return "midnight";
        if (whole.equals(LocalTime.NOON)) return "noon";
        String clock = whole.getSecond() == 0
                ? String.format("%d:%02d", whole.getHour(), whole.getMinute())
                : String.format("%d:%02d:%02d", whole.getHour(), whole.getMinute(), whole.getSecond());
        return clock + " " + part(whole.getHour());
    }

    private static String part(int hour) {
        if (hour <= 4 || hour >= 21) return "at night";
        if (hour <= 11) return "in the morning";
        if (hour <= 16) return "in the afternoon";
        return "in the evening";
    }
}
