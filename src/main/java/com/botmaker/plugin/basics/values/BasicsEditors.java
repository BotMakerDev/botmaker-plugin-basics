package com.botmaker.plugin.basics.values;

import com.botmaker.plugin.api.slot.Bounds;
import com.botmaker.plugin.api.slot.ValueContext;
import com.botmaker.plugin.toolkit.Editors;
import com.botmaker.plugin.toolkit.Fields;
import com.botmaker.plugin.toolkit.Modals;
import com.botmaker.plugin.toolkit.Pills;
import com.botmaker.plugin.toolkit.Slots;
import com.botmaker.plugin.toolkit.Styles;
import com.botmaker.plugin.toolkit.Values;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.awt.Color;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetTime;
import java.time.ZoneOffset;

/**
 * The widgets for {@link BasicsTypes}' eleven.
 *
 * <h2>This is not a second toolkit, and the rule that keeps it from becoming one</h2>
 *
 * <p><b>Nothing here is reusable and nothing here is meant to be.</b> Every method is the editor for one
 * type this plugin declares, named after that type, reachable only through it. The generic shapes — a
 * committing field, a bounded number, a pill, a modal — are {@code botmaker-plugin-toolkit}'s and are
 * called from here; the day a method here would be useful to a plugin with no vocabulary of its own, it
 * belongs there instead.
 *
 * <p>They were Studio's own {@code ValueEditors} until 2026-09-22, drawn by the host off a value-type id.
 * That was the host holding one plugin's vocabulary: a switch on {@code "DATE"} in the editor is the thing
 * an open type system exists to remove. Basics declares the type, so basics draws it.
 *
 * <h2>The JDK types are drawn here, and nowhere else but Color</h2>
 *
 * <p>Basics owns the JDK types, so it draws them — the Duration picker and the time-of-day dial included
 * (2026-09-27; the SDK's own Duration editor was a near copy and is deleted). {@link #color} stays a plain
 * swatch: the SDK offers an eyedropper over the capture target through {@code slotEditors()}, which needs
 * screen capture this plugin does not have, and the host asks the user which editor to use.
 *
 * <h2>Reading a box is not parsing Java</h2>
 *
 * <p>The number fields below read what a person typed into them, which is the one kind of parsing a widget
 * cannot avoid and is nothing to do with the rule that no plugin reads Java. The value arrives typed
 * through {@link ValueContext#value} and leaves typed through {@link ValueContext#set(Object)}; the host
 * owns every character of syntax in both directions.
 *
 * <p><b>Building an editor never writes.</b> Not even to normalise what is already there — a project merely
 * opened and closed must come back byte-identical.
 */
public final class BasicsEditors {

    private BasicsEditors() {
    }

    /**
     * Text: a field that commits on Enter and on losing focus, and ⤢, which opens it as a multi-line editor
     * for text too long for a row (picker 6e3). OK writes; Cancel writes nothing.
     */
    public static Node text(ValueContext ctx) {
        Node field = Editors.text(ctx, "text");
        Button expand = Pills.icon("⤢", () -> {
            TextArea area = new TextArea(Values.text(ctx, ""));
            area.setWrapText(true);
            area.setPrefRowCount(8);
            area.setPrefColumnCount(40);
            Modals.form(ctx, "Text", area, () -> ctx.set(area.getText()));
        });
        expand.setTooltip(new Tooltip("Edit as several lines"));
        HBox row = new HBox(4, field, expand);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(field, Priority.ALWAYS);
        return row;
    }

    /**
     * An on/off switch saying its state in words (picker 6e3) and in colour, red Off and green On — a bare
     * tick box beside a row name reads as unset rather than off. No label of its own: the row already says
     * the field's name.
     */
    public static Node flag(ValueContext ctx) {
        boolean held = Values.flag(ctx, false);
        ToggleButton toggle = new ToggleButton(PickRules.flagLabel(held));
        toggle.setSelected(held);
        toggle.setMinWidth(52);
        toggle.getStyleClass().add(Styles.SWITCH);
        toggle.selectedProperty().addListener((o, was, is) -> {
            toggle.setText(PickRules.flagLabel(is));
            ctx.set(is);
        });
        return toggle;
    }

