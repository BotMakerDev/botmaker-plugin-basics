package com.botmaker.plugin.basics.values;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.util.StringConverter;

import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

/**
 * The time-of-day popup's body: a header {@code [07] : [30] : [00]}, a 24h clock dial (outer ring 1–12, inner
 * 13–23 and 00; then a minute ring), quick chips and Now, and the time in words. Geometry is {@link ClockDial}'s.
 * Colours are the host's looked-up ones, so both themes draw it. Writes nothing — the caller reads
 * {@link #time()} or {@link #offsetTime()} on OK.
 *
 * <p><b>The face alone takes the mouse (feedback 3).</b> Every drawn shape and number sits in one
 * mouse-transparent layer. A press on the hand used to land on the hand, and the redraw that press caused
 * removed it, so the drag that followed went to a node no longer on screen and the hand never moved.
 *
 * <p>With an offset (an {@code OffsetTime}), the offset is picked under the dial and named in the words; without
 * one, the words say it is this computer's clock.
 */
final class TimeDial {

    private enum Mode { HOUR, MINUTE }

    private static final double SIZE = 220;
    private static final double R = 100;
    private static final double C = SIZE / 2;
    private static final double OUTER_AT = 0.85;
    private static final double INNER_AT = 0.55;

    private final Pane face = new Pane();
    private final Group drawn = new Group();
    private final Spinner<Integer> hours = field(23);
    private final Spinner<Integer> minutes = field(59);
    private final Spinner<Integer> seconds = field(59);
    private final ComboBox<ZoneOffset> offsets;
    private final Label summary = new Label();
    private final VBox root;
    private int h;
    private int m;
    private int s;
    private ZoneOffset offset;
    private Mode mode = Mode.HOUR;
    private boolean touched;
    private boolean refreshing;

    /** A time on this computer's clock. */
    TimeDial(LocalTime initial) {
        this(initial, null);
    }

    /** A time at {@code offset} from UTC, or on this computer's clock when it is {@code null}. */
    TimeDial(LocalTime initial, ZoneOffset offset) {
        h = initial.getHour();
        m = initial.getMinute();
        s = initial.getSecond();
        this.offset = offset;

        hours.valueProperty().addListener((o, was, now) -> typed(() -> h = now, now));
        minutes.valueProperty().addListener((o, was, now) -> typed(() -> m = now, now));
        seconds.valueProperty().addListener((o, was, now) -> typed(() -> s = now, now));
        // The box being typed in is the ring being shown: hours on the hour ring, minutes on the minute one.
        hours.focusedProperty().addListener((o, was, is) -> {
            if (is) switchTo(Mode.HOUR);
        });
        minutes.focusedProperty().addListener((o, was, is) -> {
            if (is) switchTo(Mode.MINUTE);
        });
        HBox header = new HBox(4, hours, new Label(":"), minutes, new Label(":"), seconds);
        header.setAlignment(Pos.CENTER);

        drawn.setMouseTransparent(true);
        face.getChildren().add(drawn);
        face.setMinSize(SIZE, SIZE);
        face.setPrefSize(SIZE, SIZE);
        face.setMaxSize(SIZE, SIZE);
        face.setFocusTraversable(true);
        face.setOnMousePressed(e -> {
            face.requestFocus();
            pick(e.getX() - C, e.getY() - C);
        });
        face.setOnMouseDragged(e -> pick(e.getX() - C, e.getY() - C));
        face.setOnMouseReleased(e -> {
            if (mode == Mode.HOUR) switchTo(Mode.MINUTE);   // an hour picked: now the minutes
        });
        face.setOnKeyPressed(e -> {
            KeyCode code = e.getCode();
            if (code == KeyCode.UP || code == KeyCode.RIGHT) step(1);
            else if (code == KeyCode.DOWN || code == KeyCode.LEFT) step(-1);
            else if (code == KeyCode.TAB && mode == Mode.HOUR) switchTo(Mode.MINUTE);
            else if (code == KeyCode.TAB) seconds.requestFocus();
            else return;
            e.consume();
        });

        HBox chips = new HBox(4);
        chips.setAlignment(Pos.CENTER);
        for (int hour : new int[]{0, 6, 12, 18}) {
            Button chip = new Button(TimeText.pill(LocalTime.of(hour, 0)));
            chip.setOnAction(e -> setTime(LocalTime.of(hour, 0)));
            chips.getChildren().add(chip);
        }
        Button now = new Button("Now");
        // A fixed value, never LocalTime.now(); at an offset, now as that offset's clock reads it.
        now.setOnAction(e -> setTime(this.offset == null ? LocalTime.now().withNano(0)
                : OffsetTime.now(this.offset).toLocalTime().withNano(0)));
        chips.getChildren().add(now);

        root = new VBox(10, header, face, chips);
        if (offset != null) {
            offsets = new ComboBox<>();
            offsets.getItems().setAll(TimeText.offsets());
            if (!offsets.getItems().contains(offset)) offsets.getItems().add(offset);
            offsets.setValue(offset);
            offsets.setConverter(new StringConverter<>() {
                @Override public String toString(ZoneOffset value) {
                    return value == null ? "" : TimeText.offset(value);
                }
                @Override public ZoneOffset fromString(String text) {
                    return offsets.getValue();
                }
            });
            offsets.valueProperty().addListener((o, was, is) -> {
                if (is == null || refreshing) return;
                this.offset = is;
                touched = true;
                refresh();
            });
            HBox at = new HBox(6, new Label("Offset"), offsets);
            at.setAlignment(Pos.CENTER);
            root.getChildren().add(at);
        } else {
            offsets = null;
        }
        root.getChildren().add(summary);
        root.setAlignment(Pos.CENTER);
        refresh();
    }

