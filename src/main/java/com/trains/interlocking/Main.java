/*
 * Main.java
 * Console test harness (JUnit is not part of our course). Each test builds a fresh
 * station, tries one scenario, and prints PASS or FAIL. At the end it prints the
 * total, e.g. "15/15 passed".
 * Lecture decks: static methods, Exception Handling (try/catch), File I/O, JDBC.
 * Owner: Utpal
 */
package com.trains.interlocking;

import com.trains.interlocking.dao.StationRepository;
import com.trains.interlocking.dao.TrackSectionDao;
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
import java.util.HashMap;

public class Main {

    private static int passed = 0;
    private static int total = 0;

    public static void main(String[] args) {
        LogConfig.configure();
        testR1AndR4BothSettable();
        testR1ThenR3Refused();
        testR1ThenR2Refused();
        testR4ThenR2Refused();
        testR1TwiceRefused();
        testTrainEnters();
        testTrainSecondMove();
        testCancelWithTrainRefused();
        testCancelUnsetRouteRefused();
        testOccupiedT2BlocksR1();
        testLockedPointRefusesToMove();
        testPointWithTrainOverItRefuses();
        testCannotMarkLockedSectionOccupied();
        testSnapshotKeepsState();
        testDatabaseKeepsState();
        System.out.println();
        System.out.println(passed + "/" + total + " passed");
    }

    private static void testR1AndR4BothSettable() {
        InterlockingService service = StationFactory.createDemoService();
        try {
            service.setRoute("R1");
            service.setRoute("R4");
            report("1. R1 and R4 can both be set", service.getActiveRoutes().size() == 2);
        } catch (Exception e) {
            report("1. R1 and R4 can both be set (" + e.getMessage() + ")", false);
        }
    }

    private static void testR1ThenR3Refused() {
        expectConflict("2. R1 then R3 refused (they share T2)", "R1", "R3");
    }

    private static void testR1ThenR2Refused() {
        expectConflict("3. R1 then R2 refused", "R1", "R2");
    }

    private static void testR4ThenR2Refused() {
        expectConflict("4. R4 then R2 refused (they share T3)", "R4", "R2");
    }

    private static void testR1TwiceRefused() {
        expectConflict("5. R1 cannot be set twice", "R1", "R1");
    }

    private static void testTrainEnters() {
        InterlockingService service = StationFactory.createDemoService();
        try {
            service.setRoute("R2");
            service.moveTrain("R2");
            boolean signalRed = !((Signal) service.getLayout().get("SIG_W")).isClear();
            boolean t1Occupied = ((TrackSection) service.getLayout().get("T1")).isOccupied();
            report("6. Train enters: SIG_W red, T1 occupied", signalRed && t1Occupied);
        } catch (Exception e) {
            report("6. Train enters (" + e.getMessage() + ")", false);
        }
    }

    private static void testTrainSecondMove() {
        InterlockingService service = StationFactory.createDemoService();
        try {
            service.setRoute("R2");
            service.moveTrain("R2");
            service.moveTrain("R2");
            TrackSection t1 = (TrackSection) service.getLayout().get("T1");
            boolean t1Released = !t1.isOccupied() && !t1.isLocked();
            boolean t3Occupied = ((TrackSection) service.getLayout().get("T3")).isOccupied();
            service.setRoute("R3"); // T4 and T2 are free now, so this must work
            report("7. Second move: T1 released, T3 occupied, R3 settable", t1Released && t3Occupied);
        } catch (Exception e) {
            report("7. Second move (" + e.getMessage() + ")", false);
        }
    }

    private static void testCancelWithTrainRefused() {
        InterlockingService service = StationFactory.createDemoService();
        try {
            service.setRoute("R1");
            service.moveTrain("R1");
            service.cancelRoute("R1");
            report("8. Cancel with a train on the route is refused", false);
        } catch (RouteConflictException e) {
            report("8. Cancel with a train on the route is refused", e.getMessage().contains("train"));
        } catch (Exception e) {
            report("8. Cancel with train (" + e.getMessage() + ")", false);
        }
    }

    private static void testCancelUnsetRouteRefused() {
        InterlockingService service = StationFactory.createDemoService();
        try {
            service.cancelRoute("R4");
            report("9. Cancelling a route that is not set is refused", false);
        } catch (RouteConflictException e) {
            report("9. Cancelling a route that is not set is refused", true);
        }
    }

    private static void testOccupiedT2BlocksR1() {
        InterlockingService service = StationFactory.createDemoService();
        try {
            service.setSectionOccupied("T2", true);
            service.setRoute("R1");
            report("10. Occupied T2 blocks R1", false);
        } catch (SectionOccupiedException e) {
            report("10. Occupied T2 blocks R1", e.getMessage().contains("T2"));
        } catch (Exception e) {
            report("10. Occupied T2 blocks R1 (" + e.getMessage() + ")", false);
        }
    }

