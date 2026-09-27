package com.botmaker.plugin.basics.values;

import com.botmaker.plugin.api.value.ComponentType;
import com.botmaker.plugin.api.value.PluginType;
import com.botmaker.plugin.toolkit.Types;

import java.awt.Color;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Supplier;

import static com.botmaker.plugin.toolkit.Types.count;
import static com.botmaker.plugin.toolkit.Types.method;
import static com.botmaker.plugin.toolkit.Types.whole;

/**
 * The thirteen types plugin #2 declares — text, a flag, two numbers, a character, a colour, a date, a time of
 * day, a duration, (since picker 6e3) a day of the week and a month, and (since picker feedback 3) a time of
 * day at an offset from UTC and the offset itself.
 *
 * <h2>These were the SDK's, and moving them is the point</h2>
 *
 * <p>Nothing about a whole number is about automating a game. They were the SDK's because the SDK was
 * written first, which made "a project can have a duration value" a privilege of plugin #1. The SDK keeps
 * the eight that are genuinely its own — {@code ImageTemplate}, {@code Precision}, {@code Point},
 * {@code Rect}, {@code Size}, {@code Direction}, {@code Key}, {@code MouseButton} — and the host reads both
 * lists the same way.
 *
 * <h2>One expression per type</h2>
 *
 * <p>Each was four declarations until 2026-09-22: a {@code ValueType} carrying a persisted id, a
 * {@code ValueCodec} with {@code parse}/{@code store}/{@code literal}/{@code valueOfLiteral}, and — for the
 * types the host could not seed — a {@code SourceSeed} carrying the fresh value as Java <em>text</em>. Two
 * of the four were strings javac never looked at. Then one class per type; since 2026-09-28 one
 * {@link Types} expression, which says only the fresh value, the editor, and for a call how a value comes
 * apart.
 *
 * <p><b>The identity is the Java class now, not an id.</b> A project's file says
 * {@code java.time.Duration} because that is what the field is declared as, and has done since a parameter
 * became a {@code @Param} field (2026-09-17); the ids {@code "DURATION"} and {@code "YES_NO"} were the last
 * thing still reading the enum vocabulary and nothing writes one any more.
 *
 * <h2>A primitive is declared, and the host resolves its wrapper to it</h2>
 *
 * <p>{@code int.class} rather than {@code Integer.class}, because {@code int} is what a bot's field is
 * overwhelmingly declared as and a picker offering both would be offering one type twice. A field declared
 * {@code Integer} resolves to the same declaration, which is the rule {@code ValueCatalog.forJava} already
 * applied and the host still does.
 *
 * <h2>Fully qualified, because a generated file cannot be made to import</h2>
 *
 * <p>The host writes {@code java.time.Duration.ofMillis(3000L)} and {@code new java.awt.Color(255, 0, 0)}
 * from the {@link ComponentType}s below — the <em>components</em>, never a {@code decode("#FF0000")} or a
 * {@code parse(…)}. A generated file holds no expression that can throw at class initialisation, which is
 * what it means for a bot never to fail to start because of its own configuration.
 *
 * <h2>The SDK overrides two of these, and that is legal</h2>
 *
 * <p>{@code Duration} and {@code Color} are declared here and get plain editors here; the SDK offers better
 * ones through {@code slotEditors()} and the host asks the user which to use. Neither could move: the SDK's
 * colour picker samples a frozen frame of the <em>capture target</em>, whose list is in the SDK's own file
 * and which no other plugin may read, and its duration editor rewrites {@code Wait.time(x)} into
 * {@code Wait.between(min, max)}, which names an SDK type. <b>This is the one place a type is named twice,
 * and it is named for two different reasons.</b>
 */
public final class BasicsTypes {

    private BasicsTypes() {
    }

    /** Text. A bot's most common field, and the one the host writes as a plain literal. */
    public static final PluginType<String> TEXT = Types.editable(String.class, () -> "", () -> BasicsEditors::text);

