package com.botmaker.plugin.basics.values;

import com.botmaker.plugin.api.value.ValueCatalog;
import com.botmaker.plugin.api.value.ValueCodec;
import com.botmaker.plugin.api.value.ValueType;
import com.botmaker.plugin.toolkit.Source;

import java.awt.Color;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.function.Function;

/**
 * The nine types plugin #2 contributes to a project's vocabulary — text, a flag, two numbers, a character,
 * a colour, a date, a time of day and a duration — registered through the same {@link ValueCatalog} builder
 * any other plugin uses.
 *
 * <h2>These were the SDK's, and moving them is the point</h2>
 *
 * <p>Nothing about a whole number is about automating a game. They were registered in
 * {@code com.botmaker.sdk.internal.authoring.SdkValueTypes} because the SDK was written first, which made
 * "a project can have a duration variable" a privilege of plugin #1. The SDK keeps the eight that are
 * genuinely its own vocabulary — {@code IMAGE_TEMPLATE}, {@code PRECISION}, {@code POINT}, {@code RECT},
 * {@code SIZE}, {@code DIRECTION}, {@code KEY}, {@code MOUSE_BUTTON} — and merges this catalog in like any
 * host merging any plugin's.
 *
 * <h2>The ids are persisted and the ids are stable</h2>
 *
 * <p>Each id is the name its old enum constant had, unchanged by the move: a project written years ago
 * still says {@code "DURATION"} and still means this. Renaming one rewrites every stored project silently
 * — don't. A project opened <em>without</em> this plugin keeps such a value as raw text, read-only and
 * un-emitted ({@link ValueType#unknown}), which is what makes an open vocabulary safe.
 *
 * <h2>Qualified, because a generated file cannot be made to import</h2>
 *
 * <p>The {@code java.time} and {@code java.awt} types are written <b>fully qualified</b> in both the source
 * name and the literal. None of them declares an {@link ValueType.Builder#importing import}: the host
 * shortens what it can and an expression it chose not to shorten is still correct on its own.
 *
 * <h2>Editor-side, unlike {@link JdkText}</h2>
 *
 * <p>This class names the contract and the toolkit, both of which are {@code provided} here and therefore
 * absent from a bot. That is fine and is the split: a bot reads its values through {@link JdkText}, which
 * names only the JDK, while the registrations, the labels and the Java literals are for whoever is editing
 * or generating a project.
 */
public final class BasicsValueTypes {

    private BasicsValueTypes() {
    }

    /**
     * The headings a picker files these under. Free strings by contract — a plugin groups its own types
     * without a constant being granted to it — so they are named once here rather than spelled at nine call
     * sites, where a typo would silently split a group in two.
     */
    private static final String BASICS = "Basics";
    private static final String WHEN = "Date & time";

    public static final ValueType TEXT = ValueType.of(ValueCatalog.TEXT_ID)
            .label("Text").group(BASICS).source("String").build();

    public static final ValueType YES_NO = ValueType.of(ValueCatalog.FLAG_ID)
            .label("Yes / no").group(BASICS)
            .source("boolean").boxed("Boolean").primitive().closedSet().build();

    public static final ValueType WHOLE_NUMBER = ValueType.of("WHOLE_NUMBER")
            .label("Whole number").group(BASICS)
            .source("int").boxed("Integer").primitive().bounded().build();

    public static final ValueType DECIMAL_NUMBER = ValueType.of("DECIMAL_NUMBER")
            .label("Decimal number").group(BASICS)
            .source("double").boxed("Double").primitive().bounded().build();

    public static final ValueType CHARACTER = ValueType.of("CHARACTER")
            .label("Character").group(BASICS)
            .source("char").boxed("Character").primitive().build();

    public static final ValueType COLOR = ValueType.of("COLOR")
            .label("Colour").group(BASICS).source("java.awt.Color").build();

    public static final ValueType DATE = ValueType.of("DATE")
            .label("Date").group(WHEN).source("java.time.LocalDate").build();

    public static final ValueType TIME_OF_DAY = ValueType.of("TIME_OF_DAY")
            .label("Time of day").group(WHEN).source("java.time.LocalTime").build();

    public static final ValueType DURATION = ValueType.of("DURATION")
            .label("Duration").group(WHEN).source("java.time.Duration").build();

