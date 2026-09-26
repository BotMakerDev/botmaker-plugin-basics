package com.botmaker.plugin.basics.values;

import com.botmaker.plugin.basics.values.DurationParts.Unit;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.EnumMap;
import java.util.Map;

/**
 * The Duration popup's body: preset chips, one spinner per unit, and the length in words. Every rule is
 * {@link DurationParts}'; this only draws it. Writes nothing — the caller reads {@link #millis()} on OK.
 */
final class DurationPicker {

    private final Map<Unit, Spinner<Long>> spinners = new EnumMap<>(Unit.class);
    private final Label summary = new Label();
    private final VBox root;
    private DurationParts parts;
    private boolean touched;
    private boolean refreshing;

    DurationPicker(long initialMillis) {
        parts = DurationParts.of(initialMillis);

        FlowPane chips = new FlowPane(4, 4);
        chips.setPrefWrapLength(280);
        for (long preset : DurationText.PRESETS) {
            Button chip = new Button(DurationText.spell(preset));
            chip.setOnAction(e -> set(DurationParts.of(preset)));
            chips.getChildren().add(chip);
        }

        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        for (Unit unit : Unit.values()) {
            Spinner<Long> spinner = new Spinner<>(new UnitFactory(unit));
            spinner.setEditable(true);
            spinner.setPrefWidth(unit == Unit.MILLIS ? 84 : 72);
            spinners.put(unit, spinner);
            row.getChildren().addAll(spinner, new Label(unit.suffix()));
        }

        root = new VBox(10, chips, row, summary);
        refresh();
    }

    Parent node() {
        return root;
    }

    long millis() {
        return parts.total();
    }

    /** Whether the person picked anything — a chip, a step or a typed box. */
    boolean touched() {
        return touched;
    }

    private void set(DurationParts next) {
        touched = true;
        parts = next.normalised();
        refresh();
    }

    private void refresh() {
        refreshing = true;
        spinners.forEach((unit, spinner) -> spinner.getValueFactory().setValue(parts.get(unit)));
        refreshing = false;
        summary.setText("= " + DurationText.words(parts.total()));
    }

    /** One unit's box: a step moves the whole length, a typed number replaces this unit and is normalised. */
    private final class UnitFactory extends SpinnerValueFactory<Long> {
        private final Unit unit;

        UnitFactory(Unit unit) {
            this.unit = unit;
            setConverter(new StringConverter<>() {
                @Override public String toString(Long value) {
                    return value == null ? "0" : Long.toString(value);
                }

                @Override public Long fromString(String text) {
                    try {
                        return Math.max(0L, Long.parseLong(text == null ? "" : text.trim()));
                    } catch (NumberFormatException e) {
                        return 0L;   // a half-typed box is a normal state, not an error
                    }
                }
            });
            valueProperty().addListener((o, was, now) -> {
                if (!refreshing) set(parts.with(unit, now == null ? 0L : now));
            });
        }

        @Override public void increment(int steps) {
            set(parts.step(unit, steps));
        }

        @Override public void decrement(int steps) {
            set(parts.step(unit, -steps));
        }
    }
}