    /**
     * A day of the week as a row of seven toggles, Monday first (picker 6e3). One is on at most; clicking the
     * one that is on leaves it on, since a day is a value and "no day" is not one.
     */
    public static Node dayOfWeek(ValueContext ctx) {
        DayOfWeek held = ctx.value(DayOfWeek.class).orElse(null);
        ToggleGroup group = new ToggleGroup();
        HBox row = new HBox(2);
        row.setAlignment(Pos.CENTER_LEFT);
        for (DayOfWeek day : DayOfWeek.values()) {
            ToggleButton button = new ToggleButton(PickRules.shortName(day));
            button.setToggleGroup(group);
            button.setUserData(day);
            button.setSelected(day == held);
            button.setOnAction(e -> {
                if (!button.isSelected()) {
                    button.setSelected(true);
                    return;
                }
                if (day != ctx.value(DayOfWeek.class).orElse(null)) ctx.set(day);
            });
            row.getChildren().add(button);
        }
        return row;
    }

    /** A month: a pill naming it that opens the twelve as a three-by-four grid (picker 6e3). */
    public static Node month(ValueContext ctx) {
        Month held = ctx.value(Month.class).orElse(null);
        MenuButton pill = Pills.bare(held == null ? Values.labelOr(ctx.source(), "Month…") : PickRules.longName(held));
        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        for (Month month : Month.values()) {
            Button cell = new Button(PickRules.shortName(month));
            cell.setMinWidth(48);
            cell.setOnAction(e -> {
                pill.hide();
                pill.setText(PickRules.longName(month));
                if (month != ctx.value(Month.class).orElse(null)) ctx.set(month);
            });
            int index = month.ordinal();
            grid.add(cell, index % 4, index / 4);
        }
        CustomMenuItem item = new CustomMenuItem(grid, false);
        pill.getItems().setAll(item);
        return pill;
    }

    /** A whole number, typed. */
    public static Node whole(ValueContext ctx) {
        return number(ctx, true);
    }

    /** A decimal number, typed. */
    public static Node decimal(ValueContext ctx) {
        return number(ctx, false);
    }

    /**
     * A number field that writes as the type the field is declared as.
     *
     * <p>{@link Values#setNumber} decides the box from {@link ValueContext#type()}, so the same widget
     * serves an {@code int} field and a {@code long} one without either of them writing {@code 3.0}.
     * Unreadable text is left alone rather than written as zero: somebody halfway through typing
     * {@code -} has not asked for anything yet.
     */
    private static Node number(ValueContext ctx, boolean whole) {
        double held = Values.number(ctx, whole ? 0 : 0.0);
        String shown = Slots.isEmpty(ctx) ? "" : whole ? Long.toString(Math.round(held)) : trim(held);
        // The declared @Param(min, max), which only the host knows (2026-09-27): typed and stepped values are
        // both held inside it, and the field says what it is.
        Bounds bounds = ctx.bounds();
        String range = PickRules.rangeLabel(bounds.min(), bounds.max());
        TextField[] box = new TextField[1];
        TextField field = Fields.committing(shown, range.isEmpty() ? whole ? "0" : "0.0" : range, typed -> {
            try {
                double wanted = Double.parseDouble(typed.trim());
                double kept = PickRules.within(wanted, whole, bounds.min(), bounds.max());
                if (kept != wanted) box[0].setText(whole ? Long.toString(Math.round(kept)) : trim(kept));
                Values.setNumber(ctx, kept);
            } catch (NumberFormatException notANumber) {
                // Left as typed. A cell that silently replaced it with 0 would lose what the user meant,
                // and a cell that refused would be refusing for a reason it cannot explain.
            }
        });
        box[0] = field;
        field.setPrefColumnCount(whole ? 8 : 10);
        if (!range.isEmpty()) field.setTooltip(new Tooltip("Allowed: " + range));

        // The stepper (picker 6e3): ▲/▼ and the scroll wheel move by one, or by the last decimal place shown;
        // Shift is ×10. Each step writes, like typing a number and pressing Enter, and stops at the range's ends.
        java.util.function.BiConsumer<Integer, Boolean> step = (direction, shift) -> {
            double current;
            try {
                current = Double.parseDouble(field.getText().trim());
            } catch (NumberFormatException notANumber) {
                current = 0;
            }
            double next = PickRules.within(PickRules.step(current, whole, field.getText(), direction, shift),
                    whole, bounds.min(), bounds.max());
            field.setText(whole ? Long.toString(Math.round(next)) : trim(next));
            Values.setNumber(ctx, next);
        };
        // Shift is read as the button is pressed: an action event does not carry the modifier keys.
        boolean[] shift = {false};
        Button up = Pills.icon("▲", () -> step.accept(1, shift[0]));
        Button down = Pills.icon("▼", () -> step.accept(-1, shift[0]));
        up.setOnMousePressed(e -> shift[0] = e.isShiftDown());
        down.setOnMousePressed(e -> shift[0] = e.isShiftDown());
        up.setTooltip(new Tooltip("Up one (Shift: ten) — or scroll over the number"));
        down.setTooltip(new Tooltip("Down one (Shift: ten) — or scroll over the number"));
        field.setOnScroll(e -> {
            if (e.getDeltaY() != 0) step.accept(e.getDeltaY() > 0 ? 1 : -1, e.isShiftDown());
        });
        HBox row = new HBox(2, field, new VBox(0, up, down));
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(field, Priority.ALWAYS);
        return row;
    }

