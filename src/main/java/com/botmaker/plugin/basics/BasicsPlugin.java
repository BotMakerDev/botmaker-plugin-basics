package com.botmaker.plugin.basics;

import com.botmaker.plugin.api.value.ValueCatalog;
import com.botmaker.plugin.basics.store.ProjectStore;
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
 * <p><b>It owns the store too</b>, since the same day: {@code com.botmaker.plugin.basics.store} — the
 * {@link ProjectStore} a project's data lives in, sectioned by owning plugin id, and the
 * {@code Settings}/{@code ValueGrammar} pair a <em>running bot</em> reads its own parameters through. Both
 * arrived from {@code com.botmaker.plugin.toolkit.config}, where they had spent a day; a widget kit owns no
 * value types, so it could hold the mechanism only by promising never to use it.
 *
 * <p>What is left for a later phase is moving the SDK plugin's own activities, flow and presets into their
 * section of that file rather than beside it.
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

    /**
     * The registered plugin id. Stored in projects; never change it.
     *
     * <p>Defined in the bot half, because a running bot needs the same string to find its section of the
     * project store and cannot load this class — it names the contract, which is {@code provided} and so
     * absent from a bot's classpath.
     */
    public static final String ID = ProjectStore.BASICS_ID;

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