    private static void testLockedPointRefusesToMove() {
        InterlockingService service = StationFactory.createDemoService();
        try {
            service.setRoute("R1"); // locks P1
            service.throwPoint("P1");
            report("11. Locked point refuses to move", false);
        } catch (PointLockedException e) {
            report("11. Locked point refuses to move", true);
        } catch (Exception e) {
            report("11. Locked point (" + e.getMessage() + ")", false);
        }
    }

    private static void testPointWithTrainOverItRefuses() {
        InterlockingService service = StationFactory.createDemoService();
        try {
            service.setSectionOccupied("T1", true); // P1 lies in T1
            service.throwPoint("P1");
            report("12. Point with a train over it refuses to move", false);
        } catch (PointLockedException e) {
            report("12. Point with a train over it refuses to move", true);
        } catch (Exception e) {
            report("12. Point with train over it (" + e.getMessage() + ")", false);
        }
    }

    private static void testCannotMarkLockedSectionOccupied() {
        InterlockingService service = StationFactory.createDemoService();
        try {
            service.setRoute("R4"); // locks T4 and T3
            service.setSectionOccupied("T3", true);
            report("13. A locked section cannot be marked occupied", false);
        } catch (SectionOccupiedException e) {
            report("13. A locked section cannot be marked occupied", true);
        } catch (Exception e) {
            report("13. Locked section (" + e.getMessage() + ")", false);
        }
    }

    private static void testSnapshotKeepsState() {
        try {
            File file = File.createTempFile("interlocking", ".ser");
            file.deleteOnExit();
            InterlockingService original = StationFactory.createDemoService();
            original.setRoute("R2");
            original.moveTrain("R2");
            original.saveSnapshot(file.getPath());

            InterlockingService restored = StationFactory.createDemoService();
            restored.loadSnapshot(file.getPath());
            Route r2 = restored.getRoute("R2");
            // After loading, the route must point at the very same T1 object as the layout.
            boolean sameObject = r2.getSections().get(0) == restored.getLayout().get("T1");
            boolean ok = r2.isActive() && ((TrackSection) restored.getLayout().get("T1")).isOccupied() && sameObject;
            report("14. Snapshot save/load keeps the state", ok);
        } catch (Exception e) {
            report("14. Snapshot (" + e.getMessage() + ")", false);
        }
    }

    private static void testDatabaseKeepsState() {
        try {
            File dbFile = File.createTempFile("interlocking-test", ".db");
            dbFile.deleteOnExit();
            String url = "jdbc:sqlite:" + dbFile.getPath();

            // First "run" of the program: set R4 and move the train into T4.
            try (Connection conn = DatabaseConnection.getConnection(url)) {
                InterlockingService service = openService(conn);
                service.setRoute("R4");
                service.moveTrain("R4");
            }
            // Second "run": a brand new connection must see the same state.
            try (Connection conn = DatabaseConnection.getConnection(url)) {
                InterlockingService service = openService(conn);
                Point p2 = (Point) service.getLayout().get("P2");
                boolean routeKept = service.getRoute("R4").isActive();
                boolean pointKept = p2.isReversed() && p2.isLocked();
                boolean rowKept = new TrackSectionDao(conn).findById("T4").get().isOccupied();
                report("15. Database close/reopen keeps the state", routeKept && pointKept && rowKept);
            }
        } catch (Exception e) {
            report("15. Database (" + e.getMessage() + ")", false);
        }
    }

    /** Sets firstRoute, then expects secondRoute to be refused with a RouteConflictException. */
    private static void expectConflict(String name, String firstRoute, String secondRoute) {
        InterlockingService service = StationFactory.createDemoService();
        try {
            service.setRoute(firstRoute);
            service.setRoute(secondRoute);
            report(name, false); // we should never get here
        } catch (RouteConflictException e) {
            report(name, true);
        } catch (Exception e) {
            report(name + " (wrong exception: " + e.getMessage() + ")", false);
        }
    }

    private static InterlockingService openService(Connection conn) throws Exception {
        DatabaseConnection.initialiseSchema(conn);
        StationRepository repository = new StationRepository(conn);
        HashMap<String, TrackElement> layout = repository.loadLayout();
        return new InterlockingService(layout, repository.loadRoutes(layout), repository);
    }

    private static void report(String name, boolean ok) {
        total++;
        if (ok) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            System.out.println("FAIL  " + name);
        }
    }
}
