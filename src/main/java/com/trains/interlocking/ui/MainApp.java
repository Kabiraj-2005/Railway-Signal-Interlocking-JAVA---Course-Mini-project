package com.trains.interlocking.ui;

import com.trains.interlocking.dao.StationRepository;
import com.trains.interlocking.exception.PointLockedException;
import com.trains.interlocking.exception.RouteConflictException;
import com.trains.interlocking.exception.SectionOccupiedException;
import com.trains.interlocking.model.EventLogEntry;
import com.trains.interlocking.model.Route;
import com.trains.interlocking.model.TrackElement;
import com.trains.interlocking.model.TrackSection;
import com.trains.interlocking.service.InterlockingService;
import com.trains.interlocking.service.StationFactory;
import com.trains.interlocking.util.AppConfig;
import com.trains.interlocking.util.DatabaseConnection;
import com.trains.interlocking.util.LogConfig;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Signaller's panel: track diagram, route/point/track controls, state tables and event log.
 * The UI only talks to {@link InterlockingService}.
 */
public class MainApp extends Application {

    private static final Logger LOG = Logger.getLogger(MainApp.class.getName());

    private Connection connection;
    private InterlockingService service;
    private String storageText;

    private TrackDiagram diagram;
    private final ComboBox<Route> routeBox = new ComboBox<>();
    private final ComboBox<String> pointBox = new ComboBox<>();
    private final ComboBox<String> sectionBox = new ComboBox<>();
    private final CheckBox occupiedBox = new CheckBox("Occupied");
    private final TextField snapshotField = new TextField("station.ser");
    private final Label statusLabel = new Label("Ready");
    private final TableView<TrackElement> elementTable = new TableView<>();
    private final TableView<Route> routeTable = new TableView<>();
    private final TableView<EventLogEntry> logTable = new TableView<>();
    private boolean updatingControls;

    @Override
    public void init() {
        LogConfig.configure();
        try {
            connection = DatabaseConnection.getConnection();
            DatabaseConnection.initialiseSchema(connection);
            StationRepository repo = new StationRepository(connection);
            Map<String, TrackElement> layout = repo.loadLayout();
            service = new InterlockingService(layout, repo.loadRoutes(layout), repo);
            service.addHistory(repo.recentEvents(200));
            storageText = "Database: " + AppConfig.getDbUrl();
        } catch (SQLException e) {
            LOG.severe("Database unavailable, running in memory: " + e.getMessage());
            closeConnection();
            service = StationFactory.createDemoService();
            storageText = "Database unavailable (" + e.getMessage() + ") - running in memory";
        }
    }