    /**
     * This plugin's vocabulary, in the order a menu should offer it: the literals a bot mostly counts, flags
     * and labels with, then the three time types.
     */
    public static final ValueCatalog CATALOG = ValueCatalog.builder()
            .add(TEXT, codec(JdkText::text, s -> s, BasicsValueTypes::quote, BasicsValueTypes::unquote))
            .add(YES_NO, codec(JdkText::flag, b -> Boolean.toString(b), b -> Boolean.toString(b),
                    java -> "true".equals(java) || "false".equals(java)
                            ? Optional.of(java) : Optional.empty()))
            .add(WHOLE_NUMBER, codec(JdkText::whole, i -> Integer.toString(i), i -> Integer.toString(i),
                    java -> integer(java)
                            .filter(n -> n == n.intValue())
                            .map(n -> Integer.toString(n.intValue()))))
            .add(DECIMAL_NUMBER, codec(JdkText::decimal, d -> Double.toString(d), d -> Double.toString(d),
                    java -> {
                        try {
                            return Optional.of(Double.toString(Double.parseDouble(java)));
                        } catch (NumberFormatException notANumber) {
                            return Optional.empty();
                        }
                    }))
            .add(CHARACTER, codec(JdkText::letter, String::valueOf, BasicsValueTypes::quoteChar,
                    java -> unquoteChar(java).map(String::valueOf)))
            .add(COLOR, codec(JdkText::color, JdkText::spellColor, BasicsValueTypes::colorLiteral,
                    BasicsValueTypes::wireOfColor))
            .add(DATE, codec(JdkText::date, LocalDate::toString, BasicsValueTypes::dateLiteral,
                    BasicsValueTypes::wireOfDate))
            .add(TIME_OF_DAY, codec(JdkText::time, LocalTime::toString, BasicsValueTypes::timeLiteral,
                    BasicsValueTypes::wireOfTime))
            .add(DURATION, codec(JdkText::duration,
                    d -> JdkText.spellDuration(d.toMillis()),
                    d -> "java.time.Duration.ofMillis(" + d.toMillis() + "L)",
                    BasicsValueTypes::wireOfDuration))
            .build();

    // ---- literals -------------------------------------------------------------------------------------
    //
    // Every one of these writes the *parsed* value, never the text — `new java.awt.Color(255, 0, 0)` rather
    // than `Color.decode("#FF0000")`, `LocalDate.of(2026, 8, 26)` rather than `LocalDate.parse(…)`. A
    // generated file therefore holds no expression that can throw at class initialisation, which is what it
    // means for a bot never to fail to start because of its own configuration file.

    /**
     * A Java string literal, escaped by the toolkit's {@code Source} rather than by a fourth hand-rolled
     * escaper. The SDK's {@code LiteralWriter.quote} deliberately duplicates it — a library half may not
     * name a widget kit — and this class has no such constraint: it is plugin code, and the toolkit is this
     * plugin's own {@code compile}-scope dependency.
     */
    private static String quote(String text) {
        return Source.string(text).source();
    }

    /** A Java char literal. Separate from {@link #quote} because what must be escaped differs by position. */
    private static String quoteChar(char c) {
        return Source.character(c).source();
    }

    /** The components, not {@code Color.decode(…)}: {@code decode} parses at class-init and can throw. */
    private static String colorLiteral(Color c) {
        return "new java.awt.Color(%d, %d, %d)".formatted(c.getRed(), c.getGreen(), c.getBlue());
    }

    private static String dateLiteral(LocalDate d) {
        return "java.time.LocalDate.of(%d, %d, %d)".formatted(d.getYear(), d.getMonthValue(),
                d.getDayOfMonth());
    }

    /** Seconds included always, so a stored {@code 07:30:15} is not silently truncated to the minute. */
    private static String timeLiteral(LocalTime t) {
        return "java.time.LocalTime.of(%d, %d, %d)".formatted(t.getHour(), t.getMinute(), t.getSecond());
    }

    // ---- plumbing -------------------------------------------------------------------------------------

    private static <T> ValueCodec<T> codec(Function<String, T> parse, Function<T, String> store,
                                           Function<T, String> literal) {
        return codec(parse, store, literal, java -> Optional.empty());
    }

    /**
     * The four-argument form, for a type that can also read its own {@link ValueCodec#literal} back.
     *
     * <p>Every one of the nine can, and none of them does it by chance: the inverse is written next to the
     * literal it undoes, in the same expression, because the two are one fact and a pair that drifts is a
     * value the editor writes and then refuses to edit.
     */
    private static <T> ValueCodec<T> codec(Function<String, T> parse, Function<T, String> store,
                                           Function<T, String> literal,
                                           Function<String, Optional<String>> wireOfLiteral) {
        return new ValueCodec<>() {
            @Override
            public T parse(String wire) {
                return parse.apply(wire);
            }

            @Override
            public String store(T value) {
                return store.apply(value);
            }

            @Override
            public String literal(T value) {
                return literal.apply(value);
            }

            @Override
            public Optional<String> wireOfLiteral(String javaSource) {
                return javaSource == null ? Optional.empty() : wireOfLiteral.apply(javaSource.strip());
            }
        };
    }

