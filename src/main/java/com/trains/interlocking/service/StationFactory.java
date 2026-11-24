/*
 * StationFactory.java
 * Builds the demo station in memory (same data as the seed rows in schema.sql):
 *
 *               T3 (loop platform)
 *           /----------------------\
 *  T1 ---- P1 ------ T2 (main) ----- P2 ---- T4
 *  [SIG_W]                                [SIG_E]
 *
 * Used by the test harness, and by the GUI if the database cannot be opened.
 * Lecture decks: static methods, Collections (HashMap, ArrayList), casting.
 * Owner: Utpal
 */
package com.trains.interlocking.service;

import com.trains.interlocking.model.Point;
import com.trains.interlocking.model.Route;
import com.trains.interlocking.model.Signal;
import com.trains.interlocking.model.TrackElement;
import com.trains.interlocking.model.TrackSection;

import java.util.ArrayList;
import java.util.HashMap;

public class StationFactory {

    public static HashMap<String, TrackElement> createDemoLayout() {
        HashMap<String, TrackElement> layout = new HashMap<>();
        layout.put("T1", new TrackSection("T1"));
        layout.put("T2", new TrackSection("T2"));
        layout.put("T3", new TrackSection("T3"));
        layout.put("T4", new TrackSection("T4"));
        layout.put("P1", new Point("P1"));
        layout.put("P2", new Point("P2"));
        layout.put("SIG_W", new Signal("SIG_W"));
        layout.put("SIG_E", new Signal("SIG_E"));
        return layout;
    }

    public static ArrayList<Route> createDemoRoutes(HashMap<String, TrackElement> layout) {
        ArrayList<Route> routes = new ArrayList<>();
        routes.add(makeRoute(layout, "R1", "West -> Main", "SIG_W", "T1", "T2", "P1", false));
        routes.add(makeRoute(layout, "R2", "West -> Loop", "SIG_W", "T1", "T3", "P1", true));
        routes.add(makeRoute(layout, "R3", "East -> Main", "SIG_E", "T4", "T2", "P2", false));
        routes.add(makeRoute(layout, "R4", "East -> Loop", "SIG_E", "T4", "T3", "P2", true));
        return routes;
    }

    /** A ready-to-use service with no database (state lives only in memory). */
    public static InterlockingService createDemoService() {
        HashMap<String, TrackElement> layout = createDemoLayout();
        return new InterlockingService(layout, createDemoRoutes(layout), null);
    }

    private static Route makeRoute(HashMap<String, TrackElement> layout, String id, String description,
                                   String signalId, String firstSection, String secondSection,
                                   String pointId, boolean reversed) {
        // Take the objects FROM the layout, so routes and layout share the same objects.
        ArrayList<TrackSection> sections = new ArrayList<>();
        sections.add((TrackSection) layout.get(firstSection));
        sections.add((TrackSection) layout.get(secondSection));

        HashMap<Point, Boolean> points = new HashMap<>();
        points.put((Point) layout.get(pointId), reversed);

        Signal signal = (Signal) layout.get(signalId);
        return new Route(id, description, signal, sections, points);
    }
}