    @Override
    public void start(Stage stage) {
        diagram = new TrackDiagram(new TrackDiagram.ClickHandler() {
            @Override
            public void sectionClicked(String sectionId) {
                TrackSection s = (TrackSection) service.getLayout().get(sectionId);
                runAction(() -> service.setSectionOccupied(sectionId, !s.isOccupied()));
            }

            @Override
            public void pointClicked(String pointId) {
                runAction(() -> service.throwPoint(pointId));
            }
        });

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));
        root.setTop(buildHeader());
        VBox centre = new VBox(10, new StackPane(diagram), buildLegend(), buildTables());
        VBox.setVgrow(centre.getChildren().get(2), Priority.ALWAYS);
        centre.setPadding(new Insets(10, 10, 0, 0));
        root.setCenter(centre);
        root.setRight(buildControls());

        refresh();
        if (service.getActiveRoutes().isEmpty()) {
            statusLabel.setText("Ready. Click a section to toggle occupancy, a point to throw it.");
        } else {
            statusLabel.setText("Restored " + service.getActiveRoutes().size() + " active route(s) from the database.");
        }

        stage.setTitle("Railway Interlocking - " + AppConfig.getStationName());
        stage.setScene(new Scene(root, 1120, 780));
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        stage.show();
    }

    @Override
    public void stop() {
        closeConnection();
    }

    // ------------------------------------------------------------------ layout

    private VBox buildHeader() {
        Label title = new Label("Railway Signal Interlocking");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        Label station = new Label(AppConfig.getStationName() + "   |   " + storageText);
        station.setTextFill(Color.web("#5f6368"));
        return new VBox(2, title, station);
    }

    private HBox buildLegend() {
        HBox legend = new HBox(18,
                legendItem(TrackDiagram.FREE, "Free"),
                legendItem(TrackDiagram.LOCKED, "Locked by route"),
                legendItem(TrackDiagram.OCCUPIED, "Occupied"),
                new Label("Point label: N = normal (main), R = reverse (loop)"));
        legend.setAlignment(Pos.CENTER_LEFT);
        return legend;
    }

    private HBox legendItem(Color color, String text) {
        Rectangle swatch = new Rectangle(22, 8, color);
        HBox box = new HBox(6, swatch, new Label(text));
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private VBox buildTables() {
        TableColumn<TrackElement, String> idCol = column("Element", 80);
        idCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        TableColumn<TrackElement, String> typeCol = column("Type", 70);
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        TableColumn<TrackElement, String> statusCol = column("Status", 160);
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        elementTable.getColumns().add(idCol);
        elementTable.getColumns().add(typeCol);
        elementTable.getColumns().add(statusCol);
        elementTable.setPrefWidth(330);

        TableColumn<Route, String> rIdCol = column("Route", 55);
        rIdCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        TableColumn<Route, String> rDescCol = column("Description", 100);
        rDescCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
        TableColumn<Route, String> rPathCol = column("Path", 150);
        rPathCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPathText()));
        TableColumn<Route, String> rStateCol = column("State", 170);
        rStateCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStateText()));
        routeTable.getColumns().add(rIdCol);
        routeTable.getColumns().add(rDescCol);
        routeTable.getColumns().add(rPathCol);
        routeTable.getColumns().add(rStateCol);
        routeTable.getSelectionModel().selectedItemProperty().addListener((obs, oldRoute, newRoute) -> {
            if (newRoute != null && !updatingControls) {
                routeBox.getSelectionModel().select(newRoute);
            }
        });

        HBox upper = new HBox(10, elementTable, routeTable);
        HBox.setHgrow(routeTable, Priority.ALWAYS);
        upper.setPrefHeight(230);

        TableColumn<EventLogEntry, String> timeCol = column("Time", 140);
        timeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTimestampText()));
        TableColumn<EventLogEntry, String> lRouteCol = column("Route", 55);
        lRouteCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRouteId()));
        TableColumn<EventLogEntry, String> actionCol = column("Action", 80);
        actionCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAction()));
        TableColumn<EventLogEntry, String> msgCol = column("Message", 480);
        msgCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMessage()));
        logTable.getColumns().add(timeCol);
        logTable.getColumns().add(lRouteCol);
        logTable.getColumns().add(actionCol);
        logTable.getColumns().add(msgCol);
        logTable.setPlaceholder(new Label("No events yet"));

        Label logTitle = new Label("Event log");
        logTitle.setFont(Font.font("System", FontWeight.BOLD, 13));
        VBox box = new VBox(6, upper, logTitle, logTable);
        VBox.setVgrow(logTable, Priority.ALWAYS);
        return box;
    }

    private <S> TableColumn<S, String> column(String title, double width) {
        TableColumn<S, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setSortable(false);
        return col;
    }

    private VBox buildControls() {
        // routes
        routeBox.getItems().setAll(service.getRoutes());
        routeBox.getSelectionModel().selectFirst();
        routeBox.setMaxWidth(Double.MAX_VALUE);
        Button setButton = wideButton("Set route");
        setButton.setOnAction(e -> runAction(() -> service.setRoute(selectedRouteId())));
        Button cancelButton = wideButton("Cancel route");
        cancelButton.setOnAction(e -> runAction(() -> service.cancelRoute(selectedRouteId())));
        Button moveButton = wideButton("Move train one section");
        moveButton.setOnAction(e -> runAction(() -> service.moveTrain(selectedRouteId())));

        // points
        List<String> pointIds = new ArrayList<>();
        List<String> sectionIds = new ArrayList<>();
        for (TrackElement el : service.getLayout().values()) {
            if (el instanceof com.trains.interlocking.model.Point) {
                pointIds.add(el.getId());
            } else if (el instanceof TrackSection) {
                sectionIds.add(el.getId());
            }
        }
        pointBox.getItems().setAll(pointIds);
        pointBox.getSelectionModel().selectFirst();
        pointBox.setMaxWidth(Double.MAX_VALUE);
        Button throwButton = wideButton("Throw point");
        throwButton.setOnAction(e -> runAction(() -> service.throwPoint(pointBox.getValue())));

        // track circuits
        sectionBox.getItems().setAll(sectionIds);
        sectionBox.getSelectionModel().selectFirst();
        sectionBox.setMaxWidth(Double.MAX_VALUE);
        sectionBox.valueProperty().addListener((obs, oldId, newId) -> syncOccupiedBox());
        occupiedBox.setOnAction(e -> {
            if (!updatingControls) {
                boolean wanted = occupiedBox.isSelected();
                runAction(() -> service.setSectionOccupied(sectionBox.getValue(), wanted));
            }
        });

        // snapshot
        Button saveButton = new Button("Save");
        saveButton.setOnAction(e -> runAction(() -> service.saveSnapshot(snapshotFile())));
        Button loadButton = new Button("Load");
        loadButton.setOnAction(e -> runAction(() -> service.loadSnapshot(snapshotFile())));
        saveButton.setMaxWidth(Double.MAX_VALUE);
        loadButton.setMaxWidth(Double.MAX_VALUE);
        HBox snapshotButtons = new HBox(6, saveButton, loadButton);
        HBox.setHgrow(saveButton, Priority.ALWAYS);
        HBox.setHgrow(loadButton, Priority.ALWAYS);

        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(250);

        VBox controls = new VBox(8,
                heading("Routes"), routeBox, setButton, cancelButton, moveButton,
                heading("Points"), pointBox, throwButton,
                heading("Track circuits"), sectionBox, occupiedBox,
                heading("Snapshot file"), snapshotField, snapshotButtons,
                heading("Last result"), statusLabel);
        controls.setPadding(new Insets(10, 0, 0, 10));
        controls.setPrefWidth(260);
        controls.setMinWidth(260);
        return controls;
    }

    private Label heading(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.BOLD, 13));
        label.setPadding(new Insets(8, 0, 0, 0));
        return label;
    }

    private Button wideButton(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    // ------------------------------------------------------------------ actions

    /** A UI action that may fail with any checked or unchecked exception. */
    private interface UiAction {
        void run() throws Exception;
    }

    /** Runs an action, shows a dialog for refusals, then redraws everything. */
    private void runAction(UiAction action) {
        int before = service.getEventLog().size();
        try {
            action.run();
            List<EventLogEntry> log = service.getEventLog();
            statusLabel.setTextFill(Color.web("#2e7d32"));
            statusLabel.setText(log.size() > before ? log.get(log.size() - 1).getMessage() : "No change");
        } catch (RouteConflictException e) {
            refused("Route conflict", e.getMessage());
        } catch (SectionOccupiedException e) {
            refused("Section occupied", e.getMessage());
        } catch (PointLockedException e) {
            refused("Point locked", e.getMessage());
        } catch (IllegalStateException | IllegalArgumentException e) {
            refused("Not allowed", e.getMessage());
        } catch (IOException e) {
            refused("Snapshot error", e.getMessage());
        } catch (Exception e) {
            LOG.severe("Unexpected error: " + e);
            refused("Error", e.toString());
        }
        refresh();
    }

    private void refused(String title, String message) {
        statusLabel.setTextFill(Color.web("#d32f2f"));
        statusLabel.setText(message);
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Request refused");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String selectedRouteId() {
        Route r = routeBox.getValue();
        if (r == null) {
            throw new IllegalArgumentException("Select a route first");
        }
        return r.getId();
    }

    private File snapshotFile() {
        String name = snapshotField.getText().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Enter a snapshot file name");
        }
        return new File(name);
    }

    // ------------------------------------------------------------------ refresh

    private void refresh() {
        updatingControls = true;
        try {
            diagram.refresh(service);
            elementTable.getItems().setAll(service.getLayout().values());
            elementTable.refresh();

            // a loaded snapshot replaces the route objects, so rebuild the combo too
            String selected = routeBox.getValue() == null ? null : routeBox.getValue().getId();
            List<Route> routes = service.getRoutes();
            routeBox.getItems().setAll(routes);
            routeTable.getItems().setAll(routes);
            routeTable.refresh();
            if (selected != null) {
                routeBox.getSelectionModel().select(service.getRoute(selected));
            }

            logTable.getItems().setAll(service.getEventLog());
            if (!logTable.getItems().isEmpty()) {
                logTable.scrollTo(logTable.getItems().size() - 1);
            }
        } finally {
            updatingControls = false;
        }
        syncOccupiedBox();
    }

    private void syncOccupiedBox() {
        TrackElement e = service.getLayout().get(sectionBox.getValue());
        if (e instanceof TrackSection) {
            updatingControls = true;
            occupiedBox.setSelected(((TrackSection) e).isOccupied());
            occupiedBox.setDisable(e.isLocked());
            updatingControls = false;
        }
    }

    private void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOG.warning("Could not close database: " + e.getMessage());
            }
            connection = null;
        }
    }
}
