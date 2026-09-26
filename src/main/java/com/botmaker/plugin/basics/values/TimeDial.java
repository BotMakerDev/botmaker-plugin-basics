package com.botmaker.plugin.basics.values;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import java.time.LocalTime;

/**
 * The time-of-day popup's body: a header {@code 07 : 30 : [00]}, a 24h clock dial (outer ring 1–12, inner
 * 13–23 and 00; then a minute ring), quick chips and Now, and the time in words. Geometry is {@link ClockDial}'s.
 * Colours are the host's looked-up ones, so both themes draw it. Writes nothing — the caller reads
 * {@link #time()} on OK.
 */
final class TimeDial {

    private enum Mode { HOUR, MINUTE }

    private static final double SIZE = 220;
    private static final double R = 100;
    private static final double C = SIZE / 2;
    private static final double OUTER_AT = 0.85;
    private static final double INNER_AT = 0.55;

    private final Pane face = new Pane();
    private final Label hours = new Label();
    private final Label minutes = new Label();
    private final Label summary = new Label();
    private final Spinner<Integer> seconds = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59));
    private final VBox root;
    private int h;
    private int m;
    private int s;
    private Mode mode = Mode.HOUR;
    private boolean touched;
    private boolean refreshing;

    TimeDial(LocalTime initial) {
        h = initial.getHour();
        m = initial.getMinute();
        s = initial.getSecond();

        ((SpinnerValueFactory.IntegerSpinnerValueFactory) seconds.getValueFactory()).setWrapAround(true);
        seconds.setEditable(true);
        seconds.setPrefWidth(72);
        seconds.valueProperty().addListener((o, was, now) -> {
            if (refreshing || now == null) return;
            s = now;
            touched = true;
            refresh();
        });

        hours.setOnMouseClicked(e -> switchTo(Mode.HOUR));
        minutes.setOnMouseClicked(e -> switchTo(Mode.MINUTE));
        HBox header = new HBox(4, hours, new Label(":"), minutes, new Label(":"), seconds);
        header.setAlignment(Pos.CENTER);

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
        now.setOnAction(e -> setTime(LocalTime.now().withNano(0)));   // a fixed value, never LocalTime.now()
        chips.getChildren().add(now);

        root = new VBox(10, header, face, chips, summary);
        root.setAlignment(Pos.CENTER);
        refresh();
    }

    Parent node() {
        return root;
    }

    LocalTime time() {
        return LocalTime.of(h, m, s);
    }

    /** Whether the person picked anything — the dial, a key, the seconds box, a chip or Now. */
    boolean touched() {
        return touched;
    }

    private void switchTo(Mode next) {
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
        hours.setText(String.format("%02d", h));
        minutes.setText(String.format("%02d", m));
        hours.setStyle(mode == Mode.HOUR ? "-fx-font-weight: bold; -fx-underline: true;" : "");
        minutes.setStyle(mode == Mode.MINUTE ? "-fx-font-weight: bold; -fx-underline: true;" : "");
        refreshing = true;
        seconds.getValueFactory().setValue(s);
        refreshing = false;
        summary.setText("= " + TimeText.words(time()));
        draw();
    }

    private void draw() {
        face.getChildren().clear();
        Circle rim = new Circle(C, C, R);
        rim.setStyle("-fx-fill: transparent; -fx-stroke: -bm-divider; -fx-stroke-width: 1;");
        face.getChildren().add(rim);

        double angle = mode == Mode.HOUR ? ClockDial.hourAngle(h) : ClockDial.minuteAngle(m);
        double reach = (mode == Mode.HOUR && ClockDial.inner(h) ? INNER_AT : OUTER_AT) * R;
        double[] tip = point(angle, reach);
        Line hand = new Line(C, C, tip[0], tip[1]);
        hand.setStyle("-fx-stroke: -fx-accent; -fx-stroke-width: 2;");
        Circle knob = new Circle(tip[0], tip[1], 13);
        knob.setStyle("-fx-fill: -fx-accent; -fx-opacity: 0.35;");
        face.getChildren().addAll(hand, knob);

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
        label.setMouseTransparent(true);
        label.setAlignment(Pos.CENTER);
        label.setMinSize(28, 20);
        label.setPrefSize(28, 20);
        if (small) label.setStyle("-fx-font-size: 0.85em; -fx-opacity: 0.8;");
        double[] p = point(angle, distance);
        label.relocate(p[0] - 14, p[1] - 10);
        face.getChildren().add(label);
    }

    private static double[] point(double angle, double distance) {
        double rad = Math.toRadians(angle);
        return new double[]{C + Math.sin(rad) * distance, C - Math.cos(rad) * distance};
    }
}
