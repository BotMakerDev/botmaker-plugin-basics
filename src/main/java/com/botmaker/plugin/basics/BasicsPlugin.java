package com.botmaker.plugin.basics;

import com.botmaker.plugin.api.value.PluginType;
import com.botmaker.plugin.basics.values.BasicsTypes;
import com.botmaker.plugin.toolkit.AbstractStudioPlugin;

import java.util.List;

/**
 * BotMaker Basics — plugin #2, and the first plugin in this project that is not the SDK.
 *
 * <p><b>It owns the nine JDK value types</b> since 2026-09-09 — text, a flag, two numbers, a character,
 * a colour, a date, a time of day and a duration, declared in
 * {@link com.botmaker.plugin.basics.values.BasicsTypes}. They are nobody's vocabulary in particular and
 * were the SDK's only because the SDK was written first; the SDK keeps the eight that really are its own,
 * and the host reads both lists the same way.
 *
 * <p><b>That is all it owns, since 2026-09-28.</b> It held a store ({@code PluginData}, {@code Settings}) and
 * the bot-side {@code @Managed} runtime ({@code ManagedValues}) too. The store had no caller left — a plugin's
 * values are Java in the bot — and the runtime is the contract's, beside {@code @Managed}. So nothing here runs
 * in a bot, and the Jackson dependency the store needed is gone.
 *
 * <p><b>The id is the identity and it never changes.</b> {@code com.botmaker.basics} is what the plugin
 * registry refuses to admit twice; the Maven coordinate may move
 * under it. A project opened without this plugin installed keeps every value of a type declared here as raw
 * text, renders it read-only and never rewrites it — which is what makes an open vocabulary safe.
 *
 * <p><b>Nothing expensive belongs in this constructor.</b> {@code ServiceLoader} runs it while a project is
 * opening, whether or not any answer is wanted; {@code AbstractStudioPlugin}'s {@code build…} hooks run at
 * most once and only when the host asks. The same rule caught the SDK on 2026-09-05, where one field write
 * in the constructor linked JavaFX and made the plugin unconstructible on every headless host.
 */
public final class BasicsPlugin extends AbstractStudioPlugin {

    /** The registered plugin id; never change it. */
    public static final String ID = "com.botmaker.basics";

    /** What Studio shows in Manage Plugins. */
    public static final String NAME = "BotMaker Basics";

    public BasicsPlugin() {
        super(ID, NAME);
    }

    /**
     * The nine types, built at most once and only when a host asks — {@code AbstractStudioPlugin}'s hook,
     * never a field, because {@code ServiceLoader} runs the constructor while a project is opening.
     *
     * <p>Each one carries its own editor, so there is no {@code slotEditors()} here: an editor for a type
     * this plugin declares belongs beside the type, and this plugin overrides nobody else's.
     */
    @Override
    protected List<PluginType<?>> buildTypes() {
        return BasicsTypes.ALL;
    }
}