    /** A tick box. {@code false} is the fresh value, which is the state that does nothing. */
    public static final PluginType<Boolean> FLAG = Types.editable(boolean.class, () -> false, () -> BasicsEditors::flag);

    /** A whole number. */
    public static final PluginType<Integer> WHOLE = Types.editable(int.class, () -> 0, () -> BasicsEditors::whole);

    /** A decimal number. */
    public static final PluginType<Double> DECIMAL = Types.editable(double.class, () -> 0.0, () -> BasicsEditors::decimal);

    /**
     * One character.
     *
     * <p>{@code 'a'} rather than {@code '\0'}: the null character is a value a person cannot see, cannot
     * type and would not recognise in their own file. It is the default the old reader answered for
     * unreadable text, carried over unchanged.
     */
    public static final PluginType<Character> CHARACTER =
            Types.editable(char.class, () -> 'a', () -> BasicsEditors::character);

    /**
     * A colour, written as its three components, {@code new Color(r, g, b)}.
     *
     * <p>Never {@code Color.decode("#FF0000")}, which parses at class initialisation and can throw. White
     * is the fresh value — visible on every background, and what the old reader answered for unreadable
     * text. A component out of range is pulled in rather than thrown at: a file may say anything.
     */
    public static final PluginType<Color> COLOR = Types.editable(Color.class, () -> Color.WHITE, () -> BasicsEditors::color)
            .writtenAs(Types.call(Color.class, Types.constructor(Color.class, int.class, int.class, int.class),
                    c -> List.of(c.getRed(), c.getGreen(), c.getBlue()),
                    parts -> new Color(channel(parts, 0), channel(parts, 1), channel(parts, 2))));

    private static final LocalDate FRESH_DATE = LocalDate.of(2000, 1, 1);

    /** A date. {@code LocalDate.of(2000, 1, 1)} is the fresh one, as the old reader's fallback was. */
    public static final PluginType<LocalDate> DATE = Types.editable(LocalDate.class, () -> FRESH_DATE, () -> BasicsEditors::date)
            .writtenAs(Types.call(LocalDate.class, method(LocalDate.class, "of", int.class, int.class, int.class),
                    d -> List.of(d.getYear(), d.getMonthValue(), d.getDayOfMonth()),
                    parts -> orElse(() -> LocalDate.of(whole(parts, 0), whole(parts, 1), whole(parts, 2)),
                            FRESH_DATE)));

    /**
     * A time of day.
     *
     * <p>Seconds are always written, so a {@code 07:30:15} in somebody's file is not silently truncated to
     * the minute the first time the window opens.
     */
    public static final PluginType<LocalTime> TIME =
            Types.editable(LocalTime.class, () -> LocalTime.MIDNIGHT, () -> BasicsEditors::time)
                    .preview(() -> BasicsEditors::timePreview)
                    .writtenAs(Types.call(LocalTime.class,
                            method(LocalTime.class, "of", int.class, int.class, int.class),
                            t -> List.of(t.getHour(), t.getMinute(), t.getSecond()),
                            parts -> orElse(() -> LocalTime.of(whole(parts, 0), whole(parts, 1), whole(parts, 2)),
                                    LocalTime.MIDNIGHT)));

