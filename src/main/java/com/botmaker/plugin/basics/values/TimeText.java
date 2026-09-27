package com.botmaker.plugin.basics.values;

import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

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

    /**
     * What an hour, minute or second box holds when {@code typed} is committed: a number from 0 to
     * {@code max}, or {@code null} for anything else, so the box keeps its value. A spinner's own converter
     * threw {@code NumberFormatException} on a letter (feedback 3).
     */
    static Integer field(String typed, int max) {
        if (typed == null) return null;
        String digits = typed.strip();
        if (digits.isEmpty() || digits.length() > 3 || !digits.chars().allMatch(Character::isDigit)) return null;
        int value = Integer.parseInt(digits);
        return value <= max ? value : null;
    }

    /** {@code UTC}, or {@code UTC+02:00} / {@code UTC-03:30}: an offset as the pill and the dial name it. */
    static String offset(ZoneOffset offset) {
        return offset.getTotalSeconds() == 0 ? "UTC" : "UTC" + offset.getId();
    }

    /** {@code 07:30 UTC}: a time at an offset. */
    static String pill(OffsetTime t) {
        return pill(t.toLocalTime()) + " " + offset(t.getOffset());
    }

    /** The offsets clocks keep, west to east: every whole hour from −12 to +14 and the half and quarter ones. */
    static List<ZoneOffset> offsets() {
        TreeSet<ZoneOffset> out = new TreeSet<>(Comparator.comparingInt(ZoneOffset::getTotalSeconds));
        for (int hour = -12; hour <= 14; hour++) out.add(ZoneOffset.ofHours(hour));
        int[][] between = {{-9, -30}, {-3, -30}, {3, 30}, {4, 30}, {5, 30}, {5, 45}, {6, 30}, {8, 45}, {9, 30},
                {10, 30}, {12, 45}, {13, 45}};
        for (int[] hm : between) out.add(ZoneOffset.ofHoursMinutes(hm[0], hm[1]));
        return List.copyOf(out);
    }

    private static String part(int hour) {
        if (hour <= 4 || hour >= 21) return "at night";
        if (hour <= 11) return "in the morning";
        if (hour <= 16) return "in the afternoon";
        return "in the evening";
    }
}
