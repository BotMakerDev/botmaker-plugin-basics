package com.botmaker.plugin.basics.values;

import java.awt.Color;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;

/**
 * What a stored value's text means, for the nine types whose Java type is the JDK's — one reader per type,
 * and the grammar every one of them is written in.
 *
 * <h2>Why this is here rather than in the SDK</h2>
 *
 * <p>Every one of these answers is about {@code java.lang}, {@code java.time} and {@code java.awt}. None of
 * them is the SDK's vocabulary; they were the SDK's only because the SDK was written first, and a second
 * plugin wanting a whole number of its own would have had to be granted one. The code moved here on
 * 2026-09-09 (phase 2 of the plan) and {@code com.botmaker.sdk.authoring.WireText} delegates to it, so
 * there is one implementation and the two cannot disagree about what {@code "1m30s"} means.
 *
 * <h2>This half runs in a bot, so it names the JDK and nothing else</h2>
 *
 * <p>This module is on a bot's classpath — the SDK declares it at {@code compile} scope, so it travels with
 * the SDK wherever the SDK goes. The contract and JavaFX are {@code provided} here and are therefore
 * <em>absent</em> from a bot, so nothing this class touches may name either. The registrations that pair
 * these readers with a {@link com.botmaker.plugin.api.value.ValueType} live next door in
 * {@link BasicsValueTypes}, which is editor-side and free to name both.
 *
 * <h2>Every reader is total</h2>
 *
 * <p>Nothing here throws and nothing returns {@code null}. A number that will not parse, a date in a shape
 * nobody knows, a duration in an unknown unit: each answers the type's documented default. A bot never
 * fails to start because of its own configuration file, and a project must still open when that file says
 * something impossible — a refusal here would be a project nobody could repair through the editor.
 */
public final class JdkText {

    private static final long SECOND = 1000L;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;

    private JdkText() {
    }

    /** Text, exactly as stored — not trimmed, because a trailing space may be the point. */
    public static String text(String stored) {
        return stored == null ? "" : stored;
    }

    /** A tick box. Anything that is not {@code "true"} is false. */
    public static boolean flag(String stored) {
        return Boolean.parseBoolean(trim(stored));
    }

    /** A whole number, rounded from what was stored so a hand-edited {@code "3.0"} still reads as 3. */
    public static int whole(String stored) {
        return (int) Math.rint(number(stored, 0));
    }

    /** A decimal number; 0.0 when unreadable. */
    public static double decimal(String stored) {
        return number(stored, 0);
    }

    /** The first character, or {@code 'a'} when nothing was stored. */
    public static char letter(String stored) {
        return stored == null || stored.isEmpty() ? 'a' : stored.charAt(0);
    }

    /** An ISO date ({@code 2026-08-24}); 2000-01-01 when unreadable. */
    public static LocalDate date(String stored) {
        try {
            return LocalDate.parse(trim(stored));
        } catch (RuntimeException e) {
            return LocalDate.of(2000, 1, 1);
        }
    }

    /** An ISO time of day ({@code 07:30}); midnight when unreadable. */
    public static LocalTime time(String stored) {
        try {
            return LocalTime.parse(trim(stored));
        } catch (RuntimeException e) {
            return LocalTime.MIDNIGHT;
        }
    }

    /**
     * A duration written the way a person says one: {@code 250ms}, {@code 90s}, {@code 5m}, {@code 1h30m}.
     *
     * <p>Deliberately generous — any ordering, any subset of units, spaces and case ignored, and a bare
     * number read as milliseconds so {@code "500"} still means something. Anything it cannot read at all is
     * {@link Duration#ZERO}: an unknown unit, a unit with no number in front of it, or a count so large it
     * is certainly a typo (a bot delay is not measured in weeks).
     *
     * <p>{@link #spellDuration} writes the same value back out in one canonical form, so a typed
     * {@code "90 s"} is stored as {@code "1m30s"} and a diff never churns on spacing.
     */
    public static Duration duration(String stored) {
        String s = trim(stored).toLowerCase(Locale.ROOT).replace(" ", "");
        long total = 0;
        long digits = 0;
        boolean sawDigit = false;
        boolean sawAny = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                digits = digits * 10 + (c - '0');
                if (digits > Integer.MAX_VALUE) return Duration.ZERO;
                sawDigit = true;
                continue;
            }
            if (!sawDigit) return Duration.ZERO;
            // "ms" is the only two-letter unit, and it must be checked before the bare "m" it starts with.
            if (c == 'm' && i + 1 < s.length() && s.charAt(i + 1) == 's') {
                total += digits;
                i++;
            } else if (c == 'h') {
                total += digits * HOUR;
            } else if (c == 'm') {
                total += digits * MINUTE;
            } else if (c == 's') {
                total += digits * SECOND;
            } else {
                return Duration.ZERO;
            }
            digits = 0;
            sawDigit = false;
            sawAny = true;
        }
        if (sawDigit) {
            total += digits;
            sawAny = true;
        }
        return Duration.ofMillis(sawAny ? total : 0L);
    }

    /**
     * {@code millis} in the one canonical spelling {@link #duration} reads back: the non-zero components in
     * descending order, and {@code 0s} for nothing and for anything negative.
     *
     * <p>Durations are stored as text rather than as a number because the unit is the part a reader needs:
     * {@code 90000} in a file says nothing, and whoever wrote it had "a minute and a half" in mind.
     */
    public static String spellDuration(long millis) {
        if (millis <= 0) return "0s";
        StringBuilder out = new StringBuilder();
        long left = millis;
        left = spellUnit(out, left, HOUR, "h");
        left = spellUnit(out, left, MINUTE, "m");
        left = spellUnit(out, left, SECOND, "s");
        if (left > 0) out.append(left).append("ms");
        return out.toString();
    }

    private static long spellUnit(StringBuilder out, long left, long unit, String suffix) {
        long count = left / unit;
        if (count > 0) out.append(count).append(suffix);
        return left % unit;
    }

    /**
     * A colour as {@code #RRGGBB}; white when unreadable.
     *
     * <p>The hash is optional, because a person typing a colour into a field routinely leaves it off and
     * {@link Color#decode} treats a bare {@code 1a2b3c} as a decimal number rather than as hex — which is
     * not a rejection the user can see the reason for. Six hex digits with nothing in front of them can
     * only have been meant one way.
     */
    public static Color color(String stored) {
        String text = trim(stored);
        if (text.matches("[0-9a-fA-F]{6}")) text = "#" + text;
        try {
            return Color.decode(text);
        } catch (RuntimeException e) {
            return Color.WHITE;
        }
    }

    /** A colour as {@code #RRGGBB}, the spelling {@link #color} reads back. */
    public static String spellColor(Color value) {
        return "#%02X%02X%02X".formatted(value.getRed(), value.getGreen(), value.getBlue());
    }

    // ---- shared parsing ---------------------------------------------------------------------------------

    /** A finite number, or {@code fallback} for anything else — including an infinity or a NaN. */
    static double number(String stored, double fallback) {
        try {
            double parsed = Double.parseDouble(trim(stored));
            return Double.isFinite(parsed) ? parsed : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static String trim(String stored) {
        return stored == null ? "" : stored.trim();
    }
}