    Parent node() {
        return root;
    }

    LocalTime time() {
        return LocalTime.of(h, m, s);
    }

    /** The time at its offset; UTC for a dial opened without one. */
    OffsetTime offsetTime() {
        return OffsetTime.of(time(), offset == null ? ZoneOffset.UTC : offset);
    }

    /** Whether the person picked anything — the dial, a key, a box, a chip, Now or the offset. */
    boolean touched() {
        return touched;
    }

    /**
     * An hour, minute or second box: editable, wrapping, and holding its value when what is typed is not a
     * number in range ({@link TimeText#field}) — the stock converter threw on a letter.
     */
    private static Spinner<Integer> field(int max) {
        SpinnerValueFactory.IntegerSpinnerValueFactory values = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, max);
        values.setWrapAround(true);
        values.setConverter(new StringConverter<>() {
            @Override public String toString(Integer value) {
                return value == null ? "" : String.format("%02d", value);
            }
            @Override public Integer fromString(String text) {
                Integer read = TimeText.field(text, max);
                return read != null ? read : values.getValue();
            }
        });
        Spinner<Integer> spinner = new Spinner<>(values);
        spinner.setEditable(true);
        spinner.setPrefWidth(72);
        // A refused entry is shown as the value it kept, not left in the box looking accepted.
        spinner.getEditor().focusedProperty().addListener((o, was, is) -> {
            if (!is) spinner.getEditor().setText(values.getConverter().toString(values.getValue()));
        });
        return spinner;
    }

    private void typed(Runnable apply, Integer now) {
        if (refreshing || now == null) return;
        apply.run();
        touched = true;
        refresh();
    }

    private void switchTo(Mode next) {
        if (mode == next) return;
        mode = next;
        refresh();
    }

    private void pick(double dx, double dy) {
        if (mode == Mode.HOUR) h = ClockDial.hourAt(dx, dy, R);
        else m = ClockDial.minuteAt(dx, dy);
        touched = true;
        refresh();
    }

    private void step(int delta) {
        if (mode == Mode.HOUR) h = Math.floorMod(h + delta, 24);
        else m = Math.floorMod(m + delta, 60);
        touched = true;
        refresh();
    }

    private void setTime(LocalTime t) {
        h = t.getHour();
        m = t.getMinute();
        s = t.getSecond();
        touched = true;
        refresh();
    }

    private void refresh() {
        refreshing = true;
        hours.getValueFactory().setValue(h);
        minutes.getValueFactory().setValue(m);
        seconds.getValueFactory().setValue(s);
        refreshing = false;
        hours.setStyle(mode == Mode.HOUR ? "-fx-font-weight: bold;" : "");
        minutes.setStyle(mode == Mode.MINUTE ? "-fx-font-weight: bold;" : "");
        summary.setText("= " + TimeText.words(time())
                + (offset == null ? ", on this computer's clock" : " " + TimeText.offset(offset)));
        draw();
    }

    private void draw() {
        drawn.getChildren().clear();
        // The whole face, invisible: the layer keeps the dial's size however far the hand reaches.
        Circle bounds = new Circle(C, C, C);
        bounds.setStyle("-fx-fill: transparent;");
        Circle rim = new Circle(C, C, R);
        rim.setStyle("-fx-fill: transparent; -fx-stroke: -bm-divider; -fx-stroke-width: 1;");
        drawn.getChildren().addAll(bounds, rim);

        double angle = mode == Mode.HOUR ? ClockDial.hourAngle(h) : ClockDial.minuteAngle(m);
        double reach = (mode == Mode.HOUR && ClockDial.inner(h) ? INNER_AT : OUTER_AT) * R;
        double[] tip = point(angle, reach);
        Line hand = new Line(C, C, tip[0], tip[1]);
        hand.setStyle("-fx-stroke: -fx-accent; -fx-stroke-width: 2;");
        Circle knob = new Circle(tip[0], tip[1], 13);
        knob.setStyle("-fx-fill: -fx-accent; -fx-opacity: 0.35;");
        drawn.getChildren().addAll(hand, knob);

        if (mode == Mode.HOUR) {
            for (int position = 0; position < 12; position++) {
                number(position == 0 ? "12" : Integer.toString(position), position * 30.0, OUTER_AT * R, false);
                number(String.format("%02d", position == 0 ? 0 : position + 12), position * 30.0, INNER_AT * R, true);
            }
        } else {
            for (int position = 0; position < 12; position++) {
                number(String.format("%02d", position * 5), position * 30.0, OUTER_AT * R, false);
            }
        }
    }

    private void number(String text, double angle, double distance, boolean small) {
        // A fixed box centred on the point: a label is not measured until it is in a scene, so its own
        // preferred size would be zero here and every number would sit off-centre.
        Label label = new Label(text);
        label.setAlignment(Pos.CENTER);
        label.setMinSize(28, 20);
        label.setPrefSize(28, 20);
        if (small) label.setStyle("-fx-font-size: 0.85em; -fx-opacity: 0.8;");
        double[] p = point(angle, distance);
        label.relocate(p[0] - 14, p[1] - 10);
        drawn.getChildren().add(label);
    }

    private static double[] point(double angle, double distance) {
        double rad = Math.toRadians(angle);
        return new double[]{C + Math.sin(rad) * distance, C - Math.cos(rad) * distance};
    }
}
