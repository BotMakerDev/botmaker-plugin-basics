package com.botmaker.plugin.basics.store;

import com.botmaker.plugin.api.parameters.ParameterEdit;
import com.botmaker.plugin.api.parameters.ParameterRow;
import com.botmaker.plugin.api.value.Range;
import com.botmaker.plugin.api.value.ValueCatalog;
import com.botmaker.plugin.api.value.ValueForm;
import com.botmaker.plugin.api.value.ValueType;
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
 * <p><b>These are a plugin's own rows, not the user's</b> (2026-09-17). A user parameter is a
 * {@code @Param} static field in the bot's own Java, read and written by the host off the syntax tree; what
 * belongs here is what the <em>plugin</em> declares about itself — an activity's enable flag, a capture
 * target. So the declaration verbs below are called by the owning plugin, from its own code, and the only
 * thing that reaches them from outside is a value ({@link #apply}). The host's way of asking for a row went
 * with {@code StudioPlugin.parameterDeclared}.
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
        return edit(edit.name(), held -> {
            ParameterRow row = held.row();
            return entry(row, normalize(wiresOf(row.form(), edit.value()),
                    row.form(), row.options(), row.bounds()));
        });
    }

    // declared(ParameterDeclaration) stood here until 2026-09-17, reconciling a row the *host* wanted against
    // the row this store held. It went with the contract method it implemented: a user parameter is a @Param
    // field in the bot's own Java now, and the host declares one by editing the syntax tree. The verbs below
    // are unchanged and stay public — a plugin declares its own rows by calling them, which is what they were
    // the implementation of all along. What is gone is the wire form for asking from outside.

    /** The row this group holds under {@code name}, or empty. */
    public Optional<ParameterRow> current(String name) {
        List<Entry> entries = read();
        int at = indexOf(entries, name);
        return at < 0 ? Optional.empty() : Optional.of(entries.get(at).row);
    }

    // ---- the declaration verbs, which are the owning plugin's own window's and nobody else's -------------

    /**
     * Declares a new parameter of {@code form}, seeded with that form's default value.
     *
     * <p>Empty when the name is blank or already taken <em>in this group</em>. It is also a generated field
     * name, so a name that is not a Java identifier is refused rather than stored and discovered at the next
     * build.
     */
    public Optional<ParameterRow> declare(String name, ValueForm form) {
        String wanted = name == null ? "" : name.trim();
        if (!isIdentifier(wanted) || form == null) return Optional.empty();
        List<Entry> entries = read();
        if (indexOf(entries, wanted) >= 0) return Optional.empty();

        Entry declared = entry(ParameterRow.named(wanted, form).build(), defaultValue(form));
        entries.add(declared);
        write(entries);
        return Optional.of(declared.row());
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
        Entry was = entries.get(at);
        ParameterRow held = was.row();
        if (!held.name().equals(wanted) && indexOf(entries, wanted) >= 0) return Optional.empty();

        Entry renamed = entry(copy(held, wanted, held.form(), held.options(), held.bounds()), was.wires());
        entries.set(at, renamed);
        write(entries);
        return Optional.of(renamed.row());
    }

    /**
     * Retypes a parameter: its value resets to the new form's default, its bounds are dropped, and its
     * declared options survive only a change of <em>container</em>.
     *
     * <p>The value does not carry across, deliberately — a date is not a number, and pretending otherwise
     * stores something the editor would have to explain away on the next open. Options survive one of and
     * many of over the same leaf type, because that is a question about how many may be picked rather than
     * about what may be picked; they do not survive a change of leaf, whose values they no longer are.
     */
    public Optional<ParameterRow> retype(String name, ValueForm form) {
        if (form == null) return Optional.empty();
        return edit(name, held -> {
            ParameterRow row = held.row();
            // Compared by id, never by identity: a ValueType's identity is its persisted id, and two plugin
            // classloaders each holding their own copy of a class would make == mean nothing.
            ValueType leaf = form.leaf();
            List<String> options =
                    leaf != null && leaf.equals(row.form().leaf()) ? row.options() : List.of();
            return entry(copy(row, row.name(), form, options, Range.NONE), defaultValue(form));
        });
    }

    /** Replaces the declared choices, pruning the stored value to what is still on offer. */
    public Optional<ParameterRow> setOptions(String name, List<String> options) {
        return edit(name, held -> {
            ParameterRow row = held.row();
            List<String> declared = normalizeOptions(options, row.form(), row.bounds());
            return entry(row.toBuilder().options(declared).build(),
                    normalize(held.wires(), row.form(), declared, row.bounds()));
        });
    }

    /** Declares a range, clamping the stored value into it. */
    public Optional<ParameterRow> setBounds(String name, Range bounds) {
        return edit(name, held -> {
            ParameterRow row = held.row();
            Range declared = bounds == null ? Range.NONE : bounds;
            return entry(row.toBuilder().bounds(declared).build(),
                    normalize(held.wires(), row.form(), row.options(), declared));
        });
    }

    /** Files the parameter under a category of the owning group's — the rail inside the section. */
    public Optional<ParameterRow> setCategory(String name, String category) {
        return edit(name, held ->
                held.with(held.row().toBuilder().category(category == null ? "" : category).build()));
    }

    /** Says whether whoever runs the bot is offered this parameter at all. */
    public Optional<ParameterRow> setVisibility(String name, Visibility visibility) {
        return edit(name, held -> held.with(held.row().toBuilder().visibility(visibility).build()));
    }

    /** The sentence a user reads instead of the field name. */
    public Optional<ParameterRow> setDescription(String name, String description) {
        return edit(name, held ->
                held.with(held.row().toBuilder().description(description == null ? "" : description).build()));
    }

    // ---- the coercion rules -----------------------------------------------------------------------------

    /**
     * A fresh value of {@code form}: the leaf's own default for a single value, nothing for a list.
     *
     * <p>An empty list rather than one empty item, because a list a user has not filled in has no items —
     * seeding one would put a blank row in every new list parameter.
     */
    public List<String> defaultValue(ValueForm form) {
        if (form == null || !(form instanceof ValueForm.Leaf leaf)) return List.of();
        return List.of(catalog.defaultItem(leaf.type().id()));
    }

    /**
     * The declared choices as the leaf actually stores them: each canonicalised, duplicates dropped, order
     * kept. Empty when there is no one leaf to be values of.
     *
     * <p>Every choice is itself a value of the leaf type, so it goes through the same normaliser a value
     * does — otherwise the radio button is labelled with one spelling and the stored value matches neither.
     *
     * <p><b>Whether a set is declared is not asked of the form.</b> It was asked of a shape until
     * 2026-09-20, and a set belongs to the declaration: a row with options has them, a row without does not,
     * and no type ever knew which.
     */
    public List<String> normalizeOptions(List<String> options, ValueForm form, Range bounds) {
        ValueType leaf = form == null ? null : form.leaf();
        if (leaf == null || options == null) return List.of();
        return options.stream()
                .filter(Objects::nonNull)
                .map(option -> item(option, leaf, bounds == null ? Range.NONE : bounds))
                .distinct()
                .toList();
    }

    /**
     * A stored value, canonicalised, clamped and constrained to what is still on offer.
     *
     * @param value   the stored wire form, one entry per item
     * @param form    what kind of value
     * @param options the declared choices, when the row declares a set
     * @param bounds  the declared range, for a bounded number
     */
    public List<String> normalize(List<String> value, ValueForm form, List<String> options, Range bounds) {
        ValueType leaf = form == null ? null : form.leaf();
        if (leaf == null) return value == null ? List.of() : List.copyOf(value);
        List<String> safe = value == null ? List.of() : value.stream().filter(Objects::nonNull).toList();
        List<String> choices = normalizeOptions(options, form, bounds);
        Range range = bounds == null ? Range.NONE : bounds;

        if (form instanceof ValueForm.Leaf) {
            return List.of(constrain(item(safe.isEmpty() ? null : safe.getFirst(), leaf, range), choices));
        }
        // An option-bearing list follows the declaration order, not the file's: two projects that picked the
        // same choices in a different order must write the same line, or a diff shows a change nobody made.
        if (!choices.isEmpty()) {
            LinkedHashSet<String> chosen = safe.stream()
                    .map(each -> item(each, leaf, range))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            return choices.stream().filter(chosen::contains).toList();
        }
        return safe.stream().map(each -> item(each, leaf, range)).toList();
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
    private String item(String wire, ValueType leaf, Range bounds) {
        String id = leaf.id();
        String canonical = catalog.normalize(id, wire);
        if (!leaf.bounded() || bounds.isEmpty()) return canonical;
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

    /**
     * One stored row and the group it belongs to — this store's, or another group of the same plugin's.
     *
     * <p><b>The wires are kept beside the row, and that is not redundancy.</b> Since 2026-09-20 a
     * {@link ParameterRow}'s value crosses as the Java initialiser it is written from, and this file holds
     * the stored form — the pair the coercion rules below are written over. A row of a type <em>this</em>
     * plugin's catalog cannot read has no initialiser at all, and every read here decodes every group's rows
     * to write the siblings back untouched. Deriving the stored form back out of a row would therefore empty
     * another window's value the first time this one saved.
     */
    private record Entry(String group, ParameterRow row, List<String> wires) {

        boolean mine(String groupId) {
            return groupId.equals(group);
        }

        /** The same row and group holding {@code changed}, which is every verb but a value edit. */
        Entry with(ParameterRow changed) {
            return new Entry(group, changed, wires);
        }
    }

    /** One entry of this store's group, with the row's initialiser written from {@code wires}. */
    private Entry entry(ParameterRow row, List<String> wires) {
        List<String> stored = wires == null ? List.of() : List.copyOf(wires);
        return new Entry(groupId, row.toBuilder().value(sourceOf(row.form(), stored)).build(), stored);
    }

    /** The stored value as the Java a field of this form takes — {@code ""} for a type nothing registers. */
    private String sourceOf(ValueForm form, List<String> wires) {
        return catalog.initializerOfWires(form, wires).orElse("");
    }

    /** That read backwards: the stored form an initialiser came from, or nothing the codec could read. */
    private List<String> wiresOf(ValueForm form, String source) {
        return catalog.wiresOfInitializer(form, source).orElse(List.of());
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
    private Optional<ParameterRow> edit(String name, java.util.function.UnaryOperator<Entry> change) {
        List<Entry> entries = read();
        int at = indexOf(entries, name);
        if (at < 0) return Optional.empty();

        Entry changed = change.apply(entries.get(at));
        if (changed == null || changed.row() == null) return Optional.empty();
        entries.set(at, changed);
        write(entries);
        return Optional.of(changed.row());
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
            List<String> wires = strings(node.path("value"));
            entries.add(new Entry(node.path(GROUP).asText(""), rowOf(node, wires), wires));
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
     * {@code ValueType.unknown} through the catalog, an unknown shape reads as one free value through
     * {@link StoredForms}, and an unknown visibility reads as the contract's own fallback.
     */
    private ParameterRow rowOf(JsonNode node, List<String> wires) {
        JsonNode type = node.path("type");
        ValueForm form = StoredForms.formOf(catalog,
                type.isObject() ? type.path("type").asText("") : type.asText(""),
                type.isObject() ? text(type.path("shape")) : null,
                type.isObject() && type.hasNonNull("list") ? type.path("list").asBoolean() : null);
        ParameterRow.Builder row = ParameterRow.named(node.path("name").asText(""), form)
                .value(sourceOf(form, wires))
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
     * One row with a new name, type, options and bounds — everything else carried across. The value is the
     * caller's, because it is the stored form that decides it and only {@link #entry} can write one.
     *
     * <p>A helper rather than {@link ParameterRow#toBuilder()} because the builder's name and type are
     * final: a row is a plugin-constructed value, so the two components that identify it are settled when it
     * is named rather than editable afterwards.
     */
    private static ParameterRow copy(ParameterRow row, String name, ValueForm form,
                                     List<String> options, Range bounds) {
        return ParameterRow.named(name, form)
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
     * <p>All three spellings of the type are written — the id, the shape and the older {@code list} boolean
     * — so a reader that predates the shape axis still sees a list as a list, and one that predates
     * {@code ValueForm} still reads a whole row. It costs two fields and it is what {@link StoredForms} is
     * total against.
     */
    private static ObjectNode nodeOf(Entry entry) {
        ParameterRow row = entry.row;
        ObjectNode node = MAPPER.createObjectNode();
        node.put("name", row.name());
        node.put(GROUP, entry.group);
        ObjectNode type = node.putObject("type");
        ValueType leaf = row.form().leaf();
        type.put("type", leaf == null ? row.form().sourceName() : leaf.id());
        type.put("shape", StoredForms.shapeOf(row.form(), !row.options().isEmpty()));
        type.put("list", StoredForms.isList(row.form()));
        put(node.putArray("value"), entry.wires);
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