    private static final OffsetTime FRESH_OFFSET_TIME = OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC);

    /**
     * A time of day at an offset from UTC (feedback 3) — {@code OffsetTime.of(7, 30, 0, 0, ZoneOffset.UTC)}. A
     * {@link #TIME} is the clock of the computer the bot runs on; this one is the same moment anywhere,
     * which is what a game's daily reset at 00:00 UTC is. Nanoseconds are always written as 0, so the Java is
     * the one {@code OffsetTime.of} a person writes.
     */
    public static final PluginType<OffsetTime> OFFSET_TIME =
            Types.editable(OffsetTime.class, () -> FRESH_OFFSET_TIME, () -> BasicsEditors::offsetTime)
                    .preview(() -> BasicsEditors::offsetTimePreview)
                    .writtenAs(Types.call(OffsetTime.class, method(OffsetTime.class, "of",
                                    int.class, int.class, int.class, int.class, ZoneOffset.class),
                            t -> List.of(t.getHour(), t.getMinute(), t.getSecond(), t.getNano(), t.getOffset()),
                            parts -> orElse(() -> OffsetTime.of(whole(parts, 0), whole(parts, 1), whole(parts, 2),
                                    whole(parts, 3), Types.part(parts, 4, ZoneOffset.class)), FRESH_OFFSET_TIME)));

    /**
     * An offset from UTC, part of an {@link #OFFSET_TIME} (feedback 3). {@code ZoneOffset.UTC} is written as
     * that constant; any other offset as {@code ZoneOffset.ofHoursMinutes(h, m)}, the minutes carrying the
     * hours' sign.
     */
    public static final PluginType<ZoneOffset> ZONE_OFFSET =
            Types.editable(ZoneOffset.class, () -> ZoneOffset.UTC, () -> BasicsEditors::zoneOffset)
                    .writtenAs(Types.call(ZoneOffset.class,
                                    method(ZoneOffset.class, "ofHoursMinutes", int.class, int.class),
                                    offset -> List.of(offset.getTotalSeconds() / 3600,
                                            (offset.getTotalSeconds() % 3600) / 60),
                                    parts -> orElse(() -> ZoneOffset.ofHoursMinutes(whole(parts, 0), whole(parts, 1)),
                                            ZoneOffset.UTC))
                            .constants(Types.constant(ZoneOffset.class, "UTC")));

    /**
     * A length of time, written as milliseconds.
     *
     * <p>One spelling, {@code Duration.ofMillis(n)}, so there is one rule for writing and one for reading.
     * {@code Duration.ofSeconds(3)} in somebody's file is <b>not</b> rewritten into it: it reads as a
     * hand-written initializer and is shown as the author wrote it, which is the same answer any expression
     * the host did not write gets.
     */
    public static final PluginType<Duration> DURATION =
            Types.editable(Duration.class, () -> Duration.ZERO, () -> BasicsEditors::duration)
                    .preview(() -> BasicsEditors::durationPreview)
                    .writtenAs(Types.call(Duration.class, method(Duration.class, "ofMillis", long.class),
                            d -> List.of(d.toMillis()), parts -> Duration.ofMillis(count(parts, 0))));

    /**
     * A day of the week — {@code DayOfWeek.MONDAY}, a constant the host reads and writes as the enum it is.
     * Declared since picker 6e3 so basics draws it (a row of seven days) rather than the host's generic enum
     * dropdown: a JDK type is basics' to draw.
     */
    public static final PluginType<DayOfWeek> DAY_OF_WEEK = Types.enumType(DayOfWeek.class, () -> BasicsEditors::dayOfWeek)
            .preview(() -> BasicsEditors::dayOfWeekPreview);

    /** A month — {@code Month.MARCH}, drawn as a pill opening the twelve (picker 6e3). */
    public static final PluginType<Month> MONTH = Types.enumType(Month.class, () -> BasicsEditors::month)
            .preview(() -> BasicsEditors::monthPreview);

    /**
     * The thirteen, in the order a picker should offer them: the literals a bot mostly counts, flags and labels
     * with, then the seven time types.
     */
    public static final List<PluginType<?>> ALL = List.of(
            TEXT, FLAG, WHOLE, DECIMAL, CHARACTER, COLOR, DATE, TIME, OFFSET_TIME, ZONE_OFFSET, DURATION,
            DAY_OF_WEEK, MONTH);

    private static int channel(List<Object> parts, int index) {
        return Math.clamp(whole(parts, index), 0, 255);
    }

    /**
     * {@code build}, or {@code fallback} when the JDK refuses the parts — an impossible date in somebody's
     * file reads as the fresh value rather than stopping the project from opening.
     */
    private static <T> T orElse(Supplier<T> build, T fallback) {
        try {
            return build.get();
        } catch (RuntimeException impossible) {
            return fallback;
        }
    }
}
