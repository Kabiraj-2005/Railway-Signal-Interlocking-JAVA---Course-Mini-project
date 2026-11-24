/*
 * MainApp.java
 * The signaller's window. It shows the track as a row of coloured labels, the routes,
 * and the event log, with buttons for every action. It only talks to InterlockingService;
 * when the service refuses something, the exception message is shown in an Alert.
 * Lecture deck: JavaFX (Application init/start/stop, Stage, Scene, layouts, controls, events).
 * Owner: Aryan
 */
package com.trains.interlocking.ui;

import com.trains.interlocking.dao.StationRepository;
import com.trains.interlocking.model.EventLogEntry;
import com.trains.interlocking.model.Route;
import com.trains.interlocking.model.TrackElement;
import com.trains.interlocking.service.InterlockingService;
import com.trains.interlocking.service.StationFactory;
import com.trains.interlocking.util.AppConfig;
import com.trains.interlocking.util.DatabaseConnection;
import com.trains.interlocking.util.LogConfig;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class MainApp extends Application {

    // The order the track is drawn in, from west to east. T3 is the loop beside T2.
    private static final String[] TRACK_ORDER = {"SIG_W", "T1", "P1", "T2", "T3", "P2", "T4", "SIG_E"};
    private static final String[] SECTIONS = {"T1", "T2", "T3", "T4"};
    private static final String[] POINTS = {"P1", "P2"};

    private Connection connection; // null when running without a database
    private InterlockingService service;
    private String storageMessage;

    private final HashMap<String, Label> trackLabels = new HashMap<>();
    private final ComboBox<String> routeBox = new ComboBox<>();
    private final ComboBox<String> pointBox = new ComboBox<>();
    private final ComboBox<String> sectionBox = new ComboBox<>();
    private final TextField snapshotField = new TextField("station.ser");
    private final ListView<String> routeList = new ListView<>();
    private final ListView<String> logList = new ListView<>();
    private final Label statusLabel = new Label("Ready");

    /** Runs before the window exists: open the database and load the station. */
    @Override
    public void init() {
        LogConfig.configure();
        try {
            connection = DatabaseConnection.getConnection();
            DatabaseConnection.initialiseSchema(connection);
            StationRepository repository = new StationRepository(connection);
            HashMap<String, TrackElement> layout = repository.loadLayout();
            service = new InterlockingService(layout, repository.loadRoutes(layout), repository);
            service.addHistory(repository.recentEvents(100));
            storageMessage = "Saved in " + AppConfig.getDbUrl();
        } catch (SQLException e) {
            // Without a database the app still works, it just forgets on exit.
            service = StationFactory.createDemoService();
            storageMessage = "Database not available, running in memory only";
        }
    }

    /** Builds the window. */
    @Override
    public void start(Stage stage) {
        Label title = new Label("Railway Signal Interlocking - " + AppConfig.getStationName());
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        VBox top = new VBox(4, title, new Label(storageMessage));

        Label legend = new Label("Grey = free     Yellow = locked by a route     Red = occupied"
                + "     Signals: green / red");
        VBox centre = new VBox(10, buildTrackRow(), legend,
                new Label("Routes"), routeList, new Label("Event log"), logList);
        centre.setPadding(new Insets(10, 10, 0, 0));

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));
        root.setTop(top);
        root.setCenter(centre);
        root.setRight(buildControls());

        refresh();
        stage.setTitle("Railway Interlocking");
        stage.setScene(new Scene(root, 1250, 720));
        stage.show();
    }

    /** Runs when the window closes: release the database file. */
    @Override
    public void stop() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.out.println("Could not close the database: " + e.getMessage());
            }
        }
    }

    private HBox buildTrackRow() {
        HBox row = new HBox(6);
        for (String id : TRACK_ORDER) {
            Label label = new Label();
            label.setMinWidth(112);
            label.setMinHeight(60);
            label.setWrapText(true);
            trackLabels.put(id, label);
            row.getChildren().add(label);
        }
        return row;
    }

    private VBox buildControls() {
        for (Route route : service.getRoutes()) {
            routeBox.getItems().add(route.getId());
        }
        routeBox.setValue("R1");
        Button setButton = new Button("Set route");
        setButton.setOnAction(e -> onSetRoute());
        Button cancelButton = new Button("Cancel route");
        cancelButton.setOnAction(e -> onCancelRoute());
        Button moveButton = new Button("Move train one section");
        moveButton.setOnAction(e -> onMoveTrain());

        pointBox.getItems().addAll(POINTS);
        pointBox.setValue("P1");
        Button throwButton = new Button("Throw point");
        throwButton.setOnAction(e -> onThrowPoint());

        sectionBox.getItems().addAll(SECTIONS);
        sectionBox.setValue("T1");
        Button occupiedButton = new Button("Mark occupied");
        occupiedButton.setOnAction(e -> onMarkSection(true));
        Button clearButton = new Button("Mark clear");
        clearButton.setOnAction(e -> onMarkSection(false));

        Button saveButton = new Button("Save snapshot");
        saveButton.setOnAction(e -> onSaveSnapshot());
        Button loadButton = new Button("Load snapshot");
        loadButton.setOnAction(e -> onLoadSnapshot());

        statusLabel.setWrapText(true);
        VBox controls = new VBox(8,
                new Label("Route"), routeBox, setButton, cancelButton, moveButton,
                new Label("Point"), pointBox, throwButton,
                new Label("Track section"), sectionBox, new HBox(6, occupiedButton, clearButton),
                new Label("Snapshot file"), snapshotField, new HBox(6, saveButton, loadButton),
                new Label("Last result"), statusLabel);
        controls.setPadding(new Insets(10, 0, 0, 10));
        controls.setMinWidth(280); // wide enough for the two-button rows
        return controls;
    }

    // ---- button handlers: each one calls the service, then redraws or shows the error

    private void onSetRoute() {
        try {
            service.setRoute(routeBox.getValue());
            refresh();
        } catch (Exception e) {
            // setRoute can throw any of our three exceptions; all are shown the same way.
            showError("Route cannot be set", e.getMessage());
        }
    }

    private void onCancelRoute() {
        try {
            service.cancelRoute(routeBox.getValue());
            refresh();
        } catch (Exception e) {
            showError("Route cannot be cancelled", e.getMessage());
        }
    }

    private void onMoveTrain() {
        try {
            service.moveTrain(routeBox.getValue());
            refresh();
        } catch (Exception e) {
            showError("Train cannot move", e.getMessage());
        }
    }

    private void onThrowPoint() {
        try {
            service.throwPoint(pointBox.getValue());
            refresh();
        } catch (Exception e) {
            showError("Point cannot move", e.getMessage());
        }
    }

    private void onMarkSection(boolean occupied) {
        try {
            service.setSectionOccupied(sectionBox.getValue(), occupied);
            refresh();
        } catch (Exception e) {
            showError("Section cannot be changed", e.getMessage());
        }
    }

    private void onSaveSnapshot() {
        try {
            service.saveSnapshot(snapshotField.getText());
            refresh();
        } catch (Exception e) {
            showError("Snapshot not saved", e.getMessage());
        }
    }

    private void onLoadSnapshot() {
        try {
            service.loadSnapshot(snapshotField.getText());
            refresh();
        } catch (Exception e) {
            showError("Snapshot not loaded", e.getMessage());
        }
    }

    // ---- drawing

    /** Copies the current state of the service onto the screen. */
    private void refresh() {
        HashMap<String, TrackElement> layout = service.getLayout();
        for (String id : TRACK_ORDER) {
            TrackElement element = layout.get(id);
            Label label = trackLabels.get(id);
            String name = id;
            if (id.equals("T2")) {
                name = "T2 (main)";
            } else if (id.equals("T3")) {
                name = "T3 (loop)";
            }
            label.setText(name + "\n" + element.getStatus());
            // Each element class decides its own colour (polymorphism, no instanceof needed).
            label.setStyle("-fx-background-color: " + element.getDisplayColour() + ";"
                    + " -fx-padding: 8; -fx-font-weight: bold; -fx-alignment: center;");
        }

        routeList.getItems().clear();
        for (Route route : service.getRoutes()) {
            routeList.getItems().add(route.toString());
        }

        ArrayList<EventLogEntry> events = service.getEventLog();
        logList.getItems().clear();
        for (EventLogEntry entry : events) {
            logList.getItems().add(entry.toString());
        }
        if (!events.isEmpty()) {
            logList.scrollTo(events.size() - 1); // show the newest entry
            statusLabel.setText(events.get(events.size() - 1).getMessage());
        }
    }

    private void showError(String title, String message) {
        refresh(); // the refusal was logged, so show it in the event log too
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Request refused");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
