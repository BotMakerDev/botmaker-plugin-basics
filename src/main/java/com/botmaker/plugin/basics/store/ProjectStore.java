package com.botmaker.plugin.basics.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * One JSON document a plugin keeps with a project, read totally and written whole.
 *
 * <p>Which document is {@link PluginData}'s business: a plugin's data is a folder of files named by that
 * plugin, and this class is what one of those files is read and written through. It knows no schema — what
 * the keys mean belongs to whoever stores them.
 *
 * <h2>It was one file sectioned by plugin id, and that is withdrawn</h2>
 *
 * <p>Until 2026-09-10 there was a single {@code activities.json} with a {@code plugins} object in it, a
 * section per owning plugin, and a {@code withSection} that copied every other section through so an editor
 * without a plugin installed could not save that plugin's data away. The maintainer's call replaced it with
 * the folder tree, and the reasoning is on {@link PluginData}: the carry-through becomes unnecessary rather
 * than merely correct, because no code opens another plugin's file at all.
 *
 * <p>Two things went with the sections. <b>There is no legacy fallback</b> — a file that predates the tree
 * is not read by anything here, so a project written before it holds data nothing loads. Nothing deletes
 * that file, so a converter is writable later; there is deliberately none now. And <b>no plugin id appears
 * in this class</b>, which was already the rule: an id here would be the mechanism knowing its first two
 * customers.
 *
 * <h2>Reading and writing it is this plugin's API, and that is the point of the module</h2>
 *
 * <p>It is <em>not</em> the platform's. The contract cannot hold it — a bot's classpath has no contract —
 * and the SDK holding it is what made storing project data a privilege of plugin #1. Any plugin that wants
 * to keep data with a project depends on {@code botmaker-plugin-basics} the way {@code botmaker-sdk} does.
 *
 * <h2>Nothing here throws while reading</h2>
 *
 * <p>A missing file, an unreadable one and a document that is not an object are ordinary states with an
 * answer. <b>A bot does not fail to start because of its own configuration file.</b> Writing is the other
 * half and does throw: a save that silently did not happen is the one failure a user cannot see.
 */
public final class ProjectStore {

    /**
     * Where the project file that predates the folder tree sits on a bot's classpath.
     *
     * <p>Still read, because the SDK's activities and flow are still in it: the readers that move are
     * phases 6c to 6f of the plan, and until then this is where a running bot finds them. Nothing writes a
     * plugin's <em>data</em> here any more — that is {@link PluginData}.
     */
    public static final String RESOURCE = "/activities.json";

    /** Its name inside a project's resources directory, for whoever is writing it. */
    public static final String FILE = "activities.json";

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

    /** A store with nothing in it. Every lookup answers a missing node. */
    public static ProjectStore empty() {
        return EMPTY;
    }

    /**
     * The store holding {@code document}, or an empty one for {@code null} — what {@link PluginData} writes
     * through.
     *
     * <p>The tree is copied rather than held, so a caller that goes on editing the node it handed over
     * cannot change what this store says it is.
     */
    public static ProjectStore of(JsonNode document) {
        return document == null || document.isMissingNode() ? EMPTY : new ProjectStore(document.deepCopy());
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
