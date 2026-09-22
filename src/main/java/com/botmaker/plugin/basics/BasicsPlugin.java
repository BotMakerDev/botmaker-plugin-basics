package com.botmaker.plugin.basics;

import com.botmaker.plugin.api.value.PluginType;
import com.botmaker.plugin.basics.store.PluginData;
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
 * <p><b>It owns the store too</b>, since the same day: {@code com.botmaker.plugin.basics.store} — the
 * {@link PluginData} tree a project's plugins keep their files in, the
 * {@code Settings}/{@code ValueGrammar} pair a <em>running bot</em> reads its own settings through. They
 * arrived from {@code com.botmaker.plugin.toolkit.config}, where they had spent a day; a widget kit owns no
 * value types, so it could hold the mechanism only by promising never to use it.
 *
 * <p><b>A third class stood beside them from 2026-09-10 to 2026-09-22 and is deleted</b>:
 * {@code ParameterStore}, which <em>any</em> plugin was to declare parameters through. Nothing ever called
 * its {@code declare}, so what the host read back was a pre-2026-09-17 project's JSON and nothing else. A
 * parameter is a {@code @Param} field in the bot's own Java now — including a plugin's own, in the file the
 * plugin ships — so there is no rows file and no store for one.
 *
 * <p>What is left for a later phase is moving the SDK plugin's own activities, flow and presets into files
 * of their own in that tree.
 *
 * <p><b>The id is the identity and it never changes.</b> {@code com.botmaker.basics} is what a project's
 * stored data refers to and what the plugin registry refuses to admit twice; the Maven coordinate may move
 * under it. A project opened without this plugin installed keeps every value of a type declared here as raw
 * text, renders it read-only and never rewrites it — which is what makes an open vocabulary safe.
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
     * <p>Defined in the bot half, because a running bot needs the same string to find this plugin's own
     * folder and cannot load this class — it names the contract, which is {@code provided} and so absent
     * from a bot's classpath.
     */
    public static final String ID = PluginData.BASICS_ID;

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