    /**
     * One character.
     *
     * <p>The field takes one and keeps the first of anything longer, which is what a paste of a whole word
     * means: the author wanted its first letter, and refusing the paste outright says less. A character a
     * person cannot see is shown by name — {@code space}, {@code tab} — and typing that name means it
     * (picker 6e3). <b>Special…</b> opens the characters that are hard to type or to see, in three labelled
     * groups ({@link PickRules#SPECIAL}); a click picks one and closes it (feedback 2, 2026-09-27 — it was a ⋯
     * menu of fifteen symbols that read as "other characters" and did not say why they were there).
     *
     * <p>The tooltip says what a character is <em>for</em>, because the question asked was how it differs
     * from a key: a character is text, a key is something pressed.
     */
    public static Node character(ValueContext ctx) {
        String held = ctx.value(Character.class).map(PickRules::charLabel).orElse("");
        TextField field = Fields.committing(held, "a", typed ->
                PickRules.charFrom(typed).ifPresent(ctx::set));
        field.setPrefColumnCount(6);
        field.setTooltip(new Tooltip("A character is text: one letter of a string, or what OCR reads. "
                + "To press something on the keyboard, use a Key instead.\n"
                + "Type it, or a name: space, tab, newline."));
        Button special = Pills.button("Special…", () -> specialCharacters(ctx, field));
        special.setTooltip(new Tooltip("Spaces, separators, brackets and quotes"));
        HBox row = new HBox(4, field, special);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(field, Priority.ALWAYS);
        return row;
    }

    /** The Special… grid: one labelled row per group, a button per character; a click is the answer. */
    private static void specialCharacters(ValueContext ctx, TextField field) {
        VBox groups = new VBox(8);
        javafx.stage.Stage[] stage = new javafx.stage.Stage[1];
        for (PickRules.CharGroup group : PickRules.SPECIAL) {
            javafx.scene.layout.FlowPane buttons = new javafx.scene.layout.FlowPane(4, 4);
            buttons.setPrefWrapLength(320);
            for (char c : group.characters()) {
                Button pick = new Button(PickRules.charLabel(c));
                pick.setMinWidth(36);
                pick.setTooltip(new Tooltip(String.format("U+%04X", (int) c)));
                pick.setOnAction(e -> {
                    field.setText(PickRules.charLabel(c));
                    ctx.set(c);
                    if (stage[0] != null) stage[0].close();
                });
                buttons.getChildren().add(pick);
            }
            Label title = Styles.on(new Label(group.title()), Styles.DIALOG_SUBHEADING);
            groups.getChildren().add(new VBox(4, title, buttons));
        }
        stage[0] = Modals.form(ctx, "Special character", groups, null);
    }

