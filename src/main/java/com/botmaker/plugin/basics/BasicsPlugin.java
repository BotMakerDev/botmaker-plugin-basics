package com.botmaker.plugin.basics;

import com.botmaker.plugin.api.DeclaredPlugin;
import com.botmaker.plugin.api.StudioPlugin;
import com.botmaker.plugin.basics.values.BasicsTypes;

/**
 * BotMaker Basics — plugin #2, and the first plugin in this project that is not the SDK.
 *
 * <p><b>It owns the JDK value types</b> since 2026-09-09 — text, a flag, two numbers, a character, a colour,
 * the time types — declared in {@link BasicsTypes}, each with its own editor, so there is no
 * {@code slotEditors()} here: this plugin overrides nobody else's editor. They are nobody's vocabulary in
 * particular and were the SDK's only because the SDK was written first; the SDK keeps the ones that really
 * are its own, and the host reads both lists the same way.
 *
 * <p><b>That is all it owns, since 2026-09-28.</b> It held a store ({@code PluginData}, {@code Settings}) and
 * the bot-side {@code @Managed} runtime ({@code ManagedValues}) too. The store had no caller left — a plugin's
 * values are Java in the bot — and the runtime is the contract's, beside {@code @Managed}. So nothing here runs
 * in a bot, and the Jackson dependency the store needed is gone.
 *
 * <p><b>The id is the identity and it never changes.</b> {@code com.botmaker.basics} is what the plugin
 * registry refuses to admit twice; the Maven coordinate may move under it. A project opened without this
 * plugin installed keeps every value of a type declared here as written, renders it read-only and never
 * rewrites it — which is what makes an open vocabulary safe.
 *
 * <p><b>The type list is behind a supplier.</b> {@code ServiceLoader} runs this constructor while a project is
 * opening, and on headless hosts too; the list is built only when the host asks for it.
 */
public final class BasicsPlugin extends DeclaredPlugin {

    /** The registered plugin id; never change it. */
    public static final String ID = "com.botmaker.basics";

    /** What Studio shows in Manage Plugins. */
    public static final String NAME = "BotMaker Basics";

    public BasicsPlugin() {
        super(StudioPlugin.id(ID).named(NAME)
                .types(() -> BasicsTypes.ALL));
    }
}
