package com.botmaker.plugin.basics.values;

/**
 * A length of time as the four units a person reads it in. Pure: the Duration picker's spinners and presets
 * go through here, so every carry and every floor is tested without a screen.
 *
 * <p>Held unnormalised on purpose: {@link #with} records what was typed into one box, and
 * {@link #normalised()} is where {@code 90} seconds becomes {@code 1m30s}.
 */
record DurationParts(long hours, long minutes, long seconds, long millis) {

    enum Unit {
        HOURS("h", 3_600_000L), MINUTES("m", 60_000L), SECONDS("s", 1_000L), MILLIS("ms", 1L);

        private final String suffix;
        private final long millis;

        Unit(String suffix, long millis) {
            this.suffix = suffix;
            this.millis = millis;
        }

        String suffix() {
            return suffix;
        }

        long millis() {
            return millis;
        }
    }

    /** {@code total} split largest unit first; a negative length is none. */
    static DurationParts of(long total) {
        long t = Math.max(0L, total);
        return new DurationParts(t / 3_600_000L, t / 60_000L % 60, t / 1_000L % 60, t % 1_000L);
    }

    long total() {
        return Math.max(0L, hours * 3_600_000L + minutes * 60_000L + seconds * 1_000L + millis);
    }

    DurationParts normalised() {
        return of(total());
    }

    /** One spinner click: the whole length moves by one of {@code unit}, carrying, never below zero. */
    DurationParts step(Unit unit, int delta) {
        return of(total() + delta * unit.millis());
    }

    /** What was typed into one box, the others kept; negative is zero. */
    DurationParts with(Unit unit, long value) {
        long v = Math.max(0L, value);
        return switch (unit) {
            case HOURS -> new DurationParts(v, minutes, seconds, millis);
            case MINUTES -> new DurationParts(hours, v, seconds, millis);
            case SECONDS -> new DurationParts(hours, minutes, v, millis);
            case MILLIS -> new DurationParts(hours, minutes, seconds, v);
        };
    }

    long get(Unit unit) {
        return switch (unit) {
            case HOURS -> hours;
            case MINUTES -> minutes;
            case SECONDS -> seconds;
            case MILLIS -> millis;
        };
    }
}
