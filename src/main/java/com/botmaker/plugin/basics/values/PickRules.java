package com.botmaker.plugin.basics.values;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The rules behind basics' reviewed pickers (picker 6e3) — names, steps and characters — kept apart from
 * their widgets so they are tested with no JavaFX, like {@link DurationParts} and {@link TimeText}.
 *
 * <p>English names, deliberately: the rest of the editor is English, and a day called "lun." beside a menu
 * called "Parameters" reads as a bug.
 */
public final class PickRules {

    /** The characters a person cannot see, by the name a person would type for them. */
    private static final Map<Character, String> INVISIBLE = Map.of(
            ' ', "space", '\t', "tab", '\n', "newline", '\r', "return");

    private PickRules() {
    }

    public static String shortName(DayOfWeek day) {
        return day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    public static String shortName(Month month) {
        return month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    public static String longName(Month month) {
        return month.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    /**
     * {@code value} moved one step in {@code direction} (+1 or -1): 1 for a whole number; for a decimal, one
     * unit of the last place {@code typed} shows, a tenth at least. Shift is ×10. The result is rounded to that
     * place, so stepping 0.2 by a tenth lands on 0.3 and not on 0.30000000000000004.
     */
    public static double step(double value, boolean whole, String typed, int direction, boolean shift) {
        int places = whole ? 0 : Math.max(1, decimals(typed));
        BigDecimal unit = BigDecimal.ONE.movePointLeft(places).multiply(BigDecimal.valueOf(shift ? 10 : 1));
        BigDecimal next = BigDecimal.valueOf(value).add(unit.multiply(BigDecimal.valueOf(Integer.signum(direction))));
        return next.setScale(places, RoundingMode.HALF_UP).doubleValue();
    }

    /** How many digits {@code typed} shows after its decimal point; 0 for none or for text that is no number. */
    static int decimals(String typed) {
        if (typed == null) return 0;
        String text = typed.strip();
        int dot = text.indexOf('.');
        if (dot < 0) return 0;
        int count = 0;
        for (int i = dot + 1; i < text.length() && Character.isDigit(text.charAt(i)); i++) count++;
        return count;
    }

    /** A character as the field shows it: a name for one a person cannot see, else itself. */
    public static String charLabel(char c) {
        return INVISIBLE.getOrDefault(c, String.valueOf(c));
    }

    /** What typing {@code text} means: a name from {@link #charLabel}, else its first character. */
    public static Optional<Character> charFrom(String text) {
        if (text == null || text.isEmpty()) return Optional.empty();
        for (Map.Entry<Character, String> named : INVISIBLE.entrySet()) {
            if (named.getValue().equalsIgnoreCase(text.strip()) && !text.isBlank()) return Optional.of(named.getKey());
        }
        return Optional.of(text.charAt(0));
    }

    public static String flagLabel(boolean on) {
        return on ? "On" : "Off";
    }
}
