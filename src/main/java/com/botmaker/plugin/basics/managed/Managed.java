package com.botmaker.plugin.basics.managed;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A value a plugin keeps up to date through its own window — <b>the declaration itself, in the bot's own
 * Java</b>.
 *
 * <pre>{@code
 * public final class Sdk {
 *
 *     @Managed("flow")
 *     public static Flow flow() {
 *         return Flow.of(List.of(...), List.of(...), "Collect", Flow.limits(1000, 1000));
 *     }
 * }
 * }</pre>
 *
 * <p>The file this sits in was <b>copied from the plugin when it was added</b>, and is yours from that
 * moment: nothing regenerates it. BotMaker rewrites the expression a {@code @Managed} method returns and
 * nothing else, so comments, helper methods and anything else you write around them survive untouched.
 *
 * <h2>What this replaced, and why it is not a JSON file any more</h2>
 *
 * <p>Until 2026-09-20 a plugin's own data lived in {@code plugins/<id prefix>/<last segment>/<name>.json}
 * and the bot read it back by name at run time. A name renamed in Java was then a silently empty value
 * three screens into a run, where the same rename here is a compile error — and a value in the bot's source
 * is one a developer with no BotMaker installed can read, grep, refactor and hand to a compiler. That is
 * the whole point of the change. {@code Settings} stays for a plugin's flags and its own files; the values
 * a plugin's window edits are methods now.
 *
 * <h2>The rules BotMaker applies to a method</h2>
 *
 * <p>All of them are about what the window can safely rewrite, and a method that breaks one is <b>shown
 * read-only with the reason</b> rather than refused: the bot still compiles, and the author is told why.
 *
 * <ul>
 *   <li><b>{@code public static}</b>, and taking no parameters. A value is not computed from arguments.
 *   <li><b>A return type some plugin registers</b> — one of the value types, a container over them, or a
 *       record the bot itself declares. An unregistered type reads as itself and is shown read-only.
 *   <li><b>A body that is exactly one {@code return <expression>;}</b>. Anything else — a local variable, a
 *       branch, a loop — is code the author wrote deliberately, and it is kept and never rewritten.
 * </ul>
 *
 * <h2>On a type instead: an open set</h2>
 *
 * <p>{@code @Managed} on a class says the plugin's window owns <em>every member of it</em>, so the code
 * canvas may not add to it, rename in it or delete from it. That is the shape for a set the user grows —
 * one constant per captured picture — where a method is the shape for a fixed value the plugin shipped a
 * declaration for. The difference is whether the rest of the bot names the parts: {@code Pictures.COLLECT}
 * is written at its use sites, and a flow's activities are not.
 *
 * <h2>Why it lives here and names nothing</h2>
 *
 * <p>This annotation is on a <b>bot's</b> classpath, so it may not name a {@code com.botmaker.plugin.api}
 * type: the contract is {@code provided} and absent from a bot. The id is a plain string for that reason,
 * and the plugin declares the same string on its side ({@code StudioPlugin.managedValues}) — which is what
 * pairs the two without either naming the other's types.
 *
 * @see com.botmaker.plugin.basics.params.Param the same idea for a value the bot's <em>user</em> changes
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Managed {

    /**
     * Which of the plugin's values this is — {@code "flow"}, {@code "capture"}, {@code "pictures"}.
     *
     * <p>Plugin-local: the plugin that shipped the file chose it and declares the same string. An id no
     * loaded plugin claims is an ordinary method, editable on the canvas like any other.
     */
    String value();
}
