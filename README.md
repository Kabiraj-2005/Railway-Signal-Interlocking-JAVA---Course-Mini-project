# Railway Signal Interlocking

UCS553 Enterprise Web Application (Core Java) mini-project.
This is a desktop app built with Java 21, JDBC (SQLite) and JavaFX.

## Team

| Member | Responsibility |
|--------|----------------|
| Utpal  | Interlocking logic: `model`, `service`, `exception`, serialization, logging |
| Harsh  | Database: `schema.sql`, `dao` implementations, `util/DatabaseConnection`, transactions |
| Aryan  | JavaFX UI: `ui` |

## Station layout

```
              T3 (loop platform)
          /----------------------\
 T1 ---- P1 ------ T2 (main) ----- P2 ---- T4
 [SIG_W]                                [SIG_E]
```

- Track sections: T1, T2, T3, T4
- Points: P1, P2
- Signals: SIG_W, SIG_E

### Routes

| Route | Description | Sections | Points |
|-------|-------------|----------|--------|
| R1 | West -> Main | T1, T2 | P1 normal |
| R2 | West -> Loop | T1, T3 | P1 reverse |
| R3 | East -> Main | T4, T2 | P2 normal |
| R4 | East -> Loop | T4, T3 | P2 reverse |

**Expected conflicts:** R1-R3 (share T2), R2-R4 (share T3), R1-R2 (P1 in different positions), R3-R4 (P2 in different positions).
**Compatible pairs:** R1 + R4, R2 + R3.

### Interlocking rules

- A route can be set only if all its sections are clear and unlocked and its points can be moved.
- Two routes conflict if they share a section or need the same point in different positions.
- Points cannot move while locked or occupied.
- Sections are released one by one as the train passes.

## Package structure

```
src/main/java/com/trains/interlocking/
├── Main.java              console test harness (15 scenarios, prints PASS/FAIL and a total)
├── model/                 TrackElement, TrackSection, Point, Signal, Route, EventLogEntry
├── service/               InterlockingService (actions), SafetyRules (checks),
│                          SnapshotFile (serialization), StationFactory, StationStore
├── dao/                   Dao<T> + TrackSectionDao, PointDao, SignalDao, RouteDao,
│                          EventLogDao, StationRepository (implements StationStore)
├── exception/             PointLockedException, RouteConflictException, SectionOccupiedException
├── util/                  AppConfig, DatabaseConnection, LogConfig
└── ui/                    MainApp (JavaFX), Launcher
src/main/resources/
├── app.properties         db.url, station.name, log.file
└── schema.sql             tables + starting data for the demo station
```

See `STUDY_GUIDE.md` for a walkthrough of every file and `CHANGES.md` for what changed.

## How it works

- **Point zone:** a point lies in the section shared by every route through it
  (P1 in T1, P2 in T4). A point cannot move while it is locked or while that section is occupied.
- **Train movement:** the first "Move train" puts the train in the first section and turns the
  entry signal red. Each later move clears and releases the section behind the train. After the
  last section the route ends and its points are released.
- **Refusals:** every refused request throws one of the three checked exceptions
  (cancel/move problems use `RouteConflictException`, locked sections use `SectionOccupiedException`).
- **Persistence:** after every change the whole state is saved to SQLite in ONE transaction,
  and every action is added to `event_log`. Restarting the app restores the exact state.
  Delete `interlocking.db` to reset the station.
- **Snapshot:** "Save snapshot" / "Load snapshot" write and read the station with
  `ObjectOutputStream` / `ObjectInputStream`.
- **Logging:** `java.util.logging` writes to the file named by `log.file` (`interlocking.log`).

### Using the UI

- The coloured row shows the track: grey = free, yellow = locked by a route, red = occupied;
  signals are green or red.
- Pick a route, then **Set route**, **Move train one section** or **Cancel route**.
- **Throw point** moves the chosen point by hand.
- **Mark occupied / Mark clear** pretends a train (or a fault) is on a section.
- Refused requests appear in a dialog and in the event log.

## Requirements

- JDK 21 or newer (the compile target is Java 21)
- Maven 3.8+

## How to run

```bash
mvn compile          # build
mvn javafx:run       # start the JavaFX app
```

To run the console test harness:

```bash
mvn compile exec:java -Dexec.mainClass=com.trains.interlocking.Main
```

You can also run `Main` directly from your IDE.

**IntelliJ:** open the folder as a Maven project, then run `com.trains.interlocking.ui.Launcher`.
Use `Launcher`, not `MainApp`, to avoid JavaFX module-path errors.

## Syllabus constraints

Stay inside the course syllabus.

**Allowed:** classes, abstract classes, interfaces, packages, access modifiers, static/final,
custom checked exceptions, ArrayList/LinkedList/HashSet/HashMap/Iterator/ListIterator/Comparator,
generics, Objects/Optional/Math/LocalDate/LocalTime/LocalDateTime, java.io streams, Serializable,
java.util.logging, JDBC (DriverManager, PreparedStatement, ResultSet, transactions), DAO pattern,
JavaFX (Application lifecycle, Stage, Scene, HBox/VBox/GridPane/BorderPane/StackPane,
Label/Button/TextField/CheckBox/ComboBox/TableView, event handlers, ChangeListener, Alert/Dialog).

**Do NOT use:**
- `enum`
- records
- Streams API (`.stream()`)
- threads / `Timeline` / animation
- FXML
- CSS files
- Spring, Hibernate, Lombok
- JUnit
