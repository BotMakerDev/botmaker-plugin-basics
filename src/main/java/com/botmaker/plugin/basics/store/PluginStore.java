package com.botmaker.plugin.basics.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * A plugin's own state as its own records — one line to write, one to read.
 *
 * <pre>{@code
 * record CaptureTargets(String window, List<String> images) {}
 *
 * PluginStore store = PluginStore.of(project.resourcesRoot(), MyPlugin.ID);
 * store.write("capture", new CaptureTargets("Game", List.of("hp.png")));
 * CaptureTargets targets = store.read("capture", CaptureTargets.class).orElse(EMPTY);
 * }</pre>
 *
 * <p>The file is {@link PluginData}'s — {@code plugins/<id prefix>/<last segment>/capture.json} — so
 * nothing here invents a location, and a plugin that also reads the raw tree gets the same bytes.
 *
 * <h2>Why typed, when {@link PluginData} already stores JSON</h2>
 *
 * <p>Because everything stored so far has been hand-walked {@code JsonNode}: a dozen lines to read a shape
 * the plugin already has a record for, and a dozen more to write it, each with its own idea of what a
 * missing field means. That is the code every plugin author would write next, and writing it once here is
 * the difference between a store a plugin uses and a store a plugin re-implements.
 *
 * <h2>What a failure reads as</h2>
 *
 * <p><b>Reading is total and writing throws</b>, which is {@link PluginData}'s rule and is kept here for
 * the same reason: a plugin asking for state it has never stored is the ordinary first call, and a save
 * that silently did not happen is the one failure a user cannot see.
 *
 * <ul>
 *   <li>no file, unparseable JSON, or JSON that is not the record's shape → {@link Optional#empty()}
 *   <li>a field the record does not declare → ignored ({@code FAIL_ON_UNKNOWN_PROPERTIES} off), so a
 *       plugin that drops a field can still read files an older version of itself wrote
 *   <li>a field the record declares and the file omits → the type's own default ({@code null}, {@code 0})
 * </ul>
 *
 * <p><b>Bot-safe</b>: a running bot reads its plugins' state through
 * {@link Settings#forPlugin(String)}, which is this class with the project directory replaced by the
 * classpath. Neither names the contract, so neither needs a host.
 */
public final class PluginStore {

    /**
     * One mapper for the whole class, configured once.
     *
     * <p>{@code FAIL_ON_UNKNOWN_PROPERTIES} off is the only setting, and it is the compatibility rule
     * above rather than a preference. Nothing else is turned on: a store that accepted comments, or
     * single quotes, or trailing commas would be writing a dialect that only it can read.
     */
    static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final PluginData data;

    private PluginStore(PluginData data) {
        this.data = data;
    }

    /**
     * The store for {@code pluginId} under a project's {@code src/main/resources}.
     *
     * @param resourcesDir the project's resources root — the same argument {@link PluginData#of} takes
     * @param pluginId     the plugin's registered id, which is what names its folder
     */
    public static PluginStore of(Path resourcesDir, String pluginId) {
        return new PluginStore(PluginData.of(resourcesDir, pluginId));
    }

    /** The store over an already-resolved {@link PluginData}, for a caller that holds one. */
    public static PluginStore of(PluginData data) {
        return new PluginStore(data);
    }

    /** The plugin whose files this reads and writes. */
    public String pluginId() {
        return data.pluginId();
    }

    /** The folder tree underneath, for a plugin that also wants the raw document. */
    public PluginData data() {
        return data;
    }

    /** What is stored under {@code name}, as {@code type}, or empty for anything that did not work. */
    public <T> Optional<T> read(String name, Class<T> type) {
        return convert(data.read(name).root(), type);
    }

    /**
     * The list stored under {@code name}, or empty.
     *
     * <p>A document that is not an array reads as empty rather than as one element: a plugin that stored a
     * single object under a name it now reads as a list has changed its own shape, and guessing which of
     * the two it meant would put an invented element in front of a user.
     */
    public <T> List<T> readAll(String name, Class<T> type) {
        JsonNode root = data.read(name).root();
        if (!root.isArray()) {
            return List.of();
        }
        List<T> out = new java.util.ArrayList<>(root.size());
        for (JsonNode element : root) {
            convert(element, type).ifPresent(out::add);
        }
        return List.copyOf(out);
    }

    /**
     * Stores {@code value} under {@code name}, creating the folders on the way.
     *
     * @throws IOException if the file could not be written — see the class note on why this throws
     */
    public void write(String name, Object value) throws IOException {
        data.write(name, MAPPER.valueToTree(value));
    }

    /** Whether anything is stored under {@code name} yet. */
    public boolean has(String name) {
        return data.has(name);
    }

    /**
     * The one conversion both reads go through: a shape mismatch is empty, never an exception.
     *
     * <p><b>An empty document reads as empty too</b>, and that is a decision rather than an accident: a
     * missing file, an unparseable one and {@code {}} all arrive here as an empty object
     * ({@code ProjectStore}'s own total answer), and converting that would hand a plugin a record full of
     * nulls that it cannot tell from one it stored. A plugin with genuinely nothing to say stores nothing.
     */
    static <T> Optional<T> convert(JsonNode node, Class<T> type) {
        if (node == null || node.isNull() || node.isMissingNode()
                || (node.isContainerNode() && node.isEmpty())) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(MAPPER.treeToValue(node, type));
        } catch (JsonProcessingException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
