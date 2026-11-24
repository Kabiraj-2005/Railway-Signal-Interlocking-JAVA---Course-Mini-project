package com.trains.interlocking.ui;

import com.trains.interlocking.model.Point;
import com.trains.interlocking.model.Signal;
import com.trains.interlocking.model.TrackElement;
import com.trains.interlocking.model.TrackSection;
import com.trains.interlocking.service.InterlockingService;
import javafx.scene.Cursor;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.HashMap;
import java.util.Map;

/**
 * Schematic of the Two-Platform Demo Station drawn with JavaFX shapes.
 * Clicking a section toggles its occupancy; clicking a point throws it.
 *
 * <pre>
 *               T3 (loop platform)
 *           /----------------------\
 *  T1 ---- P1 ------ T2 (main) ----- P2 ---- T4
 *  [SIG_W]                                [SIG_E]
 * </pre>
 */
public final class TrackDiagram extends Pane {

    public static final Color FREE = Color.web("#9aa0a6");
    public static final Color LOCKED = Color.web("#2e7d32");
    public static final Color OCCUPIED = Color.web("#d32f2f");
    private static final Color INACTIVE_LEG = Color.web("#dadce0");
    private static final Color TEXT = Color.web("#202124");

    private static final double MAIN_Y = 160;
    private static final double LOOP_Y = 70;

    /** Callbacks the diagram uses to ask the main window to act. */
    public interface ClickHandler {
        void sectionClicked(String sectionId);

        void pointClicked(String pointId);
    }

    private final Map<String, Shape> sectionShapes = new HashMap<>();
    private final Map<String, Line> normalLegs = new HashMap<>();
    private final Map<String, Line> reverseLegs = new HashMap<>();
    private final Map<String, Circle> signalLamps = new HashMap<>();
    private final Map<String, Text> pointLabels = new HashMap<>();

    public TrackDiagram(ClickHandler handler) {
        setPrefSize(760, 230);
        setMinSize(760, 230);

        Rectangle background = new Rectangle(760, 230, Color.web("#f8f9fa"));
        background.setArcWidth(12);
        background.setArcHeight(12);
        getChildren().add(background);

        // sections
        addSection("T1", new Line(70, MAIN_Y, 210, MAIN_Y), 140, "T1  West approach", handler);
        addSection("T2", new Line(270, MAIN_Y, 490, MAIN_Y), 380, "T2  Main platform", handler);
        addSection("T3", new Polyline(270, MAIN_Y - 40, 300, LOOP_Y, 460, LOOP_Y, 490, MAIN_Y - 40),
                380, "T3  Loop platform", handler);
        addSection("T4", new Line(550, MAIN_Y, 690, MAIN_Y), 620, "T4  East approach", handler);

        // points: the leg towards the main line is "normal", towards the loop "reverse"
        addPoint("P1", new Line(210, MAIN_Y, 270, MAIN_Y), new Line(210, MAIN_Y, 270, MAIN_Y - 40),
                240, handler);
        addPoint("P2", new Line(490, MAIN_Y, 550, MAIN_Y), new Line(490, MAIN_Y - 40, 550, MAIN_Y),
                520, handler);

        // signals sit at the station entrances, facing inwards
        addSignal("SIG_W", 45);
        addSignal("SIG_E", 715);
    }

    private void addSection(String id, Shape shape, double labelX, String label, ClickHandler handler) {
        shape.setStrokeWidth(9);
        shape.setStrokeLineCap(StrokeLineCap.ROUND);
        shape.setFill(null);
        shape.setCursor(Cursor.HAND);
        shape.setOnMouseClicked(e -> handler.sectionClicked(id));
        sectionShapes.put(id, shape);
        getChildren().add(shape);

        Text text = new Text(label);
        text.setFont(Font.font(12));
        text.setFill(TEXT);
        text.setX(labelX - text.getLayoutBounds().getWidth() / 2);
        text.setY("T3".equals(id) ? LOOP_Y - 14 : MAIN_Y + 30);
        getChildren().add(text);
    }

    private void addPoint(String id, Line normal, Line reverse, double labelX, ClickHandler handler) {
        for (Line leg : new Line[]{normal, reverse}) {
            leg.setStrokeLineCap(StrokeLineCap.ROUND);
            leg.setCursor(Cursor.HAND);
            leg.setOnMouseClicked(e -> handler.pointClicked(id));
            getChildren().add(leg);
        }
        normalLegs.put(id, normal);
        reverseLegs.put(id, reverse);

        Text text = new Text(id);
        text.setFont(Font.font("System", FontWeight.BOLD, 12));
        text.setFill(TEXT);
        text.setX(labelX - 30);
        text.setY(MAIN_Y + 52);
        text.setCursor(Cursor.HAND);
        text.setOnMouseClicked(e -> handler.pointClicked(id));
        pointLabels.put(id, text);
        getChildren().add(text);
    }

    private void addSignal(String id, double x) {
        Line post = new Line(x, MAIN_Y - 8, x, MAIN_Y - 40);
        post.setStrokeWidth(3);
        post.setStroke(TEXT);
        Rectangle head = new Rectangle(x - 13, MAIN_Y - 70, 26, 30);
        head.setArcWidth(8);
        head.setArcHeight(8);
        head.setFill(Color.web("#3c4043"));
        Circle lamp = new Circle(x, MAIN_Y - 55, 9);
        signalLamps.put(id, lamp);

        Text text = new Text(id);
        text.setFont(Font.font("System", FontWeight.BOLD, 12));
        text.setFill(TEXT);
        text.setX(x - text.getLayoutBounds().getWidth() / 2);
        text.setY(MAIN_Y + 30);
        getChildren().addAll(post, head, lamp, text);
    }

    /** Recolours every element from the current service state. */
    public void refresh(InterlockingService service) {
        for (TrackElement e : service.getLayout().values()) {
            if (e instanceof TrackSection) {
                TrackSection s = (TrackSection) e;
                Shape shape = sectionShapes.get(s.getId());
                if (shape != null) {
                    shape.setStroke(s.isOccupied() ? OCCUPIED : s.isLocked() ? LOCKED : FREE);
                }
            } else if (e instanceof Point) {
                Point p = (Point) e;
                Line active = p.isReversed() ? reverseLegs.get(p.getId()) : normalLegs.get(p.getId());
                Line inactive = p.isReversed() ? normalLegs.get(p.getId()) : reverseLegs.get(p.getId());
                if (active != null) {
                    active.setStroke(p.isLocked() ? LOCKED : Color.web("#5f6368"));
                    active.setStrokeWidth(9);
                    active.toFront();
                    inactive.setStroke(INACTIVE_LEG);
                    inactive.setStrokeWidth(4);
                    pointLabels.get(p.getId()).setText(p.getId() + " "
                            + (p.isReversed() ? "R" : "N") + (p.isLocked() ? " (locked)" : ""));
                }
            } else if (e instanceof Signal) {
                Signal sig = (Signal) e;
                Circle lamp = signalLamps.get(sig.getId());
                if (lamp != null) {
                    lamp.setFill(sig.isClear() ? Color.web("#34a853") : Color.web("#ea4335"));
                }
            }
        }
    }
}
