package com.botmaker.plugin.basics.values;

import com.botmaker.plugin.api.slot.ValueContext;
import com.botmaker.plugin.toolkit.Editors;
import com.botmaker.plugin.toolkit.Fields;
import com.botmaker.plugin.toolkit.Modals;
import com.botmaker.plugin.toolkit.Pills;
import com.botmaker.plugin.toolkit.Slots;
import com.botmaker.plugin.toolkit.Styles;
import com.botmaker.plugin.toolkit.Values;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.awt.Color;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * The widgets for {@link BasicsTypes}' nine.
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

    /** Text: a field that commits on Enter and on losing focus. */
    public static Node text(ValueContext ctx) {
        return Editors.text(ctx, "text");
    }

    /**
     * A tick box with no label of its own.
     *
     * <p>The toolkit's {@link Editors#flag} carries one because a bare box beside
     * {@code enableDebug(true)} reads as though the box is the argument to something else. Here the row
     * already says the field's name, so a second copy of it would read as two settings.
     */
    public static Node flag(ValueContext ctx) {
        return Editors.flag(ctx, "");
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
        TextField field = Fields.committing(shown, whole ? "0" : "0.0", typed -> {
            try {
                Values.setNumber(ctx, Double.parseDouble(typed.trim()));
            } catch (NumberFormatException notANumber) {
                // Left as typed. A cell that silently replaced it with 0 would lose what the user meant,
                // and a cell that refused would be refusing for a reason it cannot explain.
            }
        });
        field.setPrefColumnCount(whole ? 8 : 10);
        return field;
    }

    /**
     * One character.
     *
     * <p>The field takes one and keeps the first of anything longer, which is what a paste of a whole word
     * means: the author wanted its first letter, and refusing the paste outright says less.
     */
    public static Node character(ValueContext ctx) {
        String held = ctx.value(Character.class).map(String::valueOf).orElse("");
        TextField field = Fields.committing(held, "a", typed -> {
            if (!typed.isEmpty()) ctx.set(typed.charAt(0));
        });
        field.setPrefColumnCount(2);
        return field;
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
     * A time of day, as three boxes.
     *
     * <p>Three rather than a text field, for the reason {@link Fields#duration} gives about four: a person
     * typing {@code 07:30} into a free field has to be told the format, and every format anybody types is
     * one somebody else's locale spells differently.
     */
    public static Node time(ValueContext ctx) {
        LocalTime held = ctx.value(LocalTime.class).orElse(LocalTime.MIDNIGHT);
        TextField hours = unit(held.getHour(), "h");
        TextField minutes = unit(held.getMinute(), "m");
        TextField seconds = unit(held.getSecond(), "s");

        Runnable report = () -> ctx.set(LocalTime.of(
                Math.clamp(whole(hours), 0, 23), Math.clamp(whole(minutes), 0, 59),
                Math.clamp(whole(seconds), 0, 59)));
        for (TextField box : new TextField[] {hours, minutes, seconds}) {
            box.focusedProperty().addListener((obs, was, focused) -> {
                if (!focused) report.run();
            });
            box.setOnAction(e -> report.run());
        }
        return new HBox(6, hours, minutes, seconds);
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

    private static TextField unit(int value, String suffix) {
        TextField field = Styles.on(new TextField(Integer.toString(value)), Styles.INSET_FIELD);
        field.setPromptText(suffix);
        field.setPrefColumnCount(3);
        return field;
    }

    /** What a box says as a whole number, floored at zero — a half-typed number is a normal state. */
    private static int whole(TextField field) {
        try {
            return Math.max(0, Integer.parseInt(field.getText() == null ? "" : field.getText().trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
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
