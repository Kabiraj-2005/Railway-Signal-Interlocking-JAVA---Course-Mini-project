package com.trains.interlocking;

import com.trains.interlocking.dao.StationRepository;
import com.trains.interlocking.exception.PointLockedException;
import com.trains.interlocking.exception.RouteConflictException;
import com.trains.interlocking.exception.SectionOccupiedException;
import com.trains.interlocking.model.Point;
import com.trains.interlocking.model.Route;
import com.trains.interlocking.model.Signal;
import com.trains.interlocking.model.TrackElement;
import com.trains.interlocking.model.TrackSection;
import com.trains.interlocking.service.InterlockingService;
import com.trains.interlocking.service.StationFactory;
import com.trains.interlocking.util.DatabaseConnection;
import com.trains.interlocking.util.LogConfig;

import java.io.File;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * Console test harness (no JUnit in the syllabus). Runs every interlocking scenario
 * against a fresh in-memory station, then serialization and database round trips.
 * Exits with status 1 if any check fails.
 */
public class Main {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        LogConfig.configure();
        testCompatibleRoutes();
        testConflicts();
        testTrainMovement();
        testCancel();
        testOccupiedSection();
        testPointLocking();
        testSerialization();
        testDatabase();

        System.out.println();
        System.out.println("Passed: " + passed + "   Failed: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ----------------------------------------------------------------- scenarios

    private static void testCompatibleRoutes() throws Exception {
        section("Compatible routes R1 + R4 and R2 + R3");
        InterlockingService s = StationFactory.createDemoService();
        s.setRoute("R1");
        s.setRoute("R4");
        check("two routes active", s.getActiveRoutes().size() == 2);
        check("P1 normal and locked", !point(s, "P1").isReversed() && point(s, "P1").isLocked());
        check("P2 reverse and locked", point(s, "P2").isReversed() && point(s, "P2").isLocked());
        check("SIG_W green", signal(s, "SIG_W").isClear());
        check("SIG_E green", signal(s, "SIG_E").isClear());
        check("T1..T4 all locked", sec(s, "T1").isLocked() && sec(s, "T2").isLocked()
                && sec(s, "T3").isLocked() && sec(s, "T4").isLocked());

        InterlockingService s2 = StationFactory.createDemoService();
        s2.setRoute("R2");
        s2.setRoute("R3");
        check("R2 + R3 both active", s2.getActiveRoutes().size() == 2);
    }

    private static void testConflicts() throws Exception {
        section("Conflicting routes");
        InterlockingService s = StationFactory.createDemoService();
        s.setRoute("R1");
        expectConflict(s, "R3", "R1-R3 share T2");
        expectConflict(s, "R2", "R1-R2 need P1 in different positions");
        expectConflict(s, "R1", "R1 cannot be set twice");

        InterlockingService s2 = StationFactory.createDemoService();
        s2.setRoute("R4");
        expectConflict(s2, "R2", "R2-R4 share T3");
        expectConflict(s2, "R3", "R3-R4 need P2 in different positions");
        Route r1 = s.getRoute("R1");
        Route r2 = s.getRoute("R2");
        check("Route.conflictWith reports R1/R2 conflict", r1.conflictWith(r2) != null);
        check("Route.conflictWith: R1/R4 compatible", r1.conflictWith(s.getRoute("R4")) == null);
        check("refusals do not change state", s2.getActiveRoutes().size() == 1
                && !signal(s2, "SIG_W").isClear());
    }

    private static void testTrainMovement() throws Exception {
        section("Train runs over R2 (West -> Loop)");
        InterlockingService s = StationFactory.createDemoService();
        s.setRoute("R2");

        s.moveTrain("R2");
        check("signal red once the train enters", !signal(s, "SIG_W").isClear());
        check("T1 occupied", sec(s, "T1").isOccupied());
        check("route still active", s.getRoute("R2").isActive());

        s.moveTrain("R2");
        check("T1 clear and released", !sec(s, "T1").isOccupied() && !sec(s, "T1").isLocked());
        check("T3 occupied and locked", sec(s, "T3").isOccupied() && sec(s, "T3").isLocked());
        check("P1 still locked", point(s, "P1").isLocked());

        s.setRoute("R3");
        check("R3 can be set behind the train (T1 released, T2 free)", s.getRoute("R3").isActive());

        s.moveTrain("R2");
        check("T3 clear and released", !sec(s, "T3").isOccupied() && !sec(s, "T3").isLocked());
        check("R2 no longer active", !s.getRoute("R2").isActive());
        check("P1 released", !point(s, "P1").isLocked());
        check("only R3 active", s.getActiveRoutes().size() == 1);

        expectIllegalState("moving a train on a route that is not set", () -> s.moveTrain("R2"));
    }

    private static void testCancel() throws Exception {
        section("Cancelling routes");
        InterlockingService s = StationFactory.createDemoService();
        s.setRoute("R1");
        s.cancelRoute("R1");
        check("R1 inactive", !s.getRoute("R1").isActive());
        check("SIG_W red", !signal(s, "SIG_W").isClear());
        check("T1, T2, P1 released", !sec(s, "T1").isLocked() && !sec(s, "T2").isLocked()
                && !point(s, "P1").isLocked());
        s.setRoute("R2");
        check("conflicting R2 can now be set", s.getRoute("R2").isActive());

        s.moveTrain("R2");
        expectIllegalState("cancel with a train on the route", () -> s.cancelRoute("R2"));
        expectIllegalState("cancel a route that is not set", () -> s.cancelRoute("R4"));
    }

    private static void testOccupiedSection() throws Exception {
        section("Occupied sections");
        InterlockingService s = StationFactory.createDemoService();
        s.setSectionOccupied("T2", true);
        try {
            s.setRoute("R1");
            check("R1 refused while T2 occupied", false);
        } catch (SectionOccupiedException e) {
            check("R1 refused while T2 occupied: " + e.getMessage(), true);
        }
        s.setRoute("R4");
        check("R4 does not use T2 and can be set", s.getRoute("R4").isActive());
        expectIllegalState("cannot fake occupancy of a locked section",
                () -> s.setSectionOccupied("T3", true));
    }

    private static void testPointLocking() throws Exception {
        section("Point locking");
        InterlockingService s = StationFactory.createDemoService();
        s.throwPoint("P1");
        check("free point can be thrown", point(s, "P1").isReversed());
        s.throwPoint("P1");

        s.setRoute("R1");
        try {
            s.throwPoint("P1");
            check("locked point refuses to move", false);
        } catch (PointLockedException e) {
            check("locked point refuses to move: " + e.getMessage(), true);
        }
        try {
            point(s, "P1").setReversed(true);
            check("Point.setReversed refuses when locked", false);
        } catch (PointLockedException e) {
            check("Point.setReversed refuses when locked", true);
        }

        InterlockingService s2 = StationFactory.createDemoService();
        s2.setSectionOccupied("T1", true); // P1 lies in T1
        try {
            s2.throwPoint("P1");
            check("point under a train cannot move", false);
        } catch (PointLockedException e) {
            check("point under a train cannot move: " + e.getMessage(), true);
        }
        s2.throwPoint("P2");
        check("train in T1 does not block P2", point(s2, "P2").isReversed());
    }

    private static void testSerialization() throws Exception {
        section("Serialization round trip");
        InterlockingService s = StationFactory.createDemoService();
        s.setRoute("R2");
        s.moveTrain("R2");
        File file = File.createTempFile("interlocking", ".ser");
        file.deleteOnExit();
        s.saveSnapshot(file);

        InterlockingService restored = StationFactory.createDemoService();
        restored.loadSnapshot(file);
        Route r2 = restored.getRoute("R2");
        check("R2 active after reload", r2.isActive());
        check("train still in T1", r2.getTrainPosition() == 0 && sec(restored, "T1").isOccupied());
        check("route refers to the same section objects as the layout",
                r2.getSections().get(0) == restored.getLayout().get("T1"));
        expectRefused(restored, "R1", "R1 still refused after reload");
        restored.moveTrain("R2");
        restored.moveTrain("R2");
        check("train completes after reload", !restored.getRoute("R2").isActive());
    }

    private static void testDatabase() throws Exception {
        section("Database round trip (temporary SQLite file)");
        File dbFile = File.createTempFile("interlocking-test", ".db");
        dbFile.deleteOnExit();
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        try (Connection conn = DatabaseConnection.getConnection(url)) {
            DatabaseConnection.initialiseSchema(conn);
            StationRepository repo = new StationRepository(conn);
            Map<String, TrackElement> layout = repo.loadLayout();
            List<Route> routes = repo.loadRoutes(layout);
            check("8 elements seeded", layout.size() == 8);
            check("4 routes seeded", routes.size() == 4);

            InterlockingService s = new InterlockingService(layout, routes, repo);
            s.setRoute("R4");
            s.moveTrain("R4");
            check("events written to the database", repo.recentEvents(10).size() == 2);
        }

        try (Connection conn = DatabaseConnection.getConnection(url)) {
            DatabaseConnection.initialiseSchema(conn); // must not reset existing state
            StationRepository repo = new StationRepository(conn);
            Map<String, TrackElement> layout = repo.loadLayout();
            InterlockingService s = new InterlockingService(layout, repo.loadRoutes(layout), repo);
            check("R4 still active after reopening", s.getRoute("R4").isActive());
            check("train still in T4", sec(s, "T4").isOccupied() && sec(s, "T4").isLocked());
            check("P2 still reverse and locked", point(s, "P2").isReversed() && point(s, "P2").isLocked());
            expectRefused(s, "R3", "R3 refused after reopening");
            s.moveTrain("R4");
            s.moveTrain("R4");
            check("route completes after reopening", s.getActiveRoutes().isEmpty());
        }
    }

    // ----------------------------------------------------------------- helpers

    private interface Action {
        void run() throws Exception;
    }

    private static void expectConflict(InterlockingService s, String routeId, String label) {
        try {
            s.setRoute(routeId);
            check(label, false);
        } catch (RouteConflictException e) {
            check(label + ": " + e.getMessage(), true);
        } catch (Exception e) {
            check(label + " (wrong exception " + e + ")", false);
        }
    }

    /** Any interlocking refusal (conflict, occupied section, locked point) is accepted. */
    private static void expectRefused(InterlockingService s, String routeId, String label) {
        try {
            s.setRoute(routeId);
            check(label, false);
        } catch (RouteConflictException | SectionOccupiedException | PointLockedException e) {
            check(label + ": " + e.getMessage(), true);
        }
    }

    private static void expectIllegalState(String label, Action action) {
        try {
            action.run();
            check(label + " is refused", false);
        } catch (IllegalStateException e) {
            check(label + " is refused: " + e.getMessage(), true);
        } catch (Exception e) {
            check(label + " (wrong exception " + e + ")", false);
        }
    }

    private static void check(String label, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("  PASS  " + label);
        } else {
            failed++;
            System.out.println("  FAIL  " + label);
        }
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("== " + title);
    }

    private static TrackSection sec(InterlockingService s, String id) {
        return (TrackSection) s.getLayout().get(id);
    }

    private static Point point(InterlockingService s, String id) {
        return (Point) s.getLayout().get(id);
    }

    private static Signal signal(InterlockingService s, String id) {
        return (Signal) s.getLayout().get(id);
    }
}
