package com.botmaker.plugin.basics.managed;

import com.botmaker.plugin.api.managed.Managed;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Hands a bot's {@code @Managed} values to the plugins that own them, so no bot writes an {@code install()}.
 *
 * <pre>{@code
 * public final class Gamebot extends Bot {
 *
 *     public static void main(String[] args) {
 *         run(Gamebot.class, Gamebot::goHome, Sdk.class);
 *     }
 * }
 * }</pre>
 *
 * <h2>What this replaced</h2>
 *
 * <p>Until 2026-09-21 the file a plugin shipped carried an {@code install()} that the bot's {@code main}
 * called by hand — {@code Flows.use(flow()); Source.set(captureSource());} and one more line in
 * {@code main} for every plugin after the first. Forgetting that line is a bot that runs with no flow and
 * no error, which is the same class of failure the whole {@code @Managed} migration exists to end: a fact
 * stated twice, where the second statement can silently go missing.
 *
 * <p>The bot still <b>names</b> each plugin's values class, because that is a fact only the bot has and one
 * javac can check — the same shape as {@code PaletteCatalog.of(Mouse.class, …)}. What it no longer does is
 * say what to <em>do</em> with them.
 *
 * <h2>Why there is no manifest and no scanning</h2>
 *
 * <p>A {@code META-INF/services} entry, or a package this walked by convention, would be <i>a second
 * statement of a fact the file already carries</i> — the reason {@code ActivityLoader} refused one. A class
 * literal in {@code main} is the statement, it is one line, and a typo in it does not compile.
 *
 * <h2>Claim ordering, which is sound without anybody arranging it</h2>
 *
 * <p>A plugin claims its ids from the static initialiser of a type its own {@code @Managed} methods return.
 * {@link #install} invokes a method <em>before</em> dispatching what it returned, and invoking it links that
 * return type — so the claim is always registered by the time its value arrives, with no load order for
 * anyone to get wrong and no registry file to keep in step.
 */
public final class ManagedValues {

    /**
     * Who takes each id's value, keyed by the id the annotation spells.
     *
     * <p>Concurrent because a bot's {@code main} thread installs while a plugin's own static initialiser may
     * still be claiming on another — and because being wrong here is a value silently going nowhere.
     */
    private static final Map<String, Consumer<Object>> SINKS = new ConcurrentHashMap<>();

    private ManagedValues() {}

    /**
     * Says that {@code sink} takes the value of every {@code @Managed(id)} method a bot declares.
     *
     * <p>Called by a plugin's <em>library</em> half, not by a bot. The last claim on an id wins, which is
     * what makes calling this from a static initialiser safe to reach twice.
     *
     * <p>The value arrives as an {@link Object} because the id is the pairing and a sink knows its own type.
     * A sink handed something it cannot cast is a bug in the plugin that claimed the id, and {@link #install}
     * reports it by name rather than taking the bot down with it.
     */
    public static void claim(String id, Consumer<Object> sink) {
        if (id == null || id.isBlank() || sink == null) {
            return;
        }
        SINKS.put(id.strip(), sink);
    }

    /**
     * Invokes every {@code @Managed} method on each class and gives what it returns to whoever claimed its id.
     *
     * <p>Only a {@code public static} method taking no arguments is a value: that is {@code @Managed}'s own
     * rule, and the same one the editor applies before it will rewrite one. <b>{@code @Managed} on a type is
     * not installed</b> — it marks a class of constants the bot names at its use sites ({@code Pictures.COLLECT}),
     * so there is nothing to hand anybody.
     *
     * <p><b>Nothing here throws.</b> An unclaimed id means that plugin is not on this bot's classpath, which
     * is an ordinary state; a method that throws is the bot author's own code failing. Both are one line on
     * {@code System.err} naming the class and the id, because the alternative — a value that silently never
     * arrives — is exactly the failure this class was written to end.
     *
     * <p>Methods are taken in name order. Reflection promises no order at all, and two bots installing the
     * same values in two different sequences would be a difference nobody wrote down.
     */
    public static void install(Class<?>... valueClasses) {
        if (valueClasses == null) {
            return;
        }
        for (Class<?> type : valueClasses) {
            if (type == null) {
                continue;
            }
            for (Method method : managedMethods(type)) {
                installOne(type, method);
            }
        }
    }

    /** Every {@code public static} no-argument {@code @Managed} method of {@code type}, in name order. */
    private static List<Method> managedMethods(Class<?> type) {
        List<Method> found = new ArrayList<>();
        Method[] declared;
        try {
            declared = type.getDeclaredMethods();
        } catch (RuntimeException | LinkageError unreadable) {
            // A values class naming a type this bot's classpath does not have. One sentence, and the other
            // plugins' values still install.
            System.err.println("[values] " + type.getName() + " could not be read: " + unreadable);
            return List.of();
        }
        for (Method method : declared) {
            if (!method.isAnnotationPresent(Managed.class)) {
                continue;
            }
            int modifiers = method.getModifiers();
            if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers)
                    || method.getParameterCount() != 0) {
                continue;
            }
            found.add(method);
        }
        found.sort(Comparator.comparing(Method::getName));
        return found;
    }

    private static void installOne(Class<?> type, Method method) {
        String id = method.getAnnotation(Managed.class).value();
        if (id == null || id.isBlank()) {
            return;
        }
        Object value;
        try {
            // Invoking links the declared return type, which is what registers a plugin's claim in time.
            value = method.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            System.err.println("[values] " + type.getSimpleName() + "." + method.getName()
                    + "() threw, so '" + id.strip() + "' is not installed: " + cause(failed));
            return;
        }
        Consumer<Object> sink = SINKS.get(id.strip());
        if (sink == null) {
            System.err.println("[values] nothing claims '" + id.strip() + "' — the plugin that reads it is "
                    + "not on this bot's classpath, so " + type.getSimpleName() + "." + method.getName()
                    + "() is ignored.");
            return;
        }
        try {
            sink.accept(value);
        } catch (RuntimeException | LinkageError refused) {
            System.err.println("[values] the plugin that claims '" + id.strip() + "' refused the value from "
                    + type.getSimpleName() + "." + method.getName() + "(): " + cause(refused));
        }
    }

    /** The exception a reflective call actually failed with, rather than the wrapper reflection adds. */
    private static Throwable cause(Throwable thrown) {
        return thrown.getCause() == null ? thrown : thrown.getCause();
    }
}
