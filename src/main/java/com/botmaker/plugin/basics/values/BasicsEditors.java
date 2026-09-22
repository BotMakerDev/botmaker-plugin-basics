package com.botmaker.plugin.basics.values;

import com.botmaker.plugin.api.slot.ValueContext;
import com.botmaker.plugin.toolkit.Editors;
import com.botmaker.plugin.toolkit.Fields;
import com.botmaker.plugin.toolkit.Pills;
import com.botmaker.plugin.toolkit.Slots;
import com.botmaker.plugin.toolkit.Styles;
import com.botmaker.plugin.toolkit.Values;
import javafx.scene.Node;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.DatePicker;
import javafx.scene.control.MenuButton;
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
 * <h2>Two of them are deliberately plain</h2>
 *
 * <p>{@link #color} and {@link #duration} are a colour swatch and four number boxes, and the SDK overrides
 * both through {@code slotEditors()} with something better — a picker that samples the capture target, and
 * one that can turn {@code Wait.time(x)} into {@code Wait.between(min, max)}. Neither of those could live
 * here: the first reads the SDK's own capture file and <em>no code reads another plugin's file</em>, and
 * the second names an SDK type. What is here is what a project with no SDK installed still gets.
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
     * A length of time, as hours, minutes, seconds and milliseconds.
     *
     * <p>The plain one, behind a pill so a value on a block reads {@code 1m30s} rather than taking four
     * boxes of width. The SDK's is offered instead wherever the SDK is installed, because it can also turn
     * a fixed wait into a random one — which is a change to the enclosing <em>call</em> and names an SDK
     * type.
     */
    public static Node duration(ValueContext ctx) {
        Duration held = ctx.value(Duration.class).orElse(null);
        MenuButton pill = Pills.bare(durationLabel(ctx, held));
        Pills.onOpen(pill, () -> java.util.List.of(Pills.item("Set duration…", () -> {
            long[] picked = {held == null ? 0 : held.toMillis()};
            HBox boxes = Fields.duration(picked[0], millis -> picked[0] = millis);
            com.botmaker.plugin.toolkit.Modals.form(ctx, "Duration", boxes, () -> {
                Duration chosen = Duration.ofMillis(picked[0]);
                ctx.set(chosen);
                pill.setText(durationLabel(ctx, chosen));
            });
        })));
        return pill;
    }

    /**
     * What a duration pill says: {@code 1m30s}, or the expression as written when the grammar could not
     * read it.
     *
     * <p>Public because it is the one piece of these editors assertable with no JavaFX toolkit. The
     * spelling itself came from {@code JdkText.spellDuration}, which was deleted with the rest of the
     * stored-text reader — a bot has no use for it, and this is the only thing that ever read it back.
     */
    public static String durationLabel(ValueContext ctx, Duration value) {
        if (value != null) return spell(value.toMillis());
        return Slots.isEmpty(ctx) ? "Duration…" : Slots.raw(ctx);
    }

    /** {@code 0s}, {@code 250ms}, {@code 1m30s}, {@code 1h30m} — the largest units first, zeroes dropped. */
    private static String spell(long millis) {
        if (millis <= 0) return "0s";
        StringBuilder out = new StringBuilder();
        long left = millis;
        left = unit(out, left, 3_600_000L, "h");
        left = unit(out, left, 60_000L, "m");
        left = unit(out, left, 1000L, "s");
        if (left > 0) out.append(left).append("ms");
        return out.toString();
    }

    private static long unit(StringBuilder out, long left, long size, String suffix) {
        long count = left / size;
        if (count > 0) out.append(count).append(suffix);
        return left - count * size;
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
