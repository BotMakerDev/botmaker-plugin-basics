package com.botmaker.plugin.basics.values;

import java.util.ArrayList;
import java.util.List;

/**
 * What a length of time says: the pill ({@link #spell}), the summary line ({@link #words}) and the preset chips.
 * Pure. A label, never a stored form — nothing reads it back.
 */
final class DurationText {

    /** The chips, in millis: the waits a bot mostly writes. */
    static final List<Long> PRESETS = List.of(100L, 250L, 500L, 1_000L, 2_000L, 5_000L,
            10_000L, 30_000L, 60_000L, 300_000L, 600_000L, 3_600_000L);

    private DurationText() {}

    /** {@code 0s}, {@code 250ms}, {@code 1m30s}, {@code 1h5s} — the largest units first, zeroes dropped. */
    static String spell(long millis) {
        if (millis <= 0) return "0s";
        DurationParts p = DurationParts.of(millis);
        StringBuilder out = new StringBuilder();
        if (p.hours() > 0) out.append(p.hours()).append('h');
        if (p.minutes() > 0) out.append(p.minutes()).append('m');
        if (p.seconds() > 0) out.append(p.seconds()).append('s');
        if (p.millis() > 0) out.append(p.millis()).append("ms");
        return out.toString();
    }

    /** {@code no wait}, {@code 1 minute 30 seconds}, {@code 250 milliseconds}. */
    static String words(long millis) {
        if (millis <= 0) return "no wait";
        DurationParts p = DurationParts.of(millis);
        List<String> out = new ArrayList<>();
        add(out, p.hours(), "hour");
        add(out, p.minutes(), "minute");
        add(out, p.seconds(), "second");
        add(out, p.millis(), "millisecond");
        return String.join(" ", out);
    }

    private static void add(List<String> out, long count, String unit) {
        if (count > 0) out.add(count + " " + unit + (count == 1 ? "" : "s"));
    }
}