    /**
     * A colour swatch.
     *
     * <p>The plain one. The SDK's samples a frozen frame of the capture target and is offered instead
     * wherever the SDK is installed — see this class's note.
     */
    public static Node color(ValueContext ctx) {
        Color held = ctx.value(Color.class).orElse(null);
        ColorPicker picker = Styles.on(new ColorPicker(), Styles.INSET_FIELD_FLAT);
        if (held != null) picker.setValue(fx(held));
        picker.setOnAction(e -> ctx.set(awt(picker.getValue())));
        return picker;
    }

    /** A date, out of the platform's own calendar. */
    public static Node date(ValueContext ctx) {
        DatePicker picker = Styles.on(new DatePicker(), Styles.INSET_FIELD_FLAT);
        ctx.value(LocalDate.class).ifPresent(picker::setValue);
        picker.valueProperty().addListener((obs, was, now) -> {
            if (now != null && !now.equals(was)) ctx.set(now);
        });
        return picker;
    }

    /**
     * A time of day: a pill ({@code 07:30}) that opens a 24h clock dial ({@link TimeDial}). OK writes through
     * {@link #commit}; a slot the host could not read opens on midnight, and a value with nanoseconds opens
     * truncated to the second without writing the truncation back unless something is picked.
     *
     * <p>A dial rather than a text field: a person typing {@code 07:30} into a free field has to be told the
     * format, and every format anybody types is one somebody else's locale spells differently.
     */
    public static Node time(ValueContext ctx) {
        Button[] pill = new Button[1];
        pill[0] = Pills.button(timeLabel(ctx), () -> {
            LocalTime before = ctx.value(LocalTime.class).orElse(null);
            TimeDial dial = new TimeDial(before == null ? LocalTime.MIDNIGHT : before.withNano(0));
            Modals.form(ctx, "Time of day", dial.node(), () -> {
                LocalTime after = dial.time();
                if (commit(ctx, before, after, dial.touched())) pill[0].setText(TimeText.pill(after));
            });
        });
        return pill[0];
    }

