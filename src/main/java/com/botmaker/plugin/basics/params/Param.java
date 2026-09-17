package com.botmaker.plugin.basics.params;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A field the person running this bot may change — <b>the declaration itself, in the bot's own Java</b>.
 *
 * <pre>{@code
 * public final class Parameters {
 *     @Param(description = "How long to rest between attempts")
 *     public static Duration restBetween = Duration.ofSeconds(3);
 *
 *     @Param(category = "Limits", min = "1", max = "50")
 *     public static int maxAttempts = 10;
 * }
 * }</pre>
 *
 * <p>The bot reads {@code Parameters.maxAttempts} — a field, so a misspelling is a compile error and the
 * type is the type. Studio reads the same fields off the syntax tree and draws the Parameters window from
 * them; editing a value there rewrites the initializer, and adding a row writes a new field.
 *
 * <h2>What this replaced, and why it is not a name any more</h2>
 *
 * <p>Until 2026-09-17 a user parameter was a row in a plugin's JSON file, read back by name
 * ({@code Settings.load("maxAttempts", int.class)}). The name was a string on both sides, so a typo
 * compiled and answered the type's fallback, the declaration lived where the author could not see it, and
 * the editor had to own a vocabulary of parameter kinds. {@code Settings.load} stays — it is never-delete,
 * and a <em>plugin's</em> own rows are still read that way — but a <b>user</b> parameter is a field now.
 *
 * <h2>The rules Studio applies</h2>
 *
 * <p>All of them are about what a value cell can safely edit, and a field that breaks one is <b>shown
 * read-only</b> rather than refused: the bot still compiles, and the author is told why the cell is grey.
 *
 * <ul>
 *   <li><b>{@code public static}</b>, and not {@code final} if the runner is ever to override it. A
 *       {@code final} field is read-only in the window; a non-{@code public} one is not a parameter at all.
 *   <li><b>A type some plugin registers</b> — one of the seventeen ({@code Duration}, {@code int},
 *       {@code Rect}, …), a {@code List<T>} of one, or an {@code enum}, whose constants become the choices.
 *       An unregistered type reads as itself and is shown read-only.
 *   <li><b>An initializer the type's editor understands</b>: a literal, or the constructor/factory call
 *       that type's own {@code ValueCodec} emits. A computed initializer (a method call, an expression over
 *       another field) is kept, shown, and never rewritten.
 * </ul>
 *
 * <h2>Why it lives here and names nothing</h2>
 *
 * <p>This annotation is on a <b>bot's</b> classpath, so it may not name a {@code com.botmaker.plugin.api}
 * type: the contract is {@code provided} and absent from a bot. That is why {@link #visibility} is a string
 * rather than the contract's {@code Visibility}, and why bounds and choices are strings rather than typed
 * values — the value's own grammar parses them, exactly as it parses what the user types into the cell.
 *
 * <p>It is in {@code botmaker-plugin-basics} rather than in the SDK because a parameter is not about
 * automating a game, for the same reason the nine JDK value types moved here on 2026-09-09. Any plugin's
 * bots get it, and a bot that uses no SDK activity still declares parameters.
 *
 * @see com.botmaker.plugin.basics.store.Settings a plugin's own rows, which are still read by name
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Param {

    /**
     * The Parameters window only. The default, and what anything unrecognised is read as.
     *
     * <p>Constants rather than two string literals in Studio and two more in whatever reads a bot: an
     * annotation may declare them, and the side that defines the vocabulary is the side that should spell
     * it.
     */
    String EDITOR = "editor";

    /** The Runner too: the person starting a run changes it without opening the project. */
    String PUBLIC = "public";

    /**
     * The rail heading this parameter appears under, free text, {@code ""} for the section's default group.
     *
     * <p>Free text and not an enumeration: the categories were the SDK plugin's declared list until this
     * annotation existed ({@code Timing}, {@code Targets}, {@code Vision}, {@code Input}, {@code Limits},
     * {@code Debug}), which is a vocabulary — and the contract grows capabilities, never vocabularies. The
     * window lists the distinct strings it finds.
     */
    String category() default "";

    /** One line under the field name in the window. The field's own javadoc is not read; say it here. */
    String description() default "";

    /**
     * Who may see and change it: {@code "editor"} (the default — the Parameters window only) or
     * {@code "public"} (also the Runner, where the person running the bot changes it before a run).
     *
     * <p>A string because the contract's {@code Visibility} enum is off a bot's classpath. Anything else is
     * read as {@code "editor"}, which is the answer that shows a parameter to fewer people rather than more.
     */
    String visibility() default EDITOR;

    /**
     * Inclusive lower bound, as the value's own grammar spells it ({@code "1"}, {@code "PT0.5S"}).
     *
     * <p>{@code ""} is no bound. A bound the grammar cannot parse is ignored and reported beside the row,
     * never enforced half-way: a cell that refuses a value for a reason it cannot explain is worse than one
     * that accepts it.
     */
    String min() default "";

    /** Inclusive upper bound, same spelling as {@link #min}. */
    String max() default "";

    /**
     * The only values offered, as the grammar spells them — a closed choice for a type that is not an enum.
     *
     * <p>Empty means the type's own editor decides. For an {@code enum} field this is unnecessary: the
     * constants are the choices, and anything named here is merged with them.
     */
    String[] options() default {};
}
