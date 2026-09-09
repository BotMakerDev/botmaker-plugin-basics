package com.botmaker.plugin.basics;

import com.botmaker.plugin.api.value.ValueCatalog;
import com.botmaker.plugin.basics.values.BasicsValueTypes;
import com.botmaker.plugin.toolkit.AbstractStudioPlugin;

/**
 * BotMaker Basics — plugin #2, and the first plugin in this project that is not the SDK.
 *
 * <p><b>It owns the nine JDK value types</b> since 2026-09-09 — {@code TEXT}, {@code YES_NO},
 * {@code WHOLE_NUMBER}, {@code DECIMAL_NUMBER}, {@code CHARACTER}, {@code COLOR}, {@code DATE},
 * {@code TIME_OF_DAY}, {@code DURATION}, registered in
 * {@link com.botmaker.plugin.basics.values.BasicsValueTypes}. They are nobody's vocabulary in particular
 * and were the SDK's only because the SDK was written first; the SDK keeps the eight that really are its
 * own and merges these in like any host merging any plugin's.
 *
 * <p><b>What it will own next</b>, in the order the plan lands it:
 *
 * <ul>
 *   <li>{@code Settings} and {@code ValueGrammar}: how a <em>running bot</em> reads its own parameters;</li>
 *   <li>the project store those values are written into, whose read/write API is this module's and is used
 *       by other plugins — the SDK plugin stores its activities, flow and presets through it.</li>
 * </ul>
 *
 * <p><b>The id is the identity and it never changes.</b> {@code com.botmaker.basics} is what a project's
 * stored data refers to and what the plugin registry refuses to admit twice; the Maven coordinate may move
 * under it. A project opened without this plugin installed keeps every value of a type declared here as raw
 * text, renders it read-only and declines to emit it — {@link com.botmaker.plugin.api.value.ValueType}'s
 * unknown state, which is what makes an open vocabulary safe.
 *
 * <p><b>Nothing expensive belongs in this constructor.</b> {@code ServiceLoader} runs it while a project is
 * opening, whether or not any answer is wanted; {@code AbstractStudioPlugin}'s {@code build…} hooks run at
 * most once and only when the host asks. The same rule caught the SDK on 2026-09-05, where one field write
 * in the constructor linked JavaFX and made the plugin unconstructible on every headless host.
 */
public final class BasicsPlugin extends AbstractStudioPlugin {

    /** The registered plugin id. Stored in projects; never change it. */
    public static final String ID = "com.botmaker.basics";

    /** What Studio shows in Manage Plugins. */
    public static final String NAME = "BotMaker Basics";

    public BasicsPlugin() {
        super(ID, NAME);
    }

    /**
     * The nine types, built at most once and only when a host asks — {@code AbstractStudioPlugin}'s hook,
     * never a field, because {@code ServiceLoader} runs the constructor while a project is opening.
     */
    @Override
    protected ValueCatalog buildValueTypes() {
        return BasicsValueTypes.CATALOG;
    }
}
