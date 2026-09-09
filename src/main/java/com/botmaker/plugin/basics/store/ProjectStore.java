package com.botmaker.plugin.basics.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The one file a project's plugins store their data in, <b>sectioned by owning plugin id</b>.
 *
 * <pre>{@code
 * {
 *   "schemaVersion": 3,
 *   "plugins": {
 *     "com.botmaker.basics": { "variables": [ … ] },
 *     "com.botmaker.sdk":    { "activities": [ … ], "flow": { … } }
 *   }
 * }
 * }</pre>
 *
 * <h2>Why one file with sections rather than a file per plugin</h2>
 *
 * <p>A project is one thing a user copies, commits and sends to somebody. A file per plugin makes that a set
 * of files whose members can go missing individually, and makes "open this project" mean "find everything
 * that might describe it". One file also means one atomic write and one place a merge conflict happens.
 *
 * <p>The sections are what stop that being a shared mutable pot. <b>A plugin reads and writes its own
 * section and nothing else</b>, and {@link #withSection} copies every other section through untouched — so a
 * plugin that is not installed today does not lose its data when a project is saved by an editor that has
 * never heard of it. That is the same judgement as {@code ValueType.unknown}: never destroy a user's data
 * because a jar is missing.
 *
 * <h2>Reading and writing it is this plugin's API, and that is the point of the module</h2>
 *
 * <p>It is <em>not</em> the platform's. The contract cannot hold it — a bot's classpath has no contract —
 * and the SDK holding it is what made storing project data a privilege of plugin #1. Any plugin that wants
 * to keep data with a project depends on {@code botmaker-plugin-basics} the way {@code botmaker-sdk} does
 * and calls this class; the SDK's own activities, flow and presets move into a section of this file rather
 * than beside it.
 *
 * <h2>A file with no sections is a legacy file, and every section answers its root</h2>
 *
 * <p>{@code activities.json} predates plugins entirely: it holds {@code activities}, {@code variables} and
 * {@code flow} at the top level, with no owner recorded anywhere. So {@link #section} falls back to the
 * whole document for <em>every</em> id, which is exactly the state such a project is in — one unsectioned
 * document that every reader sees. The alternative is telling every project ever written that it has no
 * data, and the fallback costs nothing once a project has been saved in the sectioned shape.
 *
 * <p>The fallback names no plugin id, deliberately: an id in this class would be the mechanism knowing its
 * first two customers, which is the shape of privilege this module exists to remove.
 *
 * <h2>Nothing here throws while reading</h2>
 *
 * <p>A missing file, an unreadable one and a section that is not an object are ordinary states with an
 * answer. <b>A bot does not fail to start because of its own configuration file.</b> Writing is the other
 * half and does throw: a save that silently did not happen is the one failure a user cannot see.
 */
public final class ProjectStore {

    /** Where a project's store sits on a bot's classpath. */
    public static final String RESOURCE = "/activities.json";

    /** Its name inside a project's resources directory, for whoever is writing it. */
    public static final String FILE = "activities.json";

    /** The object the sections live under. */
    public static final String SECTIONS = "plugins";

    /**
     * This plugin's own id, and the section its variables live in.
     *
     * <p>Here rather than on {@code BasicsPlugin} because a running bot reads this section and cannot load
     * that class — it names the plugin contract, which is {@code provided} and so absent from a bot.
     * {@code BasicsPlugin.ID} is defined as this constant, so the id is written once.
     */
    public static final String BASICS_ID = "com.botmaker.basics";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final ProjectStore EMPTY = new ProjectStore(MAPPER.createObjectNode());

    private static ProjectStore current;

    private final JsonNode root;

    private ProjectStore(JsonNode root) {
        this.root = root;
    }

    // ---- loading ----------------------------------------------------------------------------------------

    /**
     * This bot's own store, parsed once and held for the life of the process.
     *
     * <p>A bot's configuration cannot change while it runs — the editor writes the file and the bot is
     * restarted — so re-reading it would only make two ticks able to disagree.
     */
    public static synchronized ProjectStore current() {
        if (current == null) current = load(RESOURCE);
        return current;
    }

    /** Test seam: make {@code store} what {@link #current()} answers, or {@code null} to read again. */
    public static synchronized void use(ProjectStore store) {
        current = store;
        ProjectValues.use(null);
    }

    /**
     * The store at {@code resource} on the classpath, or an empty one.
     *
     * <p>The two failures are told apart on purpose. A <b>missing</b> resource is silent: a project that has
     * never had an activity or a variable added is not misconfigured. A resource that exists and will not
     * parse says so once, because that is a real mistake somebody can act on — and it still yields an empty
     * store rather than throwing, since a bot that refuses to start tells its user far less than one that
     * starts and reports empty configuration.
     */
    public static ProjectStore load(String resource) {
        try (InputStream in = ProjectStore.class.getResourceAsStream(resource)) {
            if (in == null) return EMPTY;
            return new ProjectStore(MAPPER.readTree(in));
        } catch (Exception e) {
            System.err.println("[store] " + resource + " could not be read (" + e.getMessage()
                    + "); running with no configuration");
            return EMPTY;
        }
    }

    /** The store in {@code json}, or an empty one — the seam a test and the flow loader read through. */
    public static ProjectStore of(String json) {
        if (json == null || json.isBlank()) return EMPTY;
        try {
            return new ProjectStore(MAPPER.readTree(json));
        } catch (Exception e) {
            System.err.println("[store] the project store could not be parsed (" + e.getMessage() + ")");
            return EMPTY;
        }
    }

    /**
     * The store in {@code file}, or an empty one when it is absent or unreadable — the editor's side.
     *
     * <p>Absent is not an error here either: a project is created before anything is stored in it.
     */
    public static ProjectStore read(Path file) {
        if (file == null || !Files.isRegularFile(file)) return EMPTY;
        try {
            return of(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("[store] " + file + " could not be read (" + e.getMessage() + ")");
            return EMPTY;
        }
    }

    /** A store with nothing in it. Every section answers a missing node. */
    public static ProjectStore empty() {
        return EMPTY;
    }

    // ---- sections ---------------------------------------------------------------------------------------

    /**
     * The data {@code pluginId} owns — never {@code null}, and a missing node when it owns none.
     *
     * <p>For a file written in the sectioned shape this is {@code plugins.<pluginId>} and nothing else. For
     * a file that predates sections it is the <b>whole document</b>, for every id: see the class note.
     */
    public JsonNode section(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) return MAPPER.missingNode();
        JsonNode sections = root.path(SECTIONS);
        if (!sections.isObject()) return root;      // a legacy file: one unsectioned document.
        return sections.path(pluginId.trim());
    }

    /** Whether this store records owners at all — false for a file written before sections existed. */
    public boolean isSectioned() {
        return root.path(SECTIONS).isObject();
    }

    /** The plugin ids this store holds a section for, in file order. Empty for a legacy file. */
    public List<String> sections() {
        JsonNode sections = root.path(SECTIONS);
        if (!sections.isObject()) return List.of();
        List<String> out = new ArrayList<>();
        sections.fieldNames().forEachRemaining(out::add);
        return List.copyOf(out);
    }

    /**
     * This store with {@code pluginId}'s section replaced — every other section carried through untouched.
     *
     * <p>A new store rather than a mutation, because the thing being replaced is what a plugin just decided
     * and the thing being carried is what other plugins decided earlier; an in-place write makes the second
     * depend on nobody having kept a reference to the first.
     *
     * <p><b>Writing a legacy file's section converts it.</b> The unsectioned top level is left exactly where
     * it is — a reader that still expects it goes on working — and the sectioned form is added beside it, so
     * the conversion cannot lose data by being half-finished. Phase 6 of the plan is what removes the old
     * keys, in the pass that gives the activities an owner.
     */
    public ProjectStore withSection(String pluginId, JsonNode data) {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("a section belongs to a plugin id");
        }
        ObjectNode copy = root.isObject() ? ((ObjectNode) root).deepCopy() : MAPPER.createObjectNode();
        ObjectNode sections = copy.path(SECTIONS).isObject()
                ? (ObjectNode) copy.get(SECTIONS)
                : copy.putObject(SECTIONS);
        sections.set(pluginId.trim(), data == null ? MAPPER.createObjectNode() : data.deepCopy());
        return new ProjectStore(copy);
    }

    // ---- writing ----------------------------------------------------------------------------------------

    /** The whole store as JSON, indented the way the file is stored. */
    public String json() {
        try {
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            // Unreachable for a tree built by this class; reported rather than swallowed if it ever is not.
            System.err.println("[store] the project store could not be written (" + e.getMessage() + ")");
            return "{}";
        }
    }

    /**
     * Writes the store to {@code file}, creating the directory if it is missing.
     *
     * <p>Throws, unlike everything above. A read that fails has a defensible answer — the defaults — and a
     * write that fails has none: the user pressed save and their data is not there.
     */
    public void write(Path file) throws IOException {
        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(file, json() + "\n", StandardCharsets.UTF_8);
    }

    /** This store's whole document. For a reader that owns the file's schema rather than one section. */
    public JsonNode root() {
        return root;
    }
}
