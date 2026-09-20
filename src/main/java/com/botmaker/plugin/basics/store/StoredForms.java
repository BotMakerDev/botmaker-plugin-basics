package com.botmaker.plugin.basics.store;

import com.botmaker.plugin.api.value.ValueCatalog;
import com.botmaker.plugin.api.value.ValueContainer;
import com.botmaker.plugin.api.value.ValueForm;
import com.botmaker.plugin.api.value.ValueType;

/**
 * How a variable's type is written in the JSON files that predate {@link ValueForm}, read back and written
 * out again.
 *
 * <p>{@code ValueChoice} and {@code ValueShape} were deleted from the contract on 2026-09-20, and the files
 * that spell a type as {@code {"type":"DURATION","shape":"ANY_OF","list":true}} are still on disk. This is
 * the one place that knows that spelling — a compatibility reader, with the totality the deleted
 * {@code ValueShape.fromWire} carried, plus the writer that keeps an older Studio able to open a project a
 * newer one saved.
 *
 * <p><b>It exists once rather than twice.</b> Both readers of that spelling are files: this plugin's own
 * {@code parameters.json} and the SDK plugin's {@code activities.json}. The SDK depends on this module, so
 * the decoder lives on the side that owns the vocabulary the files are written in, and the two cannot drift.
 *
 * <p>The whole class goes when the files do — see phase L of {@code docs/refactor/32-generic-values.md}.
 */
public final class StoredForms {

    /** The pseudo-type of the files that predate the shape axis: text out of a written-down set. */
    private static final String LEGACY_CHOICE = "CHOICE";

    private StoredForms() {
    }

    /**
     * The form a stored type names. Total: an id nothing registers becomes an
     * {@linkplain ValueType#unknown unknown type} rather than a failed open, and a shape a newer writer
     * invented reads as one free value, which holds the stored text.
     *
     * <p>The one thing this cannot decide is the difference between the two list shapes, and it does not
     * have to: they emit the same {@code List<T>} and differed only in whether the author had written a set
     * of choices down, which is a sibling field and is now a question the declaration answers.
     *
     * @param catalog what resolves an id; the legacy {@code CHOICE} pseudo-type never reaches it
     * @param type    the stored type id
     * @param shape   the stored shape name, or {@code null} for a file older than the axis
     * @param list    the stored {@code list} boolean, or {@code null} when the file has no such field
     */
    public static ValueForm formOf(ValueCatalog catalog, String type, String shape, Boolean list) {
        boolean wasChoice = LEGACY_CHOICE.equals(type);
        ValueType base = wasChoice ? catalog.text() : catalog.type(type);
        ValueForm leaf = ValueForm.of(base);
        return isList(shape, list) ? ValueForm.listOf(leaf) : leaf;
    }

    /**
     * The shape name to write beside the type id, so a Studio built before this change still reads a list as
     * a list and a set of choices as a set of choices.
     *
     * @param form       what the row actually holds
     * @param hasOptions whether the row declares a set of values — the question the shape used to carry and
     *                   the type never did
     */
    public static String shapeOf(ValueForm form, boolean hasOptions) {
        if (isList(form)) return hasOptions ? "ANY_OF" : "OPEN_LIST";
        return hasOptions ? "ONE_OF" : "ONE";
    }

    /** Whether this form is emitted as {@code List<T>} — the other half of what the old files recorded. */
    public static boolean isList(ValueForm form) {
        return form instanceof ValueForm.Of of && ValueContainer.LIST.id().equals(of.container().id());
    }

    private static boolean isList(String shape, Boolean list) {
        if (shape != null && !shape.isBlank()) {
            String trimmed = shape.trim();
            return "ANY_OF".equals(trimmed) || "OPEN_LIST".equals(trimmed);
        }
        return Boolean.TRUE.equals(list);
    }
}