    // ---- the inverses ---------------------------------------------------------------------------------
    //
    // Each one recognises exactly what its literal writes, and declines everything else — including a
    // spelling that means the same thing. `Duration.ofSeconds(3)` is not what this plugin emits, so it
    // reads as a hand-written initialiser and is shown read-only rather than silently rewritten into the
    // canonical form the moment somebody opens the window. Recognising more would be recognising *other
    // people's* Java, which is a parser's job and not a codec's.

    /** Unwraps a call this plugin writes, {@code Prefix(args)} → {@code args}, or empty. */
    private static Optional<String> arguments(String java, String prefix) {
        if (!java.startsWith(prefix) || !java.endsWith(")")) return Optional.empty();
        return Optional.of(java.substring(prefix.length(), java.length() - 1).strip());
    }

    /** The content of a Java string literal, or empty when the source is not one. */
    private static Optional<String> unquote(String java) {
        if (java.length() < 2 || java.charAt(0) != '"' || !java.endsWith("\"")) return Optional.empty();
        String body = java.substring(1, java.length() - 1);
        StringBuilder out = new StringBuilder(body.length());
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c != '\\') {
                if (c == '"') return Optional.empty();        // an unescaped quote: not one literal
                out.append(c);
                continue;
            }
            if (++i >= body.length()) return Optional.empty();
            char escaped = body.charAt(i);
            switch (escaped) {
                case 'n' -> out.append('\n');
                case 't' -> out.append('\t');
                case 'r' -> out.append('\r');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 's' -> out.append(' ');
                case '0' -> out.append('\0');
                case '\\', '"', '\'' -> out.append(escaped);
                default -> {
                    return Optional.empty();                  // \\u…, an octal escape: not ours to read
                }
            }
        }
        return Optional.of(out.toString());
    }

    /** The character of a Java char literal, or empty. The escapes are {@link #quoteChar}'s own. */
    private static Optional<Character> unquoteChar(String java) {
        if (java.length() < 3 || java.charAt(0) != '\'' || !java.endsWith("'")) return Optional.empty();
        String body = java.substring(1, java.length() - 1);
        if (body.length() == 1 && body.charAt(0) != '\\') return Optional.of(body.charAt(0));
        if (body.length() != 2 || body.charAt(0) != '\\') return Optional.empty();
        return switch (body.charAt(1)) {
            case 'n' -> Optional.of('\n');
            case 't' -> Optional.of('\t');
            case 'r' -> Optional.of('\r');
            case 'b' -> Optional.of('\b');
            case 'f' -> Optional.of('\f');
            case 's' -> Optional.of(' ');
            case '0' -> Optional.of('\0');
            case '\\', '\'', '"' -> Optional.of(body.charAt(1));
            default -> Optional.empty();
        };
    }

    /** The {@code n} of an integer literal, tolerating a trailing {@code L}, or empty. */
    private static Optional<Long> integer(String java) {
        String digits = java.endsWith("L") || java.endsWith("l")
                ? java.substring(0, java.length() - 1) : java;
        try {
            return Optional.of(Long.parseLong(digits.strip()));
        } catch (NumberFormatException notANumber) {
            return Optional.empty();
        }
    }

    /** The comma-separated integer arguments of a call, exactly {@code count} of them, or empty. */
    private static Optional<int[]> integers(String arguments, int count) {
        String[] parts = arguments.split(",", -1);
        if (parts.length != count) return Optional.empty();
        int[] out = new int[count];
        for (int i = 0; i < count; i++) {
            Optional<Long> value = integer(parts[i].strip());
            if (value.isEmpty() || value.get() != value.get().intValue()) return Optional.empty();
            out[i] = value.get().intValue();
        }
        return Optional.of(out);
    }

    private static Optional<String> wireOfColor(String java) {
        return arguments(java, "new java.awt.Color(")
                .flatMap(args -> integers(args, 3))
                .map(rgb -> JdkText.spellColor(new Color(rgb[0], rgb[1], rgb[2])));
    }

    private static Optional<String> wireOfDate(String java) {
        return arguments(java, "java.time.LocalDate.of(")
                .flatMap(args -> integers(args, 3))
                .flatMap(ymd -> {
                    try {
                        return Optional.of(LocalDate.of(ymd[0], ymd[1], ymd[2]).toString());
                    } catch (RuntimeException impossibleDate) {
                        return Optional.empty();
                    }
                });
    }

    private static Optional<String> wireOfTime(String java) {
        return arguments(java, "java.time.LocalTime.of(")
                .flatMap(args -> integers(args, 3))
                .flatMap(hms -> {
                    try {
                        return Optional.of(LocalTime.of(hms[0], hms[1], hms[2]).toString());
                    } catch (RuntimeException impossibleTime) {
                        return Optional.empty();
                    }
                });
    }

    private static Optional<String> wireOfDuration(String java) {
        return arguments(java, "java.time.Duration.ofMillis(")
                .flatMap(BasicsValueTypes::integer)
                .map(JdkText::spellDuration);
    }
}
