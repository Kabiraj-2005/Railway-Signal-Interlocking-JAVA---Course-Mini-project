package com.trains.interlocking.service;

import com.trains.interlocking.model.Point;
import com.trains.interlocking.model.Route;
import com.trains.interlocking.model.Signal;
import com.trains.interlocking.model.TrackElement;
import com.trains.interlocking.model.TrackSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the Two-Platform Demo Station in memory (same data as the seed rows in schema.sql).
 *
 * <pre>
 *               T3 (loop platform)
 *           /----------------------\
 *  T1 ---- P1 ------ T2 (main) ----- P2 ---- T4
 *  [SIG_W]                                [SIG_E]
 * </pre>
 */
public final class StationFactory {

    private StationFactory() {
    }

    public static Map<String, TrackElement> createDemoLayout() {
        Map<String, TrackElement> layout = new LinkedHashMap<>();
        add(layout, new TrackSection("T1", "West approach"));
        add(layout, new TrackSection("T2", "Main platform"));
        add(layout, new TrackSection("T3", "Loop platform"));
        add(layout, new TrackSection("T4", "East approach"));
        add(layout, new Point("P1"));
        add(layout, new Point("P2"));
        add(layout, new Signal("SIG_W"));
        add(layout, new Signal("SIG_E"));
        return layout;
    }

    public static List<Route> createDemoRoutes(Map<String, TrackElement> layout) {
        List<Route> routes = new ArrayList<>();
        routes.add(route(layout, "R1", "West -> Main", "SIG_W", "T1", "T2", "P1", false));
        routes.add(route(layout, "R2", "West -> Loop", "SIG_W", "T1", "T3", "P1", true));
        routes.add(route(layout, "R3", "East -> Main", "SIG_E", "T4", "T2", "P2", false));
        routes.add(route(layout, "R4", "East -> Loop", "SIG_E", "T4", "T3", "P2", true));
        return routes;
    }

    /** Convenience: a ready-to-use in-memory service for the demo station. */
    public static InterlockingService createDemoService() {
        Map<String, TrackElement> layout = createDemoLayout();
        return new InterlockingService(layout, createDemoRoutes(layout), null);
    }

    private static void add(Map<String, TrackElement> layout, TrackElement e) {
        layout.put(e.getId(), e);
    }

    private static Route route(Map<String, TrackElement> layout, String id, String description,
                               String signalId, String firstSection, String secondSection,
                               String pointId, boolean reversed) {
        List<TrackSection> sections = new ArrayList<>();
        sections.add((TrackSection) layout.get(firstSection));
        sections.add((TrackSection) layout.get(secondSection));
        Map<Point, Boolean> points = new HashMap<>();
        points.put((Point) layout.get(pointId), reversed);
        return new Route(id, description, (Signal) layout.get(signalId), sections, points);
    }
}
