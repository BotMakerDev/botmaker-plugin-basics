package com.botmaker.plugin.basics.store;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What a bot reads a <b>plugin's own stored state</b> through.
 *
 * <pre>{@code
 * record CaptureTargets(String window, List<String> images) {}
 *
 * CaptureTargets targets = Settings.forPlugin(MyPlugin.ID)
 *         .read("capture", CaptureTargets.class)
 *         .orElse(CaptureTargets.NONE);
 * }</pre>
 *
 * <h2>What this class was, and what took each part of it</h2>
 *
 * <p>It was how a bot read its <em>parameters</em>: {@code Settings.load("minHealth", int.class)} against
 * {@code activities.json}, with a {@code ValueGrammar} per plugin supplying the parsers. Both halves of that
 * are gone, and neither was replaced by something of the same shape:
 *
 * <ul>
 *   <li><b>A user parameter is a {@code @}{@link com.botmaker.plugin.basics.params.Param} field in the
 *       bot's own Java</b> (2026-09-17). A field, so a misspelling is a compile error and the type is the
 *       type — which is exactly what a table of total answers keyed by string could not give. The whole
 *       point of {@code load} was to make a missing name survivable; a field cannot be missing.</li>
 *   <li><b>An activity's enable flag is part of the flow</b> (2026-09-21), which is a {@code Flow} value in
 *       the bot's own Java. {@code Settings.enabled("Mining")} read it out of {@code activities.json}; that
 *       file does not exist any more, so the method could only ever have answered {@code false}.</li>
 * </ul>
 *
 * <p>{@code load}, {@code loadAll}, {@code enabled}, {@code declares} and {@code use} went with them, and so
 * did {@code ValueGrammar}, {@code BasicsGrammar} and the {@link java.util.ServiceLoader} that found them.
 * Deleting rather than deprecating is deliberate: a kept method whose only data source has been removed
 * answers a fallback for every call, which is worse than a compile error naming the line.
 *
 * <h2>Only read. Never write.</h2>
 *
 * <p>There is no {@code write}. A plugin's file has one author — the plugin's editor half, through
 * {@link PluginData} — and a bot that could write back would be a second author racing it whenever the
 * editor is open while a bot runs. A bot's own scratch state is a file of the bot's own; it is not this.
 *
 * <h2>Every answer is total</h2>
 *
 * <p>Absent, unparseable and shaped-wrong all read as empty, which is {@link ProjectStore}'s rule. A plugin
 * asking for data it has never stored is the ordinary first call, not a failure.
 */
public final class Settings {

    private Settings() {}

    /**
     * A plugin's own stored state, as the plugin's own records — the bot side of {@link PluginStore}.
     *
     * <pre>{@code
     * record CaptureTargets(String window, List<String> images) {}
     *
     * CaptureTargets targets = Settings.forPlugin(MyPlugin.ID)
     *         .read("capture", CaptureTargets.class)
     *         .orElse(CaptureTargets.NONE);
     * }</pre>
     *
     * <p>Same file, same mapper, same rules as the editor's side: absent, unparseable and shaped-wrong all
     * read as empty. The difference is where it reads from — <b>the classpath, by one resolved path</b>,
     * because a bot has no project directory and enumerates nothing.
     *
     * <p><b>This is not how a bot reads a user parameter.</b> A user parameter is a {@code @Param} field in
     * the bot's own Java; this is a plugin's state, which the bot reads only if the plugin's runtime half
     * gives it a reason to.
     */
    public static Plugin forPlugin(String pluginId) {
        return new Plugin(pluginId);
    }

    /**
     * One plugin's stored files, read off this bot's classpath.
     *
     * <p>A handle rather than static methods taking an id, so a caller names the plugin once — a bot that
     * repeated the id at every call would eventually repeat it wrongly, and a misspelled id reads as empty
     * exactly like a plugin that has stored nothing.
     */
    public static final class Plugin {

        private final String pluginId;

        private Plugin(String pluginId) {
            this.pluginId = pluginId;
        }

        /** The plugin whose files this reads. */
        public String pluginId() {
            return pluginId;
        }

        /** What the plugin stored under {@code name}, as {@code type}, or empty. */
        public <T> Optional<T> read(String name, Class<T> type) {
            return PluginStore.convert(document(name), type);
        }

        /** The list the plugin stored under {@code name}, or empty — a non-array document reads as empty. */
        public <T> List<T> readAll(String name, Class<T> type) {
            JsonNode root = document(name);
            if (root == null || !root.isArray()) {
                return List.of();
            }
            List<T> out = new ArrayList<>(root.size());
            for (JsonNode element : root) {
                PluginStore.convert(element, type).ifPresent(out::add);
            }
            return List.copyOf(out);
        }

        private JsonNode document(String name) {
            return ProjectStore.load(PluginData.resource(pluginId, name)).root();
        }
    }

}