    /**
     * A time of day at an offset from UTC (feedback 3): the same dial, with the offset picked under it. A slot
     * the host could not read opens on midnight UTC.
     */
    public static Node offsetTime(ValueContext ctx) {
        Button[] pill = new Button[1];
        pill[0] = Pills.button(offsetTimeLabel(ctx), () -> {
            OffsetTime before = ctx.value(OffsetTime.class).orElse(null);
            OffsetTime start = before == null ? OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC) : before.withNano(0);
            TimeDial dial = new TimeDial(start.toLocalTime(), start.getOffset());
            Modals.form(ctx, "Time of day", dial.node(), () -> {
                OffsetTime after = dial.offsetTime();
                if (commit(ctx, before, after, dial.touched())) pill[0].setText(TimeText.pill(after));
            });
        });
        return pill[0];
    }

    /** What an offset time pill says: {@code 07:30 UTC}, the source as written, or {@code Time…} when empty. */
    static String offsetTimeLabel(ValueContext ctx) {
        return ctx.value(OffsetTime.class).map(TimeText::pill)
                .orElseGet(() -> Slots.isEmpty(ctx) ? "Time…" : Slots.raw(ctx));
    }

    // ---- previews: a value shown, not edited ------------------------------------------------------------
    //
    // Here rather than as lambdas in BasicsTypes: a lambda answering `new Label(…)` where a Node is declared
    // makes the verifier load both classes when BasicsTypes loads, so a host without JavaFX could not read
    // this plugin's type list at all. Declared as Node, and only linked when the host draws.

    /** A time of day, read-only. */
    public static Node timePreview(ValueContext ctx) {
        return new Label(timeLabel(ctx));
    }

    /** A time at an offset from UTC, read-only. */
    public static Node offsetTimePreview(ValueContext ctx) {
        return new Label(offsetTimeLabel(ctx));
    }

    /** A length of time, read-only, in words. */
    public static Node durationPreview(ValueContext ctx) {
        return new Label(durationLabel(ctx, ctx.value(Duration.class).orElse(null)));
    }

    /** A day of the week, read-only, short. */
    public static Node dayOfWeekPreview(ValueContext ctx) {
        return new Label(ctx.value(DayOfWeek.class).map(PickRules::shortName).orElse(""));
    }

    /** A month, read-only, in full. */
    public static Node monthPreview(ValueContext ctx) {
        return new Label(ctx.value(Month.class).map(PickRules::longName).orElse(""));
    }

    /** An offset from UTC: a pill naming it ({@code UTC+02:00}) that opens every offset clocks keep. */
    public static Node zoneOffset(ValueContext ctx) {
        ZoneOffset held = ctx.value(ZoneOffset.class).orElse(null);
        MenuButton pill = Pills.bare(held == null ? Values.labelOr(ctx.source(), "Offset…") : TimeText.offset(held));
        for (ZoneOffset offset : TimeText.offsets()) {
            javafx.scene.control.MenuItem item = new javafx.scene.control.MenuItem(TimeText.offset(offset));
            item.setOnAction(e -> {
                pill.setText(TimeText.offset(offset));
                if (!offset.equals(ctx.value(ZoneOffset.class).orElse(null))) ctx.set(offset);
            });
            pill.getItems().add(item);
        }
        return pill;
    }

    /** What a time pill says: {@code 07:30}, the source as written, or {@code Time…} when empty. */
    static String timeLabel(ValueContext ctx) {
        return ctx.value(LocalTime.class).map(TimeText::pill)
                .orElseGet(() -> Slots.isEmpty(ctx) ? "Time…" : Slots.raw(ctx));
    }

    /**
     * A length of time: a pill ({@code 1m30s}) that opens preset chips, one spinner per unit and the length in
     * words ({@link DurationPicker}). OK writes through {@link #commit}; a slot the host could not read opens on
     * one second.
     */
    public static Node duration(ValueContext ctx) {
        Button[] pill = new Button[1];
        pill[0] = Pills.button(durationLabel(ctx, ctx.value(Duration.class).orElse(null)), () -> {
            Duration before = ctx.value(Duration.class).orElse(null);
            DurationPicker picker = new DurationPicker(before == null ? 1_000L : before.toMillis());
            Modals.form(ctx, "Duration", picker.node(), () -> {
                Duration after = Duration.ofMillis(picker.millis());
                if (commit(ctx, before, after, picker.touched())) pill[0].setText(durationLabel(ctx, after));
            });
        });
        return pill[0];
    }

    /**
     * What a duration pill says: {@code 1m30s}, or the expression as written when the grammar could not
     * read it, or {@code Duration…} when the slot is empty.
     *
     * <p>Public because it is the one piece of these editors assertable with no JavaFX toolkit; the spelling
     * is {@link DurationText#spell}.
     */
    public static String durationLabel(ValueContext ctx, Duration value) {
        if (value != null) return DurationText.spell(value.toMillis());
        return Slots.isEmpty(ctx) ? "Duration…" : Slots.raw(ctx);
    }

    /**
     * The one write rule both time pickers share: only what the person picked, and only when it differs from
     * what was read. So opening a picker and pressing OK leaves the file byte-identical, and a source the host
     * could not read ({@code before == null}) is replaced only once something was chosen.
     *
     * @return whether it wrote
     */
    static <T> boolean commit(ValueContext ctx, T before, T after, boolean touched) {
        if (!touched || after == null || after.equals(before)) return false;
        ctx.set(after);
        return true;
    }

    /** A whole number reads as one — {@code 3}, not {@code 3.0}. */
    private static String trim(double value) {
        return value == Math.rint(value) ? Long.toString(Math.round(value)) : Double.toString(value);
    }

    private static javafx.scene.paint.Color fx(Color awt) {
        return javafx.scene.paint.Color.rgb(awt.getRed(), awt.getGreen(), awt.getBlue());
    }

    private static Color awt(javafx.scene.paint.Color fx) {
        if (fx == null) return Color.WHITE;
        return new Color((int) Math.round(fx.getRed() * 255), (int) Math.round(fx.getGreen() * 255),
                (int) Math.round(fx.getBlue() * 255));
    }
}
