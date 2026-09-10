package com.botmaker.plugin.basics.store;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Where a plugin's data lives inside a project, and how it names a file of its own.
 *
 * <pre>{@code
 * src/main/resources/plugins/com.botmaker/sdk/parameters.json     ← com.botmaker.sdk
 * src/main/resources/plugins/com.botmaker/basics/parameters.json  ← com.botmaker.basics
 * src/main/resources/plugins/com.example/discord/webhooks.json    ← com.example.discord
 * }</pre>
 *
 * <h2>A folder per author, a folder per plugin</h2>
 *
 * <p>The tree is derived from the plugin id alone — everything before the last dot is the author's folder,
 * the last segment is the plugin's — so it needs no new metadata and cannot disagree with anything. It is
 * unique for the same reason the registry is: an entry file is named after the id, so git refuses a second
 * claim on one.
 *
 * <p><b>This replaces one project file sectioned by plugin id</b> (2026-09-10, the maintainer's call). Two
 * of that design's four defences survive the swap and are why the tree is better rather than merely
 * different: <em>no code reads or rewrites another plugin's bytes at all</em>, so a plugin that is not
 * installed cannot lose its data by construction rather than by a carry-through somebody has to remember;
 * and a merge conflict lands in one plugin's file instead of in one shared document. Two are lost and are
 * stated rather than glossed: the single atomic write goes — a crash mid-save can leave one plugin saved
 * and another not — and a project is a directory to copy rather than a file.
 *
 * <h2>A bot enumerates nothing</h2>
 *
 * <p>Which is what would have made a tree awkward inside a jar, where listing a classpath directory is not
 * a thing a plugin can rely on. It never has to: {@link #resource} turns a plugin id and a file name into
 * one resource path, and a plugin knows both. Nothing here scans.
 *
 * <h2>Files on demand, and one file per name</h2>
 *
 * <p>{@link #write} creates the folders and the file the first time something is stored, so a plugin never
 * has to prepare a project it has not been used in yet. The name is {@linkplain #normalize normalised}
 * first — trimmed, lower-cased, stripped of a {@code .json} somebody wrote themselves — so
 * {@code "Parameters"}, {@code "parameters"} and {@code "parameters.json"} are one file rather than three
 * that shadow each other depending on which reader asked.
 *
 * <p>This class is bot-safe: it names no contract type and no JavaFX, because a running bot resolves its own
 * files through it.
 */
public final class PluginData {

    /** The one directory under a project's resources that holds every plugin's data. */
    public static final String ROOT = "plugins";

    /** The file a {@link ParameterStore} keeps its rows in. */
    public static final String PARAMETERS = "parameters";

    /**
     * This plugin's own id.
     *
     * <p>Here rather than on {@code BasicsPlugin} because a running bot reads this plugin's data and cannot
     * load that class — it names the plugin contract, which is {@code provided} and so absent from a bot.
     * {@code BasicsPlugin.ID} is defined as this constant, so the id is written once.
     */
    public static final String BASICS_ID = "com.botmaker.basics";

    private static final String SUFFIX = ".json";

    private final Path resourcesDir;
    private final String pluginId;

    private PluginData(Path resourcesDir, String pluginId) {
        this.resourcesDir = resourcesDir;
        this.pluginId = pluginId;
    }

    /**
     * The data of {@code pluginId} inside {@code resourcesDir} — a project's {@code src/main/resources}.
     *
     * <p>Nothing is read or created here. A handle over a project that has never held this plugin's data is
     * an ordinary thing to hold: every read answers empty and the first write makes the folders.
     */
    public static PluginData of(Path resourcesDir, String pluginId) {
        if (resourcesDir == null) throw new IllegalArgumentException("a project's resources directory is required");
        return new PluginData(resourcesDir, requireId(pluginId));
    }

    /** The plugin whose data this is. */
    public String pluginId() {
        return pluginId;
    }

    /** The folder this plugin's files live in, whether or not it exists yet. */
    public Path folder() {
        Path folder = resourcesDir.resolve(ROOT);
        for (String segment : segments(pluginId)) folder = folder.resolve(segment);
        return folder;
    }

    /** The file {@code name} names, whether or not it exists yet. */
    public Path file(String name) {
        return folder().resolve(normalize(name) + SUFFIX);
    }

    /**
     * The document stored under {@code name}, or an empty one.
     *
     * <p>Absent, unreadable and unparseable all read as empty, which is {@link ProjectStore}'s rule and the
     * reason it is the thing being returned: a plugin asking for data it has never stored is the ordinary
     * first call, not a failure.
     */
    public ProjectStore read(String name) {
        return ProjectStore.read(file(name));
    }

    /**
     * Stores {@code document} under {@code name}, creating the folders on the way.
     *
     * <p>Throws, unlike reading. A save that silently did not happen is the one failure a user cannot see.
     */
    public void write(String name, JsonNode document) throws IOException {
        ProjectStore.of(document).write(file(name));
    }

    /** Whether this plugin has stored anything under {@code name} yet. */
    public boolean has(String name) {
        return Files.isRegularFile(file(name));
    }

    /**
     * Where a bot finds {@code name} on its classpath — {@code /plugins/com.botmaker/sdk/parameters.json}.
     *
     * <p>Static and taking the id, because the bot side has no project directory and must not scan: it
     * resolves the one path it knows it wants and reads it, exactly as {@link ProjectStore#load} does.
     */
    public static String resource(String pluginId, String name) {
        StringBuilder path = new StringBuilder("/").append(ROOT);
        for (String segment : segments(requireId(pluginId))) path.append('/').append(segment);
        return path.append('/').append(normalize(name)).append(SUFFIX).toString();
    }

    /**
     * The folder segments {@code pluginId} maps to: the author, then the plugin.
     *
     * <p>An id with no dot in it has no author to name, so it becomes one folder rather than a folder
     * called something invented — {@code discord} is {@code plugins/discord}. A trailing dot is treated the
     * same way, because {@code com.example.} names no plugin either.
     */
    private static String[] segments(String pluginId) {
        int lastDot = pluginId.lastIndexOf('.');
        if (lastDot <= 0 || lastDot == pluginId.length() - 1) return new String[] {pluginId};
        return new String[] {pluginId.substring(0, lastDot), pluginId.substring(lastDot + 1)};
    }

    /**
     * The file name as it is actually spelled on disk: trimmed, lower-cased, without its extension, and with
     * anything that is not a letter, a digit, {@code -} or {@code _} replaced by {@code -}.
     *
     * <p>Total for anything that leaves a character behind, and a refusal for anything that does not — a
     * file has to be called something, and a blank name would silently become one shared file for every
     * caller that got it wrong. Lower-casing is what stops {@code Activities} and {@code activities} being
     * two files on Linux and one file that shadows itself on macOS.
     */
    public static String normalize(String name) {
        String trimmed = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        if (trimmed.endsWith(SUFFIX)) trimmed = trimmed.substring(0, trimmed.length() - SUFFIX.length());
        StringBuilder out = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            boolean plain = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '_';
            out.append(plain ? c : '-');
        }
        String normalized = out.toString();
        if (normalized.isBlank() || normalized.chars().allMatch(c -> c == '-')) {
            throw new IllegalArgumentException("a data file needs a name; \"" + name + "\" leaves none");
        }
        return normalized;
    }

    private static String requireId(String pluginId) {
        String trimmed = pluginId == null ? "" : pluginId.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("data belongs to a plugin id");
        return trimmed;
    }
}
