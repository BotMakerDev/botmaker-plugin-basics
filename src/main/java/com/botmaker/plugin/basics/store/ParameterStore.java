package com.botmaker.plugin.basics.store;

import com.botmaker.plugin.api.ParameterEdit;
import com.botmaker.plugin.api.ParameterRow;
import com.botmaker.plugin.api.value.Range;
import com.botmaker.plugin.api.value.ValueCatalog;
import com.botmaker.plugin.api.value.ValueChoice;
import com.botmaker.plugin.api.value.Visibility;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * How a plugin stores and reads the parameters of one of its groups: rows in, rows out, coercion between.
 *
 * <p><b>This is the mechanism, and it is deliberately not a document.</b> The maintainer's call on
 * 2026-09-10: a plugin exposes parameters the way a bot reads them — through something shaped like
 * {@link Settings}, owned here rather than in Studio, so the standard is code a plugin calls instead of a
 * convention a plugin is asked to follow. It was written in the SDK first, as {@code SdkParameters}, and
 * generalised out of it the same day; plugin #1's last storage privilege went with the move.
 *
 * <p>A plugin gets parameters by declaring a {@code ParameterGroup}, holding one of these over its own
 * {@link PluginData}, and handing the host back what {@link #rows} answers. Nothing about the SDK is left
 * in it — the vocabulary is the contract's, and the value types are whatever catalog the owner passes.
 *
 * <h2>What is stored, and where</h2>
 *
 * <p>One file per plugin, {@code parameters.json} inside that plugin's own folder — see {@link PluginData}
 * for the tree and for why it replaced a single sectioned project file. The document is one array:
 *
 * <pre>{@code
 * {"parameters": [
 *   {"name": "restBetweenRuns", "group": "", "type": {"type": "DURATION", "shape": "ONE", "list": false},
 *    "value": ["90s"], "description": "", "category": "General", "visibility": "public",
 *    "options": [], "bounds": {"min": "", "max": ""}}
 * ]}
 * }</pre>
 *
 * <p><b>Every group the plugin owns shares that file, and a group only ever touches its own rows.</b> The
 * others are decoded, left alone and written back in place — the same rule the withdrawn sectioned file had
 * between <em>plugins</em>, which is still needed <em>within</em> one, because a plugin with two groups has
 * two windows writing one file.
 *
 * <p>A name is unique within a group and only there, which is what lets two plugins — and two groups of one
 * plugin — both offer a {@code timeout}.
 *
 * <h2>Coercion belongs to the editor, and this is the editor's side</h2>
 *
 * <p>The rules are Studio's own {@code ValueWire}, carried through the SDK's {@code SdkValues} and landing
 * here unchanged: <b>canonicalise</b> through the owning type's codec, so the editor shows the value the bot
 * will actually get; <b>clamp</b> to a declared {@link Range}, which is advice to a widget and a clamp when
 * a value is normalised, never a validation that can fail; <b>prune</b> a value to the options still on
 * offer, so a deleted choice stops being a stored one. That is why {@link #apply} answers the row it stored
 * rather than the row it was handed.
 *
 * <p><b>The catalog is the owner's own, and that is a real limit.</b> A plugin can only read the value types
 * it registered or compiled against, so a row whose type id nothing in {@code catalog} claims is left
 * untouched rather than canonicalised — which is exactly what {@link ValueCatalog#normalize} does for an
 * unknown id, and for the same reason: never rewrite a value you cannot read.
 *
 * <h2>It names the contract, so a bot never loads it</h2>
 *
 * <p>Which is why it is in {@code BasicsIsBotSafeTest}'s exemption list beside {@code BasicsPlugin}. The
 * reading half a bot does run is {@link ProjectValues#forPlugin}, which resolves the same file by id and
 * name and answers untyped text.
 */
public final class ParameterStore {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String ROWS = "parameters";
    private static final String GROUP = "group";

    private final PluginData data;
    private final String groupId;
    private final ValueCatalog catalog;

    /**
     * @param data    the owning plugin's data folder in the open project
     * @param groupId the {@code ParameterGroup} id this serves; every other id answers nothing
     * @param catalog the value types this plugin can read — its own, merged with whatever it depends on
     */
    public ParameterStore(PluginData data, String groupId, ValueCatalog catalog) {
        this.data = Objects.requireNonNull(data, "a parameter store belongs to a plugin's data");
        this.groupId = groupId == null ? "" : groupId.trim();
        this.catalog = catalog == null ? ValueCatalog.empty() : catalog;
    }

    /** The group this serves. */
    public String groupId() {
        return groupId;
    }

    // ---- the two verbs a host's window asks for ---------------------------------------------------------

    /**
     * The rows filed under {@code requested}, in the order the file holds them.
     *
     * <p>Answers nothing for an id this store does not own and nothing for a project that has never stored a
     * parameter — an absent file is what a new project is, not a reason to refuse to draw the window.
     */
    public List<ParameterRow> rows(String requested) {
        if (!groupId.equals(requested == null ? "" : requested.trim())) return List.of();
        List<ParameterRow> rows = new ArrayList<>();
        for (Entry entry : read()) {
            if (isMine(entry)) rows.add(entry.row);
        }
        return List.copyOf(rows);
    }

    /**
     * Stores {@code edit} and answers the row as stored, or empty when this store owns neither the group nor
     * a row of that name.
     *
     * <p>Empty is <em>not mine</em>, and it is deliberately not how a failed save reports itself: a host
     * reads empty as "leave the screen alone", so a write that could not happen throws instead, with the row
     * left as it was — the truth rather than a silent discard.
     */
    public Optional<ParameterRow> apply(ParameterEdit edit) {
        if (edit == null || !groupId.equals(edit.groupId())) return Optional.empty();
        return edit(edit.name(), row -> row.withValue(
                normalize(edit.value(), row.type(), row.options(), row.bounds())));
    }

    // ---- the declaration verbs, which are the owning plugin's own window's and nobody else's -------------

    /**
     * Declares a new parameter of {@code type}, seeded with that type's default value.
     *
     * <p>Empty when the name is blank or already taken <em>in this group</em>. It is also a generated field
     * name, so a name that is not a Java identifier is refused rather than stored and discovered at the next
     * build.
     */
    public Optional<ParameterRow> declare(String name, ValueChoice type) {
        String wanted = name == null ? "" : name.trim();
        if (!isIdentifier(wanted) || type == null) return Optional.empty();
        List<Entry> entries = read();
        if (indexOf(entries, wanted) >= 0) return Optional.empty();

        ParameterRow declared = ParameterRow.named(wanted, type).value(defaultValue(type)).build();
        entries.add(new Entry(groupId, declared));
        write(entries);
        return Optional.of(declared);
    }

    /** Removes the parameter {@code name} names. False when this group holds no such row. */
    public boolean remove(String name) {
        List<Entry> entries = read();
        int at = indexOf(entries, name);
        if (at < 0) return false;
        entries.remove(at);
        write(entries);
        return true;
    }

    /**
     * Renames a parameter, refusing a name that is blank, not an identifier, or already taken here.
     *
     * <p><b>The user's own source is not touched, and that is the honest behaviour rather than a gap.</b> A
     * bot reads a parameter as a field of a generated class, so a rename makes that field's old spelling
     * stop compiling — a readable error naming the line, at the moment the user next builds. Rewriting their
     * source from a settings window would be an edit they did not ask for in a file they own.
     */
    public Optional<ParameterRow> rename(String from, String to) {
        String wanted = to == null ? "" : to.trim();
        if (!isIdentifier(wanted)) return Optional.empty();
        List<Entry> entries = read();
        int at = indexOf(entries, from);
        if (at < 0) return Optional.empty();
        ParameterRow held = entries.get(at).row;
        if (!held.name().equals(wanted) && indexOf(entries, wanted) >= 0) return Optional.empty();

        ParameterRow renamed = copy(held, wanted, held.type(), held.value(), held.options(), held.bounds());
        entries.set(at, new Entry(groupId, renamed));
        write(entries);
        return Optional.of(renamed);
    }

    /**
     * Retypes a parameter: its value resets to the new type's default, its bounds are dropped, and its
     * declared options survive only a change of <em>shape</em>.
     *
     * <p>The value does not carry across, deliberately — a date is not a number, and pretending otherwise
     * stores something the editor would have to explain away on the next open. Options survive one of and
     * many of over the same base type, because that is a question about how many may be picked rather than
     * about what may be picked; they do not survive a change of base type, whose values they no longer are.
     */
    public Optional<ParameterRow> retype(String name, ValueChoice type) {
        if (type == null) return Optional.empty();
        return edit(name, row -> {
            // Compared by id, never by identity: a ValueType's identity is its persisted id, and two plugin
            // classloaders each holding their own copy of a class would make == mean nothing.
            List<String> options =
                    type.hasOptions() && type.type().equals(row.type().type()) ? row.options() : List.of();
            return copy(row, row.name(), type, defaultValue(type), options, Range.NONE);
        });
    }

    /** Replaces the declared choices, pruning the stored value to what is still on offer. */
    public Optional<ParameterRow> setOptions(String name, List<String> options) {
        return edit(name, row -> {
            List<String> declared = normalizeOptions(options, row.type(), row.bounds());
            return row.toBuilder()
                    .options(declared)
                    .value(normalize(row.value(), row.type(), declared, row.bounds()))
                    .build();
        });
    }

    /** Declares a range, clamping the stored value into it. */
    public Optional<ParameterRow> setBounds(String name, Range bounds) {
        return edit(name, row -> {
            Range declared = bounds == null ? Range.NONE : bounds;
            return row.toBuilder()
                    .bounds(declared)
                    .value(normalize(row.value(), row.type(), row.options(), declared))
                    .build();
        });
    }

    /** Files the parameter under a category of the owning group's — the rail inside the section. */
    public Optional<ParameterRow> setCategory(String name, String category) {
        return edit(name, row -> row.toBuilder().category(category == null ? "" : category).build());
    }

    /** Says whether whoever runs the bot is offered this parameter at all. */
    public Optional<ParameterRow> setVisibility(String name, Visibility visibility) {
        return edit(name, row -> row.toBuilder().visibility(visibility).build());
    }

    /** The sentence a user reads instead of the field name. */
    public Optional<ParameterRow> setDescription(String name, String description) {
        return edit(name, row -> row.toBuilder().description(description == null ? "" : description).build());
    }

    // ---- the coercion rules -----------------------------------------------------------------------------

    /**
     * A fresh value of {@code type}: the type's own default for a single value, nothing for a list.
     *
     * <p>An empty list rather than one empty item, because a list a user has not filled in has no items —
     * seeding one would put a blank row in every new list-shaped parameter.
     */
    public List<String> defaultValue(ValueChoice type) {
        if (type == null || type.isList()) return List.of();
        return List.of(catalog.defaultItem(type.type().id()));
    }

    /**
     * The declared choices as the type actually stores them: each canonicalised, duplicates dropped, order
     * kept. Empty when the shape declares no set.
     *
     * <p>Every choice is itself a value of the base type, so it goes through the same normaliser a value
     * does — otherwise the radio button is labelled with one spelling and the stored value matches neither.
     */
    public List<String> normalizeOptions(List<String> options, ValueChoice type, Range bounds) {
        if (type == null || !type.hasOptions() || options == null) return List.of();
        return options.stream()
                .filter(Objects::nonNull)
                .map(option -> item(option, type, bounds == null ? Range.NONE : bounds))
                .distinct()
                .toList();
    }

    /**
     * A stored value, canonicalised, clamped and constrained to what is still on offer.
     *
     * @param value   the stored wire form, one entry per item
     * @param type    what kind of value, and in what shape
     * @param options the declared choices, for an option-bearing shape
     * @param bounds  the declared range, for a bounded number
     */
    public List<String> normalize(List<String> value, ValueChoice type, List<String> options, Range bounds) {
        if (type == null) return value == null ? List.of() : List.copyOf(value);
        List<String> safe = value == null ? List.of() : value.stream().filter(Objects::nonNull).toList();
        List<String> choices = normalizeOptions(options, type, bounds);
        Range range = bounds == null ? Range.NONE : bounds;

        if (!type.isList()) {
            return List.of(constrain(item(safe.isEmpty() ? null : safe.getFirst(), type, range), choices));
        }
        // An option-bearing list follows the declaration order, not the file's: two projects that picked the
        // same choices in a different order must write the same line, or a diff shows a change nobody made.
        if (!choices.isEmpty()) {
            LinkedHashSet<String> chosen = safe.stream()
                    .map(each -> item(each, type, range))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            return choices.stream().filter(chosen::contains).toList();
        }
        return safe.stream().map(each -> item(each, type, range)).toList();
    }

    /** {@code value} if it is still on offer, else the first thing that is. Unconstrained when nothing is. */
    private static String constrain(String value, List<String> choices) {
        if (choices.isEmpty()) return value;
        return choices.contains(value) ? value : choices.getFirst();
    }

    /**
     * One item, canonicalised by its own codec and then clamped to the declared range.
     *
     * <p>Clamping runs <em>after</em> the codec and is re-canonicalised afterwards, so the two cannot
     * disagree about the spelling of the result — a clamp that produced {@code "5"} for a decimal would
     * otherwise store text its own reader normalises to {@code "5.0"} on the very next read.
     */
    private String item(String wire, ValueChoice type, Range bounds) {
        String id = type.type().id();
        String canonical = catalog.normalize(id, wire);
        if (!type.type().bounded() || bounds.isEmpty()) return canonical;
        return catalog.normalize(id, clamp(canonical, bounds));
    }

    private static String clamp(String canonical, Range bounds) {
        double value = number(canonical, 0.0);
        double min = number(bounds.min(), Double.NEGATIVE_INFINITY);
        double max = number(bounds.max(), Double.POSITIVE_INFINITY);
        double clamped = Math.max(min, Math.min(max, value));
        // Written back through the plain double spelling and read by the type's own codec, which is what
        // turns it into an int again for a whole number.
        return clamped == value ? canonical : Double.toString(clamped);
    }

    private static double number(String text, double fallback) {
        if (text == null || text.isBlank()) return fallback;
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ---- the file ---------------------------------------------------------------------------------------

    /** One stored row and the group it belongs to — this store's, or another group of the same plugin's. */
    private record Entry(String group, ParameterRow row) {

        boolean mine(String groupId) {
            return groupId.equals(group);
        }
    }

    private boolean isMine(Entry entry) {
        return entry.mine(groupId);
    }

    /**
     * Read, change one row, write, answer it — every verb above but {@link #declare} and {@link #remove}.
     *
     * <p>One place, because the alternative is nine copies of <i>read the file, find the row, put it back</i>,
     * and the copy that eventually forgets to write is the one nobody notices: the screen shows the change
     * either way, and only the next open disagrees.
     */
    private Optional<ParameterRow> edit(String name, java.util.function.UnaryOperator<ParameterRow> change) {
        List<Entry> entries = read();
        int at = indexOf(entries, name);
        if (at < 0) return Optional.empty();

        ParameterRow changed = change.apply(entries.get(at).row);
        if (changed == null) return Optional.empty();
        entries.set(at, new Entry(groupId, changed));
        write(entries);
        return Optional.of(changed);
    }

    /** The index of the row {@code name} names within this group, or -1. */
    private int indexOf(List<Entry> entries, String name) {
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            if (isMine(entry) && entry.row.name().equals(name)) return i;
        }
        return -1;
    }

    /**
     * Every row in the plugin's file, this group's and its siblings', in file order.
     *
     * <p>A row of another group is decoded like any other and written back untouched. Decoding it is safe
     * because every component of a row is contract vocabulary, and keeping it is what stops one window of a
     * two-group plugin saving the other window's rows away.
     */
    private List<Entry> read() {
        List<Entry> entries = new ArrayList<>();
        for (JsonNode node : data.read(PluginData.PARAMETERS).root().path(ROWS)) {
            if (!node.isObject()) continue;
            entries.add(new Entry(node.path(GROUP).asText(""), rowOf(node)));
        }
        return entries;
    }

    private void write(List<Entry> entries) {
        ObjectNode document = MAPPER.createObjectNode();
        ArrayNode rows = document.putArray(ROWS);
        for (Entry entry : entries) rows.add(nodeOf(entry));
        try {
            data.write(PluginData.PARAMETERS, document);
        } catch (IOException e) {
            // A save that did not happen must not read as a save that did. The host contains this and leaves
            // the row as it was, which is what the user's next look at the window should show them.
            throw new UncheckedIOException(
                    "could not store the parameter into " + data.file(PluginData.PARAMETERS), e);
        }
    }

    /**
     * One stored object as a row.
     *
     * <p>Every read is total, because a hand-edited file must open: an unknown type id becomes
     * {@code ValueType.unknown} through the catalog, an unknown shape reads as one free value, and an
     * unknown visibility reads as the contract's own fallback.
     */
    private ParameterRow rowOf(JsonNode node) {
        JsonNode type = node.path("type");
        ValueChoice choice = ValueChoice.fromWire(catalog,
                type.isObject() ? type.path("type").asText("") : type.asText(""),
                type.isObject() ? text(type.path("shape")) : null,
                type.isObject() && type.hasNonNull("list") ? type.path("list").asBoolean() : null);
        ParameterRow.Builder row = ParameterRow.named(node.path("name").asText(""), choice)
                .value(strings(node.path("value")))
                .description(node.path("description").asText(""))
                .category(node.path("category").asText(""))
                .options(strings(node.path("options")))
                .bounds(new Range(text(node.path("bounds").path("min")),
                        text(node.path("bounds").path("max"))));
        // A file that records no visibility keeps the builder's own default, which is PUBLIC. Reading a
        // missing field through fromId would answer EDITOR_ONLY and quietly hide the row from the Runner.
        String visibility = text(node.path("visibility"));
        if (visibility != null && !visibility.isBlank()) row.visibility(Visibility.fromId(visibility));
        return row.build();
    }

    /**
     * One row with a new name, type, value, options and bounds — everything else carried across.
     *
     * <p>A helper rather than {@link ParameterRow#toBuilder()} because the builder's name and type are
     * final: a row is a plugin-constructed value, so the two components that identify it are settled when it
     * is named rather than editable afterwards.
     */
    private static ParameterRow copy(ParameterRow row, String name, ValueChoice type, List<String> value,
                                     List<String> options, Range bounds) {
        return ParameterRow.named(name, type)
                .value(value)
                .description(row.description())
                .category(row.category())
                .visibility(row.visibility())
                .options(options)
                .bounds(bounds)
                .build();
    }

    /**
     * One row as a stored object.
     *
     * <p>Both spellings of the type are written — the shape and the older {@code list} boolean — so a reader
     * that predates the shape axis still sees a list as a list. It costs one field and it is what
     * {@link ValueChoice#fromWire} is total against.
     */
    private static ObjectNode nodeOf(Entry entry) {
        ParameterRow row = entry.row;
        ObjectNode node = MAPPER.createObjectNode();
        node.put("name", row.name());
        node.put(GROUP, entry.group);
        ObjectNode type = node.putObject("type");
        type.put("type", row.type().type().id());
        type.put("shape", row.type().shape().name());
        type.put("list", row.type().isList());
        put(node.putArray("value"), row.value());
        node.put("description", row.description());
        node.put("category", row.category());
        node.put("visibility", row.visibility().id());
        put(node.putArray("options"), row.options());
        ObjectNode bounds = node.putObject("bounds");
        bounds.put("min", row.bounds().min() == null ? "" : row.bounds().min());
        bounds.put("max", row.bounds().max() == null ? "" : row.bounds().max());
        return node;
    }

    private static void put(ArrayNode array, List<String> values) {
        for (String value : values) array.add(value);
    }

    private static List<String> strings(JsonNode array) {
        if (array == null || !array.isArray()) return List.of();
        List<String> out = new ArrayList<>(array.size());
        for (JsonNode element : array) out.add(element.asText(""));
        return List.copyOf(out);
    }

    /** A stored string, or {@code null} for a field the file does not have — which {@link Range} reads. */
    private static String text(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asText("");
    }

    /**
     * A valid Java identifier, because a parameter's name is a generated field's.
     *
     * <p>Checked here rather than at the widget, so that every path into the file gets it — a dialog, a
     * paste, a future import.
     */
    private static boolean isIdentifier(String name) {
        if (name.isEmpty() || !Character.isJavaIdentifierStart(name.charAt(0))) return false;
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) return false;
        }
        return true;
    }
}
