package com.botmaker.plugin.basics.values;

import com.botmaker.plugin.api.slot.ValueContext;
import com.botmaker.plugin.api.value.ComponentType;
import com.botmaker.plugin.api.value.PluginType;
import com.botmaker.plugin.toolkit.AbstractPluginType;
import javafx.scene.Node;

import java.awt.Color;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * The nine types plugin #2 declares — text, a flag, two numbers, a character, a colour, a date, a time of
 * day and a duration.
 *
 * <h2>These were the SDK's, and moving them is the point</h2>
 *
 * <p>Nothing about a whole number is about automating a game. They were the SDK's because the SDK was
 * written first, which made "a project can have a duration value" a privilege of plugin #1. The SDK keeps
 * the eight that are genuinely its own — {@code ImageTemplate}, {@code Precision}, {@code Point},
 * {@code Rect}, {@code Size}, {@code Direction}, {@code Key}, {@code MouseButton} — and the host reads both
 * lists the same way.
 *
 * <h2>One class per type, every method abstract</h2>
 *
 * <p>Each was four declarations until 2026-09-22: a {@code ValueType} carrying a persisted id, a
 * {@code ValueCodec} with {@code parse}/{@code store}/{@code literal}/{@code valueOfLiteral}, and — for the
 * types the host could not seed — a {@code SourceSeed} carrying the fresh value as Java <em>text</em>. Two
 * of the four were strings javac never looked at.
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
    public static final class TextType extends AbstractPluginType<String> {
        public TextType() { super(String.class); }
        @Override public String fresh() { return ""; }
        @Override public Node editor(ValueContext ctx) { return BasicsEditors.text(ctx); }
    }

    /** A tick box. {@code false} is the fresh value, which is the state that does nothing. */
    public static final class FlagType extends AbstractPluginType<Boolean> {
        public FlagType() { super(boolean.class); }
        @Override public Boolean fresh() { return false; }
        @Override public Node editor(ValueContext ctx) { return BasicsEditors.flag(ctx); }
    }

    /** A whole number. */
    public static final class WholeType extends AbstractPluginType<Integer> {
        public WholeType() { super(int.class); }
        @Override public Integer fresh() { return 0; }
        @Override public Node editor(ValueContext ctx) { return BasicsEditors.whole(ctx); }
    }

    /** A decimal number. */
    public static final class DecimalType extends AbstractPluginType<Double> {
        public DecimalType() { super(double.class); }
        @Override public Double fresh() { return 0.0; }
        @Override public Node editor(ValueContext ctx) { return BasicsEditors.decimal(ctx); }
    }

    /**
     * One character.
     *
     * <p>{@code 'a'} rather than {@code '\0'}: the null character is a value a person cannot see, cannot
     * type and would not recognise in their own file. It is the default the old reader answered for
     * unreadable text, carried over unchanged.
     */
    public static final class CharacterType extends AbstractPluginType<Character> {
        public CharacterType() { super(char.class); }
        @Override public Character fresh() { return 'a'; }
        @Override public Node editor(ValueContext ctx) { return BasicsEditors.character(ctx); }
    }

    /**
     * A colour, written as its three components.
     *
     * <p>Never {@code Color.decode("#FF0000")}, which parses at class initialisation and can throw. White
     * is the fresh value — visible on every background, and what the old reader answered for unreadable
     * text.
     */
    public static final class ColorType extends AbstractPluginType<Color> implements ComponentType<Color> {
        public ColorType() { super(Color.class); }
        @Override public Color fresh() { return Color.WHITE; }
        @Override public Node editor(ValueContext ctx) { return BasicsEditors.color(ctx); }

        @Override public List<Class<?>> componentTypes() {
            return List.of(int.class, int.class, int.class);
        }
        @Override public List<Object> components(Color c) {
            return List.of(c.getRed(), c.getGreen(), c.getBlue());
        }
        @Override public Color build(List<Object> parts) {
            return new Color(clamp(whole(parts, 0)), clamp(whole(parts, 1)), clamp(whole(parts, 2)));
        }

        /** A component out of range is pulled in rather than thrown at: a file may say anything. */
        private static int clamp(int channel) {
            return Math.clamp(channel, 0, 255);
        }
    }

    /** A date. {@code LocalDate.of(2000, 1, 1)} is the fresh one, as the old reader's fallback was. */
    public static final class DateType extends AbstractPluginType<LocalDate>
            implements ComponentType<LocalDate> {
        public DateType() { super(LocalDate.class); }
        @Override public LocalDate fresh() { return LocalDate.of(2000, 1, 1); }
        @Override public Node editor(ValueContext ctx) { return BasicsEditors.date(ctx); }

        @Override public String factory() { return "of"; }
        @Override public List<Class<?>> componentTypes() {
            return List.of(int.class, int.class, int.class);
        }
        @Override public List<Object> components(LocalDate d) {
            return List.of(d.getYear(), d.getMonthValue(), d.getDayOfMonth());
        }
        @Override public LocalDate build(List<Object> parts) {
            try {
                return LocalDate.of(whole(parts, 0), whole(parts, 1), whole(parts, 2));
            } catch (RuntimeException impossibleDate) {
                return fresh();
            }
        }
    }

    /**
     * A time of day.
     *
     * <p>Seconds are always written, so a {@code 07:30:15} in somebody's file is not silently truncated to
     * the minute the first time the window opens.
     */
    public static final class TimeType extends AbstractPluginType<LocalTime>
            implements ComponentType<LocalTime> {
        public TimeType() { super(LocalTime.class); }
        @Override public LocalTime fresh() { return LocalTime.MIDNIGHT; }
        @Override public Node editor(ValueContext ctx) { return BasicsEditors.time(ctx); }

        @Override public String factory() { return "of"; }
        @Override public List<Class<?>> componentTypes() {
            return List.of(int.class, int.class, int.class);
        }
        @Override public List<Object> components(LocalTime t) {
            return List.of(t.getHour(), t.getMinute(), t.getSecond());
        }
        @Override public LocalTime build(List<Object> parts) {
            try {
                return LocalTime.of(whole(parts, 0), whole(parts, 1), whole(parts, 2));
            } catch (RuntimeException impossibleTime) {
                return fresh();
            }
        }
    }

    /**
     * A length of time, written as milliseconds.
     *
     * <p>One spelling, {@code Duration.ofMillis(n)}, so there is one rule for writing and one for reading.
     * {@code Duration.ofSeconds(3)} in somebody's file is <b>not</b> rewritten into it: it reads as a
     * hand-written initializer and is shown as the author wrote it, which is the same answer any expression
     * the host did not write gets.
     */
    public static final class DurationType extends AbstractPluginType<Duration>
            implements ComponentType<Duration> {
        public DurationType() { super(Duration.class); }
        @Override public Duration fresh() { return Duration.ZERO; }
        @Override public Node editor(ValueContext ctx) { return BasicsEditors.duration(ctx); }

        @Override public String factory() { return "ofMillis"; }
        @Override public List<Class<?>> componentTypes() { return List.of(long.class); }
        @Override public List<Object> components(Duration d) { return List.of(d.toMillis()); }
        @Override public Duration build(List<Object> parts) { return Duration.ofMillis(count(parts, 0)); }
    }

    /**
     * The nine, in the order a picker should offer them: the literals a bot mostly counts, flags and labels
     * with, then the three time types.
     */
    public static final List<PluginType<?>> ALL = List.of(
            new TextType(), new FlagType(), new WholeType(), new DecimalType(), new CharacterType(),
            new ColorType(), new DateType(), new TimeType(), new DurationType());
}
